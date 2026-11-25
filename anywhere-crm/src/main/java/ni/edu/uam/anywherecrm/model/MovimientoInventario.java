package ni.edu.uam.anywherecrm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tipo; // ENTRADA o SALIDA

    @Column(nullable = false)
    private Integer cantidad;

    private String motivo; // Ej: "Compra a proveedor", "Factura #5"

    private LocalDateTime fecha;

    @ManyToOne // Muchos movimientos pertenecen a UN producto
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne // Muchos movimientos pueden ser registrados por UN usuario
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public MovimientoInventario() {}

    public MovimientoInventario(Producto producto, String tipo, Integer cantidad, String motivo) {
        this.producto = producto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.fecha = LocalDateTime.now();
    }

    public MovimientoInventario(Producto producto, String tipo, Integer cantidad, String motivo, Usuario usuario) {
        this(producto, tipo, cantidad, motivo);
        this.usuario = usuario;
    }

    // Getters y Setters
    public Long getId() { return id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
