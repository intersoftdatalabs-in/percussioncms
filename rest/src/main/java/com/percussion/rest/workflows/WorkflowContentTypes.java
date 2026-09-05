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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.contenttypes.NamedObjectRef;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Full-replace body for a workflow's allowed content-type associations (SY-06).
 *
 * <p>Peer of {@code ContentTypeWorkflows} (CD-08 CT→workflow). Jackson root wrap is {@code
 * WorkflowContentTypes}. {@code allowedContentTypes} is required (empty list clears associations
 * for this workflow).
 */
@XmlRootElement(name = "WorkflowContentTypes")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Workflow allowed-content-type association replace body")
public class WorkflowContentTypes {

  @Schema(
      required = true,
      description =
          "Allowed content types (full replace for this workflow). Empty list clears"
              + " associations. Each entry needs name or guid of an existing content type.")
  private List<NamedObjectRef> allowedContentTypes;

  public WorkflowContentTypes() {}

  public List<NamedObjectRef> getAllowedContentTypes() {
    return allowedContentTypes;
  }

  public void setAllowedContentTypes(List<NamedObjectRef> allowedContentTypes) {
    this.allowedContentTypes = allowedContentTypes;
  }
}
