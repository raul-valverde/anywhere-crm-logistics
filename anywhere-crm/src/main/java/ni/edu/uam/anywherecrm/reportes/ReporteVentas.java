package ni.edu.uam.anywherecrm.reportes;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import ni.edu.uam.anywherecrm.model.Orden;
import org.hibernate.query.Page;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// Arma el reporte de ventas (facturas) entre dos fechas con la plantilla ventas.jrxml.
// Devuelve el JasperPrint: la vista previa lo muestra y lo exporta a PDF o Excel.
public class ReporteVentas {
    private static final String PLANTILLA =
            "/ni/edu/uam/anywherecrm/reportes/ventas.jrxml";
    public JasperPrint crear(LocalDate desde, LocalDate hasta, List<Orden>
            facturas) throws JRException, IOException {
// 1. Una fila por factura
        List<Map<String, ?>> filas = new ArrayList<>();
        for (Orden factura : facturas) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("factura", "#" + factura.getId());
            fila.put("fecha", factura.getFecha() != null ?
                    factura.getFecha().toString() : "");
            fila.put("cliente", factura.getCliente() != null ?
                    factura.getCliente().getNombre() : "");
            fila.put("vendedor", factura.getVendedor() != null ?
                    factura.getVendedor().getUsername() : "");
            fila.put("total", factura.getTotal() != null ? factura.getTotal()
                    : 0.0);
            filas.add(fila);
        }
// 2. El rango de fechas va en la cabecera
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("DESDE", desde.toString());
        parametros.put("HASTA", hasta.toString());
// 3. Compilar y llenar (el total general lo suma Jasper con una variable del jrxml)
        return MotorReportes.llenar(PLANTILLA, parametros, filas);
    }
}