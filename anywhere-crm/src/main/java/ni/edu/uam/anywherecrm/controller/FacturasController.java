package ni.edu.uam.anywherecrm.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import ni.edu.uam.anywherecrm.dao.FacturaDAO;
import ni.edu.uam.anywherecrm.model.DetalleOrden;
import ni.edu.uam.anywherecrm.model.Orden;
import ni.edu.uam.anywherecrm.reportes.FacturaReporte;
import ni.edu.uam.anywherecrm.util.Navegacion;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

// Pantalla para consultar facturas y generar su PDF con JasperReports
public class FacturasController {

    @FXML private TableView<Orden> tablaFacturas;
    @FXML private TableColumn<Orden, Long> colId;
    @FXML private TableColumn<Orden, LocalDate> colFecha;
    @FXML private TableColumn<Orden, String> colCliente;
    @FXML private TableColumn<Orden, String> colVendedor;
    @FXML private TableColumn<Orden, Double> colTotal;
    @FXML private TableColumn<Orden, String> colEstado;

    @FXML private TableView<DetalleOrden> tablaDetalles;
    @FXML private TableColumn<DetalleOrden, String> colProducto;
    @FXML private TableColumn<DetalleOrden, Integer> colCantidad;
    @FXML private TableColumn<DetalleOrden, Double> colPrecio;
    @FXML private TableColumn<DetalleOrden, Double> colSubtotal;

    private final FacturaDAO facturaDAO = new FacturaDAO();
    private final FacturaReporte facturaReporte = new FacturaReporte();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        // Cliente y vendedor son objetos: mostramos solo su nombre
        colCliente.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getCliente() != null ? fila.getValue().getCliente().getNombre() : ""));
        colVendedor.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getVendedor() != null ? fila.getValue().getVendedor().getUsername() : ""));

        colProducto.setCellValueFactory(fila -> new SimpleStringProperty(fila.getValue().getProducto().getNombre()));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        // Al elegir una factura se muestran sus productos
        tablaFacturas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nueva) -> {
            if (nueva != null) {
                tablaDetalles.setItems(FXCollections.observableArrayList(facturaDAO.obtenerDetalles(nueva.getId())));
            }
        });

        cargarFacturas();
    }

    @FXML
    private void cargarFacturas() {
        tablaFacturas.setItems(FXCollections.observableArrayList(facturaDAO.obtenerFacturas()));
        tablaDetalles.getItems().clear();
    }

    // Pide dónde guardar y genera el PDF de la factura seleccionada
    @FXML
    private void handleGenerarPdf() {
        Orden factura = tablaFacturas.getSelectionModel().getSelectedItem();
        if (factura == null) {
            mostrarAlerta("Sin selección", "Selecciona una factura", Alert.AlertType.WARNING);
            return;
        }

        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar factura en PDF");
        selector.setInitialFileName("Factura-" + factura.getId() + ".pdf");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destino = selector.showSaveDialog(tablaFacturas.getScene().getWindow());
        if (destino == null) return; // El usuario canceló

        try {
            List<DetalleOrden> detalles = facturaDAO.obtenerDetalles(factura.getId());
            facturaReporte.generarPdf(factura, detalles, destino);
            mostrarAlerta("PDF generado", "Factura guardada en:\n" + destino.getAbsolutePath(), Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo generar el PDF: " + e.getMessage(), Alert.AlertType.ERROR);
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