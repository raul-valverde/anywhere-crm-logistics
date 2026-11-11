package ni.edu.uam.anywherecrm.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.anywherecrm.dao.GenericDAO;
import ni.edu.uam.anywherecrm.model.Producto;

public class InventarioController {

    // Componentes mapeados desde el FXML
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtStock;

    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, Long> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, Double> colPrecio;
    @FXML private TableColumn<Producto, Integer> colStock;

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