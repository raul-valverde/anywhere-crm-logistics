package ni.edu.uam.anywherecrm.reportes;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Motor común de JasperReports: todos los reportes del sistema pasan por aquí.
// Hace los 3 pasos de Jasper (compilar, llenar, exportar) y agrega Excel y la imagen para la vista previa.
public final class MotorReportes {

    // Plantillas ya compiladas: compilar un .jrxml es lo más lento, así se hace una sola vez por plantilla
    private static final Map<String, JasperReport> COMPILADAS = new HashMap<>();

    private MotorReportes() {
        // Clase de utilidades: solo métodos static, no se crean objetos
    }

    // Paso 1 y 2: compila la plantilla (si hace falta) y la llena con los datos.
    // El resultado (JasperPrint) es el reporte ya armado en memoria: se puede ver, imprimir o exportar.
    public static JasperPrint llenar(String plantilla, Map<String, Object> parametros, List<Map<String, ?>> filas)
            throws JRException, IOException {
        JasperReport reporte = compilar(plantilla);
        return JasperFillManager.fillReport(reporte, parametros, new JRMapCollectionDataSource(filas));
    }

    private static synchronized JasperReport compilar(String plantilla) throws JRException, IOException {
        JasperReport reporte = COMPILADAS.get(plantilla);
        if (reporte == null) {
            try (InputStream entrada = MotorReportes.class.getResourceAsStream(plantilla)) {
                if (entrada == null) {
                    throw new IOException("No se encontró la plantilla " + plantilla);
                }
                reporte = JasperCompileManager.compileReport(entrada);
                COMPILADAS.put(plantilla, reporte);
            }
        }
        return reporte;
    }

    // Paso 3 (PDF): el mismo JasperPrint se guarda como documento PDF
    public static void exportarPdf(JasperPrint impresion, File destino) throws JRException {
        JasperExportManager.exportReportToPdfFile(impresion, destino.getAbsolutePath());
    }

    // Paso 3 (Excel): el mismo JasperPrint se guarda como hoja de cálculo .xlsx
    public static void exportarExcel(JasperPrint impresion, File destino) throws JRException {
        SimpleXlsxReportConfiguration configuracion = new SimpleXlsxReportConfiguration();
        configuracion.setOnePagePerSheet(false);                 // Todas las páginas seguidas en una sola hoja
        configuracion.setDetectCellType(true);                   // Los números quedan como números: Excel los puede sumar
        configuracion.setRemoveEmptySpaceBetweenRows(true);      // Sin filas vacías entre los datos
        configuracion.setRemoveEmptySpaceBetweenColumns(true);   // Sin columnas vacías entre los datos
        configuracion.setWhitePageBackground(false);             // Fondo normal de Excel
        configuracion.setCollapseRowSpan(false);
        configuracion.setSheetNames(new String[]{nombreHoja(impresion)});

        JRXlsxExporter exportador = new JRXlsxExporter();
        exportador.setExporterInput(new SimpleExporterInput(impresion));
        exportador.setExporterOutput(new SimpleOutputStreamExporterOutput(destino));
        exportador.setConfiguration(configuracion);
        exportador.exportReport();
    }

    // Decide el formato por la extensión del archivo que eligió el usuario
    public static void exportar(JasperPrint impresion, File destino) throws JRException {
        if (destino.getName().toLowerCase().endsWith(".xlsx")) {
            exportarExcel(impresion, destino);
        } else {
            exportarPdf(impresion, destino);
        }
    }

    // Dibuja una página del reporte como imagen. zoom 1 = tamaño real, 1.5 = 150 %
    public static BufferedImage dibujarPagina(JasperPrint impresion, int pagina, float zoom) throws JRException {
        return (BufferedImage) JasperPrintManager.printPageToImage(impresion, pagina, zoom);
    }

    public static int totalPaginas(JasperPrint impresion) {
        return impresion.getPages().size();
    }

    // Excel no acepta nombres de hoja de más de 31 caracteres
    private static String nombreHoja(JasperPrint impresion) {
        String nombre = impresion.getName() == null ? "Reporte" : impresion.getName();
        nombre = Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
        return nombre.length() > 31 ? nombre.substring(0, 31) : nombre;
    }
}