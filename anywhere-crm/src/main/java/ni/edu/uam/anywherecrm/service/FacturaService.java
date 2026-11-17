package ni.edu.uam.anywherecrm.service;

import ni.edu.uam.anywherecrm.config.HibernateUtil;
import ni.edu.uam.anywherecrm.model.DetalleOrden;
import ni.edu.uam.anywherecrm.model.DetalleProforma;
import ni.edu.uam.anywherecrm.model.MovimientoInventario;
import ni.edu.uam.anywherecrm.model.Orden;
import ni.edu.uam.anywherecrm.model.Producto;
import ni.edu.uam.anywherecrm.model.Proforma;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Convierte una proforma en factura revisando primero que todo esté en bodega
public class FacturaService {

    // Revisa la bodega sin cambiar nada.
    // Devuelve los productos que faltan; si la lista está vacía, hay stock de todo.
    public List<String> verificarStock(Long proformaId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<String> faltantes = new ArrayList<>();
            for (DetalleProforma detalle : obtenerDetalles(session, proformaId)) {
                Producto producto = detalle.getProducto();
                int stock = producto.getStock() == null ? 0 : producto.getStock();
                if (detalle.getCantidad() > stock) {
                    faltantes.add(producto.getNombre() + ": pide " + detalle.getCantidad() + ", hay " + stock);
                }
            }
            return faltantes;
        }
    }

    // Todo pasa en UNA transacción: si un producto no alcanza, no se guarda nada
    public Orden convertirProformaEnFactura(Long proformaId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                Proforma proforma = session.find(Proforma.class, proformaId);
                if (proforma == null) {
                    throw new IllegalArgumentException("La proforma no existe");
                }
                if (!"PENDIENTE".equals(proforma.getEstado())) {
                    throw new IllegalStateException("Solo se pueden facturar proformas PENDIENTES");
                }

                List<DetalleProforma> detalles = obtenerDetalles(session, proformaId);
                if (detalles.isEmpty()) {
                    throw new IllegalStateException("La proforma no tiene productos");
                }

                // 1. Revisar la bodega antes de tocar nada
                for (DetalleProforma detalle : detalles) {
                    Producto producto = detalle.getProducto();
                    int stock = producto.getStock() == null ? 0 : producto.getStock();
                    if (detalle.getCantidad() > stock) {
                        throw new IllegalStateException("Stock insuficiente de " + producto.getNombre()
                                + ". Disponible: " + stock + ", pedido: " + detalle.getCantidad());
                    }
                }

                // 2. Crear la factura (en la base de datos es una Orden)
                Orden orden = new Orden();
                orden.setFecha(LocalDate.now());
                orden.setCliente(proforma.getCliente());
                orden.setVendedor(proforma.getVendedor());
                orden.setEstado("COMPLETED"); // Así la cuentan los reportes de ventas
                orden.setTotal(0.0);
                session.persist(orden); // Aquí ya recibe su número (id)

                // 3. Copiar cada producto a la factura y sacarlo de bodega
                double total = 0;
                for (DetalleProforma detalle : detalles) {
                    Producto producto = detalle.getProducto();

                    DetalleOrden linea = new DetalleOrden();
                    linea.setOrden(orden);
                    linea.setProducto(producto);
                    linea.setCantidad(detalle.getCantidad());
                    linea.setPrecioUnitario(detalle.getPrecioUnitario());
                    linea.setSubtotal(detalle.getSubtotal());
                    session.persist(linea);

                    producto.setStock(producto.getStock() - detalle.getCantidad());
                    session.persist(new MovimientoInventario(producto,
                            "SALIDA", detalle.getCantidad(),
                            "Factura #" + orden.getId()));
                    total += detalle.getSubtotal();
                }

                // 4. Cerrar la factura y marcar la proforma
                orden.setTotal(total);
                proforma.setEstado("FACTURADA");

                tx.commit(); // Hibernate guarda también el nuevo stock y los cambios de estado
                return orden;
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }

    private List<DetalleProforma> obtenerDetalles(Session session, Long proformaId) {
        return session.createQuery("FROM DetalleProforma d WHERE d.proforma.id = :id", DetalleProforma.class)
                .setParameter("id", proformaId)
                .list();
    }
}