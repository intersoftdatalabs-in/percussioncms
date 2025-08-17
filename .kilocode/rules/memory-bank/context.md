# Context

This document captures the current work focus, recent changes, and next steps for the Percussion CMS modernization effort.

## Current Work Focus

- Java 17 migration of the multi-module Maven monorepo.
- Centralizing dependencyManagement and pluginManagement in the parent POM to ensure consistent versions.
- Test stack alignment: support JUnit 5.
- Stabilizing dependency resolution (e.g., Axis 1.4, CXF 3.5.11, JAXB/Activation externalized) and cleaning duplicate/legacy declarations.
- Resolving enforcer upper bound dependency warnings by central version pins.

## Recent Changes

- Axis resolution fixed in modules/webservices by aligning Axis properties to 1.4 and avoiding non-existent 1.4.1/1.4.2 artifacts.
- ojdbc updates: moved modules from ojdbc6 to com.oracle.database.jdbc:ojdbc8, version managed in parent with appropriate scope.
- Root POM cleanup: removed JaCoCo remnants, ensured CycloneDX and SpotBugs plugins are not active, consolidated versions for JAXB and JUnit.
- Targeted extensions modules reviewed for junit/jmock versions; explicit pins or inherited versions verified.
- Defensive exclusions for Axis added under system module via commons-discovery to reduce transitive pulls.
- Small reactor validations executed to surface dependency issues and refine fixes.

## Known Issues / Open Items

- perc-system duplicate dependency declaration for org.jmock:jmock-junit5 needs cleanup (single test-scoped entry).
- junit-platform-launcher 5.13.3 intermittently fails to resolve from Central; temporary pin to 5.13.1 at module or parent may be required.
- Enforcer upper bound warnings observed:
  - commons-io (managed higher in parent vs lower transitive in modules)
  - commons-collections4 (parent may point to milestone 4.5.0-M2 vs 4.4 transitives)
  - xmlgraphics-commons (parent higher than transitive)
  - commons-logging (multiple older transitives via legacy libraries)
  - slf4j-api (minor version drifts)
- Proprietary/IDE-only artifacts (smartgwt, tinymce, caja, perc-jetty-jars) require environment handling or repository setup to avoid blocking builds.

## Next Steps

1. Fix perc-system POM hygiene:
   - Remove duplicate org.jmock:jmock-junit5 entry.
   - Add test-scoped junit-platform-launcher pin (5.13.1) or manage centrally in parent to stabilize resolution.
2. Centralize enforcer conflict resolutions in parent:
   - Pin commons-io, commons-collections4, xmlgraphics-commons, commons-logging, slf4j-api to consistent versions across modules.
   - Prefer stable releases over milestones where possible; if a milestone is required, document rationale.
3. Verify CXF alignment at 3.5.11 across delivery-tier modules via parent dependencyManagement.
4. Ensure surefire version is standardized (3.2.5+ recommended).
5. Continue module-by-module validation with -am reactors to surface remaining gaps; address missing versions (junit, jmock, axis) where flagged.
6. Plan handling for proprietary dependencies:
   - Document expected repositories or local artifact installation instructions.
   - Gate CI to skip modules requiring proprietary bits until credentials/artifacts are available.
7. Run full build on JDK 11 and smoke deploy to DTS tomcat; then enable CI on JDK 11.

## Notes

- Maintain backward compatibility for customers while modernizing. Avoid jakarta servlet/annotation artifacts.
- Keep JAXB and Activation external on Java 17 (api and runtime versions managed centrally).
- Use -U and dependency:tree to diagnose resolution issues; adjust parent management and module exclusions accordingly.
- DTS = Delivery Tier Suite, the target deployment environment for the delivery-tier-suite modules.  This is deployed seperate from the CMS. Typically on the web server.

