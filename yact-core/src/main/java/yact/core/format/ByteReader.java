/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.format;

/** Bounds-checked big-endian reader; every overrun is a {@link ContainerFormatException}. */
final class ByteReader
{
	private final byte[] data;
	private final int end;
	private int pos;

	ByteReader(byte[] data)
	{
		this(data, 0, data.length);
	}

	ByteReader(byte[] data, int off, int len)
	{
		this.data = data;
		this.pos = off;
		this.end = off + len;
	}

	int position()
	{
		return pos;
	}

	int remaining()
	{
		return end - pos;
	}

	int u8() throws ContainerFormatException
	{
		need(1);
		return data[pos++] & 0xFF;
	}

	int u16() throws ContainerFormatException
	{
		need(2);
		int v = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
		pos += 2;
		return v;
	}

	/** Reads an unsigned 32-bit value that must fit into an int. */
	int u31() throws ContainerFormatException
	{
		need(4);
		int v = ((data[pos] & 0xFF) << 24) | ((data[pos + 1] & 0xFF) << 16)
				| ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
		pos += 4;
		if (v < 0)
			throw new ContainerFormatException("value out of range");
		return v;
	}

	byte[] bytes(int n) throws ContainerFormatException
	{
		need(n);
		byte[] out = java.util.Arrays.copyOfRange(data, pos, pos + n);
		pos += n;
		return out;
	}

	void expectEnd() throws ContainerFormatException
	{
		if (pos != end)
			throw new ContainerFormatException("unexpected trailing data");
	}

	private void need(int n) throws ContainerFormatException
	{
		if (n < 0 || end - pos < n)
			throw new ContainerFormatException("truncated data");
	}
}
