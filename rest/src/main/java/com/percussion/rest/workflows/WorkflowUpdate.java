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
 * Update body for a stepped workflow (slice 21 Developer workflow update).
 *
 * <p>Name must match the path {@code idOrName} (renames are out of scope for this surface; full
 * graph design stays outside the Developer catalog). Description is replaced when given (empty
 * string clears). States, transitions, and roles are managed by the workflow-admin editor; this
 * slice is intentionally limited to description updates. Jackson root wrap is {@code
 * WorkflowUpdate}.
 */
@XmlRootElement(name = "WorkflowUpdate")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Workflow update body (description only; name is immutable from this surface)")
public class WorkflowUpdate {

  @Schema(
      required = true,
      description =
          "Workflow name echoed from the path (must match idOrName; rename is not supported on"
              + " this surface)")
  private String name;

  @Schema(
      description =
          "Replacement description (empty string clears). Omitted/null leaves the stored value"
              + " untouched.")
  private String description;

  public WorkflowUpdate() {}

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
