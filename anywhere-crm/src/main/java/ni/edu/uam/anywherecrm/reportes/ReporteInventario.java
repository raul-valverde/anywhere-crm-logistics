package ni.edu.uam.anywherecrm.reportes;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import ni.edu.uam.anywherecrm.model.Producto;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Genera en PDF el estado de la bodega: stock y valor de cada producto
public class ReporteInventario {

    private static final String PLANTILLA = "/ni/edu/uam/anywherecrm/reportes/inventario.jrxml";
    public static final int STOCK_MINIMO = 10; // Con menos de esto el producto sale como BAJO

    public void generarPdf(List<Producto> productos, File destino) throws JRException, IOException {
        // 1. Una fila por producto; el valor es precio x stock
        List> filas = new ArrayList<>();
        for (Producto producto : productos) {
            int stock = producto.getStock() == null ? 0 : producto.getStock();
            double precio = producto.getPrecio() == null ? 0 : producto.getPrecio();

            Map<String, Object> fila = new HashMap<>();
            fila.put("producto", producto.getNombre());
            fila.put("categoria", producto.getCategoria() != null ? producto.getCategoria().getNombre() : "Sin categoría");
            fila.put("precio", precio);
            fila.put("stock", stock);
            fila.put("valor", precio * stock);
            fila.put("estado", stock < STOCK_MINIMO ? "BAJO" : "OK");
            filas.add(fila);
        }

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("FECHA", LocalDate.now().toString());
        parametros.put("STOCK_MINIMO", STOCK_MINIMO);

        // 2. Compilar, llenar y exportar
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