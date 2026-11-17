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
import type { PSPathItem } from "../api/contentExplorer/types";
import { findChildren } from "../api/contentExplorer/pathApi";
import { emptyRecycleBin } from "../api/contentExplorer/folderMutations";
import { message } from "../i18n/message";
import { formatEmptyRecycleError } from "./emptyRecycleErrors";
import { EXPLORER_MSG } from "./messages";

const FOCUSABLE =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/** Max bin names shown on the confirm step before a "+N more" tail. */
const MAX_LISTED_NAMES = 25;

/**
 * Explorer roots that can hold recycled rows. Recycled folders land under
 * structural mirrors ({@code Recycling/Assets}, {@code Recycling/Sites}),
 * so the top-level bin listing alone misses them (#6257) — the same roots
 * {@code findInRecycling} probes.
 */
function binListRoots(binPath: string): string[] {
  const base = String(binPath ?? "").trim().replace(/\/+$/, "") || "/Recycling";
  return [base, `${base}/Assets`, `${base}/Sites`];
}

function rowKey(item: PSPathItem): string {
  return (
    (item.id ?? "").trim() ||
    (item.path ?? item.folderPath ?? "").trim() ||
    (item.name ?? "").trim()
  );
}

/**
 * Default bin loader: lists each bin root, skipping roots the server does
 * not expose (404 on H2 QA), and dedupes rows listed under several roots.
 */
export async function loadBinContents(binPath: string): Promise<PSPathItem[]> {
  const seen = new Set<string>();
  const rows: PSPathItem[] = [];
  for (const root of binListRoots(binPath)) {
    let kids: PSPathItem[];
    try {
      kids = (await findChildren(root)) ?? [];
    } catch {
      // Roots the server does not expose (404 on H2 QA) contribute nothing.
      continue;
    }
    for (const kid of kids) {
      const key = rowKey(kid);
      if (!key || seen.has(key)) {
        continue;
      }
      seen.add(key);
      rows.push(kid);
    }
  }
  return rows;
}

export interface EmptyRecycleOutcome {
  /** True when the confirm step accepted and the empty call was attempted. */
  ran: boolean;
  /** True when the bin emptied so the list should refresh. */
  refresh: boolean;
}

export interface EmptyRecycleDialogProps {
  /** Explorer path of the open recycle bin (usually {@code /Recycling}). */
  binPath: string;
  /**
   * Loads the bin contents for the confirm list. Defaults to the production
   * {@code findChildren} listing; tests inject a stub.
   */
  loadBin?: () => Promise<PSPathItem[]>;
  /**
   * Runs the empty call. Defaults to the production POST; tests inject a
   * stub. Host refresh (list + tree) stays with the caller via
   * {@link EmptyRecycleOutcome.refresh}.
   */
  runEmpty?: () => Promise<void>;
  onDone: (outcome: EmptyRecycleOutcome) => void;
}

type Step = "loading" | "confirm" | "loadError" | "running" | "report";

function itemLabel(item: PSPathItem): string {
  return (item.name ?? item.path ?? item.id ?? "item").trim() || "item";
}

/**
 * Empty-recycle-bin confirm with per-item reporting (#6470).
 * Loading lists the bin contents (loading / empty / load-error states);
 * confirm names what will be deleted; OK empties and the report names each
 * removed item with summary counts. Replaces the {@code window.confirm} for
 * this flow so the round-trip (recycle → restore → empty) stays in React.
 */
