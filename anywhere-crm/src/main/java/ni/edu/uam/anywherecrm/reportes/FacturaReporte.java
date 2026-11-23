package ni.edu.uam.anywherecrm.reportes;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import ni.edu.uam.anywherecrm.model.DetalleOrden;
import ni.edu.uam.anywherecrm.model.Orden;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Arma la factura con la plantilla factura.jrxml.
// Devuelve el reporte listo (JasperPrint): la vista previa lo muestra y desde ahí se guarda en PDF o Excel.
public class FacturaReporte {

    private static final String PLANTILLA = "/ni/edu/uam/anywherecrm/reportes/factura.jrxml";

    public JasperPrint crear(Orden orden, List<DetalleOrden> detalles) throws JRException, IOException {
        // 1. Cada producto de la factura es una fila del reporte (un Map con los $F{} del jrxml)
        List> filas = new ArrayList<>();
        for (DetalleOrden d : detalles) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("producto", d.getProducto().getNombre());
            fila.put("cantidad", d.getCantidad());
            fila.put("precio", d.getPrecioUnitario());
            fila.put("subtotal", d.getSubtotal());
            filas.add(fila);
        }

        // 2. Datos de la cabecera (los $P{} del jrxml)
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("NUMERO_FACTURA", String.valueOf(orden.getId()));
        parametros.put("FECHA", orden.getFecha() != null ? orden.getFecha().toString() : "");
        parametros.put("CLIENTE", orden.getCliente() != null ? orden.getCliente().getNombre() : "");
        parametros.put("VENDEDOR", orden.getVendedor() != null ? orden.getVendedor().getUsername() : "");
        parametros.put("TOTAL", orden.getTotal() != null ? orden.getTotal() : 0.0);

        // 3. El motor compila (una sola vez) y llena la plantilla
        return MotorReportes.llenar(PLANTILLA, parametros, filas);
    }
}