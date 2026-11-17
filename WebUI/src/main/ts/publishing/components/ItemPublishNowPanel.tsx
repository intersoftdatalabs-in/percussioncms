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
import {
  formatApiError,
  isApiError,
  isSessionRedirectError,
} from "../../api/client";
import { publishSelectedItem } from "../../contentExplorer/itemPublish";
import { message, MSG } from "../../i18n/message";
import { mapIdParam } from "../deepLinkMap";
import {
  clearTemplateAssemblyStale,
  listStaleAssemblyTemplates,
  type StaleAssemblyEntry,
} from "../../design/staleAssembly";
import {
  formatPublishNowBatchDetail,
  parsePublishNowItemIds,
  runPublishNowBatch,
} from "../itemPublishNowBatch";
import {
  mapPublishNowKind,
  pathItemForPublishNow,
  type PublishNowKind,
} from "../itemPublishNow";
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

export interface PublishNowBatchRow {
  id: string;
  ok: boolean;
  detail: string;
}

function rowsFromReport(
  published: readonly string[],
  failures: readonly { id: string; message: string }[],
  okText: string,
): PublishNowBatchRow[] {
  const rows: PublishNowBatchRow[] = published.map((id) => ({
    id,
    ok: true,
    detail: okText,
  }));
  for (const failure of failures) {
    rows.push({ id: failure.id, ok: false, detail: failure.message });
  }
  return rows;
}

export interface ItemPublishNowPanelProps {
  itemId?: string;
  onItemIdChange?: (itemId: string) => void;
  onPublished?: () => void;
}

