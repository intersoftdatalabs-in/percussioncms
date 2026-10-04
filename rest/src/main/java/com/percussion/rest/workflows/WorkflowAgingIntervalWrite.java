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
 * Change the minute interval on one existing absolute aging transition (slice 58).
 *
 * <p>{@code from}, {@code to}, and {@code intervalMinutes} identify the edge. {@code
 * newIntervalMinutes} is the replacement, in minutes. Does not move the destination step, change
 * the aging type, or delete the transition. Jackson root wrap is {@code WorkflowAgingIntervalWrite}.
 */
@XmlRootElement(name = "WorkflowAgingIntervalWrite")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Absolute aging interval change for one existing transition")
public class WorkflowAgingIntervalWrite {

  @Schema(required = true, description = "Source step name. Must already exist on the workflow.")
  private String from;

  @Schema(required = true, description = "Destination step name. Identifies the edge; it is not changed.")
  private String to;

  @Schema(
      required = true,
      description = "Current absolute interval in minutes. Identifies which aging edge to change.")
  private long intervalMinutes;

  @Schema(
      required = true,
      description = "Replacement interval in minutes. Must be a positive whole number and must differ.")
  private long newIntervalMinutes;

  public WorkflowAgingIntervalWrite() {}

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

  public long getIntervalMinutes() {
    return intervalMinutes;
  }

  public void setIntervalMinutes(long intervalMinutes) {
    this.intervalMinutes = intervalMinutes;
  }

  public long getNewIntervalMinutes() {
    return newIntervalMinutes;
  }

  public void setNewIntervalMinutes(long newIntervalMinutes) {
    this.newIntervalMinutes = newIntervalMinutes;
  }
}
