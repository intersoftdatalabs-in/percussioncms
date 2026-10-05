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
import com.percussion.services.workflow.data.PSTransitionRole;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.services.workflow.data.PSWorkflowRole;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.workflow.IPSTransitionsContext;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Role list changes for one existing regular transition. {@link #apply} replaces allow-all with
 * exactly one role. {@link #addOne} appends one role to a list that is already restricted. {@link
 * #clear} drops that list so every role may fire the transition again. None of these calls edit
 * step roles or change aging transitions. The label, destination, comment flag, approval count,
 * and default flag stay as they were.
 */
public final class WorkflowTransitionAllowedRoleLimiter {

  private WorkflowTransitionAllowedRoleLimiter() {}

  public static void apply(
      PSWorkflow workflow, String fromStep, String label, String toStep, String roleName) {
    if (workflow == null) {
      throw new WebApplicationException("Workflow not found", 404);
    }
    if (StringUtils.isBlank(fromStep)) {
      throw new IllegalArgumentException("from is required");
    }
    if (StringUtils.isBlank(label)) {
      throw new IllegalArgumentException("label is required");
    }
    if (StringUtils.isBlank(roleName)) {
      throw new IllegalArgumentException("role name is required");
    }
    String from = fromStep.trim();
    String wantLabel = label.trim();
    String wantTo = toStep == null ? "" : toStep.trim();
    String wantRole = roleName.trim();
    if (IPSTransitionsContext.NO_TRANSITION_ROLE_RESTRICTION.equalsIgnoreCase(wantRole)) {
      throw new WebApplicationException(
          "Allow-all is not a role. This call does not clear or keep every role.", 409);
    }

    List<PSState> states = workflow.getStates() != null ? workflow.getStates() : List.of();
    Map<Long, String> names = new LinkedHashMap<>();
    PSState source = null;
    for (PSState state : states) {
      if (state == null || StringUtils.isBlank(state.getName())) {
        continue;
      }
      names.put(state.getStateId(), state.getName().trim());
      if (state.getName().trim().equalsIgnoreCase(from)) {
        source = state;
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
          "Transition role restriction applies only to workflow transitions, not aging transitions");
    }

    PSTransition chosen = regular.get(regularHits.get(0));
    if (chosen == null || !chosen.isAllowAllRoles()) {
      throw new WebApplicationException(
          "Transition is already restricted to specific roles. The role list was not changed.",
          409);
    }

    PSWorkflowRole role = findRole(workflow.getRoles(), wantRole);
    PSTransitionRole link = new PSTransitionRole();
    link.setRoleId(role.getGUID().longValue());
    link.setTransitionId(chosen.getGUID().longValue());
    link.setWorkflowId(workflowId(workflow, role, source));
    chosen.setAllowAllRoles(false);
    chosen.setTransitionRoles(new ArrayList<>(List.of(link)));
    // PSState caches PSTransition copies. setTransitions copies them back onto the Hibernate rows.
    source.setTransitions(regular);
  }

  /**
   * Appends one existing workflow role to a regular transition that is already restricted. Does not
   * replace allow-all, drop roles already stored, or flip the transition back to every role.
   */
  public static void addOne(
      PSWorkflow workflow, String fromStep, String label, String toStep, String roleName) {
    if (workflow == null) {
      throw new WebApplicationException("Workflow not found", 404);
    }
    if (StringUtils.isBlank(fromStep)) {
      throw new IllegalArgumentException("from is required");
    }
    if (StringUtils.isBlank(label)) {
      throw new IllegalArgumentException("label is required");
    }
    if (StringUtils.isBlank(roleName)) {
      throw new IllegalArgumentException("role name is required");
    }
    String from = fromStep.trim();
    String wantLabel = label.trim();
    String wantTo = toStep == null ? "" : toStep.trim();
    String wantRole = roleName.trim();
    if (IPSTransitionsContext.NO_TRANSITION_ROLE_RESTRICTION.equalsIgnoreCase(wantRole)) {
      throw new WebApplicationException(
          "Allow-all is not a role. This call does not clear the role list.", 409);
    }

    List<PSState> states = workflow.getStates() != null ? workflow.getStates() : List.of();
    Map<Long, String> names = new LinkedHashMap<>();
    PSState source = null;
    for (PSState state : states) {
      if (state == null || StringUtils.isBlank(state.getName())) {
        continue;
      }
      names.put(state.getStateId(), state.getName().trim());
      if (state.getName().trim().equalsIgnoreCase(from)) {
        source = state;
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
          "Transition role restriction applies only to workflow transitions, not aging transitions");
    }

    PSTransition chosen = regular.get(regularHits.get(0));
    if (chosen == null || chosen.isAllowAllRoles()) {
      throw new WebApplicationException(
          "Transition still allows every role. The role list was not changed.", 409);
    }

    PSWorkflowRole role = findRole(workflow.getRoles(), wantRole);
    long roleId = role.getGUID().longValue();
    List<PSTransitionRole> current =
        chosen.getTransitionRoles() != null ? chosen.getTransitionRoles() : List.of();
    for (PSTransitionRole existing : current) {
      if (existing != null && existing.getRoleId() == roleId) {
        throw new WebApplicationException(
            "That role may already fire this transition. The role list was not changed.", 409);
      }
    }
    List<PSTransitionRole> next = new ArrayList<>();
    for (PSTransitionRole existing : current) {
      if (existing != null) {
        next.add(existing);
      }
    }
    PSTransitionRole link = new PSTransitionRole();
    link.setRoleId(roleId);
    link.setTransitionId(chosen.getGUID().longValue());
    link.setWorkflowId(workflowId(workflow, role, source));
    next.add(link);
    chosen.setAllowAllRoles(false);
    chosen.setTransitionRoles(next);
    source.setTransitions(regular);
  }

  /**
   * Clears the stored role list on one regular transition that is already restricted so every role
   * may fire it. Does not perform the first restriction, append a role, or leave a single-role
   * limit in place. An allow-all transition is refused and is not rewritten.
   */
  public static void clear(PSWorkflow workflow, String fromStep, String label, String toStep) {
    if (workflow == null) {
      throw new WebApplicationException("Workflow not found", 404);
    }
    if (StringUtils.isBlank(fromStep)) {
      throw new IllegalArgumentException("from is required");
    }
    if (StringUtils.isBlank(label)) {
      throw new IllegalArgumentException("label is required");
    }
    String from = fromStep.trim();
    String wantLabel = label.trim();
    String wantTo = toStep == null ? "" : toStep.trim();

    List<PSState> states = workflow.getStates() != null ? workflow.getStates() : List.of();
    Map<Long, String> names = new LinkedHashMap<>();
    PSState source = null;
    for (PSState state : states) {
      if (state == null || StringUtils.isBlank(state.getName())) {
        continue;
      }
      names.put(state.getStateId(), state.getName().trim());
      if (state.getName().trim().equalsIgnoreCase(from)) {
        source = state;
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
          "Transition role restriction applies only to workflow transitions, not aging transitions");
    }

    PSTransition chosen = regular.get(regularHits.get(0));
    if (chosen == null || chosen.isAllowAllRoles()) {
      throw new WebApplicationException(
          "Transition already allows every role. The role list was not changed.", 409);
    }
    chosen.setAllowAllRoles(true);
    chosen.setTransitionRoles(new ArrayList<>());
    source.setTransitions(regular);
  }

  private static long workflowId(PSWorkflow workflow, PSWorkflowRole role, PSState source) {
    IPSGuid guid = workflow.getGUID();
    if (guid != null) {
      return guid.longValue();
    }
    if (role.getWorkflowId() != 0L) {
      return role.getWorkflowId();
    }
    return source.getWorkflowId();
  }

  private static PSWorkflowRole findRole(List<PSWorkflowRole> roles, String roleName) {
    if (roles != null) {
      for (PSWorkflowRole role : roles) {
        if (role != null
            && role.getGUID() != null
            && StringUtils.isNotBlank(role.getName())
            && role.getName().trim().equalsIgnoreCase(roleName)) {
          return role;
        }
      }
    }
    throw new WebApplicationException("Role not found: " + roleName, 404);
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
    if (transitions == null) {
      return hits;
    }
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
