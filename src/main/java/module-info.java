module org.agty.torchanger.torchanger {
    requires java.desktop;
    requires java.net.http;
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    opens org.agty.torchanger.torchanger.ui to javafx.fxml;
    exports org.agty.torchanger.torchanger.app;
    exports org.agty.torchanger.torchanger.bridge;
    exports org.agty.torchanger.torchanger.config;
    exports org.agty.torchanger.torchanger.proxy;
    exports org.agty.torchanger.torchanger.settings;
    exports org.agty.torchanger.torchanger.tor;
    exports org.agty.torchanger.torchanger.tor.profile;
    exports org.agty.torchanger.torchanger.ui;
}
