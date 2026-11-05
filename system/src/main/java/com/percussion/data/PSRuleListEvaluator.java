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
package com.percussion.data;

import com.percussion.design.objectstore.PSRule;
import com.percussion.error.PSNotFoundException;
import com.percussion.extension.PSExtensionException;
import com.percussion.util.PSCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A new concept known as a PSRule was added when the Content Editors were first created. A rule is
 * either a collection of conditionals or an extension. Rules can be combined with boolean 'and' and
 * 'or' operators to create complex expressions. For the given list of rules, 'and' operations will
 * be considered to have higher precedence, which means that rules seperated by 'and' operators will
 * be grouped and evaluated prior to applying 'or' operations.
 *
 * <p>This class constructs an appropriate representation of the definitions that can be executed
 * repeatedly at run time using the {@link #isMatch(PSExecutionData) isMatch} method.
 */
public class PSRuleListEvaluator {
  /**
   * Constructs a rule list evaluator, building an appropriate internal representation of the
   * supplied list of rules as defined by their operators.
   *
   * @param rules a collection of <code>PSRule</code> objects. If <code>null</code> or empty, <code>
   *     isMatch</code> will always return <code>true</code>.
   * @throws PSNotFoundException if a specified extension cannot be found.
   * @throws PSExtensionException if any errors occur while preparing a runnable version of an
   *     extension.
   */
  public PSRuleListEvaluator(PSCollection rules) throws PSNotFoundException, PSExtensionException {
    this(rules == null ? null : rules.iterator());
  }

  /*
   * Convenience method for supplying an iterator pointing to a list of
   * <code>PSRules</code>, see the <code>PSCollection</code> constructor for
   * more information.
   */
  public PSRuleListEvaluator(Iterator<?> rules) throws PSNotFoundException, PSExtensionException {
    List<PSRuleEvaluator> andRules = null;
    if (rules != null) {
      while (rules.hasNext()) {
        Object next = rules.next();
        if (!(next instanceof PSRule rule)) {
          throw new IllegalArgumentException(
              "rules iterator must contain only PSRule elements, got: "
                  + (next == null ? "null" : next.getClass().getName()));
        }
        PSRuleEvaluator evaluator = new PSRuleEvaluator(rule);

        if (rules.hasNext()) {
          if (rule.getOperator() == PSRule.BOOLEAN_AND) {
            if (andRules == null) {
              andRules = new ArrayList<>();
              m_andGroups.add(andRules);
            }

            andRules.add(evaluator);
          } else {
            if (andRules != null) {
              andRules.add(evaluator);
              andRules = null;
            } else m_orRules.add(evaluator);
          }
        } else {
          if (andRules == null) m_orRules.add(evaluator);
          else andRules.add(evaluator);
        }
      }
    }
  }

  /**
   * Evaluates this rule list against the supplied execution data.
   *
   * <p>This evaluator may use the request context hash tables, the input XML document and the
   * result set(s) for processing.
   *
   * <p>When using multiple rules (chaining) a boolean operator must be specified on all but the
   * last one. The boolean operators currently supported are AND and OR. AND is the default boolean
   * operator. The rules are processed in the order they were supplied in the collection, with AND
   * operations having higher precedence than OR operations.
   *
   * <p>A 'short-circuit' algorithm is used, meaning as soon as the result is known, the rest of the
   * rules will not be processed.
   *
   * @param data the execution data the evaluator will be applied to. The row data will be obtained
   *     by calling getCurrentResultRowData() on this parameter.
   * @return <code>true</code> if the conditional criteria is met, <code>false</code> otherwise.
   */
  public boolean isMatch(PSExecutionData data) {
    // no evaluators evaluate to true
    if (m_andGroups.isEmpty() && m_orRules.isEmpty()) return true;

    boolean isMatch = true;

    // first evaluate all AND groups and collect the results
    List<Boolean> andResults = new ArrayList<>();
    for (int i = 0; i < m_andGroups.size(); i++) {
      isMatch = true;
      Iterator<PSRuleEvaluator> evaluators = m_andGroups.get(i).iterator();
      while (isMatch && evaluators.hasNext()) {
        PSRuleEvaluator evaluator = evaluators.next();
        isMatch = evaluator.isMatch(data);
      }

      andResults.add(isMatch);
    }

    // then OR all group results
    isMatch = false;
    Iterator<Boolean> results = andResults.iterator();
    while (!isMatch && results.hasNext()) {
      Boolean result = results.next();
      isMatch = result.booleanValue();
    }

    // finally evaluate all OR evaluators
    Iterator<PSRuleEvaluator> evaluators = m_orRules.iterator();
    while (!isMatch && evaluators.hasNext()) {
      PSRuleEvaluator evaluator = evaluators.next();
      isMatch = evaluator.isMatch(data);
    }

    return isMatch;
  }

  /**
   * This list contains a list of list of <code>PSRuleEvaluator</code> objects. All <code>
   * PSRuleEvaluator</code> objects in these lists are ANDed together. The results of all lists are
   * ORed together. Initialized in the constructor and never changed after that, never <code>null
   * </code>, may be empty.
   */
  private final List<List<PSRuleEvaluator>> m_andGroups = new ArrayList<>();

  /**
   * A list with <code>PSRuleEvaluator</code> objects ORd together. Initialized in the constructor
   * and never changed after that, never <code>null</code>, may be empty.
   */
  private final List<PSRuleEvaluator> m_orRules = new ArrayList<>();
}
