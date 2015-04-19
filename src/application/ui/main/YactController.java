/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package application.ui.main;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import application.UiLoader;

public class YactController 
{	
	ResourceBundle resources;
	
	@FXML
	private TextArea newContainerHelpArea;
	@FXML
	private TextArea openContainerHelpArea;
	@FXML
	private TextArea closeContainerHelpArea;
	@FXML
	private TextArea alterContainerHelpArea;
	
	@FXML
	private Pane containerView;
	
	
	private Parent newContainerPane;
	private Parent openContainerPane;
	private Parent closeContainerPane;
	private Parent alterContainerPane;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public YactController() throws IOException
	{
		newContainerPane = UiLoader.loadNewContainerUI();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Button NewContainer
	@FXML
	private void newContainerPane()
	{
		//onClick onTouch
	}
	
	@FXML
	private void newContainerHelp()
	{
		//onMouseOver
		newContainerHelpArea.visibleProperty().set(!containerView.isVisible());
	}
	
	@FXML
	private void newContainerHelpExit()
	{
		//onMouseOut
		newContainerHelpArea.visibleProperty().set(false);
	}
	
	@FXML
	private void newContainerHelpArea()
	{
		
	}
	
	@FXML
	private void newContainerPaneFire()
	{
		System.out.println("Click!");
		containerView.getChildren().clear();
		containerView.getChildren().add(newContainerPane);
		containerView.setVisible(true);
//		newContainerPane.visibleProperty().set(true);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Button OpenContainer
	@FXML
	private void openContainerPane()
	{
		//onClick onTouch
	}
	
	@FXML
	private void openContainerHelp()
	{
		//onMouseOver
		openContainerHelpArea.visibleProperty().set(!containerView.isVisible());
	}

	@FXML
	private void openContainerHelpExit()
	{
		//onMouseOut
		openContainerHelpArea.visibleProperty().set(false);
	}
	
	@FXML
	private void openContainerHelpArea()
	{
		
	}
	
	@FXML
	private void openContainerPaneFire()
	{
		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Button CloseContainer
	@FXML
	private void closeContainerPane()
	{
		//onClick onTouch
	}
	
	@FXML
	private void closeContainerHelp()
	{
		//onMouseOver
		closeContainerHelpArea.visibleProperty().set(!containerView.isVisible());
	}
	
	@FXML
	private void closeContainerHelpExit()
	{
		//onMouseOut
		closeContainerHelpArea.visibleProperty().set(false);
	}
	
	@FXML
	private void closeContainerHelpArea()
	{}
	
	@FXML
	private void closeContainerPaneFire()
	{}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Button AlterContainer
	@FXML
	private void alterContainerPane()
	{
		//onClick onTouch
	}
	
	@FXML
	private void alterContainerHelp()
	{
		//onMouseOver
		alterContainerHelpArea.visibleProperty().set(!containerView.isVisible());
	}
	
	@FXML
	private void alterContainerHelpExit()
	{
		//onMouseOut
		alterContainerHelpArea.visibleProperty().set(false);
	}
	
	@FXML
	private void alterContainerHelpArea()
	{}
	
	@FXML
	private void alterContainerPaneFire()
	{}
	
	
	
	

}
