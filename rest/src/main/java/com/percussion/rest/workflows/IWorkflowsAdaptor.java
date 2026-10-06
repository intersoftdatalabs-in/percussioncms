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

package com.percussion.rest.workflows;

import com.percussion.rest.contenttypes.NamedObjectRef;
import java.net.URI;
import java.util.List;

/**
 * Adaptor for workflow design associations (SY-06 workflow → content types).
 *
 * <p>CT→workflow associations remain on {@code IContentTypesAdaptor#setAllowedWorkflows} (CD-08).
 */
public interface IWorkflowsAdaptor {

  /**
   * List content types currently associated with the workflow (Admin). No design lock required.
   *
   * @return association list (empty when none), or {@code null} when the workflow is not found
   */
  List<NamedObjectRef> getAllowedContentTypes(URI baseUri, String idOrName);

  /**
   * Full-replace content types allowed for the workflow (Admin, SY-06).
   *
   * <p>Empty list clears associations for this workflow. Acquires a design lock on each affected
   * content type and releases it on save (unlike CD-08, which requires a pre-held CT lock).
   *
   * @param allowedContentTypes replacement set, never {@code null} (empty clears)
   * @return the persisted set (empty when none), or {@code null} when the workflow is not found
   * @throws IllegalArgumentException when a content-type id/name cannot be resolved or idOrName is
   *     invalid
   */
  List<NamedObjectRef> setAllowedContentTypes(
      URI baseUri, String idOrName, List<NamedObjectRef> allowedContentTypes);

  /**
   * Create a stepped workflow (Admin, slice 21 Developer workflow create).
   *
   * <p>Name is required, unique (case-insensitive), and must match workflow-admin rules. States,
   * transitions, and roles come from the product base-workflow template; the optional description
   * is stored on the new workflow.
   *
   * @param body create body, never {@code null}
   * @return the created workflow summary, never {@code null}
   * @throws IllegalArgumentException when the name is blank, too long, or has invalid characters
   * @throws jakarta.ws.rs.WebApplicationException with 409 when a workflow with that name exists
   */
  WorkflowSummary createWorkflow(URI baseUri, WorkflowCreate body);

  /**
   * Copy an existing workflow, including its states and transitions, under a new name (Admin,
   * slice 36).
   *
   * <p>The source is not modified or deleted. {@code body.name} must be unique (case-insensitive)
   * and follow workflow-admin name rules. A non-null description replaces the copied description;
   * a null description keeps the source text. The copy is not marked default.
   *
   * @param idOrName source workflow name, numeric uuid, or guid string
   * @param body new name (required) and optional description
   * @return the new workflow summary
   * @throws IllegalArgumentException when the new name is invalid
   * @throws jakarta.ws.rs.WebApplicationException 404 when the source is missing, 409 when the new
   *     name already exists
   */
  WorkflowSummary copyWorkflow(URI baseUri, String idOrName, WorkflowCreate body);

  /**
   * Update a stepped workflow's description (Admin, slice 21 Developer workflow update).
   *
   * <p>The name is immutable on this description update. Renames use {@link #renameWorkflow}. A
   * {@code null} or missing description is treated as no-op; a non-null description (including the
   * empty string) is stored on the matching workflow.
   *
   * @param idOrName workflow name, numeric uuid, or guid string; must resolve
   * @param body update body, never {@code null}; {@code name} must match {@code idOrName}
   * @return the updated {@link WorkflowSummary}, never {@code null}
   * @throws IllegalArgumentException when {@code idOrName} or {@code body} is invalid
   * @throws jakarta.ws.rs.WebApplicationException with 404 when the workflow is not found
   */
  WorkflowSummary updateWorkflow(URI baseUri, String idOrName, WorkflowUpdate body);

  /**
   * Rename one custom workflow (Admin, slice 60).
   *
   * <p>Delegates to {@code IPSSteppedWorkflowService#updateWorkflow}, which validates the new name
   * against {@code previousWorkflowName}. Description, steps, transitions, and roles are left as
   * they were. Packaged names (Default Workflow, Simple Workflow, Local Content) and the current
   * system default are rejected. {@link #updateWorkflow} still requires its body name to match the
   * path.
   *
   * @param idOrName current workflow name, numeric uuid, or guid string
   * @param body new name, never {@code null}
   * @return summary under the new name
   * @throws IllegalArgumentException when the new name is blank, too long, or has invalid
   *     characters
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged or system default, 404 missing, 409
   *     duplicate name
   */
  WorkflowSummary renameWorkflow(URI baseUri, String idOrName, WorkflowRename body);

