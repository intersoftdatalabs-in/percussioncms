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

import org.junit.jupiter.api.Test;

/** Check-in and checkout must look up roles on the current state, not to-state 0 (#5246). */
class PSWorkflowAssignmentStateTest {

  @Test
  void checkInUsesTheCurrentStateWhenToStateWasNotSet() {
    assertEquals(1, PSWorkflowAssignmentState.stateIdForRoles(1, 0));
  }

  @Test
  void unchangedStateUsesTheCurrentState() {
    assertEquals(4, PSWorkflowAssignmentState.stateIdForRoles(4, 4));
  }

  @Test
  void transitionUsesTheDestinationState() {
    assertEquals(7, PSWorkflowAssignmentState.stateIdForRoles(1, 7));
  }
}
