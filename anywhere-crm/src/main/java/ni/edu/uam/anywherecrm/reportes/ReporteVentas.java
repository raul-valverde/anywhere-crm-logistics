package ni.edu.uam.anywherecrm.reportes;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import ni.edu.uam.anywherecrm.model.Orden;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// Genera en PDF el reporte de ventas (facturas) entre dos fechas
public class ReporteVentas {
    private static final String PLANTILLA =
            "/ni/edu/uam/anywherecrm/reportes/ventas.jrxml";
    public void generarPdf(LocalDate desde, LocalDate hasta, List<Orden>
            facturas, File destino)
            throws JRException, IOException {
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
        // 3. Compilar, llenar y exportar (el total general lo suma Jasper con una variable)
        try (InputStream plantilla = getClass().getResourceAsStream(PLANTILLA)) {
            if (plantilla == null) {
                throw new IOException("No se encontró la plantilla " +
                        PLANTILLA);
            }
            JasperReport reporte = JasperCompileManager.compileReport(plantilla);
            JasperPrint impresion = JasperFillManager.fillReport(reporte, parametros, new JRMapCollectionDataSource(filas));
            JasperExportManager.exportReportToPdfFile(impresion, destino.getAbsolutePath());
        }
    }
}

