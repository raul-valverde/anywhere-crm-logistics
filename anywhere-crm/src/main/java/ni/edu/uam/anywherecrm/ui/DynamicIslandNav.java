package ni.edu.uam.anywherecrm.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import ni.edu.uam.anywherecrm.model.Usuario;
import ni.edu.uam.anywherecrm.util.Sesion;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Navegación flotante estilo "Isla Dinámica" en la parte superior central.
 *
 * Uso en el FXML (raíz StackPane: capa de contenido + esta capa flotante encima):
 * <pre>
 *   &lt;StackPane ...&gt;
 *       &lt;ScrollPane styleClass="content-layer"&gt; ...contenido... &lt;/ScrollPane&gt;
 *       &lt;DynamicIslandNav activo="CLIENTES"/&gt;
 *   &lt;/StackPane&gt;
 * </pre>
 *
 * - Contraída (por defecto): punto cian + ícono y nombre del módulo actual + chevron.
 * - Expandida (clic en la cápsula): logo | 7 módulos (solo el activo con ícono) | usuario + Cerrar sesión.
 * - Se contrae sola al elegir un módulo, al hacer clic fuera de la cápsula o con ESC.
 * - Esta capa es transparente y no captura clics fuera de la cápsula (pickOnBounds = false).
 *
 * El Menú Principal puede entregarle sus propios botones (con sus fx:id y onAction originales)
 * mediante las propiedades "modulos" y "botonSalir"; la isla solo les da estilo y los coloca.
 */
public class DynamicIslandNav extends StackPane {

    private static final double ALTO_CONTRAIDO = 42;
    private static final double ALTO_EXPANDIDO = 52;
    private static final double ANCHO_MIN_EXPANDIDO = 920;
    private static final double ANCHO_MAX_EXPANDIDO = 1050;
    private static final double MARGEN_LATERAL = 24;
    private static final Duration DURACION = Duration.millis(200);

    private Modulo activo = Modulo.INICIO;
    private boolean expandida = false;
    private Timeline animacion;

    // Tamaño animado de la cápsula (ancho < 0 = "lo que mida la vista contraída")
    private final DoubleProperty ancho = new SimpleDoubleProperty(-1);
    private final DoubleProperty alto = new SimpleDoubleProperty(ALTO_CONTRAIDO);

    // Vista contraída
    private final SVGPath iconoActivo = icono("", "island-icon");
    private final Label nombreActivo = new Label();
    private final HBox vistaContraida;

    // Vista expandida
    private final HBox zonaModulos = new HBox(4);
    private final HBox zonaDerecha = new HBox(10);
    private final Label usuario = new Label();
    private final HBox vistaExpandida;
    private final Map<Modulo, Button> botones = new EnumMap<>(Modulo.class);
    private Button botonSalir;

    // Botones aportados desde el FXML (Menú Principal)
    private final ObservableList<Button> modulos = FXCollections.observableArrayList();

    private Capsula capsula; // Se crea en el constructor (la usan los manejadores de clic fuera / ESC)

    // Clic fuera de la cápsula o ESC: se contrae
    private final EventHandler<MouseEvent> clicFuera = e -> {
        if (expandida && !capsula.localToScene(capsula.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY())) {
            contraer();
        }
    };
    private final EventHandler<KeyEvent> teclaEsc = e -> {
        if (expandida && e.getCode() == KeyCode.ESCAPE) contraer();
    };

    public DynamicIslandNav() {
        getStyleClass().add("island-layer");
        setPickOnBounds(false); // La capa transparente deja pasar los clics a la pantalla de abajo
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(12, 0, 0, 0));

