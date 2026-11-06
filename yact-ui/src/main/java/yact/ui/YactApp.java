/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.ui;

import java.util.List;
import java.util.function.Function;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import yact.core.crypto.CryptoCapabilities;
import yact.core.crypto.CryptoCapabilities.Capability;

/**
 * Temporary entry point of the new client.
 *
 * <p>For now it only shows the crypto self-test, which proves that the
 * build, JavaFX and BouncyCastle work together. The existing FXML screens
 * from the legacy {@code src/} tree are migrated in a later milestone.
 */
public class YactApp extends Application
{
	public static final String VERSION = "1.0.0-SNAPSHOT";

	@Override
	public void start(Stage stage)
	{
		List<Capability> caps = CryptoCapabilities.check();

		TableView<Capability> table = new TableView<>(FXCollections.observableList(caps));
		table.getColumns().add(column("Status", c -> c.available() ? "OK" : "FAILED"));
		table.getColumns().add(column("Algorithm", Capability::name));
		table.getColumns().add(column("PQ", c -> c.postQuantum() ? "yes" : ""));
		table.getColumns().add(column("Purpose", Capability::purpose));
		table.getColumns().add(column("Detail", Capability::detail));
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

		Label summary = new Label(CryptoCapabilities.allAvailable(caps)
				? "All cryptographic primitives are available."
				: "Some cryptographic primitives are missing. YaCT cannot run safely.");
		BorderPane.setMargin(summary, new Insets(8));

		BorderPane root = new BorderPane(table);
		root.setTop(summary);

		stage.setTitle("YaCT " + VERSION + " – Crypto self-test");
		stage.setScene(new Scene(root, 900, 360));
		stage.show();
	}

	private static TableColumn<Capability, String> column(String title, Function<Capability, String> value)
	{
		TableColumn<Capability, String> col = new TableColumn<>(title);
		col.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
		return col;
	}

	public static void main(String[] args)
	{
		launch(args);
	}
}
