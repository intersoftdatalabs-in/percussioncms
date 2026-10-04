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
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Creates or updates one workflow transition between existing steps. Does not add or delete
 * states. Absolute aging creates go through {@link #createAbsoluteAging}. Interval changes go
 * through {@link #changeAbsoluteInterval}. Comment-required is not used for aging.
 */
public final class WorkflowTransitionWriter {

  static final int NAME_MAX = 50;

  private WorkflowTransitionWriter() {}

  /** Allocates a persisted transition id for {@code source}. Must not return null. */
  @FunctionalInterface
  public interface TransitionFactory {
    PSTransition allocate(PSState source);
  }

  public static void create(
      List<PSState> states, String fromStep, String toStep, String label, TransitionFactory factory) {
    if (factory == null) {
      throw new IllegalStateException("Transition id factory is required");
    }
    String from = requireName(fromStep, "from");
    String to = requireName(toStep, "to");
    String wantLabel = requireName(label, "label");
    Index index = index(states);
    PSState source = requireStep(index, from);
    PSState dest = requireStep(index, to);
    if (sameEdge(source, wantLabel, dest.getName(), index.names)) {
      throw new WebApplicationException("Workflow transition already exists: " + wantLabel, 409);
    }
    PSTransition fresh = factory.allocate(source);
    if (fresh == null) {
      throw new IllegalStateException("Could not allocate a workflow transition id");
    }
    fresh.setStateId(source.getStateId());
    if (fresh.getWorkflowId() == 0 && source.getWorkflowId() != 0) {
      fresh.setWorkflowId(source.getWorkflowId());
    }
    fresh.setToState(dest.getStateId());
    fresh.setLabel(wantLabel);
    fresh.setTrigger(wantLabel);
    if (StringUtils.isBlank(fresh.getDescription())) {
      fresh.setDescription(wantLabel);
    }
    fresh.setAllowAllRoles(true);
    if (fresh.getApprovals() < 1) {
      fresh.setApprovals(1);
    }
    source.addTransition(fresh);
  }

  /**
   * Inserts one absolute aging transition. Interval is minutes. Does not add a regular transition
   * and does not add or delete states.
   */
  public static void createAbsoluteAging(
      List<PSState> states,
      String fromStep,
      String toStep,
      long intervalMinutes,
      TransitionFactory factory) {
    if (factory == null) {
      throw new IllegalStateException("Transition id factory is required");
    }
    if (intervalMinutes <= 0) {
      throw new IllegalArgumentException("interval must be a positive number of minutes");
    }
    String from = requireName(fromStep, "from");
    String to = requireName(toStep, "to");
    String label = absoluteAgingLabel(intervalMinutes);
    Index index = index(states);
    PSState source = requireStep(index, from);
    PSState dest = requireStep(index, to);
    if (sameEdge(source, label, dest.getName(), index.names)
        || sameAbsoluteInterval(source, dest.getStateId(), intervalMinutes)) {
      throw new WebApplicationException("Workflow aging transition already exists: " + label, 409);
    }
    PSTransition allocated = factory.allocate(source);
    if (allocated == null) {
      throw new IllegalStateException("Could not allocate a workflow transition id");
    }
    PSAgingTransition aging = new PSAgingTransition();
    aging.setGUID(allocated.getGUID());
    long workflowId = allocated.getWorkflowId();
    if (workflowId == 0) {
      workflowId = source.getWorkflowId();
    }
    aging.setWorkflowId(workflowId);
    aging.setStateId(source.getStateId());
    aging.setToState(dest.getStateId());
    aging.setType(PSAgingTypeEnum.ABSOLUTE);
    aging.setInterval(intervalMinutes);
    aging.setLabel(label);
    aging.setTrigger(label);
    if (StringUtils.isBlank(aging.getDescription())) {
      aging.setDescription(label);
    }
    source.addAgingTransition(aging);
  }

  static String absoluteAgingLabel(long intervalMinutes) {
    String label = "Aging " + intervalMinutes;
    if (label.length() > NAME_MAX) {
      throw new IllegalArgumentException("interval is too large");
    }
    return label;
  }

  private static boolean sameAbsoluteInterval(PSState source, long toStateId, long intervalMinutes) {
    for (PSAgingTransition existing : copyAging(source)) {
      if (existing == null || existing.getToState() != toStateId) {
        continue;
      }
      if (existing.getInterval() != intervalMinutes) {
        continue;
      }
      PSAgingTypeEnum type = existing.getAgingTypeEnum();
      if (type == null || type == PSAgingTypeEnum.ABSOLUTE) {
        return true;
      }
    }
    return false;
  }

  /**
   * Changes the minute interval on one existing absolute aging transition. Does not change the
   * destination, the aging type, or the set of steps. A generated {@code Aging N} label, trigger,
   * and description are rewritten to the new interval. A custom label is left alone.
   */
  public static void changeAbsoluteInterval(
      List<PSState> states,
      String fromStep,
      String toStep,
      long currentMinutes,
      long newMinutes) {
    if (currentMinutes <= 0 || newMinutes <= 0) {
      throw new IllegalArgumentException("interval must be a positive number of minutes");
    }
    if (currentMinutes == newMinutes) {
      throw new IllegalArgumentException("new interval must differ from the current interval");
    }
    String from = requireName(fromStep, "from");
    String to = requireName(toStep, "to");
    String nextLabel = absoluteAgingLabel(newMinutes);
    Index index = index(states);
    PSState source = requireStep(index, from);
    PSState dest = requireStep(index, to);
    PSAgingTransition hit = locateAbsolute(source, dest.getStateId(), currentMinutes);
    String oldLabel = absoluteAgingLabel(currentMinutes);
    String previousLabel = StringUtils.defaultString(hit.getLabel()).trim();
    boolean renameLabel = previousLabel.equalsIgnoreCase(oldLabel);
    if (sameAbsoluteInterval(source, dest.getStateId(), newMinutes)
        || (renameLabel
            && otherEdge(source, new Hit(true, hit), nextLabel, dest.getName(), index.names))) {
      throw new WebApplicationException(
          "Workflow aging transition already exists: " + nextLabel, 409);
    }
    String previousTrigger = StringUtils.defaultString(hit.getTrigger()).trim();
    String previousDescription = StringUtils.defaultString(hit.getDescription()).trim();
    hit.setInterval(newMinutes);
    if (previousLabel.equalsIgnoreCase(oldLabel)) {
      hit.setLabel(nextLabel);
    }
    if (previousTrigger.equalsIgnoreCase(oldLabel)
        || (previousTrigger.isEmpty() && previousLabel.equalsIgnoreCase(oldLabel))) {
      hit.setTrigger(nextLabel);
    }
    if (previousDescription.equalsIgnoreCase(oldLabel)
        || (previousDescription.isEmpty() && previousLabel.equalsIgnoreCase(oldLabel))) {
      hit.setDescription(nextLabel);
    }
    source.setAgingTransitions(copyAging(source));
  }

  private static PSAgingTransition locateAbsolute(
      PSState source, long toStateId, long intervalMinutes) {
    PSAgingTransition match = null;
    boolean nonAbsolute = false;
    for (PSAgingTransition existing : copyAging(source)) {
      if (existing == null || existing.getToState() != toStateId) {
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
      if (match != null) {
        throw new IllegalArgumentException(
            "More than one absolute aging transition uses that interval");
      }
      match = existing;
    }
    if (match == null) {
      if (nonAbsolute) {
        throw new IllegalArgumentException("Only an absolute aging interval can be changed");
      }
      throw new WebApplicationException("Workflow aging transition not found", 404);
    }
    return match;
  }

  public static void update(
      List<PSState> states,
      String fromStep,
      String currentLabel,
      String currentTo,
      String newLabel,
      String newTo) {
    String from = requireName(fromStep, "from");
    String wantLabel = requireName(currentLabel, "label");
    String wantTo = currentTo == null ? "" : currentTo.trim();
    String nextLabel = requireName(newLabel, "label");
    String nextTo = requireName(newTo, "to");
    Index index = index(states);
    PSState source = requireStep(index, from);
    PSState dest = requireStep(index, nextTo);
    Hit hit = locate(source, wantLabel, wantTo, index.names);
    if (otherEdge(source, hit, nextLabel, dest.getName(), index.names)) {
      throw new WebApplicationException("Workflow transition already exists: " + nextLabel, 409);
    }
    String oldLabel = StringUtils.defaultString(hit.transition.getLabel()).trim();
    String oldTrigger = StringUtils.defaultString(hit.transition.getTrigger()).trim();
    hit.transition.setLabel(nextLabel);
    if (oldTrigger.isEmpty() || oldTrigger.equalsIgnoreCase(oldLabel)) {
      hit.transition.setTrigger(nextLabel);
    }
    hit.transition.setToState(dest.getStateId());
    if (hit.aging) {
      source.setAgingTransitions(copyAging(source));
    } else {
      List<PSTransition> regular = new ArrayList<>();
      if (source.getTransitions() != null) {
        regular.addAll(source.getTransitions());
      }
      source.setTransitions(regular);
    }
  }

  private static boolean sameEdge(PSState source, String label, String toName, Map<Long, String> names) {
    return countMatches(source, label, toName, names) > 0;
  }

  private static boolean otherEdge(
      PSState source, Hit self, String label, String toName, Map<Long, String> names) {
    for (PSTransition transition : regular(source)) {
      if (transition == null || transition == self.transition) {
        continue;
      }
      if (edgeMatches(transition, label, toName, names)) {
        return true;
      }
    }
    for (PSAgingTransition transition : copyAging(source)) {
      if (transition == null || transition == self.transition) {
        continue;
      }
      if (edgeMatches(transition, label, toName, names)) {
        return true;
      }
    }
    return false;
  }

  private static int countMatches(
      PSState source, String label, String toName, Map<Long, String> names) {
    int total = 0;
    for (PSTransition transition : regular(source)) {
      if (transition != null && edgeMatches(transition, label, toName, names)) {
        total++;
      }
    }
    for (PSAgingTransition transition : copyAging(source)) {
      if (transition != null && edgeMatches(transition, label, toName, names)) {
        total++;
      }
    }
    return total;
  }

  private static boolean edgeMatches(
      PSTransitionBase transition, String label, String toName, Map<Long, String> names) {
    if (!labelMatches(transition, label)) {
      return false;
    }
    String dest = names.get(transition.getToState());
    return dest != null && dest.equalsIgnoreCase(toName);
  }

  private static Hit locate(PSState source, String label, String toName, Map<Long, String> names) {
    List<Hit> hits = new ArrayList<>();
    for (PSTransition transition : regular(source)) {
      if (transition != null && identityMatches(transition, label, toName, names)) {
        hits.add(new Hit(false, transition));
      }
    }
    for (PSAgingTransition transition : copyAging(source)) {
      if (transition != null && identityMatches(transition, label, toName, names)) {
        hits.add(new Hit(true, transition));
      }
    }
    if (hits.isEmpty()) {
      throw new WebApplicationException("Workflow transition not found: " + label, 404);
    }
    if (hits.size() > 1) {
      throw new IllegalArgumentException(
          "More than one transition named " + label + " on step " + source.getName() + "; specify to");
    }
    return hits.get(0);
  }

  private static boolean identityMatches(
      PSTransitionBase transition, String label, String toName, Map<Long, String> names) {
    if (!labelMatches(transition, label)) {
      return false;
    }
    if (StringUtils.isNotBlank(toName)) {
      String dest = names.get(transition.getToState());
      return dest != null && dest.equalsIgnoreCase(toName);
    }
    return true;
  }

  private static boolean labelMatches(PSTransitionBase transition, String wantLabel) {
    String label = StringUtils.defaultString(transition.getLabel()).trim();
    String trigger = StringUtils.defaultString(transition.getTrigger()).trim();
    return wantLabel.equalsIgnoreCase(label) || wantLabel.equalsIgnoreCase(trigger);
  }

  private static List<PSTransition> regular(PSState source) {
    List<PSTransition> regular = new ArrayList<>();
    if (source.getTransitions() != null) {
      regular.addAll(source.getTransitions());
    }
    return regular;
  }

  private static List<PSAgingTransition> copyAging(PSState state) {
    try {
      List<PSAgingTransition> aging = state.getAgingTransitions();
      return aging != null ? new ArrayList<>(aging) : new ArrayList<>();
    } catch (RuntimeException ex) {
      return new ArrayList<>();
    }
  }

  private static PSState requireStep(Index index, String name) {
    PSState state = index.byName.get(name.toLowerCase(Locale.ROOT));
    if (state == null) {
      throw new WebApplicationException("Workflow step not found: " + name, 404);
    }
    return state;
  }

  private static Index index(List<PSState> states) {
    Map<Long, String> names = new LinkedHashMap<>();
    Map<String, PSState> byName = new LinkedHashMap<>();
    if (states != null) {
      for (PSState state : states) {
        if (state == null || StringUtils.isBlank(state.getName())) {
          continue;
        }
        String name = state.getName().trim();
        names.put(state.getStateId(), name);
        byName.putIfAbsent(name.toLowerCase(Locale.ROOT), state);
      }
    }
    return new Index(names, byName);
  }

  static String requireName(String raw, String field) {
    String name = raw == null ? "" : raw.trim();
    if (name.isEmpty()) {
      throw new IllegalArgumentException(field + " is required");
    }
    if (name.indexOf('*') >= 0 || name.indexOf('%') >= 0) {
      throw new IllegalArgumentException(field + " must not contain wildcards: " + name);
    }
    if (name.length() > NAME_MAX) {
      throw new IllegalArgumentException(field + " cannot have more than " + NAME_MAX + " characters");
    }
    if (!name.matches("[\\s\\w-]+")) {
      throw new IllegalArgumentException(
          "Invalid character in "
              + field
              + ". Characters allowed are: a-z, 0-9, -, _ and [space].");
    }
    return name;
  }

  private record Index(Map<Long, String> names, Map<String, PSState> byName) {}

  private record Hit(boolean aging, PSTransitionBase transition) {}
}
