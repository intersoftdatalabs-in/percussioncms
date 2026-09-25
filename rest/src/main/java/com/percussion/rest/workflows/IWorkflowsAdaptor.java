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
   * <p>The name is immutable from this surface; renaming and step/transitions/roles editing stay
   * on the workflow-admin editor. A {@code null} or missing description is treated as no-op; a
   * non-null description (including the empty string) is stored on the matching workflow.
   *
   * @param idOrName workflow name, numeric uuid, or guid string; must resolve
   * @param body update body, never {@code null}; {@code name} must match {@code idOrName}
   * @return the updated {@link WorkflowSummary}, never {@code null}
   * @throws IllegalArgumentException when {@code idOrName} or {@code body} is invalid
   * @throws jakarta.ws.rs.WebApplicationException with 404 when the workflow is not found
   */
  WorkflowSummary updateWorkflow(URI baseUri, String idOrName, WorkflowUpdate body);

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
}
