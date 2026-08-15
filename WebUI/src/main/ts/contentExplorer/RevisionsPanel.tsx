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
 * revisions (revision rows + transition comments) and optionally restores
 * a prior revision.
 */

import React, { useCallback, useEffect, useState } from "react";
import { formatApiError } from "../api/client";
import {
  fetchItemRevisions,
  restoreItemRevision,
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

export function RevisionsPanel(props: RevisionsPanelProps): React.JSX.Element {
  const {
    itemId,
    itemLabel,
    initialTab = "revisions",
    loadSummary = defaultLoad,
    restoreRevision = defaultRestore,
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
    loadSummary(itemId)
      .then((data) => {
        if (!alive) return;
        setState({ kind: "ok", data });
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
