/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package application;
	
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.Debug;


public class Main extends Application 
{
	private String version = "0.7.0";
	
	@Override
	public void start(Stage primaryStage) 
	{
		Security.addProvider(new BouncyCastleProvider());
		try 
		{
			Parent root = UiLoader.loadMainUI();
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("yact.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("YaCT" + " " + version);
			primaryStage.show();
		} 
		catch(Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException, GeneralSecurityException 
	{
		try 
		{
			// Debug informations
			System.out.println(Debug.debugMsg());
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		launch(args);
	}
}
