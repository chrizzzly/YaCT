package application.ui.create;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import utils.Filesystems;
import utils.Mode;
import application.newContainer.NewContainer;
import application.ui.main.ISubController;

public class NCStepController implements ISubController<NewContainer>
{
	private ObservableList<Filesystems> filesystems;
	
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
    }

	@Override
	public void cancel() 
	{
		// TODO Auto-generated method stub
		
	}


	@Override
	public void doIt(NewContainer object) 
	{
		int blocksize = Integer.parseInt(bsField.getText());
		object.setBlocksize(blocksize);
		
		object.setFileSystem(fsField.getValue());
		
	}

	@Override
	public Class<?> getExpectedClass() 
	{
		return NewContainer.class;
	}
}
