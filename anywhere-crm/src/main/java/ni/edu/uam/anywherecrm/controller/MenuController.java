package ni.edu.uam.anywherecrm.controller;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ni.edu.uam.anywherecrm.dao.DespachoDAO;
import ni.edu.uam.anywherecrm.dao.GenericDAO;
import ni.edu.uam.anywherecrm.dao.ProformaDAO;
import ni.edu.uam.anywherecrm.dao.ReportesDAO;
import ni.edu.uam.anywherecrm.model.Cliente;
import ni.edu.uam.anywherecrm.model.Despacho;
import ni.edu.uam.anywherecrm.model.Proforma;
import ni.edu.uam.anywherecrm.model.Usuario;
import ni.edu.uam.anywherecrm.ui.Ventanas;
import ni.edu.uam.anywherecrm.util.Sesion;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

// Menú principal: habilita solo los módulos que le tocan a cada rol
public class MenuController {
    @FXML private Label lblBienvenida;
    @FXML private Button btnClientes;
    @FXML private Button btnProformas;
    @FXML private Button btnInventario;
    @FXML private Button btnReportes;
    @FXML private Button btnFacturas;
    @FXML private Button btnDespachos;

    // Dashboard: fecha, tarjetas resumen, gráfico y actividad reciente
    @FXML private Label lblFecha;
    @FXML private Label lblKpiClientes;
    @FXML private Label lblKpiClientesDetalle;
    @FXML private Label lblKpiProformas;
    @FXML private Label lblKpiProformasDetalle;
    @FXML private Label lblKpiDespachos;
    @FXML private Label lblKpiDespachosDetalle;
    @FXML private BarChart<String, Number> chartTopProductos;
    @FXML private Label lblSinDatosGrafico;
    @FXML private VBox boxActividad;

    private static final int MAX_BARRAS = 6;
    private static final int MAX_ACTIVIDAD = 6;
    private static final DateTimeFormatter FORMATO_CORTO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Datos del dashboard que se consultan juntos en segundo plano
    private record Indicadores(long clientes, long proformas, long pendientes, long despachosActivos,
                               long entregados, List<Object[]> topProductos, List<Proforma> recientes) {
    }

    @FXML
    public void initialize() {
        Usuario usuario = Sesion.getUsuarioActual();
        lblBienvenida.setText("Bienvenido, " + usuario.getUsername() + " (" +
                usuario.getRol() + ")");
// Permisos por rol
        btnClientes.setDisable(!Sesion.tieneRol("ADMIN", "VENDEDOR"));
        btnProformas.setDisable(!Sesion.tieneRol("ADMIN", "VENDEDOR"));
        btnInventario.setDisable(!Sesion.tieneRol("ADMIN", "BODEGUERO"));
        btnReportes.setDisable(!Sesion.tieneRol("ADMIN", "DESPACHADOR"));
        btnFacturas.setDisable(!Sesion.tieneRol("ADMIN", "VENDEDOR"));
        btnDespachos.setDisable(!Sesion.tieneRol("ADMIN", "DESPACHADOR"));

        String fecha = LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es")));
        lblFecha.setText("Resumen general · " + Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1));

