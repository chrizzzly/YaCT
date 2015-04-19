/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package security;

import java.security.SecureRandom;

/**
 * @author Christian Hoppe
 *
 */
public class Random
{
	/**
	 * Generates a 512-Byte array with random data 
	 * @return returns byte[] with 512-Byte random data
	 */
	public byte[] generateRandom()
	{
		byte[] retVal = new byte[512];
		SecureRandom rand = new SecureRandom();
		rand.nextBytes(retVal);
		return retVal;
	}
}
