package ni.edu.uam.anywherecrm.util;
import ni.edu.uam.anywherecrm.model.Usuario;
// Guarda el usuario que inició sesión para usarlo en cualquier pantalla
public class Sesion {
    private static Usuario usuarioActual;
    public static void iniciar(Usuario usuario) {
        usuarioActual = usuario;
    }
    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }
    // true si el usuario actual tiene alguno de los roles indicados
    public static boolean tieneRol(String... roles) {
        if (usuarioActual == null) return false;
        for (String rol : roles) {
            if (rol.equalsIgnoreCase(usuarioActual.getRol())) return true;
        }
        return false;
    }
    public static void cerrar() {
        usuarioActual = null;
    }
}
