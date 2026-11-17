package ni.edu.uam.anywherecrm.dao;
import ni.edu.uam.anywherecrm.config.HibernateUtil;
import ni.edu.uam.anywherecrm.model.DetalleProforma;
import ni.edu.uam.anywherecrm.model.Proforma;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.time.LocalDate;
import java.util.List;
// Consultas y operaciones propias de las proformas
public class ProformaDAO {
    // Guarda la proforma y todos sus detalles en una sola transacción:
// si falla un detalle, no se guarda nada
    public void guardarConDetalles(Proforma proforma, List<DetalleProforma>
            detalles) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                double total = 0;
                for (DetalleProforma d : detalles) {
                    d.calcularSubtotal();
                    total += d.getSubtotal();
                }
                proforma.setMontoEstimado(total);
                proforma.setEstado("PENDIENTE");
                if (proforma.getFechaEmision() == null) {
                    proforma.setFechaEmision(LocalDate.now());
                }
                session.persist(proforma);
                for (DetalleProforma d : detalles) {
                    d.setProforma(proforma);
                    session.persist(d);
                }
                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }
    // Todas las proformas, de la más nueva a la más vieja
    public List<Proforma> obtenerTodas() {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Proforma p ORDER BY p.id DESC",
                    Proforma.class).list();
        }
    }
    // Proformas filtradas por estado (PENDIENTE, FACTURADA o ANULADA)
    public List<Proforma> obtenerPorEstado(String estado) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Proforma p WHERE p.estado = :estado ORDER BY p.id DESC", Proforma.class)
                    .setParameter("estado", estado)
                    .list();
        }
    }
    // Productos que lleva una proforma
    public List<DetalleProforma> obtenerDetalles(Long proformaId) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM DetalleProforma d WHERE d.proforma.id = :id", DetalleProforma.class)
                    .setParameter("id", proformaId)
                    .list();
        }
    }
    // Cambia el estado de una proforma (por ejemplo, a ANULADA o FACTURADA)
    public void cambiarEstado(Long proformaId, String nuevoEstado) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                Proforma proforma = session.get(Proforma.class, proformaId);
                proforma.setEstado(nuevoEstado);
                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }
}
