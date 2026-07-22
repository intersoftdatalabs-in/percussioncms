# perc-distribution-tree Module Agent Guidelines

## Overview

The `perc-distribution-tree` module is responsible for assembling the complete Percussion CMS distribution package. This includes packaging Jetty, web applications, configuration files, and automatic configuration of the Jetty logging module.

## Before You Begin

**ALWAYS READ** the following documentation in this order:

1. [README.md](README.md) - Start here for a complete overview of the module's purpose, architecture, and building instructions
2. [src/main/resources/installDistributionFiles.xml](src/main/resources/installDistributionFiles.xml) - The ANT build script that orchestrates the distribution assembly
3. [pom.xml](pom.xml) - Maven configuration showing how the ANT script is invoked

## Key Responsibilities

This module:

- **Assembles distributions** by packaging Jetty, WAR files, and configuration
- **Configures Jetty modules**, particularly the `logging-log4j2` module for centralized logging
- **Deploys web applications** (Rhythmyx, REST, SiteManage)
- **Manages configuration files** and database setup scripts
- **Optimizes startup** by enabling logging integration during the build process

## Important Architecture Notes

### Logging Integration

The module automatically enables Jetty's `logging-log4j2` module during distribution assembly. This ensures:

- All System.out/System.err streams are captured by Log4j2
- Logs are centralized to `server.log`
- All Jetty and application logs are unified

The module enablement command is:

```bash
java -jar ../start.jar --add-modules=logging-log4j2
```

This is executed in `installDistributionFiles.xml` after MySQL connector setup and before file permission fixup. See README.md for details.

### Build Process Flow

1. Read [pom.xml](pom.xml) to understand Maven plugin configuration
2. The `maven-antrun-plugin` executes [src/main/resources/installDistributionFiles.xml](src/main/resources/installDistributionFiles.xml)
3. The ANT script unpacks Jetty, deploys applications, and enables modules
4. The `maven-assembly-plugin` packages the assembled distribution

## Related Modules

- `modules/perc-jetty` - Jetty distribution packaging and default module configurations
- `modules/perc-jetty-logging` - Log4j2 JAR artifacts for Jetty
- `modules/perc-distribution-tree/WebUI` - Main web application
- `rest` - REST API web application
- `system` - Core system module

## Common Tasks

### Building This Module

```bash
cd modules/perc-distribution-tree
../../mvn-env.sh clean install
```

### Verifying the JDBC driver set

After `mvn verify`, the `scripts/verify-jdbc-drivers.py` script (cross-platform Python port of the original `.sh`/`.bat`) runs against the built distribution artifact and asserts that `jetty/base/lib/jdbc/` is populated with the expected JDBC drivers (sourced from parent-POM-managed Maven coordinates; see `pom.xml` execution `stage-jdbc-drivers`). See `scripts/README.md` for invocation details and exit-code table.

The Maven `verify` phase invokes the canonical Java main `com.percussion.distribution.install.VerifyJdbcDrivers` directly via `exec-maven-plugin:java`, so the build gate does not depend on a Python interpreter being on PATH. The `.bat` shim that previously wrapped that Java main has been removed (spec 994 / FR-004).

### Modifying Distribution Assembly

Changes to `installDistributionFiles.xml` affect how all distributions are built. Always:

1. Understand the entire assembly flow in the ANT script
2. Test with `./mvn-env.sh clean install` in this module directory
3. Verify the generated distribution in `target/` has expected structure

### Enabling Additional Jetty Modules

To enable additional Jetty modules during distribution build:

1. Add an `<exec>` task in `installDistributionFiles.xml` similar to the logging module enablement
2. Execute in the `${assembly-directory}/jetty/base` directory
3. Use `java -jar ../start.jar --add-modules=MODULE_NAME`
4. Set `failonerror="false"` to avoid breaking builds if modules already enabled

## When to Update Documentation

Update [README.md](README.md) when:

- You modify the build process or ANT script logic
- You add new features or modules
- You change how distributions are configured or optimized
- You discover undocumented behaviors or best practices

This ensures future agents have accurate, up-to-date information.
