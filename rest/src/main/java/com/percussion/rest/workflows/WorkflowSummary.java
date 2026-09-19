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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Created-workflow summary returned by {@code POST /services/workflows} (slice 21).
 *
 * <p>Rest wire projection of the stepped editor's {@code PSUiWorkflow} for the create path. The
 * created workflow also appears on the stepped catalog ({@code GET
 * /services/workflowmanagement/workflows/metadata}). Jackson root wrap is {@code WorkflowSummary}.
 */
@XmlRootElement(name = "WorkflowSummary")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Created workflow summary (name, description, default flag)")
public class WorkflowSummary {

  @Schema(description = "Workflow name (catalog key for GET .../workflows/{name})")
  private String workflowName;

  @Schema(description = "Workflow description stored on the new workflow")
  private String workflowDescription;

  @Schema(description = "Whether this workflow is the system default")
  private boolean defaultWorkflow;

  public WorkflowSummary() {}

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public String getWorkflowDescription() {
    return workflowDescription;
  }

  public void setWorkflowDescription(String workflowDescription) {
    this.workflowDescription = workflowDescription;
  }

  public boolean isDefaultWorkflow() {
    return defaultWorkflow;
  }

  public void setDefaultWorkflow(boolean defaultWorkflow) {
    this.defaultWorkflow = defaultWorkflow;
  }
}
