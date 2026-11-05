/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.PSConditional;
import com.percussion.design.objectstore.PSExtensionCallSet;
import com.percussion.design.objectstore.PSRule;
import com.percussion.util.PSCollection;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed {@link PSRuleListEvaluator} after rawtypes cleanup: empty rule list
 * always matches; non-empty typed iterators of empty-condition rules also match.
 */
@Tag("UnitTest")
class PSRuleListEvaluatorTypedTest {

  @Test
  void emptyRuleListAlwaysMatches() throws Exception {
    PSRuleListEvaluator evaluator = new PSRuleListEvaluator((java.util.Iterator<?>) null);
    assertTrue(evaluator.isMatch(null));
  }

  @Test
  void emptyCollectionAlwaysMatches() throws Exception {
    PSRuleListEvaluator evaluator = new PSRuleListEvaluator((com.percussion.util.PSCollection) null);
    assertTrue(evaluator.isMatch(null));
  }

  @Test
  void nonEmptyTypedIteratorWithEmptyConditionRuleMatches() throws Exception {
    PSRule emptyRule = emptyConditionRule();
    Iterator<PSRule> rules = List.of(emptyRule).iterator();
    PSRuleListEvaluator evaluator = new PSRuleListEvaluator(rules);
    // Empty conditionals + empty extension set → rule matches.
    assertTrue(evaluator.isMatch(null));
  }

  @Test
  void nonEmptyCollectionWithEmptyConditionRuleMatches() throws Exception {
    PSCollection rules = new PSCollection(PSRule.class);
    rules.add(emptyConditionRule());
    PSRuleListEvaluator evaluator = new PSRuleListEvaluator(rules);
    assertTrue(evaluator.isMatch(null));
  }

  @Test
  void nonRuleElementRejected() {
    Iterator<?> bad = List.of("not-a-rule").iterator();
    assertThrows(IllegalArgumentException.class, () -> new PSRuleListEvaluator(bad));
  }

  /**
   * Conditional-style rule with no conditions and no extensions (always matches). Real {@link
   * PSRule}(empty conditionals) leaves {@code getExtensionRules()} null, which NPEs in {@link
   * PSRuleEvaluator}; mock supplies an empty call set.
   */
  private static PSRule emptyConditionRule() {
    PSRule rule = mock(PSRule.class);
    when(rule.getConditionalRulesCollection()).thenReturn(new PSCollection(PSConditional.class));
    when(rule.getExtensionRules()).thenReturn(new PSExtensionCallSet());
    when(rule.getOperator()).thenReturn(PSRule.BOOLEAN_AND);
    return rule;
  }
}
