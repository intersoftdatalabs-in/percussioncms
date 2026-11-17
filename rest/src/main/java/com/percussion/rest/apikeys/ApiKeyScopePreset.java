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
 * A least-privilege starter set for the scope editor. Presets only select scopes in the UI and
 * in {@code PUT /keys/{name}} validation — they are never stored as a separate grant.
 */
@XmlRootElement(name = "ApiKeyScopePreset")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Least-privilege API key scope preset")
public class ApiKeyScopePreset {

  private String id;
  private String name;
  private String description;
  private List<String> scopes = new ArrayList<>();

  public ApiKeyScopePreset() {}

  public ApiKeyScopePreset(String id, String name, String description, List<String> scopes) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.scopes = scopes != null ? new ArrayList<>(scopes) : new ArrayList<>();
  }

  @Schema(description = "Stable preset id, e.g. readonly")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Schema(description = "Short human label")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Schema(description = "When to choose this preset")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Schema(description = "Scope keys selected by this preset, never null")
  public List<String> getScopes() {
    return scopes;
  }

  public void setScopes(List<String> scopes) {
    this.scopes = scopes != null ? new ArrayList<>(scopes) : new ArrayList<>();
  }
}
