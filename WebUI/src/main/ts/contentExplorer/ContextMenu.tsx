/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Server-driven context menu for the modern Content Explorer (US3 / T053).
 *
 * <p>Renders the configuration-driven action set returned by
 * {@code /actions/find*} as a keyboard-accessible menu attached to the
 * currently-selected item. Click <em>and</em> Enter / Space all
 * activate the focused menu item; Escape closes the menu. Server-provided
 * URLs are navigated to via the {@code safeNavigate} protocol /
 * same-origin guard (closes the {@code javascript:} XSS vector flagged
 * by kilo-code-bot on PR #1396); rejected URLs surface a console
 * warning and fall back to the {@link ContextMenuProps.onInvoke}
 * callback so the host can react.</p>
 *
 * <p>Server is authoritative: actions not in the supplied list are
 * hidden (FR-011). Action execution is the host's responsibility — this
 * component is presentation-only.</p>
 */

import React, { useEffect, useId, useState } from "react";
import type { MenuAction } from "../api/contentExplorer/types";
import { safeNavigate } from "../util/safeNavigate";

export interface ContextMenuProps {
  /** Server-provided menu actions (already filtered for the current selection). */
  actions: MenuAction[];
  /** Triggered when the user activates an action. Receives the action's `name`. */
  onInvoke?: (actionName: string, action: MenuAction) => void;
  /** Triggered when the user closes the menu without selecting (Escape / blur). */
  onClose?: () => void;
  /** Optional ARIA label; defaults to "Context menu". */
  ariaLabel?: string;
  /** Optional CSS class name for the menu container. */
  className?: string;
}

/**
 * The keys that activate a menu item — Enter and Space. Per the
 * ARIA Authoring Practices, both MUST activate a menu item when
 * focused. Browsers do not synthesize {@code click} for non-interactive
 * {@code div[role="menuitem"]} elements, so we handle Enter / Space
 * explicitly (mitigation for kilo-code-bot PR #1396 review).
 */
const ACTIVATE_KEYS = new Set(["Enter", " "]);

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
        `[ContextMenu] rejected action "${action.name}" url "${result.href}" reason="${result.reason}"`,
      );
      onInvoke?.(action.name, action);
      return;
    }
    return;
  }
  onInvoke?.(action.name, action);
}

export function ContextMenu(props: ContextMenuProps): React.JSX.Element {
  const { actions, onInvoke, onClose, ariaLabel, className } = props;
  const [openPivot, setOpenPivot] = useState<string | null>(null);
  const baseId = useId();
  const baseHref =
    typeof window !== "undefined" ? window.location.href : "http://localhost/";

  useEffect(() => {
    setOpenPivot(null);
  }, [actions]);

  function handleContainerKey(e: React.KeyboardEvent<HTMLDivElement>): void {
    if (e.key === "Escape") {
      onClose?.();
    }
  }

  function handleItemKey(
    e: React.KeyboardEvent<HTMLDivElement>,
    action: MenuAction,
    isPivot: boolean,
    expanded: boolean,
  ): void {
    if (ACTIVATE_KEYS.has(e.key)) {
      e.preventDefault();
      if (isPivot) {
        setOpenPivot(expanded ? null : action.name);
      } else {
        activate(action, baseHref, onInvoke);
      }
    }
  }

  return (
    <div
      role="menu"
      aria-label={ariaLabel ?? "Context menu"}
      className={className}
      data-testid="context-menu"
      onKeyDown={handleContainerKey}
    >
      {actions.length === 0 ? (
        <div role="presentation" data-testid="context-menu-empty">
          (No actions)
        </div>
      ) : (
        <ul
          role="presentation"
          style={{ listStyle: "none", padding: 0, margin: 0 }}
        >
          {actions.map((action) => {
            const isPivot = (action.children?.length ?? 0) > 0;
            const expanded = openPivot === action.name;
            const submenuId = `${baseId}-${action.name}-submenu`;
            return (
              <li key={action.name} role="none">
                <div
                  role="menuitem"
                  tabIndex={0}
                  id={`${baseId}-${action.name}`}
                  aria-haspopup={isPivot ? "menu" : undefined}
                  aria-expanded={isPivot ? expanded : undefined}
                  aria-controls={isPivot ? submenuId : undefined}
                  data-testid={`context-menu-item-${action.name}`}
                  onClick={() => {
                    if (isPivot) {
                      setOpenPivot(expanded ? null : action.name);
                    } else {
                      activate(action, baseHref, onInvoke);
                    }
                  }}
                  onKeyDown={(e) => handleItemKey(e, action, isPivot, expanded)}
                  style={{ padding: "4px 8px", cursor: "pointer" }}
                >
                  {action.label}
                  {isPivot ? <span aria-hidden="true"> ▶</span> : null}
                </div>
                {isPivot && expanded && action.children ? (
                  <ul
                    id={submenuId}
                    role="menu"
                    aria-label={`${action.label} submenu`}
                    style={{
                      listStyle: "none",
                      paddingLeft: 16,
                      margin: 0,
                      borderLeft: "2px solid #ccc",
                    }}
                  >
                    {action.children.map((c) => (
                      <li
                        key={c.name}
                        role="none"
                        style={{ padding: "2px 4px" }}
                      >
                        <div
                          role="menuitem"
                          tabIndex={0}
                          data-testid={`context-menu-item-${c.name}`}
                          onClick={() => activate(c, baseHref, onInvoke)}
                          onKeyDown={(e) => {
                            if (ACTIVATE_KEYS.has(e.key)) {
                              e.preventDefault();
                              activate(c, baseHref, onInvoke);
                            }
                          }}
                          style={{ cursor: "pointer" }}
                        >
                          {c.label}
                        </div>
                      </li>
                    ))}
                  </ul>
                ) : null}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
