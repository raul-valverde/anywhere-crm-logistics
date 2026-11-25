package ni.edu.uam.anywherecrm;

import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import ni.edu.uam.anywherecrm.ui.Ventanas;
import ni.edu.uam.anywherecrm.util.DatosPrueba;
import ni.edu.uam.anywherecrm.util.Tema;

import java.io.IOException;

public class App extends Application {

    // Hoja de estilos global "Deep Navy & Steel"
    private static final String ESTILOS = "/ni/edu/uam/anywherecrm/styles.css";

    @Override
    public void start(Stage stage) throws IOException {
        Tema.activar(); // Diseño CSS para todas las ventanas (login, menú, módulos, alertas y vista previa)
        activarEstilosGlobales(); // styles.css + ventanas maximizadas en todo el programa
        DatosPrueba.cargarSiEstaVacio(); // Solo inserta datos donde las tablas están vacías
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("login-view.fxml"));//Para la ventana que va a abrir de primero
        // La escena nace del tamaño del área visible del monitor: así no queda franja del escritorio a la derecha
        Scene scene = Ventanas.escenaCompleta(fxmlLoader.load());
        stage.setTitle(Ventanas.TITULO_PRINCIPAL);
        Ventanas.registrarPrincipal(stage); // Única ventana de la sesión: login, menú y módulos viven aquí
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        Ventanas.maximizar(stage); // Posición + tamaño del área visible y luego maximizado (no pantalla completa)
        stage.show();
    }

    // Aplica styles.css a TODAS las escenas, incluidas las que se crean por código
    // (alertas y la vista previa de reportes), y abre maximizada toda ventana que no sea un diálogo.
    private static void activarEstilosGlobales() {
        Window.getWindows().addListener((ListChangeListener<Window>) cambio -> {
            while (cambio.next()) {
                for (Window ventana : cambio.getAddedSubList()) {
                    prepararVentana(ventana);
                }
            }
        });
    }

    private static void prepararVentana(Window ventana) {
        agregarEstilos(ventana.getScene());
        ventana.sceneProperty().addListener((obs, anterior, nueva) -> agregarEstilos(nueva));
        if (ventana instanceof Stage stage) {
            stage.addEventHandler(WindowEvent.WINDOW_SHOWING, e -> {
                agregarEstilos(stage.getScene());
                // Las alertas (Alert/Dialog) envuelven un DialogPane: esas se dejan de su tamaño normal
                boolean esDialogo = stage.getScene() != null
                        && (stage.getScene().getRoot() instanceof DialogPane
                        || stage.getScene().getRoot().lookup(".dialog-pane") != null);
                if (!esDialogo) {
                    Ventanas.maximizar(stage);
                }
            });
        }
    }

    private static void agregarEstilos(Scene escena) {
        if (escena == null) return;
        String css = App.class.getResource(ESTILOS).toExternalForm();
        if (!escena.getStylesheets().contains(css)) {
            escena.getStylesheets().add(css);
        }
    }
}
