package ni.edu.uam.anywherecrm.reportes;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import ni.edu.uam.anywherecrm.ui.Ventanas;

import java.awt.image.BufferedImage;
import java.io.File;

// Vista previa a pantalla completa de cualquier reporte de JasperReports (Factura, Ventas, Inventario y Movimientos).
// Se muestra dentro de la misma ventana principal maximizada sin abrir ventanitas flotantes,
// permite cambiar de página, hacer zoom, exportar a PDF o Excel y regresar al módulo anterior.
public class VistaPrevia {

    private static final float ZOOM_MINIMO = 0.5f;
    private static final float ZOOM_MAXIMO = 2.5f;
    private static final float PASO_ZOOM = 0.25f;
    private static final Duration DURACION_FADE = Duration.millis(160);

    private final JasperPrint impresion; // El reporte ya lleno: aquí no se vuelve a consultar la base
    private final String nombreArchivo;  // Nombre sugerido al guardar, sin extensión
    private final int totalPaginas;
    private final BorderPane raiz;
    private final ImageView hoja = new ImageView();
    private final Label lblPagina = new Label();
    private final Label lblZoom = new Label();
    private final Button btnAnterior = new Button("◀");
    private final Button btnSiguiente = new Button("▶");
    private int pagina = 0;      // Las páginas de Jasper empiezan en 0
    private float zoom = 1.25f;  // 125 %: lectura nítida centrada en pantalla completa

    // Punto de entrada común para todos los reportes del sistema
    public static void mostrar(JasperPrint impresion, String titulo, String nombreArchivo, Window duena) {
        if (impresion.getPages().isEmpty()) {
            alerta(Alert.AlertType.INFORMATION, "Reporte vacío", "El reporte no tiene páginas para mostrar");
            return;
        }

        // Si viene desde un módulo de la ventana principal, se abre a pantalla completa dentro de la misma ventana
        if (duena instanceof Stage stage && stage.getScene() != null) {
            Scene escena = stage.getScene();
            Parent pantallaAnterior = escena.getRoot();
            String tituloAnterior = stage.getTitle();

            VistaPrevia previa = new VistaPrevia(impresion, titulo, nombreArchivo, () -> {
                escena.setRoot(pantallaAnterior);
                stage.setTitle(tituloAnterior);
                animarEntrada(pantallaAnterior);
            });

            stage.setTitle("Anywhere CRM - Vista previa: " + titulo);
            escena.setRoot(previa.raiz);
            Ventanas.maximizar(stage);
            animarEntrada(previa.raiz);
            previa.raiz.requestFocus();
            return;
        }

        // Respaldo: ventana nueva maximizada si no se recibió un Stage con escena
        Stage ventana = new Stage();
        VistaPrevia previa = new VistaPrevia(impresion, titulo, nombreArchivo, ventana::close);
        ventana.setTitle("Anywhere CRM - Vista previa: " + titulo);
        ventana.setScene(Ventanas.escenaCompleta(previa.raiz));
        Ventanas.maximizar(ventana);
        ventana.show();
        previa.raiz.requestFocus();
    }

