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
 * Scheduled-transition (embargo/expiry trigger) panel for the React
 * Content Editor host (#5830, parent #4532).
 *
 * <p>Presentational: the host loads the item dates (the same
 * {@code getitemdates} payload as the Schedule dialog) and passes the
 * embargo/expiry entries down. Lists the publish-from (embargo) and
 * until (expiry) dates that arm the workflow's scheduled transitions;
 * an item with neither date shows an explicit empty state instead of a
 * blank panel. Set/Clear buttons open the host's existing Schedule /
 * Clear-schedule dialogs — this panel never posts dates itself. Read-only
 * callers (view mode) hide both buttons.</p>
 */

import React from "react";
import { message } from "../i18n/message";
import type { ScheduledTransitionEntry } from "./editorScheduledTransitions";
import styles from "./EditorHost.module.css";
import { EDITOR_MSG } from "./messages";

export interface EditorScheduledTransitionsPanelProps {
  entries: readonly ScheduledTransitionEntry[];
  loading?: boolean;
  loadErrorKey?: string | null;
  loadErrorDetail?: string;
  onRetryLoad?: () => void;
  /** Edit mode shows the Set/Clear actions; view mode is read-only. */
  canSet?: boolean;
  busy?: boolean;
  onSetSchedule?: () => void;
  onClearSchedule?: () => void;
}

export function EditorScheduledTransitionsPanel({
  entries,
  loading = false,
  loadErrorKey = null,
  loadErrorDetail = "",
  onRetryLoad,
  canSet = false,
  busy = false,
  onSetSchedule,
  onClearSchedule,
}: EditorScheduledTransitionsPanelProps): React.ReactElement {
  return (
    <div className={styles.form} data-testid="editor-scheduled-transitions">
      <h2 className={styles.label}>
        {message(EDITOR_MSG.SCHEDULED_TRANSITIONS_TITLE)}
      </h2>
      {loading ? (
        <div
          className={styles.status}
          data-testid="editor-scheduled-transitions-loading"
        >
          {message(EDITOR_MSG.SCHEDULED_TRANSITIONS_LOADING)}
        </div>
      ) : null}
      {!loading && loadErrorKey ? (
        <div
          className={styles.status}
          role="alert"
          data-testid="editor-scheduled-transitions-load-error"
        >
          {message(loadErrorKey)}
          {loadErrorDetail ? ` ${loadErrorDetail}` : ""}
          <div className={styles.actions}>
            <button
              type="button"
              className={styles.button}
              data-testid="editor-retry-scheduled-transitions-load"
              onClick={() => onRetryLoad?.()}
            >
              {message(EDITOR_MSG.LOAD_RETRY)}
            </button>
          </div>
        </div>
      ) : null}
      {!loading && loadErrorKey == null && entries.length === 0 ? (
        <div
          className={styles.status}
          data-testid="editor-scheduled-transitions-empty"
        >
          {message(EDITOR_MSG.SCHEDULED_TRANSITIONS_EMPTY)}
        </div>
      ) : null}
      {!loading && loadErrorKey == null && entries.length > 0 ? (
        <dl
          className={styles.workflowActions}
          data-testid="editor-scheduled-transitions-list"
        >
          {entries.map((entry) => (
            <div
              key={entry.kind}
              data-testid={`editor-scheduled-transition-row-${entry.kind}`}
            >
              <dt
                data-testid={`editor-scheduled-transition-${entry.kind}-label`}
              >
                {message(
                  entry.kind === "embargo"
                    ? EDITOR_MSG.SCHEDULED_TRANSITIONS_EMBARGO
                    : EDITOR_MSG.SCHEDULED_TRANSITIONS_EXPIRY,
                )}
              </dt>
              <dd
                data-testid={`editor-scheduled-transition-${entry.kind}-date`}
              >
                {entry.value}
              </dd>
            </div>
          ))}
        </dl>
      ) : null}
      {!loading && loadErrorKey == null && canSet ? (
        <div
          className={styles.actions}
          data-testid="editor-scheduled-transitions-actions"
        >
          <button
            type="button"
            className={styles.button}
            data-testid="editor-scheduled-transitions-set"
            disabled={busy}
            onClick={() => onSetSchedule?.()}
          >
            {message(EDITOR_MSG.SCHEDULED_TRANSITIONS_SET)}
          </button>
          <button
            type="button"
            className={styles.button}
            data-testid="editor-scheduled-transitions-clear"
            disabled={busy}
            onClick={() => onClearSchedule?.()}
          >
            {message(EDITOR_MSG.SCHEDULED_TRANSITIONS_CLEAR)}
          </button>
        </div>
      ) : null}
    </div>
  );
}
