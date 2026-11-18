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

import React, { useEffect, useMemo, useRef, useState } from "react";
import { useDialogEscape } from "../architecture/useDialogEscape";
import type { PSPathItem } from "../api/contentExplorer/types";
import { message } from "../i18n/message";
import {
  buildBulkStageRows,
  partitionStageSelection,
  removeFromStagingSelectedItems,
  stageSelectedItems,
  type BulkStageReportInput,
  type BulkStageRow,
} from "./itemPublish";
import { EXPLORER_MSG } from "./messages";

const FOCUSABLE =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

export type BulkStageMode = "stage" | "unstage";

export interface BulkStageOutcome {
  /** True when the batch ran (confirm step accepted). */
  ran: boolean;
  /** True when at least one item staged/unstaged so the list refreshes. */
  refresh: boolean;
}

export interface BulkStageDialogProps {
  mode: BulkStageMode;
  items: readonly PSPathItem[];
  /**
   * Runs the eligible rows. Defaults to the production batch calls so the
   * dialog owns confirm → run → report; tests inject a stub.
   */
  runBatch?: (eligible: PSPathItem[]) => Promise<BulkStageReportInput>;
  onDone: (outcome: BulkStageOutcome) => void;
}

type Step = "confirm" | "running" | "report";

function outcomeText(row: BulkStageRow, mode: BulkStageMode): string {
  if (row.outcome === "ok") {
    return message(
      mode === "stage"
        ? EXPLORER_MSG.BULK_STAGE_ROW_OK
        : EXPLORER_MSG.BULK_UNSTAGE_ROW_OK,
    );
  }
  if (row.outcome === "failed") {
    const base = message(EXPLORER_MSG.BULK_STAGE_ROW_FAILED);
    return row.detail ? `${base} (${row.detail})` : base;
  }
  if (row.outcome === "skipped-folder") {
    return message(EXPLORER_MSG.BULK_STAGE_ROW_SKIPPED_FOLDER);
  }
  return message(EXPLORER_MSG.BULK_STAGE_ROW_SKIPPED_OTHER);
}

/**
 * Bulk Stage / Remove-from-Staging confirm with per-item reporting (#6469).
 * One confirm lists every eligible page/asset, then the report names each
 * row outcome (staged/removed, failed with HTTP reason, skipped folders)
 * with summary counts — replacing the transient banner for this flow.
 */
