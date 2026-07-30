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

/** Represents a Visibility Context for an Action Menu. */
@XmlRootElement(name = "ActionMenuVisibilityContext")
@Schema(description = "Represents a Visibility Context for an Action Menu")
public class ActionMenuVisibilityContext {

  private String name;
  private String description;
  private String values;
  private UIContext uiContext;

  public ActionMenuVisibilityContext() {}

  public UIContext getUiContext() {
    return uiContext;
  }

  public void setUiContext(UIContext uiContext) {
    this.uiContext = uiContext;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getValue() {
    return values;
  }

  public void setValue(String values) {
    this.values = values;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ActionMenuVisibilityContext)) return false;
    var that = (ActionMenuVisibilityContext) o;
    return Objects.equals(name, that.name)
        && Objects.equals(description, that.description)
        && Objects.equals(values, that.values)
        && Objects.equals(uiContext, that.uiContext);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, values, uiContext);
  }

  @Override
  public String toString() {
    return "ActionMenuVisibilityContext{"
        + "name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + ", values='"
        + values
        + '\''
        + ", uiContext="
        + uiContext
        + '}';
  }
}
