/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

class CryptoCapabilitiesTest
{
	@Test
	void allPrimitivesPassSelfTest()
	{
		List<CryptoCapabilities.Capability> caps = CryptoCapabilities.check();
		String failures = caps.stream()
				.filter(c -> !c.available())
				.map(c -> c.name() + ": " + c.detail())
				.collect(Collectors.joining("; "));
		assertTrue(CryptoCapabilities.allAvailable(caps), failures);
	}

	@Test
	void postQuantumPrimitivesAreReported()
	{
		List<String> pq = CryptoCapabilities.check().stream()
				.filter(CryptoCapabilities.Capability::postQuantum)
				.map(CryptoCapabilities.Capability::name)
				.toList();
		assertEquals(List.of("ML-KEM-768", "ML-KEM-1024", "ML-DSA-65", "SLH-DSA-SHA2-128s"), pq);
	}

	// ------------------------------------------------------------ known-answer tests

	/** McGrew/Viega GCM spec, test case 14: AES-256, zero key, zero IV, one zero block. */
	@Test
	void aes256GcmKnownAnswer()
	{
		byte[] out = CryptoCapabilities.aeadProcess(GCMBlockCipher.newInstance(AESEngine.newInstance()), true,
				new byte[32], new byte[12], new byte[0], new byte[16]);
		assertArrayEquals(Hex.decode("cea7403d4d606b6e074ec5d3baf39d18" + "d0d1c8a799996bf0265b98b5d48ab919"), out);
	}

	/** RFC 9106, section 5.3: Argon2id test vector. */
	@Test
	void argon2idKnownAnswer()
	{
		Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
				.withVersion(Argon2Parameters.ARGON2_VERSION_13)
				.withIterations(3)
				.withMemoryAsKB(32)
				.withParallelism(4)
				.withSalt(fill(16, 0x02))
				.withSecret(fill(8, 0x03))
				.withAdditional(fill(12, 0x04))
				.build();
		assertArrayEquals(Hex.decode("0d640df58d78766c08c037a34a8b53c9d01ef0452d75b65eb52520e96b01e659"),
				CryptoCapabilities.argon2(params, fill(32, 0x01)));
	}

	/** RFC 7748, section 6.1: X25519 Diffie-Hellman. */
	@Test
	void x25519KnownAnswer()
	{
		X25519PrivateKeyParameters alice = new X25519PrivateKeyParameters(
				Hex.decode("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a"));
		X25519PublicKeyParameters bob = new X25519PublicKeyParameters(
				Hex.decode("de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f"));

		X25519Agreement agreement = new X25519Agreement();
		agreement.init(alice);
		byte[] shared = new byte[32];
		agreement.calculateAgreement(bob, shared, 0);
		assertArrayEquals(Hex.decode("4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742"), shared);
	}

	private static byte[] fill(int length, int value)
	{
		byte[] b = new byte[length];
		java.util.Arrays.fill(b, (byte) value);
		return b;
	}
}
