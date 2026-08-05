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

package com.percussion.rest.actions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/** Represents an Action Menu property. */
@XmlRootElement(name = "ActionMenuProperty")
@Schema(description = "Represents an Action Menu property")
public class ActionMenuProperty {

  /** Identifier of the action this property belongs to. */
  @Schema(description = "The action to which this property belongs.")
  private int actionId;

  /** Property name. */
  @Schema(description = "The name of the property")
  private String name;

  /** Property value. */
  @Schema(description = "The value of the property")
  private String value;

  /** Property description. */
  @Schema(description = "The description of the property")
  private String description;

  /** No-op constructor. */
  public ActionMenuProperty() {}

  /**
   * Returns the owning action id.
   *
   * @return the action id
   */
  public int getActionId() {
    return actionId;
  }

  /**
   * Sets the owning action id.
   *
   * @param actionId the new action id
   */
  public void setActionId(int actionId) {
    this.actionId = actionId;
  }

  /**
   * Returns the property name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the property name.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the property value.
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the property value.
   *
   * @param value the new value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Returns the property description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the property description.
   *
   * @param description the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ActionMenuProperty)) return false;
    var that = (ActionMenuProperty) o;
    return actionId == that.actionId
        && Objects.equals(name, that.name)
        && Objects.equals(value, that.value)
        && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actionId, name, value, description);
  }

  @Override
  public String toString() {
    return "ActionMenuProperty{"
        + "actionId="
        + actionId
        + ", name='"
        + name
        + '\''
        + ", value='"
        + value
        + '\''
        + ", description='"
        + description
        + '\''
        + '}';
  }
}