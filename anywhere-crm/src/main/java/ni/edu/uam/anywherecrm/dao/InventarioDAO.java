package ni.edu.uam.anywherecrm.dao;

import ni.edu.uam.anywherecrm.config.HibernateUtil;
import ni.edu.uam.anywherecrm.model.MovimientoInventario;
import ni.edu.uam.anywherecrm.model.Producto;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

// Maneja las entradas y salidas de bodega
public class InventarioDAO {

    // Registra el movimiento y actualiza el stock en la misma transacción.
    // Una SALIDA mayor al stock disponible se rechaza.
    public void registrarMovimiento(Long productoId, String tipo, int cantidad, String motivo) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                Producto producto = session.get(Producto.class, productoId);
                if (producto == null) {
                    throw new IllegalArgumentException("El producto no existe");
                }

                int stockActual = producto.getStock() == null ? 0 : producto.getStock();

                if ("SALIDA".equals(tipo)) {
                    if (cantidad > stockActual) {
                        throw new IllegalStateException("Stock insuficiente de " + producto.getNombre()
                                + ". Disponible: " + stockActual);
                    }
                    producto.setStock(stockActual - cantidad);
                } else {
                    producto.setStock(stockActual + cantidad);
                }

                session.persist(new MovimientoInventario(producto, tipo, cantidad, motivo));
                tx.commit(); // Hibernate guarda también el nuevo stock del producto
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }

    // true si hay suficiente stock para sacar esa cantidad
    public boolean hayStockSuficiente(Long productoId, int cantidad) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Producto producto = session.get(Producto.class, productoId);
            return producto != null && producto.getStock() != null && producto.getStock() >= cantidad;
        }
    }

    // Historial completo, del más reciente al más antiguo
    public List<MovimientoInventario> obtenerMovimientos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM MovimientoInventario m ORDER BY m.fecha DESC",
                    MovimientoInventario.class).list();
        }
    }

    // Historial de un solo producto
    public List<MovimientoInventario> obtenerPorProducto(Long productoId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM MovimientoInventario m WHERE m.producto.id = :id ORDER BY m.fecha DESC",
                            MovimientoInventario.class)
                    .setParameter("id", productoId)
                    .list();
        }
    }
}