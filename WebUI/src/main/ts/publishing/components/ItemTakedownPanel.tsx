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
  formatApiError,
  isApiError,
  isSessionRedirectError,
} from "../../api/client";
import {
  formatTakedownConfirmBody,
  linkedPagePathsForConfirm,
  loadLinkedPagesForTakedown,
  takedownSelectedItem,
  type LinkedPageForTakedown,
} from "../../contentExplorer/itemPublish";
import { message, MSG } from "../../i18n/message";
import { mapIdParam } from "../deepLinkMap";
import {
  formatTakedownBatchDetail,
  parseTakedownItemIds,
  reviewTakedownBatch,
  runTakedownBatch,
  type TakedownBatchReviewReport,
} from "../itemTakedownBatch";
import {
  mapTakedownKind,
  pathItemForTakedown,
  type TakedownKind,
} from "../itemTakedown";
import {
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

function fill(template: string, vars: Record<string, string>): string {
  let out = template;
  for (const [name, value] of Object.entries(vars)) {
    out = out.split(`{${name}}`).join(value);
  }
  return out;
}

export interface TakedownBatchRow {
  id: string;
  ok: boolean;
  detail: string;
}

function rowsFromReport(
  takenDown: readonly string[],
  failures: readonly { id: string; message: string }[],
  okText: string,
): TakedownBatchRow[] {
  const rows: TakedownBatchRow[] = takenDown.map((id) => ({
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
function mergeReport(
  previous: TakedownBatchRow[] | null,
  takenDown: readonly string[],
  failures: readonly { id: string; message: string }[],
  okText: string,
): TakedownBatchRow[] {
  const next = new Map<string, TakedownBatchRow>();
  for (const row of previous ?? []) {
    next.set(row.id, row);
  }
  for (const row of rowsFromReport(takenDown, failures, okText)) {
    next.set(row.id, row);
  }
  return [...next.values()];
}

export interface ItemTakedownPanelProps {
  itemId?: string;
  onItemIdChange?: (itemId: string) => void;
}

/** Which batch op left failures behind for the retry button. */
type TakedownFailedOp = "run" | "review";

function takedownErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 400) {
      return formatApiError(err, message(MSG.PUBLISH_BADCONFIG));
    }
    if (err.status === 404) {
      return formatApiError(err, message(MSG.PUBLISH_NOW_NOT_FOUND));
    }
    if (err.status === 409) {
      return formatApiError(err, message(MSG.PUBLISH_SCHEDULE_CONFLICT));
    }
  }
  const text = formatApiError(err, message(MSG.PUBLISH_ERROR));
  if (/\bFORBIDDEN\b/i.test(text)) {
    return message(MSG.PUBLISH_FORBIDDEN);
  }
  if (/\bBADCONFIG\b/i.test(text)) {
    return message(MSG.PUBLISH_BADCONFIG);
  }
  if (/\b404\b|\bNOT FOUND\b/i.test(text)) {
    return message(MSG.PUBLISH_NOW_NOT_FOUND);
  }
  if (/\b409\b|\bCONFLICT\b|checked out|editing this/i.test(text)) {
    return message(MSG.PUBLISH_SCHEDULE_CONFLICT);
  }
  return text;
}

export function ItemTakedownPanel({
  itemId,
  onItemIdChange,
}: ItemTakedownPanelProps): React.ReactElement {
  const [inputId, setInputId] = useState(itemId ?? "");
  const [kind, setKind] = useState<TakedownKind>("page");
  const [linked, setLinked] = useState<LinkedPageForTakedown[]>([]);
  const [reviewed, setReviewed] = useState(false);
  const [failedIds, setFailedIds] = useState<string[]>([]);
  const [failedOp, setFailedOp] = useState<TakedownFailedOp | null>(null);
  const [multiIds, setMultiIds] = useState<string[]>([]);
  const [multiLinked, setMultiLinked] = useState<
    Record<string, LinkedPageForTakedown[]>
  >({});
  const [report, setReport] = useState<TakedownBatchRow[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    setInputId(itemId ?? "");
    setFailedIds([]);
    setFailedOp(null);
  }, [itemId]);

  const applyReviewOutcome = useCallback(
    (batch: TakedownBatchReviewReport): void => {
      if (batch.failures.length > 0) {
        setFailedIds(batch.failures.map((failure) => failure.id));
        setFailedOp("review");
        setError(
          fill(message(MSG.PUBLISH_TAKEDOWN_BATCH_REVIEW_PARTIAL), {
            // reviewed[] keeps failed ids (with no linked pages), so the
            // loaded count excludes them.
            loaded: String(batch.reviewed.length - batch.failures.length),
            total: String(batch.total),
            detail: formatTakedownBatchDetail(batch.failures),
          }),
        );
      } else {
        setFailedIds([]);
        setFailedOp(null);
      }
    },
    [],
  );

  const review = useCallback(async (rawId: string) => {
    const ids = parseTakedownItemIds(rawId);
    if (ids.length === 0) {
      setLinked([]);
      setReviewed(false);
      setFailedIds([]);
      setFailedOp(null);
      setMultiIds([]);
      setMultiLinked({});
      setReport(null);
      setSuccess(null);
      setError(
        rawId.trim()
          ? message(MSG.PUBLISH_ITEM_HISTORY_INVALID_ID)
          : message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID),
      );
      return;
    }
    if (ids.length > 1) {
      await reviewBatch(ids);
      return;
    }
    const safe = ids[0];
    setMultiIds([]);
    setFailedIds([]);
    setFailedOp(null);
    setMultiLinked({});
    setReport(null);
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      // Linked-list failures return [] (classic Finder still confirms).
      const pages = await loadLinkedPagesForTakedown(safe);
      setLinked(pages);
      // Stash for a single-item retry: runBatch reuses reviewed linked
      // pages instead of taking down with no linked-page list.
      setMultiLinked({ [safe]: pages });
      setReviewed(true);
    } finally {
      setLoading(false);
    }
  }, []);

  const reviewBatch = useCallback(
    async (ids: string[]) => {
      setLoading(true);
      setError(null);
      setSuccess(null);
      setFailedIds([]);
      setFailedOp(null);
      setReport(null);
      setLinked([]);
      try {
        const batch = await reviewTakedownBatch(ids);
        const byId: Record<string, LinkedPageForTakedown[]> = {};
        for (const row of batch.reviewed) {
          byId[row.id] = row.linked;
        }
        setMultiIds(ids);
        setMultiLinked(byId);
        setReviewed(true);
        applyReviewOutcome(batch);
      } finally {
        setLoading(false);
      }
    },
    [applyReviewOutcome],
  );

  /**
   * Re-read linked pages for the failed review ids only and merge the
   * results into the same confirm; already-reviewed ids keep their
   * linked pages, so no second full lookup is needed.
   */
  const retryReview = useCallback(
    async (ids: string[]) => {
      setLoading(true);
      setError(null);
      setSuccess(null);
      try {
        const batch = await reviewTakedownBatch(ids);
        const byId: Record<string, LinkedPageForTakedown[]> = {};
        for (const row of batch.reviewed) {
          byId[row.id] = row.linked;
        }
        setMultiLinked((previous) => ({ ...previous, ...byId }));
        setReviewed(true);
        applyReviewOutcome(batch);
      } finally {
        setLoading(false);
      }
    },
    [applyReviewOutcome],
  );

  useEffect(() => {
    const initial = mapIdParam(itemId);
    if (initial) {
      void review(initial);
    }
  }, [itemId, review]);

  function onLookup(e: React.FormEvent): void {
    e.preventDefault();
    const ids = parseTakedownItemIds(inputId);
    if (ids.length > 1) {
      // Keep the whole list in the box: propagating one id would echo back
      // through the shared workspace itemId prop and clobber the rest.
      void review(ids.join(","));
      return;
    }
    const next = mapIdParam(inputId) || inputId.trim();
    onItemIdChange?.(mapIdParam(next) || next);
    void review(next);
  }

  async function runBatch(ids: string[]): Promise<void> {
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      const batch = await runTakedownBatch(ids, kind, multiLinked);
      setReport((previous) =>
        mergeReport(
          previous,
          batch.takenDown,
          batch.failures,
          message(MSG.PUBLISH_TAKEDOWN_BATCH_ITEM_OK),
        ),
      );
      if (batch.failures.length === 0) {
        setFailedIds([]);
        setFailedOp(null);
        setSuccess(
          fill(message(MSG.PUBLISH_TAKEDOWN_BATCH_DONE), {
            total: String(batch.total),
          }),
        );
      } else {
        setFailedIds(batch.failures.map((failure) => failure.id));
        setFailedOp("run");
        setSuccess(null);
        setError(
          fill(message(MSG.PUBLISH_TAKEDOWN_BATCH_PARTIAL), {
            saved: String(batch.takenDown.length),
            total: String(batch.total),
            detail: formatTakedownBatchDetail(batch.failures),
          }),
        );
      }
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(takedownErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function onRetry(): Promise<void> {
    if (failedOp === "review") {
      await retryReview(failedIds);
      return;
    }
    await runBatch(failedIds);
  }

  async function onTakeDown(e: React.FormEvent): Promise<void> {
    e.preventDefault();
    const ids = parseTakedownItemIds(inputId);
    if (ids.length > 1) {
      if (multiIds.join(",") !== ids.join(",")) {
        await review(ids.join(","));
        return;
      }
      await runBatch(ids);
      return;
    }
    const safe = mapIdParam(inputId) || mapIdParam(itemId);
    if (!safe) {
      setError(message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID));
      setSuccess(null);
      return;
    }
    if (!reviewed) {
      await review(safe);
      return;
    }
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      const ok = await takedownSelectedItem(
        pathItemForTakedown(safe, kind),
        linked,
      );
      if (!ok) {
        setFailedIds([safe]);
        setFailedOp("run");
        setError(message(MSG.PUBLISH_ERROR));
        return;
      }
      onItemIdChange?.(safe);
      setFailedIds([]);
      setFailedOp(null);
      setSuccess(message(MSG.PUBLISH_TAKEDOWN_SUCCESS));
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setFailedIds([safe]);
      setFailedOp("run");
      setError(takedownErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  const linkedPaths = linkedPagePathsForConfirm(linked);

  return (
    <section
      data-testid="item-takedown"
      aria-labelledby="item-takedown-title"
      style={{
        marginBottom: 20,
        padding: 12,
        border: "1px solid #ddd",
        borderRadius: 6,
        background: "#fafafa",
      }}
    >
      <h2
        id="item-takedown-title"
        style={{ margin: "0 0 8px", fontSize: "1.05rem" }}
      >
        {message(MSG.PUBLISH_TAKEDOWN)}
      </h2>
      <p style={{ margin: "0 0 12px", fontSize: "0.85rem", color: "#555" }}>
        {message(MSG.PUBLISH_TAKEDOWN_HINT)}{" "}
        {message(MSG.PUBLISH_TAKEDOWN_IDS_HINT)}
      </p>
      <form onSubmit={onLookup} style={toolbarStyle}>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 280 }}>
          <label htmlFor="item-takedown-id">
            {message(MSG.PUBLISH_ITEM_HISTORY_ITEM_ID)}
          </label>
          <input
            id="item-takedown-id"
            name="itemId"
            value={inputId}
            onChange={(e) => {
              setInputId(e.target.value);
              setReviewed(false);
              setFailedIds([]);
              setFailedOp(null);
              setMultiIds([]);
              setSuccess(null);
            }}
            data-testid="item-takedown-id"
            autoComplete="off"
          />
        </div>
        <fieldset
          style={{ border: "none", margin: 0, padding: 0 }}
          data-testid="item-takedown-kind"
        >
          <legend style={{ fontSize: 13 }}>
            {message(MSG.PUBLISH_TAKEDOWN_KIND)}
          </legend>
          <label style={{ marginRight: 12, fontSize: 13 }}>
            <input
              type="radio"
              name="item-takedown-kind"
              value="page"
              checked={kind === "page"}
              onChange={(e) => setKind(mapTakedownKind(e.target.value))}
              data-testid="item-takedown-kind-page"
            />{" "}
            {message(MSG.PUBLISH_TAKEDOWN_KIND_PAGE)}
          </label>
          <label style={{ fontSize: 13 }}>
            <input
              type="radio"
              name="item-takedown-kind"
              value="resource"
              checked={kind === "resource"}
              onChange={(e) => setKind(mapTakedownKind(e.target.value))}
              data-testid="item-takedown-kind-resource"
            />{" "}
            {message(MSG.PUBLISH_TAKEDOWN_KIND_RESOURCE)}
          </label>
        </fieldset>
        <button
          type="submit"
          style={primaryButtonStyle}
          data-testid="item-takedown-review"
          disabled={loading || submitting}
        >
          {message(MSG.PUBLISH_TAKEDOWN_LOAD)}
        </button>
      </form>
      {reviewed && multiIds.length > 1 && (
        <div data-testid="item-takedown-confirm" style={{ marginTop: 12 }}>
          <p style={{ margin: "0 0 8px", fontSize: 13 }}>
            {fill(message(MSG.PUBLISH_TAKEDOWN_BATCH_CONFIRM), {
              total: String(multiIds.length),
            })}
          </p>
          {multiIds.map((id) => {
            const paths = linkedPagePathsForConfirm(multiLinked[id] ?? []);
            return (
              <div
                key={id}
                data-testid={`item-takedown-batch-item-${id}`}
                style={{ marginBottom: 8 }}
              >
                <p style={{ margin: "0 0 4px", fontSize: 13 }}>
                  <strong>{id}</strong>
                </p>
                {paths.length > 0 ? (
                  <ul
                    data-testid={`item-takedown-batch-linked-${id}`}
                    style={{ margin: "0 0 4px", paddingLeft: 18 }}
                  >
                    {paths.map((path) => (
                      <li key={path}>{path}</li>
                    ))}
                  </ul>
                ) : (
                  <p
                    data-testid={`item-takedown-batch-linked-empty-${id}`}
                    style={{ fontSize: 13, margin: "0 0 4px" }}
                  >
                    {message(MSG.PUBLISH_TAKEDOWN_NONE)}
                  </p>
                )}
              </div>
            );
          })}
          <form onSubmit={(e) => void onTakeDown(e)}>
            <button
              type="submit"
              style={primaryButtonStyle}
              data-testid="item-takedown-submit"
              disabled={loading || submitting}
            >
              {message(MSG.PUBLISH_TAKEDOWN_SUBMIT)}
            </button>
          </form>
        </div>
      )}
      {reviewed && multiIds.length <= 1 && (
        <div data-testid="item-takedown-confirm" style={{ marginTop: 12 }}>
          <p style={{ margin: "0 0 8px", fontSize: 13, whiteSpace: "pre-wrap" }}>
            {formatTakedownConfirmBody(linked)}
          </p>
          {linkedPaths.length > 0 ? (
            <ul data-testid="item-takedown-linked" style={{ margin: "0 0 12px" }}>
              {linkedPaths.map((path) => (
                <li key={path}>{path}</li>
              ))}
            </ul>
          ) : (
            <p data-testid="item-takedown-linked-empty" style={{ fontSize: 13 }}>
              {message(MSG.PUBLISH_TAKEDOWN_NONE)}
            </p>
          )}
          <form onSubmit={(e) => void onTakeDown(e)}>
            <button
              type="submit"
              style={primaryButtonStyle}
              data-testid="item-takedown-submit"
              disabled={loading || submitting}
            >
              {message(MSG.PUBLISH_TAKEDOWN_SUBMIT)}
            </button>
          </form>
        </div>
      )}
      {report && report.length > 0 && (
        <ul
          data-testid="item-takedown-batch-report"
          aria-label={message(MSG.PUBLISH_TAKEDOWN)}
          style={{ margin: "12px 0 0", paddingLeft: 18, fontSize: 13 }}
        >
          {report.map((row) => (
            <li
              key={row.id}
              data-testid={`item-takedown-batch-report-item-${row.id}`}
            >
              <span>{row.id}</span>
              {": "}
              <span>{row.detail}</span>
            </li>
          ))}
        </ul>
      )}
      {failedIds.length > 0 && failedOp !== null && !submitting && !loading && (
        <button
          type="button"
          style={{ ...primaryButtonStyle, marginTop: 12 }}
          data-testid="item-takedown-retry"
          onClick={() => void onRetry()}
        >
          {message(MSG.PUBLISH_TAKEDOWN_RETRY_FAILED)}
        </button>
      )}
      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert" data-testid="item-takedown-error">
          {error}
        </p>
      )}
      {success && (
        <p
          data-testid="item-takedown-success"
          style={{ color: "#166534", fontSize: 13 }}
        >
          {success}
        </p>
      )}
    </section>
  );
}
