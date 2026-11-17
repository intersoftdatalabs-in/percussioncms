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
import { formatApproveRejectBody } from "./editorApproveReject";
import { EDITOR_MSG } from "./messages";

export type EditorApproveRejectAction = "approve" | "reject";

export interface EditorApproveRejectDialogProps {
  action: EditorApproveRejectAction;
  /** Allowlisted trigger this confirm runs (named in the body). */
  trigger: string;
  comment: string;
  onCommentChange: (value: string) => void;
  /** Reject-style triggers (and server-marked Approve) block blank comments. */
  commentRequired: boolean;
  busy?: boolean;
  /** HTTP 400/403/409 and gate errors. Dialog stays open. */
  serverError?: string | null;
  onCancel: () => void;
  onConfirm: () => void;
}

/**
 * Quick Approve / Reject confirm with comment for the editor header bar
 * (#6483). Cancel calls nothing. A blank comment on a comment-required
 * action keeps the confirm disabled so nothing fires; failures stay on this
 * dialog and never claim the item moved.
 */
export function EditorApproveRejectDialog({
  action,
  trigger,
  comment,
  onCommentChange,
  commentRequired,
  busy = false,
  serverError = null,
  onCancel,
  onConfirm,
}: EditorApproveRejectDialogProps): React.ReactElement {
  useDialogEscape(true, busy, onCancel);
  const approve = action === "approve";
  const confirmDisabled =
    busy || (commentRequired && String(comment ?? "").trim().length === 0);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="editor-approve-reject-title"
      data-testid="editor-approve-reject-dialog"
      data-action={action}
      data-trigger={trigger}
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
          if (!confirmDisabled) {
            onConfirm();
          }
        }}
      >
        <h2
          id="editor-approve-reject-title"
          style={{ fontSize: 16, margin: "0 0 12px" }}
        >
          {message(
            approve
              ? EDITOR_MSG.APPROVE_REJECT_TITLE_APPROVE
              : EDITOR_MSG.APPROVE_REJECT_TITLE_REJECT,
          )}
        </h2>
        <p style={{ fontSize: 13, margin: "0 0 8px" }}>
          {formatApproveRejectBody(
            message(EDITOR_MSG.APPROVE_REJECT_BODY),
            trigger,
          )}
        </p>
        <label
          htmlFor="editor-approve-reject-comment"
          style={{ fontSize: 13, margin: "8px 0 4px", display: "block" }}
        >
          {message(EDITOR_MSG.WORKFLOW_COMMENT)}
        </label>
        <textarea
          id="editor-approve-reject-comment"
          data-testid="editor-approve-reject-comment"
          value={comment}
          disabled={busy}
          rows={3}
          style={{ width: "100%", fontSize: 13 }}
          onChange={(e) => onCommentChange(e.target.value)}
        />
        {commentRequired ? (
          <p
            data-testid="editor-approve-reject-required"
            style={{ fontSize: 12, margin: "4px 0 0" }}
          >
            {message(EDITOR_MSG.WORKFLOW_COMMENT_REQUIRED)}
          </p>
        ) : null}
        {serverError ? (
          <p
            role="alert"
            data-testid="editor-approve-reject-error"
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
            data-testid="editor-approve-reject-cancel"
            disabled={busy}
            onClick={onCancel}
          >
            {message(EDITOR_MSG.APPROVE_REJECT_CANCEL)}
          </button>
          <button
            type="submit"
            data-testid="editor-approve-reject-confirm"
            disabled={confirmDisabled}
          >
            {message(
              approve
                ? busy
                  ? EDITOR_MSG.APPROVE_REJECT_APPROVING
                  : EDITOR_MSG.APPROVE_REJECT_APPROVE
                : busy
                  ? EDITOR_MSG.APPROVE_REJECT_REJECTING
                  : EDITOR_MSG.APPROVE_REJECT_REJECT,
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
