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
package com.percussion.itemmanagement.workflow;

import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSWorkflow;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ad-hoc assignee rules for an editor workflow transition (#5163 / parent #4532).
 *
 * <p>A trigger requires assignees when its destination state has ad-hoc assignment enabled
 * ({@link PSState#isAdhocEnabled()}). The comma-separated {@code adhocAssignees} query value is
 * the same list {@code WorkflowActionsPanel} already sends. Blank tokens are dropped. An empty
 * list is not a server error — callers that must not fire an empty required transition stop
 * before the request.
 */
public final class EditorWorkflowAdhocRules {

  private EditorWorkflowAdhocRules() {}

  /**
   * @param raw comma-separated user names; {@code null} or blank is an empty list
   * @return trimmed unique names in first-seen order, never {@code null}
   */
  public static List<String> parseAdhocAssignees(String raw) {
    List<String> out = new ArrayList<>();
    if (raw == null || raw.isBlank()) {
      return out;
    }
    Set<String> seen = new LinkedHashSet<>();
    for (String part : raw.split(",", -1)) {
      if (part == null) {
        continue;
      }
      String name = part.trim();
      if (name.isEmpty() || !seen.add(name)) {
        continue;
      }
      out.add(name);
    }
    return out;
  }

  /** Destination state of {@code transition}, or {@code null} when the workflow has no such state. */
  public static PSState destinationState(PSWorkflow workflow, PSTransition transition) {
    if (workflow == null || transition == null || workflow.getStates() == null) {
      return null;
    }
    long toState = transition.getToState();
    for (PSState state : workflow.getStates()) {
      if (state != null && state.getStateId() == toState) {
        return state;
      }
    }
    return null;
  }

  /**
   * True when the destination state's roles enable ad-hoc assignment for an assignee or admin.
   * A missing state, a reader-only role, or ad-hoc disabled does not require assignees.
   */
  public static boolean triggerRequiresAssignees(PSWorkflow workflow, PSTransition transition) {
    if (transition == null) {
      return false;
    }
    String trigger = transition.getTrigger();
    if (trigger == null || trigger.isBlank()) {
      return false;
    }
    PSState toState = destinationState(workflow, transition);
    if (toState == null || toState.getAssignedRoles() == null) {
      return false;
    }
    return toState.isAdhocEnabled();
  }
}
