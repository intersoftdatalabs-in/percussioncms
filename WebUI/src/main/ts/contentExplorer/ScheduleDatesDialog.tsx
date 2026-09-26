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

import React, { useEffect, useRef, useState } from "react";
import { useDialogEscape } from "../architecture/useDialogEscape";
import { message } from "../i18n/message";
import {
  datetimeLocalToServerDate,
  serverDateToDatetimeLocal,
  validateScheduleDateRange,
  type ItemScheduleDates,
} from "./itemScheduleDates";
import { EXPLORER_MSG } from "./messages";

const FOCUSABLE =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

const COMMENT_MAX = 500;

export interface ScheduleDatesDialogProps {
  current: ItemScheduleDates;
  /** When greater than 1, the dialog states that one save covers the selection. */
  applyCount?: number;
  onSave: (dates: ItemScheduleDates) => void;
  onCancel: () => void;
  /** Server 400/403 (or application-level) failure; dialog stays open. */
  serverError?: string | null;
  busy?: boolean;
}

export function ScheduleDatesDialog({
  current,
  applyCount = 1,
  onSave,
  onCancel,
  serverError = null,
  busy = false,
}: ScheduleDatesDialogProps): React.ReactElement {
  const [startLocal, setStartLocal] = useState(
    serverDateToDatetimeLocal(current.startDate),
  );
  const [endLocal, setEndLocal] = useState(
    serverDateToDatetimeLocal(current.endDate),
  );
  const [comments, setComments] = useState(current.comments ?? "");
  const [error, setError] = useState<string | null>(null);
  const rootRef = useRef<HTMLDivElement>(null);
  const startRef = useRef<HTMLInputElement>(null);

  useDialogEscape(true, false, onCancel);

  useEffect(() => {
    const root = rootRef.current;
    startRef.current?.focus();
    if (!root) {
      return;
    }
    const focusables = (): HTMLElement[] =>
      Array.from(root.querySelectorAll<HTMLElement>(FOCUSABLE));
    const onKey = (ev: KeyboardEvent) => {
      if (ev.key !== "Tab") {
        return;
      }
      const list = focusables();
      if (list.length === 0) {
        return;
      }
      const first = list[0];
      const last = list[list.length - 1];
      if (ev.shiftKey && document.activeElement === first) {
        ev.preventDefault();
        last.focus();
      } else if (!ev.shiftKey && document.activeElement === last) {
        ev.preventDefault();
        first.focus();
      }
    };
    root.addEventListener("keydown", onKey);
    return () => root.removeEventListener("keydown", onKey);
  }, []);

  function submit(): void {
    const startDate = datetimeLocalToServerDate(startLocal);
    const endDate = datetimeLocalToServerDate(endLocal);
    const rangeError = validateScheduleDateRange(startDate, endDate);
    if (rangeError) {
      setError(message(rangeError));
      return;
    }
    onSave({
      itemId: current.itemId,
      startDate,
      endDate,
      comments: comments.trim().slice(0, COMMENT_MAX),
    });
  }

  return (
    <div
      ref={rootRef}
      role="dialog"
      aria-modal="true"
      aria-labelledby="explorer-schedule-title"
      data-testid="explorer-schedule-dialog"
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
        onSubmit={(e) => {
          e.preventDefault();
          submit();
        }}
      >
        <h2 id="explorer-schedule-title" style={{ fontSize: 16, margin: "0 0 12px" }}>
          {message(EXPLORER_MSG.SCHEDULE_TITLE)}
        </h2>
        {applyCount > 1 ? (
          <p
            data-testid="explorer-schedule-multi"
            style={{ fontSize: 13, margin: "0 0 12px" }}
          >
            {message(EXPLORER_MSG.SCHEDULE_MULTI_HINT)}
          </p>
        ) : null}
        <label style={{ display: "block", fontSize: 13, marginBottom: 10 }}>
          {message(EXPLORER_MSG.SCHEDULE_PUBLISH_DATE)}
          <input
            ref={startRef}
            type="datetime-local"
            data-testid="explorer-schedule-start"
            value={startLocal}
            onChange={(e) => {
              setStartLocal(e.target.value);
              setError(null);
            }}
            style={{ display: "block", width: "100%", marginTop: 6, padding: 6 }}
          />
        </label>
        <label style={{ display: "block", fontSize: 13, marginBottom: 10 }}>
          {message(EXPLORER_MSG.SCHEDULE_REMOVAL_DATE)}
          <input
            type="datetime-local"
            data-testid="explorer-schedule-end"
            value={endLocal}
            onChange={(e) => {
              setEndLocal(e.target.value);
              setError(null);
            }}
            style={{ display: "block", width: "100%", marginTop: 6, padding: 6 }}
          />
        </label>
        <label style={{ display: "block", fontSize: 13 }}>
          {message(EXPLORER_MSG.SCHEDULE_COMMENTS)}
          <textarea
            data-testid="explorer-schedule-comments"
            value={comments}
            maxLength={COMMENT_MAX}
            onChange={(e) => setComments(e.target.value)}
            rows={3}
            style={{ display: "block", width: "100%", marginTop: 6, padding: 6 }}
          />
        </label>
        {error || serverError ? (
          <p
            role="alert"
            data-testid="explorer-schedule-dialog-error"
            style={{ color: "#b91c1c", fontSize: 13, margin: "8px 0 0" }}
          >
            {error || serverError}
          </p>
        ) : null}
        <div style={{ display: "flex", justifyContent: "space-between", gap: 8, marginTop: 16 }}>
          <button
            type="button"
            data-testid="explorer-schedule-clear"
            disabled={busy}
            onClick={() => {
              setStartLocal("");
              setEndLocal("");
              setError(null);
            }}
          >
            {message(EXPLORER_MSG.SCHEDULE_CLEAR)}
          </button>
          <div style={{ display: "flex", gap: 8 }}>
            <button
              type="button"
              data-testid="explorer-schedule-cancel"
              disabled={busy}
              onClick={onCancel}
            >
              {message(EXPLORER_MSG.CONFIRM_CANCEL)}
            </button>
            <button type="submit" data-testid="explorer-schedule-save" disabled={busy}>
              {message(EXPLORER_MSG.CONFIRM_OK)}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}
