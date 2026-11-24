package ni.edu.uam.anywherecrm.reportes;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import ni.edu.uam.anywherecrm.model.Producto;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Arma el estado de la bodega (stock y valor de cada producto) con la plantilla inventario.jrxml.
// Devuelve el JasperPrint: la vista previa lo muestra y lo exporta a PDF o Excel.
public class ReporteInventario {

    private static final String PLANTILLA = "/ni/edu/uam/anywherecrm/reportes/inventario.jrxml";
    public static final int STOCK_MINIMO = 10; // Con menos de esto el producto sale como BAJO

    public JasperPrint crear(List<Producto> productos) throws JRException, IOException {
        // 1. Una fila por producto; el valor es precio x stock
        List<Map<String, ?>> filas = new ArrayList<>();
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

        // 2. Parámetros de la cabecera
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("FECHA", LocalDate.now().toString());
        parametros.put("STOCK_MINIMO", STOCK_MINIMO);

        // 3. Compilar y llenar; exportar lo decide la vista previa (PDF o Excel)
        return MotorReportes.llenar(PLANTILLA, parametros, filas);
    }
}