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

package com.percussion.rest.apikeys;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Scope set assigned to one named Developer API key.
 *
 * <p>{@code presetId} is a hint only: the id of the preset whose scope set exactly matches {@code
 * scopes}, or {@code null} for a custom combination. The server recomputes it on read.
 */
@XmlRootElement(name = "ApiKeyAssignment")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Scope set assigned to one named Developer API key")
public class ApiKeyAssignment {

  private String name;
  private String displayName;
  private List<String> scopes = new ArrayList<>();
  private String presetId;

  public ApiKeyAssignment() {}

  @Schema(description = "Stable key name (letters, digits, dash, underscore, dot)")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Schema(description = "Optional human label")
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Schema(description = "Assigned scope keys, deduplicated and catalog-sorted on read")
  public List<String> getScopes() {
    return scopes;
  }

  public void setScopes(List<String> scopes) {
    this.scopes = scopes != null ? new ArrayList<>(scopes) : new ArrayList<>();
  }

  @Schema(description = "Matching preset id, or null for a custom combination")
  public String getPresetId() {
    return presetId;
  }

  public void setPresetId(String presetId) {
    this.presetId = presetId;
  }
}
