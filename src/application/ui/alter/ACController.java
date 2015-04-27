package application.ui.alter;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ACController 
{
private FileChooser chooser = new FileChooser();
	
	@FXML
	private void buttonOpen()
	{
		chooser.setTitle("Open File");
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