  /**
   * Mark one workflow as the system default (Admin, slice 37).
   *
   * <p>The product stores a single default-workflow name. Setting a second workflow replaces that
   * name so the previous workflow is no longer default. Does not rewrite content items.
   *
   * @param idOrName workflow name, numeric uuid, or guid string; must resolve
   * @return the workflow summary with {@code defaultWorkflow} true
   * @throws IllegalArgumentException when {@code idOrName} is blank
   * @throws jakarta.ws.rs.WebApplicationException 403 non-Admin, 404 missing workflow
   */
  WorkflowSummary setDefaultWorkflow(URI baseUri, String idOrName);

  /**
   * Delete a stepped workflow (Admin, slice 21 Developer workflow delete).
   *
   * <p>Delegates to the stepped-workflow editor (acquires and releases the workflow design lock).
   * System workflows cannot be deleted; workflows that still own content items return a
   * conflict.
   *
   * @param idOrName workflow name, numeric uuid, or guid string; must resolve
   * @throws IllegalArgumentException when {@code idOrName} is invalid
   * @throws jakarta.ws.rs.WebApplicationException with 404 when the workflow is not found
   * @throws jakarta.ws.rs.WebApplicationException with 409 when items still belong to the
   *     workflow or the workflow is a system workflow
   */
  void deleteWorkflow(URI baseUri, String idOrName);

  /**
   * Create a step on a non-packaged workflow (Admin, slice 30).
   *
   * @param idOrName workflow name, numeric uuid, or guid string
   * @param body create body, never {@code null}; {@code name} is the new step
   * @return the parent workflow summary after persist
   * @throws IllegalArgumentException when names are invalid
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow
   */
  WorkflowSummary createWorkflowStep(URI baseUri, String idOrName, WorkflowStepWrite body);

  /**
   * Update a step on a non-packaged workflow (Admin, slice 30).
   *
   * @param stepName current step name (path); must exist
   * @param body update body; {@code name} is the new/same step name
   */
  WorkflowSummary updateWorkflowStep(
      URI baseUri, String idOrName, String stepName, WorkflowStepWrite body);

  /**
   * Read-only state/transition graph (slice 32). Packaged workflows are readable; this method does
   * not mutate.
   *
   * @param idOrName workflow name, numeric uuid, or guid string
   * @return graph, never {@code null}
   * @throws jakarta.ws.rs.WebApplicationException 404 when the workflow is not found
   */
  WorkflowGraph getWorkflowGraph(URI baseUri, String idOrName);

  /**
   * Delete one transition between existing steps (Admin, slice 33). Does not delete steps.
   *
   * @param fromStep source step name
   * @param label transition label or trigger
   * @param toStep destination step name; required when the label is not unique on the source step
   * @return the graph after the delete
   * @throws IllegalArgumentException when {@code fromStep} or {@code label} is blank, or the label
   *     is ambiguous without {@code toStep}
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     or transition
   */
  WorkflowGraph deleteWorkflowTransition(
      URI baseUri, String idOrName, String fromStep, String label, String toStep);

  /**
   * Set whether an existing transition requires a comment (Admin, slice 38). Does not create or
   * delete the transition. Editor and Explorer dialogs read the same flag from item transitions.
   *
   * @param fromStep source step name
   * @param label transition label or trigger
   * @param toStep destination step; required when the label is not unique on the source step
   * @param commentRequired true to require a non-blank comment; false leaves the comment optional
   * @return the graph after the update, with {@code commentRequired} on the edge
   */
  WorkflowGraph updateTransitionCommentRequired(
      URI baseUri,
      String idOrName,
      String fromStep,
      String label,
      String toStep,
      boolean commentRequired);

