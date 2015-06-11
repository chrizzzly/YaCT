/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Encoder;


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
	public byte[] generateRandomByte(int length)
	{
		byte[] retVal = new byte[length];
		SecureRandom rand = new SecureRandom();
		rand.nextBytes(retVal);
		return retVal;
	}
	
	
	/**
	 * Generates a random int
	 * @return returns random int
	 */
	public int generateRandomInt()
	{
		int retVal = 1;
		SecureRandom rand = new SecureRandom();
		rand.nextInt(retVal);
		return retVal;
	}
	
	public char[] generateRandomChar(int length)
	{
		byte[] temp = new byte[length];
		char[] retVal = new char[length];
		SecureRandom rand = new SecureRandom();
		rand.nextBytes(temp);
		
		for(int i = 0; i < temp.length; i++)
		{
			retVal[i] = (char) temp[i];
		}
		return retVal;
	}
	
	public char[] generateRandomPepper(int length)
	{
		Encoder base64 = Base64.getEncoder();
		SecureRandom rand = new SecureRandom();
		
		byte[] temp = new byte[4];
		char[] retVal = new char[length];
		
		for(int i = 0; i < retVal.length; i++)
		{
			rand.nextBytes(temp);
			
			retVal[i] = (char) base64.encode(temp)[0];
		}
		return retVal;
	}
	
	
}
