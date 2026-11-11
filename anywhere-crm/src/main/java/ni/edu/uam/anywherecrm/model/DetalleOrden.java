package ni.edu.uam.anywherecrm.model;

import jakarta.persistence.*;

@Entity
@Table(name = "detalles_orden")
public class DetalleOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;

    @ManyToOne // Muchos detalles pertenecen a UNA sola Orden principal
    @JoinColumn(name = "orden_id")
    private Orden orden;

    @ManyToOne // Muchos detalles se refieren a UN Producto del catálogo
    @JoinColumn(name = "producto_id")
    private Producto producto;

    public DetalleOrden() {}

    // Getters y Setters
    public Long getId() { return id; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }
    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    public Orden getOrden() { return orden; }
    public void setOrden(Orden orden) { this.orden = orden; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
}