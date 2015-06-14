/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package application.ui.main.settings;

import java.util.Locale;

import application.UiLoader;
import utils.SysProps;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;

public class SController 
{
    @FXML
    private ComboBox<Locale> languagePicker;

    @FXML
    private TextArea settingsTextArea;

    @FXML
    private TextArea helpLanguage;

    @FXML
    private Pane helpView;

    @FXML
    void helpLanguageShow() 
    {
    	//onMouseOver
    	helpLanguage.visibleProperty().set(!helpView.isVisible());
    }

    @FXML
    void helpLanguageHide() 
    {
		//onMouseOut
    	helpLanguage.visibleProperty().set(false);
    }

    @FXML
    void initialize() 
    {
        assert languagePicker != null : "fx:id=\"languagePicker\" was not injected: check your FXML file 'yactSettings.fxml'.";
        assert settingsTextArea != null : "fx:id=\"aboutTextArea\" was not injected: check your FXML file 'yactSettings.fxml'.";
        assert helpLanguage != null : "fx:id=\"helpLanguage\" was not injected: check your FXML file 'yactSettings.fxml'.";
        assert helpView != null : "fx:id=\"helpView\" was not injected: check your FXML file 'yactSettings.fxml'.";

        languagePicker.itemsProperty().set(SysProps.getImplementedLocales(UiLoader.getBundle()));
        languagePicker.setValue(SysProps.getSystemLocale());
    }
}


