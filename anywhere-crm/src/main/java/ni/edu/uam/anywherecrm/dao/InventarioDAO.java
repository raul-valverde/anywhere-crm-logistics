package ni.edu.uam.anywherecrm.dao;

import ni.edu.uam.anywherecrm.config.HibernateUtil;
import ni.edu.uam.anywherecrm.model.MovimientoInventario;
import ni.edu.uam.anywherecrm.model.Producto;
import ni.edu.uam.anywherecrm.model.Usuario;
import ni.edu.uam.anywherecrm.util.Sesion;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Maneja las entradas y salidas de bodega
public class InventarioDAO {

    // Registra el movimiento (con el usuario en sesión) y actualiza el stock en la misma transacción.
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

                Usuario actual = Sesion.getUsuarioActual();
                Usuario usuario = (actual != null && actual.getId() != null)
                        ? session.find(Usuario.class, actual.getId())
                        : null;

                session.persist(new MovimientoInventario(producto, tipo, cantidad, motivo, usuario));
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

    // Movimientos entre dos fechas (incluidas) y opcionalmente por tipo (TODOS, ENTRADA o SALIDA)
    public List<MovimientoInventario> obtenerMovimientosEntre(LocalDate desde, LocalDate hasta, String tipo) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();
        boolean filtrarTipo = tipo != null && !"TODOS".equalsIgnoreCase(tipo);

        String hql = "FROM MovimientoInventario m WHERE m.fecha >= :inicio AND m.fecha < :fin"
                + (filtrarTipo ? " AND m.tipo = :tipo" : "")
                + " ORDER BY m.fecha ASC, m.id ASC";

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            var consulta = session.createQuery(hql, MovimientoInventario.class)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin);
            if (filtrarTipo) {
                consulta.setParameter("tipo", tipo);
            }
            return consulta.list();
        }
    }

    // Mapa idOrden -> username del vendedor para respaldar movimientos históricos de facturas anteriores
    public Map<Long, String> obtenerVendedoresPorFactura() {
        Map<Long, String> mapa = new HashMap<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> filas = session.createQuery(
                    "SELECT o.id, o.vendedor.username FROM Orden o WHERE o.vendedor IS NOT NULL",
                    Object[].class).list();
            for (Object[] fila : filas) {
                if (fila[0] instanceof Long id && fila[1] instanceof String username) {
                    mapa.put(id, username);
                }
            }
        }
        return mapa;
    }

    // Devuelve el usuario que realizó el movimiento (o el vendedor de la factura si es un registro anterior)
    public static String resolverUsuario(MovimientoInventario mov, Map<Long, String> vendedoresFactura) {
        if (mov.getUsuario() != null && mov.getUsuario().getUsername() != null) {
            return mov.getUsuario().getUsername();
        }
        String motivo = mov.getMotivo();
        if (motivo != null && motivo.startsWith("Factura #") && vendedoresFactura != null) {
            try {
                Long ordenId = Long.parseLong(motivo.substring("Factura #".length()).trim());
                String vendedor = vendedoresFactura.get(ordenId);
                if (vendedor != null) return vendedor;
            } catch (NumberFormatException ignored) {
            }
        }
        return "-";
    }
}
