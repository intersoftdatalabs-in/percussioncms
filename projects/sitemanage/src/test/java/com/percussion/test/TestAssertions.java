package com.percussion.test;

/**
 * Compatibility layer for assertions.
 *
 * <p>Tests in the sitemanage module mix JUnit4/5 styles and sometimes put the "message" parameter
 * first or last. JUnit5 only supports message *last* by default, which means legacy code fails to
 * compile. This utility exposes overloads for the most commonly used assertions so that either
 * ordering works transparently. All methods delegate to the corresponding JUnit assertion.
 *
 * <p>To use it simply add
 *
 * <pre>
 * import static com.percussion.test.TestAssertions.*;
 * </pre>
 *
 * at the top of your test class instead of importing directly from <code>
 * org.junit.jupiter.api.Assertions</code> or <code>org.junit.Assert</code>.
 */
public final class TestAssertions {
  private TestAssertions() {
    // utility class
  }

  // ---------- assertEquals overloads ----------
  // two‑arg variants (no message)
  public static <T> void assertEquals(T expected, T actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, (String) null);
  }

  // message-first variant
  public static void assertEquals(String message, Object expected, Object actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
  }

  // message-last variant
  public static <T> void assertEquals(T expected, T actual, String message) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
  }

  // ----- break ties when all three args are strings (expected,last) -----
  public static void assertEquals(String expected, String actual, String message) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
  }

  public static void assertEquals(long expected, long actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, (String) null);
  }

  public static void assertEquals(String message, long expected, long actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
  }

  public static void assertEquals(long expected, long actual, String message) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
  }

  public static void assertEquals(int expected, int actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, (String) null);
  }

  public static void assertEquals(String message, int expected, int actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
  }

  public static void assertEquals(int expected, int actual, String message) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
  }

  public static void assertEquals(double expected, double actual, double epsilon) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, epsilon, (String) null);
  }

  public static void assertEquals(String message, double expected, double actual, double epsilon) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, epsilon, message);
  }

  public static void assertEquals(double expected, double actual, double epsilon, String message) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, epsilon, message);
  }

  public static void assertEquals(float expected, float actual, float epsilon) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, epsilon, (String) null);
  }

  public static void assertEquals(String message, float expected, float actual, float epsilon) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, epsilon, message);
  }

  public static void assertEquals(float expected, float actual, float epsilon, String message) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, epsilon, message);
  }

  public static void assertEquals(boolean expected, boolean actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, (String) null);
  }

  public static void assertEquals(String message, boolean expected, boolean actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
  }

  public static void assertEquals(boolean expected, boolean actual, String message) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
  }

  public static void assertEquals(char expected, char actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, (String) null);
  }

  public static void assertEquals(String message, char expected, char actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
  }

  public static void assertEquals(char expected, char actual, String message) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
  }

  // ---------- assertTrue / assertFalse ----------
  public static void assertTrue(boolean condition) {
    org.junit.jupiter.api.Assertions.assertTrue(condition, (String) null);
  }

  public static void assertTrue(String message, boolean condition) {
    org.junit.jupiter.api.Assertions.assertTrue(condition, message);
  }

  public static void assertTrue(boolean condition, String message) {
    org.junit.jupiter.api.Assertions.assertTrue(condition, message);
  }

  public static void assertFalse(boolean condition) {
    org.junit.jupiter.api.Assertions.assertFalse(condition, (String) null);
  }

  public static void assertFalse(String message, boolean condition) {
    org.junit.jupiter.api.Assertions.assertFalse(condition, message);
  }

  public static void assertFalse(boolean condition, String message) {
    org.junit.jupiter.api.Assertions.assertFalse(condition, message);
  }

  // ---------- assertNotNull / assertNull ----------
  public static void assertNotNull(Object obj) {
    org.junit.jupiter.api.Assertions.assertNotNull(obj, (String) null);
  }

  public static void assertNotNull(String message, Object obj) {
    org.junit.jupiter.api.Assertions.assertNotNull(obj, message);
  }

  public static void assertNotNull(Object obj, String message) {
    org.junit.jupiter.api.Assertions.assertNotNull(obj, message);
  }

  public static void assertNull(Object obj) {
    org.junit.jupiter.api.Assertions.assertNull(obj, (String) null);
  }

  public static void assertNull(String message, Object obj) {
    org.junit.jupiter.api.Assertions.assertNull(obj, message);
  }

  public static void assertNull(Object obj, String message) {
    org.junit.jupiter.api.Assertions.assertNull(obj, message);
  }

  // ---------- other convenience wrappers ----------
  public static void fail(String message) {
    org.junit.jupiter.api.Assertions.fail(message);
  }

  public static <T extends Throwable> T assertThrows(
      Class<T> expectedType, org.junit.jupiter.api.function.Executable executable) {
    return org.junit.jupiter.api.Assertions.assertThrows(expectedType, executable);
  }
}
