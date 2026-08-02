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

### Workbench replacement APIs (HARD RULES)

For **Developer module / Workbench replacement** surfaces, do **not** lazily reuse partial
sitemanage “already REST-like” product endpoints. Build a **clean** public REST contract here
and use **classic SOAP / design webservices as the behavioral reference**.

Full rules + dev/QA modes:
[`docs/developer-module/workbench-rest-and-qa-modes.md`](../docs/developer-module/workbench-rest-and-qa-modes.md).

```text
SOAP / IPS*DesignWs (reference) → REST resource + DTO + IXxxAdaptor (this module)
  → sitemanage apibridge (thin) → same design/system backends Workbench used
```

### Adaptor Pattern (with sitemanage apibridge)

The REST module uses the **Adaptor pattern**. HTTP concerns live here; **implementations live in
`projects/sitemanage`**, not in this module.

```text
HTTP Request
    ↓
JAX-RS Resource  (rest)              e.g. PreferenceResource, RelationshipSummaryResource
    ↓
Adaptor Interface (rest)             e.g. IPreferenceAdaptor, IRelationshipSummaryAdaptor
    ↓
Adaptor Implementation (sitemanage)  com.percussion.apibridge.*  e.g. PreferencesAdaptor
    ↓
Design WS / system (prefer IPS*DesignWs when replacing Workbench)
  or documented domain services when no design-WS twin exists
    ↓
Database & Internal Services
```

|         Layer          |            Module            |                           Package example                           |                         Owns                         |
|------------------------|------------------------------|---------------------------------------------------------------------|------------------------------------------------------|
| JAX-RS resource        | **rest**                     | `com.percussion.rest.*`                                             | Paths, HTTP verbs, status codes, OpenAPI annotations |
| Wire DTOs              | **rest**                     | `com.percussion.rest.*` or `com.percussion.share.relationship.data` | JSON/XML shapes returned by the public API           |
| Adaptor interface      | **rest**                     | `com.percussion.rest.*.IXxxAdaptor`                                 | Contract the resource injects                        |
| Adaptor implementation | **sitemanage**               | `com.percussion.apibridge.*`                                        | CMS/domain calls, Optional→HTTP mapping              |
| Domain services        | **sitemanage** / perc-system | e.g. `IPSRelationshipSummaryService`                                | Business logic, AuthZ                                |

**Rules for agents adding a new REST surface:**

1. Put the **resource**, **adaptor interface**, and **wire DTOs** in `rest`.
2. Put the **adaptor implementation** in `projects/sitemanage/.../apibridge/` with `@PSSiteManageBean`
   (same pattern as `PreferencesAdaptor`, `UserAdaptor`, `RelationshipSummaryAdaptor`).
3. Unit-test the resource in `rest` (mock the interface). Unit-test the adaptor in `sitemanage`
   (mock domain services).
4. **Also** add a Spring test stub for the adaptor interface (see **MainTest Spring stubs** below).
   Mockito unit tests alone are **not** enough.
5. Register beans via `@PSSiteManageBean` / component-scan — do not invent a rest→sitemanage Maven edge.

See also `projects/sitemanage/AGENTS.md` (apibridge side) and README.md.

## Maven dependency direction (HARD RULE — no reactor cycles)

```text
rest  ──depends on──▶  perc-system, perc-security-utils, utils, ...
  ▲
  │  (allowed)
  │
sitemanage  ──depends on──▶  rest
```

|       Direction       |      Allowed?      |                                  Why                                   |
|-----------------------|--------------------|------------------------------------------------------------------------|
| **sitemanage → rest** | **Yes** (required) | apibridge implements `IXxxAdaptor`; services may return rest wire DTOs |
| **rest → sitemanage** | **Never**          | Introduces `rest ↔ sitemanage` reactor cycle (`ProjectCycleException`) |

**Do not** add:

```xml
<!-- FORBIDDEN in rest/pom.xml -->
<dependency>
  <groupId>com.percussion</groupId>
  <artifactId>sitemanage</artifactId>
</dependency>
```

If a rest class seems to need a type from sitemanage:

- **Wire DTO / API shape** → define it in **rest** (resource package or a shared wire package under rest).
- **Service / domain interface used only by the adaptor** → keep it in **sitemanage**; call it from
  `com.percussion.apibridge.*`, not from the JAX-RS resource.
- **Need a shared type used by both** → put the shared API surface in **rest** (or a lower module
  both already depend on, e.g. perc-system). Never reverse the Maven arrow.

Historical foot-gun (US8 / relationship summary): DTOs were added under sitemanage and rest was given
a sitemanage dependency for the adaptor. That cycle was fixed by moving wire DTOs into rest and the
adaptor impl into `sitemanage` apibridge. Do not reintroduce that edge.

## Key Integration Points

### sitemanage Module Integration

Runtime integration is **in-process Spring wiring**, not a Maven dependency from rest to sitemanage:

- Resources use `@PSSiteManageBean` (from perc-system / site-manage bean utilities) so CXF/Spring
  discovers them in the Rhythmyx webapp that also loads sitemanage.
