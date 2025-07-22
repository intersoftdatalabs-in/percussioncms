module desktop.content.explorer {
    requires javafx.controls;
    requires javafx.fxml;

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