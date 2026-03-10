# Percussion OpenAPI Generator Maven Plugin

A Maven plugin that generates an OpenAPI 3.0 specification from JAX-RS REST resource annotations in the Percussion CMS REST module. This plugin is part of the effort to decouple OpenAPI/Swagger runtime from the main Percussion CMS application servers, enabling modular architecture and supporting Jackson 3 migration.

## Purpose

The OpenAPI Generator plugin enables **build-time** generation of API documentation from JAX-RS and Swagger annotations, decoupling OpenAPI specification generation and serving from the main application runtime. This approach:

- **Eliminates runtime overhead** by generating specifications at build time rather than during application startup
- **Reduces dependencies** on the main application servers by moving Swagger/OpenAPI runtime to a standalone module
- **Supports modular architecture** where REST API documentation is generated and served independently
- **Enables Jackson 3 migration** by removing runtime Swagger library dependencies that conflict with Jackson 3 compatibility

## Usage

### Basic Configuration

Add the plugin to your `pom.xml`:

```xml
<plugin>
    <groupId>com.percussion</groupId>
    <artifactId>perc-openapi-generator-plugin</artifactId>
    <version>${project.version}</version>
    <executions>
        <execution>
            <phase>process-classes</phase>
            <goals>
                <goal>generate-spec</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <outputFile>${project.build.directory}/../src/main/webapp/openapi.json</outputFile>
        <apiTitle>My API</apiTitle>
        <apiVersion>1.0.0</apiVersion>
        <apiDescription>My API Description</apiDescription>
    </configuration>
</plugin>
```

### Configuration Parameters

- **outputFile** (default: `${project.build.directory}/../src/main/webapp/openapi.json`)
  - The file path where the generated OpenAPI specification will be written in JSON format.

- **apiTitle** (default: `Percussion CMS REST API`)
  - The title of the API as it will appear in the OpenAPI specification.

- **apiVersion** (default: `${project.version}`)
  - The semantic version of the API.

- **apiDescription** (default: `Public REST API for Percussion CMS content management and delivery`)
  - A detailed description of the API's purpose and capabilities.

- **restModuleJar** (optional)
  - Path to the REST module JAR file to scan for JAX-RS annotations. If not specified, the plugin will scan the project's classes.

## How It Works

1. **Classpath Scanning**: The plugin uses bytecode reflection to scan the compiled REST module classes for JAX-RS annotations.

2. **Annotation Detection**: It identifies:
   - `@Path` annotations on resource classes
   - HTTP method annotations: `@GET`, `@POST`, `@PUT`, `@DELETE`, `@HEAD`, `@OPTIONS`
   - Parameter annotations: `@PathParam`, `@QueryParam`, `@Consumes`, `@Produces`
   - JAX-RS media types and content negotiation patterns

3. **Swagger Annotation Processing**: The plugin reads Swagger/OpenAPI annotations:
   - `@OpenAPIDefinition` - Overall API definition
   - `@Operation` - Operation-level documentation
   - `@Parameter` - Parameter documentation
   - `@Schema` - Data model documentation

4. **OpenAPI Specification Generation**: The collected metadata is synthesized into a valid OpenAPI 3.0 specification in JSON format.

5. **Output**: The specification is written to the configured output file, which can be served by a separate web module (e.g., perc-openapi-webapp) or integrated into the build artifacts.

## Supported Annotations

### JAX-RS Core Annotations
- `@Path` - Resource class and method paths
- `@GET`, `@POST`, `@PUT`, `@DELETE`, `@HEAD`, `@OPTIONS` - HTTP methods
- `@Consumes`, `@Produces` - Media type negotiation
- `@PathParam`, `@QueryParam` - Request parameters
- `@DefaultValue` - Default parameter values

### Swagger/OpenAPI Annotations
- `@OpenAPIDefinition` - API-level metadata
- `@Server` - Server information
- `@Operation` - Operation documentation
- `@Parameter` - Parameter documentation
- `@Schema` - Schema/model documentation
- `@Content`, `@MediaType` - Response content type definitions

## Integration with perc-openapi-webapp

The generated OpenAPI specification is intended to be used with the `perc-openapi-webapp` module, which serves:
- The OpenAPI specification at `/openapi/openapi.json`
- Swagger UI for interactive API documentation at `/openapi/`

This separation ensures that the main Percussion CMS application servers do **not** dependency on Swagger/OpenAPI libraries at runtime.

## Build-Time Execution

The plugin runs during the `process-classes` Maven lifecycle phase, which occurs after compilation but before packaging. This ensures:
- All JAX-RS classes are compiled and available for scanning
- The specification is generated before the module is packaged
- The specification can be included in the final artifact

## Dependencies

The plugin depends on:
- Apache Maven Plugin API (3.9.6+)
- Apache CXF JAX-RS frontend and OpenAPI service description
- Swagger Core v3 annotations and JSON processing
- Reflections library for bytecode scanning
- Jackson for JSON serialization

## Troubleshooting

### OpenAPI Specification Not Generated

1. **Check plugin configuration**: Ensure the plugin is properly configured in the `pom.xml`.
2. **Verify classpath**: Ensure REST resource classes are compiled and available on the classpath.
3. **Check output directory**: Ensure the output directory is writable and created if necessary.
4. **Enable debug logging**: Run Maven with `-X` flag to see detailed plugin execution logs.

### Missing Operations or Parameters

1. **Verify annotations**: Ensure REST resources are properly annotated with JAX-RS and Swagger annotations.
2. **Check REST module JAR**: If using `restModuleJar`, ensure the JAR contains the compiled REST classes.
3. **Validate annotation syntax**: Ensure all annotations are correctly applied to methods and parameters.

### Specification Not Served

1. **Verify perc-openapi-webapp module**: Ensure the standalone webapp module is deployed and includes the generated specification.
2. **Check endpoint paths**: Confirm that the OpenAPI endpoint is at `/openapi/openapi.json`.
3. **Review authentication**: Ensure API documentation endpoints are accessible (not blocked by authentication filters).

## See Also

- [perc-openapi-webapp](../perc-openapi-webapp) - Standalone web module for serving OpenAPI specifications
- [Swagger Annotations](https://github.com/swagger-api/swagger-core/wiki) - Swagger/OpenAPI annotation documentation
- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.3) - OpenAPI 3.0 standard
- [JAX-RS Specification](https://projects.eclipse.org/projects/ee4j.rest) - Jakarta RESTful Web Services specification

## Contributing

When modifying this plugin:
1. Ensure changes maintain backward compatibility with existing REST modules
2. Add unit tests for new functionality
3. Update this README with any new features or configuration options
4. Follow the Percussion CMS Java coding standards as documented in `.github/instructions/java-coding-standards.md`
