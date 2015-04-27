/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package application.ui.create;

import java.io.File;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import security.Hash;
import utils.Algorithms;
import utils.HashAlgorithms;
import utils.Mode;
import utils.SysProps;
import application.newContainer.NewContainer;
import application.ui.main.ISubController;

import com.sun.javafx.scene.traversal.Algorithm;

public class NCController implements ISubController<NewContainer>
{
	private FileChooser chooser;
	private NewContainer nc;
	private ObservableList<HashAlgorithms> hashes;
	private ObservableList<Algorithms> algorithms;
	private ObservableList<Mode> modes;
	private ObservableList<String> units;
	
	public NCController()
	{
		chooser = new FileChooser();
		nc = new NewContainer();
		hashes = FXCollections.observableArrayList();
		hashes.addAll(HashAlgorithms.values());
		
		algorithms = FXCollections.observableArrayList();
		algorithms.addAll(Algorithms.values());
		
		modes = FXCollections.observableArrayList();
		modes.addAll(Mode.values());
		
		units = FXCollections.observableArrayList();
		units.addAll("GB","MB");
	}
	
	@FXML
	private File buttonOpen()
	{
		System.out.println("Open clicked");
		chooser.setTitle("Open File");
		chooser.setInitialFileName(SysProps.getUserhome());
		File selectedFile = chooser.showOpenDialog(new Stage());
		pathField.setText(selectedFile.getPath());
		nc.setPath(selectedFile.getPath());
		return selectedFile;
	}
	

    @FXML
    private ToggleGroup groupNewContainer;
    
    private void toggle()
    {
    	if(rbS.isArmed())
    	{
    		hashField.setVisible(false);
    		modeField.setVisible(false);
    		algField.setVisible(false);
    	}
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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

    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
    }

	@Override
	public void cancel() 
	{
		
	}

	@Override
	public NewContainer doIt() 
	{
		
//		nc.getAlgorithm()==null ? AES : Algorithm
//		nc.setAlgorithm();
		return nc;	
	}
}
