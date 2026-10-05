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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Explorer Revisions / Audit Trail panel. Loads GET itemmanagement
 * revisions (revision rows + transition comments), optionally restores a
 * prior revision, and compares two revision field payloads.
 */

import React, { useCallback, useEffect, useRef, useState } from "react";
import { formatApiError, isApiError } from "../api/client";
import {
  currentRevisionId,
  fetchItemRevisions,
  fetchItemRevisionCompare,
  isRecordedAuditComment,
  restoreItemRevision,
  type ItemRevisionCompare,
  type ItemRevisionsSummary,
} from "../api/contentExplorer/itemRevisionsApi";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";
import { RevisionRestoreConfirmDialog } from "./RevisionRestoreConfirmDialog";

export type RevisionsPanelTab = "revisions" | "audit";

export interface RevisionsPanelProps {
  itemId: string;
  itemLabel?: string;
  initialTab?: RevisionsPanelTab;
  loadSummary?: (itemId: string) => Promise<ItemRevisionsSummary>;
  restoreRevision?: (itemId: string, revId: number) => Promise<void>;
  compareRevisions?: (
    itemId: string,
    rev1: number,
    rev2: number,
  ) => Promise<ItemRevisionCompare>;
  onRestored?: (revId: number) => void;
  confirm?: (body: string) => boolean;
  ariaLabel?: string;
  className?: string;
}

type PanelState =
  | { kind: "loading" }
  | { kind: "ok"; data: ItemRevisionsSummary; forItem: string }
  | { kind: "error"; message: string };

function restoreErrorMessage(err: unknown): string {
  if (isApiError(err) && err.status === 403) {
    return message(EXPLORER_MSG.REVISIONS_RESTORE_FORBIDDEN);
  }
  if (isApiError(err) && err.status === 409) {
    return message(EXPLORER_MSG.REVISIONS_RESTORE_CONFLICT);
  }
  return formatApiError(err, message(EXPLORER_MSG.REVISIONS_RESTORE_ERROR));
}

async function defaultLoad(itemId: string): Promise<ItemRevisionsSummary> {
  return fetchItemRevisions(itemId);
}

async function defaultRestore(itemId: string, revId: number): Promise<void> {
  await restoreItemRevision(itemId, revId);
}

async function defaultCompare(
  itemId: string,
  rev1: number,
  rev2: number,
): Promise<ItemRevisionCompare> {
  return fetchItemRevisionCompare(itemId, rev1, rev2);
}

function compareErrorMessage(err: unknown): string {
  if (isApiError(err) && err.status === 404) {
    return message(EXPLORER_MSG.REVISIONS_COMPARE_NOT_FOUND);
  }
  if (isApiError(err) && err.status === 403) {
    return message(EXPLORER_MSG.REVISIONS_COMPARE_FORBIDDEN);
  }
  return formatApiError(err, message(EXPLORER_MSG.REVISIONS_COMPARE_ERROR));
}

function historyLoadErrorMessage(err: unknown): string {
  if (isApiError(err) && err.status === 404) {
    return message(EXPLORER_MSG.REVISIONS_LOAD_NOT_FOUND);
  }
  if (isApiError(err) && err.status === 403) {
    return message(EXPLORER_MSG.REVISIONS_LOAD_FORBIDDEN);
  }
  return formatApiError(err, message(EXPLORER_MSG.REVISIONS_ERROR));
}

