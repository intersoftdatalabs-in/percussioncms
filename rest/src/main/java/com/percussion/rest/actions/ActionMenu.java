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

  /** Menu id; {@code -1} if not yet assigned. */
  @Schema(description = "The id of the menu. It may be -1 if the id has not been assigned.")
  private int id;

  /** Universally unique id of the menu. */
  @Schema(description = "The universally unique id of the menu, never null")
  private Guid guid;

  /** Action name, never {@code null}. */
  @Schema(description = "The name of the action. Never null.", required = true)
  private String name;

  /** Display label for dynamic context menu actions. */
  @Schema(
      description =
          "Display label for this action. Can be used to set the label for dynamic context menu"
              + " actions.")
  private String label;

  /** Free-form description of the menu. */
  @Schema(description = "The action menu description.")
  private String description;

  /** URL relative to the document base for the page hosting the menu. */
  @Schema(
      description =
          "The action url that is relative to the document base for the page hosting the menu.")
  private String url;

  /** Sort rank within the parent's child actions. */
  @Schema(description = "Sort rank of this Menu Action in its parent's children actions.")
  private int sortRank;

  /** Menu type, never {@code null} or empty. */
  @Schema(
      description =
          "The menu type, never null or empty, must be a valid menu type.",
      allowableValues =
          PSAction.TYPE_MENU
              + ","
              + PSAction.TYPE_CONTEXTMENU
              + ","
              + PSAction.TYPE_MENUITEM
              + ",DYNAMICMENU")
  private String menuType;

  /** Whether the action is handled by client or server. */
  @Schema(
      description =
          "Finds whether the action to be handled by client or not. An action that cannot be"
              + " handled by client is handled by server.")
  private String handler;

  /**
   * Parent action id from {@code RXMENUACTIONRELATION}. {@code 0} when this menu is a
   * catalog root. Lets clients reconstruct a cascade if a serializer flattens children
   * (#3379).
   */
  @Schema(
      description =
          "Parent action id when this menu is a child in RXMENUACTIONRELATION. 0 if this"
              + " menu is a catalog root.")
  private int parentId;

  /** Child actions when this action is a menu. */
  @Schema(
      description =
          "Gets children actions of this action. Should be called only if the action represents a"
              + " menu as indicated by isCascadedMenu() or isDynamicMenu. If this action represents"
              + " a menu, then a valid object is returned, otherwise, it may be empty, but never"
              + " null.")
  private ActionMenuList children;

  /** Action url parameters. */
  @Schema(description = "A collection of action url parameters.")
  private ActionMenuParameter[] parameters;

  /** Visibility contexts controlling when the action is visible. */
  @Schema(
      description =
          "Set the visibility contexts that is used to control when this action will be visible.")
  private ActionMenuVisibilityContext[] visibilityContexts;

  /** Mode-uicontexts attached to the action. */
  @Schema(description = "Gets the list of mode-uicontexts with the action")
  private ActionMenuModeUIContext[] uiContexts;

  /** Action properties. */
  @Schema(
      description =
          "An array of the Properties defined for this menu. See documentation for details.")
  private ActionMenuProperty[] properties;

  /** Default constructor for JAXB. */
  public ActionMenu() {
    // Default constructor for JAXB
  }

  // --- Getters and Setters ---

  /**
   * Returns the menu id.
   *
   * @return the menu id
   */
  public int getId() {
    return id;
  }

  /**
   * Sets the menu id.
   *
   * @param id the new id
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * Returns the menu GUID.
   *
   * @return the GUID, may be {@code null}
   */
  public Guid getGuid() {
    return guid;
  }

  /**
   * Sets the menu GUID.
   *
   * @param guid the new GUID
   */
  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  /**
   * Returns the menu name.
   *
   * @return the name, never {@code null}
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the menu name.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the menu display label.
   *
   * @return the label, may be {@code null}
   */
  public String getLabel() {
    return label;
  }

  /**
   * Sets the menu display label.
   *
   * @param label the new label
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Returns the menu description.
   *
   * @return the description, may be {@code null}
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the menu description.
   *
   * @param description the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the action URL.
   *
   * @return the URL, may be {@code null}
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the action URL.
   *
   * @param url the new URL
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Returns the sort rank within the parent's child actions.
   *
   * @return the sort rank
   */
  public int getSortRank() {
    return sortRank;
  }

  /**
   * Sets the sort rank within the parent's child actions.
   *
   * @param sortRank the new sort rank
   */
  public void setSortRank(int sortRank) {
    this.sortRank = sortRank;
  }

  /**
   * Returns the menu type.
   *
   * @return the menu type, never {@code null} or empty
   */
  public String getMenuType() {
    return menuType;
  }

  /**
   * Sets the menu type.
   *
   * @param menuType the new menu type
   */
  public void setMenuType(String menuType) {
    this.menuType = menuType;
  }

  /**
   * Returns the action handler.
   *
   * @return the handler, may be {@code null}
   */
  public String getHandler() {
    return handler;
  }

  /**
   * Sets the action handler.
   *
   * @param handler the new handler
   */
  public void setHandler(String handler) {
    this.handler = handler;
  }

  /**
   * Returns the parent action id, or {@code 0} when this menu is a catalog root.
   *
   * @return parent action id
   */
  public int getParentId() {
    return parentId;
  }

  /**
   * Sets the parent action id from {@code RXMENUACTIONRELATION}.
   *
   * @param parentId parent action id, or {@code 0} for a root
   */
  public void setParentId(int parentId) {
    this.parentId = parentId;
  }

  /**
   * Returns the child actions of this menu.
   *
   * @return the children, may be {@code null}
   */
  public ActionMenuList getChildren() {
    return children;
  }

  /**
   * Sets the child actions of this menu.
   *
   * @param children the new children
   */
  public void setChildren(ActionMenuList children) {
    this.children = children;
  }

  /**
   * Returns the action URL parameters.
   *
   * @return the parameters, may be {@code null}
   */
  public ActionMenuParameter[] getParameters() {
    return parameters;
  }

  /**
   * Sets the action URL parameters.
   *
   * @param parameters the new parameters
   */
  public void setParameters(ActionMenuParameter[] parameters) {
    this.parameters = parameters;
  }

  /**
   * Returns the visibility contexts of this menu.
   *
   * @return the contexts, may be {@code null}
   */
  public ActionMenuVisibilityContext[] getVisibilityContexts() {
    return visibilityContexts;
  }

  /**
   * Sets the visibility contexts of this menu.
   *
   * @param visibilityContexts the new contexts
   */
  public void setVisibilityContexts(ActionMenuVisibilityContext[] visibilityContexts) {
    this.visibilityContexts = visibilityContexts;
  }

  /**
   * Returns the mode-UI contexts of this action.
   *
   * @return the UI contexts, may be {@code null}
   */
  public ActionMenuModeUIContext[] getUiContexts() {
    return uiContexts;
  }

  /**
   * Sets the mode-UI contexts of this action.
   *
   * @param uiContexts the new UI contexts
   */
  public void setUiContexts(ActionMenuModeUIContext[] uiContexts) {
    this.uiContexts = uiContexts;
  }

  /**
   * Returns the action properties.
   *
   * @return the properties, may be {@code null}
   */
  public ActionMenuProperty[] getProperties() {
    return properties;
  }

  /**
   * Sets the action properties.
   *
   * @param properties the new properties
   */
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
        && parentId == that.parentId
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
            id,
            guid,
            name,
            label,
            description,
            url,
            sortRank,
            menuType,
            handler,
            parentId,
            children);
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
        + ", parentId="
        + parentId
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