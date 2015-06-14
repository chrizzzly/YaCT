/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package application.ui.create;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import security.InitVector;
import utils.Filesystems;
import utils.HashAlgorithms;
import application.newContainer.NewContainer;
import application.ui.main.ISubController;

@Deprecated
public class NCStepController implements ISubController<NewContainer>
{
	private ObservableList<Filesystems> filesystems;
	private InitVector iv = new InitVector();
	
	public NCStepController()
	{
		filesystems = FXCollections.observableArrayList();
		filesystems.addAll(Filesystems.values());
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//HelpArea
    @FXML
    private TextArea helpStep;
    
    @FXML
    private TextArea helpBlocksize;

    @FXML
    private TextArea helpFS;
    
    @FXML
    private Pane helpView;
    
    @FXML
	private void helpStepShow()
	{
		//onMouseOver
    	helpStep.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpStepHide()
	{
		//onMouseOut
		helpStep.visibleProperty().set(false);
	}
	
	 @FXML
	private void helpFSShow()
	{
		//onMouseOver
		 helpFS.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpFSHide()
	{
		//onMouseOut
		helpFS.visibleProperty().set(false);
	}
		
	@FXML
	private void helpBSShow()
	{
		//onMouseOver
		helpBlocksize.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpBSHide()
	{
		//onMouseOut
		helpBlocksize.visibleProperty().set(false);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
    @FXML
    private TextField bsField;

    @FXML
    private TextField ivField;

    @FXML
    private ComboBox<Filesystems> fsField;


    @FXML
    void initialize() 
    {
        assert helpStep != null : "fx:id=\"helpStep\" was not injected: check your FXML file 'yactNewContainerStep.fxml'.";
        assert bsField != null : "fx:id=\"bsField\" was not injected: check your FXML file 'yactNewContainerStep.fxml'.";
        assert helpBlocksize != null : "fx:id=\"helpBlocksize\" was not injected: check your FXML file 'yactNewContainerStep.fxml'.";
        assert helpFS != null : "fx:id=\"helpFS\" was not injected: check your FXML file 'yactNewContainerStep.fxml'.";
        assert ivField != null : "fx:id=\"ivField\" was not injected: check your FXML file 'yactNewContainerStep.fxml'.";
        assert fsField != null : "fx:id=\"fsField\" was not injected: check your FXML file 'yactNewContainerStep.fxml'.";
        assert helpView != null : "fx:id=\"helpView\" was not injected: check your FXML file 'yactNewContainerStep.fxml'.";

        fsField.itemsProperty().set(filesystems);
		fsField.setValue(Filesystems.NTFS);
		
		new Thread(new Runnable() 
		{
		    public void run() 
		    {
		    	ivField.setText(iv.InitVector());
		    }
		}).start();
    }


	@Override
	public void cancel() 
	{
		// TODO Auto-generated method stub
		
	}


	@Override
	public void doIt(NewContainer object) 
	{
		System.out.println("NCStepController doIt with object");
		
	}

	@Override
	public Class<?> getExpectedClass() 
	{
		return NewContainer.class;
	}
	
	
	
	@Override
	public boolean validate() 
	{return true;}
	
}
