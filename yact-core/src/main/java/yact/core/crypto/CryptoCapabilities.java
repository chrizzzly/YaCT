/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.crypto;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.generators.MLDSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.MLKEMKeyPairGenerator;
import org.bouncycastle.crypto.generators.SLHDSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator;
import org.bouncycastle.crypto.kems.MLKEMExtractor;
import org.bouncycastle.crypto.kems.MLKEMGenerator;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.MLDSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.MLDSAParameters;
import org.bouncycastle.crypto.params.MLKEMKeyGenerationParameters;
import org.bouncycastle.crypto.params.MLKEMParameters;
import org.bouncycastle.crypto.params.MLKEMPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.SLHDSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.SLHDSAParameters;
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters;
import org.bouncycastle.crypto.signers.MLDSASigner;
import org.bouncycastle.crypto.signers.SLHDSASigner;

/**
 * Checks at runtime that every cryptographic primitive YaCT relies on is
 * present and behaves correctly in the current runtime.
 *
 * <p>Each check runs a small self-test (round trip and, where it applies,
 * rejection of tampered input). This is not a replacement for known-answer
 * tests: those live in the test suite. The purpose is a quick "does this
 * installation work" answer that the UI and the command line can show.
 *
 * <p>All primitives are used through the BouncyCastle lightweight API so
 * behaviour does not depend on JCA provider registration or on the JDK
 * version (JDK 21 has no built-in ML-KEM or ML-DSA).
 */
public final class CryptoCapabilities
{
	/** Result of a single self-test. */
	public record Capability(String name, String purpose, boolean postQuantum, boolean available, String detail)
	{
	}

	private static final SecureRandom RANDOM = new SecureRandom();

	private CryptoCapabilities()
	{
	}

	/** Runs all self-tests and returns one entry per primitive, in a fixed order. */
	public static List<Capability> check()
	{
		List<Capability> result = new ArrayList<>();
		result.add(run("AES-256-GCM", "Data encryption (default)", false, CryptoCapabilities::aesGcm));
		result.add(run("ChaCha20-Poly1305", "Data encryption (alternative)", false, CryptoCapabilities::chaCha20Poly1305));
		result.add(run("Argon2id", "Password-based key derivation", false, CryptoCapabilities::argon2id));
		result.add(run("HKDF-SHA3-256", "Key derivation / combining secrets", false, CryptoCapabilities::hkdf));
		result.add(run("X25519", "Classical half of hybrid key slot", false, CryptoCapabilities::x25519));
		result.add(run("ML-KEM-768", "PQ key encapsulation (FIPS 203)", true, () -> mlKem(MLKEMParameters.ml_kem_768)));
		result.add(run("ML-KEM-1024", "PQ key encapsulation (FIPS 203)", true, () -> mlKem(MLKEMParameters.ml_kem_1024)));
		result.add(run("ML-DSA-65", "PQ signature (FIPS 204)", true, CryptoCapabilities::mlDsa));
		result.add(run("SLH-DSA-SHA2-128s", "PQ hash-based signature (FIPS 205)", true, CryptoCapabilities::slhDsa));
		return result;
	}

	/** True if every primitive passed its self-test. */
	public static boolean allAvailable(List<Capability> capabilities)
	{
		return capabilities.stream().allMatch(Capability::available);
	}

