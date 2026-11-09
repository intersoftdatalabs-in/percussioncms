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

export interface EditorPublishNowDialogProps {
  busy?: boolean;
  /** Item cannot publish, or HTTP 400/403/409. Dialog stays open. */
  serverError?: string | null;
  onCancel: () => void;
  onConfirm: () => void;
}

/**
 * One confirm demand-publishes the open editor page or asset (#5939).
 * Cancel does not call the server. Failures stay on this dialog.
 */
export function EditorPublishNowDialog({
  busy = false,
  serverError = null,
  onCancel,
  onConfirm,
}: EditorPublishNowDialogProps): React.ReactElement {
  useDialogEscape(true, busy, onCancel);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="editor-publish-now-title"
      data-testid="editor-publish-now-dialog"
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
          id="editor-publish-now-title"
          style={{ fontSize: 16, margin: "0 0 12px" }}
        >
          {message(EDITOR_MSG.PUBLISH_TITLE)}
        </h2>
        <p style={{ fontSize: 13, margin: "0 0 8px" }}>
          {message(EDITOR_MSG.CONFIRM_PUBLISH_NOW)}
        </p>
        {serverError ? (
          <p
            role="alert"
            data-testid="editor-publish-now-error"
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
            data-testid="editor-publish-now-cancel"
            disabled={busy}
            onClick={onCancel}
          >
            {message(EDITOR_MSG.PUBLISH_CANCEL)}
          </button>
          <button
            type="submit"
            data-testid="editor-publish-now-confirm"
            disabled={busy}
          >
            {message(
              busy ? EDITOR_MSG.PUBLISHING : EDITOR_MSG.PUBLISH_CONFIRM,
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
