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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Recent quick-access panel for the modern Content Explorer
 * (#6256 / parent #4530).
 *
 * <p>Renders the operator's recently opened rows (folders included) as a
 * keyboard-reachable list: every row is a native button, so Tab reaches it
 * and Enter/Space activates it. Activating a row re-opens the item —
 * folders browse in place, pages/assets open in the editor. Dropped
 * (deleted) entries surface a status notice, never an error.</p>
 */

import React from "react";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";
import { recentItemKey, type RecentEntry } from "./recentItems";

export interface RecentPanelProps {
  /** Most-recent-first entries to render. */
  items: ReadonlyArray<RecentEntry>;
  /** Activate a row (host re-opens the item or drops a stale entry). */
  onOpen: (entry: RecentEntry) => void;
  /** Remove one entry from recents. */
  onRemove: (entry: RecentEntry) => void;
  /** Clear the whole list. */
  onClear: () => void;
  /** Transient notice (e.g. a stale entry was dropped). */
  notice?: string | null;
  ariaLabel?: string;
  className?: string;
}

export function RecentPanel(props: RecentPanelProps): React.JSX.Element {
  const { items, onOpen, onRemove, onClear, notice, ariaLabel, className } =
    props;
  const safeItems = Array.isArray(items) ? items : [];
  return (
    <section
      role="region"
      aria-label={ariaLabel ?? message(EXPLORER_MSG.RECENT_REGION)}
      data-testid="recent-panel"
      className={className}
      style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
    >
      <header style={{ display: "flex", gap: 8, alignItems: "center" }}>
        <strong>{message(EXPLORER_MSG.RECENT_TITLE)}</strong>
        <span
          aria-live="polite"
          data-testid="recent-size"
          style={{ color: "#555" }}
        >
          ({safeItems.length})
        </span>
        <button
          type="button"
          disabled={safeItems.length === 0}
          onClick={onClear}
          data-testid="recent-clear"
          style={{ marginLeft: "auto" }}
        >
          {message(EXPLORER_MSG.RECENT_CLEAR)}
        </button>
      </header>
      {notice ? (
        <div
          role="status"
          aria-live="polite"
          data-testid="recent-status"
          style={{ marginTop: 8, color: "#0f172a" }}
        >
          {notice}
        </div>
      ) : null}
      {safeItems.length === 0 ? (
        <p data-testid="recent-empty" style={{ margin: "8px 0 0", color: "#555" }}>
          {message(EXPLORER_MSG.RECENT_EMPTY)}
        </p>
      ) : (
        <ul
          data-testid="recent-items"
          style={{ listStyle: "none", padding: 0, margin: "8px 0 0 0" }}
        >
          {safeItems.map((entry, idx) => {
            const key = recentItemKey(entry) ?? `recent-${idx}`;
            const kindLabel = message(
              entry.kind === "folder"
                ? EXPLORER_MSG.RECENT_KIND_FOLDER
                : EXPLORER_MSG.RECENT_KIND_ITEM,
            );
            return (
              <li
                key={key}
                data-testid="recent-item-row"
                data-recent-kind={entry.kind}
                data-recent-key={key}
                style={{ padding: "2px 0" }}
              >
                <button
                  type="button"
                  data-testid="recent-item-open"
                  data-recent-key={key}
                  onClick={() => onOpen(entry)}
                  aria-label={`${entry.name} (${kindLabel})`}
                  style={{ textAlign: "left" }}
                >
                  {entry.name}
                </button>
                <small style={{ marginLeft: 6, color: "#767676" }}>
                  ({kindLabel})
                </small>
                <button
                  type="button"
                  data-testid="recent-item-remove"
                  data-recent-key={key}
                  onClick={() => onRemove(entry)}
                  aria-label={`${message(EXPLORER_MSG.RECENT_REMOVE)}: ${entry.name}`}
                  style={{ marginLeft: 6 }}
                >
                  {message(EXPLORER_MSG.RECENT_REMOVE)}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
