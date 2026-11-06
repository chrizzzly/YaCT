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
import yact.core.crypto.CipherSuite;
import yact.core.crypto.Passwords;
import yact.core.pq.Hybrid;
import yact.core.pq.HybridKem;
import yact.core.pq.HybridKeyPair;
import yact.core.pq.HybridPublicKey;

/**
 * Key slot unlocked by a private key, protected with hybrid post-quantum
 * key encapsulation (ML-KEM + X25519), see {@link Hybrid}.
 *
 * <p>Body layout:
 * <pre>
 * u8   scheme id, see {@link HybridKem#id()}
 * 32   ephemeral X25519 public key
 * n    ML-KEM ciphertext (1088 bytes for ML-KEM-768, 1568 for ML-KEM-1024)
 * 12   nonce
 * 48   master key wrapped with the container cipher (32 bytes + 16-byte tag)
 * </pre>
 *
 * <p>The wrapping key is the hybrid KEM output with the container id as
 * context. The slot deliberately does not name its recipient: a reader tries
 * every slot, which is cheap, and the container does not reveal whose keys can
 * open it.
 */
public final class HybridKeySlot implements KeySlot
{
	public static final int TYPE = 2;
	private static final int WRAPPED_LENGTH = ContainerHeader.MASTER_KEY_LENGTH + CipherSuite.TAG_LENGTH;
	private static final byte[] AAD_LABEL = "YaCT v1 hybrid slot".getBytes(StandardCharsets.US_ASCII);

	private final HybridKem kem;
	private final byte[] x25519Ephemeral;
	private final byte[] mlKemCiphertext;
	private final byte[] nonce;
	private final byte[] wrappedKey;

	private HybridKeySlot(HybridKem kem, byte[] x25519Ephemeral, byte[] mlKemCiphertext, byte[] nonce, byte[] wrappedKey)
	{
		this.kem = kem;
		this.x25519Ephemeral = x25519Ephemeral;
		this.mlKemCiphertext = mlKemCiphertext;
		this.nonce = nonce;
		this.wrappedKey = wrappedKey;
	}

	static HybridKeySlot create(byte[] masterKey, HybridPublicKey recipient, CipherSuite suite, byte[] containerId,
			SecureRandom random)
	{
		Hybrid.Encapsulation enc = Hybrid.encapsulate(recipient, containerId, random);
		byte[] nonce = new byte[CipherSuite.NONCE_LENGTH];
		random.nextBytes(nonce);
		try
		{
			byte[] aad = aad(containerId, recipient.kem(), enc.x25519Ephemeral(), enc.mlKemCiphertext());
			byte[] wrapped = suite.seal(enc.key(), nonce, aad, masterKey);
			return new HybridKeySlot(recipient.kem(), enc.x25519Ephemeral(), enc.mlKemCiphertext(), nonce, wrapped);
		}
		finally
		{
			Passwords.wipe(enc.key());
		}
	}

	/** @return the master key, or {@code null} if this slot is not for {@code key} or was modified */
	byte[] tryUnwrap(HybridKeyPair key, CipherSuite suite, byte[] containerId)
	{
		if (key.kem() != kem)
			return null;
		byte[] kek = Hybrid.decapsulate(key, x25519Ephemeral, mlKemCiphertext, containerId);
		if (kek == null)
			return null;
		try
		{
			return suite.open(kek, nonce, aad(containerId, kem, x25519Ephemeral, mlKemCiphertext), wrappedKey);
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

	public HybridKem kem()
	{
		return kem;
	}

	@Override
	public int type()
	{
		return TYPE;
	}

	@Override
	public byte[] encodeBody()
	{
		return new ByteWriter().u8(kem.id()).bytes(x25519Ephemeral).bytes(mlKemCiphertext).bytes(nonce)
				.bytes(wrappedKey).toByteArray();
	}

	static HybridKeySlot decode(byte[] body) throws FormatException
	{
		ByteReader r = new ByteReader(body);
		int kemId = r.u8();
		HybridKem kem = HybridKem.byId(kemId);
		if (kem == null)
			throw new FormatException("unsupported key scheme " + kemId);
		byte[] eph = r.bytes(HybridKem.X25519_KEY_LENGTH);
		byte[] ct = r.bytes(kem.mlKemCiphertextLength());
		byte[] nonce = r.bytes(CipherSuite.NONCE_LENGTH);
		byte[] wrapped = r.bytes(WRAPPED_LENGTH);
		r.expectEnd();
		return new HybridKeySlot(kem, eph, ct, nonce, wrapped);
	}

	private static byte[] aad(byte[] containerId, HybridKem kem, byte[] eph, byte[] ct)
	{
		return new ByteWriter().bytes(AAD_LABEL).bytes(containerId).u8(kem.id()).bytes(eph).bytes(ct).toByteArray();
	}
}
