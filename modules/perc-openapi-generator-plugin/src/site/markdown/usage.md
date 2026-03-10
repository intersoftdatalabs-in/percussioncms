# Usage Guide

## Quick Start

To generate an OpenAPI specification in your Maven module, add the following plugin configuration to your `pom.xml`:

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
        <outputFile>${project.build.directory}/openapi.json</outputFile>
        <apiTitle>Percussion CMS REST API</apiTitle>
        <apiVersion>${project.version}</apiVersion>
        <apiDescription>Public REST API for Percussion CMS</apiDescription>
    </configuration>
</plugin>
```

## Building the OpenAPI Specification

The plugin automatically generates the OpenAPI specification during the `process-classes` Maven lifecycle phase:

```bash
mvn clean install
```

The generated specification will be available at the configured output location.

## Annotating Your REST Resources

To ensure your REST API operations are properly documented in the OpenAPI specification, use the following annotations:

### Class-Level Annotations

```java
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Server;
import jakarta.ws.rs.Path;

@OpenAPIDefinition(
    servers = {
        @Server(url = "/rest", description = "REST API Server")
    }
)
@Path("/api/v1")
public class ApiResource {
    // ...
}
```

### Method-Level Annotations

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@GET
@Path("/{id}")
@Produces("application/json")
@Operation(
    summary = "Get a resource by ID",
    description = "Retrieve a specific resource by its unique identifier",
    tags = {"Resources"}
)
@ApiResponse(
    responseCode = "200",
    description = "Resource found",
    content = @Content(schema = @Schema(implementation = ResourceDTO.class))
)
@ApiResponse(responseCode = "404", description = "Resource not found")
public ResponseEntity<ResourceDTO> getResource(
    @PathParam("id")
    @Parameter(description = "The resource ID", example = "12345")
    String id
) {
    // Implementation
}
```

## Output Format

The plugin generates an OpenAPI 3.0 specification in JSON format. The generated file can be served by a web application or processed by documentation tools like Swagger UI.

Example output structure:

```json
{
  "openapi": "3.0.0",
  "info": {
    "title": "Percussion CMS REST API",
    "version": "8.2.0",
    "description": "Public REST API for Percussion CMS"
  },
  "servers": [
    {
      "url": "/rest",
      "description": "REST API Server"
    }
  ],
  "paths": {
    "/api/v1/{id}": {
      "get": {
        "summary": "Get a resource by ID",
        "operationId": "getResource",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "description": "The resource ID",
            "required": true,
            "schema": {
              "type": "string"
            },
            "example": "12345"
          }
        ],
        "responses": {
          "200": {
            "description": "Resource found",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/ResourceDTO"
                }
              }
            }
          },
          "404": {
            "description": "Resource not found"
          }
        }
      }
    }
  },
  "components": {
    "schemas": {}
  }
}
```

## Integrating with perc-openapi-webapp

The generated OpenAPI specification can be served by the `perc-openapi-webapp` module:

1. Copy the generated `openapi.json` file to the webapp's resource directory
2. Deploy the webapp alongside your main application
3. Access the API documentation at `/openapi/`

See the [perc-openapi-webapp documentation](../perc-openapi-webapp) for more details.

## Troubleshooting

### Specification Not Generated

1. **Check plugin invocation**: Run Maven with `-X` flag to see detailed execution logs
2. **Verify classpath**: Ensure REST resource classes are compiled
3. **Check output path**: Ensure the output directory exists and is writable

### Missing REST Endpoints

1. **Verify annotations**: Ensure resources are properly annotated with `@Path` and HTTP method annotations
2. **Check compilation**: Verify that all REST classes are compiled before the plugin runs
3. **Review plugin log output**: The plugin will log discovered resources and operations

### Incomplete API Documentation

1. **Add operation annotations**: Use `@Operation`, `@Parameter`, `@ApiResponse` annotations
2. **Document parameters**: Use `@Parameter` to describe path, query, and header parameters
3. **Document responses**: Use `@ApiResponse` to describe response codes and content

## Advanced Usage

### Custom Output Location

```xml
<configuration>
    <outputFile>/var/www/api-docs/openapi.json</outputFile>
</configuration>
```

### Custom API Metadata

```xml
<configuration>
    <apiTitle>My Custom API</apiTitle>
    <apiVersion>2.0.0</apiVersion>
    <apiDescription>Custom API with detailed description</apiDescription>
</configuration>
```

### Processing External REST Module

```xml
<configuration>
    <restModuleJar>/path/to/rest-module.jar</restModuleJar>
</configuration>
```

This is useful when generating specifications from external or pre-built REST modules.
