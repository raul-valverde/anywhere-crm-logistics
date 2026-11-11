module ni.edu.uam.anywherecrm {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;

    opens ni.edu.uam.anywherecrm to javafx.fxml;
    exports ni.edu.uam.anywherecrm;
}