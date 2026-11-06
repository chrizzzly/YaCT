/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.format;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import yact.core.codec.ByteReader;
import yact.core.codec.ByteWriter;
import yact.core.codec.FormatException;
import yact.core.crypto.CipherSuite;
import yact.core.crypto.Kdf;
import yact.core.crypto.Passwords;
import yact.core.pq.HybridKeyPair;

/**
 * The header at the start of every container file.
 *
 * <p>The header occupies a fixed area of {@link #AREA_SIZE} bytes, so that
 * adding or removing key slots never moves the encrypted data behind it.
 * Unused space is zero-filled.
 *
 * <p>Layout (big-endian):
 * <pre>
 * 4    magic "YACT"
 * u16  format version (1)
 * u8   cipher suite id
 * u8   chunk size exponent (chunk size = 2^n bytes)
 * 16   container id (random)
 * u8   number of key slots (1..16)
 *      per slot: u8 type, u16 body length, body
 * 32   HMAC-SHA3-256 over all preceding header bytes
 * </pre>
 *
 * <p>The MAC key is derived from the master key, so the header can only be
 * verified after a key slot has been opened. Until then, nothing in the
 * header is trusted beyond the hard limits checked while parsing.
 *
 * <p>Instances are immutable. Changes go through {@link UnlockedHeader}.
 */
public final class ContainerHeader
{
	public static final int AREA_SIZE = 64 * 1024;
	public static final int VERSION = 1;
	public static final int MAX_KEY_SLOTS = 16;
	public static final int MIN_CHUNK_EXPONENT = 12; // 4 KiB
	public static final int MAX_CHUNK_EXPONENT = 24; // 16 MiB
	public static final int DEFAULT_CHUNK_EXPONENT = 16; // 64 KiB

	static final int MASTER_KEY_LENGTH = 32;
	static final int CONTAINER_ID_LENGTH = 16;
	static final int MAC_LENGTH = 32;
	private static final byte[] MAGIC = "YACT".getBytes(StandardCharsets.US_ASCII);

	private final CipherSuite cipherSuite;
	private final int chunkExponent;
	private final byte[] containerId;
	private final List<KeySlot> keySlots;
	private final byte[] mac;

	private ContainerHeader(CipherSuite cipherSuite, int chunkExponent, byte[] containerId, List<KeySlot> keySlots,
			byte[] mac)
	{
		this.cipherSuite = cipherSuite;
		this.chunkExponent = chunkExponent;
		this.containerId = containerId;
		this.keySlots = List.copyOf(keySlots);
		this.mac = mac;
	}

	/** Builds a header and authenticates it with a key derived from {@code masterKey}. */
	static ContainerHeader sign(CipherSuite cipherSuite, int chunkExponent, byte[] containerId, List<KeySlot> keySlots,
			byte[] masterKey)
	{
		if (keySlots.isEmpty() || keySlots.size() > MAX_KEY_SLOTS)
			throw new IllegalArgumentException("a container needs 1.." + MAX_KEY_SLOTS + " key slots");
		if (chunkExponent < MIN_CHUNK_EXPONENT || chunkExponent > MAX_CHUNK_EXPONENT)
			throw new IllegalArgumentException("chunk exponent out of range");

		byte[] body = encodeBody(cipherSuite, chunkExponent, containerId, keySlots);
		if (body.length + MAC_LENGTH > AREA_SIZE)
			throw new IllegalArgumentException("key slots do not fit into the header area");
		byte[] mac = computeMac(masterKey, containerId, body);
		return new ContainerHeader(cipherSuite, chunkExponent, containerId, keySlots, mac);
	}

	// ------------------------------------------------------------------ reading

	/** Reads exactly {@link #AREA_SIZE} bytes and parses them. Nothing is verified yet. */
	public static ContainerHeader read(InputStream in) throws IOException
	{
		byte[] area = in.readNBytes(AREA_SIZE);
		if (area.length != AREA_SIZE)
			throw new ContainerFormatException("file too short for a YaCT container");
		return parse(area);
	}

	/** Parses a header area. Nothing is verified yet. */
	public static ContainerHeader parse(byte[] area) throws ContainerFormatException
	{
		if (area.length != AREA_SIZE)
			throw new ContainerFormatException("header area must be " + AREA_SIZE + " bytes");
		try
		{
			return parseArea(area);
		}
		catch (ContainerFormatException e)
		{
			throw e;
		}
		catch (FormatException e)
		{
			throw new ContainerFormatException(e.getMessage());
		}
	}

