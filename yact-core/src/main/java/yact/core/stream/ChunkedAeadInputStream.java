/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.bouncycastle.crypto.InvalidCipherTextException;

import yact.core.crypto.CipherSuite;
import yact.core.crypto.Passwords;
import yact.core.format.IntegrityException;

/**
 * Reads a stream written by {@link ChunkedAeadOutputStream}.
 *
 * <p>Plaintext is released one verified chunk at a time. A modified, missing,
 * reordered or appended chunk, or a stream without its final chunk, makes a
 * read fail with {@link IntegrityException}. Data returned before that point
 * was authentic, but the caller must not treat the stream as complete until
 * {@code read} has returned -1.
 *
 * <p>The underlying stream must end exactly where the encrypted stream ends.
 */
public final class ChunkedAeadInputStream extends InputStream
{
	private final InputStream in;
	private final CipherSuite suite;
	private final byte[] key;
	private final byte[] aad;
	private final int sealedChunkSize;

	private final byte[] sealed;
	private int lookahead = -1;
	private long index;
	private boolean sawLast;
	private boolean failed;

	private byte[] plain = new byte[0];
	private int plainPos;
	private boolean closed;

	public ChunkedAeadInputStream(InputStream in, CipherSuite suite, byte[] key, byte[] aad, int chunkSize)
	{
		if (chunkSize < 1)
			throw new IllegalArgumentException("chunk size must be positive");
		if (key.length != CipherSuite.KEY_LENGTH)
			throw new IllegalArgumentException("key must be " + CipherSuite.KEY_LENGTH + " bytes");
		this.in = Objects.requireNonNull(in);
		this.suite = Objects.requireNonNull(suite);
		this.key = key.clone();
		this.aad = aad.clone();
		this.sealedChunkSize = chunkSize + CipherSuite.TAG_LENGTH;
		this.sealed = new byte[sealedChunkSize];
	}

	@Override
	public int read() throws IOException
	{
		byte[] one = new byte[1];
		int n = read(one, 0, 1);
		return n == -1 ? -1 : one[0] & 0xFF;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		Objects.checkFromIndexSize(off, len, b.length);
		if (closed)
			throw new IOException("stream closed");
		if (failed)
			throw new IntegrityException("encrypted stream failed authentication earlier");
		if (len == 0)
			return 0;
		while (plainPos == plain.length)
		{
			if (sawLast)
				return -1;
			nextChunk();
		}
		int n = Math.min(len, plain.length - plainPos);
		System.arraycopy(plain, plainPos, b, off, n);
		plainPos += n;
		return n;
	}

	@Override
	public int available()
	{
		return plain.length - plainPos;
	}

	private void nextChunk() throws IOException
	{
		int n = 0;
		if (lookahead >= 0)
		{
			sealed[n++] = (byte) lookahead;
			lookahead = -1;
		}
		n += in.readNBytes(sealed, n, sealedChunkSize - n);

		boolean last;
		if (n < sealedChunkSize)
		{
			last = true;
		}
		else
		{
			// A full-size chunk is the last one exactly when nothing follows it.
			lookahead = in.read();
			last = lookahead == -1;
		}
		if (n < CipherSuite.TAG_LENGTH)
		{
			failed = true;
			throw new IntegrityException("encrypted stream is truncated");
		}

		Passwords.wipe(plain);
		try
		{
			plain = suite.open(key, ChunkNonce.of(index, last), aad, sealed, 0, n);
		}
		catch (InvalidCipherTextException e)
		{
			plain = new byte[0];
			failed = true;
			throw new IntegrityException("encrypted chunk " + index + " failed authentication", e);
		}
		plainPos = 0;
		index++;
		sawLast = last;
	}

	@Override
	public void close() throws IOException
	{
		if (closed)
			return;
		closed = true;
		Passwords.wipe(key, plain, sealed);
		in.close();
	}
}
