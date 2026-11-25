package ni.edu.uam.anywherecrm.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Celda limpia para los resultados de las consultas HQL de Reportes.
 * Recibe el mismo texto que arma ReportesController, por ejemplo
 *   "Producto: Laptop | Total Vendido: 42"  o  "Vendedor: ana | Monto Acumulado: $1500.0"
 * y lo muestra como: [posición]  nombre ............ etiqueta  VALOR (en negrita).
 * Las filas vacías no se pintan, así no aparecen renglones grises de relleno.
 */
public class CeldaResultado extends ListCell<String> {

    private final Label posicion = new Label();
    private final Label tipo = new Label();
    private final Label nombre = new Label();
    private final Label clave = new Label();
    private final Label valor = new Label();
    private final HBox fila;

    public CeldaResultado() {
        posicion.getStyleClass().add("rank-badge");
        tipo.getStyleClass().add("result-key");
        nombre.getStyleClass().add("result-name");
        clave.getStyleClass().add("result-key");
        valor.getStyleClass().add("result-value");

        VBox izquierda = new VBox(1, tipo, nombre);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        VBox derecha = new VBox(1, clave, valor);
        derecha.setAlignment(Pos.CENTER_RIGHT);

        fila = new HBox(12, posicion, izquierda, espacio, derecha);
        fila.setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("result-cell");
    }

    @Override
    protected void updateItem(String texto, boolean vacio) {
        super.updateItem(texto, vacio);
        setText(null);
        if (vacio || texto == null) {
            setGraphic(null);
            return;
        }
        String[] partes = texto.split("\\s*\\|\\s*", 2);
        String[] izquierda = separar(partes[0]);
        tipo.setText(izquierda[0]);
        nombre.setText(izquierda[1]);
        if (partes.length > 1) {
            String[] derecha = separar(partes[1]);
            clave.setText(derecha[0]);
            valor.setText(formatear(derecha[1]));
        } else {
            clave.setText("");
            valor.setText("");
        }
        posicion.setText(String.valueOf(getIndex() + 1));
        setGraphic(fila);
    }

    // "Producto: Laptop" -> {"Producto", "Laptop"}; sin ":" se usa el texto completo como nombre
    private static String[] separar(String parte) {
        int dosPuntos = parte.indexOf(':');
        if (dosPuntos < 0) return new String[]{"", parte.trim()};
        return new String[]{parte.substring(0, dosPuntos).trim(), parte.substring(dosPuntos + 1).trim()};
    }

    // "$1500.0" -> "$1,500.00"; "42" se deja igual
    private static String formatear(String valor) {
        boolean dinero = valor.startsWith("$");
        String numero = dinero ? valor.substring(1) : valor;
        if (!dinero) return valor;
        try {
            return String.format("$%,.2f", Double.parseDouble(numero));
        } catch (NumberFormatException e) {
            return valor;
        }
    }
}
