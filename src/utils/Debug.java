/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package utils;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import security.Crypt;

/**
 * @author Christian Hoppe
 *
 */
public class Debug 
{
	/**
	 * Tests the Classes in the Project.</br>This method is for testing only!
	 * @return returns debug messages
	 */
	public static String debugMsg() throws UnsupportedEncodingException, GeneralSecurityException
	{
		String retVal = "noch nichts gemacht";
		
		retVal = firsttest();
	
		
		return retVal;
	}

	private static String firsttest() throws UnsupportedEncodingException, GeneralSecurityException 
	{
		String input = "INPUT";
		String key = "Der sollte möglichst lang sein!";
		String cryptoalg = "Serpent";
		String hashalg = "Whirlpool";
		Crypt.encrypt(input.getBytes("UTF-16"), key.getBytes("UTF-16"), cryptoalg, hashalg);
		return "firsttest fertig";
			
	}

}
