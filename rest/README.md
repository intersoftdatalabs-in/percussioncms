# rest

This module contains the public REST API for the Percussion CMS platform. It provides customer-facing endpoints for managing content, assets, sites, workflows, and other CMS resources. The module packages JAX-RS REST resources that are deployed as part of the main `Rhythmyx` web application.

## Overview

The REST API module exposes core CMS functionality through HTTP endpoints with OpenAPI/Swagger documentation. It is built as a JAR library and packaged into the main `Rhythmyx` web application by the `perc-distribution-tree` module.

### Key Characteristics

- **Framework**: JAX-RS (Jakarta REST) with Apache CXF
- **API Documentation**: OpenAPI 3.0 via Swagger annotations
- **Data Format**: JSON and XML support via Jackson serialization
- **Authentication**: Integrated with CMS security system
- **Deployment**: Runs within the main `Rhythmyx` servlet container

## REST API Resources

The module provides 24+ REST resource endpoints organized by functional area:

### Content Management

- **Content Types** (`ContentTypesResource`) - Define and manage content structures
- **Content Lists** (`ContentListsResource`) - Query and list content items
- **Pages** (`PagesResource`) - Create, update, and manage page content
- **Assets** (`AssetsResource`) - Upload, retrieve, and manage digital assets
- **Folders** (`FoldersResource`) - Organize content hierarchically

### Site & Template Management

- **Sites** (`SitesResource`) - Create and manage CMS sites
- **Templates** (`TemplatesResource`) - Manage site templates and layouts
- **Delivery Types** (`DeliveryTypesResource`) - Configure delivery channels
- **Publishing Server** (`PublishingServerResource`) - Manage publishing infrastructure

### Configuration & Metadata

- **Contexts** (`ContextsResource`) - Manage publishing contexts
- **Communities** (`CommunityResource`) - Define organizational units
- **Community new-search defaults** (`CommunityNewSearchDefaultsResource`) - Admin GET/PUT of CX new-search defaults per community (UI-09)
- **Roles** (`RolesResource`) - Manage user roles and permissions
- **Users** (`UsersResource`) - User management and preferences
- **ACLs** (`AclResource`) - Access control list management
- **MIME Types** (`MimeTypeResource`) - Configure media type handling

### Workflow & Automation

- **Editions** (`EditionsResource`) - Manage content editions/versions
- **Action Menus** (`ActionMenuResource`) - UI action menu configuration (Admin PUT `{id}/children` for cascading MENU associations)
- **Extensions** (`ExtensionsResource`) - Custom extension management

### Utilities & Support

- **Location Schemes** (`LocationSchemesResource`) - URL location configuration
- **Display Format** - Content presentation options
- **JEXL** (`JexlResource`) - JEXL expression evaluation
- **Velocity** - Server-side template processing
- **Item Filter** - Content filtering utilities
- **Preferences** (`PreferenceResource`) - User and system preferences

## Architecture

### Adaptor Pattern

The module implements an **Adaptor pattern** to bridge REST resources with the underlying CMS system:

- **Resource Classes**: JAX-RS endpoints that expose HTTP operations
- **Adaptor Classes**: Service adapters that translate REST requests to CMS operations
- **Base Classes**: `AdaptorBase` provides common functionality for resource adaptors

This separation allows REST endpoints to remain focused on HTTP/REST concerns while adaptors handle CMS-specific business logic.

### Integration Points

#### sitemanage Module Integration

Runtime wiring is in-process Spring; **Maven direction is one-way: `sitemanage` → `rest` only**
(never `rest` → `sitemanage` — that creates a reactor cycle).

|       This module (`rest`) owns       |                           `projects/sitemanage` owns                           |
|---------------------------------------|--------------------------------------------------------------------------------|
| JAX-RS resources, OpenAPI annotations | Domain services, CM1/WebUI middleware                                          |
| Wire DTOs + `IXxxAdaptor` interfaces  | `com.percussion.apibridge.*` adaptor **implementations** (`@PSSiteManageBean`) |

