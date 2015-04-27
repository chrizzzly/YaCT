/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package utils;

import java.io.File;
import java.util.Locale;

public class SysProps 
{
	/**
	 * Helper for system Linebrak
	 * @return returns the system linebrake
	 */
	public static String getLinebreak()
	{
		return System.getProperty("line.separator");
	}
	
	/**
	 * Helper for system Fileseperator
	 * @return returns the system Fileseperator
	 */
	public static String getFileseperator()
	{
		return System.getProperty("file.separator");
	}
	
	/**
	 * Helper for system Pathseperator
	 * @return returns the system pathseperator
	 */
	public static String getPathseperator()
	{
		return System.getProperty("path.separator");
	}
	
	/**
	 * Helper for system Homedirectory
	 * @return returns the users homedirectory
	 */
	public static String getUserhome()
	{
		return System.getProperty("user.home");
	}
	
	/**
	 * Helper for system language
	 * @return returns the system language
	 */
	public static Locale getSystemLocale()
	{
		return Locale.getDefault();
	}
	
}
