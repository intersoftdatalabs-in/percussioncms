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

/** Represents a UI Context that can be used to scope a Menu. */
@XmlRootElement(name = "ActionMenuModeUIContext")
@Schema(description = "Represents a UI Context that can be used to scope a Menu")
public class ActionMenuModeUIContext {

  /** Identifier of the UI mode. */
  private String modeId;

  /** Name of the UI mode. */
  private String modeName;

  /** Identifier of the UI context. */
  private String contextId;

  /** Name of the UI context. */
  private String contextName;

  /** Free-form description. */
  private String description;

  /** No-op constructor. */
  public ActionMenuModeUIContext() {}

  /**
   * Returns the UI mode id.
   *
   * @return the mode id
   */
  public String getModeId() {
    return modeId;
  }

  /**
   * Sets the UI mode id.
   *
   * @param modeId the new mode id
   */
  public void setModeId(String modeId) {
    this.modeId = modeId;
  }

  /**
   * Returns the UI mode name.
   *
   * @return the mode name
   */
  public String getModeName() {
    return modeName;
  }

  /**
   * Sets the UI mode name.
   *
   * @param modeName the new mode name
   */
  public void setModeName(String modeName) {
    this.modeName = modeName;
  }

  /**
   * Returns the UI context id.
   *
   * @return the context id
   */
  public String getContextId() {
    return contextId;
  }

  /**
   * Sets the UI context id.
   *
   * @param contextId the new context id
   */
  public void setContextId(String contextId) {
    this.contextId = contextId;
  }

  /**
   * Returns the UI context name.
   *
   * @return the context name
   */
  public String getContextName() {
    return contextName;
  }

  /**
   * Sets the UI context name.
   *
   * @param contextName the new context name
   */
  public void setContextName(String contextName) {
    this.contextName = contextName;
  }

  /**
   * Returns the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ActionMenuModeUIContext)) return false;
    var that = (ActionMenuModeUIContext) o;
    return Objects.equals(modeId, that.modeId)
        && Objects.equals(modeName, that.modeName)
        && Objects.equals(contextId, that.contextId)
        && Objects.equals(contextName, that.contextName)
        && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modeId, modeName, contextId, contextName, description);
  }

  @Override
  public String toString() {
    return "ActionMenuModeUIContext{"
        + "modeId='"
        + modeId
        + '\''
        + ", modeName='"
        + modeName
        + '\''
        + ", contextId='"
        + contextId
        + '\''
        + ", contextName='"
        + contextName
        + '\''
        + ", description='"
        + description
        + '\''
        + '}';
  }
}
