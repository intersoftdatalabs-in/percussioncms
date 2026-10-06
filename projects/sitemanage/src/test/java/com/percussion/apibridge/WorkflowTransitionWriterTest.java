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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.data.PSAgingTransition;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class WorkflowTransitionWriterTest {

  private final AtomicLong ids = new AtomicLong(800);

  @Test
  void createsTransitionBetweenExistingSteps() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    List<PSState> states = List.of(draft, review);

    WorkflowTransitionWriter.create(states, "Draft", "Review", "Send", this::allocate);

    assertEquals(2, states.size());
    assertEquals(1, draft.getTransitions().size());
    PSTransition created = draft.getTransitions().get(0);
    assertEquals("Send", created.getLabel());
    assertEquals("Send", created.getTrigger());
    assertEquals(2L, created.getToState());
    assertEquals(1L, created.getStateId());
    assertTrue(created.isAllowAllRoles());
    assertTrue(created.getGUID().longValue() >= 800);
  }

  @Test
  void duplicateEdgeIs409AndDoesNotAdd() {
    PSState draft = state(1, "Draft");
    draft.addTransition(transition("Submit", 2));
    PSState review = state(2, "Review");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowTransitionWriter.create(
                    List.of(draft, review), "draft", "review", "submit", this::allocate));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(1, draft.getTransitions().size());
  }

  @Test
  void missingDestinationIs404() {
    PSState draft = state(1, "Draft");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowTransitionWriter.create(
                    List.of(draft), "Draft", "Archive", "Send", this::allocate));
    assertEquals(404, ex.getResponse().getStatus());
    assertTrue(draft.getTransitions().isEmpty());
  }

  @Test
  void invalidLabelIs400() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            WorkflowTransitionWriter.create(
                List.of(draft, review), "Draft", "Review", "Send!", this::allocate));
    assertTrue(draft.getTransitions().isEmpty());
  }

  @Test
  void updatesLabelAndDestinationWithoutAddingSteps() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSState pending = state(3, "Pending");
    PSTransition submit = transition("Submit", 2);
    submit.setTrigger("Submit");
    draft.addTransition(submit);
    List<PSState> states = List.of(draft, review, pending);

    WorkflowTransitionWriter.update(states, "Draft", "Submit", "Review", "Send", "Pending");

    assertEquals(3, states.size());
    assertEquals(1, draft.getTransitions().size());
    PSTransition updated = draft.getTransitions().get(0);
    assertEquals("Send", updated.getLabel());
    assertEquals("Send", updated.getTrigger());
    assertEquals(3L, updated.getToState());
  }

  @Test
  void keepsDistinctTriggerWhenRenamingLabel() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSTransition submit = transition("Submit for review", 2);
    submit.setTrigger("Submit");
    draft.addTransition(submit);

    WorkflowTransitionWriter.update(
        List.of(draft, review), "Draft", "Submit", "Review", "Send", "Review");

    PSTransition updated = draft.getTransitions().get(0);
    assertEquals("Send", updated.getLabel());
    assertEquals("Submit", updated.getTrigger());
  }

  @Test
  void ambiguousLabelWithoutDestinationIs400() {
    PSState draft = state(1, "Draft");
    draft.addTransition(transition("Submit", 2));
    draft.addTransition(transition("Submit", 3));
    PSState review = state(2, "Review");
    PSState pending = state(3, "Pending");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                WorkflowTransitionWriter.update(
                    List.of(draft, review, pending), "Draft", "Submit", null, "Send", "Review"));
    assertTrue(ex.getMessage().toLowerCase().contains("specify to"));
    assertEquals("Submit", draft.getTransitions().get(0).getLabel());
  }

  @Test
  void missingTransitionIs404() {
    PSState draft = state(1, "Draft");
    draft.addTransition(transition("Submit", 2));
    PSState review = state(2, "Review");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowTransitionWriter.update(
                    List.of(draft, review), "Draft", "Publish", "Review", "Send", "Review"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void updateCollisionIs409() {
    PSState draft = state(1, "Draft");
    draft.addTransition(transition("Submit", 2));
    draft.addTransition(transition("Send", 3));
    PSState review = state(2, "Review");
    PSState pending = state(3, "Pending");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowTransitionWriter.update(
                    List.of(draft, review, pending), "Draft", "Submit", "Review", "Send", "Pending"));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals("Submit", draft.getTransitions().get(0).getLabel());
  }

  @Test
  void updatesAgingLabelAndDestination() {
    PSState live = state(4, "Live");
    PSState archive = state(5, "Archive");
    PSState review = state(2, "Review");
    PSAgingTransition expire = new PSAgingTransition();
    expire.setLabel("Expire");
    expire.setTrigger("Expire");
    expire.setToState(5L);
    live.addAgingTransition(expire);

    WorkflowTransitionWriter.update(
        List.of(live, archive, review), "Live", "Expire", "Archive", "Retire", "Review");

    assertEquals(1, live.getAgingTransitions().size());
    PSAgingTransition updated = live.getAgingTransitions().get(0);
    assertEquals("Retire", updated.getLabel());
    assertEquals("Retire", updated.getTrigger());
    assertEquals(2L, updated.getToState());
  }

  @Test
  void createsAbsoluteAgingWithoutAddingARegularTransition() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    long before = ids.get();

    WorkflowTransitionWriter.createAbsoluteAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);

    assertEquals(before + 1, ids.get());
    assertEquals(0, draft.getTransitions().size());
    assertEquals(1, draft.getAgingTransitions().size());
    PSAgingTransition created = draft.getAgingTransitions().get(0);
    assertEquals(15L, created.getInterval());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, created.getAgingTypeEnum());
    assertEquals("Aging 15", created.getLabel());
    assertEquals("Aging 15", created.getTrigger());
    assertEquals(2L, created.getToState());
    assertEquals(1L, created.getStateId());
  }

  @Test
  void nonPositiveIntervalDoesNotAllocate() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    long before = ids.get();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                WorkflowTransitionWriter.createAbsoluteAging(
                    List.of(draft, review), "Draft", "Review", 0, this::allocate));
    assertTrue(ex.getMessage().contains("positive"));
    assertEquals(before, ids.get());
    assertEquals(0, draft.getAgingTransitions().size());
  }

  @Test
  void blankDestinationIsRejected() {
    PSState draft = state(1, "Draft");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            WorkflowTransitionWriter.createAbsoluteAging(
                List.of(draft, state(2, "Review")), "Draft", " ", 15, this::allocate));
    assertEquals(0, draft.getAgingTransitions().size());
  }

  @Test
  void duplicateAbsoluteIntervalIs409() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    WorkflowTransitionWriter.createAbsoluteAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowTransitionWriter.createAbsoluteAging(
                    List.of(draft, review), "Draft", "Review", 15, this::allocate));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(1, draft.getAgingTransitions().size());
  }

  @Test
  void missingStepIs404() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowTransitionWriter.createAbsoluteAging(
                    List.of(state(1, "Draft")), "Draft", "Review", 15, this::allocate));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void changesAbsoluteIntervalWithoutMovingTheDestination() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    WorkflowTransitionWriter.createAbsoluteAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);
    long beforeIds = ids.get();

    WorkflowTransitionWriter.changeAbsoluteInterval(List.of(draft, review), "Draft", "Review", 15, 30);

    assertEquals(beforeIds, ids.get());
    assertEquals(0, draft.getTransitions().size());
    assertEquals(1, draft.getAgingTransitions().size());
    PSAgingTransition updated = draft.getAgingTransitions().get(0);
    assertEquals(30L, updated.getInterval());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, updated.getAgingTypeEnum());
    assertEquals(2L, updated.getToState());
    assertEquals("Aging 30", updated.getLabel());
    assertEquals("Aging 30", updated.getTrigger());
    assertEquals("Aging 30", updated.getDescription());
  }

  @Test
  void keepsACustomAgingLabelWhenTheIntervalChanges() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSAgingTransition aging = new PSAgingTransition();
    aging.setType(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE);
    aging.setInterval(15);
    aging.setToState(2L);
    aging.setLabel("Expire");
    aging.setTrigger("Expire");
    aging.setDescription("Keep me");
    draft.addAgingTransition(aging);

    WorkflowTransitionWriter.changeAbsoluteInterval(List.of(draft, review), "Draft", "Review", 15, 45);

    // addAgingTransition copies into transition hibernates and rebuilds the cache, so the
    // caller's original reference is not the object the writer mutates.
    assertEquals(1, draft.getAgingTransitions().size());
    PSAgingTransition updated = draft.getAgingTransitions().get(0);
    assertEquals(45L, updated.getInterval());
    assertEquals("Expire", updated.getLabel());
    assertEquals("Expire", updated.getTrigger());
    assertEquals("Keep me", updated.getDescription());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, updated.getAgingTypeEnum());
    assertEquals(2L, updated.getToState());
  }

  @Test
  void nonPositiveIntervalChangeDoesNotMutate() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    WorkflowTransitionWriter.createAbsoluteAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            WorkflowTransitionWriter.changeAbsoluteInterval(
                List.of(draft, review), "Draft", "Review", 15, 0));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            WorkflowTransitionWriter.changeAbsoluteInterval(
                List.of(draft, review), "Draft", "Review", 15, 15));
    assertEquals(15L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals("Aging 15", draft.getAgingTransitions().get(0).getLabel());
  }

  @Test
  void duplicateNewIntervalIs409() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    WorkflowTransitionWriter.createAbsoluteAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);
    WorkflowTransitionWriter.createAbsoluteAging(
        List.of(draft, review), "Draft", "Review", 45, this::allocate);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowTransitionWriter.changeAbsoluteInterval(
                    List.of(draft, review), "Draft", "Review", 15, 45));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(15L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(45L, draft.getAgingTransitions().get(1).getInterval());
  }

  @Test
  void missingAbsoluteAgingIs404() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowTransitionWriter.changeAbsoluteInterval(
                    List.of(draft, review), "Draft", "Review", 15, 30));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals(0, draft.getAgingTransitions().size());
  }

  @Test
  void repeatedAgingIntervalIsRejected() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSAgingTransition repeated = new PSAgingTransition();
    repeated.setType(PSAgingTransition.PSAgingTypeEnum.REPEATED);
    repeated.setInterval(15);
    repeated.setToState(2L);
    repeated.setLabel("Repeat");
    draft.addAgingTransition(repeated);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                WorkflowTransitionWriter.changeAbsoluteInterval(
                    List.of(draft, review), "Draft", "Review", 15, 30));
    assertTrue(ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("absolute"));
    assertEquals(15L, repeated.getInterval());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.REPEATED, repeated.getAgingTypeEnum());
  }

  @Test
  void changesRepeatedIntervalAndLeavesTheAbsoluteEdge() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    WorkflowTransitionWriter.createAbsoluteAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);
    WorkflowTransitionWriter.createRepeatedAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);
    long beforeIds = ids.get();

    WorkflowTransitionWriter.changeRepeatedInterval(List.of(draft, review), "Draft", "Review", 15, 30);

    assertEquals(beforeIds, ids.get());
    assertEquals(0, draft.getTransitions().size());
    assertEquals(2, draft.getAgingTransitions().size());
    PSAgingTransition absolute = draft.getAgingTransitions().get(0);
    PSAgingTransition repeated = draft.getAgingTransitions().get(1);
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, absolute.getAgingTypeEnum());
    assertEquals(15L, absolute.getInterval());
    assertEquals("Aging 15", absolute.getLabel());
    assertEquals(2L, absolute.getToState());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.REPEATED, repeated.getAgingTypeEnum());
    assertEquals(30L, repeated.getInterval());
    assertEquals("Repeated aging 30", repeated.getLabel());
    assertEquals("Repeated aging 30", repeated.getTrigger());
    assertEquals("Repeated aging 30", repeated.getDescription());
    assertEquals(2L, repeated.getToState());
  }

  @Test
  void absoluteIntervalChangeLeavesTheRepeatedEdge() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    WorkflowTransitionWriter.createAbsoluteAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);
    WorkflowTransitionWriter.createRepeatedAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);

    WorkflowTransitionWriter.changeAbsoluteInterval(List.of(draft, review), "Draft", "Review", 15, 30);

    assertEquals(30L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, draft.getAgingTransitions().get(0).getAgingTypeEnum());
    assertEquals(15L, draft.getAgingTransitions().get(1).getInterval());
    assertEquals("Repeated aging 15", draft.getAgingTransitions().get(1).getLabel());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.REPEATED, draft.getAgingTransitions().get(1).getAgingTypeEnum());
  }

  @Test
  void repeatedChangeMayUseAnIntervalThatAnAbsoluteEdgeAlreadyHas() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    WorkflowTransitionWriter.createAbsoluteAging(
        List.of(draft, review), "Draft", "Review", 30, this::allocate);
    WorkflowTransitionWriter.createRepeatedAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);

    WorkflowTransitionWriter.changeRepeatedInterval(List.of(draft, review), "Draft", "Review", 15, 30);

    assertEquals(30L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, draft.getAgingTransitions().get(0).getAgingTypeEnum());
    assertEquals(30L, draft.getAgingTransitions().get(1).getInterval());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.REPEATED, draft.getAgingTransitions().get(1).getAgingTypeEnum());
    assertEquals("Repeated aging 30", draft.getAgingTransitions().get(1).getLabel());
  }

  @Test
  void missingRepeatedIntervalWhenOnlyAbsoluteExistsIs404() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    WorkflowTransitionWriter.createAbsoluteAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowTransitionWriter.changeRepeatedInterval(
                    List.of(draft, review), "Draft", "Review", 15, 30));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals(15L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, draft.getAgingTransitions().get(0).getAgingTypeEnum());
    assertEquals("Aging 15", draft.getAgingTransitions().get(0).getLabel());
  }

  @Test
  void duplicateRepeatedNewIntervalIs409AndLeavesBothEdges() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    WorkflowTransitionWriter.createAbsoluteAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);
    WorkflowTransitionWriter.createRepeatedAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);
    WorkflowTransitionWriter.createRepeatedAging(
        List.of(draft, review), "Draft", "Review", 45, this::allocate);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowTransitionWriter.changeRepeatedInterval(
                    List.of(draft, review), "Draft", "Review", 15, 45));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(15L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(15L, draft.getAgingTransitions().get(1).getInterval());
    assertEquals(45L, draft.getAgingTransitions().get(2).getInterval());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.REPEATED, draft.getAgingTransitions().get(1).getAgingTypeEnum());
  }

  @Test
  void nonPositiveRepeatedIntervalChangeDoesNotMutate() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    WorkflowTransitionWriter.createRepeatedAging(
        List.of(draft, review), "Draft", "Review", 15, this::allocate);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            WorkflowTransitionWriter.changeRepeatedInterval(
                List.of(draft, review), "Draft", "Review", 15, 0));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            WorkflowTransitionWriter.changeRepeatedInterval(
                List.of(draft, review), "Draft", "Review", 15, 15));
    assertEquals(15L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals("Repeated aging 15", draft.getAgingTransitions().get(0).getLabel());
  }

  @Test
  void keepsACustomRepeatedLabelWhenTheIntervalChanges() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSAgingTransition aging = new PSAgingTransition();
    aging.setType(PSAgingTransition.PSAgingTypeEnum.REPEATED);
    aging.setInterval(15);
    aging.setToState(2L);
    aging.setLabel("Nudge");
    aging.setTrigger("Nudge");
    aging.setDescription("Keep me");
    draft.addAgingTransition(aging);

    WorkflowTransitionWriter.changeRepeatedInterval(List.of(draft, review), "Draft", "Review", 15, 45);

    PSAgingTransition updated = draft.getAgingTransitions().get(0);
    assertEquals(45L, updated.getInterval());
    assertEquals("Nudge", updated.getLabel());
    assertEquals("Nudge", updated.getTrigger());
    assertEquals("Keep me", updated.getDescription());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.REPEATED, updated.getAgingTypeEnum());
    assertEquals(2L, updated.getToState());
  }

  private PSTransition allocate(PSState source) {
    PSTransition transition = new PSTransition();
    transition.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_TRANSITION, ids.incrementAndGet()));
    transition.setWorkflowId(42);
    transition.setStateId(source.getStateId());
    return transition;
  }

  private static PSState state(long id, String name) {
    PSState state = new PSState();
    state.setStateId(id);
    state.setName(name);
    state.setWorkflowId(42);
    return state;
  }

  private static PSTransition transition(String label, long toState) {
    PSTransition transition = new PSTransition();
    transition.setLabel(label);
    transition.setTrigger(label);
    transition.setToState(toState);
    return transition;
  }
}
