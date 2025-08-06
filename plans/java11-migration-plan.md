# Java 11 Migration Plan

This plan completes and standardizes the repository migration to Java 11. It minimizes risk by aligning build configuration, consolidating dependencies, unifying the servlet/JAX-RS stack, and validating with local builds and lightweight CI.

## 1) Context Summary

- Build: Maven multi-module reactor with parent POM at [`pom.xml`](pom.xml).
- Java config: Parent properties already set to Java 11: [`pom.xml`](pom.xml:86), [`maven-compiler-plugin`](pom.xml:557). Most children inherit Java 11; one outlier uses Java 8: [`modules/perc-checkboxtree/pom.xml`](modules/perc-checkboxtree/pom.xml:32).
- Servlet/JAX-RS:
  - Majority uses javax.* Servlet API (3.1/4.0) and JAX-RS via CXF 3.5.x or Jersey 2.x.
  - One module uses Jakarta 5 APIs: [`deliverytiersuite/delivery-tier-suite/membership/pom.xml`](deliverytiersuite/delivery-tier-suite/membership/pom.xml:47,53), which is incompatible with the javax-based stack.
- Java 11 removed modules:
  - No direct source imports found, but POMs add JAXB and Activation where needed: [`system/pom.xml`](system/pom.xml:416), etc.
- Axis/JAX-RPC legacy: Axis 1.4 remains throughout (webservices/integrations/system). Works on Java 11 with explicit JAXB/Activation deps and codegen plugins, but is legacy.
- Version drift:
  - CXF 3.5.11 in root [`pom.xml`](pom.xml:145,1023), CXF 3.3.5 in delivery-tier parent [`deliverytiersuite/delivery-tier-suite/pom.xml`](deliverytiersuite/delivery-tier-suite/pom.xml:58).
  - Jersey 2.33 across delivery-tier.

## 2) Key Risks

1. Mixed javax vs jakarta artifacts: Jakarta 5 (membership module) alongside javax stack => compile/runtime conflicts on Tomcat 9.
2. Axis/JAX-RPC: Requires JAXB/Activation at build and runtime; codegen plugins must run under JDK 11.
3. REST stack inconsistency: Different CXF versions across parents complicate resolution.
4. Servlet API and scopes: Some modules use old servlet-api artifacts and compile scope instead of provided.
5. Module compiler flags: A module still targets Java 8.

## 3) Staged Execution Plan

### Stage 0 — Branching and baseline
- Create branch: `development-java-11-integration`.
- Cherry-pick/merge any missing changes from your prior `development-java-11` branch.

### Stage 1 — Enforce a consistent Java 11 toolchain
- Parent [`pom.xml`](pom.xml):
  - Ensure `maven-compiler-plugin` 3.11.0+ set with source/target 11 and optionally `release=11` in pluginManagement.
  - Keep properties `maven.compiler.source/target=11` [`pom.xml`](pom.xml:87-88). Prefer central configuration.
- Modules:
  - Update residual Java 8 flags in [`modules/perc-checkboxtree/pom.xml`](modules/perc-checkboxtree/pom.xml:32-34) to 11 or remove to inherit.

### Stage 2 — Unify on javax.* stack (Tomcat 9+)
- Replace Jakarta APIs with javax in the membership module:
  - [`deliverytiersuite/delivery-tier-suite/membership/pom.xml`](deliverytiersuite/delivery-tier-suite/membership/pom.xml:47,53): replace `jakarta.servlet-api` 5.0.0 with `javax.servlet:javax.servlet-api` 4.0.1 (scope provided) and `jakarta.annotation-api` with `javax.annotation:javax.annotation-api` 1.3.2.
- Standardize Servlet API:
  - Adopt `javax.servlet:javax.servlet-api:4.0.1` (provided) for all WARs and libraries; remove legacy `servlet-api` artifacts and ensure they are not packaged in WARs. Example offenders to adjust:
    - [`modules/perc-security-utils/pom.xml`](modules/perc-security-utils/pom.xml:111) (switch to provided or remove if not necessary at runtime)
    - [`modules/perc-ant/pom.xml`](modules/perc-ant/pom.xml:105) (should typically be provided)
    - Check exclusions referencing `servlet-api` vs `javax.servlet-api` in root and module POMs.
- Annotation API:
  - Standardize to `javax.annotation:javax.annotation-api:1.3.2` (avoid jakarta.* until a future Jakarta migration).

### Stage 3 — JAXB/Activation for Java 11
- Keep explicit dependencies where JAXB is used:
  - `javax.xml.bind:jaxb-api:2.3.1` and `org.glassfish.jaxb:jaxb-runtime:2.3.3` (already present e.g. [`system/pom.xml`](system/pom.xml:416-423)).
  - `javax.activation:javax.activation-api:1.2.0` (already present).
- Centralize versions via parent `dependencyManagement` and remove duplicate child declarations where possible.

### Stage 4 — REST/CXF version consolidation
- Align delivery-tier to CXF 3.5.11 to match root:
  - Update `cxf.version` in [`deliverytiersuite/delivery-tier-suite/pom.xml`](deliverytiersuite/delivery-tier-suite/pom.xml:58) from 3.3.5 to 3.5.11.
- Keep Jersey 2.33 for delivery-tier modules, ensuring no Jakarta variants sneak in. Preserve exclusions that remove jakarta.annotation from jersey-spring5 (already configured).

