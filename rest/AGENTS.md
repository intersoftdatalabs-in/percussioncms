# rest AI Agent Notes

## Required Reading

- Read [rest/README.md](README.md) before making changes to REST resources or adaptors
- Review the [projects/sitemanage/README.md](../projects/sitemanage/README.md) for site management integration
- Check [modules/perc-openapi-webapp/README.md](../modules/perc-openapi-webapp/README.md) for OpenAPI documentation requirements

## Technology Stack

### Dependency Versions

Source of truth for dependency versions is the root `pom.xml`:

- `jakarta.ws.rs.version` - JAX-RS API version
- `cxf.version` - Apache CXF version
- `jackson.version` - Jackson core libraries
- `jakarta.jackson.version` - Jackson Jakarta bindings
- `swagger-core.version` - Swagger/OpenAPI annotations

**Important**: Use `jakarta.ws.rs.*` (not `javax.ws.rs.*`) as this project targets Jakarta EE 10.

## Design Patterns

### Adaptor Pattern

The REST module uses the **Adaptor pattern** to separate concerns:

```text
HTTP Request
    ↓
JAX-RS Resource (@Path, @GET, @POST, etc.)
    ↓
Adaptor Interface (IXxxAdaptor)
    ↓
Adaptor Implementation
    ↓
CMS System Services (perc-system)
    ↓
Database & Internal Services
```

See README.md for detailed explanation of the Adaptor pattern components and responsibilities.

## Key Integration Points

### sitemanage Module Integration

The REST module integrates with `projects/sitemanage` for site operations. See README.md for details on:

- How to use `PSSiteManageBean` for site operations
- Site DTO imports from `com.percussion.sitemanage.*`
- Exception handling for site management

### openapi-webapp Module Integration

The REST module works with `modules/perc-openapi-webapp` for API documentation. Key points:

- All Open API annotations are **source-level documentation only**
- Annotations are parsed at build time by `perc-openapi-webapp`
- No runtime Swagger dependencies in the REST JAR
- See README.md for Swagger annotation guidelines

### perc-system Module Dependency

The REST module depends on core system services from `perc-system`. See README.md for details on:

- Available domain objects and services
- How to use system services through dependency injection
- Exception handling patterns

## Resource Development Guide

1. **Define the Resource Class**:

```java
@Path("/resources")
@OpenAPIDefinition(servers = {@Server(url = "/rest")})
@Tag(name = "resources", description = "Resource operations")
public class ResourcesResource extends AdaptorBase {

    @Inject
    private IResourceAdaptor adaptor;

    @GET
    @Operation(summary = "Get all resources")
    public Response getResources() {
        return adaptor.getResources();
    }
}
```

1. **Define the Adaptor Interface**:

```java
public interface IResourceAdaptor extends IAdaptor {
    Response getResources();
}
```

1. **Implement the Adaptor**:

```java
@Component
public class ResourceAdaptor implements IResourceAdaptor {

    private final ResourceService resourceService;

    public ResourceAdaptor(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public Response getResources() {
        try {
            List<Resource> resources = resourceService.getAll();
            return Response.ok(resources).build();
        } catch (Exception e) {
            return handleError(e);
        }
    }
}
```

### Best Practices for REST Resources

- **Resource Class**: Keep focused on HTTP concerns (routing, binding, serialization)
- **Adaptor Interface**: Define clear contracts for business operations
- **Adaptor Implementation**: Handle CMS business logic and error cases
- **OpenAPI Annotations**: Always include `@Operation`, `@ApiResponse`, `@Parameter` annotations
- **HTTP Methods**: Use `@GET`, `@POST`, `@PUT`, `@PATCH`, `@DELETE` appropriately
- **Path Parameters**: Use `/resources/{id}` for resource identity
- **Query Parameters**: Use `?type=X&status=Y` for filtering
- **Request Body**: Use `@Consumes("application/json")` for POST/PUT
- **Response Control**: Always return `Response` object for status code control
- **Response Objects**: Use `@XmlRootElement` and `@XmlElement` for serialization

### Error Handling

REST resources should return appropriate HTTP status codes. See README.md for the complete status code reference and error handling patterns.

Use the standard error response structure from `com.percussion.rest.errors.*` package for consistency.

## Testing

### Quick Test Structure

```java
@ExtendWith(MockitoExtension.class)
class ResourceTests {

    @Mock
    private IResourceAdaptor adaptor;

    @InjectMocks
    private ResourcesResource resource;

    @Test
    void testGetResources() {
        // Arrange
        List<Resource> expected = // ...
        when(adaptor.getResources()).thenReturn(Response.ok(expected).build());

        // Act
        Response response = resource.getResources();

        // Assert
        assertEquals(200, response.getStatus());
    }
}
```

For detailed testing guidance (local transport testing, mocking strategies, best practices), see README.md.

## Building & Code Quality

For building, code style, and Spotless formatting details, see README.md.

Build the module with:

```bash
./mvn-env.sh -pl rest clean install
```

## Hot Deployment

For rapid development iteration:

```bash
./scripts/hot-deploy-local.sh --install-dir /path/to/install --modules rest,system --restart
```

Details on using this script are in README.md.

## Common Issues & Solutions

### REST endpoint not registered

**Check**:

1. Resource class is in `com.percussion.rest.*` package (component-scan base package)
2. Resource is annotated with `@Path`
3. Methods have JAX-RS method annotations (`@GET`, `@POST`, etc.)
4. Spring context loads correctly: check `applicationContext.xml`

### OpenAPI annotations not appearing in spec

**Check**:

1. Annotations are present in resource class
2. `perc-openapi-webapp` module was rebuilt
3. Check `/openapi/openapi.json` for generated spec
4. Verify `swagger-annotations` dependency is available in compile scope

### Request/Response deserialization fails

**Check**:

1. DTOs have proper Jackson annotations (`@JsonProperty`, `@JsonIgnore`)
2. DTOs have JAXB annotations if supporting XML (`@XmlRootElement`)
3. Jackson dependencies include required modules (datatype-jsr310, parameter-names, etc.)
4. Complex types have no-arg constructor

### Authorization fails for endpoint

**Check**:

1. Endpoint is not accidentally annotated with permissions that block it
2. REST servlet is configured with proper security context
3. Check `perc-security-utils` for available security annotations
4. Verify user has required roles/ACLs for operation

## Backwards Compatibility

**Important**: REST endpoints are part of the public API. When modifying existing endpoints:

1. **Never remove** parameters or change their semantics without deprecation period
2. **Never change** response structure in a breaking way
3. **Add new fields** to response objects as optional (nullable)
4. **Use `@Deprecated` annotation** on old endpoints and document migration path
5. **Version endpoints** in path (e.g., `/v1/resources`) if major changes needed
6. **Document changes** in API documentation and migration guide

## Documentation & Knowledge Sharing

When discovering new patterns, issues, or best practices:

1. Update this AGENTS.md file with agent-specific guidance
2. Update rest/README.md with architectural changes or new information
3. Add code comments explaining non-obvious patterns
4. Create test cases demonstrating proper usage
5. Document integration points when adding dependencies
