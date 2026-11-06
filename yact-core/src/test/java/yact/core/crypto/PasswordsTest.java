/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class PasswordsTest
{
	@Test
	void asciiIsEncodedAsIs()
	{
		assertArrayEquals("Secret-123".getBytes(StandardCharsets.US_ASCII), Passwords.encode("Secret-123".toCharArray()));
	}

	@Test
	void composedAndDecomposedFormsGiveTheSameBytes()
	{
		byte[] composed = Passwords.encode("Grüße".toCharArray());       // ü as one code point
		byte[] decomposed = Passwords.encode("Grüße".toCharArray());   // u + combining diaeresis
		assertArrayEquals(composed, decomposed);
		assertArrayEquals("Grüße".getBytes(StandardCharsets.UTF_8), composed);
	}

	@Test
	void unpairedSurrogateIsRejected()
	{
		assertThrows(IllegalArgumentException.class, () -> Passwords.encode(new char[] { 'a', '\ud800' }));
	}

	@Test
	void wipeZeroesAndIgnoresNull()
	{
		byte[] a = { 1, 2, 3 };
		Passwords.wipe(a, null);
		assertArrayEquals(new byte[3], a);
	}

	@Test
	void argon2ParamLimits()
	{
		assertThrows(IllegalArgumentException.class, () -> new Argon2Params(1024, 0, 1));
		assertThrows(IllegalArgumentException.class, () -> new Argon2Params(1024, 1, 0));
		assertThrows(IllegalArgumentException.class, () -> new Argon2Params(31, 1, 4));   // < 8 KiB per lane
		assertThrows(IllegalArgumentException.class, () -> new Argon2Params(Argon2Params.MAX_MEMORY_KIB + 1, 1, 1));
		assertTrue(new Argon2Params(64, 1, 1).isBelowRecommended());
		assertTrue(!Argon2Params.DEFAULT.isBelowRecommended() && !Argon2Params.STRONG.isBelowRecommended());
	}
}
