module org.agty.torchanger.torchanger {
    requires java.desktop;
    requires java.net.http;
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    opens org.agty.torchanger.torchanger to javafx.fxml;
    exports org.agty.torchanger.torchanger;
}
