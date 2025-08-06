# Product

Percussion CMS enables enterprises to author, manage, and deliver web content with strong XML capabilities, modular architecture, and security-first practices aligned with OWASP. It targets on-premise DTS tomcat deployments, prioritizing backward compatibility for existing customers while steadily modernizing the stack (e.g., Java 11 migration).

## Why it exists
- Provide a robust, extensible CMS with fine-grained control over content structures (XML-first) and delivery.
- Support legacy deployments while enabling incremental modernization to current Java and ecosystem standards.
- Offer a modular, multi-module architecture for clear ownership and scalable development.

## Problems it solves
- Complex enterprise content modeling and transformations (XML processing, templating, service integration).
- Secure content workflows and integrations with legacy stacks without breaking existing deployments.
- Operational stability across a large Maven monorepo with consistent dependency and plugin management.

## How it should work (high-level)
- Core services provide content modeling, security utilities, and shared libraries.
- Delivery-tier modules expose APIs and services for content distribution.
- Extensions (e.g., linkback, workflow) integrate with core and system modules.
- Webservices module exposes SOAP-style integration points where required for legacy compatibility.
- Parent POM manages dependency and plugin versions centrally to maintain consistency.

## User experience goals
- Stable upgrades with minimal breaking changes for customers.
- Predictable builds and deployments on Java 11 and DTS tomcat.
- Clear module boundaries and consistent dependency versions to simplify maintenance.