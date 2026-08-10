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
 * as a horizontal action bar that the user can activate by mouse click
 * or keyboard. Cascading groups (e.g. Workflow transitions, #2732) render
 * as labeled button groups so each child is one-click invokable.
 * Server-provided URLs navigate; otherwise the action is delegated to
 * {@link ActionToolbarProps.onInvoke}.</p>
 *
 * <p>Toolbar refreshes when the {@link ActionToolbarProps.actions} prop
 * changes (e.g. on selection change the parent fetches the allowed
 * actions via {@code findAllowedContentTypeMenus} + workflow transitions
 * and re-renders this component).</p>
 */

import React from "react";
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

const buttonStyle: React.CSSProperties = {
  padding: "4px 10px",
  border: "1px solid #ccc",
  background: "#fafafa",
  cursor: "pointer",
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

export function ActionToolbar(props: ActionToolbarProps): React.JSX.Element {
  const { actions, onInvoke, ariaLabel, emptyMessage, className } = props;
  const baseHref =
    typeof window !== "undefined" ? window.location.href : "http://localhost/";
  return (
    <div
      role="toolbar"
      aria-label={ariaLabel ?? "Action toolbar"}
      data-testid="action-toolbar"
      className={className}
      style={{ display: "flex", gap: 4, flexWrap: "wrap", alignItems: "center" }}
    >
      {actions.length === 0 ? (
        <span data-testid="action-toolbar-empty" style={{ color: "#888" }}>
          {message(emptyMessage ?? "perc.ui.explorer@No actions")}
        </span>
      ) : (
        actions.map((a) => {
          const children = a.children ?? [];
          if (children.length > 0) {
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
                {children.map((c) => (
                  <button
                    type="button"
                    key={c.name}
                    data-testid={`action-toolbar-item-${c.name}`}
                    aria-label={c.label}
                    title={c.description ?? c.label}
                    onClick={() => activate(c, baseHref, onInvoke)}
                    style={buttonStyle}
                  >
                    {c.label}
                  </button>
                ))}
              </span>
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
