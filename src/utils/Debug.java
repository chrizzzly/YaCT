/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javafx.stage.Stage;
import filehandling.FileCreateContainer;
import filehandling.FileOpen;
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
	 * @throws Exception 
	 */
	public static String debugMsg() throws Exception
	{
		String retVal = "noch nichts gemacht";
		System.out.println(retVal);
		retVal = firsttest();
		System.out.println(retVal);
		retVal = secondtest();
		System.out.println(retVal);
		retVal = thirdtest();
		
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
	
	private static String secondtest() throws UnsupportedEncodingException, GeneralSecurityException 
	{
		FileCreateContainer.setEnvVars("G", 128, 8);
		System.out.println(FileCreateContainer.getEnvVars());
		
		return "secondtest fertig";
			
	}
	
	private static String thirdtest() throws Exception 
	{
//		Stage primaryStage = null;
//		FileOpen.start(primaryStage);
		
		return "thirdtest fertig";
			
	}

}
