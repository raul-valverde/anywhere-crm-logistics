package ni.edu.uam.anywherecrm.controller;

import ni.edu.uam.anywherecrm.model.Orden;
import ni.edu.uam.anywherecrm.service.FacturaService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.anywherecrm.dao.ProformaDAO;
import ni.edu.uam.anywherecrm.model.DetalleProforma;
import ni.edu.uam.anywherecrm.model.Proforma;
import java.time.LocalDate;
import java.util.List;
// Pantalla para consultar proformas, ver sus productos y anularlas
public class ProformasController {
    @FXML private ComboBox<String> cbEstado;
    @FXML private Label lblTotal;
    @FXML private TableView<Proforma> tablaProformas;
    @FXML private TableColumn<Proforma, Long> colId;
    @FXML private TableColumn<Proforma, String> colCliente;
    @FXML private TableColumn<Proforma, LocalDate> colFecha;
    @FXML private TableColumn<Proforma, Double> colMonto;
    @FXML private TableColumn<Proforma, String> colEstado;
    @FXML private TableView<DetalleProforma> tablaDetalles;
    @FXML private TableColumn<DetalleProforma, String> colProducto;
    @FXML private TableColumn<DetalleProforma, Integer> colCantidad;
    @FXML private TableColumn<DetalleProforma, Double> colPrecio;
    @FXML private TableColumn<DetalleProforma, Double> colSubtotal;

    private final ProformaDAO proformaDAO = new ProformaDAO();
    private final FacturaService facturaService = new FacturaService();

    @FXML
    public void initialize() {
        // ... (el resto del código sigue igual)
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>
                ("fechaEmision"));
        colMonto.setCellValueFactory(new PropertyValueFactory<>
                ("montoEstimado"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
// El cliente es un objeto, así que mostramos solo su nombre
        colCliente.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getCliente() != null ?
                        fila.getValue().getCliente().getNombre() : ""));
        colProducto.setCellValueFactory(fila -> new
                SimpleStringProperty(fila.getValue().getProducto().getNombre()));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>
                ("cantidad"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>
                ("precioUnitario"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>
                ("subtotal"));
        cbEstado.getItems().addAll("TODAS", "PENDIENTE", "FACTURADA",
                "ANULADA");
        cbEstado.setValue("TODAS");
// Al elegir una proforma se muestran sus productos
        tablaProformas.getSelectionModel().selectedItemProperty().addListener((obs,
                                                                               anterior, nueva) -> {
            if (nueva != null) cargarDetalles(nueva);
        });
        cargarProformas();
    }
    // Carga las proformas según el estado elegido en el ComboBox
    @FXML
    private void cargarProformas() {
        String estado = cbEstado.getValue();
        List<Proforma> proformas = "TODAS".equals(estado)
                ? proformaDAO.obtenerTodas()
                : proformaDAO.obtenerPorEstado(estado);
        tablaProformas.setItems(FXCollections.observableArrayList(proformas));
        tablaDetalles.getItems().clear();
        lblTotal.setText("Total: $0.00");
    }
    private void cargarDetalles(Proforma proforma) {
        tablaDetalles.setItems(FXCollections.observableArrayList(proformaDAO.obtenerDetalles(proforma.getId())));
        double total = proforma.getMontoEstimado() == null ? 0 :
                proforma.getMontoEstimado();
        lblTotal.setText(String.format("Total: $%.2f", total));
    }
    // Solo se pueden anular proformas que siguen pendientes
    @FXML
    private void handleAnular() {
        Proforma seleccionada =
                tablaProformas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta("Sin selección", "Selecciona una proforma", Alert.AlertType.WARNING);
            return;
        }
        if (!"PENDIENTE".equals(seleccionada.getEstado())) {
            mostrarAlerta("No permitido", "Solo se pueden anular proformas PENDIENTES", Alert.AlertType.WARNING);
            return;
        }
        proformaDAO.cambiarEstado(seleccionada.getId(), "ANULADA");
        cargarProformas();
        mostrarAlerta("Listo", "Proforma anulada",
                Alert.AlertType.INFORMATION);
    }

    // Revisa en bodega si hay stock de todos los productos de la proforma
    @FXML
    private void handleVerificarStock() {
        Proforma seleccionada = tablaProformas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta("Sin selección", "Selecciona una proforma", Alert.AlertType.WARNING);
            return;
        }

        List<String> faltantes = facturaService.verificarStock(seleccionada.getId());
        if (faltantes.isEmpty()) {
            mostrarAlerta("Bodega", "Hay stock de todos los productos", Alert.AlertType.INFORMATION);
        } else {
            mostrarAlerta("Falta stock", String.join("\n", faltantes), Alert.AlertType.WARNING);
        }
    }

    // Convierte la proforma seleccionada en factura y descuenta la bodega
    @FXML
    private void handleFacturar() {
        Proforma seleccionada = tablaProformas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta("Sin selección", "Selecciona una proforma", Alert.AlertType.WARNING);
            return;
        }
        if (!"PENDIENTE".equals(seleccionada.getEstado())) {
            mostrarAlerta("No permitido", "Solo se pueden facturar proformas PENDIENTES", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Facturar la proforma #" + seleccionada.getId() + "? Se descontará de bodega.",
                ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setHeaderText(null);
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            Orden factura = facturaService.convertirProformaEnFactura(seleccionada.getId());
            cargarProformas();
            mostrarAlerta("Factura creada",
                    String.format("Factura #%d creada por $%.2f", factura.getId(), factura.getTotal()),
                    Alert.AlertType.INFORMATION);
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarAlerta("No se pudo facturar", e.getMessage(), Alert.AlertType.WARNING);
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
