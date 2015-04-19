package application;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class UiLoader 
{	
	private static Parent loadFile(String path, ResourceBundle resources) throws IOException
	{
		Parent root = FXMLLoader.load(Main.class.getResource(path), resources);
		return root;
	}
	
	private static ResourceBundle loadBundle(String path)
	{
		ResourceBundle resources = ResourceBundle.getBundle(path, Locale.GERMANY);
		return resources;
	}
	
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
}
