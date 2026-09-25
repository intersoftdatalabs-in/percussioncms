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
import com.percussion.services.workflow.data.PSTransition.PSWorkflowCommentEnum;
import com.percussion.services.workflow.data.PSTransitionBase;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Sets {@link PSTransition#setRequiresComment} on one existing transition. Does not create or
 * delete transitions. Aging transitions have no comment-required flag.
 */
public final class WorkflowTransitionCommentFlag {

  private WorkflowTransitionCommentFlag() {}

  public static void apply(
      List<PSState> states, String fromStep, String label, String toStep, boolean commentRequired) {
    if (StringUtils.isBlank(fromStep)) {
      throw new IllegalArgumentException("from is required");
    }
    if (StringUtils.isBlank(label)) {
      throw new IllegalArgumentException("label is required");
    }
    String from = fromStep.trim();
    String wantLabel = label.trim();
    String wantTo = toStep == null ? "" : toStep.trim();

    Map<Long, String> names = new LinkedHashMap<>();
    PSState source = null;
    if (states != null) {
      for (PSState state : states) {
        if (state == null || StringUtils.isBlank(state.getName())) {
          continue;
        }
        names.put(state.getStateId(), state.getName().trim());
        if (state.getName().trim().equalsIgnoreCase(from)) {
          source = state;
        }
      }
    }
    if (source == null) {
      throw new WebApplicationException("Workflow step not found: " + from, 404);
    }

    List<PSTransition> regular = new ArrayList<>();
    if (source.getTransitions() != null) {
      regular.addAll(source.getTransitions());
    }
    List<PSAgingTransition> aging = safeAging(source);
    List<Integer> regularHits = matching(regular, wantLabel, wantTo, names);
    List<Integer> agingHits = matching(aging, wantLabel, wantTo, names);
    int total = regularHits.size() + agingHits.size();
    if (total == 0) {
      throw new WebApplicationException("Workflow transition not found: " + wantLabel, 404);
    }
    if (total > 1) {
      throw new IllegalArgumentException(
          "More than one transition named " + wantLabel + " on step " + from + "; specify to");
    }
    if (regularHits.isEmpty()) {
      throw new IllegalArgumentException(
          "Comment requirement applies only to workflow transitions, not aging transitions");
    }
    PSTransition transition = regular.get(regularHits.get(0));
    transition.setRequiresComment(
        commentRequired ? PSWorkflowCommentEnum.REQUIRED : PSWorkflowCommentEnum.OPTIONAL);
    source.setTransitions(regular);
  }

  private static List<PSAgingTransition> safeAging(PSState state) {
    try {
      List<PSAgingTransition> aging = state.getAgingTransitions();
      return aging != null ? new ArrayList<>(aging) : new ArrayList<>();
    } catch (RuntimeException ex) {
      return new ArrayList<>();
    }
  }

  private static List<Integer> matching(
      List<? extends PSTransitionBase> transitions,
      String wantLabel,
      String wantTo,
      Map<Long, String> names) {
    List<Integer> hits = new ArrayList<>();
    for (int i = 0; i < transitions.size(); i++) {
      PSTransitionBase transition = transitions.get(i);
      if (transition == null) {
        continue;
      }
      String transitionLabel = StringUtils.defaultString(transition.getLabel()).trim();
      String trigger = StringUtils.defaultString(transition.getTrigger()).trim();
      if (!wantLabel.equalsIgnoreCase(transitionLabel) && !wantLabel.equalsIgnoreCase(trigger)) {
        continue;
      }
      if (StringUtils.isNotBlank(wantTo)) {
        String dest = names.get(transition.getToState());
        if (dest == null || !dest.equalsIgnoreCase(wantTo)) {
          continue;
        }
      }
      hits.add(i);
    }
    return hits;
  }
}
