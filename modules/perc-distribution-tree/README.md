# perc-distribution-tree

This module contains all Percussion CMS distribution assembly, installation, and upgrade related classes, configurations, and scripts. It is responsible for packaging Jetty, web applications, and configuration files into a complete, deployable Percussion CMS distribution.

## Overview

The `perc-distribution-tree` module is a critical component in the build pipeline that:

- **Assembles the Percussion Distribution**: Packages Jetty application server, WAR files, and configuration files
- **Configures Jetty**: Unpacks Jetty, configures modules, and sets up the base directory
- **Deploys Web Applications**: Stages Rhythmyx (main application), REST API, and SiteManage web applications
- **Manages Configuration**: Copies and initializes Percussion configuration files and database setup scripts
- **Enables Logging Integration**: Automatically configures Jetty's `logging-log4j2` module to ensure all System streams (stdout/stderr) are captured and routed through Apache Log4j2

## Architecture

### Build Process

The module uses Maven's `maven-antrun-plugin` to execute an ANT build script (`src/main/resources/installDistributionFiles.xml`) that orchestrates the distribution assembly. This ANT script:

1. **Unpacks Jetty**: Extracts the Jetty application server from the Maven artifact
2. **Sets Up Directory Structure**: Creates necessary directories (`webapps/`, `lib/`, `logs/`, etc.)
3. **Deploys Web Applications**: Copies and unpacks WAR files for each application
4. **Configures Databases**: Sets up development/production database connectors
5. **Enables Jetty Modules**: Configures the `logging-log4j2` module for log aggregation
6. **Fixes File Permissions**: Adjusts line endings and permissions for shell scripts

### Jetty Logging Integration

**Key Feature**: The distribution automatically enables Jetty's `logging-log4j2` module during assembly. This ensures:

- All console output (System.out/System.err) is captured by Log4j2
- Logging is centralized to a single `server.log` file
- Startup logs, application logs, and system output are unified
- Log rotation, formatting, and filtering follow the centralized Log4j2 configuration

The module enablement is performed by executing:

```bash
java -jar ../start.jar --add-modules=logging-log4j2
```

This happens automatically during the distribution build process. Refer to `src/main/resources/installDistributionFiles.xml` (lines ~705-710) for implementation details.

## Building

```bash
Use ./mvn-env.sh clean install (or mvn-env.bat clean install on Windows) so Maven runs with JDK 21.
```

To build only this module:

```bash
cd modules/perc-distribution-tree
../../mvn-env.sh clean install
```

## Key Files

- **`src/main/resources/installDistributionFiles.xml`**: ANT build script that assembles the distribution
- **`src/main/assembly/perc-assembly.xml`**: Maven Assembly plugin descriptor for distribution packaging
- **`pom.xml`**: Maven configuration with `maven-antrun-plugin` and `maven-assembly-plugin`

## Related Documentation

For information about logging configuration and how the centralized logging works, refer to:

- `modules/perc-jetty/src/main/jetty/defaults/modules/perc-logging/resources/log4j2.xml` - Log4j2 configuration
- `modules/perc-jetty-logging/README.md` - Jetty logging module artifacts
- Main project documentation on logging and Jetty configuration

