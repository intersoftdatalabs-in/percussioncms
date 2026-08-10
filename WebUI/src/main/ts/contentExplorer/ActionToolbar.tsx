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
 * Server-driven action toolbar for the modern Content Explorer (US3 / T054).
 *
 * <p>Renders the configured action set returned by {@code /actions/find*}
 * as a horizontal action bar. Leaf {@code MENUITEM} actions activate on
 * click; parent {@code MENU} (or any action with {@code children[]})
 * opens a nested dropdown instead of dumping every child as a flat
 * button (#2730 / #2731). Server-provided URLs navigate; otherwise the
 * action is delegated to {@link ActionToolbarProps.onInvoke}.</p>
 *
 * <p>Toolbar refreshes when the {@link ActionToolbarProps.actions} prop
 * changes (e.g. on selection change the parent fetches the allowed
 * actions via {@code findAllowedContentTypeMenus} and re-renders this
 * component).</p>
 */

import React, { useEffect, useId, useRef, useState } from "react";
import type { MenuAction } from "../api/contentExplorer/types";
import { message } from "../i18n/message";
import { safeNavigate } from "../util/safeNavigate";

export interface ActionToolbarProps {
  actions: MenuAction[];
  onInvoke?: (actionName: string, action: MenuAction) => void;
  ariaLabel?: string;
  /** Optional i18n message for the no-actions placeholder. */
  emptyMessage?: string;
  className?: string;
}

const ACTIVATE_KEYS = new Set(["Enter", " "]);

const toolbarStyle: React.CSSProperties = {
  display: "flex",
  gap: 4,
  flexWrap: "wrap",
  alignItems: "center",
};

const buttonStyle: React.CSSProperties = {
  padding: "4px 10px",
  border: "1px solid #ccc",
  background: "#fafafa",
  cursor: "pointer",
};

const dropdownStyle: React.CSSProperties = {
  position: "absolute",
  top: "100%",
  left: 0,
  zIndex: 15,
  minWidth: 160,
  margin: 0,
  padding: "4px 0",
  listStyle: "none",
  background: "#fff",
  border: "1px solid #bbb",
  boxShadow: "0 2px 8px rgba(0,0,0,0.12)",
};

const menuItemStyle: React.CSSProperties = {
  display: "block",
  width: "100%",
  textAlign: "left",
  padding: "6px 12px",
  border: "none",
  background: "transparent",
  cursor: "pointer",
  font: "inherit",
};

function hasChildren(action: MenuAction): boolean {
  return (action.children?.length ?? 0) > 0;
}

function activate(
  action: MenuAction,
  baseHref: string,
  onInvoke?: (name: string, a: MenuAction) => void,
): void {
  if (action.url) {
    const result = safeNavigate(action.url, baseHref);
    if (!result.ok) {
      // eslint-disable-next-line no-console -- surface rejection for ops
      console.warn(
        `[ActionToolbar] rejected action "${action.name}" url "${result.href}" reason="${result.reason}"`,
      );
      onInvoke?.(action.name, action);
      return;
    }
    return;
  }
  onInvoke?.(action.name, action);
}

export function ActionToolbar(props: ActionToolbarProps): React.JSX.Element {
  const { actions, onInvoke, ariaLabel, emptyMessage, className } = props;
  const [openMenu, setOpenMenu] = useState<string | null>(null);
  const rootRef = useRef<HTMLDivElement | null>(null);
  const baseId = useId();
  const baseHref =
    typeof window !== "undefined" ? window.location.href : "http://localhost/";

  useEffect(() => {
    setOpenMenu(null);
  }, [actions]);

  useEffect(() => {
    if (!openMenu) return;
    function onDocMouseDown(e: MouseEvent): void {
      const root = rootRef.current;
      if (!root) return;
      if (e.target instanceof Node && !root.contains(e.target)) {
        setOpenMenu(null);
      }
    }
    function onKey(e: KeyboardEvent): void {
      if (e.key === "Escape") {
        setOpenMenu(null);
      }
    }
    document.addEventListener("mousedown", onDocMouseDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDocMouseDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [openMenu]);

  return (
    <div
      ref={rootRef}
      role="toolbar"
      aria-label={ariaLabel ?? "Action toolbar"}
      data-testid="action-toolbar"
      className={className}
      style={toolbarStyle}
    >
      {actions.length === 0 ? (
        <span data-testid="action-toolbar-empty" style={{ color: "#888" }}>
          {message(emptyMessage ?? "perc.ui.explorer@No actions")}
        </span>
      ) : (
        actions.map((a) => {
          if (hasChildren(a)) {
            const expanded = openMenu === a.name;
            const menuId = `${baseId}-${a.name}-menu`;
            return (
              <div
                key={a.name}
                role="none"
                style={{ position: "relative", display: "inline-block" }}
              >
                <button
                  type="button"
                  data-testid={`action-toolbar-item-${a.name}`}
                  aria-label={a.label}
                  aria-haspopup="menu"
                  aria-expanded={expanded}
                  aria-controls={menuId}
                  title={a.description ?? a.label}
                  onClick={() =>
                    setOpenMenu((prev) => (prev === a.name ? null : a.name))
                  }
                  onKeyDown={(e) => {
                    if (ACTIVATE_KEYS.has(e.key)) {
                      e.preventDefault();
                      setOpenMenu((prev) =>
                        prev === a.name ? null : a.name,
                      );
                    }
                  }}
                  style={buttonStyle}
                >
                  {a.label}
                  <span aria-hidden="true"> ▾</span>
                </button>
                {expanded && a.children ? (
                  <ul
                    id={menuId}
                    role="menu"
                    aria-label={a.label}
                    data-testid={`action-toolbar-menu-${a.name}`}
                    style={dropdownStyle}
                  >
                    {a.children.map((child) => (
                      <li key={child.name} role="none">
                        <button
                          type="button"
                          role="menuitem"
                          data-testid={`action-toolbar-item-${child.name}`}
                          aria-label={child.label}
                          title={child.description ?? child.label}
                          style={menuItemStyle}
                          onClick={() => {
                            activate(child, baseHref, onInvoke);
                            setOpenMenu(null);
                          }}
                          onKeyDown={(e) => {
                            if (ACTIVATE_KEYS.has(e.key)) {
                              e.preventDefault();
                              activate(child, baseHref, onInvoke);
                              setOpenMenu(null);
                            }
                          }}
                        >
                          {child.label}
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : null}
              </div>
            );
          }
          return (
            <button
              type="button"
              key={a.name}
              data-testid={`action-toolbar-item-${a.name}`}
              aria-label={a.label}
              title={a.description ?? a.label}
              onClick={() => activate(a, baseHref, onInvoke)}
              style={buttonStyle}
            >
              {a.label}
            </button>
          );
        })
      )}
    </div>
  );
}
