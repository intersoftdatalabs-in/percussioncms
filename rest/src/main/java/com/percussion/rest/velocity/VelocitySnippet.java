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

package com.percussion.rest.velocity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/** Built-in Velocity macro snippet for template authors (AS-09). */
@XmlRootElement(name = "VelocitySnippet")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Built-in Velocity macro snippet (field, slot, or misc)")
public class VelocitySnippet {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Stable catalog id")
  private String id;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Human-readable title")
  private String title;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Catalog category: field, slot, or misc")
  private String category;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Text to insert into a Velocity template body")
  private String insertText;

  public VelocitySnippet() {}

  public VelocitySnippet(String id, String title, String category, String insertText) {
    this.id = id;
    this.title = title;
    this.category = category;
    this.insertText = insertText;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getInsertText() {
    return insertText;
  }

  public void setInsertText(String insertText) {
    this.insertText = insertText;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VelocitySnippet that)) {
      return false;
    }
    return Objects.equals(id, that.id)
        && Objects.equals(title, that.title)
        && Objects.equals(category, that.category)
        && Objects.equals(insertText, that.insertText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, category, insertText);
  }
}
