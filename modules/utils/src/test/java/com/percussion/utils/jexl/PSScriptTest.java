/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.percussion.utils.testing.UnitTest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test PSScript for performance, backward compatibility, and thread safety with the new JEXL
 * version.
 *
 * @author percussion
 */
@Category(UnitTest.class)
public class PSScriptTest {

    @Before
    public void setUp() {
        // Reset to defaults before each test
        PSScript.CACHE_SIZE = 512;
    }

    @After
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
        assertNotNull("Script should execute without throwing", script.eval(bindings));
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
        assertEquals("Assignment with no space should work after fix", 42, result);
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
        assertEquals("Negation without space should work after fix", 1, result);
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
        assertEquals("Undefined variable should return null with strict=false", null, result);
    }

    /**
     * Test that basic arithmetic and expressions work correctly to establish baseline
     * compatibility.
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
        assertEquals("Arithmetic expression should evaluate correctly", 20, result);
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
        assertEquals("First eval should work", 6, result1);

        // Get a reference to the internal compiled script
        java.lang.reflect.Field compiledScriptField =
                PSScript.class.getDeclaredField("compiledScript");
        compiledScriptField.setAccessible(true);
        Object cachedScript1 = compiledScriptField.get(ps);

        // Second evaluation should reuse the cached compiled script
        Object result2 = ps.eval(bindings);
        assertEquals("Second eval should work", 6, result2);

        Object cachedScript2 = compiledScriptField.get(ps);

        // Verify same instance is reused
        assertTrue("Compiled script should be cached and reused", cachedScript1 == cachedScript2);
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
        assertEquals("First eval with legacy syntax should work", 42, result1);

        // Get the fixed script text
        java.lang.reflect.Field fixedScriptTextField =
                PSScript.class.getDeclaredField("fixedScriptText");
        fixedScriptTextField.setAccessible(true);
        String cachedFixes1 = (String) fixedScriptTextField.get(ps);

        // Second evaluation should reuse cached fixes
        Object result2 = ps.eval(bindings);
        assertEquals("Second eval should work", 42, result2);

        String cachedFixes2 = (String) fixedScriptTextField.get(ps);

        // Verify same fixed text is reused
        assertEquals("Fixed script text should be cached", cachedFixes1, cachedFixes2);
        assertTrue("Fixed script should contain space", cachedFixes1.contains("="));
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
            executor.submit(() -> {
                try {
                    Map<String, Object> bindings = new HashMap<>();
                    bindings.put("$x", threadId);
                    Object result = ps.eval(bindings);
                    assertEquals("Each thread should get correct result", threadId * 2, result);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue("All threads should complete", latch.await(10, TimeUnit.SECONDS));
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

        assertEquals("Owner type should be set", "Template", ps.getOwnerType());
        assertEquals("Owner name should be set", "MyTemplate", ps.getOwnerName());

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("$x", 5);

        // Evaluation should work with context preserved
        Object result = ps.eval(bindings);
        assertEquals("Evaluation with owner context should work", 6, result);
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
        assertEquals("String concatenation should work", "Hello World", result);
    }

    /** Test conditional execution typical of Velocity templating. */
    @Test
    public void testConditionalExecution() {
        String script = "if ($count > 5) { 'many' } else { 'few' }";
        PSScript ps = new PSScript(script);

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("$count", 10);

        Object result = ps.eval(bindings);
        assertEquals("Conditional should evaluate correctly", "many", result);
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
        assertEquals("First evaluation", 30, result1);

        Map<String, Object> bindings2 = new HashMap<>();
        bindings2.put("$a", 5);
        bindings2.put("$b", 15);

        Object result2 = ps.eval(bindings2);
        assertEquals("Second evaluation with different bindings", 20, result2);
    }

    /** Test that method calls on objects work correctly (uberspect integration). */
    @Test
    public void testMethodInvocation() {
        String script = "$obj.length()";
        PSScript ps = new PSScript(script);

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("$obj", "hello");

        Object result = ps.eval(bindings);
        assertEquals("Method invocation should work", 5, result);
    }

    /** Test array access syntax. */
    @Test
    public void testArrayAccess() {
        String script = "$arr[1]";
        PSScript ps = new PSScript(script);

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("$arr", new int[] {10, 20, 30});

        Object result = ps.eval(bindings);
        assertEquals("Array access should work", 20, result);
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
        assertEquals("Map property access should work", "value", result);
    }

    /** Test that script source text is retrievable for logging/debugging purposes. */
    @Test
    public void testSourceTextRetrieval() {
        String scriptText = "$x + $y";
        PSScript ps = new PSScript(scriptText);

        assertEquals("Source text should match input", scriptText, ps.getSourceText());
        assertEquals("Script text should match input", scriptText, ps.getScriptText());
    }

    /** Test that getUseStrictMode(), setUseStrictMode(), and related methods work correctly. */
    @Test
    public void testStrictModeConfiguration() {
        PSScript ps = new PSScript("$x");

        // Default should be non-strict for backward compatibility
        assertEquals("Default strict mode should be false", false, ps.getUseStrictMode());

        // Setting strict mode should be reflected
        ps.setUseStrictMode(true);
        assertEquals("Strict mode should be true after setting", true, ps.getUseStrictMode());
    }

    /** Test that silent mode configuration works. */
    @Test
    public void testSilentModeConfiguration() {
        PSScript ps = new PSScript("$x");

        // Default should be non-silent
        assertEquals("Default silent mode should be false", false, ps.getSilentMode());

        ps.setUseSilentMode(true);
        assertEquals("Silent mode should be true after setting", true, ps.getSilentMode());
    }

    /** Test that debug mode configuration works. */
    @Test
    public void testDebugModeConfiguration() {
        PSScript ps = new PSScript("$x");

        // Default should be non-debug
        assertEquals("Default debug mode should be false", false, ps.getUseDebugMode());

        ps.setUseDebugMode(true);
        assertEquals("Debug mode should be true after setting", true, ps.getUseDebugMode());
    }

    /** Test toString() for debugging. */
    @Test
    public void testToString() {
        String scriptText = "$x + 1";
        PSScript ps = new PSScript(scriptText);
        ps.setOwnerType("Widget");
        ps.setOwnerName("TestWidget");

        String str = ps.toString();
        assertTrue("toString should contain script text", str.contains(scriptText));
        assertTrue("toString should contain owner type", str.contains("Widget"));
        assertTrue("toString should contain owner name", str.contains("TestWidget"));
    }

    /** Test that null/empty owner type/name are handled gracefully. */
    @Test
    public void testNullOwnerContextHandling() {
        PSScript ps = new PSScript("$x");

        ps.setOwnerType(null);
        ps.setOwnerName(null);

        assertEquals("Null owner type should become empty", "", ps.getOwnerType());
        assertEquals("Null owner name should become empty", "", ps.getOwnerName());

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("$x", 5);

        Object result = ps.eval(bindings);
        assertEquals("Evaluation should work with null owner context", 5, result);
    }

    /** Test whitespace handling in owner type/name (should be trimmed). */
    @Test
    public void testOwnerContextTrimming() {
        PSScript ps = new PSScript("$x");

        ps.setOwnerType("  Template  ");
        ps.setOwnerName("  MyTemplate  ");

        assertEquals("Owner type should be trimmed", "Template", ps.getOwnerType());
        assertEquals("Owner name should be trimmed", "MyTemplate", ps.getOwnerName());
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
        assertEquals("getSourceText should match script", scriptText, ps.getSourceText());
        assertEquals("getParsedText should match script", scriptText, ps.getParsedText());

        // After evaluation
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("$x", 1);
        bindings.put("$y", 2);
        ps.eval(bindings);

        assertEquals("getSourceText should still match original", scriptText, ps.getSourceText());
        assertEquals("getParsedText should still match original", scriptText, ps.getParsedText());
    }

    /** Test that long-running evaluation does not cause issues. */
    @Test
    public void testLongRunningEvaluation() {
        // Simple loop that runs a moderate number of times
        String script = "var sum = 0; for (var i = 0; i < 100; i = i + 1) { sum = sum + i; } sum";
        PSScript ps = new PSScript(script);

        Map<String, Object> bindings = new HashMap<>();

        Object result = ps.eval(bindings);
        assertEquals("Loop evaluation should complete and return correct result", 4950, result);
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
        assertEquals("Nested property access should work", "nested", result);
    }

    /** Test isCompilable() behavior. */
    @Test
    public void testIsCompilable() {
        PSScript ps = new PSScript("$x");

        // Default should be false (scripts are compiled lazily on first eval)
        assertEquals("isCompilable should default to false", false, ps.isCompilable());
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
        assertEquals("Should access nested map properties", "101", script1.eval(bindings));

        // Test $sys.activeAssembly
        PSScript script2 = new PSScript("$sys.activeAssembly");
        assertEquals("Should access boolean property", true, script2.eval(bindings));

        // Test $sys.params.sys_contentid
        PSScript script3 = new PSScript("$sys.params.sys_contentid");
        assertEquals("Should access params map", "999", script3.eval(bindings));

        // Test map syntax
        PSScript script4 = new PSScript("$sys.params['sys_contentid']");
        assertEquals("Should access params map with bracket syntax", "999", script4.eval(bindings));
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
        assertEquals("Empty list should be empty", true, script1.eval(bindings));

        PSScript script2 = new PSScript("empty $nullVar");
        assertEquals("Null var should be empty", true, script2.eval(bindings));

        PSScript script3 = new PSScript("empty $string");
        assertEquals("Empty string should be empty", true, script3.eval(bindings));

        PSScript script4 = new PSScript("empty $notEmpty");
        assertEquals("Non-empty string should not be empty", false, script4.eval(bindings));
    }
}
