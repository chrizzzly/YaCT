/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.crypto;

import java.nio.charset.StandardCharsets;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;

/** Key derivation and MAC functions. All outputs are 32 bytes. */
public final class Kdf
{
	public static final int OUTPUT_LENGTH = 32;

	private Kdf()
	{
	}

	/**
	 * Derives a key from a password with Argon2id (version 1.3).
	 *
	 * @param password the password bytes, see {@link Passwords#encode(char[])}
	 */
	public static byte[] argon2id(byte[] password, byte[] salt, Argon2Params params)
	{
		Argon2BytesGenerator gen = new Argon2BytesGenerator();
		gen.init(new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
				.withVersion(Argon2Parameters.ARGON2_VERSION_13)
				.withSalt(salt)
				.withMemoryAsKB(params.memoryKiB())
				.withIterations(params.iterations())
				.withParallelism(params.parallelism())
				.build());
		byte[] out = new byte[OUTPUT_LENGTH];
		gen.generateBytes(password, out);
		return out;
	}

	/**
	 * HKDF (RFC 5869) over SHA3-256.
	 *
	 * @param ikm  input key material
	 * @param salt    optional salt, may be {@code null}
	 * @param label   purpose label; every purpose uses its own label
	 * @param context further values bound into the output, length-prefixed
	 */
	public static byte[] hkdf(byte[] ikm, byte[] salt, String label, byte[]... context)
	{
		HKDFBytesGenerator gen = new HKDFBytesGenerator(new SHA3Digest(256));
		gen.init(new HKDFParameters(ikm, salt, info(label, context)));
		byte[] out = new byte[OUTPUT_LENGTH];
		gen.generateBytes(out, 0, out.length);
		return out;
	}

	/** HMAC-SHA3-256. */
	public static byte[] hmac(byte[] key, byte[] data, int off, int len)
	{
		HMac mac = new HMac(new SHA3Digest(256));
		mac.init(new KeyParameter(key));
		mac.update(data, off, len);
		byte[] out = new byte[mac.getMacSize()];
		mac.doFinal(out, 0);
		return out;
	}

	/** Label bytes followed by each context element, each prefixed with its 2-byte length. */
	private static byte[] info(String label, byte[]... context)
	{
		byte[] l = label.getBytes(StandardCharsets.US_ASCII);
		int size = 2 + l.length;
		for (byte[] c : context)
			size += 2 + c.length;
		byte[] out = new byte[size];
		int p = put(out, 0, l);
		for (byte[] c : context)
			p = put(out, p, c);
		return out;
	}

	private static int put(byte[] out, int p, byte[] value)
	{
		if (value.length > 0xFFFF)
			throw new IllegalArgumentException("context element too long");
		out[p++] = (byte) (value.length >>> 8);
		out[p++] = (byte) value.length;
		System.arraycopy(value, 0, out, p, value.length);
		return p + value.length;
	}
}
