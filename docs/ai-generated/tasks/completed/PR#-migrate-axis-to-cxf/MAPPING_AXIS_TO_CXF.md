# Axis → CXF Migration Mapping

Status: Draft

## Summary

This document maps current Axis usages in the repository to recommended CXF replacements and migration notes. It is intended to guide incremental module-by-module migration to Apache CXF (standardize on CXF 3.5.x for parity with existing modules, with a later plan to evaluate CXF 4.x / Jakarta namespace migration).

---

## High-level plan

- Inventory Axis occurrences (completed).
- Prioritize and migrate client usages first (low-risk), then converters/handlers, and finally server endpoints configured via WSDD (higher risk).
- Introduce adapter interfaces per subsystem to minimize blast radius (e.g., `SoapClient`, `SoapHandlerAdapter`).
- Replace Axis wsdl2java plugin with CXF `cxf-codegen-plugin` (`wsdl2java`) for stub generation.
- Remove `system/lib/axis` jars and Axis dependencies from POMs after successful migration of each module.

---

## Global dependency recommendations

- Align on Apache CXF 4.x for initial migration.
- Add CXF artifacts as needed per module, for example:
  - `org.apache.cxf:cxf-rt-frontend-jaxws`
  - `org.apache.cxf:cxf-rt-frontend-jaxrs` (if REST)
  - `org.apache.cxf:cxf-rt-transports-http`
  - `org.apache.cxf:cxf-rt-ws-security` (if WS-Security used)
  - `org.apache.cxf:cxf-rt-bindings-soap` / `cxf-rt-wsdl` as needed
- Replace Axis wsdl2java/axistools executions with the `cxf-codegen-plugin` in module POMs.

---

## Per-file / per-module mappings & notes

Below are the concrete Axis hits and recommended mapping actions.

### system/ear/WEB-INF/server-config.wsdd

- Problem: Axis WSDD with many `typeMapping`, handlers (`org.apache.axis.handlers.http.HTTPAuthHandler`), transport handlers (QSList/QSWSDL handlers).
- Recommendation:
  - Replace WSDD-managed Axis endpoints with standard JAX-WS endpoints and CXF servlet configuration (Spring or `cxf-servlet`).
  - Convert `typeMapping` entries to JAXB annotations on classes (use `@XmlRootElement`, `@XmlType`) or provide CXF `DataBinding`/XmlAdapter for custom types.
  - Rewrite Axis handlers as CXF interceptors or JAX-WS `SOAPHandler` implementations.
  - For WSDL publishing/qs handlers: let CXF servlet expose WSDLs at `?wsdl` automatically.

### system/lib/axis/** (axis, axis-jaxrpc, axis-saaj jars)

- Problem: bundled Axis libraries and JAX-RPC/SAAJ Axis flavors.
- Recommendation:
  - Remove these jars from module libs once migration complete.
  - Add CXF runtime and SAAJ/JAX-WS equivalents; ensure SAAJ implementation is available (provided by CXF or SAAJ RI depending on CXF version).
  - If code relied on JAX-RPC (deprecated), rewrite to JAX-WS model (stubs are similar but require changes). This may be a higher-effort rewrite.

### system/integration/PSAxisSocketFactory.java

- Problem: Uses Axis `org.apache.axis.components.net.DefaultSocketFactory` and `BooleanHolder` interface.
- Recommendation:
  - Replace with a direct implementation using Java sockets or Apache HttpClient (preferred) and expose a small adapter that implements the same internal contract.
  - If `Axis` socket factory was discovered by Axis runtime via `axis.socketFactory` property for outbound calls, use CXF `HTTPConduit` configuration for timeouts/connection settings.

### system/integration/PSWsHelper.java

- Problem: Uses `AxisProperties`, `org.apache.axis.client.Stub`, `SOAPEnvelope`, `SOAPHeaderElement`.
- Recommendation:
  - Generate CXF client stubs with `cxf-codegen-plugin` (`wsdl2java`) and replace direct `Stub` usage with generated client proxy or `Dispatch<SOAPMessage>` for raw SOAP envelopes.
  - Map header manipulation to CXF `BindingProvider` request context or to a `SOAPHandler` / CXF client interceptor (implement `org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor` or write custom interceptors).
  - Replace `AxisProperties` configuration with CXF `Bus` or `Client` configuration.

### system/webservices/transformation/converter/*.java

