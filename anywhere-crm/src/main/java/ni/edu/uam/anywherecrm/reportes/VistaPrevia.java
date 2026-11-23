package ni.edu.uam.anywherecrm.reportes;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import java.awt.image.BufferedImage;
import java.io.File;
// Ventana de vista previa de cualquier reporte de Jasper.
// Muestra las páginas como imagen, permite pasar de página y hacer zoom,
// y desde aquí el usuario decide si lo guarda en PDF o en Excel.
// Se arma con código (sin FXML) para no tocar las vistas existentes.
public class VistaPrevia {
    private static final float ZOOM_MINIMO = 0.5f;
    private static final float ZOOM_MAXIMO = 2.5f;
    private static final float PASO_ZOOM = 0.25f;
    private final JasperPrint impresion; // El reporte ya lleno: aquí no se vuelve a consultar la base
    private final String nombreArchivo; // Nombre sugerido al guardar, sin extensión
    private final int totalPaginas;
    private final Stage ventana = new Stage();
    private final ImageView hoja = new ImageView();
    private final Label lblPagina = new Label();
    private final Label lblZoom = new Label();
    private final Button btnAnterior = new Button("◀");
    private final Button btnSiguiente = new Button("▶");
    private int pagina = 0; // Las páginas de Jasper empiezan en 0
    private float zoom = 1.25f; // 125 %: se lee bien sin ocupar toda la pantalla
    // Punto de entrada: los controladores solo llaman a este método
    public static void mostrar(JasperPrint impresion, String titulo, String
            nombreArchivo, Window duena) {
        if (impresion.getPages().isEmpty()) {
            alerta(Alert.AlertType.INFORMATION, "Reporte vacío", "El reporte no tiene páginas para mostrar");
            return;
        }
        new VistaPrevia(impresion, titulo, nombreArchivo, duena).ventana.show();
    }
    private VistaPrevia(JasperPrint impresion, String titulo, String
            nombreArchivo, Window duena) {
        this.impresion = impresion;
        this.nombreArchivo = nombreArchivo;
        this.totalPaginas = MotorReportes.totalPaginas(impresion);
// Barra de herramientas: páginas | zoom | exportar
        Button btnMenos = new Button("−");
        Button btnMas = new Button("+");
        Button btnPdf = new Button("Guardar PDF");
        Button btnExcel = new Button("Guardar Excel");
        Button btnCerrar = new Button("Cerrar");
        btnPdf.getStyleClass().add("btn-peligro"); // Rojo como el ícono de PDF (el color lo pone el CSS)
        btnExcel.getStyleClass().add("btn-exito"); // Verde como Excel
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS); // Empuja los botones de guardar a la derecha
        btnAnterior.setOnAction(e -> irA(pagina - 1));
        btnSiguiente.setOnAction(e -> irA(pagina + 1));
        btnMenos.setOnAction(e -> cambiarZoom(-PASO_ZOOM));
        btnMas.setOnAction(e -> cambiarZoom(PASO_ZOOM));
        btnPdf.setOnAction(e -> guardar("PDF", "*.pdf", ".pdf"));
        btnExcel.setOnAction(e -> guardar("Excel", "*.xlsx", ".xlsx"));
        btnCerrar.setOnAction(e -> ventana.close());
        ToolBar barra = new ToolBar(btnAnterior, lblPagina, btnSiguiente, new
                Separator(),
                btnMenos, lblZoom, btnMas, espacio, btnPdf, btnExcel,
                btnCerrar);
        barra.getStyleClass().add("barra-vista-previa");
// La hoja va centrada sobre un fondo gris, como en un lector de PDF
        hoja.getStyleClass().add("hoja");
        StackPane fondo = new StackPane(hoja);
        fondo.setPadding(new Insets(24));
        fondo.setAlignment(Pos.TOP_CENTER);
        fondo.getStyleClass().add("fondo-hojas");
        ScrollPane desplazamiento = new ScrollPane(fondo);
        desplazamiento.setFitToWidth(true); // Centra la hoja si es más angosta que la ventana
        desplazamiento.setPannable(true); // Se puede arrastrar con el mouse
        BorderPane raiz = new BorderPane(desplazamiento);
        raiz.setTop(barra);
        raiz.getStyleClass().add("vista-previa");
        Scene escena = new Scene(raiz, 900, 760);
// Atajos: flechas para cambiar de página, + y - para el zoom
        escena.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.RIGHT || e.getCode() ==
                    KeyCode.PAGE_DOWN) irA(pagina + 1);
            if (e.getCode() == KeyCode.LEFT || e.getCode() ==
                    KeyCode.PAGE_UP) irA(pagina - 1);
            if (e.getCode() == KeyCode.ADD || e.getCode() == KeyCode.PLUS)
                cambiarZoom(PASO_ZOOM);
            if (e.getCode() == KeyCode.SUBTRACT || e.getCode() ==
                    KeyCode.MINUS) cambiarZoom(-PASO_ZOOM);
        });
        ventana.setTitle("Vista previa - " + titulo);
        ventana.setScene(escena);
        if (duena != null) ventana.initOwner(duena); // Se cierra junto con el módulo que la abrió
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
            BufferedImage imagen = MotorReportes.dibujarPagina(impresion,
                    pagina, zoom);
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
        selector.getExtensionFilters().add(new
                FileChooser.ExtensionFilter(formato, patron));
        File destino = selector.showSaveDialog(ventana);
        if (destino == null) return; // El usuario canceló
// Si el usuario borró la extensión, se le agrega para que el sistema sepa con qué abrirlo
        if (!destino.getName().toLowerCase().endsWith(extension)) {
            destino = new File(destino.getParentFile(), destino.getName() +
                    extension);
        }
        try {
            MotorReportes.exportar(impresion, destino);
            alerta(Alert.AlertType.INFORMATION, formato + " generado",
                    "Reporte guardado en:\n" + destino.getAbsolutePath());

        }
    catch (JRException e) {
        alerta(Alert.AlertType.ERROR, "Error", "No se pudo guardar en " +formato + ": " + e.getMessage());
    }
}
        private static void alerta(Alert.AlertType tipo, String titulo, String
                mensaje) {
            Alert alert = new Alert(tipo);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        }
}
