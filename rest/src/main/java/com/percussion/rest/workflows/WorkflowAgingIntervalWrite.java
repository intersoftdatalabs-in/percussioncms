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
import java.util.Locale;

/**
 * Change the minute interval on one existing absolute or repeated aging transition (slice 58 and
 * slice 78).
 *
 * <p>{@code from}, {@code to}, and {@code intervalMinutes} identify the edge. Optional {@code type}
 * is {@code REPEATED} so an absolute edge that shares those three values is not changed. Omit
 * {@code type}, or send {@code ABSOLUTE}, to keep the absolute change. {@code newIntervalMinutes}
 * is the replacement, in minutes. Does not move the destination step, change the aging type, or
 * delete the transition. System-field aging is not changed here. Jackson root wrap is {@code
 * WorkflowAgingIntervalWrite}.
 */
@XmlRootElement(name = "WorkflowAgingIntervalWrite")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Absolute or repeated aging interval change for one existing transition")
public class WorkflowAgingIntervalWrite {

  @Schema(required = true, description = "Source step name. Must already exist on the workflow.")
  private String from;

  @Schema(required = true, description = "Destination step name. Identifies the edge; it is not changed.")
  private String to;

  @Schema(
      required = true,
      description =
          "Current interval in minutes. Together with from, to, and type, identifies which aging"
              + " edge to change.")
  private long intervalMinutes;

  @Schema(
      required = true,
      description = "Replacement interval in minutes. Must be a positive whole number and must differ.")
  private long newIntervalMinutes;

  @Schema(
      description =
          "Optional aging type. Omit or ABSOLUTE to change one absolute aging transition."
              + " REPEATED changes one repeated aging transition and leaves an absolute edge that"
              + " shares from, to, and the current interval. SYSTEM_FIELD and any other value are"
              + " rejected.")
  private String type;

  public WorkflowAgingIntervalWrite() {}

  /**
   * Which aging edge this body changes. Blank and {@code ABSOLUTE} are absolute. {@code REPEATED}
   * is repeated. {@code SYSTEM_FIELD} and any other value are rejected.
   */
  public WorkflowAgingTransitionWrite.Kind intervalKind() {
    if (type == null || type.isBlank()) {
      return WorkflowAgingTransitionWrite.Kind.ABSOLUTE;
    }
    String normalized = type.trim().toUpperCase(Locale.ROOT);
    if ("ABSOLUTE".equals(normalized)) {
      return WorkflowAgingTransitionWrite.Kind.ABSOLUTE;
    }
    if ("REPEATED".equals(normalized)) {
      return WorkflowAgingTransitionWrite.Kind.REPEATED;
    }
    throw new IllegalArgumentException("aging type must be ABSOLUTE or REPEATED");
  }

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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