	private static ContainerHeader parseArea(byte[] area) throws FormatException
	{

		ByteReader r = new ByteReader(area);
		if (!Arrays.equals(r.bytes(MAGIC.length), MAGIC))
			throw new ContainerFormatException("not a YaCT container");
		int version = r.u16();
		if (version != VERSION)
			throw new ContainerFormatException("unsupported container version " + version);

		int suiteId = r.u8();
		CipherSuite suite = CipherSuite.byId(suiteId);
		if (suite == null)
			throw new ContainerFormatException("unsupported cipher suite " + suiteId);

		int chunkExponent = r.u8();
		if (chunkExponent < MIN_CHUNK_EXPONENT || chunkExponent > MAX_CHUNK_EXPONENT)
			throw new ContainerFormatException("invalid chunk size");

		byte[] containerId = r.bytes(CONTAINER_ID_LENGTH);

		int slotCount = r.u8();
		if (slotCount < 1 || slotCount > MAX_KEY_SLOTS)
			throw new ContainerFormatException("invalid number of key slots");
		List<KeySlot> slots = new ArrayList<>(slotCount);
		for (int i = 0; i < slotCount; i++)
		{
			int type = r.u8();
			byte[] slotBody = r.bytes(r.u16());
			slots.add(switch (type)
			{
				case PasswordKeySlot.TYPE -> PasswordKeySlot.decode(slotBody);
				case HybridKeySlot.TYPE -> HybridKeySlot.decode(slotBody);
				default -> new UnknownKeySlot(type, slotBody);
			});
		}

		byte[] mac = r.bytes(MAC_LENGTH);
		return new ContainerHeader(suite, chunkExponent, containerId, slots, mac);
	}

	// ------------------------------------------------------------------ unlocking

	/**
	 * Opens the container with a password.
	 *
	 * <p>Every password slot is tried in turn; each attempt costs one Argon2id run.
	 *
	 * @throws InvalidCredentialsException if no slot accepts the password
	 * @throws IntegrityException          if a slot opened but the header was modified
	 */
	public UnlockedHeader unlock(char[] password) throws InvalidCredentialsException, IntegrityException
	{
		for (KeySlot slot : keySlots)
			if (slot instanceof PasswordKeySlot ps)
			{
				byte[] masterKey = ps.tryUnwrap(password, cipherSuite, containerId);
				if (masterKey != null)
					return verified(masterKey);
			}
		throw new InvalidCredentialsException();
	}

	/**
	 * Opens the container with a private key.
	 *
	 * <p>Slots do not say which key they belong to, so every slot of the key's
	 * scheme is tried; each attempt is one ML-KEM decapsulation and one X25519
	 * operation, well under a millisecond.
	 *
	 * @throws InvalidCredentialsException if no slot belongs to this key
	 * @throws IntegrityException          if a slot opened but the header was modified
	 */
	public UnlockedHeader unlock(HybridKeyPair key) throws InvalidCredentialsException, IntegrityException
	{
		for (KeySlot slot : keySlots)
			if (slot instanceof HybridKeySlot hs)
			{
				byte[] masterKey = hs.tryUnwrap(key, cipherSuite, containerId);
				if (masterKey != null)
					return verified(masterKey);
			}
		throw new InvalidCredentialsException();
	}

	private UnlockedHeader verified(byte[] masterKey) throws IntegrityException
	{
		if (!verifyMac(masterKey))
		{
			Passwords.wipe(masterKey);
			throw new IntegrityException("container header was modified");
		}
		return new UnlockedHeader(this, masterKey);
	}

	boolean verifyMac(byte[] masterKey)
	{
		byte[] expected = computeMac(masterKey, containerId, encodeBody(cipherSuite, chunkExponent, containerId, keySlots));
		return org.bouncycastle.util.Arrays.constantTimeAreEqual(expected, mac);
	}

	// ------------------------------------------------------------------ writing

	/** The complete header area, ready to be written at offset 0. */
	public byte[] encode()
	{
		byte[] body = encodeBody(cipherSuite, chunkExponent, containerId, keySlots);
		byte[] area = new byte[AREA_SIZE];
		System.arraycopy(body, 0, area, 0, body.length);
		System.arraycopy(mac, 0, area, body.length, MAC_LENGTH);
		return area;
	}

	private static byte[] encodeBody(CipherSuite suite, int chunkExponent, byte[] containerId, List<KeySlot> slots)
	{
		ByteWriter w = new ByteWriter()
				.bytes(MAGIC)
				.u16(VERSION)
				.u8(suite.id())
				.u8(chunkExponent)
				.bytes(containerId)
				.u8(slots.size());
		for (KeySlot slot : slots)
		{
			byte[] b = slot.encodeBody();
			w.u8(slot.type()).u16(b.length).bytes(b);
		}
		return w.toByteArray();
	}

	private static byte[] computeMac(byte[] masterKey, byte[] containerId, byte[] body)
	{
		byte[] macKey = Kdf.hkdf(masterKey, containerId, "YaCT v1 header mac");
		try
		{
			return Kdf.hmac(macKey, body, 0, body.length);
		}
		finally
		{
			Passwords.wipe(macKey);
		}
	}

	// ------------------------------------------------------------------ accessors

	public CipherSuite cipherSuite()
	{
		return cipherSuite;
	}

	public int chunkSize()
	{
		return 1 << chunkExponent;
	}

	int chunkExponent()
	{
		return chunkExponent;
	}

	public byte[] containerId()
	{
		return containerId.clone();
	}

	public List<KeySlot> keySlots()
	{
		return keySlots;
	}
}
