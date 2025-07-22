module desktop.content.explorer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    // Note: Many dependencies are not yet modularized, requiring automatic modules
    requires java.desktop;
    requires java.xml;
    requires java.logging;
    requires java.base;
    
    // Automatic modules (non-modular JARs)
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires commons.lang;
    requires commons.io;
    
    opens com.percussion.cx to javafx.fxml;
    opens com.percussion.cx.javafx to javafx.fxml;
    opens com.percussion.cx.objectstore to javafx.fxml;
    
    exports com.percussion.cx;
    exports com.percussion.cx.javafx;
    exports com.percussion.cx.objectstore;
}