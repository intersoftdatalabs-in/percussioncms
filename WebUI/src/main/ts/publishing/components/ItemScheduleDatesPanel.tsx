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

import React, { useCallback, useEffect, useState } from "react";
import {
  fetchItemScheduleDates,
  saveItemScheduleDates,
} from "../../api/publishing/itemScheduleDatesApi";
import {
  isSessionRedirectError,
} from "../../api/client";
import {
  datetimeLocalToServerDate,
  serverDateToDatetimeLocal,
  validateScheduleDateRange,
} from "../../contentExplorer/itemScheduleDates";
import { message, MSG } from "../../i18n/message";
import { mapIdParam } from "../deepLinkMap";
import {
  clearScheduleDatesBatch,
  formatScheduleBatchDetail,
  loadScheduleDatesBatch,
  parseScheduleItemIds,
  saveScheduleDatesBatch,
  scheduleBatchFailureMessage,
  type ScheduleBatchFailure,
} from "../itemScheduleBatch";
import {
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

const COMMENT_MAX = 500;

function fill(template: string, vars: Record<string, string>): string {
  let out = template;
  for (const [name, value] of Object.entries(vars)) {
    out = out.split(`{${name}}`).join(value);
  }
  return out;
}

export interface ScheduleBatchRow {
  id: string;
  ok: boolean;
  detail: string;
}

function rowsFromReport(
  saved: readonly string[],
  failures: readonly ScheduleBatchFailure[],
  okText: string,
): ScheduleBatchRow[] {
  const rows: ScheduleBatchRow[] = saved.map((id) => ({
    id,
    ok: true,
    detail: okText,
  }));
  for (const failure of failures) {
    rows.push({ id: failure.id, ok: false, detail: failure.message });
  }
  return rows;
}

/** Merge one run into the visible per-item report; retried ids flip. */
function mergeRows(
  previous: ScheduleBatchRow[] | null,
  saved: readonly string[],
  failures: readonly ScheduleBatchFailure[],
  okText: string,
): ScheduleBatchRow[] {
  const next = new Map<string, ScheduleBatchRow>();
  for (const row of previous ?? []) {
    next.set(row.id, row);
  }
  for (const row of rowsFromReport(saved, failures, okText)) {
    next.set(row.id, row);
  }
  return [...next.values()];
}

/** Which batch op left failures behind for the retry button. */
type ScheduleFailedOp = "save" | "clear" | "load";

export interface ItemScheduleDatesPanelProps {
  itemId?: string;
  onItemIdChange?: (itemId: string) => void;
}

function scheduleErrorMessage(err: unknown): string {
  return scheduleBatchFailureMessage(err);
}

export function ItemScheduleDatesPanel({
  itemId,
  onItemIdChange,
}: ItemScheduleDatesPanelProps): React.ReactElement {
  const [inputId, setInputId] = useState(itemId ?? "");
  const [loadedId, setLoadedId] = useState("");
  const [startLocal, setStartLocal] = useState("");
  const [endLocal, setEndLocal] = useState("");
  const [comments, setComments] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [confirmingClear, setConfirmingClear] = useState(false);
  const [clearing, setClearing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [report, setReport] = useState<ScheduleBatchRow[] | null>(null);
  const [reportTotal, setReportTotal] = useState(0);
  const [failedIds, setFailedIds] = useState<string[]>([]);
  const [failedOp, setFailedOp] = useState<ScheduleFailedOp | null>(null);

  useEffect(() => {
    setInputId(itemId ?? "");
    setFailedIds([]);
    setFailedOp(null);
  }, [itemId]);

  const load = useCallback(async (rawId: string) => {
    const ids = parseScheduleItemIds(rawId);
    if (ids.length === 0) {
      setLoadedId("");
      setStartLocal("");
      setEndLocal("");
      setComments("");
      setSuccess(null);
      setReport(null);
      setReportTotal(0);
      setError(
        rawId.trim()
          ? message(MSG.PUBLISH_ITEM_HISTORY_INVALID_ID)
          : message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID),
      );
      return;
    }
    if (ids.length === 1) {
      await loadSingle(ids[0]);
      return;
    }
    setLoading(true);
    setError(null);
    setSuccess(null);
    setReport(null);
    setReportTotal(ids.length);
    setFailedIds([]);
    setFailedOp(null);
    setConfirmingClear(false);
    setLoadedId(ids[0]);
    try {
      await loadBatch(ids);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadSingle = useCallback(async (safe: string) => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    setReport(null);
    setReportTotal(0);
    setFailedIds([]);
    setFailedOp(null);
    setConfirmingClear(false);
    setLoadedId(safe);
    try {
      const dates = await fetchItemScheduleDates(safe);
      setStartLocal(serverDateToDatetimeLocal(dates.startDate));
      setEndLocal(serverDateToDatetimeLocal(dates.endDate));
      setComments(dates.comments ?? "");
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setStartLocal("");
      setEndLocal("");
      setComments("");
      setError(scheduleErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const initial = mapIdParam(itemId);
    if (initial) {
      void load(initial);
    }
  }, [itemId, load]);

  function onLookup(e: React.FormEvent): void {
    e.preventDefault();
    const ids = parseScheduleItemIds(inputId);
    if (ids.length > 1) {
      // Keep the whole list in the box: propagating one id would echo back
      // through the shared workspace itemId prop and clobber the rest.
      void load(ids.join(","));
      return;
    }
    const next = ids[0] ?? mapIdParam(inputId) ?? inputId.trim();
    onItemIdChange?.(mapIdParam(next) || next);
    void load(ids.length > 0 ? ids.join(",") : next);
  }

  async function onSave(e: React.FormEvent): Promise<void> {
    e.preventDefault();
    const ids = parseScheduleItemIds(inputId);
    if (ids.length === 0) {
      const fallback = mapIdParam(loadedId);
      if (fallback) {
        ids.push(fallback);
      }
    }
    if (ids.length === 0) {
      setError(message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID));
      setReport(null);
      setReportTotal(0);
      return;
    }
    const startDate = datetimeLocalToServerDate(startLocal);
    const endDate = datetimeLocalToServerDate(endLocal);
    const rangeError = validateScheduleDateRange(startDate, endDate);
    if (rangeError) {
      setError(message(rangeError));
      setSuccess(null);
      setReport(null);
      setReportTotal(0);
      return;
    }
    if (ids.length > 1) {
      await saveBatch(ids, {
        startDate,
        endDate,
        comments: comments.trim().slice(0, COMMENT_MAX),
      });
      return;
    }
    const safe = ids[0];
    setSaving(true);
    setError(null);
    setSuccess(null);
    setReport(null);
    setReportTotal(0);
    try {
      const savedComments = comments.trim().slice(0, COMMENT_MAX);
      await saveItemScheduleDates({
        itemId: safe,
        startDate,
        endDate,
        comments: savedComments,
      });
      // GET item dates does not return the workflow comment. Reload start/end
      // only so a successful save still shows the stored schedule.
      const saved = await fetchItemScheduleDates(safe);
      setStartLocal(serverDateToDatetimeLocal(saved.startDate));
      setEndLocal(serverDateToDatetimeLocal(saved.endDate));
      setComments(savedComments);
      setLoadedId(safe);
      setConfirmingClear(false);
      setSuccess(message(MSG.PUBLISH_SCHEDULE_SAVED));
      setFailedIds([]);
      setFailedOp(null);
      onItemIdChange?.(safe);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setFailedIds([safe]);
      setFailedOp("save");
      setError(scheduleErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  /**
   * Load every id; a per-item load failure never stops the rest.
   *
   * The initial lookup fills the edit form from the first loaded item.
   * Retries pass `{ fillForm: false }` so they only merge report rows and
   * refresh failed/success state — never clobbering the user's in-progress
   * form values or the currently loaded id.
   */
  async function loadBatch(
    ids: string[],
    options?: { fillForm?: boolean },
  ): Promise<void> {
    const fillForm = options?.fillForm ?? true;
    setError(null);
    setSuccess(null);
    setReportTotal(ids.length);
    const batch = await loadScheduleDatesBatch(ids);
    if (fillForm) {
      const first = batch.loaded[0];
      if (first) {
        setStartLocal(serverDateToDatetimeLocal(first.dates.startDate));
        setEndLocal(serverDateToDatetimeLocal(first.dates.endDate));
        setComments(first.dates.comments ?? "");
      } else {
        setStartLocal("");
        setEndLocal("");
        setComments("");
      }
    }
    setReport((previous) =>
      mergeRows(
        previous,
        batch.loaded.map((row) => row.id),
        batch.failures,
        message(MSG.PUBLISH_SCHEDULE_BATCH_ITEM_OK),
      ),
    );
    if (fillForm) {
      setLoadedId(ids[0]);
    }
    setConfirmingClear(false);
    if (batch.failures.length === 0) {
      setFailedIds([]);
      setFailedOp(null);
      setSuccess(
        fill(message(MSG.PUBLISH_SCHEDULE_BATCH_LOADED), {
          total: String(batch.total),
        }),
      );
    } else {
      setFailedIds(batch.failures.map((failure) => failure.id));
      setFailedOp("load");
      setSuccess(null);
      setError(
        fill(message(MSG.PUBLISH_SCHEDULE_BATCH_LOAD_PARTIAL), {
          loaded: String(batch.loaded.length),
          total: String(batch.total),
          detail: formatScheduleBatchDetail(batch.failures),
        }),
      );
    }
  }

  async function saveBatch(
    ids: string[],
    dates: { startDate: string; endDate: string; comments: string },
  ): Promise<void> {
    setSaving(true);
    setError(null);
    setSuccess(null);
    setReportTotal(ids.length);
    try {
      const batch = await saveScheduleDatesBatch(ids, dates);
      setReport((previous) =>
        mergeRows(
          previous,
          batch.saved,
          batch.failures,
          message(MSG.PUBLISH_SCHEDULE_BATCH_ITEM_OK),
        ),
      );
      setLoadedId(ids[0]);
      setConfirmingClear(false);
      if (batch.failures.length === 0) {
        setFailedIds([]);
        setFailedOp(null);
        setSuccess(
          fill(message(MSG.PUBLISH_SCHEDULE_BATCH_SAVED), {
            total: String(batch.total),
          }),
        );
      } else {
        setFailedIds(batch.failures.map((failure) => failure.id));
        setFailedOp("save");
        setSuccess(null);
        setError(
          fill(message(MSG.PUBLISH_SCHEDULE_BATCH_PARTIAL), {
            saved: String(batch.saved.length),
            total: String(batch.total),
            detail: formatScheduleBatchDetail(batch.failures),
          }),
        );
      }
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(scheduleErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  async function clearBatch(ids: string[]): Promise<void> {
    setClearing(true);
    setError(null);
    setSuccess(null);
    setReportTotal(ids.length);
    try {
      const batch = await clearScheduleDatesBatch(
        ids,
        message(MSG.PUBLISH_SCHEDULE_INVALID),
      );
      setReport((previous) =>
        mergeRows(
          previous,
          batch.saved,
          batch.failures,
          message(MSG.PUBLISH_SCHEDULE_BATCH_ITEM_OK),
        ),
      );
      const first = batch.saved[0] ?? batch.failures[0]?.id ?? ids[0];
      const reloaded = await fetchItemScheduleDates(first);
      setStartLocal(serverDateToDatetimeLocal(reloaded.startDate));
      setEndLocal(serverDateToDatetimeLocal(reloaded.endDate));
      setComments("");
      setLoadedId(ids[0]);
      if (batch.failures.length === 0) {
        setFailedIds([]);
        setFailedOp(null);
        setConfirmingClear(false);
        setSuccess(
          fill(message(MSG.PUBLISH_SCHEDULE_BATCH_CLEARED), {
            total: String(batch.total),
          }),
        );
      } else {
        setFailedIds(batch.failures.map((failure) => failure.id));
        setFailedOp("clear");
        setSuccess(null);
        setError(
          fill(message(MSG.PUBLISH_SCHEDULE_BATCH_CLEAR_PARTIAL), {
            saved: String(batch.saved.length),
            total: String(batch.total),
            detail: formatScheduleBatchDetail(batch.failures),
          }),
        );
      }
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(scheduleErrorMessage(err));
    } finally {
      setClearing(false);
    }
  }

  async function onRetry(): Promise<void> {
    const ids = failedIds;
    if (ids.length === 0 || failedOp === null) {
      return;
    }
    if (failedOp === "load") {
      setLoading(true);
      try {
        await loadBatch(ids, { fillForm: false });
      } finally {
        setLoading(false);
      }
      return;
    }
    if (failedOp === "clear") {
      await clearBatch(ids);
      return;
    }
    const startDate = datetimeLocalToServerDate(startLocal);
    const endDate = datetimeLocalToServerDate(endLocal);
    const rangeError = validateScheduleDateRange(startDate, endDate);
    if (rangeError) {
      setError(message(rangeError));
      setSuccess(null);
      return;
    }
    await saveBatch(ids, {
      startDate,
      endDate,
      comments: comments.trim().slice(0, COMMENT_MAX),
    });
  }

  async function onClear(e: React.FormEvent): Promise<void> {
    e.preventDefault();
    const ids = parseScheduleItemIds(inputId);
    if (ids.length === 0) {
      const fallback = mapIdParam(loadedId);
      if (fallback) {
        ids.push(fallback);
      }
    }
    if (ids.length === 0) {
      setError(message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID));
      setReport(null);
      setReportTotal(0);
      return;
    }
    if (ids.length > 1) {
      await clearBatch(ids);
      return;
    }
    const safe = ids[0];
    setClearing(true);
    setError(null);
    setSuccess(null);
    setReport(null);
    setReportTotal(0);
    try {
      // Empty dates clear the stored schedule through the existing route.
      await saveItemScheduleDates({
        itemId: safe,
        startDate: "",
        endDate: "",
        comments: "",
      });
      const reloaded = await fetchItemScheduleDates(safe);
      // Show the stored schedule: only empty when the reload has neither date.
      setStartLocal(serverDateToDatetimeLocal(reloaded.startDate));
      setEndLocal(serverDateToDatetimeLocal(reloaded.endDate));
      setComments("");
      setLoadedId(safe);
      onItemIdChange?.(safe);
      if (!reloaded.startDate && !reloaded.endDate) {
        setConfirmingClear(false);
        setFailedIds([]);
        setFailedOp(null);
        setSuccess(message(MSG.PUBLISH_SCHEDULE_CLEARED));
      } else {
        setFailedIds([safe]);
        setFailedOp("clear");
        setError(message(MSG.PUBLISH_SCHEDULE_INVALID));
      }
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setFailedIds([safe]);
      setFailedOp("clear");
      setError(scheduleErrorMessage(err));
    } finally {
      setClearing(false);
    }
  }

  return (
    <section
      data-testid="item-schedule-dates"
      aria-labelledby="item-schedule-dates-title"
      style={{
        marginBottom: 20,
        padding: 12,
        border: "1px solid #ddd",
        borderRadius: 6,
        background: "#fafafa",
      }}
    >
      <h2
        id="item-schedule-dates-title"
        style={{ margin: "0 0 8px", fontSize: "1.05rem" }}
      >
        {message(MSG.PUBLISH_SCHEDULE)}
      </h2>
      <p style={{ margin: "0 0 12px", fontSize: "0.85rem", color: "#555" }}>
        {message(MSG.PUBLISH_SCHEDULE_HINT)}{" "}
        {message(MSG.PUBLISH_SCHEDULE_IDS_HINT)}
      </p>
      <form onSubmit={onLookup} style={toolbarStyle}>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 280 }}>
          <label htmlFor="item-schedule-id">
            {message(MSG.PUBLISH_ITEM_HISTORY_ITEM_ID)}
          </label>
          <input
            id="item-schedule-id"
            name="itemId"
            value={inputId}
            onChange={(e) => {
              setInputId(e.target.value);
              setFailedIds([]);
              setFailedOp(null);
            }}
            data-testid="item-schedule-id"
            autoComplete="off"
          />
        </div>
        <button
          type="submit"
          style={primaryButtonStyle}
          data-testid="item-schedule-load"
          disabled={loading || saving}
        >
          {message(MSG.PUBLISH_SCHEDULE_LOAD)}
        </button>
      </form>
      <form onSubmit={(e) => void onSave(e)}>
        <label style={{ display: "block", fontSize: 13, marginBottom: 10 }}>
          {message(MSG.PUBLISH_SCHEDULE_PUBLISH_DATE)}
          <input
            type="datetime-local"
            data-testid="item-schedule-start"
            value={startLocal}
            onChange={(e) => {
              setStartLocal(e.target.value);
              setError(null);
              setSuccess(null);
            }}
            style={{ display: "block", width: "100%", maxWidth: 280, marginTop: 6, padding: 6 }}
          />
        </label>
        <label style={{ display: "block", fontSize: 13, marginBottom: 10 }}>
          {message(MSG.PUBLISH_SCHEDULE_REMOVAL_DATE)}
          <input
            type="datetime-local"
            data-testid="item-schedule-end"
            value={endLocal}
            onChange={(e) => {
              setEndLocal(e.target.value);
              setError(null);
              setSuccess(null);
            }}
            style={{ display: "block", width: "100%", maxWidth: 280, marginTop: 6, padding: 6 }}
          />
        </label>
        <label style={{ display: "block", fontSize: 13, marginBottom: 10 }}>
          {message(MSG.PUBLISH_SCHEDULE_COMMENTS)}
          <textarea
            data-testid="item-schedule-comments"
            value={comments}
            maxLength={COMMENT_MAX}
            onChange={(e) => setComments(e.target.value)}
            rows={3}
            style={{ display: "block", width: "100%", maxWidth: 420, marginTop: 6, padding: 6 }}
          />
        </label>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button
            type="button"
            data-testid="item-schedule-clear"
            disabled={loading || saving}
            onClick={() => {
              setStartLocal("");
              setEndLocal("");
              setError(null);
              setSuccess(null);
            }}
          >
            {message(MSG.PUBLISH_SCHEDULE_CLEAR)}
          </button>
          <button
            type="submit"
            style={primaryButtonStyle}
            data-testid="item-schedule-save"
            disabled={loading || saving || clearing}
          >
            {message(MSG.PUBLISH_SCHEDULE_SAVE)}
          </button>
          <button
            type="button"
            data-testid="item-schedule-clear-submit"
            disabled={loading || saving || clearing}
            onClick={() => {
              const ids = parseScheduleItemIds(inputId);
              const fallback = ids.length > 0 ? ids : [mapIdParam(loadedId)];
              if (!fallback[0]) {
                setError(message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID));
                return;
              }
              setError(null);
              setSuccess(null);
              setReport(null);
              setReportTotal(ids.length > 1 ? ids.length : 0);
              setConfirmingClear(true);
            }}
          >
            {message(MSG.PUBLISH_SCHEDULE_CLEAR_SUBMIT)}
          </button>
        </div>
      </form>
      {confirmingClear && (
        <div data-testid="item-schedule-clear-confirm" style={{ marginTop: 12 }}>
          <p style={{ margin: "0 0 8px", fontSize: 13 }}>
            {reportTotal > 1
              ? fill(message(MSG.PUBLISH_SCHEDULE_CLEAR_CONFIRM_MULTI), {
                  total: String(reportTotal),
                })
              : message(MSG.PUBLISH_SCHEDULE_CLEAR_CONFIRM)}
          </p>
          <form onSubmit={(e) => void onClear(e)}>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              <button
                type="submit"
                style={primaryButtonStyle}
                data-testid="item-schedule-clear-yes"
                disabled={loading || saving || clearing}
              >
                {message(MSG.PUBLISH_SCHEDULE_CLEAR_SUBMIT)}
              </button>
              <button
                type="button"
                data-testid="item-schedule-clear-cancel"
                disabled={clearing}
                onClick={() => setConfirmingClear(false)}
              >
                {message(MSG.PUBLISH_SCHEDULE_CLEAR_CANCEL)}
              </button>
            </div>
          </form>
        </div>
      )}
      {failedIds.length > 0 && !loading && !saving && !clearing && (
        <button
          type="button"
          style={{ ...primaryButtonStyle, marginTop: 12 }}
          data-testid="item-schedule-retry"
          onClick={() => void onRetry()}
        >
          {message(MSG.PUBLISH_SCHEDULE_RETRY_FAILED)}
        </button>
      )}
      {report && report.length > 0 && (
        <ul
          data-testid="item-schedule-batch-report"
          aria-label={message(MSG.PUBLISH_SCHEDULE)}
          style={{ margin: "12px 0 0", paddingLeft: 18, fontSize: 13 }}
        >
          {report.map((row) => (
            <li
              key={row.id}
              data-testid={`item-schedule-batch-item-${row.id}`}
            >
              <span>{row.id}</span>
              {": "}
              <span>{row.detail}</span>
            </li>
          ))}
        </ul>
      )}
      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert" data-testid="item-schedule-error">
          {error}
        </p>
      )}
      {success && (
        <p data-testid="item-schedule-success" style={{ color: "#166534", fontSize: 13 }}>
          {success}
        </p>
      )}
    </section>
  );
}
