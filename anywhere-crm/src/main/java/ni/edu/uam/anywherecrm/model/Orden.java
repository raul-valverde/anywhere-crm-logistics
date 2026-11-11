package ni.edu.uam.anywherecrm.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "ordenes")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;
    private Double total;
    private String estado; // COMPLETED, PENDING, CANCELLED

    @ManyToOne // Muchas órdenes pertenecen a UN Cliente
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne // Muchas órdenes son registradas por UN Usuario (Vendedor)
    @JoinColumn(name = "vendedor_id")
    private Usuario vendedor;

    public Orden() {}

    // Getters y Setters
    public Long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Usuario getVendedor() { return vendedor; }
    public void setVendedor(Usuario vendedor) { this.vendedor = vendedor; }
}