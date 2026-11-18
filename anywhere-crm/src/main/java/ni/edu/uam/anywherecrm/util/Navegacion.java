package ni.edu.uam.anywherecrm.util;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;

// Ayuda para moverse entre ventanas sin repetir código en cada controlador
public class Navegacion {

    // El menú vive en la ventana principal, que se llama igual que en App
    private static final String TITULO_MENU = "Anywhere-CRM";

    // Cierra la ventana del módulo donde se presionó el botón y trae el menú al frente
    public static void volverAlMenu(ActionEvent evento) {
        Stage actual = (Stage) ((Node) evento.getSource()).getScene().getWindow();
        actual.close();
        for (Window ventana : new ArrayList<>(Window.getWindows())) {
            if (ventana instanceof Stage stage && TITULO_MENU.equals(stage.getTitle())) {
                stage.toFront();
            }
        }
    }
}