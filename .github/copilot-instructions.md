Read @AGENTS.md

## Coding Style

Important: Ensure backwards compatibility when modifying public methods/interfaces.
Follow Google Java Style Guide for Java (https://google.github.io/styleguide/javaguide.html); reformat code as needed.
Use camelCase, clear variable names, and Java 17 features (var, Optional, Streams).
Write English-only code and comments.
Fix any existing spelling/grammar issues in comments whenever you are working on code.
Use JUnit5 for tests; refactor JUnit4 tests to JUnit5.
Resources like images or property files should be in the `src/main/resources` directory.
Use `src/test/resources` for test-specific resources.
Use `src/main/java` for main application code and `src/test/java` for unit tests
If you detect a resource file that is not in the correct directory, move it to the appropriate location.

## Best Practices

Apply SOLID, DRY, KISS, YAGNI, OWASP, DOP, and DDD principles.
Write small, focused functions (< 20 lines) and pure functions for data manipulation.
Use immutable, flat, denormalized data structures; validate data explicitly.
Prefer dependency injection, static factory methods, and builders over constructors.
Avoid reflection as a shortcut for overload disambiguation or type mismatches; instead add explicit methods, overloads, or adjust method signatures to preserve compile-time type safety and clarity. See `.github/instructions/reflection-policy.md` for the policy and examples.
Use try-with-resources, minimize mutability, and avoid raw types.
Synchronize shared mutable data; prefer concurrency utilities over threads.
Avoid Java serialization; use alternatives or defensive serialization.
Write side-effect-free streams and standard functional interfaces.
Use Repository pattern for data access; avoid direct database calls in services.

## Project Structure

Source, Test, and Resource directories can be identified from the maven pom.xml files, general structure is:

```
src/main/java/: Main application code
src/main/resources/: Configuration files (e.g., application.properties)
src/test/java/: Unit tests
src/test/resources/: Test resources (e.g., test data, configuration)
docs/: Markdown documentation and API specs.
.github/copilot-instructions.md:    Copilot instructions and guidelines
.github/instructions/:    Additional Copilot instructions
.github/prompts/:    Copilot prompts for specific tasks
.github/chatmodes/:    Copilot chat modes for different contexts
.github/ISSUE_DRAFTS/:    Drafts for GitHub issues
plans/: Contains plans for tasks and issues. New plans should be added here in ISSUE-#<number>-<plan name>.md format.
```





## Git Workflow

- **NEVER commit directly** to the development-8.1.x branch
- **NEVER commit without explicit permission**
- **NEVER push to remote** without explicit user permission
- Before creating any feature branch:
  1. Always pull latest changes on the base branch first
  2. Always prompt to use an existing GitHub issue or to create a new issue first in order to document the bug/feature
  3. Always include the issue number in the branch name (e.g., `bugfix/123-fix-logging`)
- Only push commits after user has reviewed and approved changes
- All changes must be tested locally before pushing



## Java Version




## Dependencies


- Dependency versions are managed in the parent pom.xml file
- Axis:
  - Axis 1.x is retired; remove any axis dependencies and refactor code that uses axis to not use it.
  - If web services functionality is needed, use JAX-WS (Jakarta XML Web Services) or another modern web services library compatible with JDK 21.
- Cactus test framework is retired; remove any cactus dependencies and relocate any cactus tests to the CMLight-Main-cactus-tests module which is currently excluded from the build.
- Any23 is retired. Remove any Any23 dependencies. Refactor code that uses Any23 to not use it.
- Add missing perc-i18n dependency where needed.
- Prefer the jakarta namespace over javax when available; migrate to the jakarta namespace on this branch as needed for JDK 21 compatibility.
- Add all AI-generated plans, tasks, and issues to the `/docs/ai-generated/tasks/` folder for future reference.
- Organize AI-generated documentation by task using the pattern: `/docs/ai-generated/tasks/PR#-TaskName/` (e.g., `/docs/ai-generated/tasks/#524-v8.1.6-release-notes/`)