- Resources `@Autowired` / constructor-inject **adaptor interfaces** defined in rest; sitemanage
  provides the only production implementations under `com.percussion.apibridge`.
- Domain exceptions and AuthZ failures are translated in the **apibridge** (e.g. `Optional.empty()`
  → `WebApplicationException` 403), not by importing sitemanage types into rest.

See `projects/sitemanage/AGENTS.md` and README.md.

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

1. **Implement the Adaptor in sitemanage** (not in rest):

```java
// projects/sitemanage/.../com/percussion/apibridge/ResourceAdaptor.java
@PSSiteManageBean
public class ResourceAdaptor implements IResourceAdaptor {

    private final SomeDomainService domainService;

    @Autowired
    public ResourceAdaptor(SomeDomainService domainService) {
        this.domainService = domainService;
    }

    @Override
    public List<ResourceDto> getResources() {
        return domainService.findAll(); // map domain → rest wire DTOs as needed
    }
}
```

### Best Practices for REST Resources

- **Resource Class** (rest): HTTP concerns only (routing, binding, serialization, OpenAPI)
- **Adaptor Interface** (rest): Clear contracts; prefer rest wire DTOs as return types
- **Adaptor Implementation** (sitemanage apibridge): CMS/domain logic, AuthZ mapping, error cases
- **Wire DTOs** (rest): Jackson/JAXB annotations; never import sitemanage domain types into resources
- **OpenAPI Annotations**: Always include `@Operation`, `@ApiResponse`, `@Parameter` annotations
- **HTTP Methods**: Use `@GET`, `@POST`, `@PUT`, `@PATCH`, `@DELETE` appropriately
- **Path Parameters**: Use `/resources/{id}` for resource identity
- **Query Parameters**: Use `?type=X&status=Y` for filtering
- **Request Body**: Use `@Consumes("application/json")` for POST/PUT
- **Response Control**: Prefer returning `Response` when status codes must vary
- **Response Objects**: Use `@XmlRootElement` / `@JsonRootName` for wire envelopes when required

### Error Handling

REST resources should return appropriate HTTP status codes. See README.md for the complete status code reference and error handling patterns.

Use the standard error response structure from `com.percussion.rest.errors.*` package for consistency.

## Testing

### MainTest Spring stubs (instance of root **Change-class completeness**)

This is **one** rest-module instance of the monorepo hard gate in root `AGENTS.md` →
**Change-class completeness**. The general failure mode is “primary artifact + one unit test”
without peer companions and without verifying the **shared** test context.

`MainTest.ContextConfiguration` component-scans `com.percussion.rest` and loads every
`@PSSiteManageBean` / `@Component` resource. Production adaptor **implementations** live only in
**sitemanage**, so the rest test classpath has **no** real `IXxxAdaptor` beans.

If a resource constructor- or field-injects an adaptor and there is no test stub bean, Spring fails with:

```text
UnsatisfiedDependencyException: No qualifying bean of type '...IXxxAdaptor'
Failed to load ApplicationContext
ApplicationContext failure threshold (1) exceeded   ← cascade on RolesTest/UsersTest/MainTest/…
```

|               Layer                |                 What to add                 |                         Why                          |
|------------------------------------|---------------------------------------------|------------------------------------------------------|
| Mockito resource unit test         | `*ResourceTest` mocking `IXxxAdaptor`       | HTTP mapping / status codes                          |
| **Spring context stub (required)** | `TestXxxAdaptor` implementing the interface | Lets `MainTest` / subclasses load ApplicationContext |
| sitemanage adaptor unit test       | `*AdaptorImplTest` mocking domain services  | Gate / GCM / domain behavior                         |

**Required stub location and annotations** (copy an existing peer):

- Preferred package: `rest/src/test/java/com/percussion/rest/test/apibridge/TestXxxAdaptor.java`
- Or package-local: `rest/src/test/java/com/percussion/rest/<area>/XxxTestAdaptor.java`
- Annotate with `@Component` + `@Lazy` (same as `TestLocalesAdaptor`, `TestSystemDefAdaptor`,
  `RelationshipSummaryTestAdaptor`, `RoleTestAdaptor`, …)
- Implement every interface method with trivial stubs (`null`, `List.of()`, fixed ok result)

**Checklist when adding a new resource that injects a new adaptor interface:**

1. [ ] Resource + interface + DTOs in rest
2. [ ] Mockito `*ResourceTest` in rest
3. [ ] **`TestXxxAdaptor` / `XxxTestAdaptor` Spring stub in rest test sources**
4. [ ] sitemanage apibridge impl + unit tests
5. [ ] Run from module: `cd rest && ../mvnw test` (Windows: `..\mvnw.cmd test`) and confirm
   **zero** `Failed to load ApplicationContext` / `No qualifying bean of type` errors

Do **not** “fix” missing stubs by removing constructor injection, making the adaptor optional, or
excluding the resource from component-scan unless product design explicitly requires it.

### Quick Test Structure (Mockito resource tests)

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
./mvnw -pl rest clean install
```

## Hot Deployment

For rapid development iteration:

```bash
./scripts/hot-deploy-local.py --install-dir /path/to/install --modules rest,system --restart
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

