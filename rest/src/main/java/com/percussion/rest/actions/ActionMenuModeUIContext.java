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

  private String modeId;
  private String modeName;
  private String contextId;
  private String contextName;
  private String description;

  public ActionMenuModeUIContext() {}

  public String getModeId() {
    return modeId;
  }

  public void setModeId(String modeId) {
    this.modeId = modeId;
  }

  public String getModeName() {
    return modeName;
  }

  public void setModeName(String modeName) {
    this.modeName = modeName;
  }

  public String getContextId() {
    return contextId;
  }

  public void setContextId(String contextId) {
    this.contextId = contextId;
  }

  public String getContextName() {
    return contextName;
  }

  public void setContextName(String contextName) {
    this.contextName = contextName;
  }

  public String getDescription() {
    return description;
  }

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
