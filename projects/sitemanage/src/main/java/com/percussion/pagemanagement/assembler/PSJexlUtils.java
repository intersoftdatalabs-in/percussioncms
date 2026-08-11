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
package com.percussion.pagemanagement.assembler;

import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.utils.jexl.IPSScript;
import com.percussion.utils.jexl.PSJexlEvaluator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Utility methods for JEXL expression evaluation and binding. */
public class PSJexlUtils {

  /**
   * Gets a JEXL evaluator with the bindings from the given assembly item.
   *
   * @param item the assembly item
   * @return a JEXL evaluator
   */
  public static PSJexlEvaluator getBindings(IPSAssemblyItem item) {
    return new PSJexlEvaluator(item.getBindings());
  }

  /**
   * Binds an expression to a value if not already set, or returns the existing value.
   *
   * @param eval the JEXL evaluator
   * @param exp the script expression
   * @param value the value to bind
   * @param <T> the type of the value
   * @return the bound or existing value
   * @throws Exception if evaluation or binding fails
   */
  public static <T> T bindExpression(PSJexlEvaluator eval, IPSScript exp, T value)
      throws Exception {
    var original = eval.evaluate(exp);
    if (original == null) {
      if (log.isTraceEnabled()) {
        log.trace("Binding expression: {} to {}", exp.getSourceText(), value);
      }
      eval.bind(exp.getSourceText(), value);
      return value;
    }
    log.debug("{} is already set to: {}", exp.getSourceText(), original);
    if (value == null) {
      // Existing binding present; caller requested null default — return null typed as T.
      return null;
    }
    Class<?> expected = value.getClass();
    if (!expected.isInstance(original)) {
      throw new RuntimeException(
          exp.getSourceText()
              + " should be of type: "
              + expected
              + " but is type: "
              + original.getClass());
    }
    // Runtime-checked cast via Class.cast; generic T is the compile-time type of value.
    @SuppressWarnings("unchecked")
    Class<T> type = (Class<T>) expected;
    return type.cast(original);
  }

  /**
   * Evaluates an expression and returns the result.
   *
   * @param eval the JEXL evaluator
   * @param exp the script expression
   * @param k the expected class used for a runtime-checked cast
   * @param <T> the type of the result
   * @return the evaluated result
   * @throws Exception if evaluation fails
   */
  public static <T> T evalExpression(PSJexlEvaluator eval, IPSScript exp, Class<T> k)
      throws Exception {
    Object result = eval.evaluate(exp);
    if (result == null) {
      return null;
    }
    return k.cast(result);
  }

  /** The log instance to use for this class, never {@code null}. */
  private static final Logger log = LogManager.getLogger(PSJexlUtils.class);
}
