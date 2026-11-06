/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import yact.core.crypto.CipherSuite;
import yact.core.crypto.Passwords;

/**
 * Encrypts a byte stream in fixed-size, individually authenticated chunks.
 *
 * <p>Every chunk becomes {@code chunkSize + 16} bytes of output, except the
 * final one, which holds the remaining 0..chunkSize bytes plus its tag. A
 * final chunk is always written, even for empty input, so that the reader can
 * tell a complete stream from a truncated one.
 *
 * <p>Call {@link #finish()} or {@link #close()}: without it the stream is
 * incomplete and will be rejected when read.
 */
public final class ChunkedAeadOutputStream extends OutputStream
{
	private final OutputStream out;
	private final CipherSuite suite;
	private final byte[] key;
	private final byte[] aad;
	private final byte[] buffer;
	private int filled;
	private long index;
	private boolean finished;

	/**
	 * @param key       32-byte stream key, unique to this stream; copied
	 * @param aad       associated data bound to every chunk (e.g. a file id); copied
	 * @param chunkSize plaintext bytes per chunk
	 */
	public ChunkedAeadOutputStream(OutputStream out, CipherSuite suite, byte[] key, byte[] aad, int chunkSize)
	{
		if (chunkSize < 1)
			throw new IllegalArgumentException("chunk size must be positive");
		if (key.length != CipherSuite.KEY_LENGTH)
			throw new IllegalArgumentException("key must be " + CipherSuite.KEY_LENGTH + " bytes");
		this.out = Objects.requireNonNull(out);
		this.suite = Objects.requireNonNull(suite);
		this.key = key.clone();
		this.aad = aad.clone();
		this.buffer = new byte[chunkSize];
	}

	@Override
	public void write(int b) throws IOException
	{
		write(new byte[] { (byte) b }, 0, 1);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		Objects.checkFromIndexSize(off, len, b.length);
		if (finished)
			throw new IOException("stream already finished");
		while (len > 0)
		{
			// Emit a full chunk only once more data arrives: the last chunk may be full.
			if (filled == buffer.length)
				emit(false);
			int n = Math.min(len, buffer.length - filled);
			System.arraycopy(b, off, buffer, filled, n);
			filled += n;
			off += n;
			len -= n;
		}
	}

	/** Writes the final chunk without closing the underlying stream. */
	public void finish() throws IOException
	{
		if (finished)
			return;
		try
		{
			emit(true);
			out.flush();
		}
		finally
		{
			finished = true;
			Passwords.wipe(key, buffer);
		}
	}

	/** Flushes the underlying stream; buffered plaintext is only written once its chunk is complete. */
	@Override
	public void flush() throws IOException
	{
		out.flush();
	}

	@Override
	public void close() throws IOException
	{
		try
		{
			finish();
		}
		finally
		{
			out.close();
		}
	}

	private void emit(boolean last) throws IOException
	{
		byte[] sealed = suite.seal(key, ChunkNonce.of(index, last), aad, buffer, 0, filled);
		out.write(sealed);
		index++;
		filled = 0;
	}
}
