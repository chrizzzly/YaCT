/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package application;
	
import java.io.IOException;
import java.security.GeneralSecurityException;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application 
{
	@Override
	public void start(Stage primaryStage) 
	{
		try 
		{
			Parent root = UiLoader.loadMainUI();
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("yact.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
		} 
		catch(Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException, GeneralSecurityException 
	{
		//System.out.println(Debug.debugMsg());
		
//		FileHeader.writeHeader(true, "123".getBytes("UTF-8"), "456".getBytes("UTF-8"), "789".getBytes("UTF-8"));
//		FileHeader.readHeader();
		launch(args);
	}
}
