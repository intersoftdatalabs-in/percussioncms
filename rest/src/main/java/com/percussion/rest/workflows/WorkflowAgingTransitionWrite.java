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
import java.util.Set;

/**
 * Create body for one aging transition between existing steps (slice 57 absolute, slice 75
 * repeated, slice 76 system-field).
 *
 * <p>{@code from} and {@code to} are required. A positive {@code intervalMinutes} is required for
 * absolute and repeated aging. The interval is minutes, the unit on {@code
 * IPSAgingTransition.setInterval}. Optional {@code type} is {@code REPEATED} for one repeated
 * aging transition, or {@code SYSTEM_FIELD} plus {@code systemField} for one system-field aging
 * transition. Omitted or {@code ABSOLUTE} stays an absolute aging transition. Does not create
 * steps, change an existing interval, or set the comment-required flag. Jackson root wrap is
 * {@code WorkflowAgingTransitionWrite}.
 */
@XmlRootElement(name = "WorkflowAgingTransitionWrite")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Aging transition create body between existing steps")
public class WorkflowAgingTransitionWrite {

  /** Content-status date columns that can drive a system-field aging transition. */
  public static final Set<String> SYSTEM_FIELDS =
      Set.of("CONTENTSTARTDATE", "CONTENTEXPIRYDATE", "REMINDERDATE");

  /** Which aging transition this body creates. */
  public enum Kind {
    ABSOLUTE,
    REPEATED,
    SYSTEM_FIELD
  }

  @Schema(required = true, description = "Source step name. Must already exist on the workflow.")
  private String from;

  @Schema(required = true, description = "Destination step name. Must already exist on the workflow.")
  private String to;

  @Schema(
      description =
          "Aging interval in minutes. Required and positive for absolute and repeated aging."
              + " Not used for SYSTEM_FIELD.")
  private long intervalMinutes;

  @Schema(
      description =
          "Optional aging type. Omit or ABSOLUTE for an absolute aging transition. REPEATED adds"
              + " one repeated aging transition. SYSTEM_FIELD adds one system-field aging"
              + " transition. Any other value is rejected.")
  private String type;

  @Schema(
      description =
          "System field that supplies the aging time. Required when type is SYSTEM_FIELD."
              + " One of CONTENTSTARTDATE, CONTENTEXPIRYDATE, or REMINDERDATE.")
  private String systemField;

  public WorkflowAgingTransitionWrite() {}

  /**
   * Aging kind for this create. Blank and {@code ABSOLUTE} are absolute. {@code REPEATED} is
   * repeated. {@code SYSTEM_FIELD} is system-field aging. Any other value is rejected.
   */
  public Kind kind() {
    if (type == null || type.isBlank()) {
      return Kind.ABSOLUTE;
    }
    String normalized = type.trim().toUpperCase(Locale.ROOT);
    if ("ABSOLUTE".equals(normalized)) {
      return Kind.ABSOLUTE;
    }
    if ("REPEATED".equals(normalized)) {
      return Kind.REPEATED;
    }
    if ("SYSTEM_FIELD".equals(normalized)) {
      return Kind.SYSTEM_FIELD;
    }
    throw new IllegalArgumentException("aging type must be ABSOLUTE, REPEATED, or SYSTEM_FIELD");
  }

  /**
   * Canonical system-field name for a system-field create. Blank and unknown names are rejected.
   */
  public String canonicalSystemField() {
    if (systemField == null || systemField.isBlank()) {
      throw new IllegalArgumentException("system field is required");
    }
    String normalized = systemField.trim().toUpperCase(Locale.ROOT);
    if (!SYSTEM_FIELDS.contains(normalized)) {
      throw new IllegalArgumentException(
          "system field must be CONTENTSTARTDATE, CONTENTEXPIRYDATE, or REMINDERDATE");
    }
    return normalized;
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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSystemField() {
    return systemField;
  }

  public void setSystemField(String systemField) {
    this.systemField = systemField;
  }
}
