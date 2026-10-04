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

import React from "react";
import { useDialogEscape } from "../architecture/useDialogEscape";
import { message } from "../i18n/message";
import { EDITOR_MSG } from "./messages";

export interface EditorWorkflowAssigneeDialogProps {
  trigger: string;
  assignees: readonly string[];
  busy?: boolean;
  /** Empty list or HTTP 400/403/409. Dialog stays open. */
  serverError?: string | null;
  onCancel: () => void;
  onConfirm: () => void;
}

/**
 * Confirm an editor transition that requires ad-hoc assignees (#5163).
 * Cancel does not call the server. An empty list does not fire the transition.
 */
export function EditorWorkflowAssigneeDialog({
  trigger,
  assignees,
  busy = false,
  serverError = null,
  onCancel,
  onConfirm,
}: EditorWorkflowAssigneeDialogProps): React.ReactElement {
  useDialogEscape(true, busy, onCancel);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="editor-workflow-assignee-title"
      data-testid="editor-workflow-assignee-dialog"
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(15,23,42,0.45)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 40,
      }}
    >
      <form
        style={{
          background: "#fff",
          color: "#0f172a",
          minWidth: 280,
          maxWidth: 440,
          padding: 16,
          borderRadius: 8,
          boxShadow: "0 8px 24px rgba(0,0,0,0.18)",
        }}
        onSubmit={(ev) => {
          ev.preventDefault();
          if (!busy) {
            onConfirm();
          }
        }}
      >
        <h2
          id="editor-workflow-assignee-title"
          style={{ fontSize: 16, margin: "0 0 12px" }}
        >
          {message(EDITOR_MSG.WORKFLOW_ASSIGNEE_TITLE)}
        </h2>
        <p style={{ fontSize: 13, margin: "0 0 8px" }}>
          {message(EDITOR_MSG.WORKFLOW_ASSIGNEE_BODY)} {trigger}
        </p>
        <p data-testid="editor-workflow-assignee-chosen" style={{ fontSize: 13, margin: "0 0 8px" }}>
          {assignees.length > 0 ? assignees.join(", ") : ""}
        </p>
        {serverError ? (
          <p
            role="alert"
            data-testid="editor-workflow-assignee-error"
            style={{ color: "#b91c1c", fontSize: 13, margin: "8px 0 0" }}
          >
            {serverError}
          </p>
        ) : null}
        <div
          style={{
            display: "flex",
            justifyContent: "flex-end",
            gap: 8,
            marginTop: 16,
          }}
        >
          <button
            type="button"
            data-testid="editor-workflow-assignee-cancel"
            disabled={busy}
            onClick={onCancel}
          >
            {message(EDITOR_MSG.WORKFLOW_ASSIGNEE_CANCEL)}
          </button>
          <button
            type="submit"
            data-testid="editor-workflow-assignee-confirm"
            disabled={busy}
          >
            {message(
              busy
                ? EDITOR_MSG.WORKFLOW_ASSIGNEE_BUSY
                : EDITOR_MSG.WORKFLOW_ASSIGNEE_CONFIRM,
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
