module desktop.content.explorer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    // Log4j2 dependencies
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;

    // Other required modules
    requires java.desktop;
    requires java.xml;
    requires commons.lang;
    requires commons.io;
    requires org.slf4j;
    
    opens com.percussion.cx to javafx.fxml;
    opens com.percussion.cx.javafx to javafx.fxml;
    opens com.percussion.cx.objectstore to javafx.fxml;
    
    exports com.percussion.cx;
    exports com.percussion.cx.javafx;
    exports com.percussion.cx.objectstore;
}