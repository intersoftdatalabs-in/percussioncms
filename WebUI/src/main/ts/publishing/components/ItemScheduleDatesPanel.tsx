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
  formatApiError,
  isApiError,
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
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

const COMMENT_MAX = 500;

export interface ItemScheduleDatesPanelProps {
  itemId?: string;
  onItemIdChange?: (itemId: string) => void;
}

function scheduleErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_SCHEDULE_FORBIDDEN));
    }
    if (err.status === 400) {
      return formatApiError(err, message(MSG.PUBLISH_SCHEDULE_INVALID));
    }
    if (err.status === 409) {
      return formatApiError(err, message(MSG.PUBLISH_SCHEDULE_CONFLICT));
    }
  }
  const text = formatApiError(err, message(MSG.PUBLISH_ERROR));
  if (/\bFORBIDDEN\b/i.test(text)) {
    return message(MSG.PUBLISH_SCHEDULE_FORBIDDEN);
  }
  if (/\bINVALID\b/i.test(text)) {
    return message(MSG.PUBLISH_SCHEDULE_INVALID);
  }
  return text;
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
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    setInputId(itemId ?? "");
  }, [itemId]);

  const load = useCallback(async (rawId: string) => {
    const safe = mapIdParam(rawId);
    if (!safe) {
      setLoadedId("");
      setStartLocal("");
      setEndLocal("");
      setComments("");
      setSuccess(null);
      setError(
        rawId.trim()
          ? message(MSG.PUBLISH_ITEM_HISTORY_INVALID_ID)
          : message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID),
      );
      return;
    }
    setLoading(true);
    setError(null);
    setSuccess(null);
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
    const next = mapIdParam(inputId) || inputId.trim();
    onItemIdChange?.(mapIdParam(next) || next);
    void load(next);
  }

  async function onSave(e: React.FormEvent): Promise<void> {
    e.preventDefault();
    const safe = mapIdParam(inputId) || loadedId;
    if (!safe) {
      setError(message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID));
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
    setSaving(true);
    setError(null);
    setSuccess(null);
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
      setSuccess(message(MSG.PUBLISH_SCHEDULE_SAVED));
      onItemIdChange?.(safe);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(scheduleErrorMessage(err));
    } finally {
      setSaving(false);
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
        {message(MSG.PUBLISH_SCHEDULE_HINT)}
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
            onChange={(e) => setInputId(e.target.value)}
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
            disabled={loading || saving}
          >
            {message(MSG.PUBLISH_SCHEDULE_SAVE)}
          </button>
        </div>
      </form>
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
