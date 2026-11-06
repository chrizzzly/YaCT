/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.pq;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import yact.core.codec.FormatException;
import yact.core.crypto.Argon2Params;
import yact.core.format.InvalidCredentialsException;

class KeyFilesTest
{
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Argon2Params FAST = new Argon2Params(64, 1, 1);
	private static final char[] PASSWORD = "key file password".toCharArray();

	@ParameterizedTest
	@EnumSource(HybridKem.class)
	void privateKeyRoundTrip(HybridKem kem) throws Exception
	{
		HybridKeyPair key = HybridKeyPair.generate(kem, RANDOM);
		byte[] file = KeyFiles.encryptPrivateKey(key, PASSWORD, FAST, RANDOM);
		assertEquals(4 + 2 + 1 + 9 + 16 + 12 + 96 + 16, file.length);

		try (HybridKeyPair loaded = KeyFiles.decryptPrivateKey(file, PASSWORD))
		{
			assertEquals(key.publicKey(), loaded.publicKey());
			Hybrid.Encapsulation enc = Hybrid.encapsulate(key.publicKey(), new byte[16], RANDOM);
			assertArrayEquals(enc.key(), Hybrid.decapsulate(loaded, enc.x25519Ephemeral(), enc.mlKemCiphertext(), new byte[16]));
		}
	}

	@Test
	void wrongPasswordIsRejected()
	{
		byte[] file = KeyFiles.encryptPrivateKey(HybridKeyPair.generate(HybridKem.DEFAULT, RANDOM), PASSWORD, FAST, RANDOM);
		assertThrows(InvalidCredentialsException.class, () -> KeyFiles.decryptPrivateKey(file, "wrong".toCharArray()));
	}

	@Test
	void everyByteIsProtected()
	{
		byte[] file = KeyFiles.encryptPrivateKey(HybridKeyPair.generate(HybridKem.DEFAULT, RANDOM), PASSWORD, FAST, RANDOM);
		// Bytes 0..15 are magic, version, scheme and Argon2 parameters: changing them is a format error
		// or yields another key. From the salt on, every change must be caught by the AEAD.
		for (int i = 16; i < file.length; i++)
		{
			byte[] bad = file.clone();
			bad[i] ^= 1;
			assertThrows(InvalidCredentialsException.class, () -> KeyFiles.decryptPrivateKey(bad, PASSWORD), "byte " + i);
		}
	}

	@Test
	void malformedFilesAreFormatErrors()
	{
		byte[] file = KeyFiles.encryptPrivateKey(HybridKeyPair.generate(HybridKem.DEFAULT, RANDOM), PASSWORD, FAST, RANDOM);
		assertThrows(FormatException.class, () -> KeyFiles.decryptPrivateKey(new byte[10], PASSWORD));
		assertThrows(FormatException.class, () -> KeyFiles.decryptPrivateKey(Arrays.copyOf(file, file.length - 1), PASSWORD));
		assertThrows(FormatException.class, () -> KeyFiles.decryptPrivateKey(Arrays.copyOf(file, file.length + 1), PASSWORD));
		byte[] scheme = file.clone();
		scheme[6] = 9;
		assertThrows(FormatException.class, () -> KeyFiles.decryptPrivateKey(scheme, PASSWORD));
	}

	@ParameterizedTest
	@EnumSource(HybridKem.class)
	void publicKeyEncodings(HybridKem kem) throws Exception
	{
		HybridPublicKey pk = HybridKeyPair.generate(kem, RANDOM).publicKey();
		assertEquals(pk, HybridPublicKey.decode(pk.encode()));

		String armored = pk.toArmored();
		assertTrue(armored.startsWith("-----BEGIN YACT PUBLIC KEY-----\n"));
		assertTrue(armored.lines().allMatch(l -> l.length() <= 64 || l.startsWith("-----")));
		assertEquals(pk, HybridPublicKey.fromArmored("Hi, here is my key:\n\n" + armored + "\nCheers"));
	}

	@Test
	void fingerprintIsStableAndDistinct()
	{
		HybridPublicKey a = HybridKeyPair.generate(HybridKem.DEFAULT, RANDOM).publicKey();
		HybridPublicKey b = HybridKeyPair.generate(HybridKem.DEFAULT, RANDOM).publicKey();
		assertTrue(a.fingerprint().matches("([0-9A-F]{4} ){9}[0-9A-F]{4}"), a.fingerprint());
		assertEquals(a.fingerprint(), a.fingerprint());
		assertNotEquals(a.fingerprint(), b.fingerprint());
	}

	@Test
	void damagedPublicKeysAreRejected()
	{
		byte[] enc = HybridKeyPair.generate(HybridKem.DEFAULT, RANDOM).publicKey().encode();
		assertThrows(FormatException.class, () -> HybridPublicKey.decode(Arrays.copyOf(enc, enc.length - 1)));
		byte[] badCoefficient = enc.clone();
		badCoefficient[4 + 2 + 1 + 32] = (byte) 0xFF; // first ML-KEM coefficient
		badCoefficient[4 + 2 + 1 + 32 + 1] |= 0x0F;   // -> 0xFFF >= q
		assertThrows(FormatException.class, () -> HybridPublicKey.decode(badCoefficient));
		assertThrows(FormatException.class, () -> HybridPublicKey.fromArmored("no key here"));
		assertThrows(FormatException.class,
				() -> HybridPublicKey.fromArmored("-----BEGIN YACT PUBLIC KEY-----\n!!!\n-----END YACT PUBLIC KEY-----"));
	}
}
