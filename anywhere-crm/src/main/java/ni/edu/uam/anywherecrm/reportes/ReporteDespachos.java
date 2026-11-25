package ni.edu.uam.anywherecrm.reportes;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import ni.edu.uam.anywherecrm.model.Despacho;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReporteDespachos {

    private static final String PLANTILLA = "/ni/edu/uam/anywherecrm/reportes/despachos.jrxml";

    @SuppressWarnings({"rawtypes", "unchecked"})
    public JasperPrint crear(LocalDate desde, LocalDate hasta, String estadoFiltro,
                             List despachos) throws JRException, IOException {
        List filas = new ArrayList();

        for (Object obj : despachos) {
            Despacho d = (Despacho) obj;
            Map fila = new HashMap();
            fila.put("despacho", "#" + d.getId());
            fila.put("factura", d.getOrden() != null ? "#" + d.getOrden().getId() : "N/A");
            fila.put("cliente", (d.getOrden() != null && d.getOrden().getCliente() != null)
                    ? d.getOrden().getCliente().getNombre()
                    : "Sin cliente");
            fila.put("tipo", d.getTipo() != null ? d.getTipo() : "RUTA");
            fila.put("repartidor", d.esRetiro()
                    ? "Retiro en bodega"
                    : (d.getRepartidor() != null ? d.getRepartidor().getUsername() : "Sin asignar"));
            fila.put("destino", construirDestino(d));
            fila.put("fecha", d.getFechaDespacho() != null ? d.getFechaDespacho().toString() : "");
            fila.put("estado", d.getEstado() != null ? d.getEstado() : "");
            filas.add(fila);
        }

        String etiquetaEstado = (estadoFiltro == null || "TODOS".equalsIgnoreCase(estadoFiltro))
                ? "Todos (Pendiente, En camino, Entregado)"
                : estadoFiltro;

        Map parametros = new HashMap();
        parametros.put("DESDE", desde.toString());
        parametros.put("HASTA", hasta.toString());
        parametros.put("ESTADO_FILTRO", etiquetaEstado);

        return MotorReportes.llenar(PLANTILLA, parametros, filas);
    }

    private String construirDestino(Despacho d) {
        if (d.esRetiro()) {
            return "Bodega central";
        }
        String dir = d.getDireccionEntrega() != null ? d.getDireccionEntrega() : "";
        String ruta = d.getRutaAsignada() != null ? d.getRutaAsignada() : "";
        if (!dir.isEmpty() && !ruta.isEmpty()) {
            return dir + " (" + ruta + ")";
        }
        return !dir.isEmpty() ? dir : (!ruta.isEmpty() ? ruta : "Sin dirección");
    }
}