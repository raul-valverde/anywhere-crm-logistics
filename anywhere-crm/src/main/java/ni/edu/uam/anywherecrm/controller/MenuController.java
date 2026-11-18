package ni.edu.uam.anywherecrm.controller;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import ni.edu.uam.anywherecrm.model.Usuario;
import ni.edu.uam.anywherecrm.util.Sesion;
import java.io.IOException;

// Menú principal: habilita solo los módulos que le tocan a cada rol
public class MenuController {
    @FXML private Label lblBienvenida;
    @FXML private Button btnClientes;
    @FXML private Button btnProformas;
    @FXML private Button btnInventario;
    @FXML private Button btnReportes;
    @FXML private Button btnFacturas;
    @FXML private Button btnDespachos;

    @FXML
    public void initialize() {
        Usuario usuario = Sesion.getUsuarioActual();
        lblBienvenida.setText("Bienvenido, " + usuario.getUsername() + " (" +
                usuario.getRol() + ")");
// Permisos por rol
        btnClientes.setDisable(!Sesion.tieneRol("ADMIN", "VENDEDOR"));
        btnProformas.setDisable(!Sesion.tieneRol("ADMIN", "VENDEDOR"));
        btnInventario.setDisable(!Sesion.tieneRol("ADMIN", "BODEGUERO"));
        btnReportes.setDisable(!Sesion.tieneRol("ADMIN", "DESPACHADOR"));
        btnFacturas.setDisable(!Sesion.tieneRol("ADMIN", "VENDEDOR"));
        btnDespachos.setDisable(!Sesion.tieneRol("ADMIN", "DESPACHADOR"));
    }
    @FXML private void abrirClientes() { abrirVentana("clientes-view.fxml",
            "Clientes"); }
    @FXML private void abrirProformas() { abrirVentana("proformas-view.fxml",
            "Proformas"); }
    @FXML private void abrirInventario() { abrirVentana("inventario-view.fxml", "Inventario"); }
    @FXML private void abrirReportes() { abrirVentana("reportes-view.fxml",
            "Reportes"); }
    @FXML private void abrirFacturas() { abrirVentana("facturas-view.fxml",
            "Facturas"); }
    @FXML private void abrirDespachos() { abrirVentana("despachos-view.fxml",
            "Despachos"); }

    // Abre el módulo en una ventana aparte para que el menú siga abierto
    private void abrirVentana(String fxml, String titulo) {
        try {
            FXMLLoader loader = new
                    FXMLLoader(getClass().getResource("/ni/edu/uam/anywherecrm/" + fxml));
            Stage ventana = new Stage();
            ventana.setTitle("Anywhere CRM - " + titulo);
            ventana.setScene(new Scene(loader.load(), 900, 600));
            ventana.show();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir " + titulo + ": " +
                    e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    // Vuelve a la pantalla de login
    @FXML
    private void handleCerrarSesion() {
        Sesion.cerrar();
        try {
            FXMLLoader loader = new
                    FXMLLoader(getClass().getResource("/ni/edu/uam/anywherecrm/login-view.fxml"));
            Stage stage = (Stage) lblBienvenida.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 400, 400));
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo volver al login: " +
                    e.getMessage(), Alert.AlertType.ERROR);
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