	private static Capability run(String name, String purpose, boolean pq, Supplier<String> test)
	{
		try
		{
			return new Capability(name, purpose, pq, true, test.get());
		}
		catch (RuntimeException | LinkageError e)
		{
			return new Capability(name, purpose, pq, false, e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	// ---------------------------------------------------------------- AEAD

	private static String aesGcm()
	{
		return aead(GCMBlockCipher.newInstance(AESEngine.newInstance()), 12);
	}

	private static String chaCha20Poly1305()
	{
		return aead(new ChaCha20Poly1305(), 12);
	}

	private static String aead(AEADCipher cipher, int nonceLength)
	{
		byte[] key = random(32);
		byte[] nonce = random(nonceLength);
		byte[] aad = "yact-selftest".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
		byte[] plain = random(1000);

		byte[] sealed = aeadProcess(cipher, true, key, nonce, aad, plain);
		byte[] opened = aeadProcess(cipher, false, key, nonce, aad, sealed);
		check(Arrays.equals(plain, opened), "round trip failed");

		sealed[sealed.length / 2] ^= 1;
		boolean rejected;
		try
		{
			aeadProcess(cipher, false, key, nonce, aad, sealed);
			rejected = false;
		}
		catch (IllegalStateException e)
		{
			rejected = true;
		}
		check(rejected, "tampered ciphertext was accepted");
		return cipher.getAlgorithmName() + ", 256-bit key, 128-bit tag";
	}

	/** Package-private so the test suite can run known-answer vectors through the same code path. */
	static byte[] aeadProcess(AEADCipher cipher, boolean encrypt, byte[] key, byte[] nonce, byte[] aad, byte[] input)
	{
		cipher.init(encrypt, new AEADParameters(new KeyParameter(key), 128, nonce, aad));
		byte[] out = new byte[cipher.getOutputSize(input.length)];
		int len = cipher.processBytes(input, 0, input.length, out, 0);
		try
		{
			len += cipher.doFinal(out, len);
		}
		catch (org.bouncycastle.crypto.InvalidCipherTextException e)
		{
			throw new IllegalStateException("authentication failed", e);
		}
		return len == out.length ? out : Arrays.copyOf(out, len);
	}

	// ---------------------------------------------------------------- KDFs

	private static String argon2id()
	{
		// Deliberately small parameters: this only proves the implementation works.
		Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
				.withVersion(Argon2Parameters.ARGON2_VERSION_13)
				.withSalt(random(16))
				.withIterations(1)
				.withMemoryAsKB(1024)
				.withParallelism(1)
				.build();
		byte[] password = "selftest".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		byte[] a = argon2(params, password);
		byte[] b = argon2(params, password);
		check(Arrays.equals(a, b), "not deterministic");
		check(!Arrays.equals(a, argon2(params, "selftesT".getBytes(java.nio.charset.StandardCharsets.UTF_8))),
				"different passwords give the same key");
		return "Argon2 v1.3, id variant";
	}

	static byte[] argon2(Argon2Parameters params, byte[] password)
	{
		Argon2BytesGenerator gen = new Argon2BytesGenerator();
		gen.init(params);
		byte[] out = new byte[32];
		gen.generateBytes(password, out);
		return out;
	}

	private static String hkdf()
	{
		byte[] ikm = random(32);
		byte[] a = hkdf(ikm, "a");
		check(Arrays.equals(a, hkdf(ikm, "a")), "not deterministic");
		check(!Arrays.equals(a, hkdf(ikm, "b")), "info is ignored");
		return "HKDF (RFC 5869) over SHA3-256";
	}

	private static byte[] hkdf(byte[] ikm, String info)
	{
		HKDFBytesGenerator gen = new HKDFBytesGenerator(new SHA3Digest(256));
		gen.init(new HKDFParameters(ikm, null, info.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
		byte[] out = new byte[32];
		gen.generateBytes(out, 0, out.length);
		return out;
	}

	// ---------------------------------------------------------------- key agreement / KEM

	private static String x25519()
	{
		X25519KeyPairGenerator gen = new X25519KeyPairGenerator();
		gen.init(new X25519KeyGenerationParameters(RANDOM));
		AsymmetricCipherKeyPair alice = gen.generateKeyPair();
		AsymmetricCipherKeyPair bob = gen.generateKeyPair();

		check(Arrays.equals(agree(alice, bob), agree(bob, alice)), "shared secrets differ");
		return "RFC 7748";
	}

	private static byte[] agree(AsymmetricCipherKeyPair self, AsymmetricCipherKeyPair peer)
	{
		X25519Agreement agreement = new X25519Agreement();
		agreement.init(self.getPrivate());
		byte[] secret = new byte[agreement.getAgreementSize()];
		agreement.calculateAgreement(peer.getPublic(), secret, 0);
		return secret;
	}

	private static String mlKem(MLKEMParameters params)
	{
		MLKEMKeyPairGenerator gen = new MLKEMKeyPairGenerator();
		gen.init(new MLKEMKeyGenerationParameters(RANDOM, params));
		AsymmetricCipherKeyPair pair = gen.generateKeyPair();

		SecretWithEncapsulation enc = new MLKEMGenerator(RANDOM).generateEncapsulated(pair.getPublic());
		MLKEMExtractor extractor = new MLKEMExtractor((MLKEMPrivateKeyParameters) pair.getPrivate());
		check(Arrays.equals(enc.getSecret(), extractor.extractSecret(enc.getEncapsulation())),
				"decapsulated secret differs");

		// ML-KEM uses implicit rejection: a modified ciphertext yields an unrelated secret.
		byte[] tampered = enc.getEncapsulation().clone();
		tampered[0] ^= 1;
		check(!Arrays.equals(enc.getSecret(), extractor.extractSecret(tampered)),
				"tampered ciphertext gave the original secret");

		return params.getName() + ", ciphertext " + enc.getEncapsulation().length + " bytes";
	}

	// ---------------------------------------------------------------- signatures

	private static String mlDsa()
	{
		MLDSAKeyPairGenerator gen = new MLDSAKeyPairGenerator();
		gen.init(new MLDSAKeyGenerationParameters(RANDOM, MLDSAParameters.ml_dsa_65));
		AsymmetricCipherKeyPair pair = gen.generateKeyPair();
		return signVerify(
				message -> {
					MLDSASigner signer = new MLDSASigner();
					signer.init(true, new ParametersWithRandom(pair.getPrivate(), RANDOM));
					signer.update(message, 0, message.length);
					try
					{
						return signer.generateSignature();
					}
					catch (CryptoException e)
					{
						throw new IllegalStateException("signing failed", e);
					}
				},
				(message, signature) -> {
					MLDSASigner signer = new MLDSASigner();
					signer.init(false, pair.getPublic());
					signer.update(message, 0, message.length);
					return signer.verifySignature(signature);
				});
	}

	private static String slhDsa()
	{
		SLHDSAKeyPairGenerator gen = new SLHDSAKeyPairGenerator();
		gen.init(new SLHDSAKeyGenerationParameters(RANDOM, SLHDSAParameters.sha2_128s));
		AsymmetricCipherKeyPair pair = gen.generateKeyPair();
		return signVerify(
				message -> {
					SLHDSASigner signer = new SLHDSASigner();
					signer.init(true, new ParametersWithRandom(pair.getPrivate(), RANDOM));
					return signer.generateSignature(message);
				},
				(message, signature) -> {
					SLHDSASigner signer = new SLHDSASigner();
					signer.init(false, pair.getPublic());
					return signer.verifySignature(message, signature);
				});
	}

	private static String signVerify(Function<byte[], byte[]> sign, BiPredicate<byte[], byte[]> verify)
	{
		byte[] message = random(64);
		byte[] signature = sign.apply(message);

		check(verify.test(message, signature), "valid signature rejected");
		message[0] ^= 1;
		check(!verify.test(message, signature), "signature over modified message accepted");
		return "signature " + signature.length + " bytes";
	}

	// ---------------------------------------------------------------- helpers

	private static byte[] random(int length)
	{
		byte[] b = new byte[length];
		RANDOM.nextBytes(b);
		return b;
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new IllegalStateException(message);
	}

	/** Prints the report; exit code 1 if any primitive is missing. */
	public static void main(String[] args)
	{
		List<Capability> caps = check();
		System.out.println("YaCT crypto self-test (BouncyCastle "
				+ new org.bouncycastle.jce.provider.BouncyCastleProvider().getVersionStr()
				+ ", Java " + Runtime.version() + ")");
		for (Capability c : caps)
		{
			System.out.printf("  [%s] %-20s %-5s %-40s %s%n",
					c.available() ? " OK " : "FAIL", c.name(), c.postQuantum() ? "PQ" : "", c.purpose(), c.detail());
		}
		System.exit(allAvailable(caps) ? 0 : 1);
	}
}
