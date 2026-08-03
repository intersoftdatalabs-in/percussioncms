This project follows the Universal Code v1.0.0 — read https://github.com/monkeyking-hq/universal-code/blob/main/UC-EMBED-v1.0.0.md

# System Module - Agent Guidelines (AGENTS.md)

**This file guides agents on working with the Percussion CMS system module. READ THIS BEFORE STARTING ANY TASK IN THIS MODULE.**

## Pre-Work Requirements

Before starting ANY task on the system module, you MUST:

1. ✅ **Read the [README.md](README.md)** in full
   - Understand the module structure and organization
   - Identify where your changes belong
   - Review the directory structure tables
2. ✅ **Review Module Documentation** at `src/site/markdown/`:
   - [index.md](src/site/markdown/index.md) – Overview and quick links
   - [overview.md](src/site/markdown/overview.md) – Complete structural details (IMPORTANT)
   - [services.md](src/site/markdown/services.md) – Service architecture (if working on services)
   - [building.md](src/site/markdown/building.md) – Build and development workflow
3. ✅ **Understand Java Version Requirements**
   - **`development` baseline is JDK 21** (parent `release=21`)
   - Spotless requires JDK 21 at runtime
   - Use `./mvnw` / `mvnw.cmd` with `JAVA_HOME` → JDK 21
   - Do **not** recreate or consult deleted Java 11/17 package modernization logs

## Rule Discovery Protocol

**For any code modifications in this module:**

1. Check for local override files in the module directory:
   - `system/AGENTS.local.md` (not currently present)
   - `system/AGENTS.md` (this file)
2. If no local overrides found, apply the rules below
3. Use the parent repository's [AGENTS.md](../../AGENTS.md) as the foundation

## Core Rules for System Module

### Architecture & Organization

**DO:**
- ✅ Follow the existing package structure (see overview.md Table: Core Service Packages)
- ✅ Place new services in `system/services/src/main/java/com/percussion/services/`
- ✅ Place business logic in `system/business/src/main/java/com/percussion/`
- ✅ Place tests in corresponding `src/test/java/` paths
- ✅ Use the service locator pattern for all new services (see services.md)
- ✅ Implement the interface → implementation → locator pattern

**DON'T:**
- ❌ Add code to legacy directories (Testing, Tools, Docs, Samples, etc.) unless specifically maintaining legacy code
- ❌ Create new packages outside the established hierarchy
- ❌ Mix services and business logic in the same package
- ❌ Use constructor injection for services; use locators instead
- ❌ Add arbitrary dependencies without documenting in updating documentation

### Code Quality

**MUST DO:**
- ✅ Run `./mvnw spotless:apply` before committing
- ✅ Run `./mvnw spotless:check` to verify formatting
- ✅ Target **JDK 21** (parent POM `release=21`); use modern Java features that compile on 21 (`var`, `Optional`, Streams, try-with-resources, records/pattern matching where they fit)
- ✅ Follow Google Java Style (enforced by Spotless; Spotless runs under JDK 21)
- ✅ Add comprehensive unit tests (use JUnit 5, not JUnit 4 for new code)
- ✅ Prefer clear commits when modernizing legacy code; ignore historical `// REFACTORED: CP-JAVA11` markers in source (labels only)
- ✅ Handle specific exceptions, not generic Exception
- ✅ Update documentation when adding new subsystems

**EXAMPLES:**

```java
// ✅ Good
var users = userService.findUsers(criteria)
    .stream()
    .filter(u -> u.isActive())
    .map(User::getName)
    .collect(Collectors.toList());

Optional<User> user = userService.findById(id);
user.ifPresent(u -> {
    // Process user
});

try {
    service.operation();
} catch (PSServiceException e) {  // Specific exception
    log.error("Service error", e);
}

// ❌ Avoid (legacy patterns)
List users = userService.findUsers(criteria);
for (int i = 0; i < users.size(); i++) {
    User u = (User) users.get(i);
    if (u.isActive()) {
        // Process
    }
}

try {
    service.operation();
} catch (Exception e) {  // Generic exception
    e.printStackTrace();
}
```

### Service Development

**When creating a new service:**

