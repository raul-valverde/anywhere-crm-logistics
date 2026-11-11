package ni.edu.uam.anywherecrm.model;

import jakarta.persistence.*;

@Entity // Indica a Hibernate que esta clase es una tabla en la BD
@Table(name = "usuarios") // Nombre exacto de la tabla en PostgreSQL
public class Usuario {

    @Id // Marca este campo como la Clave Primaria (PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Hace que el ID sea autoincrementable (SERIAL)
    private Long id;

    @Column(nullable = false, unique = true) // El usuario es obligatorio y no se puede repetir
    private String username;

    @Column(nullable = false) // La contraseña es obligatoria
    private String password;

    @Column(nullable = false)
    private String rol; // Valores posibles: ADMIN, VENDEDOR, BODEGUERO, DESPACHADOR

    // Constructor vacío obligatorio para que JPA/Hibernate funcione
    public Usuario() {}

    public Usuario(String username, String password, String rol) {
        this.username = username;
        this.password = password;
        this.rol = rol;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}