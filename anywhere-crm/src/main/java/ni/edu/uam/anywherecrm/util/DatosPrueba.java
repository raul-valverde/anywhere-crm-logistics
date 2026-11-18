package ni.edu.uam.anywherecrm.util;
import ni.edu.uam.anywherecrm.config.HibernateUtil;
import ni.edu.uam.anywherecrm.dao.GenericDAO;
import ni.edu.uam.anywherecrm.dao.InventarioDAO;
import ni.edu.uam.anywherecrm.dao.ProformaDAO;
import ni.edu.uam.anywherecrm.model.*;
import org.hibernate.Session;
import org.hibernate.query.Page;

import java.util.List;
// Llena la base de datos con ejemplos solo donde está vacía, para probar y hacer la demo
        public class DatosPrueba {
    // Cada paso va por separado: si uno falla, los demás igual se intentan
    public static void cargarSiEstaVacio() {
        ejecutar("usuarios", DatosPrueba::crearUsuarios);
        ejecutar("productos", DatosPrueba::crearProductos);
        ejecutar("clientes", DatosPrueba::crearClientes);
        ejecutar("proforma de ejemplo", DatosPrueba::crearProformaEjemplo);
    }
    private static void ejecutar(String paso, Runnable accion) {
        try {
            accion.run();
        } catch (Exception e) {
            System.err.println("Datos de prueba (" + paso + "): " +
                    e.getMessage());
        }
    }

    // Un usuario de prueba por cada rol
            private static void crearUsuarios() {
                crearUsuarioSiNoExiste("admin", "admin123", "ADMIN");
                crearUsuarioSiNoExiste("vendedor", "vendedor123", "VENDEDOR");
                crearUsuarioSiNoExiste("bodega", "bodega123", "BODEGUERO");
                crearUsuarioSiNoExiste("despacho", "despacho123", "DESPACHADOR");
            }
            private static void crearUsuarioSiNoExiste(String username, String
                    password, String rol) {
                if (buscarUsuario(username) == null) {
                    new GenericDAO<>(Usuario.class).guardar(new Usuario(username,
                            password, rol));
                }
            }
            private static Usuario buscarUsuario(String username) {
                try (Session session =
                             HibernateUtil.getSessionFactory().openSession()) {
                    return session.createQuery("FROM Usuario u WHERE u.username = :user", Usuario.class)
                            .setParameter("user", username)
                            .uniqueResult();
                }
            }
            private static void crearProductos() {
                if (!new GenericDAO<>(Producto.class).obtenerTodos().isEmpty())
                    return;
                GenericDAO<Categoria> categoriaDAO = new GenericDAO<>
                        (Categoria.class);
                Categoria construccion = new Categoria();
                construccion.setNombre("Construcción");
                construccion.setDescripcion("Materiales de obra");
                categoriaDAO.guardar(construccion);
                Categoria ferreteria = new Categoria();
                ferreteria.setNombre("Ferretería");
                ferreteria.setDescripcion("Herramientas y accesorios");
                categoriaDAO.guardar(ferreteria);
                crearProducto("Cemento 42.5 kg", 9.50, construccion, 200);
                crearProducto("Varilla 3/8", 4.25, construccion, 500);
                crearProducto("Martillo", 7.80, ferreteria, 40);
                crearProducto("Caja de clavos 2 pulgadas", 3.10, ferreteria, 120);
            }
            // El producto nace con stock 0 y recibe su stock con una ENTRADA, así queda en el historial
            private static void crearProducto(String nombre, double precio, Categoria
                    categoria, int stockInicial) {
                Producto producto = new Producto();
                producto.setNombre(nombre);
                producto.setPrecio(precio);
                producto.setStock(0);
                producto.setCategoria(categoria);
                new GenericDAO<>(Producto.class).guardar(producto);
                new InventarioDAO().registrarMovimiento(producto.getId(), "ENTRADA",
                        stockInicial, "Inventario inicial");
            }
            private static void crearClientes() {
                GenericDAO<Cliente> clienteDAO = new GenericDAO<>(Cliente.class);
                if (!clienteDAO.obtenerTodos().isEmpty()) return;
                String[][] datos = {
                        {"Constructora Los Robles", "2222-1111",
                                "compras@losrobles.test"},
                        {"Ferretería El Martillo", "2222-2222",
                                "ventas@elmartillo.test"},
                        {"Juan Pérez", "8888-3333", ""}
                };
                for (String[] fila : datos) {
                    Cliente cliente = new Cliente();
                    cliente.setNombre(fila[0]);
                    cliente.setTelefono(fila[1]);
                    cliente.setEmail(fila[2].isEmpty() ? null : fila[2]);
                    clienteDAO.guardar(cliente);
                }
            }
// Una proforma pendiente con dos productos, para probar la pantalla de Kellys
            private static void crearProformaEjemplo() {
                ProformaDAO proformaDAO = new ProformaDAO();
                List<Cliente> clientes = new GenericDAO<>
                        (Cliente.class).obtenerTodos();
                List<Producto> productos = new GenericDAO<>
                        (Producto.class).obtenerTodos();
                if (!proformaDAO.obtenerTodas().isEmpty() || clientes.isEmpty() ||
                        productos.size() < 2) return;
                Proforma proforma = new Proforma();
                proforma.setCliente(clientes.get(0));
                proforma.setVendedor(buscarUsuario("vendedor"));
                List<DetalleProforma> detalles = List.of(
                        new DetalleProforma(productos.get(0), 10),
                        new DetalleProforma(productos.get(1), 25));
                proformaDAO.guardarConDetalles(proforma, detalles);
            }
        }