  /**
   * Set how many approvals an existing regular transition requires (Admin, slice 70). Does not
   * create or delete the transition, and does not change the label, destination, comment flag, or
   * default flag. A stored negative count is the each-role sentinel and is not overwritten.
   *
   * @param fromStep source step name
   * @param label transition label or trigger
   * @param toStep destination step; required when the label is not unique on the source step
   * @param approvalsRequired non-negative count; zero is allowed
   * @return the graph after the update, with {@code approvalsRequired} on the edge
   * @throws IllegalArgumentException when the count is negative, the label is ambiguous, the match
   *     is an aging transition, or the stored count is already that number
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     or transition, 409 when the transition uses each-role approval
   */
  WorkflowGraph updateTransitionApprovalsRequired(
      URI baseUri,
      String idOrName,
      String fromStep,
      String label,
      String toStep,
      int approvalsRequired);

  /**
   * Mark one existing regular transition as the default from its step (Admin, slice 71). Other
   * regular transitions from that step lose the flag in the same write. Does not create or delete
   * the transition, and does not change the label, destination, comment flag, or approval count.
   * {@code false} does not clear a stored default.
   *
   * @param fromStep source step name
   * @param label transition label or trigger
   * @param toStep destination step; required when the label is not unique on the source step
   * @param defaultTransition {@code true} to mark this transition; {@code false} refuses a clear
   * @return the graph after the update, with {@code defaultTransition} on regular edges
   * @throws IllegalArgumentException when the value is not a mark, the label is ambiguous, the
   *     match is an aging transition, or the transition is already the only default
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     or transition, 409 when the body would clear the current default
   */
  WorkflowGraph updateTransitionDefault(
      URI baseUri,
      String idOrName,
      String fromStep,
      String label,
      String toStep,
      boolean defaultTransition);

  /**
   * Restrict one existing regular transition so only one workflow role may fire it (Admin, slice
   * 72). The transition must currently allow every role. Does not add a second role, clear the
   * restriction, edit step roles, or change aging transitions.
   *
   * @param fromStep source step name
   * @param label transition label or trigger
   * @param toStep destination step; required when the label is not unique on the source step
   * @param roleName one existing workflow role; the allow-all marker is refused
   * @return the graph after the update, with {@code allowAllRoles} false and that role on the edge
   * @throws IllegalArgumentException when the role name is blank, the label is ambiguous, or the
   *     match is an aging transition
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     transition, or role, 409 when the transition is already restricted or the name is the
   *     allow-all marker
   */
  WorkflowGraph restrictTransitionToOneRole(
      URI baseUri,
      String idOrName,
      String fromStep,
      String label,
      String toStep,
      String roleName);

  /**
   * Add one existing workflow role to a regular transition that is already restricted (Admin, slice
   * 73). Does not perform the first restriction, clear the list, edit step roles, or change aging
   * transitions. The transition stays restricted.
   *
   * @param fromStep source step name
   * @param label transition label or trigger
   * @param toStep destination step; required when the label is not unique on the source step
   * @param roleName one existing workflow role that is not already allowed to fire it
   * @return the graph after the update, with {@code allowAllRoles} false and both the previous
   *     roles and this role on the edge
   * @throws IllegalArgumentException when the role name is blank, the label is ambiguous, or the
   *     match is an aging transition
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     transition, or role, 409 when the transition still allows every role, the role is already
   *     on the list, or the name is the allow-all marker
   */
  WorkflowGraph addTransitionAllowedRole(
      URI baseUri,
      String idOrName,
      String fromStep,
      String label,
      String toStep,
      String roleName);

  /**
   * Clear the role list on one regular transition that is already restricted so every role may
   * fire it again (Admin, slice 74). Does not perform the first restriction, append a role, edit
   * step roles, or change aging transitions.
   *
   * @param fromStep source step name
   * @param label transition label or trigger
   * @param toStep destination step; required when the label is not unique on the source step
   * @return the graph after the update, with {@code allowAllRoles} true and no role list on the
   *     edge
   * @throws IllegalArgumentException when the label is blank or ambiguous, or the match is an
   *     aging transition
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     or transition, 409 when the transition already allows every role
   */
  WorkflowGraph clearTransitionAllowedRoles(
      URI baseUri, String idOrName, String fromStep, String label, String toStep);

  /**
   * Create one transition between existing steps (Admin, slice 31). Does not create steps.
   *
   * @param body {@code from}, {@code to}, and {@code label}; both steps must already exist
   * @return the graph after the insert
   * @throws IllegalArgumentException when a name is blank or invalid
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow or
   *     step, 409 when that from/label/to edge already exists
   */
  WorkflowGraph createWorkflowTransition(URI baseUri, String idOrName, WorkflowTransitionWrite body);

