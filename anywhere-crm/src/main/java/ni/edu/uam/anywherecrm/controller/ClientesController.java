package ni.edu.uam.anywherecrm.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.anywherecrm.dao.GenericDAO;
import ni.edu.uam.anywherecrm.model.Cliente;
//NOTA IMPORTANTE!! SI UN CLIENTE YA TIENE PROFORMAS O FACTURAS NO DEJA ELIMINARLO
// Controlador para registrar, buscar, editar y eliminar clientes
public class ClientesController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextField txtBuscar;

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, Long> colId;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colEmail;

    private final GenericDAO<Cliente> clienteDAO = new GenericDAO<>(Cliente.class);
    private final ObservableList<Cliente> listaClientes = FXCollections.observableArrayList();

    // Cliente elegido en la tabla; si es null, el formulario crea uno nuevo
    private Cliente clienteSeleccionado;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Al hacer clic en una fila, sus datos pasan al formulario para editarlos
        tablaClientes.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null) {
                clienteSeleccionado = nuevo;
                txtNombre.setText(nuevo.getNombre());
                txtTelefono.setText(nuevo.getTelefono());
                txtEmail.setText(nuevo.getEmail());
            }
        });

        cargarDatos();
    }

    // Trae los clientes de PostgreSQL y los muestra en la tabla
    private void cargarDatos() {
        listaClientes.setAll(clienteDAO.obtenerTodos());
        tablaClientes.setItems(listaClientes);
    }

    // Guarda un cliente nuevo o actualiza el seleccionado
    @FXML
    private void handleGuardar() {
        if (!validarFormulario()) return;

        boolean esNuevo = (clienteSeleccionado == null);
        Cliente cliente = esNuevo ? new Cliente() : clienteSeleccionado;
        cliente.setNombre(txtNombre.getText().trim());
        cliente.setTelefono(txtTelefono.getText().trim());
        String email = txtEmail.getText().trim();
        cliente.setEmail(email.isEmpty() ? null : email); // null evita choques con la columna unique

        if (esNuevo) {
            clienteDAO.guardar(cliente);
        } else {
            clienteDAO.actualizar(cliente);
        }

        limpiarFormulario();
        cargarDatos();
        mostrarAlerta("Éxito", esNuevo ? "Cliente registrado" : "Cliente actualizado", Alert.AlertType.INFORMATION);
    }

    // Elimina el cliente seleccionado después de pedir confirmación
    @FXML
    private void handleEliminar() {
        if (clienteSeleccionado == null) {
            mostrarAlerta("Sin selección", "Selecciona un cliente de la tabla", Alert.AlertType.WARNING);
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar a " + clienteSeleccionado.getNombre() + "?", ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setHeaderText(null);
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            clienteDAO.eliminar(clienteSeleccionado);
            limpiarFormulario();
            cargarDatos();
        }
    }

    // Filtra la tabla por nombre sin volver a consultar la base de datos
    @FXML
    private void handleBuscar() {
        String texto = txtBuscar.getText().trim().toLowerCase();
        if (texto.isEmpty()) {
            tablaClientes.setItems(listaClientes);
            return;
        }
        ObservableList<Cliente> filtrados = FXCollections.observableArrayList();
        for (Cliente c : listaClientes) {
            if (c.getNombre().toLowerCase().contains(texto)) {
                filtrados.add(c);
            }
        }
        tablaClientes.setItems(filtrados);
    }

    // Deja el formulario listo para registrar un cliente nuevo
    @FXML
    private void limpiarFormulario() {
        clienteSeleccionado = null;
        tablaClientes.getSelectionModel().clearSelection();
        txtNombre.clear();
        txtTelefono.clear();
        txtEmail.clear();
    }

    // Nombre obligatorio, email con formato básico y sin repetirse
    private boolean validarFormulario() {
        String nombre = txtNombre.getText().trim();
        String email = txtEmail.getText().trim();

        if (nombre.isEmpty()) {
            mostrarAlerta("Campo requerido", "El nombre es obligatorio", Alert.AlertType.WARNING);
            return false;
        }
        if (!email.isEmpty() && (!email.contains("@") || !email.contains("."))) {
            mostrarAlerta("Email inválido", "Escribe un email válido, por ejemplo cliente@correo.com", Alert.AlertType.WARNING);
            return false;
        }
        for (Cliente c : listaClientes) {
            boolean mismoEmail = email.equalsIgnoreCase(c.getEmail());
            boolean esOtroCliente = clienteSeleccionado == null || !c.getId().equals(clienteSeleccionado.getId());
            if (!email.isEmpty() && mismoEmail && esOtroCliente) {
                mostrarAlerta("Email repetido", "Ya existe un cliente con ese email", Alert.AlertType.WARNING);
                return false;
            }
        }
        return true;
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}