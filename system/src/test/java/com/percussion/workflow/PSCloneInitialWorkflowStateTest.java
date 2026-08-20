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
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * NewCopy clone check-in requires {@code CONTENTSTATEID > 0} (#3667). When the
 * clone row has state 0, the workflow initial state (by app id) is used.
 */
@Tag("UnitTest")
class PSCloneInitialWorkflowStateTest {

  @Test
  void leavesPositiveStateUnchanged() {
    IntUnaryOperator lookup =
        wf -> {
          fail("must not look up when state is already set");
          return 0;
        };

    assertEquals(11, PSCloneInitialWorkflowState.coerceStateId(7, 11, lookup));
  }

  @Test
  void usesWorkflowInitialStateWhenCloneStateIsZero() {
    assertEquals(5, PSCloneInitialWorkflowState.coerceStateId(7, 0, wf -> 5));
  }

  @Test
  void usesWorkflowInitialStateWhenCloneStateIsNegative() {
    AtomicInteger seenWf = new AtomicInteger();
    int coerced =
        PSCloneInitialWorkflowState.coerceStateId(
            4,
            -1,
            wf -> {
              seenWf.set(wf);
              return 1;
            });
    assertEquals(1, coerced);
    assertEquals(4, seenWf.get());
  }

  @Test
  void keepsZeroWhenLookupIsNull() {
    assertEquals(0, PSCloneInitialWorkflowState.coerceStateId(7, 0, null));
  }

  @Test
  void keepsZeroWhenWorkflowIdIsNotPositive() {
    IntUnaryOperator lookup =
        wf -> {
          fail("must not look up without a workflow id");
          return 9;
        };
    assertEquals(0, PSCloneInitialWorkflowState.coerceStateId(0, 0, lookup));
  }

  @Test
  void keepsZeroWhenLookupReturnsNonPositive() {
    assertEquals(0, PSCloneInitialWorkflowState.coerceStateId(7, 0, wf -> 0));
    assertEquals(0, PSCloneInitialWorkflowState.coerceStateId(7, 0, wf -> -3));
  }

  @Test
  void assignOnContentStatusRejectsNonPositiveContentId() {
    assertEquals(false, PSCloneInitialWorkflowState.assignOnContentStatus(0));
    assertEquals(false, PSCloneInitialWorkflowState.assignOnContentStatus(-1));
  }
}
