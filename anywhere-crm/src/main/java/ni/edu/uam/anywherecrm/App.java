package ni.edu.uam.anywherecrm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ni.edu.uam.anywherecrm.util.DatosPrueba;
import ni.edu.uam.anywherecrm.util.Tema;


import ni.edu.uam.anywherecrm.util.Tema;
import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Tema.activar(); // Diseño CSS para todas las ventanas (login, menú, módulos, alertas y vista previa)
        DatosPrueba.cargarSiEstaVacio(); // Solo inserta datos donde las tablas están vacías
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("login-view.fxml"));//Para la ventana que va a abrir de primero
        Scene scene = new Scene(fxmlLoader.load());//El tamaño del login
        stage.setTitle("Anywhere-CRM");
        stage.setScene(scene);
        stage.show();
    }
}
