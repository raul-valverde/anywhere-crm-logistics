package ni.edu.uam.anywherecrm.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import ni.edu.uam.anywherecrm.dao.DespachoDAO;
import ni.edu.uam.anywherecrm.model.Despacho;
import ni.edu.uam.anywherecrm.model.Orden;
import ni.edu.uam.anywherecrm.model.RutaEntrega;
import ni.edu.uam.anywherecrm.model.Usuario;
import ni.edu.uam.anywherecrm.service.RutaService;
import ni.edu.uam.anywherecrm.util.Navegacion;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Pantalla de despachos:
//  1. Se crea un despacho por factura: RUTA (camión) o RETIRO (el cliente llega a bodega)
//  2. Para RUTA, la API ubica la dirección y guarda sus coordenadas
//  3. Se seleccionan varios despachos y "Armar ruta" calcula el mejor orden para el camión
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
    @FXML private Button btnUbicar;
    @FXML private Label lblUbicacion;
    @FXML private ComboBox<Usuario> cbRepartidor;
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
    @FXML private TableColumn<Despacho, String> colDesRuta;
    @FXML private Button btnArmarRuta;

    private final DespachoDAO despachoDAO = new DespachoDAO();
    private final RutaService rutaService = new RutaService();

    private RutaService.Ubicacion ubicacionActual; // Dirección ya ubicada por la API
    private String ultimoEnlaceMapa;               // Mapa de la última ruta armada

    @FXML
    public void initialize() {
        colFacId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFacFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFacTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colFacCliente.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getCliente() != null ? fila.getValue().getCliente().getNombre() : ""));

        colDesId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDesTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colDesEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colDesFecha.setCellValueFactory(new PropertyValueFactory<>("fechaDespacho"));
        colDesDireccion.setCellValueFactory(new PropertyValueFactory<>("direccionEntrega"));
        colDesRuta.setCellValueFactory(new PropertyValueFactory<>("rutaAsignada"));
        colDesFactura.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getOrden() != null ? "#" + fila.getValue().getOrden().getId() : ""));
        colDesCliente.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getOrden() != null && fila.getValue().getOrden().getCliente() != null
                        ? fila.getValue().getOrden().getCliente().getNombre() : ""));
        colDesRepartidor.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getRepartidor() != null ? fila.getValue().getRepartidor().getUsername() : "-"));

        // Se pueden seleccionar varios despachos (Ctrl + clic) para armar una ruta
        tablaDespachos.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        cbTipo.getItems().addAll("RUTA", "RETIRO");
        cbTipo.setValue("RUTA");
        // Un retiro en bodega no necesita dirección, ubicación ni repartidor
        cbTipo.valueProperty().addListener((obs, anterior, nuevo) -> {
            boolean esRetiro = "RETIRO".equals(nuevo);
            txtDireccion.setDisable(esRetiro);
            btnUbicar.setDisable(esRetiro);
            cbRepartidor.setDisable(esRetiro);
        });

        // Si se cambia la dirección, hay que volver a ubicarla
        txtDireccion.textProperty().addListener((obs, anterior, nuevo) -> {
            ubicacionActual = null;
            lblUbicacion.setText("Sin ubicar");
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

    // API Nominatim: busca la dirección y guarda sus coordenadas (en otro hilo para no congelar la ventana)
    @FXML
    private void handleUbicar() {
        String direccion = txtDireccion.getText().trim();
        if (direccion.isEmpty()) {
            mostrarAlerta("Falta la dirección", "Escribe la dirección de entrega", Alert.AlertType.WARNING);
            return;
        }
        btnUbicar.setDisable(true);
        lblUbicacion.setText("Buscando...");

        new Thread(() -> {
            try {
                RutaService.Ubicacion ubicacion = rutaService.ubicar(direccion);
                // Los controles de JavaFX solo se tocan desde su propio hilo
                Platform.runLater(() -> {
                    ubicacionActual = ubicacion;
                    lblUbicacion.setText("Ubicada: " + resumirNombre(ubicacion.getNombre()));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblUbicacion.setText("Sin ubicar");
                    mostrarAlerta("No se encontró la dirección", e.getMessage(), Alert.AlertType.WARNING);
                });
            } finally {
                Platform.runLater(() -> btnUbicar.setDisable(false));
            }
        }).start();
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
            mostrarAlerta("Falta la fecha", "Elige la fecha del despacho", Alert.AlertType.WARNING);
            return;
        }

        Despacho despacho = new Despacho();
        despacho.setOrden(factura);
        despacho.setTipo(cbTipo.getValue());
        despacho.setEstado("PENDIENTE");
        despacho.setFechaDespacho(dpFecha.getValue());

        if ("RUTA".equals(cbTipo.getValue())) {
            if (ubicacionActual == null) {
                mostrarAlerta("Dirección sin ubicar", "Escribe la dirección y presiona \"Ubicar\" antes de crear el despacho",
                        Alert.AlertType.WARNING);
                return;
            }
            if (cbRepartidor.getValue() == null) {
                mostrarAlerta("Falta el repartidor", "Elige quién lleva el camión", Alert.AlertType.WARNING);
                return;
            }
            despacho.setDireccionEntrega(txtDireccion.getText().trim());
            despacho.setLatitud(ubicacionActual.getLatitud());
            despacho.setLongitud(ubicacionActual.getLongitud());
            despacho.setRepartidor(cbRepartidor.getValue());
            despacho.setRutaAsignada("Sin ruta");
        } else {
            despacho.setDireccionEntrega("Retiro en bodega");
        }

        try {
            despachoDAO.guardar(despacho);
            limpiarFormulario();
            cargarDatos();
            mostrarAlerta("Despacho creado", "Despacho de la factura #" + factura.getId() + " creado", Alert.AlertType.INFORMATION);
        } catch (RuntimeException e) {
            mostrarAlerta("Error de BD", "No se pudo crear el despacho: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // API OSRM: con los despachos seleccionados arma UNA ruta con el mejor orden de entrega
    @FXML
    private void handleArmarRuta() {
        List<Despacho> seleccionados = new ArrayList<>(tablaDespachos.getSelectionModel().getSelectedItems());
        if (seleccionados.isEmpty()) {
            mostrarAlerta("Sin selección", "Selecciona los despachos del viaje (Ctrl + clic para elegir varios)",
                    Alert.AlertType.WARNING);
            return;
        }

        // Validar que todos puedan ir en el mismo camión
        Usuario repartidor = seleccionados.get(0).getRepartidor();
        for (Despacho d : seleccionados) {
            String factura = "La factura #" + d.getOrden().getId();
            if (!"RUTA".equals(d.getTipo()) || d.getLatitud() == null) {
                mostrarAlerta("No se puede armar", factura + " no es un despacho en camión con dirección ubicada", Alert.AlertType.WARNING);
                return;
            }
            if (!"PENDIENTE".equals(d.getEstado()) || d.getRuta() != null) {
                mostrarAlerta("No se puede armar", factura + " ya salió o ya está en otra ruta", Alert.AlertType.WARNING);
                return;
            }
            if (repartidor == null || d.getRepartidor() == null || !repartidor.getId().equals(d.getRepartidor().getId())) {
                mostrarAlerta("No se puede armar", "Todos los despachos de una ruta deben tener el mismo repartidor", Alert.AlertType.WARNING);
                return;
            }
        }

        List<double[]> paradas = new ArrayList<>();
        for (Despacho d : seleccionados) {
            paradas.add(new double[]{d.getLatitud(), d.getLongitud()});
        }

        btnArmarRuta.setDisable(true);
        btnArmarRuta.setText("Calculando...");
        new Thread(() -> {
            try {
                RutaService.PlanRuta plan = rutaService.planificar(paradas);
                Platform.runLater(() -> guardarRuta(plan, seleccionados, repartidor));
            } catch (Exception e) {
                Platform.runLater(() -> mostrarAlerta("No se pudo armar la ruta", e.getMessage(), Alert.AlertType.WARNING));
            } finally {
                Platform.runLater(() -> {
                    btnArmarRuta.setDisable(false);
                    btnArmarRuta.setText("Armar ruta");
                });
            }
        }).start();
    }

    // Guarda la ruta con los despachos en el orden que calculó la API y muestra el recorrido
    private void guardarRuta(RutaService.PlanRuta plan, List<Despacho> seleccionados, Usuario repartidor) {
        List<Despacho> enOrden = new ArrayList<>();
        StringBuilder recorrido = new StringBuilder("Bodega");
        for (int indice : plan.getOrden()) {
            Despacho d = seleccionados.get(indice);
            enOrden.add(d);
            recorrido.append("\n").append(enOrden.size()).append(". Factura #").append(d.getOrden().getId())
                    .append(" - ").append(d.getDireccionEntrega());
        }
        recorrido.append("\nRegreso a bodega");

        RutaEntrega ruta = new RutaEntrega();
        ruta.setFecha(LocalDate.now());
        ruta.setRepartidor(repartidor);
        ruta.setParadas(enOrden.size());
        ruta.setKilometros(plan.getKilometros());
        ruta.setMinutos(plan.getMinutos());
        ruta.setEnlaceMapa(plan.getEnlaceMapa());

        try {
            despachoDAO.guardarRuta(ruta, enOrden);
            ultimoEnlaceMapa = plan.getEnlaceMapa();
            cargarDatos();
            mostrarAlerta("Ruta #" + ruta.getId() + " armada", plan.resumen() + "\n\n" + recorrido, Alert.AlertType.INFORMATION);
        } catch (RuntimeException e) {
            mostrarAlerta("Error de BD", "No se pudo guardar la ruta: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // Abre en el navegador la ruta del despacho seleccionado (o la última armada)
    @FXML
    private void handleVerMapa() {
        Despacho seleccionado = tablaDespachos.getSelectionModel().getSelectedItem();
        String enlace = (seleccionado != null && seleccionado.getRuta() != null)
                ? seleccionado.getRuta().getEnlaceMapa()
                : ultimoEnlaceMapa;
        if (enlace == null) {
            mostrarAlerta("Sin ruta", "Selecciona un despacho que ya tenga ruta, o arma una ruta primero", Alert.AlertType.WARNING);
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(enlace));
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo abrir el navegador: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // PENDIENTE -> EN_CAMINO -> ENTREGADO (un retiro pasa directo a ENTREGADO)
    @FXML
    private void handleAvanzarEstado() {
        Despacho despacho = tablaDespachos.getSelectionModel().getSelectedItem();
        if (despacho == null) {
            mostrarAlerta("Sin selección", "Selecciona un despacho de la tabla de abajo", Alert.AlertType.WARNING);
            return;
        }
        try {
            String nuevoEstado = despachoDAO.avanzarEstado(despacho.getId());
            cargarDatos();
            mostrarAlerta("Estado actualizado", "El despacho #" + despacho.getId() + " ahora está " + nuevoEstado,
                    Alert.AlertType.INFORMATION);
        } catch (IllegalStateException e) {
            mostrarAlerta("No permitido", e.getMessage(), Alert.AlertType.WARNING);
        }
    }

    // Deja solo el nombre corto del lugar, sin todo el detalle que da la API
    private String resumirNombre(String nombreCompleto) {
        String[] partes = nombreCompleto.split(",");
        return partes.length >= 2 ? partes[0].trim() + ", " + partes[partes.length - 2].trim() : nombreCompleto;
    }

    private void limpiarFormulario() {
        cbTipo.setValue("RUTA");
        txtDireccion.clear();
        cbRepartidor.setValue(null);
        dpFecha.setValue(LocalDate.now());
        ubicacionActual = null;
        lblUbicacion.setText("Sin ubicar");
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