/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.crypto;

import java.util.Arrays;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * The authenticated ciphers a container can use for its data and key slots.
 *
 * <p>The numeric {@link #id()} is written to the container header and must
 * never change for an existing constant.
 */
public enum CipherSuite
{
	AES_256_GCM(1, "AES-256-GCM"),
	CHACHA20_POLY1305(2, "ChaCha20-Poly1305");

	public static final int KEY_LENGTH = 32;
	public static final int NONCE_LENGTH = 12;
	public static final int TAG_LENGTH = 16;

	private final int id;
	private final String displayName;

	CipherSuite(int id, String displayName)
	{
		this.id = id;
		this.displayName = displayName;
	}

	public int id()
	{
		return id;
	}

	public String displayName()
	{
		return displayName;
	}

	/** Looks up a suite by its header id; {@code null} if unknown. */
	public static CipherSuite byId(int id)
	{
		for (CipherSuite s : values())
			if (s.id == id)
				return s;
		return null;
	}

	/** A fresh, uninitialised cipher instance. Instances are not thread-safe. */
	public AEADCipher newCipher()
	{
		return switch (this)
		{
			case AES_256_GCM -> GCMBlockCipher.newInstance(AESEngine.newInstance());
			case CHACHA20_POLY1305 -> new ChaCha20Poly1305();
		};
	}

	/** Encrypts {@code plain} and appends the authentication tag. */
	public byte[] seal(byte[] key, byte[] nonce, byte[] aad, byte[] plain)
	{
		return seal(key, nonce, aad, plain, 0, plain.length);
	}

	/** Encrypts a range of {@code plain} and appends the authentication tag. */
	public byte[] seal(byte[] key, byte[] nonce, byte[] aad, byte[] plain, int off, int len)
	{
		try
		{
			return process(newCipher(), true, key, nonce, aad, plain, off, len);
		}
		catch (InvalidCipherTextException e)
		{
			throw new IllegalStateException("encryption cannot fail authentication", e);
		}
	}

	/**
	 * Verifies and decrypts {@code sealed} (ciphertext followed by tag).
	 *
	 * @throws InvalidCipherTextException if the key, nonce, AAD or data do not match
	 */
	public byte[] open(byte[] key, byte[] nonce, byte[] aad, byte[] sealed) throws InvalidCipherTextException
	{
		return open(key, nonce, aad, sealed, 0, sealed.length);
	}

	/** Verifies and decrypts a range of {@code sealed}. */
	public byte[] open(byte[] key, byte[] nonce, byte[] aad, byte[] sealed, int off, int len)
			throws InvalidCipherTextException
	{
		return process(newCipher(), false, key, nonce, aad, sealed, off, len);
	}

	static byte[] process(AEADCipher cipher, boolean encrypt, byte[] key, byte[] nonce, byte[] aad,
			byte[] in, int off, int len) throws InvalidCipherTextException
	{
		checkLength("key", key, KEY_LENGTH);
		checkLength("nonce", nonce, NONCE_LENGTH);
		cipher.init(encrypt, new AEADParameters(new KeyParameter(key), TAG_LENGTH * 8, nonce, aad));
		byte[] out = new byte[cipher.getOutputSize(len)];
		int n = cipher.processBytes(in, off, len, out, 0);
		n += cipher.doFinal(out, n);
		return n == out.length ? out : Arrays.copyOf(out, n);
	}

	private static void checkLength(String what, byte[] value, int expected)
	{
		if (value == null || value.length != expected)
			throw new IllegalArgumentException(what + " must be " + expected + " bytes");
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
