package ni.edu.uam.anywherecrm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("login-view.fxml"));//Para la ventana que va a abrir de primero
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Anywhere-CRM");
        stage.setScene(scene);
        stage.show();

    }
}
