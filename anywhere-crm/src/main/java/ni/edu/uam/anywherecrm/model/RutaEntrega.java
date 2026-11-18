package ni.edu.uam.anywherecrm.model;

import jakarta.persistence.*;
import java.time.LocalDate;

// Un viaje del camión: sale de la bodega, entrega varias facturas y regresa.
// Cada Despacho que va en el viaje apunta a esta ruta (relación @ManyToOne en Despacho).
@Entity
@Table(name = "rutas_entrega")
public class RutaEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;
    private Integer paradas;     // Cuántas entregas lleva el camión
    private Double kilometros;   // Distancia total ida y vuelta (calculada por la API)
    private Double minutos;      // Tiempo total de manejo (calculado por la API)

    @Column(length = 2000)
    private String enlaceMapa;   // Enlace a Google Maps con todas las paradas en orden

    @ManyToOne // Muchas rutas pueden ser del mismo repartidor
    @JoinColumn(name = "repartidor_id")
    private Usuario repartidor;

    public RutaEntrega() {}

    // Getters y Setters
    public Long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Integer getParadas() { return paradas; }
    public void setParadas(Integer paradas) { this.paradas = paradas; }
    public Double getKilometros() { return kilometros; }
    public void setKilometros(Double kilometros) { this.kilometros = kilometros; }
    public Double getMinutos() { return minutos; }
    public void setMinutos(Double minutos) { this.minutos = minutos; }
    public String getEnlaceMapa() { return enlaceMapa; }
    public void setEnlaceMapa(String enlaceMapa) { this.enlaceMapa = enlaceMapa; }
    public Usuario getRepartidor() { return repartidor; }
    public void setRepartidor(Usuario repartidor) { this.repartidor = repartidor; }
}