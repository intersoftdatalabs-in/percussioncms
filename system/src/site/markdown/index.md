# System Module - Technical Documentation

Welcome to the technical documentation for the Percussion CMS **system** module. This module contains the core infrastructure, services, and business logic for the Percussion CMS platform.

## Quick Links

- [Module Overview](overview.html) – Complete structural overview and architecture
- [Services Architecture](services.html) – Detailed guide to service infrastructure and service locators
- [XML Application Server](xml-application-server.html) – Content type engine and dynamic class generation
- [Request Handling Flow](request-handling-flow.html) – Architecture of request routing and handler dispatch
- [Building & Development](building.html) – Build instructions, testing, and development guidelines
- [Package Reference](packages.html) – Detailed package-by-package reference documentation
- [Modernization Status](modernization.html) – Java modernization history (module is now on JDK 21)
- [Legacy Code](legacy.html) – Information about legacy components and backward compatibility
- [S3 publish on EC2 (IMDSv2)](s3-publish-ec2-imds.html) – Operator notes for Amazon Linux 2023+, hop limit, Assume Role

## Module At A Glance

The system module is organized into three primary components:

### Active Development

- **Services** (`services/src`) – Catalog, assembly, content, GUID, data, and error services
- **Business Logic** (`business/src`) – Delivery, metadata, authentication, and configuration services
- **Core CMS** (`src/main/java`) – Object store, utilities, and foundational implementations
- **Tests** (`src/test/java`) – Comprehensive unit and integration test coverage

### Configuration & Resources

- **Applications** (`applications/`) – Application definitions and XML structures
- **Configuration** (`config/`) – Server configuration, content editors, and workflow definitions
- **Deployment** (`ear/`, `release/`) – Packaging and deployment resources

### Legacy Components

- **Testing** (`Testing/`)
- **Documentation** (`Docs/`)
- **Tools** (`Tools/`)
- **Samples** (`Samples/`)
- And others maintained for backward compatibility

## Key Documentation Topics

### For New Agents

Start here if you're new to this module:

1. **[Module Overview](overview.html)** – Understand the directory structure and key components
2. **[Request Handling Flow](request-handling-flow.html)** – Learn how HTTP requests are routed and processed
3. **[XML Application Server](xml-application-server.html)** – Learn about the content type engine
4. **[Services Architecture](services.html)** – Understand how services are structured and used
5. **[Building & Development](building.html)** – Set up your environment and build the module

### For Active Development

If you're modifying code in this module:

1. **[Services Architecture](services.html)** – Service patterns and implementations
2. **[XML Application Server](xml-application-server.html)** – Content type definitions and dynamic class generation
3. **[Modernization Status](modernization.html)** – Understand Java 21 compatibility and refactoring
4. **[Building & Development](building.html)** – Test and validate your changes

### For Maintenance

If you're maintaining or refactoring legacy code:

1. **[Legacy Code](legacy.html)** – Guidelines for legacy code and backward compatibility
2. **[Modernization Status](modernization.html)** – Migration patterns and standards

---

## Important Notes

- **Java Version:** JDK 21 (required for spotless formatting)
- **Build Tool:** Maven 3.8+ with `./mvnw` wrapper script
- **Testing:** JUnit 5 (JUnit 4 support available for legacy tests)
- **Code Style:** Google Java Style with spotless auto-formatting

---

**Module Version:** 8.2.0-SNAPSHOT | **Last Updated:** March 2026