        // ----- Vista contraída: ● [ícono] Módulo ⌄
        Circle punto = new Circle(4);
        punto.getStyleClass().add("island-dot");
        iconoActivo.setScaleX(0.75);
        iconoActivo.setScaleY(0.75);
        nombreActivo.getStyleClass().add("island-title");
        SVGPath chevron = icono("M6 9 L12 15 L18 9", "island-chevron");
        chevron.setScaleX(0.6);
        chevron.setScaleY(0.6);
        vistaContraida = new HBox(8, punto, iconoActivo, nombreActivo, chevron);
        vistaContraida.setAlignment(Pos.CENTER);
        vistaContraida.getStyleClass().add("island-compact");

        // ----- Vista expandida: logo | módulos | usuario + salir
        SVGPath logo = icono(Modulo.ICONO_LOGO, "island-logo-icon");
        logo.setScaleX(0.8);
        logo.setScaleY(0.8);
        Label marca = new Label("anywhere");
        marca.getStyleClass().add("island-logo-text");
        HBox zonaLogo = new HBox(6, logo, marca);
        zonaLogo.setAlignment(Pos.CENTER_LEFT);
        zonaLogo.setMinWidth(Region.USE_PREF_SIZE);

        zonaModulos.setAlignment(Pos.CENTER);
        HBox.setHgrow(zonaModulos, Priority.ALWAYS);
        for (Modulo modulo : Modulo.values()) {
            Button boton = new Button(modulo.getTitulo());
            boton.setDisable(!modulo.permitido());
            boton.setOnAction(e -> navegar(modulo));
            colocarBoton(modulo, boton);
        }

        usuario.getStyleClass().add("island-user");
        Usuario actual = Sesion.getUsuarioActual();
        usuario.setText(actual == null ? "" : actual.getUsername() + " (" + actual.getRol() + ")");
        Region divisor = new Region();
        divisor.getStyleClass().add("island-divider");
        Button salir = new Button("Cerrar sesión");
        salir.setOnAction(e -> cerrarSesion());
        zonaDerecha.setAlignment(Pos.CENTER_RIGHT);
        zonaDerecha.setMinWidth(Region.USE_PREF_SIZE);
        zonaDerecha.getChildren().addAll(divisor, usuario);
        setBotonSalir(salir);

        vistaExpandida = new HBox(16, zonaLogo, zonaModulos, zonaDerecha);
        vistaExpandida.setAlignment(Pos.CENTER);
        vistaExpandida.getStyleClass().add("island-expanded");
        vistaExpandida.setOpacity(0);
        vistaExpandida.setMouseTransparent(true);

        capsula = new Capsula();
        capsula.setOnMouseClicked(e -> {
            if (!expandida) expandir();
        });
        getChildren().add(capsula);

        // Botones que llegan desde el FXML del menú (conservan su fx:id y su onAction)
        modulos.addListener((ListChangeListener<Button>) cambio -> {
            while (cambio.next()) {
                for (Button boton : cambio.getAddedSubList()) adoptarBoton(boton);
            }
        });

        // El clic fuera / ESC se escuchan en la pantalla que contiene a la isla
        parentProperty().addListener((obs, anterior, nuevo) -> {
            if (anterior != null) {
                anterior.removeEventFilter(MouseEvent.MOUSE_PRESSED, clicFuera);
                anterior.removeEventFilter(KeyEvent.KEY_PRESSED, teclaEsc);
            }
            if (nuevo != null) {
                nuevo.addEventFilter(MouseEvent.MOUSE_PRESSED, clicFuera);
                nuevo.addEventFilter(KeyEvent.KEY_PRESSED, teclaEsc);
            }
        });

