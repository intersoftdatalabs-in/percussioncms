// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.widgetbuilder.data;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

/** Represents a single validation result for a widget builder definition. */
@XmlRootElement(name = "WidgetBuilderValidationResult")
public class PSWidgetBuilderValidationResult implements Serializable {

  private static final long serialVersionUID = 1L;

  private String category;
  private String name;
  private String message;

  public PSWidgetBuilderValidationResult() {
    // Default constructor
  }

  /**
   * @param category validation category, not null or empty
   * @param name validation name, not null or empty
   * @param message validation message, not null or empty
   */
  public PSWidgetBuilderValidationResult(String category, String name, String message) {
    if (category == null || category.isEmpty())
      throw new IllegalArgumentException("category must not be empty");
    if (name == null || name.isEmpty())
      throw new IllegalArgumentException("name must not be empty");
    if (message == null || message.isEmpty())
      throw new IllegalArgumentException("message must not be empty");
    this.category = category;
    this.name = name;
    this.message = message;
  }

  public String getCategory() {
    return category;
  }

  public String getName() {
    return name;
  }

  public String getMessage() {
    return message;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public enum ValidationCategory {
    GENERAL,
    CONTENT,
    RESOURCES,
    DISPLAY
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSWidgetBuilderValidationResult)) return false;
    var that = (PSWidgetBuilderValidationResult) o;
    return Objects.equals(getCategory(), that.getCategory())
        && Objects.equals(getName(), that.getName())
        && Objects.equals(getMessage(), that.getMessage());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCategory(), getName(), getMessage());
  }

  @Override
  public String toString() {
    return "PSWidgetBuilderValidationResult{"
        + "category='"
        + category
        + '\''
        + ", name='"
        + name
        + '\''
        + ", message='"
        + message
        + '\''
        + '}';
  }
}