export function EmptyRecycleDialog({
  binPath,
  loadBin,
  runEmpty,
  onDone,
}: EmptyRecycleDialogProps): React.ReactElement {
  const [step, setStep] = useState<Step>("loading");
  const [items, setItems] = useState<PSPathItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const rootRef = useRef<HTMLDivElement>(null);
  const okRef = useRef<HTMLButtonElement>(null);

  const busy = step === "loading" || step === "running";
  const titleId = "explorer-empty-recycle-title";
  const listId = "explorer-empty-recycle-list";

  useDialogEscape(true, busy, () => {
    if (!busy) {
      onDone({ ran: false, refresh: false });
    }
  });

  useEffect(() => {
    let cancelled = false;
    setStep("loading");
    setError(null);
    const load = loadBin ?? (() => loadBinContents(binPath));
    load().then(
      (rows) => {
        if (cancelled) {
          return;
        }
        setItems(rows ?? []);
        setStep("confirm");
      },
      () => {
        if (cancelled) {
          return;
        }
        setStep("loadError");
      },
    );
    return () => {
      cancelled = true;
    };
    // Load once per mount: the bin snapshot belongs to this dialog.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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

  async function confirmRun(): Promise<void> {
    if (busy || items.length === 0) {
      return;
    }
    setStep("running");
    setError(null);
    try {
      if (runEmpty) {
        await runEmpty();
      } else {
        await emptyRecycleBin();
      }
      setStep("report");
    } catch (err) {
      setError(formatEmptyRecycleError(err));
      setStep("report");
    }
  }

  const names = items.map(itemLabel);
  const listed = names.slice(0, MAX_LISTED_NAMES);
  const overflow = names.length - listed.length;
  const confirmBody = message(EXPLORER_MSG.EMPTY_RECYCLE_CONFIRM_COUNT)
    .split("{count}")
    .join(String(items.length));
  const reportBody =
    error ??
    message(EXPLORER_MSG.EMPTY_RECYCLE_REMOVED)
      .split("{count}")
      .join(String(items.length))
      .split("{names}")
      .join(names.join(", "));

  return (
    <div
      ref={rootRef}
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      data-testid="explorer-empty-recycle-confirm"
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
          if (step === "confirm" && items.length > 0) {
            void confirmRun();
          } else if (step === "report") {
            onDone({ ran: true, refresh: error == null });
          }
        }}
      >
        <h2 id={titleId} style={{ fontSize: 16, margin: "0 0 12px" }}>
          {message(EXPLORER_MSG.ACTION_EMPTY_RECYCLE)}
        </h2>
        {step === "loading" ? (
          <p
            role="status"
            data-testid="explorer-empty-recycle-loading"
            style={{ fontSize: 13, margin: "0 0 8px" }}
          >
            {message(EXPLORER_MSG.EMPTY_RECYCLE_LOADING)}
          </p>
        ) : step === "loadError" ? (
          <>
            <p
              role="alert"
              data-testid="explorer-empty-recycle-load-error"
              style={{ fontSize: 13, margin: "0 0 8px" }}
            >
              {message(EXPLORER_MSG.EMPTY_RECYCLE_LOAD_FAILED)}
            </p>
            <div
              style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}
            >
              <button
                type="button"
                data-testid="explorer-empty-recycle-cancel"
                onClick={() => onDone({ ran: false, refresh: false })}
              >
                {message(EXPLORER_MSG.CONFIRM_CANCEL)}
              </button>
              <button
                type="button"
                ref={okRef}
                data-testid="explorer-empty-recycle-retry"
                onClick={() => {
                  setStep("loading");
                  const load = loadBin ?? (() => loadBinContents(binPath));
                  load().then(
                    (rows) => {
                      setItems(rows ?? []);
                      setStep("confirm");
                    },
                    () => setStep("loadError"),
                  );
                }}
              >
                {message(EXPLORER_MSG.RETRY)}
              </button>
            </div>
          </>
        ) : step === "report" ? (
          <>
            <p
              role={error == null ? "status" : "alert"}
              data-testid="explorer-empty-recycle-report"
              style={{ fontSize: 13, margin: "0 0 8px" }}
            >
              {reportBody}
            </p>
            {error == null && names.length > 0 ? (
              <ul
                aria-label={message(EXPLORER_MSG.ACTION_EMPTY_RECYCLE)}
                data-testid="explorer-empty-recycle-report-list"
                style={{
                  fontSize: 13,
                  margin: "0 0 8px",
                  paddingLeft: 18,
                  overflowY: "auto",
                  maxHeight: 240,
                }}
              >
                {names.map((name, index) => (
                  <li
                    key={`${name}-${index}`}
                    data-testid="explorer-empty-recycle-report-row"
                    data-outcome="removed"
                  >
                    <span>{name}</span>
                    {" — "}
                    <span>{message(EXPLORER_MSG.EMPTY_RECYCLE_ROW_REMOVED)}</span>
                  </li>
                ))}
              </ul>
            ) : null}
            <div
              style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}
            >
              <button
                type="submit"
                ref={okRef}
                data-testid="explorer-empty-recycle-close"
                autoFocus
              >
                {message(EXPLORER_MSG.CONFIRM_OK)}
              </button>
            </div>
          </>
        ) : (
          <>
            {step === "running" ? (
              <p
                role="status"
                data-testid="explorer-empty-recycle-running"
                style={{ fontSize: 13, margin: "0 0 8px" }}
              >
                {message(EXPLORER_MSG.EMPTY_RECYCLE_LOADING)}
              </p>
            ) : items.length === 0 ? (
              <p
                role="status"
                data-testid="explorer-empty-recycle-empty"
                style={{ fontSize: 13, margin: "0 0 8px" }}
              >
                {message(EXPLORER_MSG.EMPTY_RECYCLE_BIN_EMPTY)}
              </p>
            ) : (
              <>
                <p style={{ fontSize: 13, margin: "0 0 8px" }}>{confirmBody}</p>
                <ul
                  id={listId}
                  data-testid="explorer-empty-recycle-list"
                  style={{
                    fontSize: 13,
                    margin: "0 0 8px",
                    paddingLeft: 18,
                    overflowY: "auto",
                    maxHeight: 240,
                  }}
                >
                  {listed.map((name, index) => (
                    <li key={`${name}-${index}`}>{name}</li>
                  ))}
                  {overflow > 0 ? (
                    <li data-testid="explorer-empty-recycle-more">
                      {message(EXPLORER_MSG.EMPTY_RECYCLE_AND_MORE)
                        .split("{count}")
                        .join(String(overflow))}
                    </li>
                  ) : null}
                </ul>
              </>
            )}
            <div
              style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}
            >
              <button
                type="button"
                data-testid="explorer-empty-recycle-cancel"
                onClick={() => onDone({ ran: false, refresh: false })}
              >
                {message(EXPLORER_MSG.CONFIRM_CANCEL)}
              </button>
              <button
                type="submit"
                ref={okRef}
                data-testid="explorer-empty-recycle-ok"
                disabled={items.length === 0}
              >
                {message(EXPLORER_MSG.CONFIRM_OK)}
              </button>
            </div>
          </>
        )}
      </form>
    </div>
  );
}
