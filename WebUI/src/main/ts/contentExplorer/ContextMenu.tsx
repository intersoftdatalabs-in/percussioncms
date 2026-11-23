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
 * Server-driven context menu for the modern Content Explorer (US3 / T053).
 *
 * <p>Renders the configuration-driven action set returned by
 * {@code /actions/find*} as a keyboard-accessible menu attached to the
 * currently-selected item. Click <em>and</em> Enter / Space all
 * activate the focused menu item; Escape closes the menu. Activation
 * always goes to {@link ContextMenuProps.onInvoke}; the host dispatcher
 * executes the action (Data Flow HTML URLs 404 from the SPA).</p>
 *
 * <p>Server is authoritative: actions not in the supplied list are
 * hidden (FR-011). Action execution is the host's responsibility — this
 * component is presentation-only. Envelope {@code children} are unwrapped
 * and descendant names that also appear as roots are collapsed so MENU
 * parents stay nested (same catalog as the toolbar) rather than a flat
 * label dump (#3629).</p>
 *
 * <p>Items render the DCE-familiar <strong>icon + label</strong> affordance
 * (#6603) via the shared {@code actionIcons} pipeline — same glyphs as
 * the toolbar, so server actions look identical in both chrome surfaces.
 * Labels and keyboard handling are unchanged; unmapped actions and the
 * empty state stay label-only (text-first clarity).</p>
 *
 * <p>Loading, error, and empty are three distinct states (#6826, #6874):
 * a pending catalog shows the loading placeholder, a failed catalog shows
 * the error text with a Retry button that re-runs the load, and only a
 * successful empty catalog shows <strong>No actions</strong>.</p>
 *
 * <p>Slot-dependent children render {@code aria-disabled} with a tooltip
 * naming the missing slot context when no Active Assembly slot (or no
 * snippet in the slot) is selected (#6950, same contract as the
 * {@code ActionToolbar} #6915 state): the controls stay focusable so
 * pivot keyboard traversal and screen-reader discovery are unchanged,
 * and activation is a no-op instead of a server call. The host
 * dispatcher keeps the {@code ACTION_NEEDS_SLOT} /
 * {@code ACTION_NEEDS_RELATIONSHIP} status hint as a fallback for
 * direct activations. The {@code arrange} MENU parent itself is never
 * disabled — it opens the submenu so keyboard and screen-reader
 * operators can discover the disabled children.</p>
 */

import React, { useEffect, useId, useRef, useState } from "react";
import type { MenuAction } from "../api/contentExplorer/types";
import type { AssemblySlotContext } from "../assembly/slotContext";
import { message } from "../i18n/message";
import { slotActionDisabledReason } from "./ActionToolbar";
import { ActionIcon } from "./actionIcons";
import {
  prepareMenuActionTree,
  unwrapMenuActionChildren,
} from "./actionEnablement";
import { EXPLORER_MSG } from "./messages";

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
  /**
   * Catalog still loading (#6826). Renders the loading placeholder instead
   * of the empty-actions text so a slow {@code /actions/find} does not read
   * as "nothing available". Cleared when the loaded actions arrive.
   */
  pending?: boolean;
  /**
   * Catalog failed to load (#6874). Renders the error text plus the Retry
   * affordance instead of the empty-actions text, so a failed
   * {@code /actions/find} never reads as "nothing available". Takes effect
   * only when the menu is not pending and no actions rendered.
   */
  loadError?: string | null;
  /**
   * Re-runs the catalog load for the same row (#6874, same Retry pattern
   * as the checkout-owner panel in #6871). Rendered only with
   * {@code loadError}; absent means the error text shows without Retry.
   */
  onRetry?: () => void;
  /**
   * Selected Active Assembly slot (and optional snippet relationship).
   * Folder browse passes {@code null}: slot-dependent children then render
   * {@code aria-disabled} with a tooltip naming the missing slot context
   * (#6950, same contract as {@code ActionToolbar} #6915) instead of
   * dispatching a needs-slot status hint. The dispatcher keeps the hint
   * as a fallback for direct activations.
   */
  slot?: AssemblySlotContext | null;
}

/**
 * The keys that activate a menu item — Enter and Space. Per the
 * ARIA Authoring Practices, both MUST activate a menu item when
 * focused. Browsers do not synthesize {@code click} for non-interactive
 * {@code div[role="menuitem"]} elements, so we handle Enter / Space
 * explicitly (mitigation for kilo-code-bot PR #1396 review).
 */
const ACTIVATE_KEYS = new Set(["Enter", " "]);

/**
 * Visible keyboard focus ring for the catalog Retry button (#7122).
 * Buttons are focusable by default but the menu container carries no UA
 * outline styling, so the focus returned after a failed retry would be
 * invisible to sighted keyboard operators. Inline styles cannot express
 * {@code :focus-visible}, so this class carries the ring (same pattern
 * as the tree rows in {@code ExplorerTree}, #7035, and the detail-list
 * rows, #6596). Plain {@code :focus} is suppressed so pointer clicks do
 * not leave a sticky outline.
 */
const CONTEXT_MENU_RETRY_FOCUS_CSS = `
.perc-context-menu-retry:focus {
  outline: none;
}
.perc-context-menu-retry:focus-visible {
  outline: 2px solid #1a73e8;
  outline-offset: 2px;
}
`;

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

export function ContextMenu(props: ContextMenuProps): React.JSX.Element {
  const { onInvoke, onClose, ariaLabel, className, pending, loadError, onRetry } =
    props;
  const slot = props.slot ?? null;
  const actions = prepareMenuActionTree(props.actions);
  const [openPivot, setOpenPivot] = useState<string | null>(null);
  const baseId = useId();
  const menuRef = useRef<HTMLDivElement | null>(null);
  const retryButtonRef = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    setOpenPivot(null);
  }, [props.actions]);

  /**
   * Move focus into the popup on open (#6826): keyboard opens
   * (context-menu key / Shift+F10 on a tree row) leave focus on the row,
   * so Escape-to-close and screen-reader announcement only work once the
   * menu itself is focused. Runs on mount only — loaded actions replace
   * the list in place without stealing focus back.
   */
  useEffect(() => {
    menuRef.current?.focus({ preventScroll: true });
  }, []);

  /**
   * Return focus to Retry on the pending → error transition (#7122).
   * The button unmounts while the retried load is pending, so when the
   * retry fails again focus is returned to the re-rendered button instead
   * of stranding keyboard operators on the menu container / {@code <body>}.
   *
   * <p>Derived from the committed transition — not from a click-armed
   * flag. The flag variant raced: the first failure's effect can flush
   * after the Retry click arms it (likely under full-suite contention),
   * consuming the flag on the stale commit so the retry's own error
   * commit never refocused. Passive effects flush in commit order, so the
   * ref below is an ordered state machine: a delayed first-failure effect
   * focuses the still-mounted button harmlessly (or no-ops via the ref
   * when a later pending render already unmounted it), and the retry's
   * own error commit always refocuses afterwards.</p>
   */
  const prevPendingRef = useRef(false);
  useEffect(() => {
    const wasPending = prevPendingRef.current;
    prevPendingRef.current = !!pending;
    if (loadError && !pending && wasPending) {
      retryButtonRef.current?.focus();
    }
  }, [loadError, pending]);

  function handleRetry(): void {
    onRetry?.();
  }

  function handleContainerKey(e: React.KeyboardEvent<HTMLDivElement>): void {
    if (e.key === "Escape") {
      onClose?.();
    }
  }

  /**
   * Loading, error, and empty are three distinct states (#6826, #6874).
   * The error state swaps the container to an alertdialog: a bare
   * {@code role="menu"} with no menuitem children fails the axe
   * {@code aria-required-children} gate, and the failure plus its Retry
   * button is an alert, not a choice list.
   */
  const showError = !pending && !!loadError && actions.length === 0;

  function handleItemKey(
    e: React.KeyboardEvent<HTMLDivElement>,
    action: MenuAction,
    isPivot: boolean,
    expanded: boolean,
    disabled?: boolean,
  ): void {
    if (ACTIVATE_KEYS.has(e.key)) {
      e.preventDefault();
      if (isPivot) {
        setOpenPivot(expanded ? null : action.name);
      } else if (!disabled) {
        activate(action, undefined, onInvoke);
      }
    }
  }

  return (
    <div
      role={showError ? "alertdialog" : "menu"}
      aria-label={ariaLabel ?? "Context menu"}
      className={className}
      data-testid="context-menu"
      tabIndex={-1}
      ref={menuRef}
      onKeyDown={handleContainerKey}
      style={{
        background: "#fff",
        border: "1px solid #bbb",
        boxShadow: "0 2px 8px rgba(0,0,0,0.12)",
        minWidth: 180,
        maxHeight: "70vh",
        overflowY: "auto",
      }}
    >
      <style>{CONTEXT_MENU_RETRY_FOCUS_CSS}</style>
      {actions.length === 0 ? (
        pending ? (
          <div
            role="presentation"
            data-testid="context-menu-loading"
            aria-live="polite"
          >
            {message(EXPLORER_MSG.CONTEXT_MENU_LOADING)}
          </div>
        ) : loadError ? (
          <div
            role="alert"
            data-testid="context-menu-error"
            style={{ padding: "4px 8px" }}
          >
            {loadError}
            {onRetry ? (
              <div style={{ marginTop: 4 }}>
                <button
                  type="button"
                  data-testid="context-menu-retry"
                  className="perc-context-menu-retry"
                  ref={retryButtonRef}
                  onClick={handleRetry}
                  style={{
                    minHeight: "44px",
                    minWidth: "44px",
                    padding: "8px 12px",
                    cursor: "pointer",
                  }}
                >
                  {message(EXPLORER_MSG.RETRY)}
                </button>
              </div>
            ) : null}
          </div>
        ) : (
          <div role="presentation" data-testid="context-menu-empty">
            {message("perc.ui.explorer@No actions")}
          </div>
        )
      ) : (
        <ul
          role="presentation"
          style={{ listStyle: "none", padding: 0, margin: 0 }}
        >
          {actions.map((action) => {
            const kids = unwrapMenuActionChildren(action.children);
            const isPivot = kids.length > 0;
            const expanded = openPivot === action.name;
            const submenuId = `${baseId}-${action.name}-submenu`;
            // Slot-less disabled state (#6950, DCE parity — same contract
            // as the toolbar #6915): the item stays focusable
            // ({@code aria-disabled}, not {@code disabled}) so pivot
            // keyboard traversal and screen-reader discovery are
            // unchanged; activation is a no-op and the tooltip +
            // extended aria-label name the missing slot context. Pivots
            // (including Arrange) stay enabled so the submenu stays
            // discoverable — {@code slotActionDisabledReason} already
            // answers {@code null} for the {@code arrange} parent.
            const leafReason = isPivot
              ? null
              : slotActionDisabledReason(action.name, slot);
            const leafDisabled = leafReason != null;
            const leafHint = leafDisabled ? message(leafReason) : null;
            return (
              <li key={action.name} role="none">
                <div
                  role="menuitem"
                  tabIndex={0}
                  id={`${baseId}-${action.name}`}
                  aria-haspopup={isPivot ? "menu" : undefined}
                  aria-expanded={isPivot ? expanded : undefined}
                  aria-controls={isPivot ? submenuId : undefined}
                  aria-disabled={leafDisabled || undefined}
                  aria-label={
                    leafDisabled ? `${action.label}. ${leafHint}` : undefined
                  }
                  title={leafDisabled ? (leafHint ?? action.label) : undefined}
                  data-testid={`context-menu-item-${action.name}`}
                  onClick={() => {
                    if (isPivot) {
                      setOpenPivot(expanded ? null : action.name);
                    } else if (!leafDisabled) {
                      activate(action, undefined, onInvoke);
                    }
                  }}
                  onKeyDown={(e) =>
                    handleItemKey(e, action, isPivot, expanded, leafDisabled)
                  }
                  style={
                    leafDisabled
                      ? {
                          padding: "4px 8px",
                          cursor: "not-allowed",
                          opacity: 0.55,
                        }
                      : { padding: "4px 8px", cursor: "pointer" }
                  }
                >
                  <ActionIcon name={action.name} testId="context-menu-icon" />
                  {action.label}
                  {isPivot ? <span aria-hidden="true"> ▶</span> : null}
                </div>
                {isPivot && expanded ? (
                  <ul
                    id={submenuId}
                    role="menu"
                    aria-label={`${action.label} submenu`}
                    data-testid={`context-menu-submenu-${action.name}`}
                    style={{
                      listStyle: "none",
                      paddingLeft: 16,
                      margin: 0,
                      borderLeft: "2px solid #ccc",
                    }}
                  >
                    {kids.map((c) => {
                      // Same slot-less disabled contract as top-level
                      // leaves (#6950): Arrange children stay focusable
                      // with the missing-context tooltip + aria-label,
                      // and activation is a no-op.
                      const childReason = slotActionDisabledReason(c.name, slot);
                      const childDisabled = childReason != null;
                      const childHint = childDisabled
                        ? message(childReason)
                        : null;
                      return (
                        <li
                          key={c.name}
                          role="none"
                          style={{ padding: "2px 4px" }}
                        >
                          <div
                            role="menuitem"
                            tabIndex={0}
                            aria-disabled={childDisabled || undefined}
                            aria-label={
                              childDisabled
                                ? `${c.label}. ${childHint}`
                                : undefined
                            }
                            title={
                              childDisabled
                                ? (childHint ?? c.label)
                                : undefined
                            }
                            data-testid={`context-menu-item-${c.name}`}
                            onClick={() => {
                              if (!childDisabled) {
                                activate(c, action, onInvoke);
                              }
                            }}
                            onKeyDown={(e) => {
                              if (ACTIVATE_KEYS.has(e.key)) {
                                e.preventDefault();
                                if (!childDisabled) {
                                  activate(c, action, onInvoke);
                                }
                              }
                            }}
                            style={
                              childDisabled
                                ? { cursor: "not-allowed", opacity: 0.55 }
                                : { cursor: "pointer" }
                            }
                          >
                            <ActionIcon
                              name={c.name}
                              testId="context-menu-icon"
                            />
                            {c.label}
                          </div>
                        </li>
                      );
                    })}
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
