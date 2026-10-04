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
import com.percussion.services.workflow.data.PSAgingTransition.PSAgingTypeEnum;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSTransitionBase;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Removes one workflow transition without deleting states.
 *
 * <p>{@link #removeOne} matches a regular or aging transition by label. When more than one
 * transition on the source step shares the label, {@code toStep} is required. {@link
 * #removeAbsoluteAging} removes only one absolute aging transition identified by source,
 * destination, and minute interval, and leaves regular transitions in place.
 */
public final class WorkflowTransitionRemover {

  private WorkflowTransitionRemover() {}

  public static void removeOne(
      List<PSState> states, String fromStep, String label, String toStep) {
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
          "More than one transition named "
              + wantLabel
              + " on step "
              + from
              + "; specify to");
    }
    if (!regularHits.isEmpty()) {
      regular.remove((int) regularHits.get(0));
      source.setTransitions(regular);
    } else {
      aging.remove((int) agingHits.get(0));
      source.setAgingTransitions(aging);
    }
  }

  /**
   * Removes one absolute aging transition. Does not remove regular transitions, steps, or a
   * repeated or system-field aging transition that happens to use the same interval.
   */
  public static void removeAbsoluteAging(
      List<PSState> states, String fromStep, String toStep, long intervalMinutes) {
    if (intervalMinutes <= 0) {
      throw new IllegalArgumentException("interval must be a positive number of minutes");
    }
    if (StringUtils.isBlank(fromStep) || StringUtils.isBlank(toStep)) {
      throw new IllegalArgumentException("from and to are required");
    }
    String from = fromStep.trim();
    String to = toStep.trim();
    PSState source = null;
    Long destId = null;
    if (states != null) {
      for (PSState state : states) {
        if (state == null || StringUtils.isBlank(state.getName())) {
          continue;
        }
        String name = state.getName().trim();
        if (name.equalsIgnoreCase(from)) {
          source = state;
        }
        if (name.equalsIgnoreCase(to)) {
          destId = state.getStateId();
        }
      }
    }
    if (source == null) {
      throw new WebApplicationException("Workflow step not found: " + from, 404);
    }
    if (destId == null) {
      throw new WebApplicationException("Workflow step not found: " + to, 404);
    }
    int regularBefore = regularCount(source);
    List<PSAgingTransition> aging = safeAging(source);
    int matchIndex = -1;
    boolean nonAbsolute = false;
    for (int i = 0; i < aging.size(); i++) {
      PSAgingTransition existing = aging.get(i);
      if (existing == null || existing.getToState() != destId.longValue()) {
        continue;
      }
      if (existing.getInterval() != intervalMinutes) {
        continue;
      }
      PSAgingTypeEnum type = existing.getAgingTypeEnum();
      if (type != null && type != PSAgingTypeEnum.ABSOLUTE) {
        nonAbsolute = true;
        continue;
      }
      if (matchIndex >= 0) {
        throw new IllegalArgumentException(
            "More than one absolute aging transition uses that interval");
      }
      matchIndex = i;
    }
    if (matchIndex < 0) {
      if (nonAbsolute) {
        throw new WebApplicationException("Only an absolute aging transition can be deleted", 409);
      }
      throw new WebApplicationException("Workflow aging transition not found", 404);
    }
    aging.remove(matchIndex);
    source.setAgingTransitions(aging);
    int regularAfter = regularCount(source);
    if (regularBefore >= 0 && regularAfter >= 0 && regularAfter != regularBefore) {
      throw new IllegalStateException(
          "Deleting an aging transition must not remove a regular transition");
    }
  }

  private static int regularCount(PSState state) {
    try {
      List<PSTransition> regular = state.getTransitions();
      return regular == null ? 0 : regular.size();
    } catch (RuntimeException ex) {
      return -1;
    }
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
      if (transition == null || !labelMatches(transition, wantLabel)) {
        continue;
      }
      String dest = names.get(transition.getToState());
      if (StringUtils.isNotBlank(wantTo)) {
        if (dest == null || !dest.equalsIgnoreCase(wantTo)) {
          continue;
        }
      }
      hits.add(i);
    }
    return hits;
  }

  private static boolean labelMatches(PSTransitionBase transition, String wantLabel) {
    String label = StringUtils.defaultString(transition.getLabel()).trim();
    String trigger = StringUtils.defaultString(transition.getTrigger()).trim();
    return wantLabel.equalsIgnoreCase(label) || wantLabel.equalsIgnoreCase(trigger);
  }
}
