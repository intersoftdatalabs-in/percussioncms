# Tech

Concise reference for technologies, development setup, constraints, dependencies, and tool usage patterns across the Percussion CMS monorepo.

## Technologies Used
- Language and Runtime
  - Java 17 target.
- Build System
  - Maven multi-module reactor with centralized dependencyManagement and pluginManagement in parent pom.xml
- Frameworks and Libraries
  - Spring, Hibernate, Apache CXF 3.5.11 for delivery-tier JAX-RS
  - Axis 1.4 for legacy SOAP services
  - OWASP ESAPI, AntiSamy for security utilities
  - Apache Commons stack, SLF4J + Log4j
  - JUnit 5 testing stack with Mockito for mocking
- Packaging and Deployment
  - DTS tomcat target; servlet APIs provided-scope in webapps

## Development Setup
- JDK: 11
- Maven: Use -U periodically to refresh metadata during migration
- IDE: IntelliJ or Eclipse; ensure Maven reimport when dependencyManagement changes
- Recommended Maven commands
  - mvn -U -DskipTests -pl <module> -am validate
  - mvn -U -DskipTests -pl <module> -am test
  - mvn -U -DskipTests -rf :artifactId validate  (resume from failure)

## Technical Constraints
- Externalize JAXB and Activation for Java 17
- Axis versions must be resolvable from Maven Central (1.4); avoid 1.4.1/1.4.2
- Minimize legacy transitive drag via selective exclusions (e.g., commons-discovery Axis transitives)
- Avoid bundling servlet APIs in WARs; use provided scope

## Dependencies and Version Management
- Centralized version pins in parent pom.xml for:
  - JUnit 5, junit-platform-launcher
  - Apache CXF (3.5.11)
  - JAXB API and runtime, Activation API
  - Commons stack: commons-io, commons-collections4, commons-logging
  - SLF4J API, Log4j
  - Oracle JDBC: com.oracle.database.jdbc:ojdbc8 (runtime as needed)
- Enforcer Rules
  - RequireUpperBoundDeps enabled to surface version drifts
  - Address via parent pins and selective exclusions rather than per-module overrides
- Known resolution hotspots
  - junit-platform-launcher occasional Central POM glitch: use 5.13.1 pin if 5.13.3 fails
  - commons-collections4 milestone 4.5.0-M2 vs 4.4 transitives; prefer stable where possible
  - xmlgraphics-commons version drift via Batik chain

## Testing
- Primary: JUnit 5 Jupiter
- Legacy: JUnit 4 and JMock must be refactored to Mockito
- Use @ExtendWith(MockitoExtension.class) for Mockito tests
- Use @Mock and @InjectMocks annotations for mocking dependencies
- Use @BeforeEach, @AfterEach for setup/teardown
- Maven Surefire
  - Standardize at latest version in pluginManagement
  - Ensure junit-platform dependencies resolve consistently
- Shared resources
  - perc-shared-test-resources module for fixtures/utilities

## Tool Usage Patterns
- dependency:tree to diagnose transitive conflicts
- -U to refresh artifact metadata during migration sweeps
- -pl ... -am to run focused reactors around a target module
- -rf :artifactId to resume after failure
- Axistools/wsdl2java plugins aligned to resolvable Axis versions (1.4)

## CI/CD Guidelines
- Build with JDK 11; run unit tests by default
- Gate proprietary dependencies (smartgwt, tinymce, caja, perc-jetty-jars) with profile or documentation on credentialed repos
- Delivery-tier artifacts packaged for DTS tomcat; no servlet API bundling
- Future: enable full CI for all non-proprietary modules; smoke deploy to DTS tomcat with basic regression suite

## Security
- OWASP-aligned practices; ESAPI and AntiSamy managed centrally
- Keep dependencies current and consistent; resolve UBD warnings promptly
- Add  jakarta.* alternatives for javax.* APIs as needed.
- Do not remove methods that are tagged as @Deprecated without consulting with the team.

# Source Code License Header and Copyright
All source files must include the standard Percussion CMS license header at the top.
Ensure copyright years are updated to the current year, 2025 in the example below.
This is important for compliance and clarity on ownership and licensing terms.

The license header should look like this:

```java
/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

---
applyTo: "**/*.java"
---
# GitHub Copilot Instructions for Java 17 Features and Best Practices

These instructions guide GitHub Copilot in generating Java 17 code for the PercussionCMS project, emphasizing modern features and best practices.

## Java 17 Features
1. **Local Variable Syntax for Lambda Parameters (`var` in Lambdas)**:
   - Use `var` in lambda expressions to improve readability when type inference is clear.
   - Example: `(var id, var name) -> id + ": " + name` instead of `(String id, String name) -> id + ": " + name`.
   - Ensure type inference does not obscure intent; avoid overuse in complex lambdas.

2. **String API Enhancements**:
   - Utilize new String methods: `isBlank()`, `lines()`, `strip()`, `stripLeading()`, `stripTrailing()`, `repeat(int)`.
   - Example: Use `content.isBlank()` instead of `content.trim().isEmpty()` for checking empty or whitespace-only strings.
   - Prefer `strip()` over `trim()` for Unicode-aware whitespace removal.

3. **Collection API Enhancements**:
   - Use `List.of()`, `Set.of()`, `Map.of()`, and `Map.ofEntries()` for immutable collections.
   - Example: `List<String> items = List.of("item1", "item2");` for concise, unmodifiable lists.
   - Avoid modifying collections created with these methods to prevent `UnsupportedOperationException`.

4. **Optional Enhancements**:
   - Leverage `Optional.orElseThrow()` for cleaner exception handling.
   - Example: `user.orElseThrow(() -> new UserNotFoundException("User not found"));`.
   - Use `Optional.isEmpty()` for explicit empty checks instead of `!isPresent()`.

5. **HTTP Client API**:
   - Use the standard `java.net.http.HttpClient` for HTTP requests instead of third-party libraries.
   - Example:
     ```java
     HttpClient client = HttpClient.newHttpClient();
     HttpRequest request = HttpRequest.newBuilder()
         .uri(URI.create("https://api.example.com"))
         .build();
     HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
     ```
   - Prefer asynchronous requests with `sendAsync` for non-blocking operations in CMS API calls.

6. **Nest-Based Access Control**:
   - Understand that Java 17 allows nested classes to access private members of enclosing classes without synthetic methods.
   - Use this for cleaner encapsulation in nested CMS component classes.

7. **File API Enhancements**:
   - Use `Files.writeString()` and `Files.readString()` for simplified file operations.
   - Example: `Files.writeString(Path.of("config.txt"), "content");` for writing to files.
   - Ensure proper exception handling for I/O operations.

## Best Practices
1. **Code Readability and Maintainability**:
   - Prioritize clear, self-documenting code. Use meaningful variable and method names.
   - Example: `fetchContentById` instead of `getData`.
   - Keep methods short and focused on a single responsibility.

2. **Null Safety**:
   - Minimize null usage; prefer `Optional` for optional values.
   - Example: `Optional<Content> content = repository.findContent(id);`.
   - Use `@NonNull` and `@Nullable` annotations for clarity in method signatures.

3. **Exception Handling**:
   - Use specific exceptions (e.g., `ContentNotFoundException`) instead of generic `Exception`.
   - Log exceptions with context using a logging framework like SLF4J.
   - Example: `logger.error("Failed to load content with id: {}", id, e);`.

4. **Immutability**:
   - Prefer immutable objects and collections to reduce side effects.
   - Example: Use `record` classes (introduced in Java 14 but backportable as POJOs in Java 17) for data carriers.
     ```java
     public record ContentItem(String id, String title) {}
     ```

5. **Stream API**:
   - Use streams for collection processing but avoid overcomplicating simple operations.
   - Example: `items.stream().filter(item -> item.isActive()).toList();`.
   - Ensure streams are closed when processing large datasets.

6. **Performance**:
   - Avoid unnecessary object creation; reuse objects where possible.
   - Use `StringBuilder` for string concatenation in loops.
   - Profile performance-critical CMS components (e.g., content rendering) using tools like VisualVM.

7. **Security**:
   - Sanitize inputs to prevent injection attacks in CMS content processing.
   - Use `HttpClient` with proper timeout configurations to avoid hanging requests.
   - Example: `HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();`.

8. **Testing**:
   - Write unit tests with JUnit 5, leveraging Java 17 features like `assertDoesNotThrow`.
   - Example:
     ```java
     @Test
     void testContentRetrieval() {
         assertDoesNotThrow(() -> contentService.getContent("id123"));
     }
     ```
   - Aim for high test coverage of CMS business logic.

9. **Code Style**:
   - Follow consistent formatting using a tool like Checkstyle or Spotless.
   - Example configuration for Spotless in `pom.xml`:
     ```xml
     <plugin>
         <groupId>com.diffplug.spotless</groupId>
         <artifactId>spotless-maven-plugin</artifactId>
         <version>2.27.2</version>
         <configuration>
             <java>
                 <googleJavaFormat>
                     <version>1.15.0</version>
                     <style>GOOGLE</style>
                 </googleJavaFormat>
             </java>
         </configuration>
     </plugin>
     ```
## Unit Testing
- Use JUnit 5 for unit tests, leveraging its features like `@BeforeEach`, `@AfterEach`, and `@Nested` for better organization.
- Example:
  ```java
  @Nested
  class ContentTests {
      @BeforeEach
      void setup() {
          // Setup code here
      }                             
      @Test
      void testContentCreation() {
          Content content = new Content("id", "title");
          assertNotNull(content);
      }

      @AfterEach
      void cleanup() {
          // Cleanup code here
      }
   }
   ```
- Use `assertThrows` for testing expected exceptions.`
- Use assertions from `org.junit.jupiter.api.Assertions` for clarity.
- Example:
  ```java
  @Test
  void testContentNotFound() {
      assertThrows(ContentNotFoundException.class, () -> contentService.getContent("nonexistent"));
  }
  ```
