# Building & Development

This guide covers building the system module, running tests, and development workflow.

## Prerequisites

### Required Software

- **Java Development Kit (JDK) 21** – Required for `spotless` code formatting
  - Download from [Amazon Corretto](https://aws.amazon.com/corretto/) or [Eclipse Adoptium](https://adoptopenjdk.net/)
  - Set environment variable: `export JAVA_HOME=/path/to/jdk21`
- **Maven 3.8+** – For project building
  - Download from [Apache Maven](https://maven.apache.org/)
- **Git** – For version control
  - Already available in most development environments

### Environment Setup

Set the correct Java version for Spotless:

```bash
# Linux/macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk

# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```

Verify your setup:

```bash
java -version
mvn --version
```

## Build Basics

### Build System

The project uses Maven with wrapper scripts to ensure correct JDK versions:

- **Linux/macOS:** `./mvnw [maven-args]`
- **Windows:** `mvnw.cmd [maven-args]`
- **Direct Maven:** `mvn [args]` (use wrapper scripts for Spotless)

### Basic Build Commands

**Compile only (no tests):**

```bash
./mvnw -pl system compile
```

**Compile and package:**

```bash
./mvnw -pl system package -DskipTests
```

**Full build with tests:**

```bash
./mvnw -pl system clean verify
```

**Build specific module:**

```bash
./mvnw -pl system:services compile
```

### Module Organization

The system module contains multiple sub-modules:

- `system/services` – Service infrastructure
- `system/business` – Business logic
- `system/servlet` – Servlet implementations
- `system/beans` – Bean definitions
- `system/uploader` – Upload service
- `system/agenthandler` – Agent support
- `system/webservices` – Web services
- `system/src` – Core CMS

To build a specific module:

```bash
./mvnw -pl system/services compile
```

## Testing

### Running Tests

**Run all tests in system module:**

```bash
./mvnw -pl system test
```

**Run tests for specific submodule:**

```bash
./mvnw -pl system/services test
```

**Run single test class:**

```bash
./mvnw -pl system test -Dtest=PSBeanGeneratorTest
```

**Run single test method:**

```bash
./mvnw -pl system test -Dtest=PSBeanGeneratorTest#testCreateClass_generatesClassWithProperties
```

**Run tests matching pattern:**

```bash
./mvnw -pl system test -Dtest=*ServiceTest
```

**Skip tests during build:**

```bash
./mvnw -pl system clean install -DskipTests
```

### Test Framework

The system module uses:

- **JUnit 5** – Primary test framework
- **JUnit 4** – Legacy support (via JUnit Vintage engine)
- **Mockito** – Mocking framework
- **AssertJ** – Fluent assertions

### Writing Tests

**Modern JUnit 5 test:**

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PSMyServiceTest {

    @Test
    void testOperationSucceeds() {
        // Arrange
        var service = new PSMyService();

        // Act
        var result = service.doSomething();

        // Assert
        assertNotNull(result);
        assertEquals("expected", result);
    }

    @Test
    void testOperationThrowsException() {
        var service = new PSMyService();

        assertThrows(PSMyException.class, () -> {
            service.failOperation();
        });
    }
}
```

**Legacy JUnit 4 test (avoid for new code):**

```java
import org.junit.Test;

public class PSLegacyServiceTest {

    @Test
    public void testOperation() {
        // Old style - still works but prefer JUnit 5
    }
}
```

### Mocking Services

```java
@Test
void testWithMockedService() {
    // Mock the service locator or service directly
    IPSCatalogService mockCatalog = Mockito.mock(IPSCatalogService.class);
    when(mockCatalog.find(any())).thenReturn(Collections.emptyList());

    // Test code that uses the mock
    assertTrue(mockCatalog.find(type).isEmpty());
}
```

### Test Coverage

Aim for high test coverage:

- **Minimum:** 70% line coverage
- **Target:** 85%+ line coverage for critical code
- **Exception:** Legacy code may have lower coverage

Run coverage report:

```bash
./mvnw -pl system test jacoco:report
```

View report in `target/site/jacoco/index.html`

## Code Style & Quality

### Spotless (Automatic Formatting)

The project uses Spotless with Google Java Format for consistent code style:

**Check code style:**

```bash
./mvnw -pl system spotless:check
```

**Auto-format code:**

```bash
./mvnw spotless:apply
```

**Before committing code, always run:**

```bash
./mvnw spotless:apply
./mvnw spotless:check
```

### Code Style Rules

- Google Java Style is the standard
- 4-space indentation (not tabs)
- Line length: 100 characters (where practical)
- One statement per line
- Use `@Override` annotation for overridden methods
- Use modern Java 17 features (var, Optional, Streams, etc.)

### Example: Preferred Style

```java
// ✅ Good - uses var, Optional, Streams
var users = userService.findUsers(criteria)
    .stream()
    .filter(u -> u.isActive())
    .map(u -> new UserDTO(u.getId(), u.getName()))
    .collect(Collectors.toList());

Optional<User> user = userService.findById(id);
user.ifPresent(u -> processUser(u));

// ❌ Avoid - verbose, using raw types
List users = userService.findUsers(criteria);
for (int i = 0; i < users.size(); i++) {
    if (((User)users.get(i)).isActive()) {
        // Process user
    }
}
```

## Complete Development Workflow

### 1. Make your changes

Edit Java source files in your IDE.

### 2. Build locally

```bash
./mvnw -pl system clean verify
```

This runs:
- Compilation
- Unit tests
- Dependency checks
- Packaging

### 3. Run tests

```bash
./mvnw -pl system test -Dtest=YourTestClass
```

Ensure all tests pass before proceeding.

### 4. Check code style

```bash
./mvnw -pl system spotless:check
```

If formatting issues found, proceed to step 5.

### 5. Apply formatting (if needed)

```bash
./mvnw spotless:apply
./mvnw -pl system spotless:check
```

Verify formatting is applied.

### 6. Rebuild and verify

```bash
./mvnw -pl system clean verify
```

### 7. Commit your changes

```bash
git add .
git commit -m "Description of changes"
git push origin branch-name
```

### 8. Create pull request

Create pull request against `development` branch.

## Common Scenarios

### Adding a New Service

1. Create service interface: `com.percussion.services.xxx.IPSXxxService`
2. Create implementation: `com.percussion.services.xxx.PSXxxService`
3. Create locator: `com.percussion.services.xxx.PSXxxServiceLocator`
4. Add comprehensive unit tests in `src/test/java/`
5. Document in this README and service architecture docs
6. Build and test: `./mvnw -pl system clean verify`

### Refactoring Legacy Code

1. Identify the package in `refactored-java11-packages.txt`
2. If not yet refactored:
   - Modernize to Java 17 (var, Optional, Streams)
   - Apply Google Java Style
   - Add/update unit tests
   - Add `// REFACTORED: CP-JAVA11` marker to class
   - Update tracking file
3. Build and verify: `./mvnw clean verify`

### Adding a Test

1. Create test class in `src/test/java/` mirroring source package structure
2. Use JUnit 5 (`org.junit.jupiter.api.*`)
3. Name test class: `<ClassName>Test`
4. Run test: `./mvnw -pl system test -Dtest=YourTestClass`

### Debugging

**Enable debug output:**

```bash
./mvnw -pl system -X clean compile
```

**Debug a test:**

1. Set breakpoint in IDE
2. Right-click test class → Debug As → JUnit Test
3. OR use Maven with debug agent:

```bash
./mvnw -pl system -Ddebug test
```

## Troubleshooting

### Java Version Mismatch

**Error:** `Unsupported class version 61.0`

**Solution:** Ensure JDK 21 is set:

```bash
java -version
export JAVA_HOME=/path/to/jdk21
```

### Spotless Formatting Error

**Error:** `Unable to format file /path/to/file.xml: Encoding error`

**Solution:** This is a pre-existing issue with legacy test resources. Use:

```bash
./mvnw -pl system/services spotless:apply  # Submodule to avoid problematic files
```

### Test Failures

**Check test output:**

```bash
./mvnw -pl system test -Dtest=YourTestClass -e
```

The `-e` flag shows full stack traces.

### Dependency Issues

**Resolve dependency conflicts:**

```bash
./mvnw -pl system dependency:tree
./mvnw -pl system dependency:analyze
```

View dependency tree to identify version conflicts.

## IDE Setup

### IntelliJ IDEA

1. **Open project** – File → Open → Select project root
2. **Import profiles**:
   - Settings → Editor → Code Style → Java → Import Scheme → IntelliCode XML
   - Use `google-java-format.xml` if available
3. **Configure JDK**:
   - Project Structure → Project → JDK → Set to 21
4. **Enable Spotless integration** (optional):
   - Plugins → Search "Spotless" → Install
5. **Run tests** – Right-click test class → Run tests

### Eclipse

1. **Import project** – File → Import → Existing Maven Project
2. **Set JDK**:
   - Preferences → Java → Installed JREs → Add JDK 21
   - Project properties → Java Build Path → Set JRE to 21
3. **Install formatter** (optional):
   - Marketplace → Search "Google Java Format" → Install
4. **Configure Maven**:
   - Preferences → Maven → Installations → Set JDK 21

### VS Code

1. **Install extensions**:
   - Extension Pack for Java (Microsoft)
   - Maven for Java (Microsoft)
   - Spotless (optional)
2. **Configure JDK**:
   - Command Palette → Java: Configure Java Runtime
   - Set to JDK 21
3. **Build** via integrated terminal:
   - `./mvnw clean verify`

## Continuous Integration

When you push to GitHub, the following checks run automatically:

- ✅ Code compilation
- ✅ All unit tests
- ✅ Code coverage checks (Jacoco)
- ✅ Dependency vulnerability scans (Dependabot)
- ✅ Code style validation (Spotless)

**Ensure all local checks pass before pushing:**

```bash
./mvnw -pl system spotless:check
./mvnw -pl system clean verify
```

## Performance Tips

### Faster Builds

```bash
# Skip tests
./mvnw -pl system clean install -DskipTests

# Parallel build
./mvnw -T 1C -pl system clean install

# Offline mode (after first full build)
./mvnw -o -pl system compile
```

### Faster Tests

```bash
# Run tests in parallel
./mvnw -pl system test -P parallel-tests

# Skip integration tests (run only unit tests)
./mvnw -pl system test -DskipITs
```

## Documentation

Always update module documentation when:

- Adding new subsystems or services
- Making significant architectural changes
- Refactoring major components
- Discovering undocumented patterns

Update these files:

- `README.md` – Module-level overview
- `src/site/markdown/*.md` – Technical documentation
- `AGENTS.md` – Agent guidelines
- Class/method JavaDoc – For public APIs

---

**Module Version:** 8.2.0-SNAPSHOT | **Last Updated:** March 2026
