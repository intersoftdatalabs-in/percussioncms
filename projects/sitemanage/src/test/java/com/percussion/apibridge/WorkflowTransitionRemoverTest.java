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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.services.workflow.data.PSAgingTransition;
import com.percussion.services.workflow.data.PSAgingTransition.PSAgingTypeEnum;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class WorkflowTransitionRemoverTest {

  @Test
  void removesOneAbsoluteAgingTransitionAndLeavesTheRegularTransition() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    draft.addTransition(regular("Submit", 2));
    draft.addAgingTransition(absolute("Aging 15", 2, 15));
    draft.addAgingTransition(absolute("Aging 45", 2, 45));

    WorkflowTransitionRemover.removeAbsoluteAging(List.of(draft, review), "Draft", "Review", 15);

    assertEquals(1, draft.getAgingTransitions().size());
    assertEquals(45L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(1, draft.getTransitions().size());
    assertEquals("Submit", draft.getTransitions().get(0).getLabel());
    assertEquals(2, List.of(draft, review).size());
  }

  @Test
  void repeatedMatchIs409AndStays() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSAgingTransition repeated = absolute("Repeat", 2, 15);
    repeated.setType(PSAgingTypeEnum.REPEATED);
    draft.addAgingTransition(repeated);
    draft.addTransition(regular("Submit", 2));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowTransitionRemover.removeAbsoluteAging(
                    List.of(draft, review), "Draft", "Review", 15));

    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(1, draft.getAgingTransitions().size());
    assertEquals(1, draft.getTransitions().size());
  }

  @Test
  void missingAbsoluteAgingIs404() {
    PSState draft = state(1, "Draft");
    draft.addTransition(regular("Submit", 2));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowTransitionRemover.removeAbsoluteAging(
                    List.of(draft, state(2, "Review")), "Draft", "Review", 15));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals(1, draft.getTransitions().size());
  }

  @Test
  void nonPositiveIntervalDoesNotRemove() {
    PSState draft = state(1, "Draft");
    draft.addAgingTransition(absolute("Aging 15", 2, 15));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            WorkflowTransitionRemover.removeAbsoluteAging(
                List.of(draft, state(2, "Review")), "Draft", "Review", 0));
    assertEquals(1, draft.getAgingTransitions().size());
  }

  private static PSState state(long id, String name) {
    PSState state = new PSState();
    state.setStateId(id);
    state.setName(name);
    state.setWorkflowId(7);
    return state;
  }

  private static PSTransition regular(String label, long toState) {
    PSTransition transition = new PSTransition();
    transition.setLabel(label);
    transition.setTrigger(label);
    transition.setToState(toState);
    return transition;
  }

  private static PSAgingTransition absolute(String label, long toState, long minutes) {
    PSAgingTransition aging = new PSAgingTransition();
    aging.setType(PSAgingTypeEnum.ABSOLUTE);
    aging.setLabel(label);
    aging.setTrigger(label);
    aging.setToState(toState);
    aging.setInterval(minutes);
    return aging;
  }
}
