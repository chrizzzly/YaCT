/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.format;

import java.io.IOException;

/**
 * Authenticated data failed verification: the container was modified,
 * truncated, reordered or damaged after it was written.
 */
public class IntegrityException extends IOException
{
	private static final long serialVersionUID = 1L;

	public IntegrityException(String message)
	{
		super(message);
	}

	public IntegrityException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
