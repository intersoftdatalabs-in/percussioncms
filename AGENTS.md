# Agent Guidelines

Percussion CMS is a de-coupled Java-based content management system. It has a long history (1999-present) with earlier versions being called Rhythmyx, CM System, CM1.

This repository is a large mono-repo with many submodules.  This code base has a lot of history and legacy, and is currently in the process of being modernized and refactored, do not assume that all code is up to date with current best practices.  When making code changes, follow these guidelines:

## Rule Discovery Protocol

**Before modifying code within this repository:**

1. **Identify the module path:** Determine the specific directory context (e.g., `modules/perc-tinymce/` or `system/services/`).
2. **Check for local override files:** Scan the identified directory for the following files in this specific order of priority:
    * `AGENTS.local.md` (Personal or task-specific overrides)
    * `AGENTS.md` (Module-specific permanent rules)
3. **Apply Hierarchy:**
    * If local files exist, their instructions **supersede** global rules for that module's logic.
    * `AGENTS.local.md` takes precedence over `AGENTS.md`.
    * If no local files are found, default strictly to the root-level instructions.

## Git Branch Information

* Branch Name: development
  * All code changes in this branch must be compatible with JDK 21
* Branch Name: development-8.1.x
  * All code changes on this branch must be compatible with JDK 8.

## Building

* Use the provided environment scripts to ensure Maven uses the correct JDK when running locally:
  * Linux/macOS: `./mvn-env.sh <maven-args>`
  * Windows: `mvn-env.bat <maven-args>`
  * These scripts set `JAVA_HOME` from `JAVA_HOME_21`.
* **Before committing:** run `./mvn-env.sh spotless:check` and, if it fails, run `./mvn-env.sh spotless:apply` and re-run the check before pushing changes. (Spotless enforces code formatting/style; `google-java-format` used by Spotless requires JDK 21, so run Spotless via the wrapper scripts.)
* Example: `export JAVA_HOME=/usr/lib/jvm/java-1.21.0-amazon-corretto` before running `mvn` commands
* **ALWAYS set JAVA_HOME to the correct JDK for the active base git branch before running any direct build or external shell commands**
* development branch current JDK is 21
* development branch current Node is 22
* To build the entire project, run `./mvnw clean install` from the root directory.
* To build a specific module, navigate to that module's directory and run `../../mvn-env.sh clean install`.
* Skip tests with `-DskipTests`
* For efficency use maven module commands to build specific modules or run specific tests, e.g. `mvn-env.sh -pl module-name test` to run tests for a specific module.
* Upgrade dependencies to their latest versions that are compatible with JDK 21.0

## General Coding Rules

* Always work with the #codebase directory as the root for all file paths.
* Always use the #codebase context when resolving missing interfaces or classes.
* Write clean, maintainable code that follows the existing style and conventions of the project.
* Ensure that your code is well-documented with comments where necessary.
* Follow the existing package structure and naming conventions. Google Code Style is the preferred standard for all languages.
* Avoid introducing new dependencies unless absolutely necessary and explicity approved by the team.
* Ensure that your code does not introduce any security vulnerabilities or performance issues.
* This project uses Maven for dependency management and build automation. Follow Maven best practices for project structure, dependency management, and build configuration.
* Suggest maven refactoring when a module does not follow best practices, such as having a non-standard structure, missing or misconfigured pom.xml, or using outdated plugins or dependencies.
* Where possible, code changes should be backwards compatible and not introduce breaking changes to existing functionality. If a breaking change is necessary, it must be clearly documented and pre-approved by the team.

## Dependency Management

* This is not a Spring Boot application; avoid Spring Boot dependencies.
* Dependabot is enabled for this repository and is configured on the development branch @.github/dependabot.yml
  * All branches requiring exclusions are managed in this dependabot.yml file, and any new exclusions must be added here.
* Use Maven for dependency management; ensure all dependencies are defined in the `pom.xml`.
* Use the parent POM to manage shared dependencies and plugin versions.
* Use the `maven-enforcer-plugin` to enforce dependency versions and prevent conflicts.
* Use `maven-dependency-plugin` to analyze and manage dependencies.
* Use `maven-surefire-plugin` for running tests; ensure JUnit5 is used.
* Use `maven-compiler-plugin` to set the Java version to 21.
* Use `maven-jar-plugin` to package the application; ensure resources are included.
* Use `maven-resources-plugin` to filter and copy resources.
* Use `maven-assembly-plugin` for creating distribution packages.
* Use `maven-shade-plugin` for creating shaded JARs if needed.
* Use `maven-toolchains-plugin` to ensure the correct Java version is used during builds.
* Use `maven-enforcer-plugin` to enforce upper bound dependencies and prevent transitive dependency drifts.
* Use `maven-spotless-plugin` and `maven-checkstyle-plugin` to ensure code style consistency.
* The parent POM (`pom.xml`) has a dependencyManagement section to manage versions of dependencies used in child modules.
* The parent POM (`pom.xml`) has a pluginManagement section to manage versions of plugins used in child modules.
* The deliverytiersuite/delivery-tier-suite module has its own POM file to manage its child dependencies and plugins.
* The deliverytiersuite/delivery-tier-suite module may override dependency versions defined in the parent POM but should do so only if absolutely necessary.

## Specific Rules for Java code changes

* Use current project target JDK language features and APIs where appropriate

* Avoid reflection and dynamic class loading unless absolutely necessary and explicitly approved by the team.
* Resolve Java compiler warnings and errors before submitting code for review.
* Use appropriate logging levels (e.g. debug, info, warn, error) for logging statements, and avoid excessive logging in production code.

### Unit Testing

* Unit test cases are required for all code changes.
* Unit tests should use mock objects for code requiring a running application server.
* Use Junit 5
* Use Mockito for mocking dependencies
* Junit 3/4 tests must be migrated to Junit 5
* @Disable failing tests requires a comment with a link to the issue tracking the failure and should be temporary until the issue is resolved.
* Tests should be quiet, avoid unnecessary logging or console output. Use logging at appropriate levels (e.g. debug) for troubleshooting information.

## Rules for Front End Code Changes

* Follow the existing coding style and conventions for JavaScript/TypeScript and React.
* Use functional components and hooks where appropriate.
* @WebUI is the target module for all front end code changes, but some shared code may be moved to a common module if necessary.
