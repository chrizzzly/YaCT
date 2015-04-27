/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package application;

import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import utils.SysProps;

public class UiLoader 
{	
	/**
	 * Loads the FXML files for the GUI
	 * @param path The path of the FXML file in the Project
	 * @param resources The ResourceBundle within the Project
	 * @return Returns the parent element
	 * @throws IOException
	 */
	private static Parent loadFile(String path, ResourceBundle resources) throws IOException
	{
		Parent root = FXMLLoader.load(Main.class.getResource(path), resources);
		return root;
	}
	
	/**
	 * Loads the ResourceBundle for the GUI
	 * @param path The path of the FXML file in the Project
	 * @return Returns the localized ResourceBundle
	 */
	private static ResourceBundle loadBundle(String path)
	{
		ResourceBundle resources = ResourceBundle.getBundle(path, SysProps.getSystemLocale());// UiLoader.class.getClassLoader());
		return resources;
	}
	
	/**
	 * Loads the Main Window of the application
	 * @return Returns the parent element
	 * @throws IOException
	 */
	public static Parent loadMainUI() throws IOException
	{
		ResourceBundle resources = loadBundle("application/language/yact");
		Parent root = loadFile("ui/main/yact.fxml", resources);
		
		return root;
	}
	
	public static Parent loadNewContainerUI() throws IOException
	{
		ResourceBundle resources = loadBundle("application/language/yact");
		Parent pane = loadFile("ui/create/yactNewContainer.fxml", resources);
		
		return pane;
	}
	
	public static Parent loadOpenContainerUI() throws IOException
	{
		ResourceBundle resources = loadBundle("application/language/yact");
		Parent pane = loadFile("ui/open/yactOpenContainer.fxml", resources);
		
		return pane;
	}
	
	public static Parent loadCloseContainerUI() throws IOException
	{
		ResourceBundle resources = loadBundle("application/language/yact");
		Parent pane = loadFile("ui/close/yactCloseContainer.fxml", resources);
		
		return pane;
	}
	
	public static Parent loadAlterContainerUI() throws IOException
	{
		ResourceBundle resources = loadBundle("application/language/yact");
		Parent pane = loadFile("ui/alter/yactAlterContainer.fxml", resources);
		
		return pane;
	}

	public static Parent loadAboutUI() throws IOException
	{
		ResourceBundle resources = loadBundle("application/language/yact");
		Parent pane = loadFile("ui/main/about/yactAbout.fxml", resources);
		
		return pane;
	}
	
}
