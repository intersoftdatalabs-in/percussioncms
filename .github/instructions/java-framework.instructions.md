---
applyTo: "**/*.java"
---
# GitHub Copilot Instructions for Java 11 Features and Best Practices

These instructions guide GitHub Copilot in generating Java 11 code for the PercussionCMS project, emphasizing modern features and best practices.

## Java 11 Features
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
   - Understand that Java 11 allows nested classes to access private members of enclosing classes without synthetic methods.
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
   - Example: Use `record` classes (introduced in Java 14 but backportable as POJOs in Java 11) for data carriers.
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
   - Write unit tests with JUnit 5, leveraging Java 11 features like `assertDoesNotThrow`.
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
  
## Copilot-Specific Guidance
- Suggest Java 11 features like `var`, `List.of()`, and `HttpClient` when generating code.
- Avoid deprecated APIs (e.g., `java.util.Date`); prefer `java.time` API.
- Example: Use `LocalDateTime.now()` instead of `new Date()`.
- Generate code snippets with proper exception handling and logging.
- When suggesting imports, include only necessary ones to avoid clutter.
- Example: `import java.net.http.HttpClient;` instead of wildcard imports.