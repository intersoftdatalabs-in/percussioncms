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
   * Mode id the modern Explorer (SPA) shell claims when it evaluates
   * per-mode UI contexts (#6619, parent #4530).
   *
   * <p>The SPA shell succeeds the Desktop Content Explorer main display
   * area, so it claims that seeded {@code RXMODE} row (installer
   * {@code cmsTableData.xml}: {@code MODEID 4}, {@code NAME CXMAIN},
   * {@code DISPLAYNAME 'Content Explorer - Main Display Area'}).
   * {@code modeName} on the wire comes from {@code RXMODE.NAME}, so menus
   * scoped to the Content Explorer main area (by far the most common
   * scoping on a real install, e.g. the 397 seeded
   * {@code RXMODEUICONTEXTACTION} rows) stay visible in the SPA, while
   * menus scoped exclusively to other modes hide. Published here (server
   * tier owns the contract) and mirrored by the WebUI evaluator; both
   * tiers apply the same rule via {@link #isVisibleInUiMode}.</p>
   */
  public static final String SPA_SHELL_MODE_ID = "4";

  /**
   * Mode name the modern Explorer (SPA) shell claims (#6619).
   *
   * <p>The seeded {@code RXMODE} {@code NAME} of the Content Explorer main
   * display area this shell succeeds ({@code CXMAIN}, {@code MODEID 4}).
   * Menus scoped (via {@code PSMenuModeContextMapping}) to this mode stay
   * visible in the SPA while menus scoped exclusively to other modes hide.
   * Matching is case-insensitive; entries with a blank id <em>and</em> a
   * blank name are unresolvable and stay permissive (visible), matching
   * the pre-#6517 behavior.</p>
   */
  public static final String SPA_SHELL_MODE_NAME = "CXMAIN";

  /**
   * Tests one mode/uicontext entry against a UI-mode token (#6619).
   *
   * @param entry the mapping entry, may be {@code null}
   * @param uiMode the claimed mode token (id or name), may be blank
   * @return {@code true} when the entry's mode id equals the token or its
   *     mode name equals the token case-insensitively
   */
  public static boolean matchesUiMode(ActionMenuModeUIContext entry, String uiMode) {
    if (entry == null || uiMode == null || uiMode.isBlank()) {
      return false;
    }
    String token = uiMode.trim();
    if (!token.isEmpty() && token.equals(entry.getModeId() == null ? "" : entry.getModeId().trim())) {
      return true;
    }
    String entryName = entry.getModeName() == null ? "" : entry.getModeName().trim();
    return !entryName.isEmpty() && entryName.equalsIgnoreCase(token);
  }

  /**
   * Menu-level visibility for a UI-mode claim (#6619).
   *
   * <p>Semantics mirror the Workbench "Used in" allowlist: a menu with no
   * {@code uiContexts} entries is visible in every mode; a scoped menu is
   * visible when any entry matches the claim or when any entry is
   * unresolvable (blank id and blank name — the client/server cannot prove
   * the menu is foreign, so it stays visible as before #6517).</p>
   *
   * @param uiContexts the menu's mode/uicontext entries, may be {@code null}
   * @param uiMode the claimed mode token (id or name), may be blank (no constraint)
   * @return {@code true} when the menu survives the mode claim
   */
  public static boolean isVisibleInUiMode(
      ActionMenuModeUIContext[] uiContexts, String uiMode) {
    if (uiContexts == null || uiContexts.length == 0) {
      return true;
    }
    if (uiMode == null || uiMode.isBlank()) {
      return true;
    }
    for (ActionMenuModeUIContext entry : uiContexts) {
      if (entry == null) {
        continue;
      }
      String id = entry.getModeId() == null ? "" : entry.getModeId().trim();
      String name = entry.getModeName() == null ? "" : entry.getModeName().trim();
      if (id.isEmpty() && name.isEmpty()) {
        return true;
      }
      if (matchesUiMode(entry, uiMode)) {
        return true;
      }
    }
    return false;
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
