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

package com.percussion.rest.roles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Workbench Security Design Roles navigator grouping (SE-03): community, workflow, or unassigned.
 */
@Schema(
    name = "RoleBrowseGroup",
    description =
        "Workbench Roles catalog grouping: community (assigned to at least one community),"
            + " workflow (assigned to at least one workflow), or unassigned (neither).")
public enum RoleBrowseGroup {
  COMMUNITY("community"),
  WORKFLOW("workflow"),
  UNASSIGNED("unassigned");

  private final String wireValue;

  RoleBrowseGroup(String wireValue) {
    this.wireValue = wireValue;
  }

  @JsonValue
  public String getWireValue() {
    return wireValue;
  }

  /**
   * Parse a query/path filter value (case-insensitive). Blank returns {@code null} (no filter).
   *
   * @param raw filter text, may be null/blank
   * @return matching group, or null when blank
   * @throws IllegalArgumentException when non-blank and not a known group
   */
  @JsonCreator
  public static RoleBrowseGroup fromWire(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String key = raw.trim().toLowerCase();
    for (RoleBrowseGroup g : values()) {
      if (g.wireValue.equals(key) || g.name().equalsIgnoreCase(key)) {
        return g;
      }
    }
    throw new IllegalArgumentException(
        "Unknown role browse group '" + raw + "'; expected community, workflow, or unassigned");
  }
}
