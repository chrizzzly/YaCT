/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package utils;

public class SysProps 
{
	public static String getLinebreak()
	{
		String retVal = System.getProperty("line.separator");
		return retVal;
	}
	
	public static String getFileseperator()
	{
		String retVal = System.getProperty("file.separator");
		return retVal;
	}
	
	public static String getPathseperator()
	{
		String retVal = System.getProperty("path.separator");
		return retVal;
	}
	
	public static String getUserhome()
	{
		String retVal = System.getProperty("user.home");
		return retVal;
	}
	
}