1. Create interface in `com.percussion.services.xxx.IPSXxxService`
2. Create implementation in `com.percussion.services.xxx.PSXxxService`
3. Create locator in `com.percussion.services.xxx.PSXxxServiceLocator`
4. Implement service locator pattern (see services.md: Architecture Overview)
5. Add comprehensive unit tests
6. Update README.md and service architecture documentation
7. Build and verify: `./mvnw clean verify`

**Service Exception Handling:**

```java
// Create domain-specific exception
public class PSXxxException extends PSServiceException {
    // Implementation
}

// Use in service implementation
try {
    // data access
} catch (SomeLowLevelException e) {
    throw new PSXxxException("User-friendly message", e);
}

// Clients catch specific exception
try {
    service.operation();
} catch (PSXxxException e) {
    // Handle domain error
}
```

### Testing Requirements

**For all new code:**

- ✅ Write unit tests in `src/test/java/`
- ✅ Use JUnit 5 (`org.junit.jupiter.api.*`)
- ✅ Use Mockito for mocking dependencies
- ✅ Aim for 85%+ code coverage
- ✅ Test both happy path and error conditions
- ✅ Use fluent assertions (AssertJ) where appropriate

**Example test:**

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PSXxxServiceTest {

    @Test
    void serviceOperationSucceeds() {
        var service = new PSXxxService();
        var result = service.doSomething();
        assertNotNull(result);
    }

    @Test
    void serviceThrowsExceptionOnError() {
        var service = new PSXxxService();
        assertThrows(PSXxxException.class, () -> {
            service.failOperation();
        });
    }
}
```

### Build & Verification

**Before committing ANY change:**

```bash
# 1. Format code (apply FIRST)
./mvnw spotless:apply

# 2. Verify formatting (check SECOND — must exit 0)
./mvnw spotless:check

# 3. Build and test
./mvnw -pl system clean verify

# 4. Optional: Run specific tests
./mvnw -pl system test -Dtest=YourTestClass
```

**If any checks fail, do not commit.** Fix the issues and re-run.

### Documentation Requirements

**Update documentation when:**

- Adding new services or subsystems
- Refactoring significant components
- Discovering undocumented patterns
- Making architectural changes

**Files to update:**

- `system/README.md` – Module overview
- `system/src/site/markdown/overview.md` – Structural details
- `system/src/site/markdown/services.md` – If adding services
- Class/method JavaDoc – For public APIs
- This file (AGENTS.md) – If adding new rules

### Backward Compatibility

**CRITICAL:** All changes to public APIs must maintain backward compatibility.

- ✅ Keep existing method signatures unchanged
- ✅ Preserve public class names and locations
- ✅ Don't remove public methods (deprecate instead if needed)
- ✅ Document breaking changes prominently (if unavoidable)

**Example: Deprecation**

```java
@Deprecated(since = "8.2.0", forRemoval = false)
public OldDataType oldMethod() {
    // Delegate to new method, converting return type
    return convertToOld(newMethod());
}

