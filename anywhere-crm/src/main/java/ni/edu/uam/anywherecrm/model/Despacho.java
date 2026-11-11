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
}