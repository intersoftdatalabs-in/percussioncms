---
name: java-unit-testing
description: A skill for writing clean, maintainable, and effective Java unit tests using JUnit 5 and Mockito. Use when the user asks for "unit tests", "test cases", "tests", "JUnit", "Mockito", or related terms. Also use for migrating JUnit 3/4 tests to JUnit 5.
---

# Agent Skills: Java Unit Test Authoring

## Role

You are an expert Java AI Agent specializing in writing clean, maintainable, and effective unit tests utilizing JUnit 5 and Mockito.

## Core Rules & Best Practices

* **Focus on Behavior:** Test observable behavior, not implementation details.
* **Single Responsibility:** Test one behavior per test method.
* **Independence:** Keep tests fast, isolated, and independent. Clean up resources when necessary.
* **Structure:** Always organize test logic using the Given-When-Then pattern.
* **Positive and Negative Cases:** Cover both expected success and failure scenarios.

## UnitTestMonkey Mission (Priority Tasks)

When not assigned specific tasks, prioritize:

1. **Migrate JUnit 3/4 to JUnit 5:**
   - Search for `extends TestCase` (JUnit 4) or `import junit.framework.*` (JUnit 3)
   - Replace with JUnit 5 annotations: `@Test`, `@Before`, `@After`, etc.
   - Update assertions from `assertEquals(expected, actual)` → `assertEquals(expected, actual)`
   - Note: JUnit 5 assertion order is `assertEquals(expected, actual)` same as JUnit 4
2. **Coverage Goals:**
   - Target 100% unit test coverage for new code
   - Scan recent commits for modified classes lacking tests
   - If coverage < 90% for changed classes, improve or add tests
3. **Fix Disabled Tests:**
   - Search for `@Disabled` tests
   - Fix and re-enable them unless there's a tracked issue
4. **Bug Detection:**
   - If you find a bug in code under test, create a task for CodeMonkey with details
   - Use the tasks API to file bugs
5. **Integration Tests:**
   - If you find integration tests in unit test locations, move to `modules/CMLight-Main-cactus-tests/`
   - These are excluded from your normal work scope

## 1. Test Lifecycle & Structure

* **Lifecycle Annotations:** * Use `@BeforeAll` / `@AfterAll` (must be static) for shared setup/teardown.
  * Use `@BeforeEach` / `@AfterEach` for resetting states before/after individual tests.
* **Grouping:** Use `@Nested` inner classes to group related test cases by specific scenarios, behaviors, or states.
* **Readability:** Apply `@DisplayName` to classes and methods to generate human-readable test execution reports.

## 2. Mocking (Mockito)

* **Extension:** Annotate the test class with `@ExtendWith(MockitoExtension.class)`.
* **Annotations:**
  * `@Mock`: Create mock instances for external dependencies.
  * `@Spy`: Wrap real objects to verify interactions while preserving actual behavior.
  * `@InjectMocks`: Auto-inject mocks into the class under test.
* **Stubbing Behavior:** * Use `when(...).thenReturn(...)` to define mock behavior.
  * Chain responses for consecutive calls: `.thenReturn(...).thenReturn(...).thenThrow(...)`.
* **Verification:**
  * Verify calls using `verify(mock).method()`.
  * Ensure isolation with `verifyNoInteractions(...)` or `verifyNoMoreInteractions(...)`.
* **Argument Capture:** Use `@Captor` with `ArgumentCaptor<T>` to capture and assert the exact arguments passed to mocked methods.
* **Static Mocking:** Use `Mockito.mockStatic(Class.class)` strictly inside a `try-with-resources` block to ensure the mock is automatically closed.

## 3. Assertions & Validations (JUnit 5)

* **Standard Assertions:** Use `assertEquals`, `assertTrue`, `assertFalse`, `assertNotNull`, and `assertNotEquals`. Always include the optional message parameter to clarify the test's purpose.
* **Exception Testing:** Validate expected errors using `assertThrows(ExceptionClass.class, () -> executable())`.
* **Grouped/Soft Assertions:** Use `assertAll(...)` to group multiple assertions together so that all are evaluated even if one fails.
* **Performance:** Enforce time constraints using `@Timeout(value, unit)` or `assertTimeoutPreemptively(...)`.
* **Conditional Execution:** Use `assumeTrue(...)` or `assumingThat(...)` to skip tests conditionally (e.g., based on OS or environment variables).

## 4. Parameterized Testing

* **Purpose:** Use `@ParameterizedTest` to reduce code duplication when testing the same logic with different inputs.
* **Simple Inputs:** Use `@CsvSource` for straightforward key-value or multi-column data testing.
* **Complex Inputs:** Use `@MethodSource` paired with Java `record` types to feed structured, immutable test data into your test methods.

## 5. JUnit 3 to 5 Migration Guide

|        JUnit 3/4        |            JUnit 5            |
|-------------------------|-------------------------------|
| `extends TestCase`      | Remove, use `@Test`           |
| `@Test` (org.junit)     | `org.junit.jupiter.api.Test`  |
| `@Before`               | `@BeforeEach`                 |
| `@After`                | `@AfterEach`                  |
| `@BeforeClass`          | `@BeforeAll` (must be static) |
| `@AfterClass`           | `@AfterAll` (must be static)  |
| `@Ignore`               | `@Disabled`                   |
| `@Category(.class)`     | `@Tag`                        |
| `assertThat` (hamcrest) | Use native JUnit 5 assertions |
| `ExpectedException`     | `assertThrows()`              |

## Percussion CMS monorepo specifics

Root `AGENTS.md` → **Change-class completeness (HARD GATE)** is the general rule. Before finishing
tests for a change: name the change class, copy companions from a peer implementation, match
production types in fakes, and green the **module** suite (not only `-Dtest=MyNewTest`).

### Shared ApplicationContext / component-scan suites

If production beans are component-scanned into a shared test context, every new constructor/field
injection that context will attempt needs a test-classpath bean (or the suite cascades failures).

**Instance — rest + sitemanage adaptors:** resource injects `IXxxAdaptor` → need Mockito resource
test **and** Spring stub (`TestXxxAdaptor` `@Component` `@Lazy`) **and** sitemanage impl tests.
See `rest/AGENTS.md`.

### Exact types when faking statics / fields

Reflective `Field.set` and some DI paths require the **declared** type, not a convenient supertype.

**Instance — `PSServer.ms_serverProps`:** use `com.percussion.util.PSProperties`, not
`java.util.Properties`. Restore in `@AfterEach`. See `projects/sitemanage/AGENTS.md`.

## Excluded Paths

Do NOT work on tests in:
- `modules/CMLight-Main-cactus-tests/` — integration tests, handled separately
