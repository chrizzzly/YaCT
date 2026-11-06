/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.codec;

import java.io.IOException;

/** Binary data is malformed, truncated or of an unsupported version. */
public class FormatException extends IOException
{
	private static final long serialVersionUID = 1L;

	public FormatException(String message)
	{
		super(message);
	}
}
