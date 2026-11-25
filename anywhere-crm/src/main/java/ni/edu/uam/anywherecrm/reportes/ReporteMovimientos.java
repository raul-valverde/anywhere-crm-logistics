package ni.edu.uam.anywherecrm.reportes;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import ni.edu.uam.anywherecrm.model.MovimientoInventario;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Arma el reporte Kardex / movimientos de bodega entre dos fechas con la plantilla movimientos.jrxml.
// Devuelve el JasperPrint: la vista previa lo muestra y permite exportarlo a PDF o Excel.
public class ReporteMovimientos {

    private static final String PLANTILLA = "/ni/edu/uam/anywherecrm/reportes/movimientos.jrxml";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public JasperPrint crear(LocalDate desde, LocalDate hasta, String tipoFiltro,
                             List<MovimientoInventario> movimientos) throws JRException, IOException {
        // 1. Una fila por movimiento registrado en bodega
        List<Map<String, ?>> filas = new ArrayList<>();
        for (MovimientoInventario mov : movimientos) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("fecha", mov.getFecha() != null ? mov.getFecha().format(FORMATO_FECHA) : "");
            fila.put("producto", mov.getProducto() != null ? mov.getProducto().getNombre() : "");
            fila.put("motivo", mov.getMotivo() != null ? mov.getMotivo() : "");
            fila.put("tipo", mov.getTipo() != null ? mov.getTipo() : "");
            fila.put("cantidad", mov.getCantidad() != null ? mov.getCantidad() : 0);
            filas.add(fila);
        }

        // 2. Rango de fechas y filtro elegido en la cabecera
        String etiquetaTipo = (tipoFiltro == null || "TODOS".equalsIgnoreCase(tipoFiltro))
                ? "Todos (Entradas y Salidas)"
                : tipoFiltro;
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("DESDE", desde.toString());
        parametros.put("HASTA", hasta.toString());
        parametros.put("TIPO_FILTRO", etiquetaTipo);

        // 3. Compilar y llenar; los totales de entradas y salidas los suma Jasper en el jrxml
        return MotorReportes.llenar(PLANTILLA, parametros, filas);
    }
}