function publishNowErrorMessage(err: unknown): string {
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

export function ItemPublishNowPanel({
  itemId,
  onItemIdChange,
  onPublished,
}: ItemPublishNowPanelProps): React.ReactElement {
  const [inputId, setInputId] = useState(itemId ?? "");
  const [kind, setKind] = useState<PublishNowKind>("page");
  const [reviewed, setReviewed] = useState(false);
  const [multiIds, setMultiIds] = useState<string[]>([]);
  const [failedIds, setFailedIds] = useState<string[]>([]);
  const [report, setReport] = useState<PublishNowBatchRow[] | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  // Templates changed in this Design session whose items may still show the
  // old assembly until republished (#6478). Reloaded whenever the panel
  // re-reviews so marks made in another tab/shell appear at publish time.
  const [staleTemplates, setStaleTemplates] = useState<StaleAssemblyEntry[]>(
    () => listStaleAssemblyTemplates(),
  );

  function refreshStaleTemplates(): void {
    setStaleTemplates(listStaleAssemblyTemplates());
  }

  function dismissStaleTemplate(name: string): void {
    clearTemplateAssemblyStale(name);
    setStaleTemplates(listStaleAssemblyTemplates());
  }

  useEffect(() => {
    setInputId(itemId ?? "");
    setReviewed(false);
    setMultiIds([]);
    setFailedIds([]);
    setReport(null);
    setSuccess(null);
  }, [itemId]);

  function onReview(e: React.FormEvent): void {
    e.preventDefault();
    refreshStaleTemplates();
    const ids = parsePublishNowItemIds(inputId);
    if (ids.length > 1) {
      // Keep the whole list in the box: propagating one id would echo back
      // through the shared workspace itemId prop and clobber the rest.
      setMultiIds(ids);
      setFailedIds([]);
      setReport(null);
      setError(null);
      setSuccess(null);
      setReviewed(true);
      return;
    }
    setMultiIds([]);
    setFailedIds([]);
    setReport(null);
    const next = mapIdParam(inputId) || inputId.trim();
    onItemIdChange?.(mapIdParam(next) || next);
    const safe = mapIdParam(next);
    if (!safe) {
      setReviewed(false);
      setSuccess(null);
      setError(
        next.trim()
          ? message(MSG.PUBLISH_ITEM_HISTORY_INVALID_ID)
          : message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID),
      );
      return;
    }
    setError(null);
    setSuccess(null);
    setReviewed(true);
  }

  /** Merge one run into the visible per-item report; retried ids flip. */
  function mergeReport(
    previous: PublishNowBatchRow[] | null,
    published: readonly string[],
    failures: readonly { id: string; message: string }[],
  ): PublishNowBatchRow[] {
    const next = new Map<string, PublishNowBatchRow>();
    for (const row of previous ?? []) {
      next.set(row.id, row);
    }
    for (const row of rowsFromReport(
      published,
      failures,
      message(MSG.PUBLISH_NOW_BATCH_ITEM_OK),
    )) {
      next.set(row.id, row);
    }
    return [...next.values()];
  }

  async function runBatch(ids: string[]): Promise<void> {
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      const batch = await runPublishNowBatch(ids, kind);
      setReport((previous) =>
        mergeReport(previous, batch.published, batch.failures),
      );
      if (batch.failures.length === 0) {
        setFailedIds([]);
        setSuccess(
          fill(message(MSG.PUBLISH_NOW_BATCH_DONE), {
            total: String(batch.total),
          }),
        );
      } else {
        setFailedIds(batch.failures.map((failure) => failure.id));
        setError(
          fill(message(MSG.PUBLISH_NOW_BATCH_PARTIAL), {
            saved: String(batch.published.length),
            total: String(batch.total),
            detail: formatPublishNowBatchDetail(batch.failures),
          }),
        );
      }
      if (batch.published.length > 0) {
        onPublished?.();
      }
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(publishNowErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function onPublish(e: React.FormEvent): Promise<void> {
    e.preventDefault();
    const ids = parsePublishNowItemIds(inputId);
    if (ids.length > 1) {
      if (multiIds.join(",") !== ids.join(",")) {
        setMultiIds(ids);
        setFailedIds([]);
        setReport(null);
        setReviewed(true);
        setError(null);
        setSuccess(null);
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
      setReviewed(true);
      return;
    }
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      const ok = await publishSelectedItem(pathItemForPublishNow(safe, kind));
      if (!ok) {
        setFailedIds([safe]);
        setError(message(MSG.PUBLISH_ERROR));
        return;
      }
      setFailedIds([]);
      onItemIdChange?.(safe);
      setSuccess(message(MSG.PUBLISH_NOW_SUCCESS));
      onPublished?.();
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setFailedIds([safe]);
      setError(publishNowErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section
      data-testid="item-publish-now"
      aria-labelledby="item-publish-now-title"
      style={{
        marginBottom: 20,
        padding: 12,
        border: "1px solid #ddd",
        borderRadius: 6,
        background: "#fafafa",
      }}
    >
      <h2
        id="item-publish-now-title"
        style={{ margin: "0 0 8px", fontSize: "1.05rem" }}
      >
        {message(MSG.PUBLISH_NOW)}
      </h2>
      <p style={{ margin: "0 0 12px", fontSize: "0.85rem", color: "#555" }}>
        {message(MSG.PUBLISH_NOW_HINT)}{" "}
        {message(MSG.PUBLISH_NOW_IDS_HINT)}
      </p>
      {staleTemplates.length > 0 && (
        <div
          role="alert"
          data-testid="item-publish-now-stale"
          aria-label={message(MSG.PUBLISH_NOW_STALE_TITLE)}
          style={{
            margin: "0 0 12px",
            padding: "8px 12px",
            fontSize: "0.85rem",
            border: "1px solid #d69e2e",
            borderRadius: 6,
            background: "#fffff0",
          }}
        >
          <strong>{message(MSG.PUBLISH_NOW_STALE_TITLE)}</strong>
          <ul style={{ margin: "8px 0 0", paddingLeft: 18 }}>
            {staleTemplates.map((entry, index) => (
              <li
                key={entry.name}
                data-testid="item-publish-now-stale-row"
                data-state={entry.state}
              >
                <span>
                  {fill(
                    message(
                      entry.state === "unsaved"
                        ? MSG.PUBLISH_NOW_STALE_UNSAVED
                        : MSG.PUBLISH_NOW_STALE_SAVED,
                    ),
                    { 0: entry.name },
                  )}
                </span>{" "}
                <button
                  type="button"
                  data-testid={`item-publish-now-stale-dismiss-${index}`}
                  aria-label={fill(
                    message(MSG.PUBLISH_NOW_STALE_DISMISS_ARIA),
                    { 0: entry.name },
                  )}
                  onClick={() => dismissStaleTemplate(entry.name)}
                  style={{
                    background: "transparent",
                    border: "1px solid #ccc",
                    borderRadius: 4,
                    padding: "2px 8px",
                    cursor: "pointer",
                    fontSize: "0.8rem",
                  }}
                >
                  {message(MSG.PUBLISH_NOW_STALE_DISMISS)}
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
      <form onSubmit={onReview} style={toolbarStyle}>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 280 }}>
          <label htmlFor="item-publish-now-id">
            {message(MSG.PUBLISH_ITEM_HISTORY_ITEM_ID)}
          </label>
          <input
            id="item-publish-now-id"
            name="itemId"
            value={inputId}
            onChange={(e) => {
              setInputId(e.target.value);
              setReviewed(false);
              setMultiIds([]);
              setFailedIds([]);
              setReport(null);
              setSuccess(null);
            }}
            data-testid="item-publish-now-id"
            autoComplete="off"
          />
        </div>
        <fieldset
          style={{ border: "none", margin: 0, padding: 0 }}
          data-testid="item-publish-now-kind"
        >
          <legend style={{ fontSize: 13 }}>
            {message(MSG.PUBLISH_TAKEDOWN_KIND)}
          </legend>
          <label style={{ marginRight: 12, fontSize: 13 }}>
            <input
              type="radio"
              name="item-publish-now-kind"
              value="page"
              checked={kind === "page"}
              onChange={(e) => setKind(mapPublishNowKind(e.target.value))}
              data-testid="item-publish-now-kind-page"
            />{" "}
            {message(MSG.PUBLISH_TAKEDOWN_KIND_PAGE)}
          </label>
          <label style={{ fontSize: 13 }}>
            <input
              type="radio"
              name="item-publish-now-kind"
              value="resource"
              checked={kind === "resource"}
              onChange={(e) => setKind(mapPublishNowKind(e.target.value))}
              data-testid="item-publish-now-kind-resource"
            />{" "}
            {message(MSG.PUBLISH_TAKEDOWN_KIND_RESOURCE)}
          </label>
        </fieldset>
        <button
          type="submit"
          style={primaryButtonStyle}
          data-testid="item-publish-now-review"
          disabled={submitting}
        >
          {message(MSG.PUBLISH_NOW_REVIEW)}
        </button>
      </form>
      {reviewed && multiIds.length > 1 && (
        <div data-testid="item-publish-now-confirm" style={{ marginTop: 12 }}>
          <p style={{ margin: "0 0 8px", fontSize: 13 }}>
            {fill(message(MSG.PUBLISH_NOW_BATCH_CONFIRM), {
              total: String(multiIds.length),
            })}
          </p>
          <ul style={{ margin: "0 0 8px", paddingLeft: 18, fontSize: 13 }}>
            {multiIds.map((id) => (
              <li key={id} data-testid={`item-publish-now-batch-item-${id}`}>
                {id}
              </li>
            ))}
          </ul>
          <form onSubmit={(e) => void onPublish(e)}>
            <button
              type="submit"
              style={primaryButtonStyle}
              data-testid="item-publish-now-submit"
              disabled={submitting}
            >
              {message(MSG.PUBLISH_NOW_SUBMIT)}
            </button>
          </form>
        </div>
      )}
      {reviewed && multiIds.length <= 1 && (
        <div data-testid="item-publish-now-confirm" style={{ marginTop: 12 }}>
          <p style={{ margin: "0 0 8px", fontSize: 13 }}>
            {message(MSG.PUBLISH_NOW_CONFIRM)}
          </p>
          <form onSubmit={(e) => void onPublish(e)}>
            <button
              type="submit"
              style={primaryButtonStyle}
              data-testid="item-publish-now-submit"
              disabled={submitting}
            >
              {message(MSG.PUBLISH_NOW_SUBMIT)}
            </button>
          </form>
        </div>
      )}
      {failedIds.length > 0 && !submitting && (
        <div style={{ marginTop: 12 }}>
          <button
            type="button"
            style={primaryButtonStyle}
            data-testid="item-publish-now-retry"
            onClick={() => void runBatch(failedIds)}
          >
            {message(MSG.PUBLISH_NOW_RETRY_FAILED)}
          </button>
        </div>
      )}
      {report && report.length > 0 && (
        <ul
          data-testid="item-publish-now-batch-report"
          aria-label={message(MSG.PUBLISH_NOW)}
          style={{ margin: "12px 0 0", paddingLeft: 18, fontSize: 13 }}
        >
          {report.map((row) => (
            <li
              key={row.id}
              data-testid={`item-publish-now-batch-report-item-${row.id}`}
            >
              <span>{row.id}</span>
              {": "}
              <span>{row.detail}</span>
            </li>
          ))}
        </ul>
      )}
      {submitting && (
        <p data-testid="item-publish-now-loading">{message(MSG.PUBLISH_LOADING)}</p>
      )}
      {error && (
        <p style={errorStyle} role="alert" data-testid="item-publish-now-error">
          {error}
        </p>
      )}
      {success && (
        <p
          data-testid="item-publish-now-success"
          style={{ color: "#166534", fontSize: 13 }}
        >
          {success}
        </p>
      )}
    </section>
  );
}