export function RevisionsPanel(props: RevisionsPanelProps): React.JSX.Element {
  const {
    itemId,
    itemLabel,
    initialTab = "revisions",
    loadSummary = defaultLoad,
    restoreRevision = defaultRestore,
    compareRevisions = defaultCompare,
    onRestored,
    confirm,
    ariaLabel,
    className,
  } = props;

  const [tab, setTab] = useState<RevisionsPanelTab>(initialTab);
  const [state, setState] = useState<PanelState>({ kind: "loading" });
  const [restoreError, setRestoreError] = useState<string | null>(null);
  const [restoringRev, setRestoringRev] = useState<number | null>(null);
  const [pendingRev, setPendingRev] = useState<number | null>(null);
  // Token stays above 0 after a successful restore. The ref is the item that
  // reload belongs to. Drop it when itemId changes: a later visit must load
  // fresh, or Compare keeps the other item's ids and a failed load stays
  // on Loading.
  const [reloadToken, setReloadToken] = useState(0);
  const reloadItemIdRef = useRef<string | null>(null);
  const itemIdRef = useRef(itemId);
  itemIdRef.current = itemId;
  const [leftRev, setLeftRev] = useState<number | null>(null);
  const [rightRev, setRightRev] = useState<number | null>(null);
  const [compareError, setCompareError] = useState<string | null>(null);
  const [compareBusy, setCompareBusy] = useState(false);
  const [compareResult, setCompareResult] = useState<ItemRevisionCompare | null>(
    null,
  );

  useEffect(() => {
    setTab(initialTab);
  }, [initialTab, itemId]);

  useEffect(() => {
    let alive = true;
    const sameItemReload =
      reloadItemIdRef.current === itemId && reloadToken > 0;
    if (reloadItemIdRef.current !== itemId) {
      reloadItemIdRef.current = null;
    }
    const resetSession = () => {
      setRestoreError(null);
      setPendingRev(null);
      setRestoringRev(null);
      setCompareError(null);
      setCompareResult(null);
      setCompareBusy(false);
      setLeftRev(null);
      setRightRev(null);
    };
    if (!itemId) {
      setState({
        kind: "error",
        message: message(EXPLORER_MSG.ACTION_NEEDS_ITEM),
      });
      resetSession();
      return;
    }
    setState((prev) => {
      if (prev.kind === "ok" && prev.forItem === itemId && sameItemReload) {
        return prev;
      }
      return { kind: "loading" };
    });
    if (!sameItemReload) {
      resetSession();
    }
    loadSummary(itemId)
      .then((data) => {
        if (!alive) return;
        setState({ kind: "ok", data, forItem: itemId });
        if (sameItemReload) {
          setRestoreError(null);
        }
        const ids = data.revisions.map((r) => r.revId).sort((a, b) => a - b);
        if (!sameItemReload) {
          if (ids.length >= 2) {
            setLeftRev(ids[0] ?? null);
            setRightRev(ids[ids.length - 1] ?? null);
          } else if (ids.length === 1) {
            setLeftRev(ids[0] ?? null);
            setRightRev(ids[0] ?? null);
          }
        }
      })
      .catch((err: unknown) => {
        if (!alive) return;
        const messageText = historyLoadErrorMessage(err);
        if (sameItemReload) {
          setRestoreError(messageText);
          return;
        }
        setState({
          kind: "error",
          message: messageText,
        });
      });
    return () => {
      alive = false;
    };
  }, [itemId, loadSummary, reloadToken]);

  const runRestore = useCallback(
    async (revId: number) => {
      const forItem = itemId;
      setPendingRev(null);
      setRestoringRev(revId);
      setRestoreError(null);
      try {
        await restoreRevision(forItem, revId);
        onRestored?.(revId);
        if (itemIdRef.current !== forItem) {
          return;
        }
        reloadItemIdRef.current = forItem;
        setReloadToken((n) => n + 1);
      } catch (err: unknown) {
        if (itemIdRef.current !== forItem) {
          return;
        }
        setRestoreError(restoreErrorMessage(err));
      } finally {
        if (itemIdRef.current === forItem) {
          setRestoringRev(null);
        }
      }
    },
    [itemId, onRestored, restoreRevision],
  );

  const handleRestore = useCallback(
    (revId: number) => {
      if (confirm) {
        if (!confirm(message(EXPLORER_MSG.CONFIRM_RESTORE_REVISION))) {
          return;
        }
        void runRestore(revId);
        return;
      }
      setPendingRev(revId);
    },
    [confirm, runRestore],
  );

  const handleCompare = useCallback(async () => {
    const forItem = itemId;
    if (leftRev == null || rightRev == null || leftRev === rightRev) {
      setCompareError(message(EXPLORER_MSG.REVISIONS_COMPARE_NEED_TWO));
      return;
    }
    setCompareBusy(true);
    setCompareError(null);
    try {
      const result = await compareRevisions(forItem, leftRev, rightRev);
      if (itemIdRef.current !== forItem) {
        return;
      }
      setCompareResult(result);
    } catch (err: unknown) {
      if (itemIdRef.current !== forItem) {
        return;
      }
      setCompareResult(null);
      setCompareError(compareErrorMessage(err));
    } finally {
      if (itemIdRef.current === forItem) {
        setCompareBusy(false);
      }
    }
  }, [compareRevisions, itemId, leftRev, rightRev]);

  const regionLabel = ariaLabel ?? message(EXPLORER_MSG.REVISIONS_TITLE);
  const panelStyle: React.CSSProperties = {
    border: "1px solid #ccc",
    padding: 12,
    background: "#fff",
  };

  if (
    state.kind === "loading" ||
    (state.kind === "ok" && state.forItem !== itemId)
  ) {
    return (
      <section
        role="region"
        aria-label={regionLabel}
        data-testid="revisions-panel"
        data-testid-state="loading"
        className={className}
        style={panelStyle}
      >
        <p aria-live="polite">{message(EXPLORER_MSG.REVISIONS_LOADING)}</p>
      </section>
    );
  }

  if (state.kind === "error") {
    return (
      <section
        role="region"
        aria-label={regionLabel}
        data-testid="revisions-panel"
        data-testid-state="error"
        className={className}
        style={panelStyle}
      >
        <p role="alert" data-testid="revisions-load-error">
          {state.message}
        </p>
      </section>
    );
  }

  const { data } = state;
  const headRev = currentRevisionId(data);
  const auditComments = data.comments.filter(isRecordedAuditComment);

  return (
    <section
      role="region"
      aria-label={regionLabel}
      data-testid="revisions-panel"
      data-testid-state="ok"
      data-current-rev={String(headRev)}
      data-testid-tab={tab}
      className={className}
      style={panelStyle}
    >
      <header style={{ marginBottom: 8 }}>
        <h2
          style={{ margin: 0, fontSize: "1rem" }}
          data-testid="revisions-panel-title"
        >
          {message(EXPLORER_MSG.REVISIONS_TITLE)}
          {itemLabel ? (
            <span style={{ fontWeight: 400, marginLeft: 8, color: "#555" }}>
              — {itemLabel}
            </span>
          ) : null}
        </h2>
      </header>
      <div
        role="tablist"
        aria-label={message(EXPLORER_MSG.REVISIONS_TABS)}
        style={{ display: "flex", gap: 8, marginBottom: 8 }}
      >
        <button
          type="button"
          role="tab"
          aria-selected={tab === "revisions"}
          data-testid="revisions-tab-revisions"
          onClick={() => setTab("revisions")}
        >
          {message(EXPLORER_MSG.REVISIONS_TAB_REVISIONS)}
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === "audit"}
          data-testid="revisions-tab-audit"
          onClick={() => setTab("audit")}
        >
          {message(EXPLORER_MSG.REVISIONS_TAB_AUDIT)}
        </button>
      </div>
      {restoreError ? (
        <p role="alert" data-testid="revisions-restore-error">
          {restoreError}
        </p>
      ) : null}
      {tab === "revisions" ? (
        data.revisions.length === 0 ? (
          <p data-testid="revisions-empty">
            {message(EXPLORER_MSG.REVISIONS_EMPTY)}
          </p>
        ) : (
          <>
          <table data-testid="revisions-table" style={{ width: "100%" }}>
            <thead>
              <tr>
                <th>{message(EXPLORER_MSG.REVISIONS_COL_REV)}</th>
                <th>{message(EXPLORER_MSG.REVISIONS_COL_DATE)}</th>
                <th>{message(EXPLORER_MSG.REVISIONS_COL_USER)}</th>
                <th>{message(EXPLORER_MSG.REVISIONS_COL_STATUS)}</th>
                <th>{message(EXPLORER_MSG.REVISIONS_COL_ACTIONS)}</th>
              </tr>
            </thead>
            <tbody>
              {data.revisions.map((rev) => {
                const isCurrent = headRev > 0 && rev.revId === headRev;
                const canRestore = data.restorable && !isCurrent;
                return (
                  <tr
                    key={rev.revId}
                    data-testid={`revisions-row-${rev.revId}`}
                    data-current={isCurrent ? "true" : "false"}
                  >
                    <td>
                      {rev.revId}
                      {isCurrent ? (
                        <span
                          data-testid="revisions-current"
                          data-current-rev={String(rev.revId)}
                        >
                          {" "}
                          {message(EXPLORER_MSG.REVISIONS_CURRENT)}
                        </span>
                      ) : null}
                    </td>
                    <td>{rev.lastModifiedDate}</td>
                    <td>{rev.lastModifier}</td>
                    <td>{rev.status}</td>
                    <td>
                      {canRestore ? (
                        <button
                          type="button"
                          data-testid={`revisions-restore-${rev.revId}`}
                          disabled={restoringRev != null}
                          onClick={() => {
                            void handleRestore(rev.revId);
                          }}
                        >
                          {message(EXPLORER_MSG.REVISIONS_RESTORE)}
                        </button>
                      ) : (
                        "—"
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          <div
            data-testid="revisions-compare"
            style={{ marginTop: 12 }}
          >
            <div
              style={{
                display: "flex",
                flexWrap: "wrap",
                gap: 8,
                alignItems: "center",
              }}
            >
              <label htmlFor="revisions-compare-left">
                {message(EXPLORER_MSG.REVISIONS_COMPARE_FROM)}
              </label>
              <select
                id="revisions-compare-left"
                data-testid="revisions-compare-left"
                value={leftRev ?? ""}
                onChange={(ev) => {
                  const n = Number(ev.target.value);
                  setLeftRev(Number.isFinite(n) ? n : null);
                }}
              >
                {data.revisions.map((rev) => (
                  <option key={`l-${rev.revId}`} value={rev.revId}>
                    {rev.revId}
                  </option>
                ))}
              </select>
              <label htmlFor="revisions-compare-right">
                {message(EXPLORER_MSG.REVISIONS_COMPARE_TO)}
              </label>
              <select
                id="revisions-compare-right"
                data-testid="revisions-compare-right"
                value={rightRev ?? ""}
                onChange={(ev) => {
                  const n = Number(ev.target.value);
                  setRightRev(Number.isFinite(n) ? n : null);
                }}
              >
                {data.revisions.map((rev) => (
                  <option key={`r-${rev.revId}`} value={rev.revId}>
                    {rev.revId}
                  </option>
                ))}
              </select>
              <button
                type="button"
                data-testid="revisions-compare-run"
                disabled={
                  compareBusy ||
                  leftRev == null ||
                  rightRev == null ||
                  leftRev === rightRev
                }
                onClick={() => {
                  void handleCompare();
                }}
              >
                {message(EXPLORER_MSG.REVISIONS_COMPARE)}
              </button>
            </div>
            {compareBusy ? (
              <p aria-live="polite" data-testid="revisions-compare-loading">
                {message(EXPLORER_MSG.REVISIONS_COMPARE_LOADING)}
              </p>
            ) : null}
            {compareError ? (
              <p role="alert" data-testid="revisions-compare-error">
                {compareError}
              </p>
            ) : null}
            {compareResult && compareResult.fields.length === 0 ? (
              <p data-testid="revisions-compare-empty">
                {message(EXPLORER_MSG.REVISIONS_COMPARE_EMPTY)}
              </p>
            ) : null}
            {compareResult && compareResult.fields.length > 0 ? (
              <table
                data-testid="revisions-compare-table"
                style={{ width: "100%", marginTop: 8 }}
              >
                <thead>
                  <tr>
                    <th>{message(EXPLORER_MSG.REVISIONS_COMPARE_COL_FIELD)}</th>
                    <th>{message(EXPLORER_MSG.REVISIONS_COMPARE_COL_LEFT)}</th>
                    <th>{message(EXPLORER_MSG.REVISIONS_COMPARE_COL_RIGHT)}</th>
                    <th>{message(EXPLORER_MSG.REVISIONS_COL_STATUS)}</th>
                  </tr>
                </thead>
                <tbody>
                  {compareResult.fields.map((field) => (
                    <tr
                      key={field.name}
                      data-testid={`revisions-compare-row-${field.name}`}
                      data-testid-changed={field.changed ? "true" : "false"}
                    >
                      <td>{field.name}</td>
                      <td>{field.leftValue}</td>
                      <td>{field.rightValue}</td>
                      <td>
                        {field.changed
                          ? message(EXPLORER_MSG.REVISIONS_COMPARE_CHANGED)
                          : message(EXPLORER_MSG.REVISIONS_COMPARE_SAME)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : null}
          </div>
          </>
        )
      ) : auditComments.length === 0 ? (
        <p data-testid="revisions-audit-empty">
          {message(EXPLORER_MSG.REVISIONS_AUDIT_EMPTY)}
        </p>
      ) : (
        <table data-testid="revisions-audit-table" style={{ width: "100%" }}>
          <thead>
            <tr>
              <th>{message(EXPLORER_MSG.REVISIONS_COL_DATE)}</th>
              <th>{message(EXPLORER_MSG.REVISIONS_COL_USER)}</th>
              <th>{message(EXPLORER_MSG.REVISIONS_COL_TYPE)}</th>
              <th>{message(EXPLORER_MSG.REVISIONS_COL_COMMENT)}</th>
            </tr>
          </thead>
          <tbody>
            {auditComments.map((c, i) => (
              <tr key={`${c.commentDate}-${i}`} data-testid={`audit-row-${i}`}>
                <td data-testid={`audit-date-${i}`}>{c.commentDate}</td>
                <td data-testid={`audit-user-${i}`}>{c.commenter}</td>
                <td data-testid={`audit-type-${i}`}>{c.commentType}</td>
                <td data-testid={`audit-comment-${i}`}>{c.comment}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {pendingRev != null ? (
        <RevisionRestoreConfirmDialog
          revId={pendingRev}
          onConfirm={() => {
            void runRestore(pendingRev);
          }}
          onCancel={() => setPendingRev(null)}
        />
      ) : null}
    </section>
  );
}

export default RevisionsPanel;
