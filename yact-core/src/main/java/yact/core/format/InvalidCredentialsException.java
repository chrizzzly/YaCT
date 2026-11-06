/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.format;

import java.security.GeneralSecurityException;

/**
 * No key slot could be opened with the given credentials.
 *
 * <p>A wrong password and a manipulated key slot are indistinguishable by
 * design, so this exception does not say which one occurred.
 */
public class InvalidCredentialsException extends GeneralSecurityException
{
	private static final long serialVersionUID = 1L;

	public InvalidCredentialsException()
	{
		super("wrong password or damaged key slot");
	}
}
