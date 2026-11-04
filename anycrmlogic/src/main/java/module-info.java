module ni.edu.uam.anycrmlogic {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;

    opens ni.edu.uam.anycrmlogic to javafx.fxml;
    exports ni.edu.uam.anycrmlogic;
}