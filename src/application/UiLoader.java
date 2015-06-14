/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package application;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import utils.SysProps;
import application.ui.main.ISubController;
import application.ui.main.SubWindow;

public class UiLoader 
{	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Utils
	private static ResourceBundle bundle = null;
	
	public static ResourceBundle getBundle()
	{
		if (bundle == null)
			bundle = loadBundle("application/language/yact");
		return bundle;
	}
	
	
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
	 * Loads the FXML files for the SubWindows of the GUI
	 * @param path The path of the FXML file in the Project
	 * @param resources The ResourceBundle within the Project
	 * @return Returns the window
	 * @throws IOException
	 */
	private static SubWindow loadSubFile(String path, ResourceBundle resources) throws IOException
	{
		FXMLLoader loader = new FXMLLoader(Main.class.getResource(path), resources);
		
		Parent parent = loader.load();
		ISubController controller = loader.<ISubController>getController();
		
		SubWindow window = new SubWindow();
		window.setParent(parent);
		window.setController(controller);
		
		return window;
	}
	
	/**
	 * Loads the ResourceBundle for the GUI
	 * @param path The path of the FXML file in the Project
	 * @return Returns the ResourceBundle
	 */
	private static ResourceBundle loadBundle(String path)
	{
		ResourceBundle resources = ResourceBundle.getBundle(path, SysProps.getSystemLocale());
		bundle = resources;
		return resources;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Main Window
	
	/**
	 * Loads the Main Window of the application
	 * @return Returns the parent element
	 * @throws IOException
	 */
	public static Parent loadMainUI() throws IOException
	{
		//ResourceBundle resources = loadBundle("application/language/yact");
		Parent root = loadFile("ui/main/yact.fxml", getBundle());
		
		return root;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Windows with SubWindows
	
	/**
	 * Loads the NewContainer SubWindow of the application
	 * @return Returns the subwindow
	 * @throws IOException
	 */
	public static SubWindow loadNewContainerUI() throws IOException
	{
		SubWindow pane = loadSubFile("ui/create/yactNewContainer.fxml", getBundle());
		
		return pane;
	}
	
	/**
	 * Loads the NewContainer - Step 2 - SubWindow of the application
	 * @return Returns the subwindow
	 * @throws IOException
	 */
	public static SubWindow loadNewContainerStepUI() throws IOException
	{
		SubWindow pane = loadSubFile("ui/create/yactNewContainerStep.fxml", getBundle());
		
		return pane;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//One-Window Screens
	/**
	 * Loads the OpenContainer Window of the application
	 * @return Returns the parent element
	 * @throws IOException
	 */
	public static Parent loadOpenContainerUI() throws IOException
	{
		Parent pane = loadFile("ui/open/yactOpenContainer.fxml", getBundle());
		
		return pane;
	}
	
	/**
	 * Loads the CloseContainer Window of the application
	 * @return Returns the parent element
	 * @throws IOException
	 */
	public static Parent loadCloseContainerUI() throws IOException
	{
		Parent pane = loadFile("ui/close/yactCloseContainer.fxml", getBundle());
		
		return pane;
	}
	
	/**
	 * Loads the AlterContainer Window of the application
	 * @return Returns the parent element
	 * @throws IOException
	 */
	public static Parent loadAlterContainerUI() throws IOException
	{
		Parent pane = loadFile("ui/alter/yactAlterContainer.fxml", getBundle());
		
		return pane;
	}

	/**
	 * Loads the About Window of the application
	 * @return Returns the parent element
	 * @throws IOException
	 */
	public static Parent loadAboutUI() throws IOException
	{
		Parent pane = loadFile("ui/main/about/yactAbout.fxml", getBundle());
		
		return pane;
	}
	
	/**
	 * Loads the Settings Window of the application
	 * @return Returns the parent element
	 * @throws IOException
	 */
	public static Parent loadSettingsUI() throws IOException
	{
		Parent pane = loadFile("ui/main/settings/yactSettings.fxml", getBundle());
		
		return pane;
	}
	

	
}