    private VistaPrevia(JasperPrint impresion, String titulo, String nombreArchivo, Runnable alCerrar) {
        this.impresion = impresion;
        this.nombreArchivo = nombreArchivo;
        this.totalPaginas = MotorReportes.totalPaginas(impresion);

        // Barra superior en 3 zonas: regresar + título | paginación y zoom | exportar PDF y Excel
        Button btnRegresar = new Button("← Regresar");
        btnRegresar.getStyleClass().add("btn-volver-previa");
        btnRegresar.setOnAction(e -> alCerrar.run());

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("titulo-vista-previa");

        Button btnMenos = new Button("−");
        Button btnMas = new Button("+");
        Button btnPdf = new Button("Guardar PDF");
        Button btnExcel = new Button("Guardar Excel");

        btnAnterior.getStyleClass().add("btn-barra");
        btnSiguiente.getStyleClass().add("btn-barra");
        btnMenos.getStyleClass().add("btn-barra");
        btnMas.getStyleClass().add("btn-barra");

        btnPdf.getStyleClass().add("btn-peligro"); // Rojo como el ícono de PDF
        btnExcel.getStyleClass().add("btn-exito"); // Verde como Excel

        Region espacioIzq = new Region();
        Region espacioDer = new Region();
        HBox.setHgrow(espacioIzq, Priority.ALWAYS);
        HBox.setHgrow(espacioDer, Priority.ALWAYS);

        btnAnterior.setOnAction(e -> irA(pagina - 1));
        btnSiguiente.setOnAction(e -> irA(pagina + 1));
        btnMenos.setOnAction(e -> cambiarZoom(-PASO_ZOOM));
        btnMas.setOnAction(e -> cambiarZoom(PASO_ZOOM));
        btnPdf.setOnAction(e -> guardar("PDF", "*.pdf", ".pdf"));
        btnExcel.setOnAction(e -> guardar("Excel", "*.xlsx", ".xlsx"));

        HBox barra = new HBox(10,
                btnRegresar, new Separator(Orientation.VERTICAL), lblTitulo,
                espacioIzq,
                btnAnterior, lblPagina, btnSiguiente, new Separator(Orientation.VERTICAL),
                btnMenos, lblZoom, btnMas,
                espacioDer,
                btnPdf, btnExcel);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("barra-vista-previa");

        // La hoja va centrada sobre un lienzo gris a pantalla completa, como en un lector profesional de PDF
        hoja.getStyleClass().add("hoja");
        StackPane fondo = new StackPane(hoja);
        fondo.setPadding(new Insets(32, 32, 40, 32));
        fondo.setAlignment(Pos.TOP_CENTER);
        fondo.getStyleClass().add("fondo-hojas");

        ScrollPane desplazamiento = new ScrollPane(fondo);
        desplazamiento.setFitToWidth(true);  // Centra la hoja cuando es más angosta que el monitor
        desplazamiento.setFitToHeight(true); // Mantiene el fondo continuo en toda la altura de la pantalla
        desplazamiento.setPannable(true);    // Se puede arrastrar con el mouse

        raiz = new BorderPane(desplazamiento);
        raiz.setTop(barra);
        raiz.getStyleClass().add("vista-previa");
        raiz.setFocusTraversable(true);

        // Atajos de teclado activos solo mientras la vista previa está en pantalla (ESC regresa al módulo)
        raiz.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                alCerrar.run();
                e.consume();
            } else if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.PAGE_DOWN) {
                irA(pagina + 1);
                e.consume();
            } else if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.PAGE_UP) {
                irA(pagina - 1);
                e.consume();
            } else if (e.getCode() == KeyCode.ADD || e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.EQUALS) {
                cambiarZoom(PASO_ZOOM);
                e.consume();
            } else if (e.getCode() == KeyCode.SUBTRACT || e.getCode() == KeyCode.MINUS) {
                cambiarZoom(-PASO_ZOOM);
                e.consume();
            }
        });

        dibujar();
    }

    private void irA(int nueva) {
        if (nueva < 0 || nueva >= totalPaginas) return;
        pagina = nueva;
        dibujar();
    }

    private void cambiarZoom(float cambio) {
        zoom = Math.max(ZOOM_MINIMO, Math.min(ZOOM_MAXIMO, zoom + cambio));
        dibujar();
    }

    // Jasper dibuja la página con AWT y aquí se pasa a una imagen de JavaFX
    private void dibujar() {
        try {
            BufferedImage imagen = MotorReportes.dibujarPagina(impresion, pagina, zoom);
            hoja.setImage(aImagenJavaFx(imagen));
        } catch (JRException e) {
            alerta(Alert.AlertType.ERROR, "Error", "No se pudo dibujar la página: " + e.getMessage());
        }
        lblPagina.setText("Página " + (pagina + 1) + " de " + totalPaginas);
        lblZoom.setText(Math.round(zoom * 100) + " %");
        btnAnterior.setDisable(pagina == 0);
        btnSiguiente.setDisable(pagina == totalPaginas - 1);
    }

    // Copia los píxeles de AWT a JavaFX (así no hace falta la librería javafx-swing)
    private static WritableImage aImagenJavaFx(BufferedImage imagen) {
        int ancho = imagen.getWidth();
        int alto = imagen.getHeight();
        int[] pixeles = imagen.getRGB(0, 0, ancho, alto, null, 0, ancho);
        WritableImage resultado = new WritableImage(ancho, alto);
        resultado.getPixelWriter().setPixels(0, 0, ancho, alto,
                PixelFormat.getIntArgbInstance(), pixeles, 0, ancho);
        return resultado;
    }

    // El mismo reporte que se está viendo se guarda en el formato elegido
    private void guardar(String formato, String patron, String extension) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar reporte en " + formato);
        selector.setInitialFileName(nombreArchivo + extension);
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter(formato, patron));

        Window duenaDialogo = raiz.getScene() != null ? raiz.getScene().getWindow() : null;
        File destino = selector.showSaveDialog(duenaDialogo);
        if (destino == null) return; // El usuario canceló

        // Si el usuario borró la extensión, se le agrega para que el sistema sepa con qué abrirlo
        if (!destino.getName().toLowerCase().endsWith(extension)) {
            destino = new File(destino.getParentFile(), destino.getName() + extension);
        }
        try {
            MotorReportes.exportar(impresion, destino);
            alerta(Alert.AlertType.INFORMATION, formato + " generado",
                    "Reporte guardado en:\n" + destino.getAbsolutePath());
        } catch (JRException e) {
            alerta(Alert.AlertType.ERROR, "Error", "No se pudo guardar en " + formato + ": " + e.getMessage());
        }
    }

    private static void animarEntrada(Node nodo) {
        nodo.setOpacity(0.88);
        FadeTransition fade = new FadeTransition(DURACION_FADE, nodo);
        fade.setFromValue(0.88);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.play();
    }

    private static void alerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
