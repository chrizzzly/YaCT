/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package application.ui.create;

import java.io.File;
import java.util.Date;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import utils.Algorithms;
import utils.HashAlgorithms;
import utils.Mode;
import utils.SysProps;
import application.newContainer.NewContainer;
import application.ui.main.ISubController;
import application.ui.main.YactController;

public class NCController implements ISubController<NewContainer>
{
	private FileChooser chooser;
	private ObservableList<HashAlgorithms> hashes;
	private ObservableList<Algorithms> algorithms;
	private ObservableList<Mode> modes;
	private ObservableList<String> units;
	
	public NCController()
	{
		chooser = new FileChooser();
		hashes = FXCollections.observableArrayList();
		hashes.addAll(HashAlgorithms.values());
		
		algorithms = FXCollections.observableArrayList();
		algorithms.addAll(Algorithms.values());
		
		modes = FXCollections.observableArrayList();
		modes.addAll(Mode.values());
		
		units = FXCollections.observableArrayList();
		units.addAll("GB","MB");
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//HelpArea
	@FXML
	private TextArea helpStandard;
	@FXML
	private TextArea helpCustom;
	@FXML
	private TextArea helpPassword;
	@FXML
	private TextArea helpPath;
	@FXML
	private TextArea helpSize;
	@FXML
	private TextArea helpAlgorithm;
	@FXML
	private TextArea helpMode;
	@FXML
	private TextArea helpHash;
	
	@FXML
	private Pane helpView;
	
	@FXML
	private void helpStandardShow()
	{
		//onMouseOver
		helpStandard.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpStandardHide()
	{
		//onMouseOut
		helpStandard.visibleProperty().set(false);
	}
	
	@FXML
	private void helpCustomShow()
	{
		//onMouseOver
		helpCustom.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpCustomHide()
	{
		//onMouseOut
		helpCustom.visibleProperty().set(false);
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
	private void helpSizeShow()
	{
		//onMouseOver
		helpSize.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpSizeHide()
	{
		//onMouseOut
		helpSize.visibleProperty().set(false);
	}
	
	@FXML
	private void helpAlgorithmShow()
	{
		//onMouseOver
		helpAlgorithm.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpAlgorithmHide()
	{
		//onMouseOut
		helpAlgorithm.visibleProperty().set(false);
	}
	
	@FXML
	private void helpModeShow()
	{
		//onMouseOver
		helpMode.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpModeHide()
	{
		//onMouseOut
		helpMode.visibleProperty().set(false);
	}
	
	@FXML
	private void helpHashShow()
	{
		//onMouseOver
		helpHash.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpHashHide()
	{
		//onMouseOut
		helpHash.visibleProperty().set(false);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
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
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// ToggleGroup for Radio Buttons
    @FXML
    private ToggleGroup groupNewContainer;
    
    @FXML
    private Label hashLabel;
    @FXML
    private Label algLabel;
    @FXML
    private Label modeLabel;
    
    @FXML
    private void toggleStandard()
    {
    	hashField.visibleProperty().set(false);
    	algField.visibleProperty().set(false);
    	modeField.visibleProperty().set(false);
    	hashLabel.visibleProperty().set(false);
    	algLabel.visibleProperty().set(false);
    	modeLabel.visibleProperty().set(false);
    }
    
    @FXML
    private void toggleCustom()
    {
    	hashLabel.visibleProperty().set(true);
    	algLabel.visibleProperty().set(true);
    	modeLabel.visibleProperty().set(true);
    	hashField.visibleProperty().set(true);
    	algField.visibleProperty().set(true);
    	modeField.visibleProperty().set(true);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Need to be filled by User
    @FXML
    private RadioButton rbS;
    
    @FXML
    private RadioButton rbC;
    
    @FXML
    private PasswordField pwField;
    
    @FXML
    private PasswordField pwFieldRepeat;

    @FXML
    private TextField sizeField;

    @FXML
    private ComboBox<String> sizeFieldUnit;

    @FXML
    private TextField pathField;

    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Only needed on custom mode
    @FXML
    private ComboBox<HashAlgorithms> hashField;
    
    @FXML
    private ComboBox<Mode> modeField;  

    @FXML
    private ComboBox<Algorithms> algField;
    


    @FXML
    void initialize() 
    {
        assert groupNewContainer != null : "fx:id=\"groupNewContainer\" was not injected: check your FXML file 'yactNewContainer.fxml'.";
        assert pwFieldRepeat != null : "fx:id=\"pwFieldRepeat\" was not injected: check your FXML file 'yactNewContainer.fxml'.";
        assert hashField != null : "fx:id=\"hashField\" was not injected: check your FXML file 'yactNewContainer.fxml'.";
        assert pwField != null : "fx:id=\"pwField\" was not injected: check your FXML file 'yactNewContainer.fxml'.";
        assert sizeFieldUnit != null : "fx:id=\"sizeFieldUnit\" was not injected: check your FXML file 'yactNewContainer.fxml'.";
        assert pathField != null : "fx:id=\"pathField\" was not injected: check your FXML file 'yactNewContainer.fxml'.";
        assert modeField != null : "fx:id=\"modeField\" was not injected: check your FXML file 'yactNewContainer.fxml'.";
        assert sizeField != null : "fx:id=\"sizeField\" was not injected: check your FXML file 'yactNewContainer.fxml'.";
        assert algField != null : "fx:id=\"algField\" was not injected: check your FXML file 'yactNewContainer.fxml'.";

        
		hashField.itemsProperty().set(hashes);
		hashField.setValue(HashAlgorithms.Whirlpool);
		
		algField.itemsProperty().set(algorithms);
		algField.setValue(Algorithms.AES);
		
		modeField.itemsProperty().set(modes);
		modeField.setValue(Mode.XTS);
		
		sizeFieldUnit.itemsProperty().set(units);
		sizeFieldUnit.setValue("MB");
		
		toggleStandard();
    }

	@Override
	public void cancel() 
	{
		
	}

	@Override
	public void doIt(NewContainer object) 
	{
		object.setAlgorithm(algField.getValue());
		object.setHash(hashField.getValue());
		object.setMode(modeField.getValue());
		
		try
		{
			if(pwField.getText() == pwFieldRepeat.getText())
				object.setPassword(pwField.getText().toCharArray());
		}
		catch(Exception e)
		{
			// Passwords do not match exception
			e.printStackTrace();
		}
		
		if(pathField.getText() == null)
		{
			Date date = new Date();
			object.setPath(SysProps.getUserhome() + "NewContainer" + date.toString() + ".container");
			System.out.println(object.getPath());
		}
		else
		{
			object.setPath(pathField.getText());
		}
		
		int size = Integer.parseInt(sizeField.getText());
		
		object.setSize(size);
		
		object.setUnit(sizeFieldUnit.getValue().charAt(0));
	}


	@Override
	public Class<?> getExpectedClass() 
	{
		return NewContainer.class;
	}
}
