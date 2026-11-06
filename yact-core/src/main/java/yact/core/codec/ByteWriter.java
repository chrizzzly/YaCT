/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.codec;

import java.io.ByteArrayOutputStream;

/** Big-endian writer for header structures. */
public final class ByteWriter
{
	private final ByteArrayOutputStream out = new ByteArrayOutputStream();

	public ByteWriter u8(int v)
	{
		if (v < 0 || v > 0xFF)
			throw new IllegalArgumentException("u8 out of range: " + v);
		out.write(v);
		return this;
	}

	public ByteWriter u16(int v)
	{
		if (v < 0 || v > 0xFFFF)
			throw new IllegalArgumentException("u16 out of range: " + v);
		out.write(v >>> 8);
		out.write(v);
		return this;
	}

	public ByteWriter u31(int v)
	{
		if (v < 0)
			throw new IllegalArgumentException("u31 out of range: " + v);
		out.write(v >>> 24);
		out.write(v >>> 16);
		out.write(v >>> 8);
		out.write(v);
		return this;
	}

	/** Writes a non-negative 64-bit value. */
	public ByteWriter u63(long v)
	{
		if (v < 0)
			throw new IllegalArgumentException("u63 out of range: " + v);
		for (int shift = 56; shift >= 0; shift -= 8)
			out.write((int) (v >>> shift));
		return this;
	}

	public ByteWriter bytes(byte[] b)
	{
		out.writeBytes(b);
		return this;
	}

	public int size()
	{
		return out.size();
	}

	public byte[] toByteArray()
	{
		return out.toByteArray();
	}
}
