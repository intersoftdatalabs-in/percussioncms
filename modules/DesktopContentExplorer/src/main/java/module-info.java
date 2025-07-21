module desktop.content.explorer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.desktop;
    requires java.logging;
    requires java.xml;
    requires java.prefs;
    requires java.management;
    
    // Note: Some dependencies may not be modularized yet
    // requires commons.lang;
    // requires commons.io;
    // requires log4j;
    // requires org.apache.logging.log4j;
    // requires org.apache.logging.log4j.core;
    // requires slf4j.api;
    
    opens com.percussion.cx to javafx.fxml;
    opens com.percussion.cx.javafx to javafx.fxml;
    opens com.percussion.cx.objectstore to javafx.fxml;
    
    exports com.percussion.cx;
    exports com.percussion.cx.javafx;
    exports com.percussion.cx.objectstore;
}