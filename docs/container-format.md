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

## Key slot type 2: hybrid public key (post-quantum)

Opened with a private key instead of a password. Anyone who has the public key
can add such a slot; only the private key opens it.

| Size | Field |
|---|---|
| 1 | scheme: `1` ML-KEM-768 + X25519, `2` ML-KEM-1024 + X25519 (default) |
| 32 | ephemeral X25519 public key |
| 1088 / 1568 | ML-KEM ciphertext (ML-KEM-768 / ML-KEM-1024) |
| 12 | nonce |
| 48 | master key encrypted with the container's cipher suite (32 + 16-byte tag) |

Wrapping key, with ss_M from ML-KEM encapsulation and ss_X from X25519
between the ephemeral key and the recipient's X25519 key:

```
KEK = HKDF-SHA3-256(ikm  = ss_M ‖ ss_X,
                    salt = container id,
                    info = "YaCT v1 hybrid kem", scheme id, ML-KEM ciphertext,
                           ephemeral X25519 key, recipient X25519 key,
                           recipient ML-KEM key)
```

Both shared secrets and all ciphertexts and public keys enter the KDF. This is
the generic KEM combiner (Giacon, Heuer, Poettering, PKC 2018): the KEK stays
secret as long as **either** ML-KEM or X25519 is unbroken.

- AEAD associated data = `"YaCT v1 hybrid slot"` ‖ container id ‖ scheme id ‖
  ephemeral key ‖ ML-KEM ciphertext.
- The slot does not name its recipient. A reader tries every slot of its key's
  scheme (well under a millisecond each), and the container does not reveal
  whose keys can open it.
- X-Wing (IETF draft) was considered. It is only defined for ML-KEM-768 and
  still a draft, so YaCT uses the combiner above, which supports ML-KEM-1024.
- 16 slots of the largest kind take about 27 KiB and fit into the header area.

## Key files

### Public key

| Size | Field |
|---|---|
| 4 | magic `YAPK` |
| 2 | version, `1` |
| 1 | scheme id (see slot type 2) |
| 32 | X25519 public key |
| 1184 / 1568 | ML-KEM encapsulation key |

On import the ML-KEM key must pass the modulus check of FIPS 203, section 7.2,
and the X25519 key must not be a low-order point.

Text form for e-mail or chat: Base64 of the above, 64 characters per line,
between `-----BEGIN YACT PUBLIC KEY-----` and `-----END YACT PUBLIC KEY-----`.

Fingerprint: first 20 bytes of SHA3-256 over the binary form, written as ten
groups of four hex digits. Users compare it over a second channel before trusting
a key.

### Private key

| Size | Field |
|---|---|
| 4 | magic `YASK` |
| 2 | version, `1` |
| 1 | scheme id |
| 4 + 4 + 1 | Argon2id memory (KiB), iterations, parallelism |
| 16 | salt |
| 12 | nonce |
| 112 | AES-256-GCM(ML-KEM seed (64) ‖ X25519 scalar (32)) + 16-byte tag |

- Key = Argon2id(password, salt, params); associated data = all preceding bytes.
- The ML-KEM key is stored as its 64-byte seed (d ‖ z), the form FIPS 203
  recommends; the public key is recomputed from the seeds.

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
- **Metadata:** the file size reveals roughly how much data is stored, and the
  header shows how many password and public-key slots exist.
- **Fake public keys:** YaCT cannot tell whether a public key really belongs to
  the person it claims to; users must compare fingerprints.
