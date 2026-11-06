/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.pq;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.params.MLKEMPublicKeyParameters;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;

import yact.core.codec.ByteReader;
import yact.core.codec.ByteWriter;
import yact.core.codec.FormatException;

/**
 * The public half of a {@link HybridKeyPair}. Anyone who has it can add a
 * key slot to a container; only the private key can open that slot.
 *
 * <p>Encoding (big-endian):
 * <pre>
 * 4    magic "YAPK"
 * u16  version (1)
 * u8   scheme id, see {@link HybridKem#id()}
 * 32   X25519 public key
 * n    ML-KEM encapsulation key
 * </pre>
 *
 * <p>Both halves are validated on construction: the ML-KEM key with the
 * modulus check of FIPS 203, section 7.2, and the X25519 key against
 * low-order points.
 */
public final class HybridPublicKey
{
	private static final byte[] MAGIC = "YAPK".getBytes(StandardCharsets.US_ASCII);
	private static final int VERSION = 1;
	private static final String ARMOR_BEGIN = "-----BEGIN YACT PUBLIC KEY-----";
	private static final String ARMOR_END = "-----END YACT PUBLIC KEY-----";
	private static final int MLKEM_Q = 3329;

	private final HybridKem kem;
	private final byte[] x25519;
	private final byte[] mlKem;

	HybridPublicKey(HybridKem kem, byte[] x25519, byte[] mlKem)
	{
		if (x25519.length != HybridKem.X25519_KEY_LENGTH)
			throw new IllegalArgumentException("X25519 public key must be 32 bytes");
		if (mlKem.length != kem.mlKemPublicKeyLength())
			throw new IllegalArgumentException("ML-KEM public key must be " + kem.mlKemPublicKeyLength() + " bytes");
		if (!isValidMlKemKey(mlKem))
			throw new IllegalArgumentException("ML-KEM public key fails the FIPS 203 modulus check");
		if (isLowOrder(x25519))
			throw new IllegalArgumentException("X25519 public key is a low-order point");
		this.kem = kem;
		this.x25519 = x25519.clone();
		this.mlKem = mlKem.clone();
	}

	public HybridKem kem()
	{
		return kem;
	}

	byte[] x25519()
	{
		return x25519.clone();
	}

	byte[] mlKem()
	{
		return mlKem.clone();
	}

	X25519PublicKeyParameters x25519Parameters()
	{
		return new X25519PublicKeyParameters(x25519);
	}

	MLKEMPublicKeyParameters mlKemParameters()
	{
		return new MLKEMPublicKeyParameters(kem.mlKemParameters(), mlKem);
	}

	// ------------------------------------------------------------------ encoding

	public byte[] encode()
	{
		return new ByteWriter().bytes(MAGIC).u16(VERSION).u8(kem.id()).bytes(x25519).bytes(mlKem).toByteArray();
	}

	public static HybridPublicKey decode(byte[] encoded) throws FormatException
	{
		ByteReader r = new ByteReader(encoded);
		if (!Arrays.equals(r.bytes(MAGIC.length), MAGIC))
			throw new FormatException("not a YaCT public key");
		int version = r.u16();
		if (version != VERSION)
			throw new FormatException("unsupported public key version " + version);
		int kemId = r.u8();
		HybridKem kem = HybridKem.byId(kemId);
		if (kem == null)
			throw new FormatException("unsupported key scheme " + kemId);
		byte[] x = r.bytes(HybridKem.X25519_KEY_LENGTH);
		byte[] m = r.bytes(kem.mlKemPublicKeyLength());
		r.expectEnd();
		try
		{
			return new HybridPublicKey(kem, x, m);
		}
		catch (IllegalArgumentException e)
		{
			throw new FormatException("invalid public key: " + e.getMessage());
		}
	}

	/** Text form for e-mail or chat: Base64 between BEGIN/END lines, 64 characters per line. */
	public String toArmored()
	{
		String b64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(encode());
		return ARMOR_BEGIN + "\n" + b64 + "\n" + ARMOR_END + "\n";
	}

	public static HybridPublicKey fromArmored(String text) throws FormatException
	{
		int begin = text.indexOf(ARMOR_BEGIN);
		int end = text.indexOf(ARMOR_END);
		if (begin < 0 || end < begin)
			throw new FormatException("no YaCT public key found in text");
		String b64 = text.substring(begin + ARMOR_BEGIN.length(), end);
		try
		{
			return decode(Base64.getMimeDecoder().decode(b64));
		}
		catch (IllegalArgumentException e)
		{
			throw new FormatException("public key text is damaged");
		}
	}

	/**
	 * A short, human-comparable identifier: the first 20 bytes of
	 * SHA3-256 over the encoded key, as ten groups of four hex digits.
	 * Compare it with the key owner over a second channel before trusting a key.
	 */
	public String fingerprint()
	{
		SHA3Digest d = new SHA3Digest(256);
		byte[] enc = encode();
		d.update(enc, 0, enc.length);
		byte[] hash = new byte[32];
		d.doFinal(hash, 0);
		String hex = HexFormat.of().withUpperCase().formatHex(hash, 0, 20);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hex.length(); i += 4)
			sb.append(i == 0 ? "" : " ").append(hex, i, i + 4);
		return sb.toString();
	}

	// ------------------------------------------------------------------ validation

	/**
	 * FIPS 203, section 7.2, "modulus check": every 12-bit coefficient of the
	 * encoded vector t (all but the last 32 bytes) must be smaller than q = 3329.
	 */
	static boolean isValidMlKemKey(byte[] ek)
	{
		int polyBytes = ek.length - 32;
		for (int i = 0; i + 2 < polyBytes; i += 3)
		{
			int b0 = ek[i] & 0xFF;
			int b1 = ek[i + 1] & 0xFF;
			int b2 = ek[i + 2] & 0xFF;
			int c0 = b0 | ((b1 & 0x0F) << 8);
			int c1 = (b1 >>> 4) | (b2 << 4);
			if (c0 >= MLKEM_Q || c1 >= MLKEM_Q)
				return false;
		}
		return true;
	}

	/** A low-order point gives the all-zero shared secret for every private key. */
	private static boolean isLowOrder(byte[] x25519)
	{
		X25519Agreement agreement = new X25519Agreement();
		agreement.init(new X25519PrivateKeyParameters(new SecureRandom()));
		try
		{
			agreement.calculateAgreement(new X25519PublicKeyParameters(x25519), new byte[32], 0);
			return false;
		}
		catch (IllegalStateException e)
		{
			return true;
		}
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof HybridPublicKey k && kem == k.kem && Arrays.equals(x25519, k.x25519)
				&& Arrays.equals(mlKem, k.mlKem);
	}

	@Override
	public int hashCode()
	{
		return 31 * Arrays.hashCode(x25519) + Arrays.hashCode(mlKem);
	}

	@Override
	public String toString()
	{
		return kem + " " + fingerprint();
	}
}
