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

import com.percussion.services.workflow.data.PSAgingTransition;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSTransitionBase;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Removes one workflow state only when no regular or aging transition still names it.
 *
 * <p>Unlike the workflow-admin editor, this path does not rewire Submit/Reject around the step.
 */
public final class WorkflowStepRemover {

  private WorkflowStepRemover() {}

  public static void removeUnreferenced(List<PSState> states, String stepName) {
    if (StringUtils.isBlank(stepName)) {
      throw new IllegalArgumentException("stepName is required");
    }
    String want = stepName.trim();
    if (states == null) {
      throw new WebApplicationException("Workflow step not found: " + want, 404);
    }
    PSState target = null;
    for (PSState state : states) {
      if (state == null || StringUtils.isBlank(state.getName())) {
        continue;
      }
      if (state.getName().trim().equalsIgnoreCase(want)) {
        target = state;
        break;
      }
    }
    if (target == null) {
      throw new WebApplicationException("Workflow step not found: " + want, 404);
    }
    long stateId = target.getStateId();
    if (hasAny(copyRegular(target))
        || hasAny(copyAging(target))
        || referencedByOther(states, target, stateId)) {
      throw new WebApplicationException(
          "Workflow step is still referenced by a transition: " + want, 409);
    }
    if (!states.remove(target)) {
      throw new WebApplicationException("Workflow step not found: " + want, 404);
    }
  }

  private static boolean referencedByOther(List<PSState> states, PSState target, long stateId) {
    for (PSState state : states) {
      if (state == null || state == target) {
        continue;
      }
      if (pointsAt(copyRegular(state), stateId) || pointsAt(copyAging(state), stateId)) {
        return true;
      }
    }
    return false;
  }

  private static boolean pointsAt(List<? extends PSTransitionBase> transitions, long stateId) {
    for (PSTransitionBase transition : transitions) {
      if (transition != null && transition.getToState() == stateId) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasAny(List<? extends PSTransitionBase> transitions) {
    for (PSTransitionBase transition : transitions) {
      if (transition != null) {
        return true;
      }
    }
    return false;
  }

  private static List<PSTransition> copyRegular(PSState state) {
    try {
      List<PSTransition> transitions = state.getTransitions();
      return transitions != null ? new ArrayList<>(transitions) : new ArrayList<>();
    } catch (RuntimeException ex) {
      return new ArrayList<>();
    }
  }

  private static List<PSAgingTransition> copyAging(PSState state) {
    try {
      List<PSAgingTransition> aging = state.getAgingTransitions();
      return aging != null ? new ArrayList<>(aging) : new ArrayList<>();
    } catch (RuntimeException ex) {
      return new ArrayList<>();
    }
  }
}
