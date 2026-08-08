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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Test evaluator for valid and invalid case handling
 *
 * @author dougrand
 */
@TestMethodOrder(MethodName.class)
@Tag("UnitTest")
public class PSJexlEvaluatorTest {

  public PSJexlEvaluatorTest() {}

  /**
   * Test variable binder implementation. The binder creates lists and maps as it encounters the
   * array and dot notation.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("unchecked")
  public void test_01_Binder() throws Exception {
    PSJexlEvaluator eval = new PSJexlEvaluator();

    eval.bind("$x", 1);
    eval.bind("$y[2]", 2);
    eval.bind("$y[5]", 3);
    eval.bind("$z.a.b.c", "n1");
    eval.bind("$z.a.c[1]", "n2");
    eval.bind("$z.a.c[0]", "n3");
    eval.bind("$z.b.x.y[0].w", "a");
    eval.bind("$z.x", 123);
    eval.evaluate("$c", PSJexlEvaluator.createExpression("$y[2] + $y[5]"));
    eval.evaluate("$d", PSJexlEvaluator.createExpression("$x * $y[5]"));
    eval.evaluate("$e", PSJexlEvaluator.createScript("if ($x > 1) {3;} else {4;}"));

    Map<String, Object> vars = eval.getVars();
    // Binder builds nested Map/List graphs as Object values; cast once per navigation step.
    Map<String, Object> z = (Map<String, Object>) vars.get("$z");
    Map<String, Object> a = (Map<String, Object>) z.get("a");
    Map<String, Object> b = (Map<String, Object>) z.get("b");
    Map<String, Object> z_a_b = (Map<String, Object>) a.get("b");
    List<Object> y = (List<Object>) vars.get("$y");
    List<Object> z_a_c = (List<Object>) a.get("c");
    Map<String, Object> z_b_x = (Map<String, Object>) b.get("x");
    List<Object> z_b_x_y = (List<Object>) z_b_x.get("y");
    Map<String, Object> y_0 = (Map<String, Object>) z_b_x_y.get(0);
    assertEquals("n3", z_a_c.get(0));
    assertEquals("n2", z_a_c.get(1));
    assertEquals("n1", z_a_b.get("c"));
    assertEquals(Integer.valueOf(2), y.get(2));
    assertEquals(Integer.valueOf(3), y.get(5));
    assertEquals(null, y.get(0));
    assertEquals("a", y_0.get("w"));
    assertEquals(Integer.valueOf(123), z.get("x"));
    assertEquals(Integer.valueOf(5), (vars.get("$c")));
    assertEquals(Integer.valueOf(3), (vars.get("$d")));
    assertEquals(Integer.valueOf(3), (vars.get("$d")));
  }

  /**
   * Simple evaluation test
   *
   * @throws Exception
   */
  @Test
  public void test_02_PrebindingAndEvaluation() throws Exception {
    Map<String, Object> initial = new HashMap<String, Object>();

    initial.put("a", 1);
    initial.put("b", 2);

    PSJexlEvaluator eval = new PSJexlEvaluator(initial);

    assertEquals(1, eval.evaluate(PSJexlEvaluator.createExpression("a")));
    assertEquals(2, eval.evaluate(PSJexlEvaluator.createExpression("b")));

    eval.bind("$foo.bar.bletch", 3);

    assertEquals(3, eval.evaluate(PSJexlEvaluator.createExpression("$foo.bar.bletch")));
  }

  @Test
  public void testMapCtorRejectsNullBindings() {
    assertThrows(IllegalArgumentException.class, () -> new PSJexlEvaluator(null));
  }

  @Test
  public void testSetValuesRejectsNullBindings() {
    PSJexlEvaluator eval = new PSJexlEvaluator();
    assertThrows(IllegalArgumentException.class, () -> eval.setValues(null));
  }

  @Test
  public void test_03_Add() throws Exception {
    Map<String, Object> initial = new HashMap<String, Object>();
    Map<String, Object> c = new HashMap<String, Object>();
    initial.put("$a", 1);
    initial.put("$b", 2);
    initial.put("$c", c);
    c.put("x", 10);
    c.put("y", 11);

    PSJexlEvaluator eval = new PSJexlEvaluator(initial);

    Map<String, Object> add = new HashMap<String, Object>();
    add.put("y", 12);
    add.put("z", 13);

    eval.add("$c", PSJexlEvaluator.createExpression("$c"), add);

    Map<String, Object> expVarsC = new HashMap<>();
    expVarsC.put("z", 13);
    expVarsC.put("y", 12);
    expVarsC.put("x", 10);
    Map<String, Object> expVars = new HashMap<>();
    expVars.put("$a", 1);
    expVars.put("$b", 2);
    expVars.put("$c", expVarsC);
    assertEquals(expVars, eval.getVars());
  }

  /**
   * Test a concatenation case that is similar
   *
   * @throws Exception
   */
  @Test
  public void test_04_Concat() throws Exception {
    Map<String, Object> initial = new HashMap<String, Object>();
    initial.put("$c", true);
    initial.put("$val1", "the quick");
    initial.put("$val2", "the slow");

    PSJexlEvaluator eval = new PSJexlEvaluator(initial);
    IPSScript exp =
        PSJexlEvaluator.createScript(
            "if ($c) {$a = $val1;} else {$a = $val2;}\n$b = $a + ' brown fox'");
    assertEquals("the quick brown fox", eval.evaluate(exp));
  }

  /**
   * Test backward compatibility from script to expression
   *
   * @throws Exception
   */
  @Test
  public void test_05_EvalScript() throws Exception {
    Map<String, Object> initial = new HashMap<String, Object>();
    initial.put("$c", 2147483647);
    initial.put("$a", 4);

    PSJexlEvaluator eval = new PSJexlEvaluator(initial);
    IPSScript exp = PSJexlEvaluator.createScript("$c");
    assertEquals(Integer.valueOf(2147483647), eval.evaluate(exp));
    exp = PSJexlEvaluator.createScript("$c * $a");
    Object result = eval.evaluate(exp);
    assertEquals(Long.valueOf(8589934588L), result);
  }

  /**
   * Test various exception cases with the uberspect
   *
   * @throws Exception
   */
  @Test
  public void test_06_Errors() throws Exception {
    Map<String, Object> initial = new HashMap<String, Object>();
    initial.put("$a", 1);
    initial.put("$b", 2);
    initial.put("$c", "the fox in the henhouse");

    PSJexlEvaluator eval = new PSJexlEvaluator(initial);

    doExceptionTest(eval, "$c.foo()");
    doExceptionTest(eval, "$c.xyz");
  }

  /**
   * Do an exception test by evaluating the expression and asserting if an exception is not thrown
   *
   * @param eval evaluator
   * @param expression
   * @throws Exception
   */
  private void doExceptionTest(PSJexlEvaluator eval, String expression) throws Exception {
    try {
      IPSScript exp = PSJexlEvaluator.createExpression(expression);

      exp.setUseDebugMode(true);
      exp.setUseSilentMode(false);
      exp.setUseStrictMode(true);
      exp.reinit(false);
      Object ret = exp.eval(eval.getVars());

      assertFalse(true, "An exception should have been thrown for " + expression);
    } catch (RuntimeException t) {
      // OK
      System.out.println(t.getLocalizedMessage());
    }
  }
}
