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

package com.percussion.soln.rx.assembly;

import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.jexl.PSJexlEvaluator;
import com.percussion.utils.jexl.PSScript;
import org.apache.commons.jexl3.JxltEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Convenience base class for assembly helpers that bind expressions to Jexl evaluators and produce
 * assembly results.
 */
public abstract class AbstractAssemblyHelper {
  /** The log instance to use for this class, never <code>null</code>. */
  private static final Logger log = LogManager.getLogger(AbstractAssemblyHelper.class);

  /** No-op constructor. */
  public AbstractAssemblyHelper() {
    // no-op
  }

  /**
   * Binds Jexl expressions on the supplied assembly item's bindings.
   *
   * @param item the assembly item whose bindings are evaluated
   * @return the bound evaluator
   * @throws Exception if evaluation or binding fails
   */
  public PSJexlEvaluator doBindings(IPSAssemblyItem item) throws Exception {
    PSJexlEvaluator eval = getBindings(item);
    PSJexlEvaluator rval = doBindings(eval, item);
    if (rval == null) throw new IllegalStateException("doBindings should not return null");
    return rval;
  }

  /**
   * Hook for subclasses to perform additional Jexl bindings. Default implementation is a no-op.
   *
   * @param eval the evaluator to extend
   * @param item the assembly item being processed
   * @return the supplied evaluator (default behaviour)
   * @throws Exception if subclass binding fails
   */
  protected PSJexlEvaluator doBindings(PSJexlEvaluator eval, IPSAssemblyItem item)
      throws Exception {
    return eval;
  }

  /**
   * Routes the result through the publish hook when publishing is active.
   *
   * @param eval the active evaluator
   * @param result the result produced so far
   * @return the possibly transformed result
   * @throws Exception if publish-time processing fails
   */
  public IPSAssemblyResult doResults(PSJexlEvaluator eval, IPSAssemblyResult result)
      throws Exception {
    if (isPublishResults(result)) {
      return doPublishResults(eval, result);
    }
    return result;
  }

  /**
   * Hook for subclasses to transform publish results. Default is identity.
   *
   * @param eval the active evaluator
   * @param result the publish-time result
   * @return the possibly transformed result
   * @throws Exception if subclass processing fails
   */
  protected IPSAssemblyResult doPublishResults(PSJexlEvaluator eval, IPSAssemblyResult result)
      throws Exception {
    return result;
  }

  /**
   * Returns a Jexl evaluator populated from the assembly item's bindings.
   *
   * @param item the assembly item providing bindings
   * @return a new evaluator over the item's bindings
   */
  public static PSJexlEvaluator getBindings(IPSAssemblyItem item) {
    return new PSJexlEvaluator(item.getBindings());
  }

  /**
   * Evaluates {@code exp} and binds the result to {@code value} if it is unset, otherwise returns
   * the existing value.
   *
   * @param eval the evaluator on which to bind
   * @param exp the Jexl expression to evaluate
   * @param value the value to bind when no existing value is set
   * @param <T> the value type
   * @return the resulting bound value
   * @throws Exception if evaluation fails or the existing value is of an incompatible type
   */
  @SuppressWarnings("unchecked")
  public static <T> T bindExpression(PSJexlEvaluator eval, JxltEngine.Expression exp, T value)
      throws Exception {

    PSScript script = new PSScript(exp.asString());
    Object original = eval.evaluate(script);
    T rvalue;
    if (original == null) {
      if (log.isTraceEnabled()) log.trace("Binding expression: {} to {}", exp.asString(), value);
      eval.bind(exp.asString(), value);
      rvalue = value;
    } else {
      log.debug("{} is already set to: {}", exp, original);
      if (value != null && !original.getClass().isInstance(value)) {
        throw new Exception(
            exp.asString()
                + " should be of type: "
                + value.getClass()
                + "but is type: "
                + original.getClass());
      }
      rvalue = (T) original;
    }
    return rvalue;
  }

  /**
   * Evaluates {@code exp} and returns the resulting value as type {@code T}.
   *
   * @param eval the evaluator on which to evaluate
   * @param exp the Jexl expression to evaluate
   * @param k the desired return type
   * @param <T> the result type
   * @return the evaluated value
   * @throws Exception if evaluation fails
   */
  @SuppressWarnings("unchecked")
  public static <T> T evalExpression(PSJexlEvaluator eval, JxltEngine.Expression exp, Class<T> k)
      throws Exception {
    PSScript script = new PSScript(exp.asString());

    return (T) eval.evaluate(script);
  }

  /**
   * Indicates whether the supplied assembly item is being processed for publishing.
   *
   * @param result the assembly item under inspection
   * @return {@code true} when publishing is active for the item
   */
  public static boolean isPublishResults(IPSAssemblyItem result) {
    String context = result.getParameterValue(IPSHtmlParameters.SYS_CONTEXT, null);
    return (!result.isDebug()
        && context != null
        && !"0".equals(context)
        && result.getCloneParentItem() == null);
  }
}
