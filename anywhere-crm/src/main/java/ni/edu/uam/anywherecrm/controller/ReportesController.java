package ni.edu.uam.anywherecrm.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import ni.edu.uam.anywherecrm.dao.ReportesDAO;
import java.util.List;

public class ReportesController {

    @FXML private ListView<String> listTopProductos;
    @FXML private ListView<String> listVentasVendedor;

    private final ReportesDAO reportesDAO = new ReportesDAO();

    @FXML
    public void initialize() {
        cargarReportes();
    }

    private void cargarReportes() {
        // Consulta 1 HQL: Top productos mas vendidos
        List<Object[]> topProd = reportesDAO.obtenerProductosMasVendidos();
        for (Object[] fila : topProd) {
            String item = "Producto: " + fila[0] + " | Total Vendido: " + fila[1];
            listTopProductos.getItems().add(item);
        }

        // Consulta 2 HQL: Ventas totales por vendedor
        List<Object[]> ventasVend = reportesDAO.obtenerVentasPorVendedor();
        for (Object[] fila : ventasVend) {
            String item = "Vendedor: " + fila[0] + " | Monto Acumulado: $" + fila[1];
            listVentasVendedor.getItems().add(item);
        }
    }
}