### Stage 5 — Axis 1.4/JAX-RPC
- Short-term: retain Axis 1.4 with plugin-based code generation:
  - Verify `axistools-maven-plugin` and `axis:wsdl2java` executions compile under JDK 11:
    - [`modules/webservices/pom.xml`](modules/webservices/pom.xml:18,206)
    - [`deliverytiersuite/delivery-tier-suite/integrations/pom.xml`](deliverytiersuite/delivery-tier-suite/integrations/pom.xml:294)
  - Ensure plugin executions include JAXB/Activation dependencies at plugin level when necessary (already shown in modules/webservices config).
- Long-term tech debt: plan deprecation/migration from Axis to JAX-WS/REST.

### Stage 6 — Plugins and test runners
- Standardize Surefire:
  - Parent pluginManagement: `maven-surefire-plugin 3.1.2` with `-Djava.awt.headless=true`, reasonable memory; many modules already 3.0.0–3.1.2.
- Enforcer:
  - Add rules to require Java 11 and to ban `jakarta.servlet*` and `jakarta.annotation*` until Tomcat 10 migration.

### Stage 7 — Build and runtime verification
- Build smoke:
  - `mvn -T 1C -DskipTests clean install` to validate compilation and packaging.
- Tests:
  - `mvn test` (consider `-DforkCount=1C -Dmaven.test.failure.ignore=false`).
- Runtime:
  - Deploy WebUI and delivery-tier WARs to Tomcat 9.x (Servlet 4.0).
  - Smoke test REST endpoints (CXF/Jersey), UI login/navigation, any SOAP endpoints.
  - Watch for javax/jakarta linkage errors, JAXB missing classes, or CXF/Jersey provider conflicts.

### Stage 8 — Minimal CI enablement
- Add GitHub Actions workflow to build on JDK 11:
  - Use `actions/setup-java@v4` with Temurin 11.
  - Command: `mvn -B -DskipTests clean verify`. Enable tests once stable.
  - Cache Maven repository.

### Stage 9 — Documentation and guardrails
- Update contributor/developer docs to require JDK 11.
- State container target: Tomcat 9.x (Servlet 4).
- Open issues:
  - Refactor away from Axis/JAX-RPC.
  - Future Jakarta namespace migration and Tomcat 10+ support.

## 4) Concrete Change Checklist

1. Parent alignment (dependencyManagement + pluginManagement)
   - Define unified versions:
     - `javax.servlet:javax.servlet-api:4.0.1` (provided)
     - `javax.annotation:javax.annotation-api:1.3.2`
     - `javax.xml.bind:jaxb-api:2.3.1`
     - `org.glassfish.jaxb:jaxb-runtime:2.3.3`
     - `javax.activation:javax.activation-api:1.2.0`
     - `org.apache.cxf:*:3.5.11` (delivery-tier alignment)
   - Plugins:
     - `maven-compiler-plugin:3.11.0` with 11 source/target (optionally `release`).
     - `maven-surefire-plugin:3.1.2`.

2. Remove Jakarta variants in `deliverytiersuite/delivery-tier-suite/membership`
   - Replace:
     - `jakarta.servlet-api` → `javax.servlet-api` 4.0.1 (provided)
     - `jakarta.annotation-api` → `javax.annotation-api` 1.3.2

3. Update Java 8 module
   - [`modules/perc-checkboxtree`](modules/perc-checkboxtree/pom.xml:32): set source/target 11 or remove to inherit.

4. Fix servlet API scopes and artifacts
   - Change compile-scoped servlet dependencies to provided in:
     - [`modules/perc-security-utils`](modules/perc-security-utils/pom.xml:111)
     - [`modules/perc-ant`](modules/perc-ant/pom.xml:105) (if not truly needed at runtime)
   - Ensure no servlet-api JARs end up in WARs (check WebUI filters: [`WebUI/pom.xml`](WebUI/pom.xml:236)).

5. Align delivery-tier CXF
   - Update `cxf.version` in [`deliverytiersuite/delivery-tier-suite/pom.xml`](deliverytiersuite/delivery-tier-suite/pom.xml:58) to 3.5.11.

6. Axis plugin verification
   - Confirm `axistools` and `wsdl2java` plugin executions succeed with JDK 11; add plugin-level dependencies where missing (mirror [`modules/webservices`](modules/webservices/pom.xml:18-33,123-141)).

## 5) Test Strategy

- Unit tests: run across reactor; fix module-specific surefire forks if failures due to memory/modulepath.
- Integration smoke:
  - Start Tomcat 9.x, deploy WARs, verify:
    - Servlet filters (e.g., etag filter in system)
    - REST endpoints under CXF/Jersey respond with expected payloads
    - JAXB serialization/deserialization where used
- Packaging checks:
  - Verify WebUI and delivery-tier resources assembled correctly; no `servlet-api`/`javax.*` duplication inside WARs.

## 6) Future Roadmap (Post-Java 11)

- Axis deprecation: Migrate SOAP clients/services to JAX-WS or REST; eliminate Axis toolchain and JAXB runtime coupling.
- Jakarta migration (optional next major):
  - Move to Tomcat 10.1+, update all javax.* to jakarta.* namespaces, upgrade Jersey/CXF to Jakarta variants, re-test.