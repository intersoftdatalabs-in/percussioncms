/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.utils.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test PSScript for performance, backward compatibility, and thread safety with the new JEXL
 * version.
 *
 * @author percussion
 */
public class PSScriptTest {

  /**
   * Product-like domain type outside JEXL 3.x RESTRICTED allowlists ({@code java.lang.*}, {@code
   * java.util.*}, etc.). Methods on this type succeed only when the engine uses {@link
   * JexlPermissions#UNRESTRICTED} (or an equivalent custom allowlist).
   */
  public static class DomainWidget {
    public String echo(String value) {
      return value;
    }

    public String getToken() {
      return "widget-token";
    }
  }

  @BeforeEach
  public void setUp() {
    // Reset to defaults before each test
    PSScript.CACHE_SIZE = 512;
  }

  @AfterEach
  public void tearDown() {
    // Clean up any state
  }

  /**
   * Test that legacy foreach syntax is automatically fixed to for syntax. Tests backward
   * compatibility with old Velocity-like scripts.
   */
  @Test
  public void testLegacyForeachSyntaxFix() {
    // Old velocity-style foreach syntax
    String legacyScript = "foreach ($item in $list) { $result = $item; }";
    PSScript script = new PSScript(legacyScript);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$list", new String[] {"a", "b", "c"});

    // Script should compile and execute despite old syntax
    assertNotNull(script.eval(bindings), "Script should execute without throwing");
  }

  /**
   * Test that legacy assignment spacing (missing space in =$ operator) is fixed. Ensures old
   * scripts that use $ref=$ref2 instead of $ref = $ref2 still work.
   */
  @Test
  public void testLegacyAssignmentSpacingFix() {
    // Missing space before assignment
    String legacyScript = "$a=$b";
    PSScript script = new PSScript(legacyScript);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$b", 42);

    Object result = script.eval(bindings);
    assertEquals(42, result, "Assignment with no space should work after fix");
  }

  /**
   * Test that legacy negation syntax (!$ref) is automatically fixed. Old scripts using !$ instead
   * of ! $ should still parse.
   */
  @Test
  public void testLegacyNegationSpacingFix() {
    // Missing space in negation
    String legacyScript = "if (!$flag) { 1 } else { 0 }";
    PSScript script = new PSScript(legacyScript);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$flag", false);

    Object result = script.eval(bindings);
    assertEquals(1, result, "Negation without space should work after fix");
  }

  /**
   * Test that undefined $ variables return null rather than throwing by default (backward
   * compatibility with strict=false default).
   */
  @Test
  public void testUndefinedVariableHandling() {
    String script = "$undefined";
    PSScript ps = new PSScript(script);

    Map<String, Object> bindings = new HashMap<>();

    // With default strict=false, undefined variables should return null gracefully
    Object result = ps.eval(bindings);
    assertEquals(null, result, "Undefined variable should return null with strict=false");
  }

  /**
   * Test that basic arithmetic and expressions work correctly to establish baseline compatibility.
   */
  @Test
  public void testBasicArithmetic() {
    String script = "$a + $b * $c";
    PSScript ps = new PSScript(script);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$a", 10);
    bindings.put("$b", 5);
    bindings.put("$c", 2);

    Object result = ps.eval(bindings);
    assertEquals(20, result, "Arithmetic expression should evaluate correctly");
  }

  /**
   * Test that compiled scripts are cached per instance and reused, improving performance on
   * repeated evaluations.
   */
  @Test
  public void testScriptCachingPerInstance() throws Exception {
    String script = "$x + 1";
    PSScript ps = new PSScript(script);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$x", 5);

    // First evaluation compiles the script
    Object result1 = ps.eval(bindings);
    assertEquals(6, result1, "First eval should work");

    // Get a reference to the internal compiled script
    java.lang.reflect.Field compiledScriptField = PSScript.class.getDeclaredField("compiledScript");
    compiledScriptField.setAccessible(true);
    Object cachedScript1 = compiledScriptField.get(ps);

    // Second evaluation should reuse the cached compiled script
    Object result2 = ps.eval(bindings);
    assertEquals(6, result2, "Second eval should work");

    Object cachedScript2 = compiledScriptField.get(ps);

    // Verify same instance is reused
    assertTrue(cachedScript1 == cachedScript2, "Compiled script should be cached and reused");
  }

