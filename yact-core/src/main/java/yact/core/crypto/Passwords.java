/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.crypto;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;

/** Turns a password into the exact bytes the key derivation sees. */
public final class Passwords
{
	private Passwords()
	{
	}

	/**
	 * Encodes a password as Unicode NFC, then UTF-8.
	 *
	 * <p>Normalising matters: "ü" can be typed as one code point or as "u" plus
	 * a combining diaeresis depending on the keyboard and operating system. Both
	 * must unlock the same container.
	 *
	 * <p>The caller owns and should wipe the returned array. Note that
	 * {@link Normalizer} works on {@code String}, so one intermediate copy of a
	 * non-ASCII password stays on the heap until garbage collection; this is a
	 * limitation of the JDK API.
	 *
	 * @throws IllegalArgumentException if the password contains unpaired surrogates
	 */
	public static byte[] encode(char[] password)
	{
		CharSequence normalized = isAscii(password)
				? CharBuffer.wrap(password)
				: Normalizer.normalize(CharBuffer.wrap(password), Normalizer.Form.NFC);
		try
		{
			ByteBuffer buf = StandardCharsets.UTF_8.newEncoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.encode(CharBuffer.wrap(normalized));
			byte[] out = new byte[buf.remaining()];
			buf.get(out);
			if (buf.hasArray())
				Arrays.fill(buf.array(), (byte) 0);
			return out;
		}
		catch (CharacterCodingException e)
		{
			throw new IllegalArgumentException("password is not valid Unicode", e);
		}
	}

	private static boolean isAscii(char[] chars)
	{
		for (char c : chars)
			if (c > 0x7F)
				return false;
		return true;
	}

	/** Overwrites the given arrays with zeros; {@code null} entries are ignored. */
	public static void wipe(byte[]... arrays)
	{
		for (byte[] a : arrays)
			if (a != null)
				Arrays.fill(a, (byte) 0);
	}
}
