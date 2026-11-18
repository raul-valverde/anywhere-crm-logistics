package ni.edu.uam.anywherecrm.dao;
import ni.edu.uam.anywherecrm.config.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;
// Clase generica que realiza el CRUD para cualquier entidad JPA.
// Si algo falla, deshace la transacción y lanza la excepción para que
// la pantalla le muestre el error al usuario (antes solo se imprimía enconsola).
public class GenericDAO<T> {
    private final Class<T> clazz;
    public GenericDAO(Class<T> clazz) {
        this.clazz = clazz;
    }
    // Guardar un nuevo registro en la base de datos
    public void guardar(T entidad) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                session.persist(entidad);
                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }
    // Actualizar un registro existente
    public void actualizar(T entidad) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                session.merge(entidad);
                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }
    // Eliminar un registro por objeto
    public void eliminar(T entidad) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                session.remove(session.contains(entidad) ? entidad :
                        session.merge(entidad));
                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }
    // Buscar por ID
    public T buscarPorId(Long id) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            return session.find(clazz, id);
        }
    }
    // Listar todos los registros de la tabla
    public List<T> obtenerTodos() {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM " + clazz.getSimpleName(),
                    clazz).list();
        }
    }
}

