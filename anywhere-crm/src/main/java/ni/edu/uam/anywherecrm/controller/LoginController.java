package ni.edu.uam.anywherecrm.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ni.edu.uam.anywherecrm.config.HibernateUtil;
import ni.edu.uam.anywherecrm.model.Usuario;
import org.hibernate.Session;

import java.io.IOException;

// Controlador para gestionar la autenticacion y navegacion segun el rol del usuario
public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        // 1. Validacion de campos vacios
        if (username.isEmpty() || password.isEmpty()) {
            mostrarAlerta("Campos requeridos", "Por favor ingresa usuario y contraseña", Alert.AlertType.WARNING);
            return;
        }

        // 2. Consulta HQL para verificar credenciales en la base de datos PostgreSQL
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Usuario usuario = session.createQuery(
                            "FROM Usuario u WHERE u.username = :user AND u.password = :pass", Usuario.class)
                    .setParameter("user", username)
                    .setParameter("pass", password)
                    .uniqueResult();

            if (usuario != null) {
                mostrarAlerta("Bienvenido", "Inicio de sesión exitoso como: " + usuario.getRol(), Alert.AlertType.INFORMATION);
                abrirVistaSegunRol(usuario.getRol());
            } else {
                mostrarAlerta("Acceso Denegado", "Usuario o contraseña incorrectos", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            mostrarAlerta("Error de Conexión", "No se pudo conectar a la base de datos: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // Redirecciona la pantalla segun el nivel de acceso del usuario
    private void abrirVistaSegunRol(String rol) {
        String vista = "inventario-view.fxml"; // Vista predeterminada
        if ("ADMIN".equalsIgnoreCase(rol) || "VENDEDOR".equalsIgnoreCase(rol)) {
            vista = "clientes-view.fxml";
        } else if ("BODEGUERO".equalsIgnoreCase(rol)) {
            vista = "inventario-view.fxml";
        } else if ("DESPACHADOR".equalsIgnoreCase(rol)) {
            vista = "reportes-view.fxml";
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ni/edu/uam/anywherecrm/" + vista));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 800, 600));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String titulo, String msg, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}