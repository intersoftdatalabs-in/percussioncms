# System Module - Percussion CMS Core

The **system** module is the foundational core of Percussion CMS, containing essential content management, service infrastructure, business logic, and deployment resources. This is a large, historically-layered module with a mix of active Java code, configuration, legacy artifacts, and deployment resources.

## Table of Contents

- [Module Overview](#module-overview)
- [Directory Structure](#directory-structure)
- [Key Components](#key-components)
- [Java Code Organization](#java-code-organization)
- [Building the Module](#building-the-module)
- [Java 17 Modernization](#java-17-modernization)
- [Guidelines for Agents](#guidelines-for-agents)

## Module Overview

The system module provides:

- **Core CMS functionality** – Content management, assembly, catalog, and workflow services
- **Service infrastructure** – GUID management, data access, error handling, change tracking
- **Business logic** – Delivery services, proxy configuration, authentication, metadata extraction
- **Configuration** – Application configurations, content editors, workflow definitions, databases
- **Deployment resources** – Installation scripts, ear/war packaging, Jetty configuration, sample content
- **Legacy components** – Historical code, deprecated APIs, support packages maintained for backward compatibility

## Directory Structure

### Active Development (Java Code)

|     Directory      |                                     Purpose                                     |    Status    |
|--------------------|---------------------------------------------------------------------------------|--------------|
| `services/src`     | Service interfaces and implementations (catalog, assembly, content, GUID, etc.) | **Active** ✅ |
| `business/src`     | Business logic for delivery, proxy config, authentication, metadata             | **Active** ✅ |
| `servlet/src`      | Servlet implementations and HTTP handlers                                       | **Active** ✅ |
| `src/main/java`    | Core CMS classes, utilities, object store implementations                       | **Active** ✅ |
| `src/test/java`    | Unit and integration tests                                                      | **Active** ✅ |
| `beans/src`        | Bean definitions and factories                                                  | **Active** ✅ |
| `uploader/src`     | File upload handling                                                            | **Active** ✅ |
| `agenthandler/src` | Agent-related functionality                                                     | **Active** ✅ |

### Configuration & Resources

|           Directory            |                          Contents                           |   Status   |
|--------------------------------|-------------------------------------------------------------|------------|
| `config/`                      | Server configuration, content editors, workflow, categories | **Active** |
| `applications/`                | Application XML definitions and resources                   | **Active** |
| `installResources/`            | Installation scripts and resource templates                 | **Active** |
| `design/dtd`, `design/schemas` | DTD and XML schema definitions                              | **Active** |

### Deployment & Packaging

|     Directory     |                     Purpose                     |   Status   |
|-------------------|-------------------------------------------------|------------|
| `ear/`            | Enterprise Archive (EAR) assembly configuration | **Active** |
| `webservices/src` | Web service implementations                     | **Active** |
| `release/`        | Release packaging for Jetty, JBoss, Tomcat      | **Active** |

### Legacy & Historical (Minimal Activity)

|      Directory       |                        Contents                        | Status |
|----------------------|--------------------------------------------------------|--------|
| `Testing/`           | Legacy test applications and test data                 | Legacy |
| `Docs/`              | Historical documentation (check for active content)    | Legacy |
| `FastForward/`       | Legacy sample content and templates                    | Legacy |
| `Designer/`          | Admin UI templates and resources                       | Legacy |
| `Defaults/`          | Error pages and default stylesheets                    | Legacy |
| `VersionControl/`    | Version tracking files                                 | Legacy |
| `Tools/`             | Legacy conversion and utility tools (HTTPClient, etc.) | Legacy |
| `Samples/`           | Sample applications and content                        | Legacy |
| `lib/`               | Legacy JAR libraries and binaries                      | Legacy |
| `DTD/`               | Legacy DTD files                                       | Legacy |
| `databases/`         | Tutorial/sample databases                              | Legacy |
| `ReleasedDocuments/` | Old documentation archives                             | Legacy |

### Configuration Management

|           Directory           |                  Purpose                  |
|-------------------------------|-------------------------------------------|
| `configmgr/`, `dtsconfigmgr/` | Legacy configuration management utilities |
| `cms/content/`                | Content type configurations               |

## Key Components

### Service Infrastructure (`services/src`)

Provides critical services for content and system management:

- **Catalog Service** – Object enumeration, type discovery, XML serialization
- **Assembly Service** – Template processing, variable binding, content rendering
- **Content Service** – Keyword management, auto-translations, folder properties
- **Content Manager** – High-level content operations
- **GUID Manager** – Unique identifier generation and management
- **Data Service** – Data access and persistence patterns
- **Error Service** – Error handling and logging
- **Change Tracking** – Content modification notifications
- **General Info** – System information and configuration access

### Business Logic (`business/src`)

Implements domain-specific functionality:

- **Delivery Services** – Content delivery, publishing, and rendering
- **Metadata Services** – Metadata extraction, processing, Solr integration
- **Authentication & Client** – SSL, trust management, client communication
- **Proxy Configuration** – Proxy management for external integrations
- **JSF/Admin Beans** – Admin UI controller logic
- **Design Implementation** – Design-time behaviors and configurations

### Core CMS (`src/main/java`, `src/test/java`)

Foundation classes and utilities:

- Object store implementations (PSItemDefinition, PSContentType, etc.)
- Data types and content models
- Backend utilities and helpers
- Database connectivity (JDBC)
- Caching and performance optimization

### Legacy Content Management

- **Content/Type Configuration** – Dynamic content type definitions and bean generation
- **Workflow** – Workflow definitions, transitions, and actions
- **Servlet Infrastructure** – HTTP request handling and routing

## Java Code Organization

### Package Structure

```
com.percussion
├── services/
│   ├── assembly/          # Template and content assembly
│   ├── catalog/           # Object cataloging and discovery
│   ├── contentchange/      # Change tracking
│   ├── contentmgr/        # Content management (includes legacy PSTypeConfiguration)
│   ├── content/           # Content operations (keywords, translations, folders)
│   ├── data/              # Data access and persistence
│   ├── error/             # Error handling
│   ├── general/           # General utilities
│   ├── guidmgr/           # GUID generation
│   └── security/          # Security and ACL utilities
├── business/
│   ├── delivery/          # Content delivery services
│   ├── metadata/          # Metadata extraction
│   ├── rx.admin.jsf.*     # Admin UI beans
│   ├── rx.design.*        # Design services
│   └── proxyconfig/       # Proxy configuration
├── cms/objectstore/       # Object store implementations
├── utils/                 # Utility classes
└── [legacy packages]      # Internal packages maintained for backward compatibility
```

### Refactoring Status

See `refactored-java11-packages.txt` and `refactored-soap-packages.txt` for lists of modernized packages. Classes marked with `// REFACTORED: CP-JAVA11` have been updated to Java 17 standards while maintaining backward compatibility.

## Building the Module

### Prerequisites

- JDK 21 (for `spotless` formatting)
- Maven 3.8+

### Build Commands

**Compile only:**

```bash
./mvnw -pl system compile
```

**Run tests:**

```bash
./mvnw -pl system test
```

**Full build with packaging:**

```bash
./mvnw -pl system clean install
```

**Code style check & formatting:**

```bash
./mvnw -pl system spotless:check
./mvnw spotless:apply  # If formatting is needed
```

### Build Output

- `target/perc-system-8.2.0-SNAPSHOT.jar` – Packaged module JAR containing all compiled classes and resources

## Java 17 Modernization

### Completed in This Module

- ✅ Migrated all active code to Java 17 (var, Optional, Streams, enhanced generics)
- ✅ Replaced Log4j 1.x with Log4j 2.x
- ✅ Refactored JUnit 4 tests to JUnit 5 (where applicable)
- ✅ Replaced CGLib with ByteBuddy for dynamic bean generation (JDK 21 compatible)
- ✅ Applied Google Java Style formatting
- ✅ Enhanced immutability and null safety throughout active code

### Key Migrations

|         Component          |                            Migration Notes                             |
|----------------------------|------------------------------------------------------------------------|
| Dynamic Bean Generation    | Replaced CGLib `BeanGenerator` with ByteBuddy in `PSTypeConfiguration` |
| Data Package               | See `business/refactored-soap-packages.txt`                            |
| Delivery/Metadata Services | All classes use Java 17 features; see `refactored-java11-packages.txt` |
| Admin UI Beans             | JSF compatibility maintained; Java 17 modernized                       |

### Backward Compatibility

- **All public APIs remain backward compatible** – No breaking changes
- Existing code using these classes requires no modifications
- Internal refactoring improves maintainability, performance, and security

## Guidelines for Agents

Before working on this module, agents **MUST**:

1. **Read this README thoroughly** to understand the module structure and organization
2. **Review relevant documentation** – See [Maven Site Documentation](../../docs/modules/system.html) (when available)
3. **Check Java modernization status** – Review `refactored-java11-packages.txt` and `refactored-soap-packages.txt`
4. **Identify the right directory** – Use the table above to find the correct package/directory for your task
5. **Understand legacy vs. active code** – Legacy directories (Testing, Tools, etc.) have minimal updates

### When Modifying Java Code

- **Ensure Java 17 compatibility** – Use modern language features and avoid deprecated APIs
- **Use Google Java Style** – Run `spotless:check` and `spotless:apply` before committing
- **Add unit tests** – Use JUnit 5; maintain high code coverage
- **Update this README** – If you discover structural issues or add new subsystems
- **Mark refactored classes** – Add `// REFACTORED: CP-JAVA11` and update tracking files if modernizing legacy code

### Building & Testing

Always test locally before pushing:

```bash
./mvnw -pl system clean verify
./mvnw spotless:apply
./mvnw -pl system test
```

For changes to service implementations, also test integration with dependent modules.

---

**Last Updated:** March 2026 | **Module Version:** 8.2.0-SNAPSHOT

## Building

mvn clean install

## Java 17 Refactoring

### Package: com.percussion.rx.delivery.impl

- All delivery handler classes in this package have been refactored to use Java 17 features (var, Optional, Streams, try-with-resources, etc.).
- Improved type safety, immutability, and error handling.
- All public APIs remain backward compatible.
- Logging is now fully Log4j 2.x and OWASP compliant.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these handlers, ensure your code is Java 17 compatible.
- Optional is now used for some return types (see getPubServerDao().findPubServer and getPropertyValue).
- No breaking changes to public interfaces.

### Package: com.percussion.rx.audit

- All audit logging and design object auditing classes in this package have been refactored to use Java 17 features (var, Optional, Streams, Google Java Style).
- Deprecated methods (e.g., setDate(Date)) are still used for backward compatibility; see inline comments for details.
- All public APIs remain backward compatible.
- Logging and exception handling improved for maintainability and security.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these audit classes, ensure your code is Java 17 compatible.
- Deprecated methods are retained for legacy integration; review comments for migration guidance.
- No breaking changes to public interfaces.

### Package: com.percussion.EditableListBox

- All EditableListBox UI components in this package have been refactored to use Java 17 features (generics, Google Java Style, removal of legacy/unused code).
- Improved type safety and maintainability.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these UI components, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.validation

- All validation constraints and framework classes in this package have been refactored to use Java 17 features (var, Optional, Streams, Google Java Style).
- Improved type safety, immutability, and error handling.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these validation classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.layout

- All grid and box layout manager classes in this package have been refactored to use Java 17 features (generics, Google Java Style).
- Improved type safety and maintainability.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these layout classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.error

- All error handling and string bundle classes in this package have been refactored to use Java 17 features (Google Java Style, improved exception handling).
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these error classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.ImageListControl

- All image list UI components in this package have been refactored to use Java 17 features (generics, Google Java Style, removal of legacy/unused code).
- Improved type safety and maintainability.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these UI components, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.integration

- All integration utility and helper classes in this package have been refactored to use Java 17 features (var, Optional, Streams, Google Java Style).
- Improved type safety, immutability, and error handling.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these integration classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.webdav

- All WebDAV servlet and constants classes in this package have been refactored to use Java 17 features (generics, Google Java Style, improved type safety).
- Improved maintainability and compliance with Google Java Style.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these WebDAV classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.hooks

- All core servlet hooks and utility classes in this package have been refactored to use Java 17 features (generics, Google Java Style, improved type safety).
- Improved maintainability and compliance with Google Java Style.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these hooks classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.hooks.webservices

- All SOAP webservices servlet endpoint classes in this package have been refactored to use Java 17 features (generics, Google Java Style, improved type safety).
- Deprecated HTTP client APIs are retained for backward compatibility; see TODO comments for future migration.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these webservices classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.
- Review TODO comments for future HTTP client migration.

### Package: com.percussion.util.servlet

- All servlet utility, HTTP request/response, and multipart handling classes in this package have been refactored to use Java 17 features (var, Optional, Streams, generics, Google Java Style).
- Improved type safety, immutability, and error handling.
- All public APIs remain backward compatible.
- Logging is now fully Log4j 2.x and OWASP compliant.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these servlet utility classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

