/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * DCE-style top menu bar for the modern Content Explorer (#2731).
 *
 * <p>Renders Content / View / Help as an ARIA menubar with nested dropdown
 * menus (not a multi-row flat button dump). Shell chrome commands (search,
 * security, clipboard, refresh, help) land here; server-driven item menus
 * remain on {@link ActionToolbar}.</p>
 */

import React, { useCallback, useEffect, useId, useRef, useState } from "react";
import type { DisplayFormat } from "../api/contentExplorer/displayFormatsApi";
import { message } from "../i18n/message";
import { displayFormatOptionKey } from "./displayFormatMap";
import {
  buildExplorerMenuBarGroups,
  type ExplorerMenuBarGroupId,
  type ExplorerMenuCommandId,
  type ExplorerMenuBarItem,
} from "./menuBarModel";
import { EXPLORER_MSG } from "./messages";

export interface ExplorerMenuBarProps {
  showSearch: boolean;
  showSecurity: boolean;
  showTranslations: boolean;
  showRelationships: boolean;
  showDependencies: boolean;
  /** True when a content item is selected (for deps aria-controls). */
  hasDependencyItem?: boolean;
  showClipboard: boolean;
  /** Content → Create Site panel open (#3002). */
  showSiteCreate?: boolean;
  /** Content → Site Copy panel open (#2767). */
  showSiteCopy?: boolean;
  /** Content → Subfolder Copy panel open (#2792). */
  showSubfolderCopy?: boolean;
  /** Multi-select size for clipboard-add disable + status badge. */
  multiSelectedCount: number;
  /** Clipboard size — enables View → Clipboard when non-empty. */
  clipboardItemCount: number;
  /**
   * True when the current folder/selection is under {@code /Sites/&lt;name&gt;}.
   * Enables Content → Site Copy (#2767).
   */
  hasSiteContext?: boolean;
  /**
   * True when a non-root folder path is in context (selected folder or active
   * tree folder). Enables Content → Subfolder Copy (#2792).
   */
  hasFolderContext?: boolean;
  displayFormats: ReadonlyArray<DisplayFormat>;
  selectedFormatKey: string;
  /** Non-fatal catalog load failure; selector remains mounted (#3208). */
  displayFormatLoadError?: string | null;
  onSelectFormat: (key: string) => void;
  onCommand: (id: ExplorerMenuCommandId) => void;
  className?: string;
}

const ACTIVATE_KEYS = new Set(["Enter", " "]);

const barStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  alignItems: "center",
  gap: 4,
  marginTop: 4,
};

const menuButtonStyle: React.CSSProperties = {
  padding: "4px 12px",
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#ccc",
  background: "#f5f5f5",
  cursor: "pointer",
  fontWeight: 600,
};

const menuButtonOpenStyle: React.CSSProperties = {
  ...menuButtonStyle,
  background: "#e8eef8",
  borderColor: "#8aa4d4",
};

const dropdownStyle: React.CSSProperties = {
  position: "absolute",
  top: "100%",
  left: 0,
  zIndex: 20,
  minWidth: 200,
  margin: 0,
  padding: "4px 0",
  listStyle: "none",
  background: "#fff",
  border: "1px solid #bbb",
  boxShadow: "0 2px 8px rgba(0,0,0,0.12)",
};

const itemStyle: React.CSSProperties = {
  display: "block",
  width: "100%",
  textAlign: "left",
  padding: "6px 14px",
  border: "none",
  background: "transparent",
  cursor: "pointer",
  font: "inherit",
};

const itemDisabledStyle: React.CSSProperties = {
  ...itemStyle,
  color: "#999",
  cursor: "not-allowed",
};

function isToggleChecked(
  id: ExplorerMenuCommandId,
  props: Pick<
    ExplorerMenuBarProps,
    | "showSearch"
    | "showSecurity"
    | "showTranslations"
    | "showRelationships"
    | "showDependencies"
    | "hasDependencyItem"
    | "showClipboard"
    | "showSiteCreate"
    | "showSiteCopy"
    | "showSubfolderCopy"
  >,
): boolean {
  switch (id) {
    case "content-search":
    case "view-search":
      return props.showSearch;
    case "view-security":
      return props.showSecurity;
    case "view-translations":
      return props.showTranslations;
    case "view-relationships":
      return props.showRelationships;
    case "view-dependencies":
      return props.showDependencies;
    case "view-clipboard":
      return props.showClipboard;
    case "content-create-site":
      return props.showSiteCreate === true;
    case "content-site-copy":
      return props.showSiteCopy === true;
    case "content-subfolder-copy":
      return props.showSubfolderCopy === true;
    default:
      return false;
  }
}

