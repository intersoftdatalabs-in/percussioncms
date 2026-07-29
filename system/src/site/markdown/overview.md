# System Module - Complete Overview

## Module Identity

- **Artifact ID:** perc-system
- **Version:** 8.2.0-SNAPSHOT
- **Packaging:** JAR
- **Parent POM:** core (root)
- **Java Version:** 21 (minimum 11)

## Strategic Purpose

The system module serves as the foundational core of Percussion CMS, providing:

1. **Service Infrastructure** – Abstraction layer for system services (catalog, assembly, content management)
2. **Content Management** – Dynamic content type definitions, item management, workflows
3. **Business Logic** – Domain-specific implementations for delivery, metadata, authentication
4. **Deployment Resources** – Configuration, scripts, and packaging for production deployments
5. **Backward Compatibility** – Maintenance of legacy APIs and components to ensure existing integrations continue to function

## Directory Structure (Annotated)

### Java Source Code

```
system/
├── services/                    # Service infrastructure (ACTIVE)
│   ├── src/main/java/
│   │   └── com/percussion/services/
│   │       ├── assembly/        # Template processing and content assembly
│   │       ├── catalog/         # Object cataloging and discovery
│   │       ├── content/         # Content operations (keywords, folders)
│   │       ├── contentchange/   # Change event tracking
│   │       ├── contentmgr/      # Content management (includes legacy PSTypeConfiguration)
│   │       ├── data/            # Data access and persistence
│   │       ├── error/           # Error handling and logging
│   │       ├── general/         # System information services
│   │       ├── guidmgr/         # GUID generation and management
│   │       ├── security/        # Security and ACL utilities
│   │       └── [refactored modernized classes]
│   └── src/test/java/           # Service unit tests
│
├── business/                    # Business logic (ACTIVE)
│   ├── src/main/java/
│   │   └── com/percussion/
│   │       ├── delivery/        # Content delivery services
│   │       ├── metadata/        # Metadata extraction and processing
│   │       ├── proxyconfig/     # Proxy configuration management
│   │       ├── rx.admin.jsf.*   # Admin UI JSF beans
│   │       ├── rx.design.*      # Design-time services
│   │       └── [other domains]
│   └── src/test/java/           # Business logic unit tests
│
├── servlet/                     # Servlet implementations (ACTIVE)
│   ├── src/main/java/
│   │   └── com/percussion/
│   │       └── servlets/        # HTTP request handlers
│   └── src/test/java/
│
├── beans/                       # Bean definitions (ACTIVE)
│   └── src/main/java/
│       └── com/percussion/      # Bean factories and configurations
│
├── uploader/                    # File upload service (ACTIVE)
│   └── src/main/java/
│
├── agenthandler/                # Agent-related functionality (ACTIVE)
│   └── src/main/java/
│
├── src/                         # Core CMS classes (ACTIVE)
│   ├── main/java/               # Core implementations
│   │   └── com/percussion/
│   │       ├── cms/objectstore/ # Object store implementations
│   │       ├── data/            # Data types and models
│   │       ├── utils/           # Utility classes
│   │       ├── server/          # Server core classes
│   │       └── [legacy packages]
│   └── test/java/               # Core unit tests
│
├── webservices/                 # Web service implementations (ACTIVE)
│   ├── src/main/java/
│   ├── src/test/java/
│   └── sample/
│
└── Testing/                     # Legacy test infrastructure (LEGACY)
    ├── applications/            # Test web applications
    ├── becredentials/           # Backend credentials for testing
    └── [test artifacts]
```

### Configuration & Resources

