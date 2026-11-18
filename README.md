# Anywhere CRM Logistics
Aplicación de escritorio para una empresa de logística: clientes, proformas,
facturas, inventario, despachos con cálculo de rutas y reportes en PDF.
Proyecto final de Programación, UAM.
## Equipo
| Integrante | Rol |
| --- | --- |
| Raúl Valverde | Líder. Clientes, facturas en PDF, API de rutas |
| Kellys Bellanger | Proformas y reporte de ventas |
| Steven Flores | Inventario, facturación y reporte de inventario |
| Nora Obregón | Sesión y menú por rol, despachos, calidad y documentación |
## Funcionalidades
- Inicio de sesión y menú que muestra solo los módulos de cada rol (ADMIN,
  VENDEDOR, BODEGUERO, DESPACHADOR).
- Clientes: registrar, buscar, editar y eliminar, con validación de email.
- Inventario: productos con categoría, entradas y salidas de bodega con
  historial. El stock nunca queda negativo.
- Proformas: crear con varios productos, consultar, anular y verificar si hay
  stock.
- Facturación: convierte una proforma en factura y descuenta la bodega en una
  sola transacción.
- Despachos: por ruta en camión (con distancia y tiempo calculados por una
  API) o por retiro en bodega, con estados PENDIENTE, EN_CAMINO y ENTREGADO.
- Reportes: consultas HQL en pantalla y tres reportes en PDF con
  JasperReports (factura, ventas por fechas e inventario).
## Tecnologías
- Java 21 con módulos (module-info.java)
- JavaFX 21 y FXML
- Hibernate ORM 7 (JPA)
- PostgreSQL en Neon
- JasperReports 6.21.5
- Jackson para leer JSON
- APIs de OpenStreetMap: Nominatim (dirección a coordenadas) y OSRM (rutas)
- Maven
## Requisitos del proyecto y dónde se cumplen
| Requisito | Dónde |
| --- | --- |
| Ocho entidades | 10 clases en model/ |
| Relaciones JPA | @ManyToOne entre productos, categorías, clientes,
proformas, facturas y despachos |
| PostgreSQL | Neon, configurado en hibernate.cfg.xml |
| Configuración de Hibernate | hibernate.cfg.xml y config/HibernateUtil.java
|
| CRUD reutilizable | dao/GenericDAO.java |
| Dos consultas HQL | dao/ReportesDAO.java (productos más vendidos y ventas
por vendedor) |
| Validaciones y manejo de errores | Validaciones en cada formulario; los DAO
deshacen la transacción y la pantalla muestra el error |
| JasperReports | reportes/ y resources/.../reportes/*.jrxml |
| Consumo de API | service/RutaService.java |
## Arquitectura
Patrón MVC con DAO. Paquetes en ni.edu.uam.anywherecrm:
- model/: entidades JPA.
- dao/: acceso a datos (GenericDAO y DAO específicos con HQL).
- service/: lógica de negocio (FacturaService) y consumo de la API
  (RutaService).
- reportes/: generación de PDF con JasperReports.
- controller/: controladores de las vistas FXML.
- config/: conexión de Hibernate.
- util/: sesión del usuario y datos de prueba.
## Cómo ejecutar
1. Instalar el JDK 21 y abrir la carpeta anywhere-crm en IntelliJ IDEA.
2. Recargar Maven (Maven > Reload All Maven Projects).
3. Configurar la contraseña de la base: en Run > Edit Configurations >
   Launcher > Environment variables, agregar ANYWHERE_DB_PASSWORD con la
   contraseña que entrega el equipo.
4. Ejecutar la clase Launcher.
5. La primera vez se crean usuarios y datos de prueba si las tablas están
   vacías.
## Usuarios de prueba
| Usuario | Contraseña | Rol |
| --- | --- | --- |
| admin | admin123 | ADMIN |
| vendedor | vendedor123 | VENDEDOR |
| bodega | bodega123 | BODEGUERO |
| despacho | despacho123 | DESPACHADOR |
## Flujo de trabajo con Git
- Rama main estable; cada integrante trabaja en su rama.
- Commits con prefijo: feat, fix, build, docs.
- Cada cambio entra a main por Pull Request, que revisa el líder.
## Créditos
Datos de mapas © OpenStreetMap contributors (Nominatim y OSRM).