- Replace deprecated JUnit 4 assertions with JUnit 5 equivalents.
- Replace `@Before` with `@BeforeEach`, `@After` with `@AfterEach`, and `@Test(expected = Exception.class)` with `assertThrows(Exception.class, () -> methodCall())`.
- Replace `@RunWith` with `@ExtendWith` for JUnit 5 extensions.
- Use `@DisplayName` for descriptive test names.
- Replace `@Ignore` with `@Disabled` for skipped tests.
- Use `@ParameterizedTest` for parameterized tests with `@ValueSource`, `@CsvSource`, or custom arguments.
- Use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` for class-level test instances if needed.
- Use `@Mock` and `@InjectMocks` annotations from Mockito for mocking dependencies.
- Example:
  ```java
  @Mock
  private ContentRepository contentRepository;
   @InjectMocks
   private ContentService contentService;
   ```   
- Replace JMock with Mockito in tests.
- Use `MockitoExtension` for JUnit 5 tests to enable Mockito annotations.
- Example:
  ```java
  @ExtendWith(MockitoExtension.class)
  public class ContentServiceTest {    
      @Mock
      private ContentRepository contentRepository;
      @InjectMocks
      private ContentService contentService;

      @Test
      void testGetContent() {
          when(contentRepository.findById("id")).thenReturn(Optional.of(new Content("id", "title")));
          Content content = contentService.getContent("id");
          assertNotNull(content);
      }
  }
  ```
  ## Maven dependencies and plugins

- Remove unused or deprecated plugins from the POM files.
- Remove the JaCoco plugin.
- Remove the Spotbugs plugin.
- Remove the CycloneDX plugin.
- Remove the PMD plugin.
- Remove the Checkstyle plugin.
- Remove the FindBugs plugin.
- Remove the JDepend plugin.
- Remove the JUnit 4 dependencies from the POM files.
- Remove the JMock dependencies from the POM files.
- Remove the JMock plugin from the POM files.
- Remove the JMock annotations from the test classes.
- All modules should use the same version of the Maven Compiler Plugin.
- Ensure all modules use the same version of the Maven Surefire Plugin.
- Ensure all modules use the same version of the Maven Enforcer Plugin.
- Ensure all modules use the same version of the Maven Javadoc Plugin.
- Ensure all modules use the same version of the Maven Assembly Plugin.
- Ensure all modules use the same version of the Maven Dependency Plugin.
- Ensure all modules use the same version of the Maven Resources Plugin.
- Ensure all modules use the same version of the Maven Clean Plugin.
- Ensure all modules use the same version of the Maven Install Plugin.
- Ensure all modules use the same version of the Maven Deploy Plugin.
- Ensure all modules use the same version of the Maven Site Plugin.
- All dependency versions should be managed in the parent POM's dependencyManagement section.
- Dependencies should not be hardcoded in module POMs unless absolutely necessary.
- All plugins should be defined in the parent POM's pluginManagement section.
- All plugin versions should be managed in the parent POM's pluginManagement section.
- Always prefer a stable release over a milestone or snapshot.
- Always prefer a release available in Maven Central over one that is not.
- Always update dependencies to the latest stable version with no known vulnerabilities.

## Logging Framework
- Use SLF4J for logging in all new code.
- Do not use Log4J directly.
- Do not use java.util.logging directly.
- Do not use Apache Commons Logging directly.
- Use parameterized logging to avoid unnecessary string concatenation.
  - Example: `logger.debug("Processing item with id: {}", itemId);`  
   - Avoid: `logger.debug("Processing item with id: " + itemId);`    
- Ensure logging statements do not expose sensitive information.  
   - Example: Avoid logging passwords, credit card numbers, or personal data.
- Use appropriate log levels (TRACE, DEBUG, INFO, WARN, ERROR) based on the importance and verbosity of the message.
- Ensure that exceptions are logged with the exception object to capture stack traces.
  - Example: `logger.error("Failed to process item with id: {}", itemId, e);`
- Avoid using System.out or System.err for logging.
- Configure logging via external configuration files (e.g., logback.xml) rather than hardcoding log levels in code.
- Refactor legacy code to use SLF4J where feasible, especially in new or modified classes.