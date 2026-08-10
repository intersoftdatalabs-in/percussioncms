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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed {@link PSRuleListEvaluator} after rawtypes cleanup: empty rule list
 * always matches.
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
}
