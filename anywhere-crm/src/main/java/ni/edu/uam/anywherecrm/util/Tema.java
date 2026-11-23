package ni.edu.uam.anywherecrm.util;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Aplica el diseño (estilos.css) a TODAS las ventanas del programa sin modificar ningún FXML.
// 1. Escucha cada ventana que se abre (módulos, formularios, alertas, vista previa) y le pone la hoja de estilos.
// 2. Convierte los estilos escritos dentro del FXML (style="...") en clases CSS, porque un estilo en línea
// tiene prioridad sobre la hoja de estilos y no dejaría ver el diseño nuevo.
public final class Tema {
    private static final String HOJA = "/ni/edu/uam/anywherecrm/estilos.css";
    private static final String MARCA = "tema.vigilada"; // Evita registrar dos veces la misma ventana
    // Color de fondo que traía cada botón en el FXML -> clase CSS que lo reemplaza
    private static final Map<String, String> CLASE_POR_COLOR = Map.of(
            "#4caf50", "btn-exito", // Guardar / Crear (verde)
            "#e53935", "btn-peligro", // Eliminar / Anular (rojo)
            "#1976d2", "btn-primario", // Acciones principales (azul)
            "#2196f3", "btn-primario", // Iniciar sesión (azul)
            "#f57c00", "btn-aviso", // Registrar salida / Avanzar estado (naranja)
            "#757575", "btn-secundario"); // Cerrar sesión (gris)

    // Toma el primer color #RRGGBB del estilo; así también funcionan estilos mal escritos como "-fxbackground-color"
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Pattern TAMANO = Pattern.compile("font-size:\\s*(\\d+)px");

    private Tema() {
    }

    // Se llama una sola vez al iniciar (App.start): desde ahí cada ventana nueva recibe el tema
    public static void activar() {
        Window.getWindows().addListener((ListChangeListener<Window>) cambio -> {
            while (cambio.next()) {
                for (Window ventana : cambio.getAddedSubList()) {
                    vigilar(ventana);
                }
            }
        });
        new ArrayList<>(Window.getWindows()).forEach(Tema::vigilar);
    }

    // La ventana del login después muestra el menú en la misma ventana: por eso se escucha también el cambio de escena
    private static void vigilar(Window ventana) {
        if (ventana.getProperties().putIfAbsent(MARCA, Boolean.TRUE) != null)
            return;
        aplicar(ventana.getScene());
        ventana.sceneProperty().addListener((obs, anterior, nueva) ->
                aplicar(nueva));
    }

    public static void aplicar(Scene escena) {
        if (escena == null) return;
        String css = Tema.class.getResource(HOJA).toExternalForm();
        if (!escena.getStylesheets().contains(css)) {
            escena.getStylesheets().add(css);
        }
        Parent raiz = escena.getRoot();
        if (raiz == null) return;
        marcarPantalla(raiz);
        convertirEstilos(raiz);
    }

    // Login y menú tienen un fondo especial: se reconocen por un control que solo ellos tienen
    private static void marcarPantalla(Parent raiz) {
        if (raiz.lookup("#txtUsername") != null) agregarClase(raiz, "vistalogin");
        else if (raiz.lookup("#lblBienvenida") != null) agregarClase(raiz,
                "vista-menu");
        else agregarClase(raiz, "vista-modulo");
    }

    // Recorre todos los controles de la pantalla
    private static void convertirEstilos(Node nodo) {
        String estilo = nodo.getStyle();
        if (estilo != null && !estilo.isBlank()) {
            String clase = claseEquivalente(nodo, estilo);
            if (clase != null) {
                agregarClase(nodo, clase);
                nodo.setStyle(""); // El CSS se encarga desde ahora
            }
        }
        if (nodo instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) {
                convertirEstilos(hijo);
            }
        }
    }

    // Decide qué clase CSS corresponde a un estilo en línea; null = se deja como está (por ejemplo, el padding)
    private static String claseEquivalente(Node nodo, String estilo) {
        String texto = estilo.toLowerCase();
        if (nodo instanceof Button) {
            Matcher color = COLOR.matcher(texto);
            return color.find() ? Tema.CLASE_POR_COLOR.get(color.group()) : null;
        }
        if (nodo instanceof Label) {
            Matcher tamano = Tema.TAMANO.matcher(texto);
            if (tamano.find()) {
                int px = Integer.parseInt(tamano.group(1));
                if (px >= 20) return "titulo-principal"; // Login y menú
                if (px >= 18) return "titulo-vista"; // Título de cada módulo
                return "texto-total"; // Totales en $
            }
            if (texto.contains("#1976d2")) return "subtitulo"; // Secciones de Reportes
            if (texto.contains("#616161")) return "texto-ayuda"; // Mensajes grises (ej. "Sin ubicar")
            if (texto.contains("bold")) return "etiqueta-seccion";
        }
        return null;
    }

    private static void agregarClase(Node nodo, String clase) {
        if (!nodo.getStyleClass().contains(clase)) {
            nodo.getStyleClass().add(clase);
        }
    }
}
