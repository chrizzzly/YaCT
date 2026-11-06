/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.format;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Destroyable;

import yact.core.crypto.Argon2Params;
import yact.core.crypto.CipherSuite;
import yact.core.crypto.Kdf;
import yact.core.crypto.Passwords;

/**
 * A container header together with its master key.
 *
 * <p>This is the only place the master key lives. Key slot changes produce a
 * new, re-authenticated {@link ContainerHeader}; the master key and therefore
 * the encrypted data stay the same. Call {@link #close()} as soon as the
 * container is no longer in use, which overwrites the key.
 *
 * <p>Not thread-safe.
 */
public final class UnlockedHeader implements AutoCloseable, Destroyable
{
	private static final SecureRandom RANDOM = new SecureRandom();

	private ContainerHeader header;
	private final byte[] masterKey;
	private boolean destroyed;

	UnlockedHeader(ContainerHeader header, byte[] masterKey)
	{
		this.header = header;
		this.masterKey = masterKey;
	}

	/**
	 * Creates the header of a new container with a random master key and one password slot.
	 *
	 * @param chunkExponent chunk size as a power of two, see {@link ContainerHeader#DEFAULT_CHUNK_EXPONENT}
	 */
	public static UnlockedHeader create(CipherSuite suite, int chunkExponent, char[] password, Argon2Params params)
	{
		byte[] masterKey = new byte[ContainerHeader.MASTER_KEY_LENGTH];
		byte[] containerId = new byte[ContainerHeader.CONTAINER_ID_LENGTH];
		RANDOM.nextBytes(masterKey);
		RANDOM.nextBytes(containerId);

		KeySlot slot = PasswordKeySlot.create(masterKey, password, params, suite, containerId, RANDOM);
		ContainerHeader header = ContainerHeader.sign(suite, chunkExponent, containerId, List.of(slot), masterKey);
		return new UnlockedHeader(header, masterKey);
	}

	/** The current header; write {@link ContainerHeader#encode()} to persist changes. */
	public ContainerHeader header()
	{
		checkAlive();
		return header;
	}

	/** Adds a password slot, e.g. a second password or the first step of a password change. */
	public void addPasswordSlot(char[] password, Argon2Params params)
	{
		checkAlive();
		List<KeySlot> slots = new ArrayList<>(header.keySlots());
		if (slots.size() >= ContainerHeader.MAX_KEY_SLOTS)
			throw new IllegalStateException("all " + ContainerHeader.MAX_KEY_SLOTS + " key slots are in use");
		slots.add(PasswordKeySlot.create(masterKey, password, params, header.cipherSuite(), header.containerId(), RANDOM));
		resign(slots);
	}

	/**
	 * Removes a key slot. The last slot cannot be removed, since the container
	 * could never be opened again.
	 *
	 * <p>Removing a slot does not re-encrypt the data. Anyone who copied the
	 * container while the slot existed can still open that copy.
	 */
	public void removeKeySlot(int index)
	{
		checkAlive();
		List<KeySlot> slots = new ArrayList<>(header.keySlots());
		if (slots.size() == 1)
			throw new IllegalStateException("cannot remove the last key slot");
		slots.remove(index);
		resign(slots);
	}

	/**
	 * Derives the key for one encrypted data stream.
	 *
	 * <p>Every stream must use a fresh random {@code streamSalt} (at least 16
	 * bytes), stored next to the stream. Reusing a salt for different data would
	 * reuse nonces, which breaks AES-GCM and ChaCha20-Poly1305.
	 */
	public byte[] streamKey(byte[] streamSalt)
	{
		checkAlive();
		if (streamSalt.length < 16)
			throw new IllegalArgumentException("stream salt must be at least 16 bytes");
		return Kdf.hkdf(masterKey, header.containerId(), "YaCT v1 stream key", streamSalt);
	}

	private void resign(List<KeySlot> slots)
	{
		header = ContainerHeader.sign(header.cipherSuite(), header.chunkExponent(), header.containerId(), slots, masterKey);
	}

	private void checkAlive()
	{
		if (destroyed)
			throw new IllegalStateException("container is closed");
	}

	@Override
	public void destroy()
	{
		Passwords.wipe(masterKey);
		destroyed = true;
	}

	@Override
	public boolean isDestroyed()
	{
		return destroyed;
	}

	/** Same as {@link #destroy()}. */
	@Override
	public void close()
	{
		destroy();
	}
}