Resources inject adaptor interfaces; sitemanage provides the production beans (e.g.
`PreferencesAdaptor`, `RelationshipSummaryAdaptor`). Agent rules: `rest/AGENTS.md` and
`projects/sitemanage/AGENTS.md`.

#### openapi-webapp Module Integration

OpenAPI documentation is delivered through the `modules/perc-openapi-webapp` module:

- **Source-Level Documentation**: REST resources use `@OpenAPI` and `@Path` annotations for documentation
- **Swagger Annotations**: Uses `io.swagger.v3.oas.annotations.*` for OpenAPI 3.0 specification metadata
- **Separate Deployment**: The `perc-openapi-webapp` module hosts Swagger UI separately from the main REST runtime
- **Documentation Scope**: Swagger annotations in this module are parsed by the OpenAPI webapp to generate API documentation

REST resources should include OpenAPI annotations:

```java
@OpenAPIDefinition(servers = {@Server(url = "/rest")})
@Tag(name = "resource-name", description = "Resource description")
@Operation(summary = "Operation summary", ...)
@ApiResponse(responseCode = "200", description = "Success", ...)
```

#### perc-system Module Dependency

The REST module depends on `perc-system` for:

- Core CMS domain objects (Content, Pages, Sites, etc.)
- Security and workflow utilities
- Database access through system services
- Logging and utility functions

## Building

Use the provided environment script to ensure Maven uses JDK 21:

```bash
# Build only the rest module
./mvnw -pl rest clean install

# Build with full testing
./mvnw clean install

# Skip tests for faster builds
./mvnw -pl rest clean install -DskipTests
```

### Code Style

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use Java 17+ features (var, records, Optional, Streams)
- Ensure backwards compatibility when modifying public REST endpoints
- Spotless formatting is optional (not a required process gate); match surrounding style

## Testing

Unit tests use JUnit 5 with Mockito:

```bash
# Run tests for this module
./mvnw -pl rest test

# Run specific test class
./mvnw -pl rest test -Dtest=ClassName
```

Key testing guidelines:

- Mock external dependencies (database, system services)
- Use `cxf-rt-transports-local` for local transport testing
- Test REST endpoints behavior, not business logic (that's tested in system module)
- Keep tests focused and quiet (minimal logging output)
- **New adaptor interfaces:** also add a Spring test stub (`TestXxxAdaptor` under
  `src/test/java/com/percussion/rest/test/apibridge/`) so `MainTest` can load its ApplicationContext.
  See [AGENTS.md](AGENTS.md) → **MainTest Spring stubs (HARD GATE)**.

## Hot Deployment

For development, use the hot-deploy script to rebuild and deploy:

```bash
./scripts/hot-deploy-local.py --install-dir /path/to/install --modules rest,system --restart
```

## Dependencies

Key external dependencies:

- **Apache CXF** - JAX-RS implementation
- **Jakarta REST & Servlet APIs** - Jakarta EE standards
- **Jackson** - JSON/XML serialization
- **Spring Framework** - Dependency injection (Spring context)
- **Swagger/OpenAPI** - API documentation annotations
- **Apache Commons** - Utility libraries
- **Apache Tika** - Media type detection

Internal dependencies:

- **perc-system** - Core CMS domain layer
- **perc-security-utils** - Security utilities
- **perc-i18n** - Internationalization support (where applicable)
- **utils** - Common utility classes

## OpenAPI Documentation

### Current Implementation

REST resources are documented using OpenAPI 3.0 annotations. The annotations are:

- **Source-level** documentation in the REST resource classes
- **Parsed by** the `perc-openapi-webapp` module during build/deployment
- **Served by** the separate OpenAPI web app at `/openapi` context path

### Swagger Annotation Guidelines

When adding new REST endpoints:

1. **Class-level annotations**:

```java
@OpenAPIDefinition(...)
@Tag(name = "endpoint-name", description = "...")
```

1. **Method-level annotations**:

```java
@Operation(summary = "...", description = "...")
@ApiResponse(...) // for each possible response code
```

1. **Parameter annotations**:

```java
@Parameter(description = "...")
@RequestParam
```

