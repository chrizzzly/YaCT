/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.stream;

import yact.core.crypto.CipherSuite;

/**
 * Nonces for the STREAM construction (Hoang, Reyhanitabar, Rogaway, Vizár,
 * "Online Authenticated-Encryption and its Nonce-Reuse Misuse-Resistance", 2015).
 *
 * <pre>
 * bytes 0..6   zero
 * bytes 7..10  chunk counter, big-endian
 * byte  11     1 for the final chunk, else 0
 * </pre>
 *
 * <p>The fixed prefix is safe only because every stream has its own key, see
 * {@link yact.core.format.UnlockedHeader#streamKey(byte[])}.
 */
final class ChunkNonce
{
	/** Highest chunk index that fits into the 32-bit counter. */
	static final long MAX_CHUNK_INDEX = 0xFFFF_FFFFL;

	private ChunkNonce()
	{
	}

	static byte[] of(long index, boolean last)
	{
		if (index < 0 || index > MAX_CHUNK_INDEX)
			throw new IllegalStateException("stream too long");
		byte[] n = new byte[CipherSuite.NONCE_LENGTH];
		n[7] = (byte) (index >>> 24);
		n[8] = (byte) (index >>> 16);
		n[9] = (byte) (index >>> 8);
		n[10] = (byte) index;
		n[11] = (byte) (last ? 1 : 0);
		return n;
	}
}
