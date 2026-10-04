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
import type { ItemScheduleDates } from "../contentExplorer/itemScheduleDates";
import { EDITOR_MSG } from "./messages";

export interface EditorClearScheduleDialogProps {
  current: ItemScheduleDates;
  busy?: boolean;
  /** HTTP 400/403/409 or a refresh that still has dates. Dialog stays open. */
  serverError?: string | null;
  onCancel: () => void;
  onConfirm: () => void;
}

/**
 * One confirm clears start and removal dates on the open editor item (#5123).
 * Cancel does not write. Failures stay on this dialog.
 */
export function EditorClearScheduleDialog({
  current,
  busy = false,
  serverError = null,
  onCancel,
  onConfirm,
}: EditorClearScheduleDialogProps): React.ReactElement {
  useDialogEscape(true, busy, onCancel);
  const start = (current.startDate ?? "").trim();
  const end = (current.endDate ?? "").trim();
  const hasDates = Boolean(start || end);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="editor-clear-schedule-title"
      data-testid="editor-clear-schedule-dialog"
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
          maxWidth: 420,
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
          id="editor-clear-schedule-title"
          style={{ fontSize: 16, margin: "0 0 12px" }}
        >
          {message(EDITOR_MSG.CLEAR_SCHEDULE_TITLE)}
        </h2>
        <p style={{ fontSize: 13, margin: "0 0 8px" }}>
          {message(EDITOR_MSG.CLEAR_SCHEDULE_BODY)}
        </p>
        <p
          data-testid="editor-clear-schedule-current"
          style={{ fontSize: 13, margin: "0 0 8px" }}
        >
          {message(EDITOR_MSG.CLEAR_SCHEDULE_CURRENT)}
          {": "}
          {hasDates
            ? `${start || "—"}${end ? ` – ${end}` : ""}`
            : message(EDITOR_MSG.CLEAR_SCHEDULE_NONE)}
        </p>
        {serverError ? (
          <p
            role="alert"
            data-testid="editor-clear-schedule-error"
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
            data-testid="editor-clear-schedule-cancel"
            disabled={busy}
            onClick={onCancel}
          >
            {message(EDITOR_MSG.CLEAR_SCHEDULE_CANCEL)}
          </button>
          <button
            type="submit"
            data-testid="editor-clear-schedule-confirm"
            disabled={busy || !current.itemId.trim()}
          >
            {message(EDITOR_MSG.CLEAR_SCHEDULE_CONFIRM)}
          </button>
        </div>
      </form>
    </div>
  );
}