        cargarIndicadores();
    }

    // Tarjetas, gráfico y actividad: solo lectura con los DAO existentes, en segundo plano para no congelar el menú
    private void cargarIndicadores() {
        Task<Indicadores> tarea = new Task<>() {
            @Override
            protected Indicadores call() {
                long clientes = new GenericDAO<>(Cliente.class).obtenerTodos().size();
                ProformaDAO proformaDAO = new ProformaDAO();
                List<Proforma> proformas = proformaDAO.obtenerTodas(); // Ya vienen de la más nueva a la más vieja
                long pendientes = proformas.stream().filter(p -> "PENDIENTE".equals(p.getEstado())).count();
                List<Despacho> despachos = new DespachoDAO().obtenerDespachos();
                long entregados = despachos.stream()
                        .filter(d -> "ENTREGADO".equals(d.getEstado())).count();
                List<Object[]> top = new ReportesDAO().obtenerProductosMasVendidos(); // Misma consulta HQL de Reportes
                return new Indicadores(clientes, proformas.size(), pendientes, despachos.size() - entregados,
                        entregados, top, proformas.subList(0, Math.min(MAX_ACTIVIDAD, proformas.size())));
            }
        };
        tarea.setOnSucceeded(e -> mostrarIndicadores(tarea.getValue()));
        tarea.setOnFailed(e -> {
            lblKpiClientesDetalle.setText("Sin conexión con la base de datos");
            lblKpiProformasDetalle.setText("Sin conexión con la base de datos");
            lblKpiDespachosDetalle.setText("Sin conexión con la base de datos");
            lblSinDatosGrafico.setText("No se pudieron cargar los datos");
            boxActividad.getChildren().setAll(textoAyuda("No se pudo cargar la actividad"));
        });
        Thread hilo = new Thread(tarea, "indicadores-menu");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarIndicadores(Indicadores datos) {
        lblKpiClientes.setText(String.valueOf(datos.clientes()));
        lblKpiClientesDetalle.setText("Cartera total de clientes");
        lblKpiProformas.setText(String.valueOf(datos.pendientes()));
        lblKpiProformasDetalle.setText("De " + datos.proformas() + " proformas registradas");
        lblKpiDespachos.setText(String.valueOf(datos.despachosActivos()));
        lblKpiDespachosDetalle.setText(datos.entregados() + " entregados a la fecha");
        mostrarGrafico(datos.topProductos());
        mostrarActividad(datos.recientes());
    }

    // Gráfico de barras con los productos más vendidos
    private void mostrarGrafico(List<Object[]> top) {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (Object[] fila : top.subList(0, Math.min(MAX_BARRAS, top.size()))) {
            String nombre = String.valueOf(fila[0]);
            Number cantidad = fila[1] instanceof Number n ? n : 0;
            XYChart.Data<String, Number> barra = new XYChart.Data<>(recortar(nombre, 18), cantidad);
            barra.nodeProperty().addListener((obs, anterior, nodo) -> {
                if (nodo != null) Tooltip.install(nodo, new Tooltip(nombre + ": " + cantidad + " unidades"));
            });
            serie.getData().add(barra);
        }
        chartTopProductos.getData().setAll(List.of(serie));
        ajustarEjeVertical(serie);
        lblSinDatosGrafico.setVisible(serie.getData().isEmpty());
        lblSinDatosGrafico.setText("Aún no hay ventas registradas");
    }

    // Eje Y con marcas enteras y "redondas" (1, 2, 5, 10, 20, 50...) y algo de aire sobre la barra más alta
    private void ajustarEjeVertical(XYChart.Series<String, Number> serie) {
        double maximo = serie.getData().stream()
                .mapToDouble(d -> d.getYValue().doubleValue()).max().orElse(0);
        double crudo = Math.max(1, maximo / 5);
        double potencia = Math.pow(10, Math.floor(Math.log10(crudo)));
        double fraccion = crudo / potencia;
        double unidad = (fraccion <= 1 ? 1 : fraccion <= 2 ? 2 : fraccion <= 5 ? 5 : 10) * potencia;
        NumberAxis eje = (NumberAxis) chartTopProductos.getYAxis();
        eje.setAutoRanging(false);
        eje.setLowerBound(0);
        eje.setTickUnit(unidad);
        eje.setUpperBound(Math.max(unidad, Math.ceil(maximo * 1.1 / unidad) * unidad));
    }

    // Lista compacta con las últimas proformas
    private void mostrarActividad(List<Proforma> recientes) {
        boxActividad.getChildren().clear();
        if (recientes.isEmpty()) {
            boxActividad.getChildren().add(textoAyuda("Todavía no hay proformas registradas"));
            return;
        }
        for (Proforma proforma : recientes) {
            boxActividad.getChildren().add(filaActividad(proforma));
        }
    }

    private HBox filaActividad(Proforma proforma) {
        String cliente = proforma.getCliente() == null ? "Sin cliente" : proforma.getCliente().getNombre();
        Label titulo = new Label("#" + proforma.getId() + " · " + recortar(cliente, 26));
        titulo.getStyleClass().add("activity-title");
        String fecha = proforma.getFechaEmision() == null ? "" : proforma.getFechaEmision().format(FORMATO_CORTO);
        double monto = proforma.getMontoEstimado() == null ? 0 : proforma.getMontoEstimado();
        Label detalle = new Label(fecha + "  ·  " + String.format("$%,.2f", monto));
        detalle.getStyleClass().add("activity-caption");
        VBox textos = new VBox(2, titulo, detalle);

        String estado = proforma.getEstado() == null ? "—" : proforma.getEstado();
        Label badge = new Label(estado);
        badge.getStyleClass().addAll("status-badge", "status-" + estado.toLowerCase());

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox fila = new HBox(10, textos, espacio, badge);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("activity-row");
        return fila;
    }

    private static Label textoAyuda(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("help-text");
        return etiqueta;
    }

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }

    @FXML private void abrirClientes() { abrirVentana("clientes-view.fxml",
            "Clientes"); }
    @FXML private void abrirProformas() { abrirVentana("proformas-view.fxml",
            "Proformas"); }
    @FXML private void abrirInventario() { abrirVentana("inventario-view.fxml", "Inventario"); }
    @FXML private void abrirReportes() { abrirVentana("reportes-view.fxml",
            "Reportes"); }
    @FXML private void abrirFacturas() { abrirVentana("facturas-view.fxml",
            "Facturas"); }
    @FXML private void abrirDespachos() { abrirVentana("despachos-view.fxml",
            "Despachos"); }

    // Abre el módulo en una ventana aparte (maximizada) para que el menú siga abierto
    private void abrirVentana(String fxml, String titulo) {
        try {
            Ventanas.abrirModulo(fxml, titulo);
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir " + titulo + ": " +
                    e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    // Vuelve a la pantalla de login (cierra también los módulos que hayan quedado abiertos)
    @FXML
    private void handleCerrarSesion() {
        try {
            Ventanas.cerrarSesion();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo volver al login: " +
                    e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType
            tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
