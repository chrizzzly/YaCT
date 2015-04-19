/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package application.ui.create;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

public class NCController 
{

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private ToggleGroup groupNewContainer;

    @FXML
    private PasswordField pwFieldRepeat;

    @FXML
    private ComboBox<?> hashField;

    @FXML
    private PasswordField pwField;

    @FXML
    private ComboBox<?> sizeFieldUnit;

    @FXML
    private TextField pathField;

    @FXML
    private ComboBox<?> modeField;

    @FXML
    private TextField sizeField;

    @FXML
    private ComboBox<?> algField;

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

    }
}
