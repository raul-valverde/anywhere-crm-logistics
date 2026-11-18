package ni.edu.uam.anywherecrm.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

// Clase encargada de conectar Hibernate con la base de datos PostgreSQL
public class HibernateUtil {
    // SessionFactory es el objeto principal de Hibernate que administra las conexiones
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            // Lee la configuración del archivo hibernate.cfg.xml
            Configuration configuracion = new Configuration().configure("hibernate.cfg.xml");

            // Si existe la variable de entorno, la contraseña se toma de ahí y no del archivo.
            // Así la contraseña real nunca se sube al repositorio.
            String clave = System.getenv("ANYWHERE_DB_PASSWORD");
            if (clave != null && !clave.isBlank()) {
                configuracion.setProperty("hibernate.connection.password", clave);
            }
            return configuracion.buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Error al iniciar Hibernate: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    // Método público para obtener la conexión en cualquier parte del código
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    // Cierra la conexión al salir de la aplicación
    public static void shutdown() {
        getSessionFactory().close();
    }
}