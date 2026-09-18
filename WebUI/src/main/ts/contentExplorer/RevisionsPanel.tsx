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

import React, { useCallback, useEffect, useState } from "react";
import { formatApiError, isApiError } from "../api/client";
import {
  fetchItemRevisions,
  fetchItemRevisionCompare,
  restoreItemRevision,
  type ItemRevisionCompare,
  type ItemRevisionsSummary,
} from "../api/contentExplorer/itemRevisionsApi";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";

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
  | { kind: "ok"; data: ItemRevisionsSummary }
  | { kind: "error"; message: string };

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
  const [reloadToken, setReloadToken] = useState(0);
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
    if (!itemId) {
      setState({
        kind: "error",
        message: message(EXPLORER_MSG.ACTION_NEEDS_ITEM),
      });
      return;
    }
    setState({ kind: "loading" });
    setRestoreError(null);
    setCompareError(null);
    setCompareResult(null);
    setLeftRev(null);
    setRightRev(null);
    loadSummary(itemId)
      .then((data) => {
        if (!alive) return;
        setState({ kind: "ok", data });
        const ids = data.revisions.map((r) => r.revId).sort((a, b) => a - b);
        if (ids.length >= 2) {
          setLeftRev(ids[0] ?? null);
          setRightRev(ids[ids.length - 1] ?? null);
        } else if (ids.length === 1) {
          setLeftRev(ids[0] ?? null);
          setRightRev(ids[0] ?? null);
        }
      })
      .catch((err: unknown) => {
        if (!alive) return;
        setState({
          kind: "error",
          message: formatApiError(err, message(EXPLORER_MSG.REVISIONS_ERROR)),
        });
      });
    return () => {
      alive = false;
    };
  }, [itemId, loadSummary, reloadToken]);

  const handleRestore = useCallback(
    async (revId: number) => {
      const ok = (confirm ?? ((b) => window.confirm(b)))(
        message(EXPLORER_MSG.CONFIRM_RESTORE_REVISION),
      );
      if (!ok) {
        return;
      }
      setRestoringRev(revId);
      setRestoreError(null);
      try {
        await restoreRevision(itemId, revId);
        setReloadToken((n) => n + 1);
        onRestored?.(revId);
      } catch (err: unknown) {
        setRestoreError(
          formatApiError(err, message(EXPLORER_MSG.REVISIONS_RESTORE_ERROR)),
        );
      } finally {
        setRestoringRev(null);
      }
    },
    [confirm, itemId, onRestored, restoreRevision],
  );

  const handleCompare = useCallback(async () => {
    if (leftRev == null || rightRev == null || leftRev === rightRev) {
      setCompareError(message(EXPLORER_MSG.REVISIONS_COMPARE_NEED_TWO));
      return;
    }
    setCompareBusy(true);
    setCompareError(null);
    try {
      const result = await compareRevisions(itemId, leftRev, rightRev);
      setCompareResult(result);
    } catch (err: unknown) {
      setCompareResult(null);
      setCompareError(compareErrorMessage(err));
    } finally {
      setCompareBusy(false);
    }
  }, [compareRevisions, itemId, leftRev, rightRev]);

  const regionLabel = ariaLabel ?? message(EXPLORER_MSG.REVISIONS_TITLE);
  const panelStyle: React.CSSProperties = {
    border: "1px solid #ccc",
    padding: 12,
    background: "#fff",
  };

  if (state.kind === "loading") {
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
        <p role="alert">{state.message}</p>
      </section>
    );
  }

  const { data } = state;
  const headRev =
    data.revisions.length > 0
      ? Math.max(...data.revisions.map((r) => r.revId))
      : 0;

  return (
    <section
      role="region"
      aria-label={regionLabel}
      data-testid="revisions-panel"
      data-testid-state="ok"
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
                const canRestore =
                  data.restorable && rev.revId !== headRev;
                return (
                  <tr
                    key={rev.revId}
                    data-testid={`revisions-row-${rev.revId}`}
                  >
                    <td>{rev.revId}</td>
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
      ) : data.comments.length === 0 ? (
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
            {data.comments.map((c, i) => (
              <tr key={`${c.commentDate}-${i}`} data-testid={`audit-row-${i}`}>
                <td>{c.commentDate}</td>
                <td>{c.commenter}</td>
                <td>{c.commentType}</td>
                <td>{c.comment}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

export default RevisionsPanel;
