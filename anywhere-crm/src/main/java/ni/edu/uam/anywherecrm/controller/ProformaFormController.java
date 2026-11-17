package ni.edu.uam.anywherecrm.controller;

import java.util.ArrayList;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ni.edu.uam.anywherecrm.dao.GenericDAO;
import ni.edu.uam.anywherecrm.dao.ProformaDAO;
import ni.edu.uam.anywherecrm.model.Cliente;
import ni.edu.uam.anywherecrm.model.DetalleProforma;
import ni.edu.uam.anywherecrm.model.Producto;
import ni.edu.uam.anywherecrm.model.Proforma;
import ni.edu.uam.anywherecrm.util.Sesion;

// Formulario para crear una proforma, se elige el cliente y se agregan productos con su cantidad
public class ProformaFormController {

    @FXML private ComboBox<Cliente> cbCliente;
    @FXML private ComboBox<Producto> cbProducto;
    @FXML private TextField txtCantidad;
    @FXML private Label lblTotal;
    @FXML private TableView<DetalleProforma> tablaLineas;
    @FXML private TableColumn<DetalleProforma, String> colProducto;
    @FXML private TableColumn<DetalleProforma, Integer> colCantidad;
    @FXML private TableColumn<DetalleProforma, Double> colPrecio;
    @FXML private TableColumn<DetalleProforma, Double> colSubtotal;

    private final GenericDAO<Cliente> clienteDAO = new GenericDAO<>(Cliente.class);
    private final GenericDAO<Producto> productoDAO = new GenericDAO<>(Producto.class);
    private final ProformaDAO proformaDAO = new ProformaDAO();

    // Productos que lleva la proforma mientras se arma
    private final ObservableList<DetalleProforma> lineas = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cbCliente.setItems(FXCollections.observableArrayList(clienteDAO.obtenerTodos()));
        cbProducto.setItems(FXCollections.observableArrayList(productoDAO.obtenerTodos()));

        cbCliente.setConverter(new StringConverter<>() {
            @Override
            public String toString(Cliente cliente) {
                return cliente == null ? "" : cliente.getNombre();
            }

            @Override
            public Cliente fromString(String texto) {
                return null;
            }
        });

        cbProducto.setConverter(new StringConverter<>() {
            @Override
            public String toString(Producto producto) {
                if (producto == null) return "";
                return producto.getNombre() + " ($" + producto.getPrecio() + " | stock: " + producto.getStock() + ")";
            }

            @Override
            public Producto fromString(String texto) {
                return null;
            }
        });

        colProducto.setCellValueFactory(fila -> new SimpleStringProperty(fila.getValue().getProducto().getNombre()));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        tablaLineas.setItems(lineas);
        actualizarTotal();
    }

    // Agrega el producto elegido, si ya estaba en la lista, solo suma la cantidad
    @FXML
    private void handleAgregar() {
        Producto producto = cbProducto.getValue();
        if (producto == null) {
            mostrarAlerta("Falta el producto", "Elige un producto", Alert.AlertType.WARNING);
            return;
        }
        if (producto.getPrecio() == null) {
            mostrarAlerta("Producto sin precio", "Ese producto no tiene precio registrado", Alert.AlertType.WARNING);
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
        } catch (NumberFormatException e) {
            mostrarAlerta("Error de Formato", "La cantidad debe ser un número entero", Alert.AlertType.ERROR);
            return;
        }

        if (cantidad <= 0) {
            mostrarAlerta("Cantidad inválida", "La cantidad debe ser mayor que cero", Alert.AlertType.WARNING);
            return;
        }

        for (DetalleProforma linea : lineas) {
            if (linea.getProducto().getId().equals(producto.getId())) {
                linea.setCantidad(linea.getCantidad() + cantidad);
                linea.calcularSubtotal();
                tablaLineas.refresh();
                txtCantidad.clear();
                actualizarTotal();
                return;
            }
        }

        lineas.add(new DetalleProforma(producto, cantidad));
        txtCantidad.clear();
        actualizarTotal();
    }

    // Quita de la lista el producto seleccionado
    @FXML
    private void handleQuitar() {
        DetalleProforma seleccionada = tablaLineas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta("Sin selección", "Selecciona un producto de la tabla", Alert.AlertType.WARNING);
            return;
        }
        lineas.remove(seleccionada);
        actualizarTotal();
    }

    // Guarda la proforma con todos sus productos y cierra el formulario
    @FXML
    private void handleGuardar() {
        if (cbCliente.getValue() == null) {
            mostrarAlerta("Falta el cliente", "Elige el cliente de la proforma", Alert.AlertType.WARNING);
            return;
        }
        if (lineas.isEmpty()) {
            mostrarAlerta("Sin productos", "Agrega al menos un producto", Alert.AlertType.WARNING);
            return;
        }

        Proforma proforma = new Proforma();
        proforma.setCliente(cbCliente.getValue());
        proforma.setVendedor(Sesion.getUsuarioActual()); // Quien inició sesión queda como vendedor

        try {
            proformaDAO.guardarConDetalles(proforma, new ArrayList<>(lineas));
            mostrarAlerta(
                    "Proforma guardada",
                    String.format("Proforma #%d guardada por $%.2f", proforma.getId(), proforma.getMontoEstimado()),
                    Alert.AlertType.INFORMATION
            );
            cerrarVentana();
        } catch (RuntimeException e) {
            mostrarAlerta("Error de BD", "No se pudo guardar la proforma: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleCancelar() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage ventana = (Stage) tablaLineas.getScene().getWindow();
        ventana.close();
    }

    private void actualizarTotal() {
        double total = 0;
        for (DetalleProforma linea : lineas) {
            total += linea.getSubtotal();
        }
        lblTotal.setText(String.format("Total: $%.2f", total));
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}