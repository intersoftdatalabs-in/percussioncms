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

import React, { useEffect, useState } from "react";
import { useDialogEscape } from "../architecture/useDialogEscape";
import type { PSPathItem } from "../api/contentExplorer/types";
import { message } from "../i18n/message";
import {
  clearScheduleFailureMessage,
  getItemScheduleDates,
  setItemScheduleDates,
  type ItemScheduleDates,
} from "./itemScheduleDates";
import { EXPLORER_MSG } from "./messages";

export interface ClearScheduledDatesDialogProps {
  item: PSPathItem;
  /** True only after empty dates were saved. Cancel and HTTP errors are false. */
  onDone: (cleared: boolean) => void;
}

/**
 * One confirm clears both publish and removal dates (#4968).
 * HTTP 400/403/409 and application-level failures stay on this dialog.
 */
export function ClearScheduledDatesDialog({
  item,
  onDone,
}: ClearScheduledDatesDialogProps): React.ReactElement {
  const [dates, setDates] = useState<ItemScheduleDates | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const itemId = (item.id ?? "").trim();

  useDialogEscape(true, busy, () => {
    if (!busy) {
      onDone(false);
    }
  });

  useEffect(() => {
    let cancelled = false;
    void getItemScheduleDates(itemId)
      .then((loaded) => {
        if (!cancelled) {
          setDates(loaded);
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(clearScheduleFailureMessage(err));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [itemId]);

  async function confirmClear(): Promise<void> {
    if (!itemId || busy) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await setItemScheduleDates({
        itemId,
        startDate: "",
        endDate: "",
        comments: "",
      });
      onDone(true);
    } catch (err: unknown) {
      setError(clearScheduleFailureMessage(err));
      setBusy(false);
    }
  }

  const start = dates?.startDate ?? "";
  const end = dates?.endDate ?? "";
  const hasDates = Boolean(start || end);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="explorer-clear-schedule-title"
      data-testid="explorer-clear-schedule-dialog"
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
          void confirmClear();
        }}
      >
        <h2
          id="explorer-clear-schedule-title"
          style={{ fontSize: 16, margin: "0 0 12px" }}
        >
          {message(EXPLORER_MSG.CLEAR_SCHEDULE_TITLE)}
        </h2>
        <p style={{ fontSize: 13, margin: "0 0 8px" }}>
          {message(EXPLORER_MSG.CLEAR_SCHEDULE_BODY)}
        </p>
        <p
          data-testid="explorer-clear-schedule-current"
          style={{ fontSize: 13, margin: "0 0 8px" }}
        >
          {message(EXPLORER_MSG.CLEAR_SCHEDULE_CURRENT)}
          {": "}
          {hasDates
            ? `${start || "—"}${end ? ` – ${end}` : ""}`
            : message(EXPLORER_MSG.CLEAR_SCHEDULE_NONE)}
        </p>
        {error ? (
          <p
            role="alert"
            data-testid="explorer-clear-schedule-error"
            style={{ color: "#b91c1c", fontSize: 13, margin: "8px 0 0" }}
          >
            {error}
          </p>
        ) : null}
        <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, marginTop: 16 }}>
          <button
            type="button"
            data-testid="explorer-clear-schedule-cancel"
            disabled={busy}
            onClick={() => onDone(false)}
          >
            {message(EXPLORER_MSG.CONFIRM_CANCEL)}
          </button>
          <button
            type="submit"
            data-testid="explorer-clear-schedule-confirm"
            disabled={busy || !itemId}
          >
            {message(EXPLORER_MSG.CLEAR_SCHEDULE_CONFIRM)}
          </button>
        </div>
      </form>
    </div>
  );
}