```
system/
├── config/                      # Server configuration (ACTIVE)
│   ├── config.xml               # Main server configuration
│   ├── ContentEditors/          # Content editor definitions
│   ├── Categories/              # Category definitions
│   ├── ContentTypeId/           # Content type registry
│   ├── ContentConnector/        # Content source connectors
│   └── [other configs]
│
├── applications/                # Application definitions (ACTIVE)
│   ├── Administration/          # Admin application
│   ├── sys_components/          # System components
│   ├── sys_logs/                # System logging app
│   ├── sys_PortalSupport/       # Portal integration
│   └── [other applications]
│
├── workflow/                    # Workflow definitions (ACTIVE)
│   ├── applications/            # Workflow applications
│   ├── config/                  # Workflow configurations
│   ├── Exits/                   # Workflow exit handlers
│   └── lib/                     # Workflow libraries
│
├── installResources/            # Installation scripts & templates (ACTIVE)
│   ├── install.properties       # Installation configuration
│   ├── installServer.xml        # Server installation
│   ├── installRepository.xml    # Repository setup
│   ├── installFastForward.xml   # FastForward template installation
│   └── [installation artifacts]
│
├── design/                      # Design-time resources (ACTIVE)
│   ├── dtd/                     # DTD definitions
│   └── schemas/                 # XML schemas
│
├── ear/                         # EAR assembly (ACTIVE)
│   ├── install.xml              # Assembly configuration
│   ├── jboss-4.0/               # JBoss deployment descriptors
│   └── [deployment configs]
│
├── release/                     # Release packaging (ACTIVE)
│   ├── installer/               # Installer resources
│   ├── jboss/                   # JBoss release
│   ├── tomcat/                  # Tomcat release
│   └── [release artifacts]
│
├── Defaults/                    # Default resources (LEGACY)
│   ├── ErrorPages/              # Default error pages
│   └── Stylesheets/             # Default XSLT stylesheets
│
└── [other directories]          # See README.md for full list
```

## Key Components Explained

### Service Infrastructure (services/)

The services module provides a layered architecture:

```
┌─────────────────────────────────────────────────┐
│           Service Locator Layer                 │
│  (PSXxxServiceLocator classes, thread-safe)    │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│         Service Interface Layer                 │
│  (IPSXxxService, defines contracts)            │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│       Service Implementation Layer              │
│  (PSXxxService, actual business logic)         │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│         Data Access Layer                       │
│  (Database, JDBC, Hibernate)                   │
└─────────────────────────────────────────────────┘
```

**Key Services:**

- **Catalog Service** – Discovers and enumerates CMS objects, manages type information
- **Assembly Service** – Binds content to templates, renders output
- **Content Service** – Manages content-related metadata (keywords, translations)
- **Content Manager** – High-level content read/write/delete operations
- **GUID Manager** – Allocates and validates content GUIDs
- **Data Service** – Generic data access (legacy, mostly superseded by Hibernate)
- **Error Service** – Centralized error code and message management

### Business Logic (business/)

Implements domain-specific functionality:

- **Delivery Services** – Content delivery pipeline, publishing
- **Metadata Services** – Extract, process, and index metadata; Solr integration
- **Authentication** – SSL/TLS support, certificate validation
- **Proxy Configuration** – External proxy routing and management
- **Admin UI** – JSF bean controllers for admin interface
- **Design Services** – Design-time object manipulation and workflow

### Core CMS (src/)

Foundation implementations:

- **Object Store** – PSItem, PSItemDefinition, PSContentType implementations
- **Data Types** – Type definitions, validators, converters
- **Server Core** – PSServer initialization, lifecycle management
- **Utilities** – String utils, XML utils, reflection helpers
- **Legacy Packages** – Internal packages maintained for backward compatibility

## Service Dependencies

Most services follow a consistent pattern:

```java
// Get service reference
IPSXxxService service = PSXxxServiceLocator.getXxxService();

// Use service
var result = service.someOperation(params);

// Never catch generic Exception; use service-specific exceptions
```

**Key Points:**

- All service locators are **thread-safe** (use AtomicReference)
- Services are typically **lazy-initialized** on first access
- Services may have **runtime dependencies** on other services
- Test code should **mock service locators** to avoid runtime dependencies

## Package Organization

### Core Service Packages

