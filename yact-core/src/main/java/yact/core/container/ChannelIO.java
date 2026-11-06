/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.container;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

/** Streams over a region of a {@link FileChannel}, using positional reads and writes. */
final class ChannelIO
{
	private ChannelIO()
	{
	}

	static void writeFully(FileChannel ch, byte[] data, long position) throws IOException
	{
		ByteBuffer buf = ByteBuffer.wrap(data);
		while (buf.hasRemaining())
			position += ch.write(buf, position);
	}

	static byte[] readFully(FileChannel ch, long position, int length) throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocate(length);
		while (buf.hasRemaining())
		{
			int n = ch.read(buf, position + buf.position());
			if (n < 0)
				throw new java.io.EOFException("unexpected end of container file");
		}
		return buf.array();
	}

	/** Reads exactly {@code length} bytes starting at {@code start}; ends early only if the file is shorter. */
	static InputStream input(FileChannel ch, long start, long length)
	{
		return new InputStream()
		{
			private long pos = start;
			private final long end = start + length;

			@Override
			public int read() throws IOException
			{
				byte[] one = new byte[1];
				return read(one, 0, 1) == -1 ? -1 : one[0] & 0xFF;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException
			{
				Objects.checkFromIndexSize(off, len, b.length);
				if (len == 0)
					return 0;
				if (pos >= end)
					return -1;
				int n = ch.read(ByteBuffer.wrap(b, off, (int) Math.min(len, end - pos)), pos);
				if (n > 0)
					pos += n;
				return n;
			}
		};
	}

	/** Writes sequentially from {@code start}; {@link #written()} counts the bytes. Closing does not close the channel. */
	static final class Output extends OutputStream
	{
		private final FileChannel ch;
		private final long start;
		private long pos;

		Output(FileChannel ch, long start)
		{
			this.ch = ch;
			this.start = start;
			this.pos = start;
		}

		long written()
		{
			return pos - start;
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
			ByteBuffer buf = ByteBuffer.wrap(b, off, len);
			while (buf.hasRemaining())
				pos += ch.write(buf, pos);
		}
	}
}
