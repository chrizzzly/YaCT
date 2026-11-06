# YaCT – Yet another Crypto Tool

Encrypted file vault with post-quantum key slots.

> **Status: under reconstruction.** The legacy Eclipse code in `src/` does not
> encrypt anything and must not be used. It stays in the repository only until its
> UI parts are migrated. The new code lives in the Maven modules below.

## Modules

| Module      | Contents                                                     |
|-------------|--------------------------------------------------------------|
| `yact-core` | UI-independent crypto and container engine (BouncyCastle)    |
| `yact-ui`   | JavaFX desktop client                                        |

## Requirements

- JDK 21 or newer
- Maven 3.9+

## Build and test

```sh
mvn verify
```

## Crypto self-test

Checks that every primitive YaCT relies on works in the current runtime:

```sh
mvn -q install -DskipTests
java -cp yact-core/target/classes:$(mvn -q -pl yact-core dependency:build-classpath -Dmdep.outputFile=/dev/stdout) \
     yact.core.crypto.CryptoCapabilities
```

Or start the client, which currently shows the same report:

```sh
mvn -q install -DskipTests
mvn -q -pl yact-ui javafx:run
```

## Planned cryptography

| Purpose                  | Algorithm                                       |
|--------------------------|-------------------------------------------------|
| Data encryption          | AES-256-GCM (default), ChaCha20-Poly1305        |
| Password → key           | Argon2id                                        |
| Combining secrets        | HKDF over SHA3-256                              |
| Public-key key slot      | Hybrid ML-KEM-768/1024 (FIPS 203) + X25519      |
| Signatures (optional)    | ML-DSA-65 (FIPS 204), SLH-DSA (FIPS 205)        |

All primitives come from BouncyCastle (`bcprov-jdk18on`); YaCT implements no
cryptographic algorithms itself. The file format is specified in
[docs/container-format.md](docs/container-format.md).

## Progress

| Milestone | Content | Status |
|---|---|---|
| 1 | Maven build, crypto self-test | done |
| 2 | Container header, password key slots, chunked data encryption | done |
| 3 | Hybrid post-quantum key slot (ML-KEM + X25519) | open |
| 4 | Container operations: create, open, add/extract/delete files | open |
| 5 | Connect the JavaFX UI | open |
| 6 | Remove legacy code, documentation | open |

## License

MIT, see `Licence.txt`.
