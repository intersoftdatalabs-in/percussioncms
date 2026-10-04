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
 * Rename body for one custom workflow (slice 60).
 *
 * <p>{@code name} is the new workflow name. Description, steps, transitions, and roles are not
 * fields on this body and stay as they were. Jackson root wrap is {@code WorkflowRename}. The
 * description-only {@link WorkflowUpdate} surface still requires its name to match the path.
 */
@XmlRootElement(name = "WorkflowRename")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Workflow rename body (new name only)")
public class WorkflowRename {

  @Schema(
      required = true,
      description =
          "New workflow name (unique case-insensitive; letters, digits, underscore, hyphen,"
              + " space; max 50 chars)")
  private String name;

  public WorkflowRename() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
