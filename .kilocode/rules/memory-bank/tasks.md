# Tasks

Documentation of repetitive tasks and workflows for the Percussion CMS modernization effort.

## Java 17 Migration Sweep (Per Module)

Last performed: 2025-08-05

Files to modify:
- pom.xml of the target module
- Parent pom.xml (when centralizing version pins or pluginManagement)
- Module-specific build/plugin sections (surefire, enforcer, axistools)

Steps:
1. Verify module compiles on Java 17 with javax.* compatibility.
2. Ensure no jakarta.* artifacts are introduced.
3. Align test dependencies:
- Prefer JUnit 5 Jupiter.
- Replace org.jmock:jmock-junit4 or jmock-junit5 with mockito for mocking.
4. Update Oracle JDBC:
- Replace ojdbc6 with com.oracle.database.jdbc:ojdbc8 and inherit version from parent dependencyManagement.
- Keep runtime scope where applicable.
5. Axis alignment (if applicable):
- Use Axis 1.4; avoid 1.4.1/1.4.2.
- Align wsdl2java/axistools plugin versions to resolvable artifacts.
6. Externalize JAXB/Activation:
- Use centrally managed versions in parent for API and runtime.
7. Servlet API:
- Ensure provided scope and do not bundle in WARs.
8. Enforcer and Surefire:
- Confirm surefire 3.2.5+ in parent pluginManagement.
- Enable RequireUpperBoundDeps; resolve conflicts via parent pins or selective exclusions.
9. Validate with focused reactor:
- mvn -U -DskipTests -pl <module> -am validate
- Use -rf :artifactId to resume after failures.

Important considerations:
- Use dependency:tree to locate transitive sources causing UBD violations.
- Prefer stable versions over milestones; document rationale when a milestone is required.
- Pin junit-platform-launcher to 5.13.1 if 5.13.3 resolution is flaky.

Example outcome:
- Module compiles on Java 17, tests execute via surefire, no servlet API bundling, and no unresolved Axis artifacts.

---

## Axis 1.4 Resolution Fix (Webservices)

Last performed: 2025-08-05

Files to modify:
- modules/webservices/pom.xml

Steps:
1. Set properties:
- axis.version = 1.4
- axiscore.version = 1.4
2. Align plugins:
- axis:wsdl2java-maven-plugin to 1.4 (avoid 1.4.1).
3. Validate:
- mvn -U -DskipTests -pl modules/webservices -am validate

Important notes:
- Avoid 1.4.1 and 1.4.2 which are not available in Central.
- Confirm no hardcoded 1.4.1 remains in plugin dependencies.

Example outcome:
- Reactor passes webservices; downstream modules no longer blocked by axis:axis:1.4.1.

---

## Upper Bound Dependency (UBD) Resolution via Parent

Last performed: 2025-08-05

Files to modify:
- Root pom.xml (dependencyManagement and pluginManagement)

Steps:
1. Collect UBD warnings across modules.
2. Choose consistent central versions:
- commons-io (e.g., 2.18.0)
- commons-collections4 (prefer stable; avoid milestone if possible)
- xmlgraphics-commons (align with Batik chain)
- commons-logging, slf4j-api
3. Add managed versions to parent dependencyManagement.
4. Remove per-module overrides unless necessary.
5. Re-run partial reactors to confirm resolution:
- mvn -U -DskipTests -pl <module> -am validate

Important notes:
- If a milestone is unavoidable (e.g., 4.5.0-M2), document the rationale and plan to move to a stable release later.
- Use selective exclusions to reduce legacy drag where feasible.

Example outcome:
- Enforcer passes with no UBD failures across migrated modules.

---

## JUnit Platform Launcher Stability Pin

Last performed: 2025-08-05

Update tests such as java.imports() to JUnit 5:
- org.junit.Test -> org.junit.jupiter.api.Test
- @Before/@After -> @BeforeEach/@AfterEach
- @Ignore -> @Disabled
- org.junit.Assert.* -> org.junit.jupiter.api.Assertions.*
- Replace TemporaryFolder Rule with @TempDir (java.nio.file.Path)
- Remove Categories/FixMethodOrder usages or convert to @Tag when present

Files to modify:
- Root pom.xml dependencyManagement (preferred), or affected module pom.xml (temporary)

Steps:
1. If Central metadata for 5.13.3 is flaky, pin launcher to 5.13.1.
2. Either:
- Add in parent dependencyManagement:
- org.junit.platform:junit-platform-launcher:5.13.1
- Or add a test-scoped dependency in the module (temporary) to unblock.
3. Re-run failing reactor segment:
- mvn -U -DskipTests -rf :perc-system validate (example)

Important notes:
- Keep junit5.version aligned for other JUnit artifacts; only pin launcher as needed.
- Remove temporary module-level pin once parent management is in place and stable.

Example outcome:
- Reactor unblocked; tests run under surefire with JUnit 5.

---

## Proprietary/IDE-only Artifacts Handling

Last performed: 2025-08-05

Files to modify:
- Documentation (README/CONTRIB), or parent POM profiles

Steps:
1. Identify proprietary dependencies (smartgwt, tinymce, caja, perc-jetty-jars).
2. Create Maven profiles to skip or replace where possible for CI.
3. Document credentialed repositories or local install steps.
4. Gate CI to skip modules requiring proprietary bits until credentials are available.

Important notes:
- Keep default build path free of proprietary blockers.
- Provide clear setup instructions for developers.

Example outcome:
- CI succeeds for non-proprietary modules; developers have documented steps to enable full build locally.

