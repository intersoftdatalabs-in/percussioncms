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
 * button (#2730 / #2731 / #3560). Envelope {@code children} are unwrapped
 * and descendant names that also appear as roots are collapsed before
 * render. The Workflow group (#2732) is a special case:
 * it renders as a labeled button group so each transition is one-click
 * invokable. Activation always goes to {@link ActionToolbarProps.onInvoke};
 * the host dispatcher executes REST / React handlers.</p>
 *
 * <p>Buttons render the DCE-familiar <strong>icon + label</strong> affordance
 * (#6515): each mapped action shows an inline SVG glyph from
 * {@code actionIcons} next to its label. Labels, {@code aria-label}s, and
 * keyboard handling are unchanged; unmapped actions stay label-only.</p>
 *
 * <p>Open dropdowns implement the WAI-APG menu-button keyboard contract
 * (#6827): items use roving {@code tabindex} (one tab stop per menu),
 * {@code ArrowDown}/{@code ArrowUp}/{@code Home}/{@code End} move focus
 * across the items (wrapping at the ends), {@code ArrowDown}/
 * {@code ArrowUp} on the parent button opens the menu and lands on the
 * first/last item, and {@code Escape} closes the menu and returns focus
 * to the parent button (#6824). The one-click Workflow group answers the
 * same movement keys across its buttons. Character type-ahead is
 * intentionally deferred (documented in the operator help).</p>
 *
 * <p>Toolbar refreshes when the {@link ActionToolbarProps.actions} prop
 * changes (e.g. on selection change the parent fetches the allowed
 * actions via {@code GET /actions/find} (plus merged type menus) and
 * workflow transitions and re-renders this component).</p>
 */

import React, { useEffect, useId, useRef, useState } from "react";
import type { MenuAction } from "../api/contentExplorer/types";
import { message } from "../i18n/message";
import { ActionIcon } from "./actionIcons";
import {
  prepareToolbarActions,
  unwrapMenuActionChildren,
} from "./actionEnablement";
import { WORKFLOW_MENU_NAME } from "./workflowMenuActions";

export interface ActionToolbarProps {
  actions: MenuAction[];
  onInvoke?: (actionName: string, action: MenuAction) => void;
  ariaLabel?: string;
  /** Optional i18n message for the no-actions placeholder. */
  emptyMessage?: string;
  className?: string;
}

const ACTIVATE_KEYS = new Set(["Enter", " "]);

/**
 * Resolve the next roving-tabindex position for a menu movement key (#6827).
 * Returns {@code null} when the key is not a movement key. Indexes wrap at
 * both ends so keyboard operators can cycle long catalog menus.
 */
function menuMoveTarget(
  key: string,
  active: number,
  count: number,
): number | null {
  if (count <= 0) {
    return null;
  }
  switch (key) {
    case "ArrowDown":
      return (active + 1) % count;
    case "ArrowUp":
      return (active - 1 + count) % count;
    case "Home":
      return 0;
    case "End":
      return count - 1;
    default:
      return null;
  }
}

const toolbarStyle: React.CSSProperties = {
  display: "flex",
  gap: 4,
  flexWrap: "wrap",
  alignItems: "center",
};

const buttonStyle: React.CSSProperties = {
  display: "inline-flex",
  alignItems: "center",
  padding: "4px 10px",
  border: "1px solid #ccc",
  background: "#fafafa",
  cursor: "pointer",
};

const menuItemStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  width: "100%",
  textAlign: "left",
  padding: "6px 12px",
  border: "none",
  background: "transparent",
  cursor: "pointer",
  font: "inherit",
};

/** Keyboard focus ring for icon+label buttons (#6515 polish). */
function applyFocusRing(e: React.FocusEvent<HTMLElement>): void {
  e.currentTarget.style.outline = "2px solid #2563eb";
  e.currentTarget.style.outlineOffset = "1px";
}

function clearFocusRing(e: React.FocusEvent<HTMLElement>): void {
  e.currentTarget.style.outline = "";
  e.currentTarget.style.outlineOffset = "";
}

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

const groupStyle: React.CSSProperties = {
  display: "inline-flex",
  flexWrap: "wrap",
  gap: 4,
  alignItems: "center",
  border: "1px solid #e2e8f0",
  borderRadius: 4,
  padding: "2px 4px",
  background: "#f8fafc",
};

const groupLabelStyle: React.CSSProperties = {
  fontSize: 11,
  color: "#64748b",
  marginRight: 4,
  textTransform: "uppercase",
  letterSpacing: "0.02em",
};

function hasChildren(action: MenuAction): boolean {
  return unwrapMenuActionChildren(action.children).length > 0;
}

function isWorkflowGroup(action: MenuAction): boolean {
  return action.name.replace(/[\s-]/g, "_").toLowerCase() === WORKFLOW_MENU_NAME;
}

function activate(
  action: MenuAction,
  parent: MenuAction | undefined,
  onInvoke?: (name: string, a: MenuAction) => void,
): void {
  const payload =
    parent?.name && !action.parentName
      ? { ...action, parentName: parent.name }
      : action;
  onInvoke?.(action.name, payload);
}

export function ActionToolbar(props: ActionToolbarProps): React.JSX.Element {
  const { onInvoke, ariaLabel, emptyMessage, className } = props;
  const actions = prepareToolbarActions(props.actions);
  const [openMenu, setOpenMenu] = useState<string | null>(null);
  /**
   * Roving-tabindex cursor for the open dropdown (#6827). Only the item at
   * this index carries {@code tabIndex=0}; every other item is
   * {@code tabIndex=-1} so the menu is a single tab stop. Reset whenever a
   * different menu opens (at most one menu is open at a time).
   */
  const [menuFocus, setMenuFocus] = useState<number>(0);
  const rootRef = useRef<HTMLDivElement | null>(null);
  const baseId = useId();
  /**
   * Parent buttons keyed by action name so Escape can return focus to the
   * invoking control instead of stranding it on {@code <body>} (#6824).
   */
  const menuButtonRefs = useRef(new Map<string, HTMLButtonElement | null>());
  /**
   * Open-dropdown item buttons keyed by menu name, in render order (#6827).
   * The movement handlers focus through this map instead of DOM queries so
   * focus never lands on a stale node after a catalog refresh.
   */
  const menuItemRefs = useRef(new Map<string, Array<HTMLButtonElement | null>>());
  /**
   * One-click Workflow group buttons keyed by group name, in render order
   * (#6827). Arrow keys move focus across the group without changing tab
   * stops (the buttons stay individually tabbable).
   */
  const groupItemRefs = useRef(new Map<string, Array<HTMLButtonElement | null>>());
  /**
   * Keyboard-open landing position (#6827). {@code ArrowDown}/{@code ArrowUp}
   * on a parent button opens the menu and must land focus on the
   * first/last item after commit, when the item refs exist. Consumed once
   * by the {@code [openMenu]} effect below.
   */
  const pendingMenuFocus = useRef<{ menu: string; index: number } | null>(null);

  /** Live (non-detached) item buttons for a menu, in render order (#6827). */
  function liveMenuItems(menuName: string): HTMLButtonElement[] {
    return (menuItemRefs.current.get(menuName) ?? []).filter(
      (el): el is HTMLButtonElement => el instanceof HTMLButtonElement,
    );
  }

  /** Move the roving cursor and DOM focus to one menu item (#6827). */
  function focusMenuItem(menuName: string, index: number): void {
    const items = liveMenuItems(menuName);
    if (items.length === 0) {
      return;
    }
    const wrapped =
      ((index % items.length) + items.length) % items.length;
    setMenuFocus(wrapped);
    items[wrapped]?.focus({ preventScroll: true });
  }

  /** Open a menu from the keyboard, landing on one item (#6827). */
  function openMenuAt(menuName: string, index: number): void {
    pendingMenuFocus.current = { menu: menuName, index };
    setOpenMenu(menuName);
  }

  /** Arrow-key traversal across one-click Workflow group buttons (#6827). */
  function handleGroupKeyDown(
    e: React.KeyboardEvent,
    groupName: string,
  ): void {
    const items = (groupItemRefs.current.get(groupName) ?? []).filter(
      (el): el is HTMLButtonElement => el instanceof HTMLButtonElement,
    );
    if (items.length === 0) {
      return;
    }
    const current = items.indexOf(document.activeElement as HTMLButtonElement);
    const target = menuMoveTarget(
      e.key,
      current === -1 ? 0 : current,
      items.length,
    );
    if (target === null) {
      return;
    }
    e.preventDefault();
    items[target]?.focus({ preventScroll: true });
  }

  useEffect(() => {
    // A selection refresh replaces the actions array while a menu is open.
    // Keep that menu when its parent is still present so the click is not
    // detached mid-activation (#5006). Close it when the parent is gone.
    setOpenMenu((prev) => {
      if (!prev) {
        return null;
      }
      return actions.some((a) => a.name === prev) ? prev : null;
    });
  }, [actions]);

  useEffect(() => {
    // Land keyboard focus inside a newly opened menu (#6827). Mouse opens
    // also move focus to the first item so the roving cursor and DOM focus
    // never disagree; Escape still returns focus to the parent (#6824).
    // Runs on open only — refreshes that keep the menu open (#5006) leave
    // focus alone because {@code openMenu} is unchanged.
    if (!openMenu) {
      return;
    }
    const pending = pendingMenuFocus.current;
    pendingMenuFocus.current = null;
    const items = liveMenuItems(openMenu);
    if (items.length === 0) {
      return;
    }
    const target =
      pending && pending.menu === openMenu
        ? ((pending.index % items.length) + items.length) % items.length
        : 0;
    setMenuFocus(target);
    items[target]?.focus({ preventScroll: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [openMenu ]);

  useEffect(() => {
    if (!openMenu) return;
    const menuName: string = openMenu;
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
        // Keyboard polish (#6824): return focus to the invoking parent
        // button so keyboard operators stay in the toolbar flow.
        menuButtonRefs.current.get(menuName)?.focus();
      } else if (e.key === "Tab") {
        // Tab leaves the menu like any other composite widget (#6827):
        // close it so a detached dropdown never lingers behind focus.
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
        <span
          data-testid="action-toolbar-empty"
          role="status"
          // #595959 keeps the hint gray while meeting WCAG AA contrast
          // on white (the previous #888 failed the shell a11y gate, #6517).
          style={{ color: "#595959" }}
        >
          {message(emptyMessage ?? "perc.ui.explorer@No actions")}
        </span>
      ) : (
        actions.map((a) => {
          // Workflow transitions (#2732): labeled one-click group.
          if (hasChildren(a) && isWorkflowGroup(a)) {
            const children = unwrapMenuActionChildren(a.children);
            return (
              <span
                key={a.name}
                role="group"
                aria-label={a.label}
                data-testid={`action-toolbar-group-${a.name}`}
                style={groupStyle}
              >
                <span style={groupLabelStyle} aria-hidden="true">
                  {a.label}
                </span>
                {children.map((c, childIdx) => (
                  <button
                    type="button"
                    key={c.name}
                    data-testid={`action-toolbar-item-${c.name}`}
                    aria-label={c.label}
                    title={c.description ?? c.label}
                    onClick={() => activate(c, a, onInvoke)}
                    onKeyDown={(e) => handleGroupKeyDown(e, a.name)}
                    ref={(el) => {
                      const arr = groupItemRefs.current.get(a.name) ?? [];
                      arr[childIdx] = el;
                      arr.length = children.length;
                      groupItemRefs.current.set(a.name, arr);
                    }}
                    onFocus={applyFocusRing}
                    onBlur={clearFocusRing}
                    style={buttonStyle}
                  >
                    <ActionIcon name={c.name} />
                    {c.label}
                  </button>
                ))}
              </span>
            );
          }
          // General MENU parents (#2730/#2731): nested dropdown chrome.
          if (hasChildren(a)) {
            const expanded = openMenu === a.name;
            const menuId = `${baseId}-${a.name}-menu`;
            const kids = unwrapMenuActionChildren(a.children);
            // Roving cursor for this menu (#6827). Only one menu is open
            // at a time and {@code menuFocus} resets on open, so the
            // shared cursor is always this menu's while it is expanded.
            const activeIdx =
              kids.length === 0
                ? 0
                : ((menuFocus % kids.length) + kids.length) % kids.length;
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
                  ref={(el) => {
                    if (el) {
                      menuButtonRefs.current.set(a.name, el);
                    } else {
                      menuButtonRefs.current.delete(a.name);
                    }
                  }}
                  onClick={() =>
                    setOpenMenu((prev) => (prev === a.name ? null : a.name))
                  }
                  onKeyDown={(e) => {
                    if (ACTIVATE_KEYS.has(e.key)) {
                      e.preventDefault();
                      setOpenMenu((prev) =>
                        prev === a.name ? null : a.name,
                      );
                      return;
                    }
                    // WAI-APG menu-button open keys (#6827): ArrowDown
                    // opens on the first item, ArrowUp on the last. When
                    // already open the keys walk the items instead.
                    if (e.key === "ArrowDown" || e.key === "ArrowUp") {
                      e.preventDefault();
                      if (openMenu !== a.name) {
                        openMenuAt(
                          a.name,
                          e.key === "ArrowDown" ? 0 : kids.length - 1,
                        );
                      } else {
                        focusMenuItem(
                          a.name,
                          e.key === "ArrowDown"
                            ? activeIdx + 1
                            : activeIdx - 1,
                        );
                      }
                    }
                  }}
                  onFocus={applyFocusRing}
                  onBlur={clearFocusRing}
                  style={buttonStyle}
                >
                  <ActionIcon name={a.name} />
                  {a.label}
                  <span aria-hidden="true"> ▾</span>
                </button>
                {expanded ? (
                  <ul
                    id={menuId}
                    role="menu"
                    aria-label={a.label}
                    data-testid={`action-toolbar-menu-${a.name}`}
                    style={dropdownStyle}
                    onKeyDown={(e) => {
                      const target = menuMoveTarget(
                        e.key,
                        activeIdx,
                        kids.length,
                      );
                      if (target === null) {
                        return;
                      }
                      e.preventDefault();
                      focusMenuItem(a.name, target);
                    }}
                  >
                    {kids.map((child, childIdx) => (
                      <li key={child.name} role="none">
                        <button
                          type="button"
                          role="menuitem"
                          data-testid={`action-toolbar-item-${child.name}`}
                          aria-label={child.label}
                          title={child.description ?? child.label}
                          style={menuItemStyle}
                          tabIndex={activeIdx === childIdx ? 0 : -1}
                          ref={(el) => {
                            const arr =
                              menuItemRefs.current.get(a.name) ?? [];
                            arr[childIdx] = el;
                            arr.length = kids.length;
                            menuItemRefs.current.set(a.name, arr);
                          }}
                          onClick={() => {
                            activate(child, a, onInvoke);
                            setOpenMenu(null);
                          }}
                          onKeyDown={(e) => {
                            if (ACTIVATE_KEYS.has(e.key)) {
                              e.preventDefault();
                              activate(child, a, onInvoke);
                              setOpenMenu(null);
                            }
                          }}
                          onFocus={applyFocusRing}
                          onBlur={clearFocusRing}
                        >
                          <ActionIcon name={child.name} />
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
              onClick={() => activate(a, undefined, onInvoke)}
              onFocus={applyFocusRing}
              onBlur={clearFocusRing}
              style={buttonStyle}
            >
              <ActionIcon name={a.name} />
              {a.label}
            </button>
          );
        })
      )}
    </div>
  );
}
