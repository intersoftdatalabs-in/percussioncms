---
name: java-unit-testing
description: A skill for writing clean, maintainable, and effective Java unit tests using JUnit 5 and Mockito. Use when the user asks for "unit tests", "test cases", "tests", "JUnit", "Mockito", or related terms.
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

