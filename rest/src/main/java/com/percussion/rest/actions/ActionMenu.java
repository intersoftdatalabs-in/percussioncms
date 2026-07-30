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

import com.percussion.cms.objectstore.PSAction;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.Objects;

/** Represents an Action Menu in Percussion CMS. */
@XmlRootElement(name = "ActionMenu")
@Schema(description = "Represents an Action Menu")
public class ActionMenu {

  @Schema(description = "The id of the menu. It may be -1 if the id has not been assigned.")
  private int id;

  @Schema(description = "The universally unique id of the menu, never null")
  private Guid guid;

  @Schema(description = "The name of the action. Never null.", required = true)
  private String name;

  @Schema(
      description =
          "Display label for this action. Can be used to set the label for dynamic context menu"
              + " actions.")
  private String label;

  @Schema(description = "The action menu description.")
  private String description;

  @Schema(
      description =
          "The action url that is relative to the document base for the page hosting the menu.")
  private String url;

  @Schema(description = "Sort rank of this Menu Action in its parent's children actions.")
  private int sortRank;

  @Schema(
      description = "The menu type, never null or empty, must be a valid menu type.",
      allowableValues =
          PSAction.TYPE_MENU
              + ","
              + PSAction.TYPE_CONTEXTMENU
              + ","
              + PSAction.TYPE_MENUITEM
              + ",DYNAMICMENU")
  private String menuType;

  @Schema(
      description =
          "Finds whether the action to be handled by client or not. An action that cannot be"
              + " handled by client is handled by server.")
  private String handler;

  @Schema(
      description =
          "Gets children actions of this action. Should be called only if the action represents a"
              + " menu as indicated by isCascadedMenu() or isDynamicMenu. If this action represents"
              + " a menu, then a valid object is returned, otherwise, it may be empty, but never"
              + " null.")
  private ActionMenuList children;

  @Schema(description = "A collection of action url parameters.")
  private ActionMenuParameter[] parameters;

  @Schema(
      description =
          "Set the visibility contexts that is used to control when this action will be visible.")
  private ActionMenuVisibilityContext[] visibilityContexts;

  @Schema(description = "Gets the list of mode-uicontexts with the action")
  private ActionMenuModeUIContext[] uiContexts;

  @Schema(
      description =
          "An array of the Properties defined for this menu. See documentation for details.")
  private ActionMenuProperty[] properties;

  public ActionMenu() {
    // Default constructor for JAXB
  }

  // --- Getters and Setters ---

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Guid getGuid() {
    return guid;
  }

  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public int getSortRank() {
    return sortRank;
  }

  public void setSortRank(int sortRank) {
    this.sortRank = sortRank;
  }

  public String getMenuType() {
    return menuType;
  }

  public void setMenuType(String menuType) {
    this.menuType = menuType;
  }

  public String getHandler() {
    return handler;
  }

  public void setHandler(String handler) {
    this.handler = handler;
  }

  public ActionMenuList getChildren() {
    return children;
  }

  public void setChildren(ActionMenuList children) {
    this.children = children;
  }

  public ActionMenuParameter[] getParameters() {
    return parameters;
  }

  public void setParameters(ActionMenuParameter[] parameters) {
    this.parameters = parameters;
  }

  public ActionMenuVisibilityContext[] getVisibilityContexts() {
    return visibilityContexts;
  }

  public void setVisibilityContexts(ActionMenuVisibilityContext[] visibilityContexts) {
    this.visibilityContexts = visibilityContexts;
  }

  public ActionMenuModeUIContext[] getUiContexts() {
    return uiContexts;
  }

  public void setUiContexts(ActionMenuModeUIContext[] uiContexts) {
    this.uiContexts = uiContexts;
  }

  public ActionMenuProperty[] getProperties() {
    return properties;
  }

  public void setProperties(ActionMenuProperty[] properties) {
    this.properties = properties;
  }

  // --- equals, hashCode, toString ---

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ActionMenu)) return false;
    var that = (ActionMenu) o;
    return id == that.id
        && sortRank == that.sortRank
        && Objects.equals(guid, that.guid)
        && Objects.equals(name, that.name)
        && Objects.equals(label, that.label)
        && Objects.equals(description, that.description)
        && Objects.equals(url, that.url)
        && Objects.equals(menuType, that.menuType)
        && Objects.equals(handler, that.handler)
        && Objects.equals(children, that.children)
        && Arrays.equals(parameters, that.parameters)
        && Arrays.equals(visibilityContexts, that.visibilityContexts)
        && Arrays.equals(uiContexts, that.uiContexts)
        && Arrays.equals(properties, that.properties);
  }

  @Override
  public int hashCode() {
    int result =
        Objects.hash(
            id, guid, name, label, description, url, sortRank, menuType, handler, children);
    result = 31 * result + Arrays.hashCode(parameters);
    result = 31 * result + Arrays.hashCode(visibilityContexts);
    result = 31 * result + Arrays.hashCode(uiContexts);
    result = 31 * result + Arrays.hashCode(properties);
    return result;
  }

  @Override
  public String toString() {
    return "ActionMenu{"
        + "id="
        + id
        + ", guid="
        + guid
        + ", name='"
        + name
        + '\''
        + ", label='"
        + label
        + '\''
        + ", description='"
        + description
        + '\''
        + ", url='"
        + url
        + '\''
        + ", sortRank="
        + sortRank
        + ", menuType='"
        + menuType
        + '\''
        + ", handler='"
        + handler
        + '\''
        + ", children="
        + children
        + ", parameters="
        + Arrays.toString(parameters)
        + ", visibilityContexts="
        + Arrays.toString(visibilityContexts)
        + ", uiContexts="
        + Arrays.toString(uiContexts)
        + ", properties="
        + Arrays.toString(properties)
        + '}';
  }
}
