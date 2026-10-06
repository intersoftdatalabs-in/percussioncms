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
 * Change the content-status date column on one existing system-field aging transition (slice 79).
 *
 * <p>{@code from}, {@code to}, and {@code systemField} identify the edge. {@code newSystemField}
 * is the replacement and must be a different column from {@code CONTENTSTARTDATE}, {@code
 * CONTENTEXPIRYDATE}, or {@code REMINDERDATE}. Does not move the destination step, change the
 * aging type, change a minute interval, or edit absolute or repeated aging. Jackson root wrap is
 * {@code WorkflowAgingSystemFieldWrite}.
 */
@XmlRootElement(name = "WorkflowAgingSystemFieldWrite")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "System-field date column change for one existing aging transition")
public class WorkflowAgingSystemFieldWrite {

  @Schema(required = true, description = "Source step name. Must already exist on the workflow.")
  private String from;

  @Schema(required = true, description = "Destination step name. Identifies the edge; it is not changed.")
  private String to;

  @Schema(
      required = true,
      description =
          "Current content-status date column. Together with from and to, identifies which"
              + " system-field aging edge to change. One of CONTENTSTARTDATE, CONTENTEXPIRYDATE,"
              + " or REMINDERDATE.")
  private String systemField;

  @Schema(
      required = true,
      description =
          "Replacement content-status date column. Must differ from systemField and must be one"
              + " of CONTENTSTARTDATE, CONTENTEXPIRYDATE, or REMINDERDATE.")
  private String newSystemField;

  public WorkflowAgingSystemFieldWrite() {}

  /** Canonical current column. Blank and unknown names are rejected. */
  public String canonicalSystemField() {
    return canonical(systemField, "system field is required");
  }

  /** Canonical replacement column. Blank and unknown names are rejected. */
  public String canonicalNewSystemField() {
    return canonical(newSystemField, "new system field is required");
  }

  private static String canonical(String raw, String blankMessage) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException(blankMessage);
    }
    String normalized = raw.trim().toUpperCase(Locale.ROOT);
    if (!WorkflowAgingTransitionWrite.SYSTEM_FIELDS.contains(normalized)) {
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

  public String getSystemField() {
    return systemField;
  }

  public void setSystemField(String systemField) {
    this.systemField = systemField;
  }

  public String getNewSystemField() {
    return newSystemField;
  }

  public void setNewSystemField(String newSystemField) {
    this.newSystemField = newSystemField;
  }
}
