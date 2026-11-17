package ni.edu.uam.anywherecrm.controller;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
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
    @FXML
    public void initialize() {
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
    // Abre el formulario de nueva proforma y recarga la lista al cerrarlo
    @FXML
    private void handleNuevaProforma() {
        try {
            FXMLLoader loader = new
                    FXMLLoader(getClass().getResource("/ni/edu/uam/anywherecrm/proforma-formview.fxml"));
            Stage ventana = new Stage();
            ventana.setTitle("Anywhere CRM - Nueva proforma");
            ventana.setScene(new Scene(loader.load(), 750, 550));
            ventana.initModality(Modality.APPLICATION_MODAL); // Bloquea esta pantalla hasta cerrar el formulario
            ventana.showAndWait();
            cargarProformas();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir el formulario: " +
                    e.getMessage(), Alert.AlertType.ERROR);
        }
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
    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType
            tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