  /**
   * Create one aging transition between existing steps (Admin, slice 57 absolute, slice 75
   * repeated). Does not create steps, change an existing interval, or set comment-required.
   * Omitted or {@code ABSOLUTE} type stays absolute. {@code REPEATED} inserts one repeated aging
   * transition on the same resource. Does not add a second URL.
   *
   * @param body {@code from}, {@code to}, a positive {@code intervalMinutes}, and optional {@code
   *     type}
   * @return the graph after the insert, including the new aging edge
   * @throws IllegalArgumentException when a name is blank, the interval is not positive, or {@code
   *     type} is not absolute or repeated
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow or
   *     step, 409 when that aging edge of the same type and interval already exists
   */
  WorkflowGraph createAbsoluteAgingTransition(
      URI baseUri, String idOrName, WorkflowAgingTransitionWrite body);

  /**
   * Change the minute interval on one existing absolute aging transition (Admin, slice 58). Does
   * not move the destination step, change the aging type, or delete the transition.
   *
   * @param body {@code from}, {@code to}, and {@code intervalMinutes} identify the edge; {@code
   *     newIntervalMinutes} is the replacement
   * @return the graph after the update, with the new minutes on that aging edge
   * @throws IllegalArgumentException when a name is blank or an interval is not a different
   *     positive number of minutes
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     or absolute aging edge, 409 when the new interval already exists
   */
  WorkflowGraph changeAbsoluteAgingInterval(
      URI baseUri, String idOrName, WorkflowAgingIntervalWrite body);

  /**
   * Delete one absolute aging transition (Admin, slice 59). Does not delete steps or regular
   * transitions. A repeated or system-field aging transition that uses the same interval is not
   * removed.
   *
   * @param fromStep source step name
   * @param toStep destination step name
   * @param intervalMinutes current absolute interval, in minutes
   * @return the graph after the delete
   * @throws IllegalArgumentException when a step name is blank, the interval is not positive, or
   *     more than one absolute aging transition uses that interval
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     or absolute aging edge, 409 when the only match is not an absolute aging transition
   */
  WorkflowGraph deleteAbsoluteAgingTransition(
      URI baseUri, String idOrName, String fromStep, String toStep, long intervalMinutes);

  /**
   * Update the label and/or destination of one existing transition (Admin, slice 31). Does not
   * move the source step and does not create steps.
   *
   * @param fromStep source step name
   * @param label current label or trigger
   * @param toStep current destination; required when the label is not unique on the source step
   * @param body new {@code label} and {@code to}
   * @return the graph after the update
   */
  WorkflowGraph updateWorkflowTransition(
      URI baseUri,
      String idOrName,
      String fromStep,
      String label,
      String toStep,
      WorkflowTransitionWrite body);

  /**
   * Delete one step that has no remaining transitions (Admin, slice 34). Does not rewire
   * neighboring steps.
   *
   * @param stepName step to delete; must exist and must not be the source or destination of a
   *     transition
   * @return the graph after the delete
   * @throws IllegalArgumentException when {@code stepName} is blank or invalid
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow or
   *     step, 409 when a transition still references the step
   */
  WorkflowGraph deleteWorkflowStep(URI baseUri, String idOrName, String stepName);

  /**
   * List assignment types for roles already assigned to steps (Admin, slice 61). Includes readers
   * so a reload can show Reader. Does not mutate.
   *
   * @param idOrName workflow name, numeric uuid, or guid string
   * @return the list, never {@code null}; empty when the workflow has no named assigned roles
   * @throws jakarta.ws.rs.WebApplicationException 404 when the workflow is not found
   */
  WorkflowStepRoleAssignmentList listStepRoleAssignments(URI baseUri, String idOrName);

  /**
   * Set Reader or Assignee on one role already assigned to one step (Admin, slice 61).
   *
   * <p>Does not rename the step, add or remove roles, or change notify / inbox flags. Packaged and
   * system-default workflows are rejected. Admin and None assignment types are not changed.
   *
   * @param stepName existing step name
   * @param body role name and READER or ASSIGNEE
   * @return the assignment list after the write, never {@code null}
   * @throws IllegalArgumentException when the role or type is blank, the type is not Reader or
   *     Assignee, or the type is unchanged
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     or role, 409 when the current type is not Reader or Assignee
   */
  WorkflowStepRoleAssignmentList setStepRoleAssignment(
      URI baseUri, String idOrName, String stepName, WorkflowStepRoleAssignmentWrite body);

