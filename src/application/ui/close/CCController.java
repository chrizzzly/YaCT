package application.ui.close;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;

public class CCController 
{
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//HelpArea
	@FXML
	private TextArea helpCloseOpen;
	@FXML
	private TextArea helpCloseAll;
	
	@FXML
	private Pane helpView;

	@FXML
	private void helpCloseOpenShow()
	{
		//onMouseOver
		helpCloseOpen.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpCloseOpenHide()
	{
		//onMouseOut
		helpCloseOpen.visibleProperty().set(false);
	}
	
	@FXML
	private void helpCloseAllShow()
	{
		//onMouseOver
		helpCloseAll.visibleProperty().set(!helpView.isVisible());
	}
	
	@FXML
	private void helpCloseAllHide()
	{
		//onMouseOut
		helpCloseAll.visibleProperty().set(false);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
    @FXML
    void initialize() 
    {

    }
}

