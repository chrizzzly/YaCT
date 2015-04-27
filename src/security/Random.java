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
	 * Generates a Byte array with random data
	 * @param length the length of the byte array 
	 * @return returns byte[] with random data
	 */
	public byte[] generateRandom(int length)
	{
		byte[] retVal = new byte[length];
		SecureRandom rand = new SecureRandom();
		rand.nextBytes(retVal);
		return retVal;
	}
}
