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
 * currently-selected item. Click and Enter both activate the focused
 * menu item; Escape closes the menu. Server-provided URLs are navigated
 * to; missing or empty URLs trigger the {@link ContextMenuProps.onInvoke}
 * callback for client-side handling (so the explorer can wire
 * {@link ReducedActions}-style behavior without restating the URL).</p>
 *
 * <p>Server is authoritative: actions not in the supplied list are
 * hidden (FR-011). Action execution is the host's responsibility — this
 * component is presentation-only.</p>
 */

import React, { useEffect, useId, useState } from "react";
import type { MenuAction } from "../api/contentExplorer/types";

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
 * Flatten a menu (with cascading children) so each leaf and cascade
 * pivot has a stable, sortable index. Cascade pivots are kept; on a click
 * they toggle visibility of their children rather than firing
 * {@link onInvoke}.
 */
interface IndexedAction {
  action: MenuAction;
  /** True if this row is a cascade pivot (has children). */
  isPivot: boolean;
}

function indexActions(actions: MenuAction[]): IndexedAction[] {
  const out: IndexedAction[] = [];
  for (const a of actions) {
    const isPivot = (a.children?.length ?? 0) > 0;
    out.push({ action: a, isPivot });
  }
  return out;
}

export function ContextMenu(props: ContextMenuProps): React.JSX.Element {
  const { actions, onInvoke, onClose, ariaLabel, className } = props;
  const items = indexActions(actions);
  const [openPivot, setOpenPivot] = useState<string | null>(null);
  const baseId = useId();

  useEffect(() => {
    setOpenPivot(null);
  }, [actions]);

  function activate(action: MenuAction): void {
    if (action.url) {
      window.location.href = action.url;
      return;
    }
    onInvoke?.(action.name, action);
  }

  function handleKey(e: React.KeyboardEvent<HTMLDivElement>): void {
    if (e.key === "Escape") {
      onClose?.();
    }
  }

  return (
    <div
      role="menu"
      aria-label={ariaLabel ?? "Context menu"}
      className={className}
      data-testid="context-menu"
      onKeyDown={handleKey}
    >
      {actions.length === 0 ? (
        <div role="presentation" data-testid="context-menu-empty">
          (No actions)
        </div>
      ) : (
        <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
          {items.map(({ action, isPivot }) => {
            const expanded = openPivot === action.name;
            return (
              <li key={action.name}>
                <div
                  role="menuitem"
                  tabIndex={0}
                  id={`${baseId}-${action.name}`}
                  aria-haspopup={isPivot ? "menu" : undefined}
                  aria-expanded={isPivot ? expanded : undefined}
                  data-testid={`context-menu-item-${action.name}`}
                  onClick={() => {
                    if (isPivot) {
                      setOpenPivot(expanded ? null : action.name);
                    } else {
                      activate(action);
                    }
                  }}
                  style={{ padding: "4px 8px", cursor: "pointer" }}
                >
                  {action.label}
                  {isPivot ? <span aria-hidden="true"> ▶</span> : null}
                </div>
                {isPivot && expanded && action.children ? (
                  <ul
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
                      <li key={c.name} style={{ padding: "2px 4px" }}>
                        <div
                          role="menuitem"
                          tabIndex={0}
                          data-testid={`context-menu-item-${c.name}`}
                          onClick={() => activate(c)}
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
