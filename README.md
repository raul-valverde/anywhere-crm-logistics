# Anywhere CRM - Sistema de Gestión Logística

## Descripción del Proyecto
Sistema integral de escritorio para la gestión de inventario, proformas, facturación y despachos. El proyecto busca automatizar y optimizar los procesos comerciales y logísticos mediante una interfaz gráfica intuitiva conectada a una base de datos centralizada en la nube.

## Equipo de Desarrollo
* **[Raúl Valverde](https://github.com/raul-valverde)** - Líder del equipo
* **[Kellys Bellanger](https://github.com/kellys-bellanger)** - Desarrolladora
* **[Steven Flores](https://github.com/st3v3n030707 )** - Desarrollador
* **[Nora Obregon](https://github.com/nora3071)** - Desarrolladora

## Tecnologías Utilizadas
* **Lenguaje:** Java 21
* **Interfaz Gráfica:** JavaFX
* **Persistencia de Datos:** Hibernate ORM
* **Base de Datos:** PostgreSQL (Alojado en la nube mediante Neon.tech)
* **Gestor de Dependencias y Build:** Maven

## Arquitectura y Estructura
El proyecto sigue el patrón de diseño MVC (Modelo-Vista-Controlador) combinado con el patrón DAO (Data Access Object) para separar la lógica de negocio, la interfaz visual y el acceso a la base de datos.

**Paquetes principales (`ni.edu.uam.anywherecrm`):**
* `config/`: Archivos de configuración y utilidades (ej. `HibernateUtil`).
* `controller/`: Controladores de las vistas FXML (`LoginController`, `InventarioController`, `ClientesController`, `ReportesController`).
* `dao/`: Clases encargadas de las transacciones con la base de datos (`GenericDAO`, `ReportesDAO`).
* `model/`: Entidades mapeadas a la base de datos (`Usuario`, `Producto`, `Categoria`, `Cliente`, `Proforma`, `Orden`, `DetalleOrden`, `Despacho`).
* `resources/`: Vistas de la aplicación (`login-view.fxml`, `clientes-view.fxml`, `inventario-view.fxml`, `reportes-view.fxml`) y configuración de conexión (`hibernate.cfg.xml`).

## Estado Actual y Avances (Fase 1)
* [x] Estructuración del proyecto en Maven y creación de paquetes.
* [x] Modelado de entidades y mapeo objeto-relacional (ORM).
* [x] Conexión exitosa a la base de datos PostgreSQL en Neon.tech.
* [x] Creación de las interfaces base (archivos FXML).
* [x] Implementación funcional del inicio de sesión (Login) con validación de credenciales reales contra la base de datos.
* [ ] (Pendiente) Desarrollo de la tabla interactiva (TableView) para el módulo de Clientes.
* [ ] (Pendiente) Desarrollo del CRUD para el módulo de Inventario.
* [ ] (Pendiente) Revisión con el docente sobre la implementación final de la capa de servicios (Conexión Directa vs API REST con Spring Boot).

## Instrucciones de Ejecución
1. Clonar el repositorio en el entorno local.
2. Abrir el proyecto en IntelliJ IDEA (o IDE de preferencia) y recargar los cambios de Maven (`Load Maven Changes`) para descargar el driver de PostgreSQL, Hibernate y JavaFX.
3. El archivo `hibernate.cfg.xml` ya está configurado para apuntar al clúster de Neon.tech. No se requiere servidor local, **se requiere la contraseña para el programa del equipo
   si no tiene que cambiar las credenciales a su base de datos**.
5. Ejecutar la clase `Launcher.java` (o `App.java`).
6. **Credenciales de acceso de prueba:(En nuestra base de datos)**
   * **Usuario:** `admin`
   * **Contraseña:** `admin123`
