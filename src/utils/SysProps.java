/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package utils;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.collections.ObservableList;

public class SysProps 
{
	/**
	 * Helper for system Linebreak
	 * @return returns the system linebreak
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
		if(customLocale != null)
			return customLocale;
		return Locale.getDefault();
	}
	
	/**
	 * Helper for custom language
	 * @param locale the custom locale
	 */
	public void setCustomLocale(Locale locale)
	{
		SysProps.customLocale = locale;
	}
	
	private static Locale customLocale = null;
	
	/**
	 * Helper for custom language Dropdown-Menu
	 * @param resources the ResourceBundle of YaCT
	 * @return returns the ObservableList of included locales
	 */
	public static ObservableList<Locale> getImplementedLocales(ResourceBundle resources) 
	{
		ObservableList<Locale> retVal = null;		 
//		while(resources.getLocale() != null)
//		{
//			retVal.add(resources.getLocale());
//		}
		
		
		return retVal;
	}
}
