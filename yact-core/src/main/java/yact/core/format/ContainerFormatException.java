/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.format;

import java.io.IOException;

/** The data is not a YaCT container, is truncated, or uses an unsupported version or algorithm. */
public class ContainerFormatException extends IOException
{
	private static final long serialVersionUID = 1L;

	public ContainerFormatException(String message)
	{
		super(message);
	}
}
