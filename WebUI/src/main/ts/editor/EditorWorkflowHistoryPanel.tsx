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
 * Read-only workflow history/audit panel for the React Content Editor
 * host (#5829, parent #4532).
 *
 * <p>Presentational: the host loads the revisions summary (the same
 * {@code revisions/{id}} payload as restore/compare) and passes the
 * recorded transition comments down. Lists date, user, transition type,
 * and comment; an item with no recorded comments shows an explicit empty
 * state instead of a blank panel. Never restores and never transitions.</p>
 */

import React from "react";
import type { ItemAuditComment } from "../api/contentExplorer/itemRevisionsApi";
import { message } from "../i18n/message";
import styles from "./EditorHost.module.css";
import { EDITOR_MSG } from "./messages";

export interface EditorWorkflowHistoryPanelProps {
  comments: readonly ItemAuditComment[];
  loading?: boolean;
  loadErrorKey?: string | null;
  loadErrorDetail?: string;
}

export function EditorWorkflowHistoryPanel({
  comments,
  loading = false,
  loadErrorKey = null,
  loadErrorDetail = "",
}: EditorWorkflowHistoryPanelProps): React.ReactElement {
  return (
    <div className={styles.form} data-testid="editor-workflow-history">
      <h2 className={styles.label}>{message(EDITOR_MSG.WORKFLOW_HISTORY_TITLE)}</h2>
      {loading ? (
        <div
          className={styles.status}
          data-testid="editor-workflow-history-loading"
        >
          {message(EDITOR_MSG.WORKFLOW_HISTORY_LOADING)}
        </div>
      ) : null}
      {!loading && loadErrorKey ? (
        <div
          className={styles.status}
          role="alert"
          data-testid="editor-workflow-history-load-error"
        >
          {message(loadErrorKey)}
          {loadErrorDetail ? ` ${loadErrorDetail}` : ""}
        </div>
      ) : null}
      {!loading && loadErrorKey == null && comments.length === 0 ? (
        <div
          className={styles.status}
          data-testid="editor-workflow-history-empty"
        >
          {message(EDITOR_MSG.WORKFLOW_HISTORY_EMPTY)}
        </div>
      ) : null}
      {!loading && loadErrorKey == null && comments.length > 0 ? (
        <table
          className={styles.tableGrid}
          data-testid="editor-workflow-history-table"
        >
          <thead>
            <tr>
              <th scope="col">{message(EDITOR_MSG.WORKFLOW_HISTORY_DATE)}</th>
              <th scope="col">{message(EDITOR_MSG.WORKFLOW_HISTORY_USER)}</th>
              <th scope="col">{message(EDITOR_MSG.WORKFLOW_HISTORY_TYPE)}</th>
              <th scope="col">{message(EDITOR_MSG.WORKFLOW_HISTORY_COMMENT)}</th>
            </tr>
          </thead>
          <tbody>
            {comments.map((entry, index) => (
              <tr
                key={`${entry.commentDate}-${entry.commenter}-${index}`}
                data-testid={`editor-workflow-history-row-${index}`}
              >
                <td data-testid={`editor-workflow-history-date-${index}`}>
                  {entry.commentDate}
                </td>
                <td data-testid={`editor-workflow-history-user-${index}`}>
                  {entry.commenter}
                </td>
                <td data-testid={`editor-workflow-history-type-${index}`}>
                  {entry.commentType}
                </td>
                <td data-testid={`editor-workflow-history-comment-${index}`}>
                  {entry.comment}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
    </div>
  );
}
