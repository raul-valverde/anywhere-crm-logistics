package ni.edu.uam.anywherecrm.ui;

import ni.edu.uam.anywherecrm.util.Sesion;

// Módulos navegables del sistema: texto, vista FXML, ícono (SVG 24x24 en trazo)
// y roles con acceso (los mismos permisos que aplica MenuController).
public enum Modulo {
    INICIO("Inicio", "menu-view.fxml",
            "M3 9 L12 2 L21 9 V20 A2 2 0 0 1 19 22 H5 A2 2 0 0 1 3 20 Z M9 22 V12 H15 V22"),
    CLIENTES("Clientes", "clientes-view.fxml",
            "M17 21 v-2 a4 4 0 0 0 -4 -4 H5 a4 4 0 0 0 -4 4 v2 M5 7 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 M23 21 v-2 a4 4 0 0 0 -3 -3.87 M16 3.13 a4 4 0 0 1 0 7.75",
            "ADMIN", "VENDEDOR"),
    PROFORMAS("Proformas", "proformas-view.fxml",
            "M14 2 H6 a2 2 0 0 0 -2 2 v16 a2 2 0 0 0 2 2 h12 a2 2 0 0 0 2 -2 V8 Z M14 2 v6 h6 M16 13 H8 M16 17 H8 M10 9 H8",
            "ADMIN", "VENDEDOR"),
    INVENTARIO("Inventario", "inventario-view.fxml",
            "M21 16 V8 a2 2 0 0 0 -1 -1.73 l-7 -4 a2 2 0 0 0 -2 0 l-7 4 A2 2 0 0 0 3 8 v8 a2 2 0 0 0 1 1.73 l7 4 a2 2 0 0 0 2 0 l7 -4 A2 2 0 0 0 21 16 Z M3.27 6.96 L12 12.01 L20.73 6.96 M12 22.08 V12",
            "ADMIN", "BODEGUERO"),
    REPORTES("Reportes", "reportes-view.fxml",
            "M3 3 V21 H21 M18 17 V10 M13 17 V5 M8 17 V13",
            "ADMIN", "DESPACHADOR"),
    FACTURAS("Facturas", "facturas-view.fxml",
            "M4 2 V22 L7 20 L10 22 L12 20 L14 22 L17 20 L20 22 V2 L17 4 L14 2 L12 4 L10 2 L7 4 Z M8 8 H16 M8 12 H16 M8 16 H13",
            "ADMIN", "VENDEDOR"),
    DESPACHOS("Despachos", "despachos-view.fxml",
            "M1 3 H16 V16 H1 Z M16 8 H20 L23 11 V16 H16 Z M3 18.5 a2.5 2.5 0 1 0 5 0 a2.5 2.5 0 1 0 -5 0 M16 18.5 a2.5 2.5 0 1 0 5 0 a2.5 2.5 0 1 0 -5 0",
            "ADMIN", "DESPACHADOR");

    public static final String ICONO_LOGO = "M2 12 A10 10 0 1 0 22 12 A10 10 0 1 0 2 12 Z M2 12 H22 M12 2 A15.3 15.3 0 0 1 16 12 A15.3 15.3 0 0 1 12 22 A15.3 15.3 0 0 1 8 12 A15.3 15.3 0 0 1 12 2 Z";

    private final String titulo;
    private final String fxml;
    private final String icono;
    private final String[] roles;

    Modulo(String titulo, String fxml, String icono, String... roles) {
        this.titulo = titulo;
        this.fxml = fxml;
        this.icono = icono;
        this.roles = roles;
    }

    public String getTitulo() { return titulo; }
    public String getFxml() { return fxml; }
    public String getIcono() { return icono; }

    public boolean permitido() {
        return roles.length == 0 || Sesion.tieneRol(roles);
    }
}
