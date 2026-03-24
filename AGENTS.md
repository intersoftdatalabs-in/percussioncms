# AGENTS.md - Agent Coding Guidelines

This file provides guidance for AI agents operating in the Percussion CMS codebase.

## Branch Information

- **Branch**: development-8.1.x
- **Java Version**: JDK 1.8.0 (all code must be compatible)
- Set `JAVA_HOME` before builds: `export JAVA_HOME=/usr/lib/jvm/java-1.8.0-amazon-corretto`

## Git Workflow

- **NEVER commit directly** to development-8.1.x branch
- **NEVER commit without explicit permission**
- **NEVER push to remote** without explicit permission
- Before creating a feature branch:
  1. Pull latest changes on the base branch
  2. Use existing GitHub issue or create new one first
  3. Include issue number in branch name (e.g., `bugfix/123-fix-logging`)
- All changes must be tested locally before pushing

## Build Commands

The project uses a build wrapper script `mvn-env.sh` (Linux/macOS) or `mvn-env.bat` (Windows).

```bash
# Full build
./mvn-env.sh clean install

# Single module (with dependencies)
./mvn-env.sh clean install -pl modules/perc-toolkit -am

# Skip tests
./mvn-env.sh clean install -DskipTests

# Run tests for single module
./mvn-env.sh test -pl modules/perc-toolkit

# Run single test class
./mvn-env.sh test -Dtest=MyTestClassName -pl modules/perc-toolkit

# Run single test method
./mvn-env.sh test -Dtest=MyTestClassName#testMethodName -pl modules/perc-toolkit

# Apply code formatter
./mvn-env.sh spotless:apply

# Check formatting (without applying)
./mvn-env.sh spotless:check

# Run Checkstyle
./mvn-env.sh checkstyle:check

# Run PMD analysis
./mvn-env.sh pmd:check

# Run all validations (formatting + checkstyle + pmd)
./mvn-env.sh verify
```

## Code Style

### Formatter (Spotless)

- Uses **Google Java Format**
- Runs automatically during `validate` phase
- 4 spaces for indentation (no tabs)
- 120 characters max line length (soft limit)
- No trailing whitespace

### POM Files

- 4-space indentation
- Alphabetical order for dependencies
- Use `sortPom` configuration

### Naming Conventions

|  Element  |    Convention    |         Example          |
|-----------|------------------|--------------------------|
| Classes   | PascalCase       | `ContentEditorHelper`    |
| Methods   | camelCase        | `getContentById`         |
| Variables | camelCase        | `contentId`              |
| Constants | UPPER_SNAKE_CASE | `DEFAULT_MAX_RESULTS`    |
| Packages  | lowercase        | `com.percussion.content` |

### Imports

- **No wildcard imports** (except static)
- Sort alphabetically within groups: static, java, javax, third-party, project

### Types & Code

- Use interfaces over concrete types (e.g., `List<T>` not `ArrayList<T>`)
- Use `BigDecimal` for financial calculations
- Avoid primitive wrappers when not needed
- One public class per file
- 50 lines max per method (soft guideline)
- Order: imports, class declaration, fields, constructors, methods

### Error Handling

- Use specific exception types
- Include meaningful error messages
- Never catch `Exception`/`Throwable` without rethrowing
- Log at appropriate level (ERROR for failures, WARN for recoverable)

## Testing

|       Framework       |               Use Case                |
|-----------------------|---------------------------------------|
| JUnit 4               | Legacy tests                          |
| JUnit Jupiter/JUnit 5 | New tests                             |
| TestNG                | Some modules                          |
| Playwright            | QA automation in `perc-qa-automation` |

- Test classes: `*Test.java` or `*IT.java` (integration tests)
- Test methods: `shouldDoXWhenY()` or `testDoXWhenY()`
- Unit tests: `src/test/java/`
- Integration tests: `src/it/` or `*IT.java`

## QA Automation

```bash
cd modules/perc-qa-automation/frontend
npm install
npm test                    # Run all tests
npm test -- --debug        # Debug mode
npx playwright test tests/login.spec.js  # Single test
```

## Documentation

- Add AI-generated plans/tasks to `/docs/ai-generated/tasks/`
- Pattern: `/docs/ai-generated/tasks/PR#-TaskName/`

## Key Modules

|       Module       |            Path            |    Description    |
|--------------------|----------------------------|-------------------|
| system             | system                     | CMS core          |
| WebUI              | WebUI                      | Primary UI        |
| sitemanage         | projects/sitemanage        | Backend for UI    |
| rest               | rest                       | REST API          |
| perc-toolkit       | modules/perc-toolkit       | Toolkit utilities |
| perc-qa-automation | modules/perc-qa-automation | Playwright tests  |

## Additional Guidelines

### Java Compatibility

- All code must be compatible with JDK 1.8.0
- Never introduce features requiring higher Java versions
- Always set `JAVA_HOME` before builds: `export JAVA_HOME=/usr/lib/jvm/java-1.8.0-amazon-corretto`

### Dependency Management

- Use `javax` namespace (NOT `jakarta`)
- Add missing `perc-i18n` dependency where needed
- Axis dependencies are in static lib folder (not external repository)
- Cactus tests: relocate to `CMLight-Main-cactus-tests` module
- Remove Any23 dependencies and refactor code that uses them
- Upgrade dependencies to latest JDK 1.8.0-compatible versions

