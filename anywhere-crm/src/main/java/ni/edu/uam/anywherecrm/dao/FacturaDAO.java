package ni.edu.uam.anywherecrm.dao;

import ni.edu.uam.anywherecrm.config.HibernateUtil;
import ni.edu.uam.anywherecrm.model.DetalleOrden;
import ni.edu.uam.anywherecrm.model.Orden;
import org.hibernate.Session;

import java.util.List;

// Consultas de facturas (en la base de datos una factura es una Orden)
public class FacturaDAO {

    // Todas las facturas, de la más nueva a la más vieja
    public List<Orden> obtenerFacturas() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Orden o ORDER BY o.id DESC", Orden.class).list();
        }
    }

    // Productos que lleva una factura
    public List<DetalleOrden> obtenerDetalles(Long ordenId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM DetalleOrden d WHERE d.orden.id = :id", DetalleOrden.class)
                    .setParameter("id", ordenId)
                    .list();
        }
    }
}