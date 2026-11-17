package ni.edu.uam.anywherecrm.reportes;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import ni.edu.uam.anywherecrm.model.DetalleOrden;
import ni.edu.uam.anywherecrm.model.Orden;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Genera la factura en PDF con JasperReports a partir de la plantilla factura.jrxml
public class FacturaReporte {

    private static final String PLANTILLA = "/ni/edu/uam/anywherecrm/reportes/factura.jrxml";

    public void generarPdf(Orden orden, List<DetalleOrden> detalles, File destino) throws JRException, IOException {
        // 1. Cada producto de la factura es una fila del reporte (un Map con los $F{} del jrxml)
        List<Map<String, ?>> filas = new ArrayList<>();
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

        // 3. Compilar la plantilla, llenarla y exportarla a PDF
        try (InputStream plantilla = getClass().getResourceAsStream(PLANTILLA)) {
            if (plantilla == null) {
                throw new IOException("No se encontró la plantilla " + PLANTILLA);
            }
            JasperReport reporte = JasperCompileManager.compileReport(plantilla);
            JasperPrint impresion = JasperFillManager.fillReport(reporte, parametros, new JRMapCollectionDataSource(filas));
            JasperExportManager.exportReportToPdfFile(impresion, destino.getAbsolutePath());
        }
    }
}