function isItemDisabled(
  item: ExplorerMenuBarItem,
  multiSelectedCount: number,
  clipboardItemCount: number,
  hasSiteContext: boolean,
  hasFolderContext: boolean,
): boolean {
  if (item.disabledWhen === "noSelection") {
    return multiSelectedCount === 0;
  }
  if (item.disabledWhen === "noClipboardContext") {
    return multiSelectedCount === 0 && clipboardItemCount === 0;
  }
  if (item.disabledWhen === "noSiteContext") {
    return !hasSiteContext;
  }
  if (item.disabledWhen === "noFolderContext") {
    return !hasFolderContext;
  }
  return false;
}

export function ExplorerMenuBar(props: ExplorerMenuBarProps): React.JSX.Element {
  const {
    showSearch,
    showSecurity,
    showTranslations,
    showRelationships,
    showDependencies,
    hasDependencyItem = false,
    showClipboard,
    showSiteCreate = false,
    showSiteCopy = false,
    showSubfolderCopy = false,
    multiSelectedCount,
    clipboardItemCount,
    hasSiteContext = false,
    hasFolderContext = false,
    displayFormats,
    selectedFormatKey,
    displayFormatLoadError = null,
    onSelectFormat,
    onCommand,
    className,
  } = props;

  const groups = buildExplorerMenuBarGroups();
  const [openGroup, setOpenGroup] = useState<ExplorerMenuBarGroupId | null>(
    null,
  );
  const rootRef = useRef<HTMLDivElement | null>(null);
  const baseId = useId();

  const close = useCallback(() => setOpenGroup(null), []);

  useEffect(() => {
    if (!openGroup) return;
    function onDocMouseDown(e: MouseEvent): void {
      const root = rootRef.current;
      if (!root) return;
      if (e.target instanceof Node && !root.contains(e.target)) {
        close();
      }
    }
    function onKey(e: KeyboardEvent): void {
      if (e.key === "Escape") {
        close();
      }
    }
    document.addEventListener("mousedown", onDocMouseDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDocMouseDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [openGroup, close]);

  function toggleGroup(id: ExplorerMenuBarGroupId): void {
    setOpenGroup((prev) => (prev === id ? null : id));
  }

  function activateItem(item: ExplorerMenuBarItem): void {
    if (
      isItemDisabled(
        item,
        multiSelectedCount,
        clipboardItemCount,
        hasSiteContext,
        hasFolderContext,
      )
    ) {
      return;
    }
    onCommand(item.id);
    // Keep View open for toggles so users can flip multiple panels. Content →
    // Search (#2850) shares the same Search panel and also stays open so
    // aria-expanded updates remain visible. Site/Subfolder Copy close the
    // Content menu after flip so the panel is not obscured. Help / one-shot
    // Content commands close the menu.
    if (
      (item.id.startsWith("view-") || item.id === "content-search") &&
      item.toggle
    ) {
      return;
    }
    close();
  }

  return (
    <div
      ref={rootRef}
      data-testid="explorer-menu-bar"
      className={className}
      style={barStyle}
    >
      <div
        role="menubar"
        aria-label={message(EXPLORER_MSG.MENU_BAR_ARIA)}
        data-testid="explorer-menu-bar-menubar"
        style={{ display: "flex", flexWrap: "wrap", gap: 4, alignItems: "center" }}
      >
        {groups.map((group) => {
          const expanded = openGroup === group.id;
          const menuId = `${baseId}-menu-${group.id}`;
          return (
            <div
              key={group.id}
              role="none"
              style={{ position: "relative", display: "inline-block" }}
            >
              <button
                type="button"
                role="menuitem"
                aria-haspopup="menu"
                aria-expanded={expanded}
                aria-controls={menuId}
                data-testid={`explorer-menu-${group.id}`}
                style={expanded ? menuButtonOpenStyle : menuButtonStyle}
                onClick={() => toggleGroup(group.id)}
                onKeyDown={(e) => {
                  if (ACTIVATE_KEYS.has(e.key)) {
                    e.preventDefault();
                    toggleGroup(group.id);
                  }
                }}
              >
                {message(group.labelKey)}
              </button>
              {expanded ? (
                <ul
                  id={menuId}
                  role="menu"
                  aria-label={message(group.labelKey)}
                  data-testid={`explorer-menu-${group.id}-dropdown`}
                  style={dropdownStyle}
                >
                  {group.items.map((item) => {
                    const disabled = isItemDisabled(
                      item,
                      multiSelectedCount,
                      clipboardItemCount,
                      hasSiteContext,
                      hasFolderContext,
                    );
                    const checked = item.toggle
                      ? isToggleChecked(item.id, {
                          showSearch,
                          showSecurity,
                          showTranslations,
                          showRelationships,
                          showDependencies,
                          hasDependencyItem,
                          showClipboard,
                          showSiteCreate,
                          showSiteCopy,
                          showSubfolderCopy,
                        })
                      : undefined;
                    const label = message(item.labelKey);
                    const ariaLabel = item.ariaLabelKey
                      ? message(item.ariaLabelKey)
                      : label;
                    return (
                      <li key={item.id} role="none">
                        <button
                          type="button"
                          role={item.toggle ? "menuitemcheckbox" : "menuitem"}
                          tabIndex={-1}
                          disabled={disabled}
                          aria-label={ariaLabel}
                          aria-checked={
                            item.toggle ? (checked ? true : false) : undefined
                          }
                          aria-expanded={
                            item.toggle ? (checked ? true : false) : undefined
                          }
                          aria-controls={
                            item.id === "view-search" ||
                            item.id === "content-search"
                              ? "explorer-search-panel"
                              : item.id === "view-security"
                                ? "explorer-security-panel"
                                : item.id === "view-translations"
                                  ? "explorer-translations-panel"
                                  : item.id === "view-relationships"
                                    ? "explorer-relationships-panel"
                                    : item.id === "view-dependencies"
                                      ? hasDependencyItem
                                        ? "explorer-dependencies-panel"
                                        : "explorer-dependencies-hint"
                                      : item.id === "view-clipboard"
                                        ? "explorer-clipboard-panel"
                                        : item.id === "content-create-site"
                                          ? "explorer-site-create-panel"
                                          : item.id === "content-site-copy"
                                            ? hasSiteContext
                                              ? "explorer-site-copy-panel"
                                              : "explorer-site-copy-hint"
                                            : item.id === "content-subfolder-copy"
                                              ? hasFolderContext
                                                ? "explorer-subfolder-copy-panel"
                                                : "explorer-subfolder-copy-hint"
                                              : undefined
                          }
                          data-testid={
                            item.testId ?? `explorer-menu-item-${item.id}`
                          }
                          style={disabled ? itemDisabledStyle : itemStyle}
                          onClick={() => activateItem(item)}
                          onKeyDown={(e) => {
                            if (ACTIVATE_KEYS.has(e.key)) {
                              e.preventDefault();
                              activateItem(item);
                            }
                          }}
                        >
                          {item.toggle ? (
                            <span aria-hidden="true" style={{ marginRight: 8 }}>
                              {checked ? "✓" : "\u00a0\u00a0"}
                            </span>
                          ) : null}
                          {label}
                        </button>
                      </li>
                    );
                  })}
                </ul>
              ) : null}
            </div>
          );
        })}
      </div>
      {/* Display format is shell chrome (not a menu item) — kept adjacent to
          the menubar so View stays pure menuitem/menuitemcheckbox children. */}
      <label
        htmlFor={`${baseId}-display-format`}
        style={{
          display: "inline-flex",
          gap: 6,
          alignItems: "center",
          marginLeft: 8,
          fontSize: "0.9em",
        }}
      >
        <span id={`${baseId}-display-format-label`}>
          {message(EXPLORER_MSG.DISPLAY_FORMAT_LABEL)}
        </span>
        <select
          id={`${baseId}-display-format`}
          data-testid="explorer-display-format"
          value={selectedFormatKey}
          onChange={(e) => onSelectFormat(e.target.value)}
          aria-labelledby={`${baseId}-display-format-label`}
        >
          <option value="">
            {message(EXPLORER_MSG.DISPLAY_FORMAT_DEFAULT)}
          </option>
          {displayFormats.map((df) => {
            const key = displayFormatOptionKey(df);
            if (!key) return null;
            const fmtLabel = df.displayName || df.label || df.name || key;
            return (
              <option key={key} value={key}>
                {fmtLabel}
              </option>
            );
          })}
        </select>
        {displayFormatLoadError ? (
          <span
            data-testid="explorer-display-format-error"
            role="status"
            aria-live="polite"
            style={{ color: "#b00020", fontSize: "0.85em" }}
          >
            {message(EXPLORER_MSG.DISPLAY_FORMAT_LOAD_ERROR)}
          </span>
        ) : null}
      </label>
      {multiSelectedCount > 0 ? (
        <span
          data-testid="explorer-multi-select-count"
          style={{ marginLeft: 8, color: "#666", fontSize: "0.9em" }}
          role="status"
        >
          {multiSelectedCount === 1
            ? message(EXPLORER_MSG.SELECTED_COUNT_SINGULAR)
            : message(EXPLORER_MSG.SELECTED_COUNT_PLURAL).replace(
                "{count}",
                String(multiSelectedCount),
              )}
        </span>
      ) : null}
    </div>
  );
}

export default ExplorerMenuBar;