|                 Package                 |               Purpose                |              Status               |
|-----------------------------------------|--------------------------------------|-----------------------------------|
| `com.percussion.services.assembly`      | Template-based content assembly      | Modernized                        |
| `com.percussion.services.catalog`       | Object discovery and enumeration     | Modernized                        |
| `com.percussion.services.content`       | Content metadata (keywords, folders) | Modernized                        |
| `com.percussion.services.contentchange` | Change event notification            | Modernized                        |
| `com.percussion.services.contentmgr`    | Content read/write/delete operations | Modernized (with CGLib→ByteBuddy) |
| `com.percussion.services.data`          | Data access (legacy pattern)         | Modernized                        |
| `com.percussion.services.error`         | Error handling and logging           | Modernized                        |
| `com.percussion.services.general`       | System info and general utilities    | Modernized                        |
| `com.percussion.services.guidmgr`       | GUID generation and validation       | Modernized                        |
| `com.percussion.services.security`      | Security and ACL utilities           | Modernized                        |

### Business Logic Packages

|               Package               |              Purpose               |   Status   |
|-------------------------------------|------------------------------------|------------|
| `com.percussion.delivery.service`   | Content delivery services          | Modernized |
| `com.percussion.delivery.metadata`  | Metadata extraction and processing | Modernized |
| `com.percussion.proxyconfig`        | Proxy configuration                | Modernized |
| `com.percussion.rx.admin.jsf.beans` | Admin UI JSF beans                 | Modernized |
| `com.percussion.rx.design.impl`     | Design services                    | Modernized |

### Legacy Internal Packages

These packages are maintained for backward compatibility and are less frequently modified:

- `com.percussion.cms.objectstore.*` – Object store implementations
- `com.percussion.design.objectstore.*` – Design object models
- `com.percussion.server.*` – Server core (initialization, configuration)
- `com.percussion.util.*` – Utility classes (for legacy code)

## Dependency Management

### Maven Dependencies

Key external dependencies (see pom.xml):

- **Hibernate 7.2.6** – ORM, entity mapping, query execution
- **ByteBuddy 1.17.7** – Dynamic class generation (JDK 21 compatible)
- **Log4j 2.x** – Logging framework
- **JUnit 5** – Unit testing (with legacy JUnit 4 support)
- **Mockito** – Mocking framework for unit tests
- **Apache Commons** – Utilities (lang, io, codec, collections)
- **Jakarta EE** – JCA, JNI, XML APIs

### Cross-Module Dependencies

The system module depends on:

- **perc-security-acl-shim** – ACL and security abstractions
- Other system modules (implicitly via class loading)

**Modules that depend on system:**

- Almost all other modules (system is the core)
- Be careful when breaking public APIs

## Build & Test Workflow

### Local Build

```bash
# Build only (compile and package)
./mvnw -pl system compile
./mvnw -pl system package

# Full build with tests
./mvnw -pl system clean verify

# Run specific test
./mvnw -pl system test -Dtest=PSBeanGeneratorTest
```

### Code Quality

```bash
# Spotless: apply first, then check (mandatory order)
./mvnw spotless:apply
./mvnw spotless:check

# Run all checks and tests
./mvnw -pl system clean verify
```

### Test Coverage

- **services/** – Service layer tests (unit and integration)
- **business/** – Business logic tests
- **src/test/java/** – Core CMS tests
- Use mocking to isolate components from runtime dependencies

## Common Tasks

### Adding a New Service

1. Create interface: `com.percussion.services.xxx.IPSXxxService`
2. Create implementation: `com.percussion.services.xxx.PSXxxService`
3. Create locator: `com.percussion.services.xxx.PSXxxServiceLocator`
4. Add unit tests in `src/test/java/`
5. Update this documentation

### Modifying Existing Code

1. Run `spotless:apply` to ensure proper formatting
2. Add/update unit tests
3. Run `mvn clean verify` locally
4. Update relevant documentation (README, this overview, package docs)

### Refactoring Legacy Code

1. Check `refactored-java11-packages.txt` to avoid duplicate work
2. Modernize for JDK 21 (var, Optional, Streams, etc.)
3. Add `// REFACTORED: CP-JAVA11` marker to class
4. Maintain backward compatibility for public APIs
5. Update tracking files and documentation

---

**Related Documentation:**
- [Services Architecture](services.html)
- [Package Reference](packages.html)
- [Modernization Status](modernization.html)
- [Building & Development](building.html)

**Module Version:** 8.2.0-SNAPSHOT | **Last Updated:** March 2026
