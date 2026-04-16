module com.tlcsdm.zeroprobe {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.dlsc.preferencesfx;
    requires org.slf4j;
    requires atlantafx.base;
    requires com.jcraft.jsch;
    requires com.fazecast.jSerialComm;
    requires java.prefs;
    requires com.google.gson;
    requires eu.hansolo.medusa;

    exports com.tlcsdm.zeroprobe.config to com.google.gson;

    opens com.tlcsdm.zeroprobe to javafx.graphics;
    opens com.tlcsdm.zeroprobe.model to com.google.gson;
    opens com.tlcsdm.zeroprobe.controller to javafx.fxml;
}
