/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package application.ui.main;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import application.UiLoader;
import application.alterContainer.AlterContainer;
import application.closeContainer.CloseContainer;
import application.newContainer.NewContainer;
import application.openContainer.OpenContainer;
import application.ui.create.NCController;
import application.ui.create.NCStepController;

public class YactController
{	
	private SubWindow activeSubWindow;
	
	//Helper for exchangeing objects from subwindow to subwindow
	private Map<Class,Object> objectMap = new HashMap<Class,Object>();
	
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
	
	
	private SubWindow newContainerPane;
	private SubWindow newContainerStepPane;
	private Parent openContainerPane;
	private Parent closeContainerPane;
	private Parent alterContainerPane;
	private Parent aboutPane;
	private Parent settingsPane;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public YactController() throws IOException
	{
		objectMap.put(NewContainer.class, new NewContainer());
		objectMap.put(OpenContainer.class, new OpenContainer());
		objectMap.put(CloseContainer.class, new CloseContainer());
		objectMap.put(AlterContainer.class, new AlterContainer());
		
		newContainerPane = UiLoader.loadNewContainerUI();
		newContainerStepPane = UiLoader.loadNewContainerStepUI();
		openContainerPane = UiLoader.loadOpenContainerUI();
		closeContainerPane = UiLoader.loadCloseContainerUI();
		alterContainerPane = UiLoader.loadAlterContainerUI();
		aboutPane = UiLoader.loadAboutUI();
		settingsPane = UiLoader.loadSettingsUI();
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
		containerView.getChildren().add(newContainerPane.getParent());
		containerView.setVisible(true);
		activeSubWindow = newContainerPane;
	}
	
	@FXML
	private void newContainerStepPaneFire()
	{
		System.out.println("NewContainerStep 'next' clicked");
		containerView.getChildren().clear();
		containerView.getChildren().add(newContainerStepPane.getParent());
		containerView.setVisible(true);
		activeSubWindow = newContainerStepPane;
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
	void settingsAreaFire()
	{
		System.out.println("Settings clicked");
		containerView.getChildren().clear();
		containerView.getChildren().add(settingsPane);
		containerView.setVisible(true);
	}
	
	public void cancel()
	{
		
	}

	public void doIt()
    {
          Object activeObject = objectMap.get(activeSubWindow.getController().getExpectedClass());
          activeSubWindow.getController().doIt(activeObject.getClass().cast(activeObject));
    }



	public Class<?> getExpectedClass(Object object) 
	{
		return object.getClass();
	}

	public void setActiveSubWindow(SubWindow sw)
	{
		this.activeSubWindow = sw;
	}
	
}
