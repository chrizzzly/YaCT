package application.ui.open;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class OCController 
{
	private FileChooser chooser = new FileChooser();
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//HelpArea
	@FXML
	private TextArea helpPath;
	@FXML
	private TextArea helpPasswordUse;
	@FXML
	private TextArea helpKeyfilesUse;
	@FXML
	private TextArea helpKeyfilesAdd;
	
	@FXML
	private Pane helpView;
	
	@FXML
	private void helpPathShow()
	{
		//onMouseOver
		helpPath.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpPathHide()
	{
		//onMouseOut
		helpPath.visibleProperty().set(false);
	}
	
	@FXML
	private void helpPasswordUseShow()
	{
		//onMouseOver
		helpPasswordUse.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpPasswordUseHide()
	{
		//onMouseOut
		helpPasswordUse.visibleProperty().set(false);
	}
	
	@FXML
	private void helpKeyfilesUseShow()
	{
		//onMouseOver
		helpKeyfilesUse.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpKeyfilesUseHide()
	{
		//onMouseOut
		helpKeyfilesUse.visibleProperty().set(false);
	}
	
	@FXML
	private void helpKeyfilesAddShow()
	{
		//onMouseOver
		helpKeyfilesAdd.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpKeyfilesAddHide()
	{
		//onMouseOut
		helpKeyfilesAdd.visibleProperty().set(false);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
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
