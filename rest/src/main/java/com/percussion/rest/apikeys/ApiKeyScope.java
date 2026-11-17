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

/**
 * One assignable Developer API key scope.
 *
 * <p>Scope keys are product-defined (see {@link ApiKeyScopeCatalog}); the registry stores only
 * which keys each named API key holds.
 */
@XmlRootElement(name = "ApiKeyScope")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Assignable Developer API key scope")
public class ApiKeyScope {

  private String key;
  private String name;
  private String description;
  private String group;

  public ApiKeyScope() {}

  public ApiKeyScope(String key, String name, String description, String group) {
    this.key = key;
    this.name = name;
    this.description = description;
    this.group = group;
  }

  @Schema(description = "Stable scope key, e.g. content:read")
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @Schema(description = "Short human label")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Schema(description = "What the scope permits")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Schema(description = "Grouping area: content, assets, design, publishing, sites, admin")
  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }
}
