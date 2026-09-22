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

import React, { useMemo, useState } from "react";
import { formatApiError, isSessionRedirectError } from "../api/client";
import {
  listColumnsHttpStatus,
  saveExplorerListColumns,
} from "../api/contentExplorer/listColumnsApi";
import { DISPLAY_FORMAT_FIELD_CATALOG, SYS_TITLE_SOURCE } from "../developer/displayFormatColumns";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";

export interface ExplorerListColumnsPanelProps {
  folderPath: string | null;
  /** Sources currently applied to the list (may be empty). */
  selectedSources: readonly string[];
  onSaved: (sources: string[]) => void;
  saveColumns?: (
    folderPath: string,
    columns: readonly string[],
  ) => Promise<{ columns: string[] }>;
}

export function ExplorerListColumnsPanel({
  folderPath,
  selectedSources,
  onSaved,
  saveColumns = saveExplorerListColumns,
}: ExplorerListColumnsPanelProps): React.ReactElement {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<string[]>(() =>
    selectedSources.length > 0 ? [...selectedSources] : [SYS_TITLE_SOURCE],
  );
  const [error, setError] = useState<string | null>(null);
  const [httpStatus, setHttpStatus] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);

  const selected = useMemo(() => new Set(draft.map((s) => s.toLowerCase())), [draft]);

  function toggle(source: string): void {
    if (source === SYS_TITLE_SOURCE) return;
    setDraft((prev) => {
      const key = source.toLowerCase();
      if (prev.some((s) => s.toLowerCase() === key)) {
        return prev.filter((s) => s.toLowerCase() !== key);
      }
      return [...prev, source];
    });
  }

  async function apply(): Promise<void> {
    setError(null);
    setHttpStatus(null);
    const path = folderPath?.trim() ?? "";
    if (!path) {
      setHttpStatus(400);
      setError(message(EXPLORER_MSG.LIST_COLUMNS_NEED_FOLDER));
      return;
    }
    setBusy(true);
    try {
      const saved = await saveColumns(path, draft);
      const next = saved.columns.length > 0 ? saved.columns : draft;
      onSaved(next);
      setOpen(false);
    } catch (err) {
      if (isSessionRedirectError(err)) return;
      const status = listColumnsHttpStatus(err);
      setHttpStatus(status);
      const detail = formatApiError(err, message(EXPLORER_MSG.LIST_COLUMNS_SAVE_ERROR));
      const withStatus =
        status != null && !detail.includes(`HTTP ${status}`)
          ? `${detail} (HTTP ${status})`
          : detail;
      setError(withStatus);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="explorer-list-columns">
      <button
        type="button"
        data-testid="explorer-list-columns-open"
        onClick={() => {
          setDraft(selectedSources.length > 0 ? [...selectedSources] : [SYS_TITLE_SOURCE]);
          setError(null);
          setHttpStatus(null);
          setOpen((v) => !v);
        }}
      >
        {message(EXPLORER_MSG.LIST_COLUMNS_OPEN)}
      </button>
      {open ? (
        <div
          role="dialog"
          aria-label={message(EXPLORER_MSG.LIST_COLUMNS_OPEN)}
          data-testid="explorer-list-columns-dialog"
        >
          <ul style={{ listStyle: "none", margin: "8px 0", padding: 0 }}>
            {DISPLAY_FORMAT_FIELD_CATALOG.map((field) => {
              const locked = field.source === SYS_TITLE_SOURCE;
              return (
                <li key={field.source}>
                  <label>
                    <input
                      type="checkbox"
                      data-testid={`explorer-list-column-${field.source}`}
                      checked={locked || selected.has(field.source)}
                      disabled={locked || busy}
                      onChange={() => toggle(field.source)}
                    />{" "}
                    {field.label}
                  </label>
                </li>
              );
            })}
          </ul>
          <button
            type="button"
            data-testid="explorer-list-columns-apply"
            disabled={busy}
            onClick={() => void apply()}
          >
            {message(EXPLORER_MSG.LIST_COLUMNS_APPLY)}
          </button>
        </div>
      ) : null}
      {error ? (
        <p
          role="alert"
          data-testid="explorer-list-columns-error"
          data-http-status={httpStatus ?? ""}
        >
          {error}
        </p>
      ) : null}
    </div>
  );
}
