# Architecture Overview

## Design Principles

The Percussion OpenAPI Generator Maven Plugin is designed around the following principles:

### 1. Build-Time Generation

The plugin generates OpenAPI specifications at build time, not at runtime. This approach provides:
- **Performance**: No runtime overhead during application startup or request processing
- **Decoupling**: REST APIs don't require Swagger/OpenAPI runtime libraries
- **Modularity**: API specifications can be served independently from the application logic

### 2. Annotation-Driven Documentation

The plugin relies entirely on standard JAX-RS and Swagger annotations:
- **JAX-RS Annotations**: `@Path`, `@GET`, `@POST`, `@PathParam`, `@Produces`, etc.
- **Swagger Annotations**: `@Operation`, `@Parameter`, `@ApiResponse`, `@Schema`, etc.
- **Build-Time Processing**: Annotations are read from compiled bytecode during the Maven build process

### 3. Standalone Serving

The generated OpenAPI specification is designed to be served by a separate web module (perc-openapi-webapp):
- **Separation of Concerns**: REST API implementation separate from documentation serving
- **Independent Deployment**: API documentation can be deployed and updated independently
- **Resource Efficiency**: Main application servers don't load Swagger/OpenAPI libraries

## Component Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    REST Module                               │
│                                                              │
│  ┌─────────────┐    ┌─────────────┐    ┌──────────────┐   │
│  │ Resource A  │    │ Resource B  │    │  Resource C  │   │
│  │  @Path      │    │  @Path      │    │    @Path     │   │
│  │ @GET/@POST  │    │ @GET/@POST  │    │  @GET/@POST  │   │
│  │ @Operation  │    │ @Operation  │    │ @Operation   │   │
│  └─────────────┘    └─────────────┘    └──────────────┘   │
│         ▲                 ▲                    ▲             │
└─────────┼─────────────────┼────────────────────┼─────────────┘
          │                 │                    │
          │ JAX-RS & Swagger Annotations         │
          │                                      │
          └──────────────────────────────────────┘
                         │
                         │ Maven Build (compile)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         OpenAPI Generator Maven Plugin                       │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Bytecode Scanner (Reflections)                        │  │
│  │  └─ Find all classes with @Path annotations         │  │
│  │  └─ Discover HTTP method annotations                │  │
│  │  └─ Extract parameter and response metadata         │  │
│  └───────────────────────────────────────────────────────┘  │
│           ▲                                                  │
│           │ Compiled .class files                            │
│           │                                                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ OpenAPI Generator (Swagger Core v3)                   │  │
│  │  └─ Process discovered annotations                   │  │
│  │  └─ Build OpenAPI operation objects                  │  │
│  │  └─ Resolve parameter and response schemas           │  │
│  │  └─ Generate complete OpenAPI specification          │  │
│  └───────────────────────────────────────────────────────┘  │
│           ▼                                                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ JSON Serializer (Jackson)                             │  │
│  │  └─ Serialize OpenAPI model to JSON format           │  │
│  │  └─ Write to configured output file                  │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                         │
                         │ Generated openapi.json
                         ▼
┌─────────────────────────────────────────────────────────────┐
│               Swagger UI (perc-openapi-webapp)               │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ /openapi/openapi.json      [API Specification]     │   │
│  └─────────────────────────────────────────────────────┘   │
│                         ▲                                    │
│                         │                                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ /openapi/               [Swagger UI Documentation]  │   │
│  │  └─ Interactive API browser                        │   │
│  │  └─ Operation testing interface                    │   │
│  │  └─ Schema documentation                           │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                      (User Facing)
```

## Execution Flow

### Maven Build Lifecycle Integration

```
1. Maven Compile Phase
   └─ Compile source code
   └─ Generate compiled .class files
   └─ Store in target/classes/

2. Maven Process-Classes Phase (Plugin Execution)
   ├─ Initialize OpenAPIGeneratorMojo
   ├─ Load plugin configuration (outputFile, apiTitle, etc.)
   ├─ Set up classpath with compiled classes
   │
   ├─ Bytecode Scanning
   │  ├─ Use Reflections to scan classpath
   │  ├─ Find classes with @Path annotation
   │  ├─ Discover HTTP method annotations
   │  ├─ Extract parameter information
   │  └─ Gather response metadata
   │
   ├─ OpenAPI Generation
   │  ├─ Create OpenAPI object
   │  ├─ Populate info (title, version, description)
   │  ├─ Add servers from @Server annotations
   │  ├─ Process each discovered resource
   │  │  ├─ Create path item for each @Path
   │  │  ├─ For each HTTP method:
   │  │  │  ├─ Create operation
   │  │  │  ├─ Add parameters from @PathParam, @QueryParam
   │  │  │  ├─ Add request body from @Consumes
   │  │  │  ├─ Add responses from @ApiResponse
   │  │  │  └─ Add summary/description from @Operation
   │  │  └─ Add path item to paths
   │  └─ Build component schemas
   │
   ├─ Output Generation
   │  ├─ Serialize OpenAPI to JSON using Jackson
   │  ├─ Create output directory if needed
   │  └─ Write openapi.json file
   │
   └─ Log results and statistics

