package ni.edu.uam.anywherecrm.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "proformas")
public class Proforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fechaEmision;
    private Double montoEstimado;

    @ManyToOne // Muchas proformas (cotizaciones) pueden ser para UN solo cliente
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne // Muchas proformas pueden ser creadas por UN mismo vendedor
    @JoinColumn(name = "vendedor_id")
    private Usuario vendedor;

    public Proforma() {}

    // Getters y Setters
    public Long getId() { return id; }
    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }
    public Double getMontoEstimado() { return montoEstimado; }
    public void setMontoEstimado(Double montoEstimado) { this.montoEstimado = montoEstimado; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Usuario getVendedor() { return vendedor; }
    public void setVendedor(Usuario vendedor) { this.vendedor = vendedor; }
}
