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
 * Create/update body for one workflow transition between existing steps (slice 31).
 *
 * <p>On create, {@code from}, {@code to}, and {@code label} are required. On update, {@code label}
 * and {@code to} are the new values; the current edge is identified by query parameters. Does not
 * create or delete steps. Jackson root wrap is {@code WorkflowTransitionWrite}.
 */
@XmlRootElement(name = "WorkflowTransitionWrite")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Workflow transition create/update body between existing steps")
public class WorkflowTransitionWrite {

  @Schema(description = "Source step name. Required on create. Ignored on update (query from).")
  private String from;

  @Schema(required = true, description = "Destination step name. Must already exist on the workflow.")
  private String to;

  @Schema(
      required = true,
      description =
          "Transition label (and trigger when the trigger matches the label). Letters, digits,"
              + " underscore, hyphen, and space; max 50 characters.")
  private String label;

  public WorkflowTransitionWrite() {}

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }
}
