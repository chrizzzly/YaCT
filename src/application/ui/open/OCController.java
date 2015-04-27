package application.ui.open;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class OCController 
{
	private FileChooser chooser = new FileChooser();
	
	@FXML
	private void buttonOpen()
	{
		chooser.setTitle("Open File");
		chooser.showOpenDialog(new Stage());
	}
	
	@FXML
	private void buttonKeyfileAdd()
	{
		chooser.setTitle("Add Keyfile");
		chooser.showOpenDialog(new Stage());
	}
	
    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    void initialize() 
    {

    }
}
