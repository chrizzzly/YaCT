/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package application.ui.main;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import application.UiLoader;

public class YactController implements ISubController
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
	private Parent aboutPane;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public YactController() throws IOException
	{
		newContainerPane = UiLoader.loadNewContainerUI();
		openContainerPane = UiLoader.loadOpenContainerUI();
		closeContainerPane = UiLoader.loadCloseContainerUI();
		alterContainerPane = UiLoader.loadAlterContainerUI();
		aboutPane = UiLoader.loadAboutUI();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Button NewContainer
	@FXML
	private void newContainerPane()
	{
		//onClick onTouch
		newContainerHelpArea.visibleProperty().set(false);
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
		System.out.println("NewContainerPane clicked");
		containerView.getChildren().clear();
		containerView.getChildren().add(newContainerPane);
		containerView.setVisible(true);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Button OpenContainer
	@FXML
	private void openContainerPane()
	{
		//onClick onTouch
		openContainerHelpArea.visibleProperty().set(false);
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
		System.out.println("OpenContainerPane clicked");
		containerView.getChildren().clear();
		containerView.getChildren().add(openContainerPane);
		containerView.setVisible(true);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Button CloseContainer
	@FXML
	private void closeContainerPane()
	{
		//onClick onTouch
		closeContainerHelpArea.visibleProperty().set(false);
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
	{
		System.out.println("CloseContainerPane clicked");
		containerView.getChildren().clear();
		containerView.getChildren().add(closeContainerPane);
		containerView.setVisible(true);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Button AlterContainer
	@FXML
	private void alterContainerPane()
	{
		//onClick onTouch
		alterContainerHelpArea.visibleProperty().set(false);
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
	{
		System.out.println("AlterContainerPane clicked");
		containerView.getChildren().clear();
		containerView.getChildren().add(alterContainerPane);
		containerView.setVisible(true);
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Menu
	@FXML
    void quitProgram()
	{
		Platform.exit();
    }
	
	@FXML
    void aboutAreaFire() 
	{
		System.out.println("About clicked");
		containerView.getChildren().clear();
		containerView.getChildren().add(aboutPane);
		containerView.setVisible(true);
    }

	@FXML
	@Override
	public void cancel() 
	{	
		
	}

	@FXML
	@Override
	public Object doIt() 
	{
		return null;
	}


}