export function BulkStageDialog({
  mode,
  items,
  runBatch,
  onDone,
}: BulkStageDialogProps): React.ReactElement {
  const plan = useMemo(() => partitionStageSelection(items), [items]);
  const [step, setStep] = useState<Step>("confirm");
  const [rows, setRows] = useState<BulkStageRow[]>([]);
  const rootRef = useRef<HTMLDivElement>(null);
  const okRef = useRef<HTMLButtonElement>(null);
  const busy = step === "running";

  const titleId = "explorer-bulk-stage-title";
  const listId = "explorer-bulk-stage-list";

  useDialogEscape(true, busy, () => {
    if (!busy) {
      onDone({ ran: false, refresh: false });
    }
  });

  useEffect(() => {
    const root = rootRef.current;
    okRef.current?.focus();
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
  }, [step]);

  const eligible = plan.eligible;
  const noneEligible = eligible.length === 0;

  async function confirmRun(): Promise<void> {
    if (busy || noneEligible) {
      return;
    }
    setStep("running");
    try {
      let okIds: string[];
      let failures: BulkStageReportInput["failures"];
      if (runBatch != null) {
        const stubbed = await runBatch(eligible);
        okIds = stubbed.okIds;
        failures = stubbed.failures;
      } else if (mode === "stage") {
        const batch = await stageSelectedItems(eligible);
        okIds = batch.stagedIds;
        failures = batch.failures;
      } else {
        const batch = await removeFromStagingSelectedItems(eligible);
        okIds = batch.removedIds;
        failures = batch.failures;
      }
      // The batch only saw the eligible rows; the plan's skips belong in
      // the same report so no selected row goes unmentioned.
      setRows(
        buildBulkStageRows(items, {
          okIds,
          skippedFolders: plan.skippedFolders,
          skippedOther: plan.skippedOther,
          failures,
        }),
      );
    } catch {
      setRows(
        buildBulkStageRows(items, {
          okIds: [],
          skippedFolders: plan.skippedFolders,
          skippedOther: plan.skippedOther,
          failures: eligible.map((item) => ({
            id: (item.id ?? "").trim(),
            name: (item.name ?? item.path ?? item.id ?? "item").trim() || "item",
            message: "request failed",
          })),
        }),
      );
    }
    setStep("report");
  }

  const okCount = rows.filter((row) => row.outcome === "ok").length;
  const failedCount = rows.filter((row) => row.outcome === "failed").length;
  const skippedCount = rows.length - okCount - failedCount;
  const summary = message(
    mode === "stage"
      ? EXPLORER_MSG.BULK_STAGE_SUMMARY
      : EXPLORER_MSG.BULK_UNSTAGE_SUMMARY,
  )
    .split("{ok}")
    .join(String(okCount))
    .split("{skipped}")
    .join(String(skippedCount))
    .split("{failed}")
    .join(String(failedCount));

  const title = message(
    mode === "stage"
      ? EXPLORER_MSG.BULK_STAGE_TITLE
      : EXPLORER_MSG.BULK_UNSTAGE_TITLE,
  );
  const confirmBody = message(
    mode === "stage"
      ? EXPLORER_MSG.CONFIRM_STAGE_MULTI
      : EXPLORER_MSG.CONFIRM_REMOVE_FROM_STAGING_MULTI,
  )
    .split("{count}")
    .join(String(eligible.length));
  const emptyBody = message(
    mode === "stage"
      ? EXPLORER_MSG.STAGE_NOTHING_ELIGIBLE
      : EXPLORER_MSG.UNSTAGE_NOTHING_ELIGIBLE,
  );

  return (
    <div
      ref={rootRef}
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      data-testid="explorer-bulk-stage-dialog"
      data-mode={mode}
      data-step={step}
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
          minWidth: 320,
          maxWidth: 520,
          width: "min(520px, calc(100vw - 48px))",
          maxHeight: "calc(100vh - 96px)",
          display: "flex",
          flexDirection: "column",
          padding: 16,
          borderRadius: 8,
          boxShadow: "0 8px 24px rgba(0,0,0,0.18)",
        }}
        onSubmit={(ev) => {
          ev.preventDefault();
          if (step === "confirm" && !noneEligible) {
            void confirmRun();
          } else if (step === "report") {
            onDone({ ran: true, refresh: okCount > 0 });
          }
        }}
      >
        <h2 id={titleId} style={{ fontSize: 16, margin: "0 0 12px" }}>
          {title}
        </h2>
        {step === "report" ? (
          <>
            <p
              role="status"
              data-testid="explorer-bulk-stage-summary"
              style={{ fontSize: 13, margin: "0 0 8px" }}
            >
              {summary}
            </p>
            {rows.length > 0 ? (
              <ul
                aria-label={message(
                  mode === "stage"
                    ? EXPLORER_MSG.BULK_STAGE_REPORT_TITLE
                    : EXPLORER_MSG.BULK_UNSTAGE_REPORT_TITLE,
                )}
                data-testid="explorer-bulk-stage-report"
                style={{
                  fontSize: 13,
                  margin: "0 0 8px",
                  paddingLeft: 18,
                  overflowY: "auto",
                  maxHeight: 240,
                }}
              >
                {rows.map((row, index) => (
                  <li
                    key={`${row.name}-${index}`}
                    data-testid="explorer-bulk-stage-report-row"
                    data-outcome={row.outcome}
                  >
                    <span>{row.name}</span>
                    {" — "}
                    <span>{outcomeText(row, mode)}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p
                data-testid="explorer-bulk-stage-report-empty"
                style={{ fontSize: 13, margin: "0 0 8px" }}
              >
                {message(EXPLORER_MSG.BULK_STAGE_REPORT_EMPTY)}
              </p>
            )}
            <div
              style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}
            >
              <button
                type="submit"
                ref={okRef}
                data-testid="explorer-bulk-stage-close"
                autoFocus
              >
                {message(EXPLORER_MSG.BULK_STAGE_CLOSE)}
              </button>
            </div>
          </>
        ) : (
          <>
            <p style={{ fontSize: 13, margin: "0 0 8px" }}>
              {noneEligible ? emptyBody : confirmBody}
            </p>
            {!noneEligible ? (
              <>
                <p
                  id={listId}
                  style={{ fontSize: 13, margin: "0 0 4px" }}
                >
                  {message(
                    mode === "stage"
                      ? EXPLORER_MSG.BULK_STAGE_ITEMS_LABEL
                      : EXPLORER_MSG.BULK_UNSTAGE_ITEMS_LABEL,
                  )}
                </p>
                <ul
                  aria-labelledby={listId}
                  data-testid="explorer-bulk-stage-items"
                  style={{
                    fontSize: 13,
                    margin: "0 0 8px",
                    paddingLeft: 18,
                    overflowY: "auto",
                    maxHeight: 200,
                  }}
                >
                  {eligible.map((item) => (
                    <li
                      key={(item.id ?? "").trim() || item.path}
                      data-testid="explorer-bulk-stage-item"
                    >
                      {(item.name ?? item.path ?? item.id ?? "item").trim() ||
                        "item"}
                    </li>
                  ))}
                </ul>
              </>
            ) : null}
            {plan.skippedFolders.length > 0 ? (
              <p
                data-testid="explorer-bulk-stage-skipped"
                style={{ fontSize: 12, margin: "0 0 4px", color: "#475569" }}
              >
                {message(
                  mode === "stage"
                    ? EXPLORER_MSG.STAGE_SKIPPED_FOLDERS
                    : EXPLORER_MSG.UNSTAGE_SKIPPED_FOLDERS,
                ).replace("{names}", plan.skippedFolders.join(", "))}
              </p>
            ) : null}
            {plan.skippedOther.length > 0 ? (
              <p
                data-testid="explorer-bulk-stage-skipped-other"
                style={{ fontSize: 12, margin: "0 0 4px", color: "#475569" }}
              >
                {message(
                  mode === "stage"
                    ? EXPLORER_MSG.STAGE_SKIPPED_OTHER
                    : EXPLORER_MSG.UNSTAGE_SKIPPED_OTHER,
                ).replace("{names}", plan.skippedOther.join(", "))}
              </p>
            ) : null}
            {busy ? (
              <p
                role="status"
                data-testid="explorer-bulk-stage-running"
                style={{ fontSize: 13, margin: "8px 0 0" }}
              >
                {message(
                  mode === "stage"
                    ? EXPLORER_MSG.BULK_STAGE_RUNNING
                    : EXPLORER_MSG.BULK_UNSTAGE_RUNNING,
                ).replace("{count}", String(eligible.length))}
              </p>
            ) : null}
            <div
              style={{
                display: "flex",
                justifyContent: "flex-end",
                gap: 8,
                marginTop: 8,
              }}
            >
              {noneEligible ? (
                <button
                  type="button"
                  ref={okRef}
                  data-testid="explorer-bulk-stage-close"
                  autoFocus
                  onClick={() => onDone({ ran: false, refresh: false })}
                >
                  {message(EXPLORER_MSG.BULK_STAGE_CLOSE)}
                </button>
              ) : (
                <>
                  <button
                    type="button"
                    data-testid="explorer-bulk-stage-cancel"
                    disabled={busy}
                    onClick={() => onDone({ ran: false, refresh: false })}
                  >
                    {message(EXPLORER_MSG.CONFIRM_CANCEL)}
                  </button>
                  <button
                    type="submit"
                    ref={okRef}
                    data-testid="explorer-bulk-stage-confirm"
                    disabled={busy}
                  >
                    {message(
                      mode === "stage"
                        ? EXPLORER_MSG.BULK_STAGE_CONFIRM_BUTTON
                        : EXPLORER_MSG.BULK_UNSTAGE_CONFIRM_BUTTON,
                    ).replace("{count}", String(eligible.length))}
                  </button>
                </>
              )}
            </div>
          </>
        )}
      </form>
    </div>
  );
}