  /**
   * Test that script fixes (regex replacements) are performed only once and cached, improving
   * performance on scripts that need legacy syntax fixes.
   */
  @Test
  public void testScriptFixesCachingPerInstance() throws Exception {
    String legacyScript = "$a=$b"; // Old syntax needing fix
    PSScript ps = new PSScript(legacyScript);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$b", 42);

    // First evaluation applies fixes
    Object result1 = ps.eval(bindings);
    assertEquals(42, result1, "First eval with legacy syntax should work");

    // Get the fixed script text
    java.lang.reflect.Field fixedScriptTextField =
        PSScript.class.getDeclaredField("fixedScriptText");
    fixedScriptTextField.setAccessible(true);
    String cachedFixes1 = (String) fixedScriptTextField.get(ps);

    // Second evaluation should reuse cached fixes
    Object result2 = ps.eval(bindings);
    assertEquals(42, result2, "Second eval should work");

    String cachedFixes2 = (String) fixedScriptTextField.get(ps);

    // Verify same fixed text is reused
    assertEquals(cachedFixes1, cachedFixes2, "Fixed script text should be cached");
    assertTrue(cachedFixes1.contains("="), "Fixed script should contain '='");
  }

  /**
   * Test thread safety: multiple threads evaluating the same script instance concurrently should
   * not cause race conditions or duplicate compilations.
   */
  @Test
  public void testThreadSafetyUnderConcurrentEvaluation() throws Exception {
    String script = "$x * 2";
    PSScript ps = new PSScript(script);

    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      executor.submit(
          () -> {
            try {
              Map<String, Object> bindings = new HashMap<>();
              bindings.put("$x", threadId);
              Object result = ps.eval(bindings);
              assertEquals(threadId * 2, result, "Each thread should get correct result");
            } finally {
              latch.countDown();
            }
          });
    }

    assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
    executor.shutdownNow();
  }

  /**
   * Test owner context is preserved and available for debugging. Verify that setOwnerType and
   * setOwnerName work as expected.
   */
  @Test
  public void testOwnerContextPreservation() {
    String script = "$x + 1";
    PSScript ps = new PSScript(script);

    ps.setOwnerType("Template");
    ps.setOwnerName("MyTemplate");

    assertEquals("Template", ps.getOwnerType(), "Owner type should be set");
    assertEquals("MyTemplate", ps.getOwnerName(), "Owner name should be set");

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$x", 5);

    // Evaluation should work with context preserved
    Object result = ps.eval(bindings);
    assertEquals(6, result, "Evaluation with owner context should work");
  }

  /** Test that string concatenation works correctly for Velocity-style templating scenarios. */
  @Test
  public void testStringConcatenation() {
    String script = "$prefix + ' ' + $value";
    PSScript ps = new PSScript(script);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$prefix", "Hello");
    bindings.put("$value", "World");

    Object result = ps.eval(bindings);
    assertEquals("Hello World", result, "String concatenation should work");
  }

  /** Test conditional execution typical of Velocity templating. */
  @Test
  public void testConditionalExecution() {
    String script = "if ($count > 5) { 'many' } else { 'few' }";
    PSScript ps = new PSScript(script);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$count", 10);

    Object result = ps.eval(bindings);
    assertEquals("many", result, "Conditional should evaluate correctly");
  }

  /**
   * Test that multiple evaluations of the same script instance with different bindings produce
   * correct results (no state leakage).
   */
  @Test
  public void testMultipleEvaluationsWithDifferentBindings() {
    String script = "$a + $b";
    PSScript ps = new PSScript(script);

    Map<String, Object> bindings1 = new HashMap<>();
    bindings1.put("$a", 10);
    bindings1.put("$b", 20);

    Object result1 = ps.eval(bindings1);
    assertEquals(30, result1, "First evaluation");

    Map<String, Object> bindings2 = new HashMap<>();
    bindings2.put("$a", 5);
    bindings2.put("$b", 15);

    Object result2 = ps.eval(bindings2);
    assertEquals(20, result2, "Second evaluation with different bindings");
  }

  /** Test that method calls on objects work correctly (uberspect integration). */
  @Test
  public void testMethodInvocation() {
    String script = "$obj.length()";
    PSScript ps = new PSScript(script);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$obj", "hello");

    Object result = ps.eval(bindings);
    assertEquals(5, result, "Method invocation should work");
  }

  /** Test array access syntax. */
  @Test
  public void testArrayAccess() {
    String script = "$arr[1]";
    PSScript ps = new PSScript(script);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$arr", new int[] {10, 20, 30});

    Object result = ps.eval(bindings);
    assertEquals(20, result, "Array access should work");
  }

  /** Test map property access using dot notation. */
  @Test
  public void testMapPropertyAccess() {
    String script = "$map.key";
    PSScript ps = new PSScript(script);

    Map<String, Object> bindings = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put("key", "value");
    bindings.put("$map", innerMap);

    Object result = ps.eval(bindings);
    assertEquals("value", result, "Map property access should work");
  }

  /** Test that script source text is retrievable for logging/debugging purposes. */
  @Test
  public void testSourceTextRetrieval() {
    String scriptText = "$x + $y";
    PSScript ps = new PSScript(scriptText);

    assertEquals(scriptText, ps.getSourceText(), "Source text should match input");
    assertEquals(scriptText, ps.getScriptText(), "Script text should match input");
  }

  /** Test that getUseStrictMode(), setUseStrictMode(), and related methods work correctly. */
  @Test
  public void testStrictModeConfiguration() {
    PSScript ps = new PSScript("$x");

    // Default should be non-strict for backward compatibility
    assertEquals(false, ps.getUseStrictMode(), "Default strict mode should be false");

    // Setting strict mode should be reflected
    ps.setUseStrictMode(true);
    assertEquals(true, ps.getUseStrictMode(), "Strict mode should be true after setting");
  }

  /** Test that silent mode configuration works. */
  @Test
  public void testSilentModeConfiguration() {
    PSScript ps = new PSScript("$x");

    // Default should be non-silent
    assertEquals(false, ps.getSilentMode(), "Default silent mode should be false");

    ps.setUseSilentMode(true);
    assertEquals(true, ps.getSilentMode(), "Silent mode should be true after setting");
  }

  /** Test that debug mode configuration works. */
  @Test
  public void testDebugModeConfiguration() {
    PSScript ps = new PSScript("$x");

    // Default should be non-debug
    assertEquals(false, ps.getUseDebugMode(), "Default debug mode should be false");

    ps.setUseDebugMode(true);
    assertEquals(true, ps.getUseDebugMode(), "Debug mode should be true after setting");
  }

  /** Test toString() for debugging. */
  @Test
  public void testToString() {
    String scriptText = "$x + 1";
    PSScript ps = new PSScript(scriptText);
    ps.setOwnerType("Widget");
    ps.setOwnerName("TestWidget");

    String str = ps.toString();
    assertTrue(str.contains(scriptText), "toString should contain script text");
    assertTrue(str.contains("Widget"), "toString should contain owner type");
    assertTrue(str.contains("TestWidget"), "toString should contain owner name");
  }

  /** Test that null/empty owner type/name are handled gracefully. */
  @Test
  public void testNullOwnerContextHandling() {
    PSScript ps = new PSScript("$x");

    ps.setOwnerType(null);
    ps.setOwnerName(null);

    assertEquals("", ps.getOwnerType(), "Null owner type should become empty");
    assertEquals("", ps.getOwnerName(), "Null owner name should become empty");

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$x", 5);

    Object result = ps.eval(bindings);
    assertEquals(5, result, "Evaluation should work with null owner context");
  }

  /** Test whitespace handling in owner type/name (should be trimmed). */
  @Test
  public void testOwnerContextTrimming() {
    PSScript ps = new PSScript("$x");

    ps.setOwnerType("  Template  ");
    ps.setOwnerName("  MyTemplate  ");

    assertEquals("Template", ps.getOwnerType(), "Owner type should be trimmed");
    assertEquals("MyTemplate", ps.getOwnerName(), "Owner name should be trimmed");
  }

  /**
   * Test that getSourceText() and getParsedText() return consistent results (before and after
   * fixes).
   */
  @Test
  public void testSourceAndParsedTextConsistency() {
    String scriptText = "$x + $y";
    PSScript ps = new PSScript(scriptText);

    // Before evaluation
    assertEquals(scriptText, ps.getSourceText(), "getSourceText should match script");
    assertEquals(scriptText, ps.getParsedText(), "getParsedText should match script");

    // After evaluation
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$x", 1);
    bindings.put("$y", 2);
    ps.eval(bindings);

    assertEquals(scriptText, ps.getSourceText(), "getSourceText should still match original");
    assertEquals(scriptText, ps.getParsedText(), "getParsedText should still match original");
  }

  /** Test that long-running evaluation does not cause issues. */
  @Test
  public void testLongRunningEvaluation() {
    // Simple loop that runs a moderate number of times
    String script = "var sum = 0; for (var i = 0; i < 100; i = i + 1) { sum = sum + i; } sum";
    PSScript ps = new PSScript(script);

    Map<String, Object> bindings = new HashMap<>();

    Object result = ps.eval(bindings);
    assertEquals(4950, result, "Loop evaluation should complete and return correct result");
  }

  /** Test that nested property access works correctly. */
  @Test
  public void testNestedPropertyAccess() {
    String script = "$obj.inner.value";
    PSScript ps = new PSScript(script);

    Map<String, Object> bindings = new HashMap<>();
    Map<String, Object> inner = new HashMap<>();
    inner.put("value", "nested");
    Map<String, Object> outer = new HashMap<>();
    outer.put("inner", inner);
    bindings.put("$obj", outer);

    Object result = ps.eval(bindings);
    assertEquals("nested", result, "Nested property access should work");
  }

  /** Test isCompilable() behavior. */
  @Test
  public void testIsCompilable() {
    PSScript ps = new PSScript("$x");

    // Default should be false (scripts are compiled lazily on first eval)
    assertEquals(false, ps.isCompilable(), "isCompilable should default to false");
  }

  /**
   * Test documented capabilities based on PSPredefinedJexlVariableDefs.properties. Verifies that
   * standard system variables and their properties can be accessed.
   */
  @Test
  public void testDocumentedCapabilities() {
    // Mock objects simulating the system environment
    Map<String, Object> sys = new HashMap<>();
    Map<String, Object> site = new HashMap<>();
    site.put("id", "101");
    site.put("url", "http://example.com");
    sys.put("site", site);
    sys.put("activeAssembly", true);

    Map<String, Object> params = new HashMap<>();
    params.put("sys_contentid", "999");
    sys.put("params", params);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$sys", sys);

    // Test $sys.site.id
    PSScript script1 = new PSScript("$sys.site.id");
    assertEquals("101", script1.eval(bindings), "Should access nested map properties");

    // Test $sys.activeAssembly
    PSScript script2 = new PSScript("$sys.activeAssembly");
    assertEquals(true, script2.eval(bindings), "Should access boolean property");

    // Test $sys.params.sys_contentid
    PSScript script3 = new PSScript("$sys.params.sys_contentid");
    assertEquals("999", script3.eval(bindings), "Should access params map");
    // Test map syntax
    PSScript script4 = new PSScript("$sys.params['sys_contentid']");
    assertEquals("999", script4.eval(bindings), "Should access params map with bracket syntax");
  }

  /** Test JEXL 'empty' operator which is commonly used in templates. */
  @Test
  public void testEmptyOperator() {
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$emptyList", new java.util.ArrayList<>());
    bindings.put("$nullVar", null);
    bindings.put("$string", "");
    bindings.put("$notEmpty", "foo");

    PSScript script1 = new PSScript("empty $emptyList");
    assertEquals(true, script1.eval(bindings), "Empty list should be empty");

    PSScript script2 = new PSScript("empty $nullVar");
    assertEquals(true, script2.eval(bindings), "Null var should be empty");

    PSScript script3 = new PSScript("empty $string");
    assertEquals(true, script3.eval(bindings), "Empty string should be empty");

    PSScript script4 = new PSScript("empty $notEmpty");
    assertEquals(false, script4.eval(bindings), "Non-empty string should not be empty");
  }

  /**
   * Concurrent first-compile smoke: many threads hit an uncompiled instance together. Double-checked
   * locking must publish a single usable compiled script without races.
   */
  @Test
  public void testConcurrentFirstCompile() throws Exception {
    String script = "$x + 1";
    PSScript ps = new PSScript(script);

    int threadCount = 16;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threadCount);
    java.util.concurrent.atomic.AtomicReference<Throwable> error =
        new java.util.concurrent.atomic.AtomicReference<>();

    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      executor.submit(
          () -> {
            try {
              start.await();
              Map<String, Object> bindings = new HashMap<>();
              bindings.put("$x", threadId);
              Object result = ps.eval(bindings);
              assertEquals(threadId + 1, result, "Concurrent first compile must eval correctly");
            } catch (Throwable t) {
              error.compareAndSet(null, t);
            } finally {
              done.countDown();
            }
          });
    }

    start.countDown();
    assertTrue(done.await(15, TimeUnit.SECONDS), "All concurrent first-compile threads should finish");
    executor.shutdownNow();
    if (error.get() != null) {
      throw new AssertionError("Concurrent first compile failed", error.get());
    }

    java.lang.reflect.Field compiledScriptField = PSScript.class.getDeclaredField("compiledScript");
    compiledScriptField.setAccessible(true);
    assertNotNull(compiledScriptField.get(ps), "Compiled script should be published after concurrent eval");
  }

  /**
   * Core hardening proof: product domain types are outside JEXL RESTRICTED allowlists. {@link
   * PSScript} must use {@link JexlPermissions#UNRESTRICTED} so CMS template/widget scripts can call
   * ordinary methods on domain objects (as they did under JEXL 2.x / early 3.x).
   *
   * <p>Control: the same script against a RESTRICTED engine does not return the domain result
   * (returns null under non-strict silent-ish evaluation). If {@code UNRESTRICTED} were dropped
   * from {@code PSScript}, this test would fail.
   */
  @Test
  public void testUnrestrictedPermissionsAllowDomainTypeMethods() {
    DomainWidget widget = new DomainWidget();
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$widget", widget);

    PSScript echoScript = new PSScript("$widget.echo('ok')");
    assertEquals(
        "ok",
        echoScript.eval(bindings),
        "Domain method echo must work under PSScript (UNRESTRICTED)");

    PSScript tokenScript = new PSScript("$widget.getToken()");
    assertEquals(
        "widget-token",
        tokenScript.eval(bindings),
        "Domain method getToken must work under PSScript (UNRESTRICTED)");

    // Control: RESTRICTED blocks non-allowlisted domain types (returns null, not the method result)
    JexlEngine restricted =
        new JexlBuilder()
            .permissions(JexlPermissions.RESTRICTED)
            .strict(false)
            .safe(true)
            .create();
    MapContext restrictedCtx = new MapContext(bindings);
    Object restrictedResult =
        restricted.createScript("$widget.echo('ok')").execute(restrictedCtx);
    assertNotEquals(
        "ok",
        restrictedResult,
        "RESTRICTED must not expose DomainWidget methods — proves the UNRESTRICTED requirement");
  }

  /**
   * JDK types remain usable (sanity for common template patterns). Alone these do not prove
   * UNRESTRICTED; see {@link #testUnrestrictedPermissionsAllowDomainTypeMethods()}.
   */
  @Test
  public void testJdkMethodCallsStillWork() {
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$text", "percussion");
    bindings.put("$list", new java.util.ArrayList<>(java.util.Arrays.asList("a", "b", "c")));

    PSScript lengthScript = new PSScript("$text.length()");
    assertEquals(10, lengthScript.eval(bindings), "String.length should be allowed");

    PSScript sizeScript = new PSScript("$list.size()");
    assertEquals(3, sizeScript.eval(bindings), "List.size should be allowed");
  }

  /**
   * When strict is off, safe navigation should return null for missing intermediate properties
   * rather than throwing (legacy template behavior; {@code .safe(true)} when not strict).
   */
  @Test
  public void testSafeModeNullPropertyNavigation() {
    PSScript ps = new PSScript("$obj.missing.prop");
    ps.setUseStrictMode(false);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$obj", new HashMap<String, Object>());

    Object result = ps.eval(bindings);
    assertEquals(null, result, "Null intermediate property should yield null when not strict/safe");
  }

  /**
   * When strict is on, {@code .safe(false)} is wired: null intermediate property navigation must
   * fail rather than return null. Uses per-instance mode so a dedicated engine is built with
   * strict=true.
   */
  @Test
  public void testStrictModeRejectsNullPropertyNavigation() {
    PSScript ps = new PSScript("$obj.missing.prop");
    ps.setUseStrictMode(true);
    ps.setUseSilentMode(false);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$obj", new HashMap<String, Object>());

    // PSScript also throws RuntimeException for top-level $expr that returns null in strict mode;
    // either JexlException or that wrapper proves safe/strict pairing is active.
    assertThrows(
        RuntimeException.class,
        () -> ps.eval(bindings),
        "Strict mode with safe=false must not silently return null for missing intermediate props");
  }

  /**
   * new(...) construction used by some legacy extension scripts must remain available under
   * unrestricted permissions.
   */
  @Test
  public void testNewInstanceConstructionAllowed() {
    PSScript ps = new PSScript("new('java.util.ArrayList')");
    Map<String, Object> bindings = new HashMap<>();
    Object result = ps.eval(bindings);
    assertNotNull(result, "new(ArrayList) should succeed with unrestricted permissions");
    assertTrue(result instanceof java.util.ArrayList, "Result should be an ArrayList instance");
  }

  /**
   * Per-instance engine path (modes diverge from global defaults) must also use UNRESTRICTED so
   * domain methods work — not only JDK String methods.
   */
  @Test
  public void testPerInstanceEngineAllowsDomainTypeMethods() {
    PSScript ps = new PSScript("$widget.echo('per-instance')");
    // Force per-instance builder (defaults are non-strict; enable silent to diverge)
    ps.setUseSilentMode(true);

    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$widget", new DomainWidget());

    Object result = ps.eval(bindings);
    assertEquals(
        "per-instance",
        result,
        "Per-instance engine must allow domain method invocation via UNRESTRICTED");
  }
}
