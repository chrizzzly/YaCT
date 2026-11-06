/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.codec;

/** Bounds-checked big-endian reader; every overrun is a {@link FormatException}. */
public final class ByteReader
{
	private final byte[] data;
	private final int end;
	private int pos;

	public ByteReader(byte[] data)
	{
		this(data, 0, data.length);
	}

	public ByteReader(byte[] data, int off, int len)
	{
		this.data = data;
		this.pos = off;
		this.end = off + len;
	}

	public int position()
	{
		return pos;
	}

	public int remaining()
	{
		return end - pos;
	}

	public int u8() throws FormatException
	{
		need(1);
		return data[pos++] & 0xFF;
	}

	public int u16() throws FormatException
	{
		need(2);
		int v = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
		pos += 2;
		return v;
	}

	/** Reads an unsigned 32-bit value that must fit into an int. */
	public int u31() throws FormatException
	{
		need(4);
		int v = ((data[pos] & 0xFF) << 24) | ((data[pos + 1] & 0xFF) << 16)
				| ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
		pos += 4;
		if (v < 0)
			throw new FormatException("value out of range");
		return v;
	}

	/** Reads an unsigned 64-bit value that must fit into a long. */
	public long u63() throws FormatException
	{
		need(8);
		long v = 0;
		for (int i = 0; i < 8; i++)
			v = (v << 8) | (data[pos + i] & 0xFF);
		pos += 8;
		if (v < 0)
			throw new FormatException("value out of range");
		return v;
	}

	public byte[] bytes(int n) throws FormatException
	{
		need(n);
		byte[] out = java.util.Arrays.copyOfRange(data, pos, pos + n);
		pos += n;
		return out;
	}

	public void expectEnd() throws FormatException
	{
		if (pos != end)
			throw new FormatException("unexpected trailing data");
	}

	private void need(int n) throws FormatException
	{
		if (n < 0 || end - pos < n)
			throw new FormatException("truncated data");
	}
}
