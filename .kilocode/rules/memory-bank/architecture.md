# Architecture

High-level view of Percussion CMS modules, their relationships, and key technical decisions that guide modernization while preserving backward compatibility.

## System Architecture

- Parent Maven project orchestrates a multi-module monorepo with centralized dependencyManagement and pluginManagement.
- Core libraries provide shared utilities leveraged by higher-level modules and extensions.
- System layer aggregates core and foundational libraries used across legacy and modern modules.
- Delivery-tier suite provides API endpoints and webapps for content delivery, packaged for DTS tomcat deployments.
- Extensions integrate optional functionality such as workflow and linkback.
- Webservices preserves SOAP-based integration points for legacy compatibility.

## Source Code Paths and Major Modules

- Root parent
  - pom.xml manages Java 17, dependency versions, plugin versions, enforcer rules, and shared properties.
- Core and libraries
  - build-tools: Enforcer and build-time helpers.
  - perc-security-utils: Security utilities, ESAPI integration, Antisamy etc.
  - perc-xml-security: XML security concerns.
  - perc-legacy: Legacy compatibility utilities.
  - rxutils: XML, commons utilities; transitive hub for several older libs.
  - perc-shared-test-resources: Shared test fixtures/utilities.
- System
  - system: Broad set of legacy and shared functionality for the CMS runtime; numerous transitive dependencies.
  - servlet-utils: Servlet helpers (javax.* APIs preserved).
  - perc-simple: Lightweight utilities used by other modules.
- Webservices
  - modules/webservices: Axis 1.x and SOAP endpoints, axistools/wsdl2java integrations.
- Delivery Tier Suite
  - deliverytiersuite/delivery-tier-suite: Aggregator and submodules for delivery webapps.
  - delivery tiers: comments, feeds, forms, common, distribution packaging for DTS tomcat layout.
- Extensions
  - modules/extensions-linkback, modules/extensions-workflow, modules/extensions-serverutils: Feature add-ons with test dependencies on JUnit and JMock.
- UI and web resources
  - WebUI, cui, PCM-PkgMgtUI: Frontend assets and legacy UI frameworks.

## Component Relationships

- Parent POM
  - Enforces consistent versions for JAXB/Activation, CXF, JUnit, Oracle JDBC, commons-* stack, and build plugins.
- Core libraries
  - Consumed by system, delivery-tier, and extensions modules.
- System
  - Provides shared runtime utilities; historically pulls transitive legacy dependencies.
- Webservices
  - Depends on Axis; properties for axis versions must align to resolvable artifacts.
- Delivery-tier
  - JAX-RS/CXF-based services pinned to CXF 3.5.11; packaged into DTS tomcat-friendly layouts.
- Extensions
  - Depend on system/core; tests require JUnit 5 with Vintage for JUnit 4 remnants.

## Key Technical Decisions

- Java 17 target.
- Externalize JAXB and Activation for Java 17 using jakarta.xml.bind-api replacement with javax compat via vendor BOMs or explicit artifacts; runtime and API versions managed in parent.
- Pin Apache CXF to 3.5.11 across delivery-tier modules to ensure consistent JAX-RS behavior.
- Migrate Oracle JDBC from ojdbc6 to com.oracle.database.jdbc:ojdbc8; manage version centrally.
- Keep Axis stack at resolvable versions (1.4) and align plugins (wsdl2java/axistools) accordingly.
- Standardize Maven Surefire (3.2.5+) and testing platform to JUnit 5 with Vintage engine where legacy JUnit 4 tests exist.
- Use Maven Enforcer RequireUpperBoundDeps to surface dependency drifts; resolve centrally rather than per-module where feasible.

## Design Patterns in Use

- Parent-driven dependency and plugin management to ensure consistency.
- Module layering: core utilities -> system/shared -> delivery/extensions.
- Backward-compatible API preservation strategy: public api signatures should not change, but internal implementations may evolve.
- New apis should be added in a way that does not break existing consumers.
- SOAP apis will be removed and must be replaced with RESTful alternatives.
- Defensive exclusions: targeted exclusions (e.g., commons-discovery to reduce Axis transitives) to minimize legacy drag.

## Critical Implementation Paths

- Parent POM properties and dependencyManagement
  - Central pins for junit, cxf, jaxb, activation, commons-* libraries, slf4j, log4j.
  - PluginManagement for surefire/failsafe, enforcer, maven-compiler (Java 17), toolchains.
- Testing stack standardization
  - JUnit 5 BOM or explicit versions, Vintage engine presence for JUnit 4 tests, junit-platform-launcher pin to stable version.
- Webservices Axis alignment
  - axis.version and axiscore.version set to 1.4, plugins aligned, ensure wsdl2java plugin versions are resolvable.
- Delivery-tier packaging
  - Ensure servlet APIs are provided scope; packaging compatible with DTS tomcat; avoid bundling servlet APIs.
- Upper bound dependency resolution
  - commons-io, commons-collections4, xmlgraphics-commons, commons-logging, slf4j-api pinned in parent; prefer stable releases.

## Risks and Mitigations

- Risk: Transitive dependency drifts cause Enforcer failures.
  - Mitigation: Central pins in parent and selective exclusions for problematic legacy paths.
- Risk: junit-platform-launcher resolution flakiness from Central mirrors.
  - Mitigation: Temporarily pin to known stable version (5.13.1) per module or in parent.
- Risk: Proprietary artifacts block CI.
  - Mitigation: Document repositories or local install steps; gate CI to skip proprietary modules until credentials are available.

## CI/CD and Deploy Targets

- Build on JDK 11; run unit tests with JUnit 5 plus Vintage where necessary.
- Package delivery-tier webapps for DTS tomcat deployment.
- Future: Enable full CI across modules except proprietary; smoke deploy and regression test.

