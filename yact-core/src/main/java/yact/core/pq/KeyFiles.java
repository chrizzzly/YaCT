/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.pq;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.InvalidCipherTextException;

import yact.core.codec.ByteReader;
import yact.core.codec.ByteWriter;
import yact.core.codec.FormatException;
import yact.core.crypto.Argon2Params;
import yact.core.crypto.CipherSuite;
import yact.core.crypto.Kdf;
import yact.core.crypto.Passwords;
import yact.core.format.InvalidCredentialsException;

/**
 * Password-protected storage of a private key.
 *
 * <p>Layout (big-endian):
 * <pre>
 * 4    magic "YASK"
 * u16  version (1)
 * u8   scheme id, see {@link HybridKem#id()}
 * u32  Argon2id memory (KiB)
 * u32  Argon2id iterations
 * u8   Argon2id parallelism
 * 16   salt
 * 12   nonce
 * 112  AES-256-GCM(ML-KEM seed ‖ X25519 scalar), 96 bytes + 16-byte tag
 * </pre>
 *
 * <p>Key = Argon2id(password, salt). Associated data = all bytes before the
 * ciphertext, so no header field can be changed unnoticed.
 */
public final class KeyFiles
{
	private static final byte[] MAGIC = "YASK".getBytes(StandardCharsets.US_ASCII);
	private static final int VERSION = 1;
	private static final int SALT_LENGTH = 16;
	private static final int PRIVATE_LENGTH = HybridKem.MLKEM_SEED_LENGTH + HybridKem.X25519_KEY_LENGTH;
	private static final CipherSuite SUITE = CipherSuite.AES_256_GCM;

	private KeyFiles()
	{
	}

	/** Encrypts the private key under {@code password}. */
	public static byte[] encryptPrivateKey(HybridKeyPair key, char[] password, Argon2Params params, SecureRandom random)
	{
		byte[] salt = new byte[SALT_LENGTH];
		byte[] nonce = new byte[CipherSuite.NONCE_LENGTH];
		random.nextBytes(salt);
		random.nextBytes(nonce);

		byte[] head = new ByteWriter()
				.bytes(MAGIC).u16(VERSION).u8(key.kem().id())
				.u31(params.memoryKiB()).u31(params.iterations()).u8(params.parallelism())
				.bytes(salt).bytes(nonce)
				.toByteArray();

		byte[] kek = deriveKey(password, salt, params);
		byte[] secret = key.privateBytes();
		try
		{
			return new ByteWriter().bytes(head).bytes(SUITE.seal(kek, nonce, head, secret)).toByteArray();
		}
		finally
		{
			Passwords.wipe(kek, secret);
		}
	}

	/**
	 * Decrypts a private key file.
	 *
	 * @throws FormatException             if the data is not a valid key file
	 * @throws InvalidCredentialsException if the password is wrong or the file was modified
	 */
	public static HybridKeyPair decryptPrivateKey(byte[] file, char[] password)
			throws FormatException, InvalidCredentialsException
	{
		ByteReader r = new ByteReader(file);
		if (!Arrays.equals(r.bytes(MAGIC.length), MAGIC))
			throw new FormatException("not a YaCT private key file");
		int version = r.u16();
		if (version != VERSION)
			throw new FormatException("unsupported key file version " + version);
		int kemId = r.u8();
		HybridKem kem = HybridKem.byId(kemId);
		if (kem == null)
			throw new FormatException("unsupported key scheme " + kemId);
		Argon2Params params;
		try
		{
			params = new Argon2Params(r.u31(), r.u31(), r.u8());
		}
		catch (IllegalArgumentException e)
		{
			throw new FormatException("invalid Argon2 parameters: " + e.getMessage());
		}
		byte[] salt = r.bytes(SALT_LENGTH);
		byte[] nonce = r.bytes(CipherSuite.NONCE_LENGTH);
		int headLength = r.position();
		byte[] sealed = r.bytes(PRIVATE_LENGTH + CipherSuite.TAG_LENGTH);
		r.expectEnd();

		byte[] kek = deriveKey(password, salt, params);
		byte[] secret = null;
		try
		{
			secret = SUITE.open(kek, nonce, Arrays.copyOf(file, headLength), sealed);
			return HybridKeyPair.fromPrivateBytes(kem, secret);
		}
		catch (InvalidCipherTextException e)
		{
			throw new InvalidCredentialsException();
		}
		finally
		{
			Passwords.wipe(kek, secret);
		}
	}

	private static byte[] deriveKey(char[] password, byte[] salt, Argon2Params params)
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
}