3. Maven Package Phase
   └─ Package module (JAR/WAR/etc.)
   └─ Include generated openapi.json in output
```

## Key Components

### OpenAPIGeneratorMojo

**Class**: `com.percussion.maven.OpenAPIGeneratorMojo`

Extends `AbstractMojo` to integrate with Maven build lifecycle.

**Responsibilities**:
- Read and validate plugin configuration
- Initialize classpath for bytecode scanning
- Orchestrate specification generation workflow
- Write output to configured file location
- Handle errors and provide logging

**Key Methods**:
- `execute()` - Main entry point called by Maven
- `scanRestResources()` - Find REST resource classes
- `generateOpenApISpec()` - Build OpenAPI specification
- `writeSpecification()` - Serialize and save OpenAPI JSON

### Bytecode Scanner

Uses the [Reflections](https://github.com/ronmamo/reflections) library to:
- Scan compiled `.class` files on the classpath
- Find classes annotated with `@Path`
- Discover HTTP method annotations (`@GET`, `@POST`, etc.)
- Extract parameter annotations (`@PathParam`, `@QueryParam`, etc.)
- Collect response and schema information

**Features**:
- Fast classpath scanning without loading classes into memory
- Support for annotation inheritance
- Filtering capabilities to focus scanning

### OpenAPI Builder

Constructs an `OpenAPI` object (from Swagger Core v3) containing:
- **Info**: API title, version, description
- **Servers**: Server URLs and descriptions
- **Paths**: REST endpoints and their operations
- **Components**: Reusable schemas and response objects
- **Tags**: Operation categorization

### JSON Serializer

Uses Jackson to serialize the OpenAPI object to JSON format:
- Pretty-prints for readability
- Handles complex nested structures
- Validates JSON output
- Supports custom serialization rules

## Dependency Management

```
OpenAPI Generator Plugin
├─ Maven Plugin API (org.apache.maven)
│  └─ Interface with Maven build lifecycle
├─ Maven Plugin Annotations (org.apache.maven.plugin-tools)
│  └─ Mojo annotations and metadata
├─ Reflections (org.reflections)
│  └─ Bytecode scanning and classpath analysis
├─ Swagger Core v3 (io.swagger.core.v3)
│  └─ OpenAPI model and annotation processing
├─ Apache CXF (org.apache.cxf)
│  └─ JAX-RS resource scanning
├─ Jackson (com.fasterxml.jackson)
│  └─ JSON serialization
└─ SLF4J (org.slf4j)
   └─ Logging
```

**Important**: These are build-time dependencies only. They do NOT appear as runtime dependencies in modules that use annotations.

## Extension Points

### Custom Annotation Processing

To add support for custom annotations, extend `OpenAPIGeneratorMojo`:

```java
public class CustomOpenAPIGeneratorMojo extends OpenAPIGeneratorMojo {
    @Override
    protected void processCustomAnnotations(Class<?> clazz, Operation operation) {
        // Process custom annotations
    }
}
```

### Schema Generation Customization

Override schema resolution to support custom types:

```java
protected Schema<?> resolveSchema(Class<?> type) {
    if (isCustomType(type)) {
        return buildCustomSchema(type);
    }
    return super.resolveSchema(type);
}
```

## Performance Characteristics

- **Scan Time**: Typically 1-5 seconds depending on classpath size
- **Generation Time**: Typically sub-second for 50-100 REST endpoints
- **Output Size**: 50-200 KB JSON for typical REST APIs
- **Memory Usage**: ~50-100 MB during build

## Integration with CI/CD

The plugin is designed for CI/CD pipelines:
- **Deterministic**: Same input always produces same output
- **Cached**: Can be skipped if REST code hasn't changed
- **Offline**: No external API calls required
- **Artifact**: Generated spec can be version-controlled or published

## Related Components

- **perc-openapi-webapp**: Standalone web module for serving generated specifications
- **rest module**: Contains JAX-RS resource implementations
- **Main application servers**: Use only annotation-driven approach, no runtime Swagger

See the project README for links to related documentation.
