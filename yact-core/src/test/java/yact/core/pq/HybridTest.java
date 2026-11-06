/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.pq;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class HybridTest
{
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final byte[] CONTEXT = "container-id-123".getBytes(StandardCharsets.US_ASCII);

	@ParameterizedTest
	@EnumSource(HybridKem.class)
	void roundTrip(HybridKem kem)
	{
		try (HybridKeyPair key = HybridKeyPair.generate(kem, RANDOM))
		{
			Hybrid.Encapsulation enc = Hybrid.encapsulate(key.publicKey(), CONTEXT, RANDOM);
			assertEquals(32, enc.key().length);
			assertEquals(kem.mlKemCiphertextLength(), enc.mlKemCiphertext().length);
			assertArrayEquals(enc.key(), Hybrid.decapsulate(key, enc.x25519Ephemeral(), enc.mlKemCiphertext(), CONTEXT));
		}
	}

	@Test
	void everyEncapsulationIsFresh()
	{
		HybridPublicKey pk = HybridKeyPair.generate(HybridKem.DEFAULT, RANDOM).publicKey();
		assertFalse(Arrays.equals(Hybrid.encapsulate(pk, CONTEXT, RANDOM).key(), Hybrid.encapsulate(pk, CONTEXT, RANDOM).key()));
	}

	@Test
	void otherKeyPairGetsADifferentKey()
	{
		HybridKeyPair alice = HybridKeyPair.generate(HybridKem.DEFAULT, RANDOM);
		HybridKeyPair mallory = HybridKeyPair.generate(HybridKem.DEFAULT, RANDOM);
		Hybrid.Encapsulation enc = Hybrid.encapsulate(alice.publicKey(), CONTEXT, RANDOM);
		assertFalse(Arrays.equals(enc.key(), Hybrid.decapsulate(mallory, enc.x25519Ephemeral(), enc.mlKemCiphertext(), CONTEXT)));
	}

	/** Changing either half, or the context, must change the key: both components are bound in. */
	@Test
	void everyInputIsBoundIntoTheKey()
	{
		HybridKeyPair key = HybridKeyPair.generate(HybridKem.DEFAULT, RANDOM);
		Hybrid.Encapsulation enc = Hybrid.encapsulate(key.publicKey(), CONTEXT, RANDOM);

		byte[] ct = enc.mlKemCiphertext().clone();
		ct[100] ^= 1;
		assertFalse(Arrays.equals(enc.key(), Hybrid.decapsulate(key, enc.x25519Ephemeral(), ct, CONTEXT)), "ML-KEM ciphertext");

		byte[] eph = enc.x25519Ephemeral().clone();
		eph[3] ^= 1;
		byte[] withOtherEph = Hybrid.decapsulate(key, eph, enc.mlKemCiphertext(), CONTEXT);
		assertTrue(withOtherEph == null || !Arrays.equals(enc.key(), withOtherEph), "X25519 ephemeral key");

		assertFalse(Arrays.equals(enc.key(),
				Hybrid.decapsulate(key, enc.x25519Ephemeral(), enc.mlKemCiphertext(), "other".getBytes())), "context");
	}

	/** The combiner must depend on each shared secret on its own: breaking one component is not enough. */
	@Test
	void keyDependsOnBothSharedSecrets()
	{
		HybridPublicKey pk = fixedKey().publicKey();
		byte[] ssM = new byte[32];
		byte[] ssX = new byte[32];
		byte[] ct = new byte[pk.kem().mlKemCiphertextLength()];
		byte[] eph = new byte[32];
		byte[] base = Hybrid.combine(pk, ssM, ssX, ct, eph, CONTEXT);

		byte[] otherM = ssM.clone();
		otherM[0] = 1;
		byte[] otherX = ssX.clone();
		otherX[0] = 1;
		assertFalse(Arrays.equals(base, Hybrid.combine(pk, otherM, ssX, ct, eph, CONTEXT)), "ML-KEM secret ignored");
		assertFalse(Arrays.equals(base, Hybrid.combine(pk, ssM, otherX, ct, eph, CONTEXT)), "X25519 secret ignored");
	}

	/**
	 * Freezes the combiner's output for fixed inputs. If this fails, the key
	 * derivation changed and existing containers could no longer be opened.
	 */
	@Test
	void combinerOutputIsStable()
	{
		HybridPublicKey pk = fixedKey().publicKey();
		byte[] ct = new byte[pk.kem().mlKemCiphertextLength()];
		Arrays.fill(ct, (byte) 0x33);
		byte[] eph = new byte[32];
		Arrays.fill(eph, (byte) 0x44);
		byte[] ssM = new byte[32];
		Arrays.fill(ssM, (byte) 0x11);
		byte[] ssX = new byte[32];
		Arrays.fill(ssX, (byte) 0x22);
		assertEquals(COMBINER_VECTOR, Hex.toHexString(Hybrid.combine(pk, ssM, ssX, ct, eph, CONTEXT)));
	}

	private static final String COMBINER_VECTOR = "ddf54c5627f06db0b87efb73738af843dddf0a18fa3715b1860903aca57c048f";

	/** Key pair from fixed seeds, so its public key is the same in every run. */
	private static HybridKeyPair fixedKey()
	{
		byte[] seed = new byte[HybridKem.MLKEM_SEED_LENGTH];
		Arrays.fill(seed, (byte) 0x55);
		byte[] x = new byte[HybridKem.X25519_KEY_LENGTH];
		Arrays.fill(x, (byte) 0x66);
		return new HybridKeyPair(HybridKem.MLKEM1024_X25519, seed, x);
	}

	@Test
	void lowOrderEphemeralKeyIsRejected()
	{
		HybridKeyPair key = HybridKeyPair.generate(HybridKem.DEFAULT, RANDOM);
		Hybrid.Encapsulation enc = Hybrid.encapsulate(key.publicKey(), CONTEXT, RANDOM);
		assertNull(Hybrid.decapsulate(key, new byte[32], enc.mlKemCiphertext(), CONTEXT));
	}

	@Test
	void wrongLengthsAreRejected()
	{
		HybridKeyPair key = HybridKeyPair.generate(HybridKem.MLKEM1024_X25519, RANDOM);
		assertThrows(IllegalArgumentException.class, () -> Hybrid.decapsulate(key, new byte[32], new byte[1088], CONTEXT));
	}

	@Test
	void destroyedKeyCannotBeUsed()
	{
		HybridKeyPair key = HybridKeyPair.generate(HybridKem.DEFAULT, RANDOM);
		Hybrid.Encapsulation enc = Hybrid.encapsulate(key.publicKey(), CONTEXT, RANDOM);
		key.close();
		assertTrue(key.isDestroyed());
		assertThrows(IllegalStateException.class,
				() -> Hybrid.decapsulate(key, enc.x25519Ephemeral(), enc.mlKemCiphertext(), CONTEXT));
	}

	// ------------------------------------------------------------------ public key validation

	@Test
	void lowOrderX25519PublicKeyIsRejected()
	{
		byte[] mlKem = HybridKeyPair.generate(HybridKem.DEFAULT, RANDOM).publicKey().mlKem();
		assertThrows(IllegalArgumentException.class, () -> new HybridPublicKey(HybridKem.DEFAULT, new byte[32], mlKem));
	}

	/** NIST ACVP encapsulationKeyCheck vectors: FIPS 203 requires rejecting keys with coefficients >= q. */
	@ParameterizedTest(name = "{0}")
	@MethodSource("ekCheckVectors")
	void mlKemModulusCheck(String name, boolean valid, byte[] ek)
	{
		assertEquals(valid, HybridPublicKey.isValidMlKemKey(ek));
	}

	static Stream<Object[]> ekCheckVectors() throws IOException
	{
		List<Map<String, String>> cases = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
				HybridTest.class.getResourceAsStream("/kat/ml-kem-ek-check.txt"), StandardCharsets.US_ASCII)))
		{
			Map<String, String> c = new LinkedHashMap<>();
			for (String line; (line = r.readLine()) != null;)
			{
				if (line.startsWith("#"))
					continue;
				if (line.isBlank())
				{
					if (!c.isEmpty())
						cases.add(c);
					c = new LinkedHashMap<>();
					continue;
				}
				int eq = line.indexOf(" = ");
				c.put(line.substring(0, eq), line.substring(eq + 3));
			}
			if (!c.isEmpty())
				cases.add(c);
		}
		assertEquals(20, cases.size());
		return cases.stream().map(c -> new Object[] {
				c.get("parameterSet") + " tcId " + c.get("tcId") + " (" + c.get("reason") + ")",
				Boolean.parseBoolean(c.get("valid")), Hex.decode(c.get("ek")) });
	}
}
