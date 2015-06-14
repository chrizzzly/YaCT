/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package application.ui.alter;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ACController 
{
	private FileChooser chooser = new FileChooser();
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//HelpArea
	@FXML
	private TextArea helpPath;
	@FXML
	private TextArea helpPassword;
	@FXML
	private TextArea helpKeyfiles;
	@FXML
	private TextArea helpPasswordUse;
	
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
	private void helpPasswordShow()
	{
		//onMouseOver
		helpPassword.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpPasswordHide()
	{
		//onMouseOut
		helpPassword.visibleProperty().set(false);
	}
	
	@FXML
	private void helpKeyfilesShow()
	{
		//onMouseOver
		helpKeyfiles.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpKeyfilesHide()
	{
		//onMouseOut
		helpKeyfiles.visibleProperty().set(false);
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
	
	
	

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
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