public NewDataType newMethod() {
    // New implementation
}
```

## File Organization

### Adding Java Code

**Source files:**

- Active code goes in `services/`, `business/`, `servlet/`, `uploader/`, `agenthandler/`, `src/main/java/`
- Never add code to `Testing/`, `Tools/`, `Docs/`, `Samples/`, `Legacy/` unless specifically maintaining
- Follow existing package hierarchy

**Test files:**

- Mirror source structure in `src/test/java/`
- Name: `<ClassName>Test.java`
- Use JUnit 5 annotations

### Configuration Files

- Server config: `system/config/config.xml`
- Application definitions: `system/applications/`
- Workflow: `system/workflow/`
- Schemas: `system/design/schemas/`

### Documentation Files

- Module overview: `system/README.md` (this is the canonical reference)
- Maven site docs: `system/src/site/markdown/`
- Javadoc comments: In source code for all public APIs

## Module Dependencies

### What System Depends On

- `perc-security-acl-shim` – ACL and security abstractions
- `Hibernate 7.2.6` – ORM
- `ByteBuddy 1.17.7` – Dynamic class generation (JDK 21 compatible)
- Apache Commons, Jakarta EE, JSON libraries

### What Depends on System

- Almost everything else (system is the core)
- **Be very careful breaking public APIs** – Affects all dependent modules

## Common Tasks

### Task: Add a New Service

1. Read services.md: Service Layer Pattern
2. Create three files:
   - Interface: `services/src/main/java/com/percussion/services/xxx/IPSXxxService.java`
   - Implementation: `services/src/main/java/com/percussion/services/xxx/PSXxxService.java`
   - Locator: `services/src/main/java/com/percussion/services/xxx/PSXxxServiceLocator.java`
3. Add tests in `services/src/test/java/com/percussion/services/xxx/PSXxxServiceTest.java`
4. Update `README.md` and `src/site/markdown/services.md`
5. Build and verify: `./mvnw clean verify`

### Task: Fix Bug in Service

1. Identify the service (use grep or IDE navigation)
2. Create/update test case to reproduce the bug
3. Fix the implementation
4. Verify test passes: `./mvnw test -Dtest=PSXxxServiceTest`
5. Run full build: `./mvnw clean verify`
6. Format code: `./mvnw spotless:apply`

### Task: Refactor Legacy Code

1. Confirm the package is still on pre-modern patterns (read the code; there is no package tracking list).
2. Modernize for **JDK 21**:
   - Replace raw types with generics
   - Use `var` / `Optional` / Streams where they improve clarity
   - Prefer modern APIs available on 21; avoid deprecated APIs
   - Apply Google Java Style (`spotless:apply`)
3. Add/update unit tests (JUnit 5)
4. Build and verify: `./mvnw -pl system clean verify`

### Task: Improve Performance

1. Identify bottleneck (use profiler)
2. Consider:
   - Caching strategy (Hibernate second-level cache)
   - Batch operations (if loading many objects)
   - Index optimization (database level)
   - Query optimization (use JPQL projections)
3. Add performance test to measure improvement
4. Build and verify that tests pass

## When You're Stuck

**If you encounter issues:**

1. **Build fails** → Run `./mvnw -X clean compile` for detailed output
2. **Test fails** → Run test with `-e` flag: `./mvnw test -Dtest=Xyz -e`
3. **Formatting issues** → Run `./mvnw spotless:apply`
4. **Dependency conflicts** → Check tree: `./mvnw dependency:tree`
5. **Documentation unclear** → Update it and submit PR with improvements

**Then check:**

- Stack traces in build output
- Unit test implementation details
- Related code in same package
- Service locator patterns (see services.md)

## Key Documentation Links

- **[README.md](README.md)** – Complete module overview (START HERE)
- **[src/site/markdown/overview.md](src/site/markdown/overview.md)** – Detailed structural guide
- **[src/site/markdown/services.md](src/site/markdown/services.md)** – Service architecture and patterns
- **[src/site/markdown/building.md](src/site/markdown/building.md)** – Build and test workflow
- **[Parent AGENTS.md](../../AGENTS.md)** – Project-wide guidelines

## Quick Reference

|       Task        |               Command               |
|-------------------|-------------------------------------|
| Build module      | `./mvnw -pl system compile`         |
| Run tests         | `./mvnw -pl system test`            |
| Format code       | `./mvnw spotless:apply`             |
| Check formatting  | `./mvnw spotless:check`             |
| Full build        | `./mvnw -pl system clean verify`    |
| View dependencies | `./mvnw -pl system dependency:tree` |

## Checklist Before Submitting Work

- [ ] Read README.md in full
- [ ] Read relevant documentation (overview.md, services.md, or building.md)
- [ ] Code targets JDK 21 and follows Google Java Style
- [ ] All unit tests written (85%+ coverage)
- [ ] `./mvnw spotless:apply` ran successfully
- [ ] `./mvnw spotless:check` passes
- [ ] `./mvnw -pl system clean verify` passes all builds and tests
- [ ] Documentation updated (README, site docs, Javadoc)
- [ ] Backward compatibility maintained or documented
- [ ] Tracking files updated (if refactoring legacy code)

---

**Module Version:** 8.2.0-SNAPSHOT | **Last Updated:** March 2026

**Questions?** Refer to the comprehensive documentation in `src/site/markdown/` or README.md
