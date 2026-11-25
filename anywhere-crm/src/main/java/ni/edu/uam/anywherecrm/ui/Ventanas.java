package ni.edu.uam.anywherecrm.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import ni.edu.uam.anywherecrm.util.Sesion;

import java.io.IOException;
import java.util.ArrayList;

// Navegación de toda la aplicación con UNA sola ventana (Stage) y UNA sola Scene:
// login, menú y módulos se intercambian con scene.setRoot(...), sin crear ventanas nuevas
// y sin volver a redimensionar/maximizar. Así no hay parpadeo al cambiar de pantalla.
public final class Ventanas {

    // Título de la ventana en login y menú (el mismo que usa App)
    public static final String TITULO_PRINCIPAL = "Anywhere-CRM";
    private static final String RUTA = "/ni/edu/uam/anywherecrm/";
    private static final Duration DURACION_FADE = Duration.millis(160);

    // La única ventana de la sesión (la registra App al iniciar)
    private static Stage principal;

    private Ventanas() {
    }

    public static void registrarPrincipal(Stage stage) {
        principal = stage;
    }

    public static Stage getPrincipal() {
        return principal;
    }

    // Área útil del monitor principal (sin la barra de tareas)
    public static Rectangle2D areaVisible() {
        return Screen.getPrimary().getVisualBounds();
    }

    // Escena del tamaño exacto del monitor: se crea UNA vez, al arrancar (App)
    public static Scene escenaCompleta(Parent raiz) {
        Rectangle2D area = areaVisible();
        return new Scene(raiz, area.getWidth(), area.getHeight());
    }

    // Maximizado real. Si la ventana ya está maximizada no se toca nada
    // (volver a fijar tamaño o maximizar es lo que provocaba el "achicar y volver a crecer").
    public static void maximizar(Stage stage) {
        if (stage.isMaximized()) return;
        Rectangle2D area = areaVisible();
        stage.setX(area.getMinX());
        stage.setY(area.getMinY());
        stage.setWidth(area.getWidth());
        stage.setHeight(area.getHeight());
        stage.setMaximized(true);
    }

    public static FXMLLoader cargador(String fxml) {
        return new FXMLLoader(Ventanas.class.getResource(RUTA + fxml));
    }

    // Núcleo de la navegación: carga la vista y la pone como raíz de la escena existente.
    // Si el FXML falla al cargar, la pantalla actual se queda tal cual.
    public static void navegar(Stage stage, String fxml, String titulo) throws IOException {
        Parent raiz = cargador(fxml).load();
        stage.setTitle(titulo);
        if (stage.getScene() == null) {
            stage.setScene(escenaCompleta(raiz)); // Solo ocurre si la ventana aún no tenía escena
        } else {
            stage.getScene().setRoot(raiz); // Misma Scene, mismo tamaño: solo cambia el contenido
        }
        maximizar(stage); // No hace nada si ya está maximizada
        animarEntrada(raiz);
    }

    // Login -> menú y similares (mantiene la firma que usa LoginController)
    public static void cambiarContenido(Stage stage, String fxml) throws IOException {
        navegar(stage, fxml, TITULO_PRINCIPAL);
    }

    // Vuelve al menú principal dentro de la misma ventana
    public static void irAlMenu() throws IOException {
        navegar(ventanaPrincipal(), "menu-view.fxml", TITULO_PRINCIPAL);
    }

    // Muestra un módulo dentro de la misma ventana (mantiene la firma que usa MenuController)
    public static Stage abrirModulo(String fxml, String titulo) throws IOException {
        Stage stage = ventanaPrincipal();
        navegar(stage, fxml, "Anywhere CRM - " + titulo);
        return stage;
    }

    // Cierra la sesión: cierra las ventanas auxiliares (p. ej. vistas previas de reportes)
    // y muestra el login en la misma ventana principal
    public static void cerrarSesion() throws IOException {
        Sesion.cerrar();
        Stage stage = ventanaPrincipal();
        for (Window ventana : new ArrayList<>(Window.getWindows())) {
            if (ventana instanceof Stage otra && otra != stage) {
                otra.close();
            }
        }
        navegar(stage, "login-view.fxml", TITULO_PRINCIPAL);
        stage.toFront();
    }

    private static Stage ventanaPrincipal() {
        if (principal == null) {
            throw new IllegalStateException("La ventana principal no fue registrada en App");
        }
        return principal;
    }

    // Entrada suave del contenido: 0.85 -> 1.0 de opacidad en 160 ms.
    // En pantallas con Isla Dinámica solo se anima la capa de contenido (.content-layer): la isla queda fija.
    private static void animarEntrada(Parent raiz) {
        Node capa = raiz.lookup(".content-layer");
        Node objetivo = capa != null ? capa : raiz;
        objetivo.setOpacity(0.85);
        FadeTransition fade = new FadeTransition(DURACION_FADE, objetivo);
        fade.setFromValue(0.85);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.play();
    }
}
