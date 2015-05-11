package application.ui.open;

import java.io.File;

import utils.SysProps;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class OCController 
{
	private FileChooser chooser;
	
	@FXML
    private Button buttonOpen;
	
	@FXML
	private Label labelKeyfiles;
	
	public OCController()
	{
		chooser = new FileChooser();
	}
	
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
	private ListView<?> keyfileList;
	
	@FXML
	private PasswordField pwField;
	  
	@FXML
	private TextField pathField;
	
	@FXML
	private Button buttonKeyfileAdd;

	@FXML
	private File buttonOpen()
	{
		System.out.println("Open clicked");
		chooser.setTitle("Open File");
		chooser.setInitialFileName(SysProps.getUserhome());
		File selectedFile = chooser.showOpenDialog(new Stage());
		pathField.setText(selectedFile.getPath());
		return selectedFile;
	}
	
	@FXML
	private File buttonKeyfileAdd()
	{
		chooser.setTitle("Add Keyfile");
		chooser.setInitialFileName(SysProps.getUserhome());
		File selectedFile = chooser.showOpenDialog(new Stage());
		pathField.setText(selectedFile.getPath());
		return selectedFile;
	}

    @FXML
    void initialize() 
    {
    	assert buttonOpen != null : "fx:id=\"buttonOpen\" was not injected: check your FXML file 'yactOpenContainer.fxml'.";
        assert helpPasswordUse != null : "fx:id=\"helpPasswordUse\" was not injected: check your FXML file 'yactOpenContainer.fxml'.";
        assert helpKeyfilesAdd != null : "fx:id=\"helpKeyfilesAdd\" was not injected: check your FXML file 'yactOpenContainer.fxml'.";
        assert keyfileList != null : "fx:id=\"keyfileList\" was not injected: check your FXML file 'yactOpenContainer.fxml'.";
        assert pwField != null : "fx:id=\"pwField\" was not injected: check your FXML file 'yactOpenContainer.fxml'.";
        assert helpKeyfilesUse != null : "fx:id=\"helpKeyfilesUse\" was not injected: check your FXML file 'yactOpenContainer.fxml'.";
        assert buttonKeyfileAdd != null : "fx:id=\"buttonKeyfileAdd\" was not injected: check your FXML file 'yactOpenContainer.fxml'.";
        assert helpView != null : "fx:id=\"helpView\" was not injected: check your FXML file 'yactOpenContainer.fxml'.";
        assert pathField != null : "fx:id=\"pathField\" was not injected: check your FXML file 'yactOpenContainer.fxml'.";
        assert helpPath != null : "fx:id=\"helpPath\" was not injected: check your FXML file 'yactOpenContainer.fxml'.";
        assert labelKeyfiles != null : "fx:id=\"labelKeyfiles\" was not injected: check your FXML file 'yactOpenContainer.fxml'.";

    	toggle();
    }
    
    private void toggle()
    {
    	labelKeyfiles.visibleProperty().set(false);
    	buttonKeyfileAdd.visibleProperty().set(false);
    	keyfileList.visibleProperty().set(false);
    }
}
