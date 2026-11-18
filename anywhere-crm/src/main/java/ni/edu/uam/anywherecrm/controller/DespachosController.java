package ni.edu.uam.anywherecrm.controller;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import ni.edu.uam.anywherecrm.dao.DespachoDAO;
import ni.edu.uam.anywherecrm.model.Despacho;
import ni.edu.uam.anywherecrm.model.Orden;
import ni.edu.uam.anywherecrm.model.Usuario;
import java.time.LocalDate;
// Pantalla para despachar facturas: en camión con ruta o retiro en bodega
public class DespachosController {
    // Facturas que todavía no tienen despacho
    @FXML private TableView<Orden> tablaFacturas;
    @FXML private TableColumn<Orden, Long> colFacId;
    @FXML private TableColumn<Orden, LocalDate> colFacFecha;
    @FXML private TableColumn<Orden, String> colFacCliente;
    @FXML private TableColumn<Orden, Double> colFacTotal;
    // Formulario del despacho
    @FXML private ComboBox<String> cbTipo;
    @FXML private TextField txtDireccion;
    @FXML private ComboBox<Usuario> cbRepartidor;
    @FXML private TextField txtRuta;
    @FXML private DatePicker dpFecha;
    // Despachos creados
    @FXML private TableView<Despacho> tablaDespachos;
    @FXML private TableColumn<Despacho, Long> colDesId;
    @FXML private TableColumn<Despacho, String> colDesFactura;
    @FXML private TableColumn<Despacho, String> colDesCliente;
    @FXML private TableColumn<Despacho, String> colDesTipo;
    @FXML private TableColumn<Despacho, String> colDesEstado;
    @FXML private TableColumn<Despacho, LocalDate> colDesFecha;
    @FXML private TableColumn<Despacho, String> colDesRepartidor;
    @FXML private TableColumn<Despacho, String> colDesDireccion;
    private final DespachoDAO despachoDAO = new DespachoDAO();
    @FXML
    public void initialize() {
        colFacId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFacFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFacTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colFacCliente.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getCliente() != null ?
                        fila.getValue().getCliente().getNombre() : ""));
        colDesId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDesTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colDesEstado.setCellValueFactory(new PropertyValueFactory<>
                ("estado"));
        colDesFecha.setCellValueFactory(new PropertyValueFactory<>
                ("fechaDespacho"));
        colDesDireccion.setCellValueFactory(new PropertyValueFactory<>
                ("direccionEntrega"));
        colDesFactura.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getOrden() != null ? "#" +
                        fila.getValue().getOrden().getId() : ""));
        colDesCliente.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getOrden() != null &&
                        fila.getValue().getOrden().getCliente() != null
                        ? fila.getValue().getOrden().getCliente().getNombre()
                        : ""));
        colDesRepartidor.setCellValueFactory(fila -> new
                SimpleStringProperty(
                fila.getValue().getRepartidor() != null ?
                        fila.getValue().getRepartidor().getUsername() : "-"));
        cbTipo.getItems().addAll("RUTA", "RETIRO");
        cbTipo.setValue("RUTA");
// Un retiro en bodega no necesita dirección, repartidor ni ruta
        cbTipo.valueProperty().addListener((obs, anterior, nuevo) -> {
            boolean esRetiro = "RETIRO".equals(nuevo);
            txtDireccion.setDisable(esRetiro);
            cbRepartidor.setDisable(esRetiro);
            txtRuta.setDisable(esRetiro);
        });
        cbRepartidor.setConverter(new StringConverter<>() {
            @Override
            public String toString(Usuario usuario) {
                return usuario == null ? "" : usuario.getUsername();
            }
            @Override
            public Usuario fromString(String texto) {
                return null;
            }
        });
        dpFecha.setValue(LocalDate.now());
        cargarDatos();
    }
    @FXML
    private void cargarDatos() {
        tablaFacturas.setItems(FXCollections.observableArrayList(despachoDAO.obtenerFacturasSinDespacho()));
        tablaDespachos.setItems(FXCollections.observableArrayList(despachoDAO.obtenerDespachos()));
        cbRepartidor.setItems(FXCollections.observableArrayList(despachoDAO.obtenerRepartidores()));
    }
    // Crea el despacho de la factura seleccionada
    @FXML
    private void handleCrearDespacho() {
        Orden factura = tablaFacturas.getSelectionModel().getSelectedItem();
        if (factura == null) {
            mostrarAlerta("Sin selección", "Selecciona una factura de la tabla de arriba", Alert.AlertType.WARNING);
            return;
        }
        if (dpFecha.getValue() == null) {
            mostrarAlerta("Falta la fecha", "Elige la fecha del despacho",
                    Alert.AlertType.WARNING);
            return;
        }
        Despacho despacho = new Despacho();
        despacho.setOrden(factura);
        despacho.setTipo(cbTipo.getValue());
        despacho.setEstado("PENDIENTE");
        despacho.setFechaDespacho(dpFecha.getValue());
        if ("RUTA".equals(cbTipo.getValue())) {
            String direccion = txtDireccion.getText().trim();
            if (direccion.isEmpty()) {
                mostrarAlerta("Falta la dirección", "Escribe la dirección de entrega", Alert.AlertType.WARNING);
                return;
            }
            if (cbRepartidor.getValue() == null) {
                mostrarAlerta("Falta el repartidor", "Elige quién lleva el camión", Alert.AlertType.WARNING);
                return;
            }
            String ruta = txtRuta.getText().trim();
            despacho.setDireccionEntrega(direccion);
            despacho.setRepartidor(cbRepartidor.getValue());
            despacho.setRutaAsignada(ruta.isEmpty() ? "Por asignar" : ruta);
        } else {
            despacho.setDireccionEntrega("Retiro en bodega");
        }
        try {
            despachoDAO.guardar(despacho);
            limpiarFormulario();
            cargarDatos();
            mostrarAlerta("Despacho creado", "Despacho de la factura #" +
                    factura.getId() + " creado", Alert.AlertType.INFORMATION);
        } catch (RuntimeException e) {
            mostrarAlerta("Error de BD", "No se pudo crear el despacho: " +
                    e.getMessage(), Alert.AlertType.ERROR);
        }
    }
// PENDIENTE -> EN_CAMINO -> ENTREGADO (un retiro pasa directo a ENTREGADO)
    @FXML
    private void handleAvanzarEstado() {
        Despacho despacho =
                tablaDespachos.getSelectionModel().getSelectedItem();
        if (despacho == null) {
            mostrarAlerta("Sin selección", "Selecciona un despacho de la tabla de abajo", Alert.AlertType.WARNING);
            return;
        }
        try {
            String nuevoEstado = despachoDAO.avanzarEstado(despacho.getId());
            cargarDatos();
            mostrarAlerta("Estado actualizado", "El despacho #" +
                            despacho.getId() + " ahora está " + nuevoEstado,
                    Alert.AlertType.INFORMATION);
        } catch (IllegalStateException e) {
            mostrarAlerta("No permitido", e.getMessage(),
                    Alert.AlertType.WARNING);
        }
    }
    private void limpiarFormulario() {
        cbTipo.setValue("RUTA");
        txtDireccion.clear();
        txtRuta.clear();
        cbRepartidor.setValue(null);
        dpFecha.setValue(LocalDate.now());
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
