package ni.edu.uam.anywherecrm.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import net.sf.jasperreports.engine.JasperPrint;
import ni.edu.uam.anywherecrm.dao.DespachoDAO;
import ni.edu.uam.anywherecrm.dao.FacturaDAO;
import ni.edu.uam.anywherecrm.dao.ReportesDAO;
import ni.edu.uam.anywherecrm.model.Despacho;
import ni.edu.uam.anywherecrm.model.Orden;
import ni.edu.uam.anywherecrm.reportes.ReporteDespachos;
import ni.edu.uam.anywherecrm.reportes.ReporteVentas;
import ni.edu.uam.anywherecrm.reportes.VistaPrevia;
import ni.edu.uam.anywherecrm.ui.CeldaResultado;
import ni.edu.uam.anywherecrm.util.Navegacion;

import java.time.LocalDate;
import java.util.List;

public class ReportesController {

    @FXML
    private ListView<String> listTopProductos;
    @FXML
    private ListView<String> listVentasVendedor;
    @FXML
    private DatePicker dpDesde;
    @FXML
    private DatePicker dpHasta;
    @FXML
    private DatePicker dpDespachoDesde;
    @FXML
    private DatePicker dpDespachoHasta;
    @FXML
    private ComboBox<String> cbEstadoDespacho;

    private final FacturaDAO facturaDAO = new FacturaDAO();
    private final ReporteVentas reporteVentas = new ReporteVentas();
    private final ReportesDAO reportesDAO = new ReportesDAO();
    private final DespachoDAO despachoDAO = new DespachoDAO();
    private final ReporteDespachos reporteDespachos = new ReporteDespachos();

    @FXML
    public void initialize() {
        // Solo presentación: cada resultado se muestra como un ítem limpio con el valor en negrita
        listTopProductos.setCellFactory(lista -> new CeldaResultado());
        listVentasVendedor.setCellFactory(lista -> new CeldaResultado());
        cargarReportes();

        // Por defecto ventas: desde el primer día del mes hasta hoy
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());

        // Por defecto despachos: desde el primer día del mes hasta hoy
        dpDespachoDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpDespachoHasta.setValue(LocalDate.now());
        cbEstadoDespacho.getItems().addAll("TODOS", "PENDIENTE", "EN_CAMINO", "ENTREGADO");
        cbEstadoDespacho.setValue("TODOS");
    }

    // Llena las dos listas con las consultas HQL de ReportesDAO
    private void cargarReportes() {
        // Consulta HQL 1: productos más vendidos
        List<Object[]> topProd = reportesDAO.obtenerProductosMasVendidos();
        for (Object[] fila : topProd) {
            listTopProductos.getItems().add("Producto: " + fila[0] + " | Total Vendido: " + fila[1]);
        }
        // Consulta HQL 2: ventas totales por vendedor
        List<Object[]> ventasVend = reportesDAO.obtenerVentasPorVendedor();
        for (Object[] fila : ventasVend) {
            listVentasVendedor.getItems().add("Vendedor: " + fila[0] + " | Monto Acumulado: $" + fila[1]);
        }
    }

    // Genera el PDF de ventas entre las dos fechas elegidas
    @FXML
    private void handlePdfVentas() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        if (desde == null || hasta == null) {
            mostrarAlerta("Faltan fechas", "Elige la fecha inicial y la final", Alert.AlertType.WARNING);
            return;
        }
        if (desde.isAfter(hasta)) {
            mostrarAlerta("Fechas inválidas", "La fecha inicial no puede ser mayor que la final", Alert.AlertType.WARNING);
            return;
        }
        List<Orden> facturas = facturaDAO.obtenerFacturasEntre(desde, hasta);
        if (facturas.isEmpty()) {
            mostrarAlerta("Sin ventas", "No hay facturas en ese rango de fechas", Alert.AlertType.INFORMATION);
            return;
        }
        try {
            // Se arma el reporte una vez y la vista previa decide el formato (PDF o Excel)
            JasperPrint impresion = reporteVentas.crear(desde, hasta, facturas);
            VistaPrevia.mostrar(impresion, "Ventas del " + desde + " al " + hasta,
                    "Ventas-" + desde + "-a-" + hasta,
                    dpDesde.getScene().getWindow());
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo generar el reporte: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // Genera el reporte de despachos y entregas logísticas entre las fechas y estado elegidos
    @FXML
    private void handlePdfDespachos() {
        LocalDate desde = dpDespachoDesde.getValue();
        LocalDate hasta = dpDespachoHasta.getValue();
        String estado = cbEstadoDespacho.getValue();

        if (desde == null || hasta == null) {
            mostrarAlerta("Faltan fechas", "Elige la fecha inicial y la final", Alert.AlertType.WARNING);
            return;
        }
        if (desde.isAfter(hasta)) {
            mostrarAlerta("Fechas inválidas", "La fecha inicial no puede ser mayor que la final", Alert.AlertType.WARNING);
            return;
        }

        List<Despacho> despachos = despachoDAO.obtenerDespachosPorFiltro(desde, hasta, estado);
        if (despachos.isEmpty()) {
            mostrarAlerta("Sin despachos", "No hay despachos registrados para los filtros seleccionados", Alert.AlertType.INFORMATION);
            return;
        }

        try {
            JasperPrint impresion = reporteDespachos.crear(desde, hasta, estado, despachos);
            VistaPrevia.mostrar(impresion,
                    "Despachos del " + desde + " al " + hasta,
                    "Despachos-" + desde + "-a-" + hasta,
                    dpDespachoDesde.getScene().getWindow());
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo generar el reporte: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // Cierra este módulo y deja el menú al frente
    @FXML
    private void volverAlMenu(ActionEvent evento) {
        Navegacion.volverAlMenu(evento);
    }
}