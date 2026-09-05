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
}