        actualizarActivo();
    }

    // ===== Propiedades para el FXML =====

    // activo="CLIENTES"
    public String getActivo() {
        return activo.name();
    }

    public void setActivo(String nombre) {
        activo = Modulo.valueOf(nombre.trim().toUpperCase());
        actualizarActivo();
    }

    // <modulos> Botones del menú con userData="CLIENTES", etc. </modulos>
    public ObservableList<Button> getModulos() {
        return modulos;
    }

    // <botonSalir> Botón "Cerrar sesión" del menú (onAction="#handleCerrarSesion") </botonSalir>
    public Button getBotonSalir() {
        return botonSalir;
    }

    public void setBotonSalir(Button boton) {
        if (botonSalir != null) zonaDerecha.getChildren().remove(botonSalir);
        botonSalir = boton;
        if (!boton.getStyleClass().contains("island-logout")) boton.getStyleClass().add("island-logout");
        boton.setMinWidth(Region.USE_PREF_SIZE);
        zonaDerecha.getChildren().add(boton);
    }

    public boolean isExpandida() {
        return expandida;
    }

    // ===== Animaciones =====

    public void expandir() {
        if (expandida) return;
        expandida = true;
        double disponible = getWidth() > 0 ? getWidth() - 2 * MARGEN_LATERAL : ANCHO_MAX_EXPANDIDO;
        double natural = vistaExpandida.prefWidth(-1);
        double objetivo = Math.min(Math.max(natural, ANCHO_MIN_EXPANDIDO), Math.min(ANCHO_MAX_EXPANDIDO, disponible));
        vistaExpandida.setPrefWidth(objetivo);
        vistaExpandida.setMouseTransparent(false);
        vistaContraida.setMouseTransparent(true);
        capsula.getStyleClass().remove("island-collapsed");
        animar(objetivo, ALTO_EXPANDIDO, 0, 1);
    }

    public void contraer() {
        if (!expandida) return;
        expandida = false;
        vistaExpandida.setMouseTransparent(true);
        vistaContraida.setMouseTransparent(false);
        capsula.getStyleClass().add("island-collapsed");
        animar(vistaContraida.prefWidth(-1), ALTO_CONTRAIDO, 1, 0);
    }

    private void animar(double anchoFinal, double altoFinal, double opacidadContraida, double opacidadExpandida) {
        if (animacion != null) animacion.stop();
        if (ancho.get() < 0) ancho.set(capsula.getWidth() > 0 ? capsula.getWidth() : vistaContraida.prefWidth(-1));
        animacion = new Timeline(new KeyFrame(DURACION,
                new KeyValue(ancho, anchoFinal, Interpolator.EASE_OUT),
                new KeyValue(alto, altoFinal, Interpolator.EASE_OUT),
                new KeyValue(vistaContraida.opacityProperty(), opacidadContraida, Interpolator.EASE_OUT),
                new KeyValue(vistaExpandida.opacityProperty(), opacidadExpandida, Interpolator.EASE_OUT)));
        animacion.play();
    }

    // ===== Botones y navegación =====

    private void colocarBoton(Modulo modulo, Button boton) {
        Button anterior = botones.put(modulo, boton);
        if (!boton.getStyleClass().contains("island-link")) boton.getStyleClass().add("island-link");
        boton.setFocusTraversable(false);
        boton.setMinWidth(Region.USE_PREF_SIZE);
        // Al elegir cualquier módulo la isla se contrae (no reemplaza el onAction propio del botón)
        boton.addEventHandler(ActionEvent.ACTION, e -> contraer());
        int posicion = anterior == null ? -1 : zonaModulos.getChildren().indexOf(anterior);
        if (posicion >= 0) {
            zonaModulos.getChildren().set(posicion, boton);
        } else {
            zonaModulos.getChildren().add(boton);
        }
        decorar(modulo, boton);
    }

    // Botón del menú: se identifica por userData="CLIENTES" (o por su texto)
    private void adoptarBoton(Button boton) {
        Object dato = boton.getUserData();
        String clave = dato != null ? dato.toString() : boton.getText();
        for (Modulo modulo : Modulo.values()) {
            if (modulo.name().equalsIgnoreCase(clave.trim()) || modulo.getTitulo().equalsIgnoreCase(clave.trim())) {
                colocarBoton(modulo, boton);
                return;
            }
        }
    }

    private void actualizarActivo() {
        iconoActivo.setContent(activo.getIcono());
        nombreActivo.setText(activo.getTitulo());
        botones.forEach(this::decorar);
    }

    // Regla visual: solo el módulo activo lleva ícono (cian); los demás, solo texto
    private void decorar(Modulo modulo, Button boton) {
        boton.getStyleClass().remove("island-active");
        if (modulo == activo) {
            boton.getStyleClass().add("island-active");
            SVGPath svg = icono(modulo.getIcono(), "island-icon");
            svg.setScaleX(0.7);
            svg.setScaleY(0.7);
            boton.setGraphic(svg);
        } else {
            boton.setGraphic(null);
        }
    }

    private void navegar(Modulo destino) {
        if (destino == activo) return; // Ya estamos aquí
        try {
            if (destino == Modulo.INICIO) {
                Ventanas.irAlMenu();
            } else {
                Ventanas.abrirModulo(destino.getFxml(), destino.getTitulo());
            }
        } catch (IOException ex) {
            alerta("No se pudo abrir " + destino.getTitulo() + ": " + ex.getMessage());
        }
    }

    private void cerrarSesion() {
        try {
            Ventanas.cerrarSesion();
        } catch (IOException ex) {
            alerta("No se pudo volver al login: " + ex.getMessage());
        }
    }

    private static SVGPath icono(String contenido, String clase) {
        SVGPath svg = new SVGPath();
        svg.setContent(contenido);
        svg.getStyleClass().add(clase);
        return svg;
    }

    private static void alerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // ===== Cápsula: fondo, borde y sombra; su tamaño sigue a las propiedades animadas =====
    private final class Capsula extends Region {
        private final Pane lienzo = new Pane() {
            @Override
            protected void layoutChildren() {
                // Ambas vistas a su tamaño natural, centradas; lo que sobresale lo recorta el clip
                for (Node vista : getChildren()) {
                    double w = vista == vistaExpandida ? vistaExpandida.getPrefWidth() : vista.prefWidth(-1);
                    if (w <= 0) w = vista.prefWidth(-1);
                    double h = vista.prefHeight(w);
                    vista.resizeRelocate(Math.round((getWidth() - w) / 2), Math.round((getHeight() - h) / 2), w, h);
                }
            }
        };

        Capsula() {
            getStyleClass().addAll("island", "island-collapsed");
            lienzo.getChildren().addAll(vistaContraida, vistaExpandida);
            Rectangle recorte = new Rectangle();
            recorte.widthProperty().bind(lienzo.widthProperty());
            recorte.heightProperty().bind(lienzo.heightProperty());
            recorte.arcWidthProperty().bind(lienzo.heightProperty());
            recorte.arcHeightProperty().bind(lienzo.heightProperty());
            lienzo.setClip(recorte); // El recorte va en el lienzo, así la sombra de la cápsula no se corta
            getChildren().add(lienzo);
            ancho.addListener((o, a, n) -> requestLayout());
            alto.addListener((o, a, n) -> requestLayout());
        }

        @Override
        protected double computePrefWidth(double h) {
            return ancho.get() < 0 ? Math.ceil(vistaContraida.prefWidth(-1)) : ancho.get();
        }

        @Override
        protected double computePrefHeight(double w) {
            return alto.get();
        }

        @Override
        protected double computeMinWidth(double h) {
            return computePrefWidth(h);
        }

        @Override
        protected double computeMinHeight(double w) {
            return computePrefHeight(w);
        }

        @Override
        protected double computeMaxWidth(double h) {
            return computePrefWidth(h);
        }

        @Override
        protected double computeMaxHeight(double w) {
            return computePrefHeight(w);
        }

        @Override
        protected void layoutChildren() {
            lienzo.resizeRelocate(0, 0, getWidth(), getHeight());
        }
    }

    // Evita que la capa transparente aporte tamaño mínimo a la pantalla
    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 0;
    }

}
