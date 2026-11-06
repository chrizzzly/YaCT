/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.format;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.bouncycastle.crypto.InvalidCipherTextException;

import yact.core.codec.ByteReader;
import yact.core.codec.ByteWriter;
import yact.core.codec.FormatException;
import yact.core.crypto.Argon2Params;
import yact.core.crypto.CipherSuite;
import yact.core.crypto.Kdf;
import yact.core.crypto.Passwords;

/**
 * Key slot unlocked by a password.
 *
 * <p>Body layout (big-endian):
 * <pre>
 * u32  Argon2 memory (KiB)
 * u32  Argon2 iterations
 * u8   Argon2 parallelism
 * 16   salt
 * 12   nonce
 * 48   master key wrapped with the container cipher (32 bytes + 16-byte tag)
 * </pre>
 *
 * <p>The wrapping key is {@code Argon2id(password, salt)}. The associated
 * data binds the wrapped key to the container id and to the slot's own
 * parameters, so a slot cannot be copied into another container.
 */
public final class PasswordKeySlot implements KeySlot
{
	public static final int TYPE = 1;
	static final int SALT_LENGTH = 16;
	private static final int WRAPPED_LENGTH = ContainerHeader.MASTER_KEY_LENGTH + CipherSuite.TAG_LENGTH;
	private static final byte[] AAD_LABEL = "YaCT v1 password slot".getBytes(StandardCharsets.US_ASCII);

	private final Argon2Params params;
	private final byte[] salt;
	private final byte[] nonce;
	private final byte[] wrappedKey;

	private PasswordKeySlot(Argon2Params params, byte[] salt, byte[] nonce, byte[] wrappedKey)
	{
		this.params = params;
		this.salt = salt;
		this.nonce = nonce;
		this.wrappedKey = wrappedKey;
	}

	/** Wraps {@code masterKey} under {@code password}. */
	static PasswordKeySlot create(byte[] masterKey, char[] password, Argon2Params params, CipherSuite suite,
			byte[] containerId, SecureRandom random)
	{
		byte[] salt = new byte[SALT_LENGTH];
		byte[] nonce = new byte[CipherSuite.NONCE_LENGTH];
		random.nextBytes(salt);
		random.nextBytes(nonce);

		byte[] kek = deriveKek(password, salt, params);
		try
		{
			byte[] wrapped = suite.seal(kek, nonce, aad(containerId, params, salt), masterKey);
			return new PasswordKeySlot(params, salt, nonce, wrapped);
		}
		finally
		{
			Passwords.wipe(kek);
		}
	}

	/**
	 * Tries to recover the master key.
	 *
	 * @return the master key, or {@code null} if the password is wrong or the slot was modified
	 */
	byte[] tryUnwrap(char[] password, CipherSuite suite, byte[] containerId)
	{
		byte[] kek = deriveKek(password, salt, params);
		try
		{
			return suite.open(kek, nonce, aad(containerId, params, salt), wrappedKey);
		}
		catch (InvalidCipherTextException e)
		{
			return null;
		}
		finally
		{
			Passwords.wipe(kek);
		}
	}

	public Argon2Params params()
	{
		return params;
	}

	@Override
	public int type()
	{
		return TYPE;
	}

	@Override
	public byte[] encodeBody()
	{
		return encodeParams(params, salt).bytes(nonce).bytes(wrappedKey).toByteArray();
	}

	static PasswordKeySlot decode(byte[] body) throws FormatException
	{
		ByteReader r = new ByteReader(body);
		Argon2Params params;
		try
		{
			params = new Argon2Params(r.u31(), r.u31(), r.u8());
		}
		catch (IllegalArgumentException e)
		{
			throw new ContainerFormatException("invalid Argon2 parameters: " + e.getMessage());
		}
		byte[] salt = r.bytes(SALT_LENGTH);
		byte[] nonce = r.bytes(CipherSuite.NONCE_LENGTH);
		byte[] wrapped = r.bytes(WRAPPED_LENGTH);
		r.expectEnd();
		return new PasswordKeySlot(params, salt, nonce, wrapped);
	}

	private static byte[] deriveKek(char[] password, byte[] salt, Argon2Params params)
	{
		byte[] pw = Passwords.encode(password);
		try
		{
			return Kdf.argon2id(pw, salt, params);
		}
		finally
		{
			Passwords.wipe(pw);
		}
	}

	private static byte[] aad(byte[] containerId, Argon2Params params, byte[] salt)
	{
		return new ByteWriter().bytes(AAD_LABEL).bytes(containerId).bytes(encodeParams(params, salt).toByteArray())
				.toByteArray();
	}

	private static ByteWriter encodeParams(Argon2Params params, byte[] salt)
	{
		return new ByteWriter()
				.u31(params.memoryKiB())
				.u31(params.iterations())
				.u8(params.parallelism())
				.bytes(salt);
	}
}
