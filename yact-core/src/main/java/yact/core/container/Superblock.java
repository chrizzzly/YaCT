/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.container;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.bouncycastle.crypto.InvalidCipherTextException;

import yact.core.codec.ByteReader;
import yact.core.codec.ByteWriter;
import yact.core.codec.FormatException;
import yact.core.crypto.CipherSuite;
import yact.core.crypto.Passwords;
import yact.core.format.UnlockedHeader;

/**
 * Points to the current index. There are two superblock slots, written
 * alternately; the valid one with the higher generation is current. A crash
 * while writing one slot therefore always leaves the previous state intact.
 *
 * <pre>
 * 16   salt
 * 56   AEAD(u64 generation, 16 index salt, u64 index offset, u64 index length) + tag
 * 56   zero padding
 * </pre>
 *
 * <p>Key = stream key for the salt; the salt is fresh for every write, so the
 * all-zero nonce is never reused under one key.
 */
record Superblock(long generation, byte[] indexSalt, long indexOffset, long indexLength)
{
	static final int SIZE = 128;
	static final int SALT_LENGTH = 16;
	private static final int PLAIN_LENGTH = 8 + SALT_LENGTH + 8 + 8;
	private static final byte[] AAD = "YaCT v1 superblock".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] ZERO_NONCE = new byte[CipherSuite.NONCE_LENGTH];

	/** Slot this generation is written to. */
	int slot()
	{
		return (int) (generation & 1);
	}

	byte[] encode(UnlockedHeader keys, SecureRandom random)
	{
		byte[] salt = new byte[SALT_LENGTH];
		random.nextBytes(salt);
		byte[] plain = new ByteWriter().u63(generation).bytes(indexSalt).u63(indexOffset).u63(indexLength).toByteArray();
		byte[] key = keys.streamKey(salt);
		try
		{
			byte[] sealed = keys.header().cipherSuite().seal(key, ZERO_NONCE, AAD, plain);
			byte[] out = new byte[SIZE];
			System.arraycopy(salt, 0, out, 0, SALT_LENGTH);
			System.arraycopy(sealed, 0, out, SALT_LENGTH, sealed.length);
			return out;
		}
		finally
		{
			Passwords.wipe(key);
		}
	}

	/** @return the superblock, or {@code null} if the slot is empty, torn or not authentic */
	static Superblock decode(byte[] slot, UnlockedHeader keys)
	{
		byte[] salt = java.util.Arrays.copyOf(slot, SALT_LENGTH);
		byte[] key = keys.streamKey(salt);
		try
		{
			byte[] plain = keys.header().cipherSuite().open(key, ZERO_NONCE, AAD, slot, SALT_LENGTH,
					PLAIN_LENGTH + CipherSuite.TAG_LENGTH);
			ByteReader r = new ByteReader(plain);
			Superblock sb = new Superblock(r.u63(), r.bytes(SALT_LENGTH), r.u63(), r.u63());
			r.expectEnd();
			return sb;
		}
		catch (InvalidCipherTextException | FormatException e)
		{
			return null;
		}
		finally
		{
			Passwords.wipe(key);
		}
	}
}