- Problem: Use of `org.apache.axis.types.NonNegativeInteger`, `UnsignedInt` etc.
- Recommendation:
  - Replace Apache Axis type wrappers with standard Java types validated as required (e.g., `int`, `long`, `BigInteger`) and add validation checks or JAXB adapters for unsigned behavior.
  - Consider using `javax.xml.bind.annotation.adapters.XmlAdapter` to preserve schema expectations for generated WSDLs.

### system/webservices/PSSoapLogHandler.java

- Problem: Implements Axis `BasicHandler` and uses Axis `MessageContext` / `AxisFault`.
- Recommendation:
  - Replace with a CXF interceptor (`AbstractPhaseInterceptor<SoapMessage>`) or a JAX-WS `SOAPHandler<SOAPMessageContext>` depending on server/client placement. Use CXF `LoggingInInterceptor`/`LoggingOutInterceptor` as examples.
  - Map Axis `MessageContext` usage to `org.apache.cxf.message.Message` or `SOAPMessageContext` where needed.

### system/webservices/tests (MaintainSessionTestCase, ContentTestCase, PSTestUtils, PSTestBase)

- Problem: Tests depend on Axis `Stub`, `Call`, `AttachmentPart`.
- Recommendation:
  - Rework tests to use CXF-generated stubs or JAX-WS `Service` + `Port` proxies for client calls.
  - Replace attachments with MTOM when possible or use `org.apache.cxf.jaxrs.ext.multipart.Attachment` / `javax.mail.internet.MimeBodyPart` if using SAAJ.

### modules using CXF already (rest, projects/sitemanage, WebUI, modules/perc-toolkit, etc.)

- Opportunity: Reuse existing CXF configuration and shared POM properties to standardize CXF version and plugin usage.
- Recommendation:
  - Consolidate CXF version/property in parent POM (if not already present).
  - Reuse common CXF beans (Spring) for features like logging, WS-Security, interceptors.

---

## Migration priorities (suggested)

1. Low-risk clients and helpers: `system/integration` (PSWsHelper, PSAxisSocketFactory) — small surface, good testability. ✅
2. Converters & utilities: fix axis types in `system/webservices/transformation`.
3. Tests refactoring: migrate tests to CXF client code.
4. Server endpoints and WSDD → CXF config: `system/ear/WEB-INF/server-config.wsdd` conversions (higher risk; do last).

---

## Example: Replacing an Axis handler with a CXF interceptor

- Axis handler: extends `org.apache.axis.handlers.BasicHandler` and accesses `MessageContext`.
- CXF replacement approach: implement `org.apache.cxf.interceptor.Interceptor<org.apache.cxf.message.Message>` or extend `AbstractPhaseInterceptor<SoapMessage>` and register it on the endpoint/bus.

> Note: A concrete code snippet and test will be added to the PR migrating `system/integration`.

---

## Build & CI changes

- Replace `axistools` / `axis:wsdl2java` plugin usage with `org.apache.cxf:cxf-codegen-plugin` in relevant module POMs.
- Add module-level exclusions for transitive Axis dependencies if any third-party artifacts still transitively bring Axis.
- Run `./mvnw -pl <module> -am test` for each migrated module; verify tests and integration tests.

---

## Next practical steps

- Create a small, focused PR that migrates `system/integration` clients & tests to CXF (generate stubs, swap uses of `Stub` and SOAP envelope code to CXF client proxies / Dispatch as needed). This will act as a template for other modules.
- Draft a checklist per module: (1) plugin changes, (2) code adapter, (3) generate stubs, (4) update tests, (5) remove Axis dependency.

---

## Questions / decisions required

- Do we standardize on CXF 3.5.x (conservative) or upgrade to CXF 4.x (Jakarta) now? (Choosing 4.x requires namespace `jakarta.*` decisions.)
- Are we allowed to change public APIs or WSDL contracts during migration, or must WSDL compatibility be preserved strictly?

---

## Files referenced during inventory

- `system/ear/WEB-INF/server-config.wsdd`
- `system/integration/PSAxisSocketFactory.java`
- `system/integration/PSWsHelper.java`
- `system/webservices/transformation/*Converter.java` (NonNegativeInteger / UnsignedInt usages)
- `system/webservices/PSSoapLogHandler.java`
- `system/webservices/test/*`
- `system/lib/axis/*` jars
- Parent `pom.xml` axis dependencies and plugin entries

---

If this mapping looks correct I will:
1) Draft a PR for `system/integration` that implements the CXF client adapter and updates tests, and
2) Provide the exact POM edits and `cxf-codegen-plugin` configuration for stub generation.

