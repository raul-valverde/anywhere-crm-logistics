package ni.edu.uam.anywherecrm.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "despachos")
public class Despacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String direccionEntrega;
    private String rutaAsignada;
    private String estado; // PENDIENTE, EN_CAMINO, ENTREGADO
    private LocalDate fechaDespacho;

    @ManyToOne // Muchos despachos pueden ser asignados a UN mismo repartidor/despachador
    @JoinColumn(name = "repartidor_id") // Nombre de la Clave Foránea (FK) en la base de datos
    private Usuario repartidor;

    @ManyToOne // Cada despacho corresponde a UNA orden (factura)
    @JoinColumn(name = "orden_id")
    private Orden orden;

    private String tipo; // RUTA (sale en camión) o RETIRO (el cliente llega a bodega)

    public Despacho() {}

    // Getters y Setters
    public Long getId() { return id; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
    public String getRutaAsignada() { return rutaAsignada; }
    public void setRutaAsignada(String rutaAsignada) { this.rutaAsignada = rutaAsignada; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDate getFechaDespacho() { return fechaDespacho; }
    public void setFechaDespacho(LocalDate fechaDespacho) { this.fechaDespacho = fechaDespacho; }
    public Usuario getRepartidor() { return repartidor; }
    public void setRepartidor(Usuario repartidor) { this.repartidor = repartidor; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Orden getOrden() { return orden; }
    public void setOrden(Orden orden) { this.orden = orden; }
    // Un retiro en bodega no necesita ruta ni repartidor
    public boolean esRetiro() { return "RETIRO".equals(tipo); }

}