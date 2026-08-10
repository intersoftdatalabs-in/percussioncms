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

  /** Context name. */
  private String name;

  /** Free-form description. */
  private String description;

  /** Allowed values for the context. */
  private String values;

  /** Owning UI context. */
  private UIContext uiContext;

  /** No-op constructor. */
  public ActionMenuVisibilityContext() {}

  /**
   * Returns the owning UI context.
   *
   * @return the UI context
   */
  public UIContext getUiContext() {
    return uiContext;
  }

  /**
   * Sets the owning UI context.
   *
   * @param uiContext the new UI context
   */
  public void setUiContext(UIContext uiContext) {
    this.uiContext = uiContext;
  }

  /**
   * Returns the context name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the context name.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the context description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the context description.
   *
   * @param description the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the context values.
   *
   * @return the values
   */
  public String getValue() {
    return values;
  }

  /**
   * Sets the context values.
   *
   * @param values the new values
   */
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
