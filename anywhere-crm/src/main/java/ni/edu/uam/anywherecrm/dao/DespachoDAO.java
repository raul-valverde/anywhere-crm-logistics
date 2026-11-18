package ni.edu.uam.anywherecrm.dao;
import ni.edu.uam.anywherecrm.config.HibernateUtil;
import ni.edu.uam.anywherecrm.model.Despacho;
import ni.edu.uam.anywherecrm.model.Orden;
import ni.edu.uam.anywherecrm.model.Usuario;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ni.edu.uam.anywherecrm.model.RutaEntrega;
import java.util.List;
// Consultas y operaciones de los despachos (ruta en camión o retiro en bodega)
public class DespachoDAO {
    // Facturas que todavía no tienen despacho
    public List<Orden> obtenerFacturasSinDespacho() {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Orden o WHERE o.estado = 'COMPLETED' "
                            + "AND NOT EXISTS (SELECT d FROM Despacho d WHERE d.orden.id = o.id) "
                    + "ORDER BY o.id DESC", Orden.class).list();
        }
    }
    // Todos los despachos, del más nuevo al más viejo
    public List<Despacho> obtenerDespachos() {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Despacho d ORDER BY d.id DESC",
                    Despacho.class).list();
        }
    }
    // Usuarios que pueden manejar el camión
    public List<Usuario> obtenerRepartidores() {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Usuario u WHERE u.rol = 'DESPACHADOR' ORDER BY u.username", Usuario.class)
                    .list();
        }
    }
    public void guardar(Despacho despacho) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                session.persist(despacho);
                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }
    // Pasa el despacho a su siguiente estado y devuelve el estado nuevo
    public String avanzarEstado(Long despachoId) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                Despacho despacho = session.find(Despacho.class, despachoId);
                String siguiente = siguienteEstado(despacho);
                if (siguiente == null) {
                    throw new IllegalStateException("Este despacho ya fue entregado");
                }
                despacho.setEstado(siguiente);
                tx.commit();
                return siguiente;
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }
    // Guarda la ruta y le asigna sus despachos en el orden de visita, todo en UNA transacción
    public void guardarRuta(RutaEntrega ruta, List<Despacho> despachosEnOrden) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                session.persist(ruta);
                int parada = 1;
                for (Despacho d : despachosEnOrden) {
                    Despacho despacho = session.find(Despacho.class, d.getId());
                    despacho.setRuta(ruta);
                    despacho.setOrdenParada(parada);
                    despacho.setRutaAsignada("Ruta #" + ruta.getId() + " - parada " + parada + " de " + despachosEnOrden.size());
                    parada++;
                }
                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }
    // RUTA: PENDIENTE -> EN_CAMINO -> ENTREGADO
// RETIRO: PENDIENTE -> ENTREGADO (el cliente llega a bodega)
    private String siguienteEstado(Despacho despacho) {
        if ("PENDIENTE".equals(despacho.getEstado())) {
            return despacho.esRetiro() ? "ENTREGADO" : "EN_CAMINO";
        }
        if ("EN_CAMINO".equals(despacho.getEstado())) {
            return "ENTREGADO";
        }
        return null; // Ya está ENTREGADO
    }
}

