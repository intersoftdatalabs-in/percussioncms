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
import java.util.Objects;
import java.util.Optional;
import javax.xml.bind.annotation.XmlRootElement;

/** Represents an Action Menu property. */
@XmlRootElement(name = "ActionMenuProperty")
@Schema(description = "Represents an Action Menu property")
public class ActionMenuProperty {

  @Schema(description = "The action to which this property belongs.")
  private int actionId;

  @Schema(description = "The name of the property")
  private String name;

  @Schema(description = "The value of the property")
  private String value;

  @Schema(description = "The description of the property")
  private String description;

  public ActionMenuProperty() {}

  public int getActionId() {
    return actionId;
  }

  public void setActionId(int actionId) {
    this.actionId = actionId;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Optional<String> getValue() {
    return Optional.ofNullable(value);
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

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
