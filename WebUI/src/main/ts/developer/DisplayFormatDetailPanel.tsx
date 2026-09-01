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
import { isApiError } from "../api/client";
import {
  getDisplayFormatDetail,
  normalizeColumns,
  resolveDisplayFormatObjectGuid,
  updateDisplayFormat,
} from "../api/developer/displayFormatsApi";
import type { DisplayFormat, DisplayFormatColumn } from "../api/developer/types";
import { catalogColors, backButton, errorAlert, metaGrid, monoCell, tableHeaderRow, tableRow } from "./catalogStyles";
import {
  addDisplayFormatColumn,
  catalogFieldsNotInUse,
  columnsOrderEqual,
  isPackagedDisplayFormat,
  isSysTitleColumn,
  isValidColumnSource,
  moveDisplayFormatColumn,
  reindexColumns,
  removeDisplayFormatColumn,
} from "./displayFormatColumns";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { ObjectAclSection } from "./ObjectAclSection";

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
};

const actionButton: React.CSSProperties = {
  padding: "4px 8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  background: catalogColors.surface,
  cursor: "pointer",
  font: "inherit",
};

export function DisplayFormatDetailPanel({
  idOrName,
  catalogGuid,
  onBack,
  onColumnsSaved,
}: {
  idOrName: string;
  /** GUID from catalog list row when detail wire omits stringValue (#2951). */
  catalogGuid?: string | null;
  onBack: () => void;
  onColumnsSaved?: (detail: DisplayFormat) => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<DisplayFormat | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [draftColumns, setDraftColumns] = useState<DisplayFormatColumn[]>([]);
  const [addSource, setAddSource] = useState("");
  const inflight = useRef(false);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setDraftColumns([]);
    setAddSource("");
    getDisplayFormatDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        const cols = reindexColumns(normalizeColumns(d.columns));
        setDraftColumns(cols);
        setAddSource(catalogFieldsNotInUse(cols)[0]?.source || "");
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.DF_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const loadedColumns = useMemo(
    () => (detail != null ? reindexColumns(normalizeColumns(detail.columns)) : []),
    [detail],
  );
  const objectGuid = resolveDisplayFormatObjectGuid(detail, catalogGuid);
  const formatName = detail?.name || detail?.internalName || idOrName;
  // Packaged vs user is the catalog key we opened, not a replayed GET name (#3269).
  const packaged = isPackagedDisplayFormat(idOrName);
  const columns = packaged ? loadedColumns : draftColumns;
  const availableFields = catalogFieldsNotInUse(draftColumns);
  const dirty = !packaged && !columnsOrderEqual(draftColumns, loadedColumns);
  const canSave = !busy && dirty && detail != null;

  function saveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 400) return DEV_MSG.DF_COLUMNS_INVALID_SOURCE;
    if (isApiError(err) && err.status === 403) return DEV_MSG.DF_COLUMNS_FORBIDDEN;
    if (isApiError(err) && err.status === 404) return DEV_MSG.DF_COLUMNS_NOT_FOUND;
    return DEV_MSG.DF_COLUMNS_SAVE_ERROR;
  }

  function handleAdd(): void {
    if (packaged || busy || !isValidColumnSource(addSource)) {
      return;
    }
    const next = addDisplayFormatColumn(draftColumns, addSource);
    setDraftColumns(next);
    const stillAvailable = catalogFieldsNotInUse(next);
    setAddSource(stillAvailable[0]?.source || "");
  }

  async function handleSave(): Promise<void> {
    if (!canSave || inflight.current || packaged) {
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await updateDisplayFormat(idOrName, {
        name: formatName,
        label: detail?.label || detail?.displayName,
        displayName: detail?.displayName || detail?.label,
        description: detail?.description,
        columns: reindexColumns(draftColumns),
      });
      setDetail(saved);
      setDraftColumns(reindexColumns(normalizeColumns(saved.columns)));
      setNotice(DEV_MSG.DF_COLUMNS_SAVED);
      onColumnsSaved?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, saveFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  return (
    <div data-testid="developer-df-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-df-back"
        aria-label={DEV_MSG.DF_BACK}
        style={backButton}
      >
        ← {DEV_MSG.DF_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-df-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid="developer-df-editor-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-df-detail-loading">{DEV_MSG.DF_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-df-detail-title">
              {detail.label || detail.displayName || detail.name || idOrName}
            </h2>
            {detail.description ? (
              <p style={{ marginTop: "8px", color: catalogColors.muted }}>{detail.description}</p>
            ) : null}
            <dl style={metaGrid}>
              <dt>{DEV_MSG.DF_COL_NAME}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.name || "—"}</dd>
              <dt>{DEV_MSG.DF_COL_GUID}</dt>
              <dd
                style={{ margin: 0, ...monoCell }}
                data-testid="developer-df-detail-guid"
              >
                {objectGuid || "—"}
              </dd>
              <dt>{DEV_MSG.DF_COL_USAGE}</dt>
              <dd style={{ margin: 0 }}>
                {[
                  detail.validForFolder ? DEV_MSG.DF_USAGE_FOLDER : null,
                  detail.validForViewsAndSearches ? DEV_MSG.DF_USAGE_VIEWS : null,
                  detail.validForRelatedContent ? DEV_MSG.DF_USAGE_RELATED : null,
                ]
                  .filter(Boolean)
                  .join(", ") || "—"}
              </dd>
            </dl>
          </header>

          <section data-testid="developer-df-columns">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.DF_COLUMNS}</h3>
            <p
              style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
              data-testid={packaged ? "developer-df-columns-readonly" : undefined}
            >
              {packaged ? DEV_MSG.DF_COLUMNS_READONLY : DEV_MSG.DF_COLUMNS_HINT}
            </p>
            {columns.length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-df-columns-empty">
                {DEV_MSG.DF_NONE}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-df-columns-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.9rem",
                  }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_POS}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_SOURCE}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_COL_LABEL}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_RENDER}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_WIDTH}</th>
                      {!packaged ? (
                        <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_ACTIONS}</th>
                      ) : null}
                    </tr>
                  </thead>
                  <tbody>
                    {columns.map((c, i) => (
                      <tr
                        key={`${c.source ?? "col"}-${c.position ?? i}-${i}`}
                        data-testid={`developer-df-column-row-${i}`}
                        data-df-column-source={c.source || ""}
                        style={tableRow}
                      >
                        <td style={{ padding: "8px" }}>
                          {c.position != null ? String(c.position) : "—"}
                        </td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {c.source || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{c.displayName || "—"}</td>
                        <td style={{ padding: "8px" }}>{c.renderType || "—"}</td>
                        <td style={{ padding: "8px" }}>
                          {c.width != null && c.width > 0 ? String(c.width) : "—"}
                        </td>
                        {!packaged ? (
                          <td style={{ padding: "8px" }}>
                            <div style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
                              <button
                                type="button"
                                data-testid={`developer-df-column-up-${i}`}
                                aria-label={DEV_MSG.DF_COLUMNS_MOVE_UP}
                                disabled={busy || i === 0}
                                onClick={() => setDraftColumns(moveDisplayFormatColumn(draftColumns, i, -1))}
                                style={actionButton}
                              >
                                {DEV_MSG.DF_COLUMNS_MOVE_UP}
                              </button>
                              <button
                                type="button"
                                data-testid={`developer-df-column-down-${i}`}
                                aria-label={DEV_MSG.DF_COLUMNS_MOVE_DOWN}
                                disabled={busy || i === columns.length - 1}
                                onClick={() => setDraftColumns(moveDisplayFormatColumn(draftColumns, i, 1))}
                                style={actionButton}
                              >
                                {DEV_MSG.DF_COLUMNS_MOVE_DOWN}
                              </button>
                              <button
                                type="button"
                                data-testid={`developer-df-column-remove-${i}`}
                                aria-label={DEV_MSG.DF_COLUMNS_REMOVE}
                                disabled={busy || isSysTitleColumn(c)}
                                onClick={() => setDraftColumns(removeDisplayFormatColumn(draftColumns, i))}
                                style={actionButton}
                              >
                                {DEV_MSG.DF_COLUMNS_REMOVE}
                              </button>
                            </div>
                          </td>
                        ) : null}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {!packaged ? (
              <div
                style={{ marginTop: "12px", display: "flex", gap: "8px", flexWrap: "wrap", alignItems: "flex-end" }}
                data-testid="developer-df-column-editor"
              >
                <label htmlFor="df-column-source" style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                  {DEV_MSG.DF_COLUMNS_SOURCE_PICKER}
                  <select
                    id="df-column-source"
                    data-testid="developer-df-column-source"
                    style={inputStyle}
                    value={addSource}
                    disabled={busy || availableFields.length === 0}
                    onChange={(e) => setAddSource(e.target.value)}
                  >
                    <option value="">{availableFields.length ? "—" : DEV_MSG.DF_NONE}</option>
                    {availableFields.map((f) => (
                      <option key={f.source} value={f.source}>
                        {f.label} ({f.source})
                      </option>
                    ))}
                  </select>
                </label>
                <button
                  type="button"
                  data-testid="developer-df-column-add"
                  aria-label={DEV_MSG.DF_COLUMNS_ADD}
                  disabled={busy || !isValidColumnSource(addSource)}
                  onClick={handleAdd}
                  style={{
                    ...actionButton,
                    padding: "8px 12px",
                    background: isValidColumnSource(addSource) ? catalogColors.accent : catalogColors.disabled,
                    color: "#fff",
                    border: "none",
                    cursor: isValidColumnSource(addSource) && !busy ? "pointer" : "not-allowed",
                  }}
                >
                  {DEV_MSG.DF_COLUMNS_ADD}
                </button>
                <button
                  type="button"
                  data-testid="developer-df-columns-save"
                  aria-label={DEV_MSG.DF_COLUMNS_SAVE}
                  disabled={!canSave}
                  onClick={() => void handleSave()}
                  style={{
                    padding: "8px 16px",
                    background: canSave ? catalogColors.accent : catalogColors.disabled,
                    color: "#fff",
                    border: "none",
                    borderRadius: "4px",
                    cursor: canSave ? "pointer" : "not-allowed",
                  }}
                >
                  {busy ? DEV_MSG.DF_COLUMNS_SAVING : DEV_MSG.DF_COLUMNS_SAVE}
                </button>
              </div>
            ) : null}
          </section>

          <ObjectAclSection
            objectGuid={objectGuid}
            objectKind="display-format"
            testIdPrefix="developer-df-acl"
          />

          <section style={{ marginTop: "16px" }} data-testid="developer-df-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.DF_GAPS}</h3>
            <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              <li>{DEV_MSG.DF_GAP_WRITE}</li>
              <li>{DEV_MSG.DF_GAP_COMMUNITIES}</li>
            </ul>
          </section>
        </>
      ) : null}
    </div>
  );
}
