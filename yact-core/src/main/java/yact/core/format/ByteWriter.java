/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.format;

import java.io.ByteArrayOutputStream;

/** Big-endian writer for header structures. */
final class ByteWriter
{
	private final ByteArrayOutputStream out = new ByteArrayOutputStream();

	ByteWriter u8(int v)
	{
		if (v < 0 || v > 0xFF)
			throw new IllegalArgumentException("u8 out of range: " + v);
		out.write(v);
		return this;
	}

	ByteWriter u16(int v)
	{
		if (v < 0 || v > 0xFFFF)
			throw new IllegalArgumentException("u16 out of range: " + v);
		out.write(v >>> 8);
		out.write(v);
		return this;
	}

	ByteWriter u31(int v)
	{
		if (v < 0)
			throw new IllegalArgumentException("u31 out of range: " + v);
		out.write(v >>> 24);
		out.write(v >>> 16);
		out.write(v >>> 8);
		out.write(v);
		return this;
	}

	ByteWriter bytes(byte[] b)
	{
		out.writeBytes(b);
		return this;
	}

	int size()
	{
		return out.size();
	}

	byte[] toByteArray()
	{
		return out.toByteArray();
	}
}
