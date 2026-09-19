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
 * Create body for a stepped workflow (slice 21 Developer workflow create).
 *
 * <p>Peer of {@code ContentTypeDetail} create (CD-01). {@code name} is required, must be unique
 * (case-insensitive), and must match the workflow-admin rules the stepped editor enforces
 * (letters, digits, underscore, hyphen, space; max 50 chars). Optional {@code description} is
 * stored on the new workflow. States, transitions, and roles come from the product base-workflow
 * template ({@code IPSSteppedWorkflowService#createWorkflow}); full graph design stays outside
 * this surface. Jackson root wrap is {@code WorkflowCreate}.
 */
@XmlRootElement(name = "WorkflowCreate")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Workflow create body (name required, description optional)")
public class WorkflowCreate {

  @Schema(
      required = true,
      description =
          "Workflow name (required, unique case-insensitive; letters, digits, underscore,"
              + " hyphen, space; max 50 chars)")
  private String name;

  @Schema(description = "Optional workflow description stored on the new workflow")
  private String description;

  public WorkflowCreate() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
