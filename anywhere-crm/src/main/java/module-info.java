module ni.edu.uam.anywherecrm {
    // Módulos de JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // Módulos del JDK necesarios para Hibernate y PostgreSQL (esto quita tu error)
    requires java.sql;
    requires java.naming;

    // Módulos de Hibernate y JPA
    requires jakarta.persistence;
    requires org.hibernate.orm.core;

    // Permisos para que JavaFX cargue tus pantallas y controladores
    opens ni.edu.uam.anywherecrm to javafx.fxml;
    opens ni.edu.uam.anywherecrm.controller to javafx.fxml;

    // Permisos combinados: JavaFX (para las tablas) e Hibernate (para la BD) acceden al modelo
    opens ni.edu.uam.anywherecrm.model to org.hibernate.orm.core, javafx.base;

    exports ni.edu.uam.anywherecrm;
    
    // JasperReports para generar los PDF
    requires jasperreports;
    requires org.apache.commons.logging;      // Jasper la usa para sus mensajes
    requires org.apache.commons.collections4; // Jasper la usa internamente
    requires org.apache.commons.beanutils;    // Jasper la usa para leer los campos
    requires com.github.librepdf.openpdf;     // Jasper la usa para escribir el PDF

    // API de rutas: lectura de JSON y abrir el mapa en el navegador
    requires com.fasterxml.jackson.databind;
    requires java.desktop;
}
