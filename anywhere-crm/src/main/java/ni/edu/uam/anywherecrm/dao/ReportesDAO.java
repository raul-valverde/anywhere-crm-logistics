package ni.edu.uam.anywherecrm.dao;

import ni.edu.uam.anywherecrm.config.HibernateUtil;
import org.hibernate.Session;
import java.util.List;

public class ReportesDAO {

    // Consulta HQL 1: Productos mas vendidos ordenados de mayor a menor
    public List<Object[]> obtenerProductosMasVendidos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT d.producto.nombre, SUM(d.cantidad) FROM DetalleOrden d GROUP BY d.producto.nombre ORDER BY SUM(d.cantidad) DESC";
            return session.createQuery(hql, Object[].class).list();
        }
    }

    // Consulta HQL 2: Total vendido por cada vendedor en estado COMPLETED
    public List<Object[]> obtenerVentasPorVendedor() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT o.vendedor.username, SUM(o.total) FROM Orden o WHERE o.estado = 'COMPLETED' GROUP BY o.vendedor.username";
            return session.createQuery(hql, Object[].class).list();
        }
    }
}