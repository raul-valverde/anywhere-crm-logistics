package ni.edu.uam.anywherecrm.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.anywherecrm.dao.GenericDAO;
import ni.edu.uam.anywherecrm.model.Producto;
import javafx.beans.property.SimpleStringProperty;
import ni.edu.uam.anywherecrm.dao.InventarioDAO;
import ni.edu.uam.anywherecrm.model.MovimientoInventario;
import java.time.LocalDateTime;

public class InventarioController {

    // Componentes mapeados desde el FXML
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtStock;

    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, Long> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, Double> colPrecio;
    @FXML private TableColumn<Producto, Integer> colStock;// Componentes del formulario de movimientos
    @FXML private TextField txtCantidadMov;
    @FXML private TextField txtMotivo;
    @FXML private TableView<MovimientoInventario> tablaMovimientos;
    @FXML private TableColumn<MovimientoInventario, LocalDateTime> colMovFecha;
    @FXML private TableColumn<MovimientoInventario, String> colMovProducto;
    @FXML private TableColumn<MovimientoInventario, String> colMovTipo;
    @FXML private TableColumn<MovimientoInventario, Integer> colMovCantidad;
    @FXML private TableColumn<MovimientoInventario, String> colMovMotivo;

    private final InventarioDAO inventarioDAO = new InventarioDAO();

    // Instancia del DAO generico para gestionar Productos en PostgreSQL
    private final GenericDAO<Producto> productoDAO = new GenericDAO<>(Producto.class);
    private final ObservableList<Producto> listaProductos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Enlazar las columnas de la TableView con las propiedades del modelo Producto
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        cargarDatos();
        configurarTablaMovimientos();
        cargarMovimientos();
    }

    // Carga los productos guardados en la BD PostgreSQL a la tabla
    private void cargarDatos() {
        listaProductos.clear();
        listaProductos.addAll(productoDAO.obtenerTodos());
        tablaProductos.setItems(listaProductos);
    }

    @FXML
    private void handleGuardarProducto() {
        // 1. Validacion de campos vacios (Requisito de la rubrica)
        if (txtNombre.getText().trim().isEmpty() || txtPrecio.getText().trim().isEmpty() || txtStock.getText().trim().isEmpty()) {
            mostrarAlerta("Error de Validacion", "Todos los campos son obligatorios", Alert.AlertType.WARNING);
            return;
        }

        try {
            // 2. Validacion de tipos de datos numericos
            String nombre = txtNombre.getText().trim();
            Double precio = Double.parseDouble(txtPrecio.getText().trim());
            Integer stock = Integer.parseInt(txtStock.getText().trim());

            if (precio <= 0 || stock < 0) {
                mostrarAlerta("Error", "El precio y el stock deben ser valores positivos", Alert.AlertType.ERROR);
                return;
            }

            // 3. Crear y Persistir el objeto con Hibernate
            Producto producto = new Producto();
            producto.setNombre(nombre);
            producto.setPrecio(precio);
            producto.setStock(stock);

            productoDAO.guardar(producto);

            // 4. Limpiar UI y Recargar Tabla
            limpiarFormulario();
            cargarDatos();
            mostrarAlerta("Exito", "Producto guardado correctamente en la BD", Alert.AlertType.INFORMATION);

        } catch (NumberFormatException e) {
            mostrarAlerta("Error de Formato", "El precio y el stock deben ser numeros validos", Alert.AlertType.ERROR);
        } catch (Exception e) {
            mostrarAlerta("Error de BD", "No se pudo guardar el producto: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    private void configurarTablaMovimientos() {
        colMovFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colMovTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colMovCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colMovMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colMovProducto.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getProducto().getNombre()));
    }

    private void cargarMovimientos() {
        tablaMovimientos.setItems(FXCollections.observableArrayList(inventarioDAO.obtenerMovimientos()));
    }

    @FXML
    private void handleRegistrarEntrada() {
        registrarMovimiento("ENTRADA");
    }

    @FXML
    private void handleRegistrarSalida() {
        registrarMovimiento("SALIDA");
    }

    // Usa el producto seleccionado en la tabla de arriba
    private void registrarMovimiento(String tipo) {
        Producto producto = tablaProductos.getSelectionModel().getSelectedItem();
        if (producto == null) {
            mostrarAlerta("Sin selección", "Selecciona un producto de la tabla", Alert.AlertType.WARNING);
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidadMov.getText().trim());
        } catch (NumberFormatException e) {
            mostrarAlerta("Error de Formato", "La cantidad debe ser un número entero", Alert.AlertType.ERROR);
            return;
        }

        String motivo = txtMotivo.getText().trim();
        try {
            inventarioDAO.registrarMovimiento(producto.getId(), tipo, cantidad, motivo.isEmpty() ? tipo : motivo);
            txtCantidadMov.clear();
            txtMotivo.clear();
            cargarDatos();
            cargarMovimientos();
            mostrarAlerta("Éxito", tipo + " registrada para " + producto.getNombre(), Alert.AlertType.INFORMATION);
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarAlerta("No se pudo registrar", e.getMessage(), Alert.AlertType.WARNING);
        }
    }
    private void limpiarFormulario() {
        txtNombre.clear();
        txtPrecio.clear();
        txtStock.clear();
    }

    // Metodo auxiliar para desplegar ventanas de alerta emergentes
    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}