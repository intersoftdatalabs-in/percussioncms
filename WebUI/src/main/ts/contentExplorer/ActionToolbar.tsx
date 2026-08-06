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
 * or keyboard. Server-provided URLs navigate; otherwise the action is
 * delegated to the {@link ActionToolbarProps.onInvoke} callback, so the
 * host (typically the explorer shell) can run client-side logic such as
 * ReducedActions-bar-style commands.</p>
 *
 * <p>Toolbar refreshes when the {@link ActionToolbarProps.actions} prop
 * changes (e.g. on selection change the parent fetches the allowed
 * actions via {@code findAllowedContentTypeMenus} and re-renders this
 * component).</p>
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
      style={{ display: "flex", gap: 4, flexWrap: "wrap" }}
    >
      {actions.length === 0 ? (
        <span data-testid="action-toolbar-empty" style={{ color: "#888" }}>
          {message(emptyMessage ?? "perc.ui.explorer@No actions")}
        </span>
      ) : (
        actions.map((a) => (
          <button
            type="button"
            key={a.name}
            data-testid={`action-toolbar-item-${a.name}`}
            aria-label={a.label}
            title={a.description ?? a.label}
            onClick={() => {
              if (a.url) {
                const result = safeNavigate(a.url, baseHref);
                if (!result.ok) {
                  // eslint-disable-next-line no-console -- surface rejection for ops
                  console.warn(
                    `[ActionToolbar] rejected action "${a.name}" url "${result.href}" reason="${result.reason}"`,
                  );
                  onInvoke?.(a.name, a);
                  return;
                }
                return;
              }
              onInvoke?.(a.name, a);
            }}
            style={{
              padding: "4px 10px",
              border: "1px solid #ccc",
              background: "#fafafa",
              cursor: "pointer",
            }}
          >
            {a.label}
          </button>
        ))
      )}
    </div>
  );
}
