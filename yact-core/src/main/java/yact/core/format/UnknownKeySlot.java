/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.format;

/**
 * A key slot of a type this version of YaCT does not know.
 *
 * <p>It is kept byte for byte, so that rewriting the header (for example to
 * add a password) does not destroy a slot created by a newer version.
 */
public record UnknownKeySlot(int type, byte[] body) implements KeySlot
{
	public UnknownKeySlot
	{
		body = body.clone();
	}

	@Override
	public byte[] body()
	{
		return body.clone();
	}

	@Override
	public byte[] encodeBody()
	{
		return body.clone();
	}
}
