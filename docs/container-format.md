# YaCT container format, version 1

Status: draft. The format may still change until the first release; there is no
migration from earlier drafts.

All integers are unsigned and big-endian. Sizes are in bytes.

## Overview

```
offset 0        header area, 65 536 bytes (fixed)
offset 65 536   encrypted data (layout defined in milestone 4)
```

The header area has a fixed size, so adding or removing key slots never moves
the encrypted data. Bytes after the header's MAC are zero.

## Key hierarchy

```
password ──Argon2id(salt, params)──► key-encryption key (KEK)
                                         │ unwraps (AEAD)
                                         ▼
                              master key (32 random bytes)
                                         │ HKDF-SHA3-256, salt = container id
             ┌───────────────────────────┴───────────────────────────┐
  "YaCT v1 header mac"                                  "YaCT v1 stream key" ‖ stream salt
             ▼                                                       ▼
   header MAC key (HMAC-SHA3-256)                       one key per encrypted stream
```

- The master key never changes for the life of a container. Changing a password
  rewraps it in a new slot; the data is not re-encrypted.
- Every key slot wraps the same master key.
- HKDF `info` is the label followed by each context value, each prefixed with
  its 2-byte length.

## Header

| Size | Field |
|---|---|
| 4 | magic `YACT` |
| 2 | format version, `1` |
| 1 | cipher suite: `1` AES-256-GCM, `2` ChaCha20-Poly1305 |
| 1 | chunk size exponent *n*, chunk size = 2^*n*, 12 ≤ *n* ≤ 24 |
| 16 | container id, random |
| 1 | number of key slots, 1..16 |
| … | key slots: 1 byte type, 2 bytes body length, body |
| 32 | HMAC-SHA3-256 over all preceding header bytes |

The MAC key is derived from the master key, so the header can only be verified
after a key slot has been opened. A reader:

1. parses the header and rejects anything outside the limits above;
2. tries each key slot it understands;
3. verifies the MAC with the recovered master key and rejects the container if
   it does not match.

Slots of unknown type are skipped when unlocking and kept byte for byte when the
header is rewritten.

## Key slot type 1: password

| Size | Field |
|---|---|
| 4 | Argon2id memory in KiB, at most 4 GiB |
| 4 | Argon2id iterations, 1..64 |
| 1 | Argon2id parallelism, 1..16 |
| 16 | salt |
| 12 | nonce |
| 48 | master key encrypted with the container's cipher suite (32 + 16-byte tag) |

- KEK = Argon2id v1.3 (password, salt, params), 32 bytes.
- Password bytes = UTF-8 of the password in Unicode NFC.
- AEAD associated data = `"YaCT v1 password slot"` ‖ container id ‖ the first
  three fields and the salt, as encoded above. This binds the slot to its
  container and its parameters.
- Default parameters: 64 MiB, 3 iterations, 4 lanes (RFC 9106, second
  recommended option). Strong profile: 256 MiB, 4 iterations, 4 lanes.

## Encrypted streams

Data is encrypted as streams using the STREAM construction (Hoang, Reyhanitabar,
Rogaway, Vizár, 2015).

- Stream key = HKDF(master key, salt = container id,
  info = `"YaCT v1 stream key"`, stream salt). The stream salt is at least
  16 random bytes, stored by the caller next to the stream, and never reused.
- Plaintext is split into chunks of the header's chunk size. Each chunk is
  encrypted separately with the container's cipher suite; output per chunk is
  ciphertext ‖ 16-byte tag.
- A final chunk with the remaining 0..chunk size bytes is always written, also
  for empty input.
- Nonce (12 bytes): 7 zero bytes ‖ 4-byte chunk index ‖ 1 byte, `1` for the
  final chunk and `0` otherwise. The fixed prefix is safe because every stream
  has its own key. Streams are limited to 2^32 chunks.
- Every chunk carries the same associated data, chosen by the caller (for
  example a file id), so a stream cannot be moved to another place in the
  container.

A reader releases plaintext one verified chunk at a time and rejects modified,
reordered, missing, appended or truncated chunks. A stream counts as complete
only once its final chunk has been verified.

## What this format does not protect against

- **Rollback:** an attacker with write access can replace the whole container
  with an older copy of itself.
- **Removed key slots in old copies:** removing a slot does not help against
  someone who copied the container while the slot existed.
- **Metadata:** the file size reveals roughly how much data is stored.
