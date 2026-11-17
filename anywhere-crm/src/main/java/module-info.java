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

    // Permisos para que Hibernate cree las tablas desde tus entidades
    opens ni.edu.uam.anywherecrm.model to org.hibernate.orm.core, javafx.base;

    exports ni.edu.uam.anywherecrm;
}