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
}