  /**
   * Turn notify on or off for one role already assigned to one step (Admin, slice 65).
   *
   * <p>Updates {@code ISNOTIFYON} only. Does not change the assignment type, add or remove roles,
   * or edit inbox. Packaged and system-default workflows are rejected. Any assigned role,
   * including Admin and None, may be updated.
   *
   * @param stepName existing step name
   * @param body role name and the new notify flag
   * @return the assignment list after the write, never {@code null}
   * @throws IllegalArgumentException when the role is blank, notify is missing, or the flag is
   *     unchanged
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     or role
   */
  WorkflowStepRoleAssignmentList setStepRoleNotify(
      URI baseUri, String idOrName, String stepName, WorkflowStepRoleNotifyWrite body);

  /**
   * Turn inbox on or off for one Reader or Assignee already assigned to one step (Admin, slice
   * 66).
   *
   * <p>Updates {@code SHOWININBOX} only. Does not change the assignment type, add or remove roles,
   * or edit notify. Packaged and system-default workflows are rejected. Admin and None assignments
   * are not changed.
   *
   * @param stepName existing step name
   * @param body role name and the new inbox flag
   * @return the assignment list after the write, never {@code null}
   * @throws IllegalArgumentException when the role is blank, inbox is missing, or the flag is
   *     unchanged
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     or role, 409 when the current type is not Reader or Assignee
   */
  WorkflowStepRoleAssignmentList setStepRoleInbox(
      URI baseUri, String idOrName, String stepName, WorkflowStepRoleInboxWrite body);

  /**
   * Set the adhoc type on one Reader or Assignee already assigned to one step (Admin, slice 69).
   *
   * <p>Updates the stored adhoc type only ({@code disabled}, {@code enabled}, or {@code
   * anonymous}). Does not change the assignment type, add or remove roles, or edit notify or
   * inbox. Packaged and system-default workflows are rejected. Admin and None assignments are not
   * changed.
   *
   * @param stepName existing step name
   * @param body role name and the new adhoc type
   * @return the assignment list after the write, never {@code null}
   * @throws IllegalArgumentException when the role is blank, the type is missing or invalid, or
   *     the type is unchanged
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     or role, 409 when the current type is not Reader or Assignee
   */
  WorkflowStepRoleAssignmentList setStepRoleAdhoc(
      URI baseUri, String idOrName, String stepName, WorkflowStepRoleAdhocWrite body);

  /**
   * Add one existing workflow role onto one step (Admin, slice 63).
   *
   * <p>Not the assignment-type update. The role must already be a workflow role and must not
   * already be assigned to the step. Assignment type is Reader or Assignee. Notify and inbox stay
   * at the assigned-role entity defaults and are not edited. Does not rename the step or change
   * other roles. Packaged and system-default workflows are rejected.
   *
   * @param stepName existing step name
   * @param body role name and READER or ASSIGNEE
   * @return the assignment list after the write, never {@code null}
   * @throws IllegalArgumentException when the role or type is blank or the type is not Reader or
   *     Assignee
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     or role, 409 when the role is already on the step
   */
  WorkflowStepRoleAssignmentList addStepRole(
      URI baseUri, String idOrName, String stepName, WorkflowStepRoleAdd body);

  /**
   * Remove one Reader or Assignee role from one step (Admin, slice 64).
   *
   * <p>Not the assignment-type update and not the add. The role must already be assigned to the
   * step. Other steps keep that role. Notify and inbox on remaining roles are not edited. Admin
   * and None assignments are not removed. Packaged and system-default workflows are rejected.
   *
   * @param stepName existing step name
   * @param roleName role already assigned to that step
   * @return the assignment list after the delete, never {@code null}
   * @throws IllegalArgumentException when the step or role name is blank
   * @throws jakarta.ws.rs.WebApplicationException 403 packaged/default, 404 missing workflow, step,
   *     or role, 409 when the current type is not Reader or Assignee
   */
  WorkflowStepRoleAssignmentList removeStepRole(
      URI baseUri, String idOrName, String stepName, String roleName);
}
