module com.app.isocketclientfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires jspeedtest;
    requires org.apache.logging.log4j;
    requires jdk.unsupported.desktop;


    opens com.app.isocketclientfx to javafx.fxml;
    exports com.app.isocketclientfx;
}