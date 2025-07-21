# Desktop Content Explorer

## Overview

The Desktop Content Explorer is a JavaFX-based desktop application for managing and exploring content in the Percussion CMS system.

## Requirements

- **Java 11** or higher
- **JavaFX 21** (automatically included as Maven dependency)
- **Maven 3.6+** for building

## Setup Instructions

### Java 11 Setup

Ensure you have Java 11 or higher installed:

```bash
java -version
```

The output should show version 11 or higher.

### Building the Module

The module uses Maven for dependency management and includes JavaFX 21 dependencies:

```bash
cd modules/DesktopContentExplorer
mvn clean compile
```

### JavaFX Dependencies

The module automatically includes the following JavaFX 21 dependencies:
- `javafx-controls` - Core JavaFX controls
- `javafx-fxml` - FXML support for declarative UI
- `javafx-web` - WebView component

### Running the Application

To run the Desktop Content Explorer:

```bash
mvn exec:java -Dexec.mainClass="com.percussion.cx.PSContentExplorerApplication"
```

Or build the shaded JAR and run:

```bash
mvn package
java -jar target/perc-content-explorer-8.1.6-SNAPSHOT.jar
```

## Testing

The module uses JUnit 5 for testing. Run tests with:

```bash
mvn test
```

## Modular Architecture

The module is designed to work with Java 11's module system. The module descriptor (`module-info.java`) defines the required dependencies and exported packages.

## Migration Notes

This module has been upgraded from Java 8/JavaFX 11 to Java 11/JavaFX 21:
- Updated compiler source and target to Java 11
- Upgraded JavaFX dependencies to version 21
- Added JavaFX FXML support
- Updated tests to use JUnit 5
- Prepared for Java module system integration