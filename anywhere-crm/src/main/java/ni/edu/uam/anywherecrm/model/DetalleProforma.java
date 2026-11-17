package ni.edu.uam.anywherecrm.model;
import jakarta.persistence.*;
@Entity
@Table(name = "detalles_proforma")
public class DetalleProforma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;
    @ManyToOne // Muchos detalles pertenecen a UNA proforma
    @JoinColumn(name = "proforma_id")
    private Proforma proforma;
    @ManyToOne // Cada detalle se refiere a UN producto del catálogo
    @JoinColumn(name = "producto_id")
    private Producto producto;
    public DetalleProforma() {}
    // Crea el detalle tomando el precio actual del producto
    public DetalleProforma(Producto producto, Integer cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecio();
        calcularSubtotal();
    }
    // subtotal = cantidad por precio unitario
    public void calcularSubtotal() {
        this.subtotal = cantidad * precioUnitario;
    }
    // Getters y Setters
    public Long getId() { return id; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) {
        this.precioUnitario = precioUnitario; }
    public Double getSubtotal() { return subtotal; }
    public Proforma getProforma() { return proforma; }
    public void setProforma(Proforma proforma) { this.proforma = proforma; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
}

