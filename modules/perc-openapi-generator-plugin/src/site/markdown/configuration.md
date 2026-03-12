# Configuration Guide

## Plugin Parameters

The Percussion OpenAPI Generator Maven Plugin accepts the following configuration parameters:

### outputFile

- **Type**: String
- **Default**: `${project.build.directory}/../src/main/webapp/openapi.json`
- **Property**: `openapi.output.file`
- **Required**: Yes
- **Description**: The file path where the generated OpenAPI specification will be written in JSON format.

**Example**:

```xml
<configuration>
    <outputFile>${project.build.directory}/openapi.json</outputFile>
</configuration>
```

### apiTitle

- **Type**: String
- **Default**: `Percussion CMS REST API`
- **Property**: `api.title`
- **Required**: Yes
- **Description**: The title of the API as it will appear in the OpenAPI specification and documentation.

**Example**:

```xml
<configuration>
    <apiTitle>My Organization API</apiTitle>
</configuration>
```

### apiVersion

- **Type**: String
- **Default**: `${project.version}`
- **Property**: `api.version`
- **Required**: Yes
- **Description**: The semantic version of the API. Typically matches the project version but can be customized.

**Example**:

```xml
<configuration>
    <apiVersion>1.0.0</apiVersion>
</configuration>
```

### apiDescription

- **Type**: String
- **Default**: `Public REST API for Percussion CMS content management and delivery`
- **Property**: `api.description`
- **Required**: Yes
- **Description**: A detailed description of the API's purpose, capabilities, and use cases.

**Example**:

```xml
<configuration>
    <apiDescription>
        REST API for managing content, users, and system configuration in Percussion CMS.
        Provides endpoints for CRUD operations on content items, asset management, and administrative tasks.
    </apiDescription>
</configuration>
```

### restModuleJar

- **Type**: String
- **Default**: None (scans project classes)
- **Property**: `restModuleJar`
- **Required**: No
- **Description**: Path to an external REST module JAR file to scan for JAX-RS annotations. If not specified, the plugin scans the project's compiled classes. Useful for generating specifications from pre-built or external REST modules.

**Example**:

```xml
<configuration>
    <restModuleJar>${project.build.directory}/rest-module.jar</restModuleJar>
</configuration>
```

## Property-Based Configuration

All configuration parameters can be specified via Maven properties, allowing command-line overrides:

```bash
mvn clean install \
    -Dapi.title="Custom API" \
    -Dapi.version="2.0.0" \
    -Dapi.description="Custom API Description" \
    -Dopenapi.output.file=/tmp/openapi.json
```

## Execution Configuration

### Lifecycle Phase

The plugin is configured to run during the `process-classes` Maven lifecycle phase, which occurs:
- After compilation of source files
- Before packaging of the module
- After all dependencies have been resolved

```xml
<executions>
    <execution>
        <phase>process-classes</phase>
        <goals>
            <goal>generate-spec</goal>
        </goals>
    </execution>
</executions>
```

To change the lifecycle phase:

```xml
<executions>
    <execution>
        <phase>package</phase>
        <goals>
            <goal>generate-spec</goal>
        </goals>
    </execution>
</executions>
```

### Multiple Executions

You can configure the plugin to run multiple times with different configurations:

```xml
<executions>
    <execution>
        <id>generate-public-api</id>
        <phase>process-classes</phase>
        <goals>
            <goal>generate-spec</goal>
        </goals>
        <configuration>
            <outputFile>${project.build.directory}/public-api.json</outputFile>
            <apiTitle>Public API</apiTitle>
        </configuration>
    </execution>
    <execution>
        <id>generate-admin-api</id>
        <phase>process-classes</phase>
        <goals>
            <goal>generate-spec</goal>
        </goals>
        <configuration>
            <outputFile>${project.build.directory}/admin-api.json</outputFile>
            <apiTitle>Admin API</apiTitle>
        </configuration>
    </execution>
</executions>
```

## Common Configuration Patterns

### Development Configuration

For development environments, you might want verbose location information:

```xml
<configuration>
    <outputFile>${project.basedir}/target/openapi-dev.json</outputFile>
    <apiTitle>${project.name} (Development)</apiTitle>
    <apiVersion>${project.version}-SNAPSHOT</apiVersion>
    <apiDescription>
        Development version of ${project.name} REST API.
        For development and testing purposes only.
    </apiDescription>
</configuration>
```

### Production Configuration

For production, ensure consistent paths and clear versioning:

```xml
<configuration>
    <outputFile>/var/app/api-specs/${project.artifactId}-${project.version}.json</outputFile>
    <apiTitle>${project.name}</apiTitle>
    <apiVersion>${project.version}</apiVersion>
    <apiDescription>
        Production REST API specification for ${project.name}.
        See https://docs.example.com/api for detailed documentation.
    </apiDescription>
</configuration>
```

### Multiple Module Setup

When using the plugin across multiple modules:

```xml
<!-- Parent POM -->
<pluginManagement>
    <plugins>
        <plugin>
            <groupId>com.percussion</groupId>
            <artifactId>perc-openapi-generator-plugin</artifactId>
            <version>${perc.openapi.version}</version>
            <configuration>
                <apiTitle>${project.name}</apiTitle>
                <apiVersion>${project.version}</apiVersion>
            </configuration>
        </plugin>
    </plugins>
</pluginManagement>

<!-- Child modules can override as needed -->
<plugin>
    <groupId>com.percussion</groupId>
    <artifactId>perc-openapi-generator-plugin</artifactId>
    <executions>
        <execution>
            <phase>process-classes</phase>
            <goals>
                <goal>generate-spec</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <outputFile>${project.build.directory}/openapi.json</outputFile>
        <apiDescription>Custom description for this module</apiDescription>
    </configuration>
</plugin>
```

## Disabling the Plugin

To temporarily disable the plugin without removing the configuration:

```xml
<plugin>
    <groupId>com.percussion</groupId>
    <artifactId>perc-openapi-generator-plugin</artifactId>
    <skip>true</skip>
</plugin>
```

Or skip via command line:

```bash
mvn clean install -Dmaven.plugin.skip=true
```

## Debugging Configuration

Enable verbose output for troubleshooting:

```bash
mvn clean install -X -Dmaven.compiler.verbose=true
```

This will show:
- Plugin initialization and configuration
- Classpath scanning details
- Discovered REST resources and operations
- Specification generation progress
- Output file location and status
