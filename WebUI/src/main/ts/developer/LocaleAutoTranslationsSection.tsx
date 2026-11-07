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

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { extractRestErrorMessage, isApiError } from "../api/client";
import {
  classifyAutoTranslationSaveError,
  listAutoTranslations,
  saveAutoTranslations,
} from "../api/developer/autoTranslationsApi";
import { listCommunities } from "../api/developer/assemblyApi";
import { listContentTypes } from "../api/developer/contentTypesApi";
import { listWorkflows } from "../api/developer/workflowsApi";
import type {
  AutoTranslationRow,
  CommunitySummary,
  ContentTypeSummary,
  WorkflowDef,
} from "../api/developer/types";
import { catalogColors, errorAlert } from "./catalogStyles";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import {
  duplicateLocaleAutoTranslationKey,
  filterLocaleAutoTranslations,
  isLocaleAutoTranslationSetReady,
  mergeLocaleAutoTranslations,
  normalizeScopedLocale,
} from "./localeAutoTranslations";
import { LOC_AT_LOCALE_MSG } from "./localeAutoTranslationsMessages";
import { panelErrMsg } from "./errors";

type DraftRow = AutoTranslationRow & { clientKey: string };

let rowSeq = 0;
function nextClientKey(): string {
  rowSeq += 1;
  return `loc-at-${rowSeq}`;
}

function toDraft(row: AutoTranslationRow): DraftRow {
  return { ...row, clientKey: nextClientKey() };
}

function countSummary(count: number): string {
  if (count === 0) return LOC_AT_LOCALE_MSG.COUNT_ZERO;
  if (count === 1) return LOC_AT_LOCALE_MSG.COUNT_ONE;
  return LOC_AT_LOCALE_MSG.COUNT_MANY.replace("{0}", String(count));
}

const inputStyle: React.CSSProperties = {
  padding: "6px 8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

const smallBtnStyle: React.CSSProperties = {
  padding: "6px 10px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  background: catalogColors.surface,
  cursor: "pointer",
};

function saveErrMsg(err: unknown): string {
  const kind = classifyAutoTranslationSaveError(err);
  if (kind === "unknown") {
    return panelErrMsg(err, LOC_AT_LOCALE_MSG.UNKNOWN);
  }
  if (kind === "lock") {
    return panelErrMsg(err, LOC_AT_LOCALE_MSG.LOCK);
  }
  if (isApiError(err)) {
    const fromBody = extractRestErrorMessage(err.body);
    if (fromBody) {
      return panelErrMsg(err, LOC_AT_LOCALE_MSG.SAVE_ERROR);
    }
  }
  return panelErrMsg(err, LOC_AT_LOCALE_MSG.SAVE_ERROR);
}

/**
 * Per-locale auto-translation rows inside the locale editor (#5704).
 *
 * <p>Loads the full singleton set, shows only {@code languageString}'s rows
 * (the locale column is fixed text, not an input), and saves the edited rows
 * merged with every other locale's untouched rows. Removing every row and
 * saving clears only this locale. The locale's language, label, description,
 * status, and format fields stay because this save never calls the locale
 * PUT.</p>
 */
export function LocaleAutoTranslationsSection({
  languageString,
}: {
  languageString: string;
}): React.ReactElement {
  const locale = normalizeScopedLocale(languageString);
  const [fullRows, setFullRows] = useState<AutoTranslationRow[] | null>(null);
  const [rows, setRows] = useState<DraftRow[] | null>(null);
  const [types, setTypes] = useState<ContentTypeSummary[]>([]);
  const [workflows, setWorkflows] = useState<WorkflowDef[]>([]);
  const [communities, setCommunities] = useState<CommunitySummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [catalogWarning, setCatalogWarning] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const mountedRef = useRef(true);
  const inflight = useRef(false);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const load = useCallback(() => {
    if (!mountedRef.current) {
      return Promise.resolve();
    }
    setError(null);
    setNotice(null);
    setCatalogWarning(null);
    setFullRows(null);
    setRows(null);
    return Promise.allSettled([
      listAutoTranslations(),
      listContentTypes(),
      listWorkflows(),
      listCommunities(),
    ]).then((results) => {
      if (!mountedRef.current) return;
      const [at, ct, wf, comm] = results;
      if (at.status === "rejected") {
        setError(panelErrMsg(at.reason, LOC_AT_LOCALE_MSG.LOAD_ERROR));
        return;
      }
      setFullRows(at.value);
      setRows(filterLocaleAutoTranslations(at.value, locale).map(toDraft));
      const catalogFailed =
        ct.status === "rejected" ||
        wf.status === "rejected" ||
        comm.status === "rejected";
      if (catalogFailed) {
        setCatalogWarning(LOC_AT_LOCALE_MSG.CATALOG_ERROR);
      }
      if (ct.status === "fulfilled") setTypes(ct.value);
      if (wf.status === "fulfilled") setWorkflows(wf.value);
      if (comm.status === "fulfilled") setCommunities(comm.value);
    });
  }, [locale]);

  useEffect(() => {
    void load();
  }, [load]);

  const writeRows: AutoTranslationRow[] = useMemo(
    () =>
      (rows || []).map(({ clientKey: _k, ...rest }) => ({
        ...rest,
        locale,
      })),
    [rows, locale],
  );

  const duplicateKey = rows == null ? null : duplicateLocaleAutoTranslationKey(rows, locale);
  const canSave =
    !busy && rows != null && isLocaleAutoTranslationSetReady(writeRows) && duplicateKey == null;

  const writeKey = locale;

  function patchRow(index: number, patch: Partial<AutoTranslationRow>): void {
    setRows((prev) => {
      if (!prev) return prev;
      return prev.map((row, i) => (i === index ? { ...row, ...patch } : row));
    });
  }

  function addRow(): void {
    setNotice(null);
    setError(null);
    setRows((prev) => [
      ...(prev || []),
      {
        clientKey: nextClientKey(),
        locale,
        contentTypeName: types[0]?.name || "",
        workflowName: workflows[0]?.workflowName || "",
        communityName: communities[0]?.name || "",
      },
    ]);
  }

  function removeRow(index: number): void {
    setNotice(null);
    setError(null);
    setRows((prev) => (prev || []).filter((_, i) => i !== index));
  }

  async function handleSave(): Promise<void> {
    if (!canSave || inflight.current || rows == null || fullRows == null || !writeKey) {
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    const cleared = writeRows.length === 0;
    try {
      const merged = mergeLocaleAutoTranslations(fullRows, writeKey, writeRows);
      const saved = await saveAutoTranslations(merged);
      if (!mountedRef.current) return;
      setFullRows(saved);
      setRows(filterLocaleAutoTranslations(saved, writeKey).map(toDraft));
      setNotice(cleared ? LOC_AT_LOCALE_MSG.CLEARED : LOC_AT_LOCALE_MSG.SAVED);
    } catch (err: unknown) {
      if (!mountedRef.current) return;
      setError(saveErrMsg(err));
    } finally {
      inflight.current = false;
      if (mountedRef.current) {
        setBusy(false);
      }
    }
  }

  if (!writeKey) {
    return (
      <section data-testid="developer-loc-set-at" aria-label={LOC_AT_LOCALE_MSG.ACTION}>
        <CatalogStatus testId="developer-loc-set-at-error" error>
          {LOC_AT_LOCALE_MSG.LOAD_ERROR}
        </CatalogStatus>
      </section>
    );
  }

  if (error && rows == null) {
    return (
      <section data-testid="developer-loc-set-at" aria-label={LOC_AT_LOCALE_MSG.ACTION}>
        <h3 style={{ fontSize: "1rem", marginBottom: "8px" }}>{LOC_AT_LOCALE_MSG.ACTION}</h3>
        <CatalogStatus testId="developer-loc-set-at-error" error>
          {error}
        </CatalogStatus>
      </section>
    );
  }

  if (rows == null) {
    return (
      <section data-testid="developer-loc-set-at" aria-label={LOC_AT_LOCALE_MSG.ACTION}>
        <h3 style={{ fontSize: "1rem", marginBottom: "8px" }}>{LOC_AT_LOCALE_MSG.ACTION}</h3>
        <CatalogStatus testId="developer-loc-set-at-loading">
          {LOC_AT_LOCALE_MSG.LOADING}
        </CatalogStatus>
      </section>
    );
  }

  return (
    <section
      data-testid="developer-loc-set-at"
      aria-label={LOC_AT_LOCALE_MSG.ACTION}
      style={{ marginBottom: "16px" }}
    >
      <h3 style={{ fontSize: "1rem", marginBottom: "8px" }}>{LOC_AT_LOCALE_MSG.ACTION}</h3>
      <CatalogHint>{LOC_AT_LOCALE_MSG.HINT}</CatalogHint>
      <p
        data-testid="developer-loc-set-at-text"
        data-loc-at-count={rows.length}
        style={{ marginTop: 0, fontFamily: "monospace" }}
      >
        {countSummary(rows.length)}
      </p>
      {catalogWarning ? (
        <CatalogStatus testId="developer-loc-set-at-catalog-warning">{catalogWarning}</CatalogStatus>
      ) : null}
      {error ? (
        <div
          data-testid="developer-loc-set-at-error"
          role="alert"
          style={{ ...errorAlert, marginBottom: "12px" }}
        >
          {error}
        </div>
      ) : null}
      {notice ? (
        <div data-testid="developer-loc-set-at-notice" style={{ marginBottom: "12px", color: catalogColors.accent }}>
          {notice}
        </div>
      ) : null}
      {duplicateKey ? (
        <div data-testid="developer-loc-set-at-duplicate" role="alert" style={{ ...errorAlert, marginBottom: "12px" }}>
          {LOC_AT_LOCALE_MSG.DUPLICATE}
        </div>
      ) : null}

      {rows.length === 0 ? (
        <CatalogStatus testId="developer-loc-set-at-empty">{LOC_AT_LOCALE_MSG.EMPTY}</CatalogStatus>
      ) : (
        <div style={{ overflowX: "auto" }}>
          <table
            data-testid="developer-loc-set-at-table"
            style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
          >
            <thead>
              <tr style={{ textAlign: "left", borderBottom: `2px solid ${catalogColors.headerBorder}` }}>
                <th style={{ padding: "8px" }}>{LOC_AT_LOCALE_MSG.FIELD}</th>
                <th style={{ padding: "8px" }}>{LOC_AT_LOCALE_MSG.COL_TYPE}</th>
                <th style={{ padding: "8px" }}>{LOC_AT_LOCALE_MSG.COL_WORKFLOW}</th>
                <th style={{ padding: "8px" }}>{LOC_AT_LOCALE_MSG.COL_COMMUNITY}</th>
                <th style={{ padding: "8px" }} />
              </tr>
            </thead>
            <tbody>
              {rows.map((row, index) => (
                <tr
                  key={row.clientKey}
                  data-testid={`developer-loc-set-at-row-${index}`}
                  data-at-locale={locale}
                  data-at-type={row.contentTypeName || ""}
                  style={{ borderBottom: `1px solid ${catalogColors.rowBorder}` }}
                >
                  <td style={{ padding: "6px" }}>
                    <span
                      data-testid={`developer-loc-set-at-locale-${index}`}
                      style={{ fontFamily: "monospace" }}
                    >
                      {locale}
                    </span>
                  </td>
                  <td style={{ padding: "6px" }}>
                    <input
                      list="developer-loc-set-at-type-options"
                      data-testid={`developer-loc-set-at-type-${index}`}
                      aria-label={LOC_AT_LOCALE_MSG.COL_TYPE}
                      value={row.contentTypeName || ""}
                      onChange={(e) =>
                        patchRow(index, { contentTypeName: e.target.value, contentTypeId: undefined })
                      }
                      style={{ ...inputStyle, fontFamily: "monospace" }}
                    />
                  </td>
                  <td style={{ padding: "6px" }}>
                    <input
                      list="developer-loc-set-at-workflow-options"
                      data-testid={`developer-loc-set-at-workflow-${index}`}
                      aria-label={LOC_AT_LOCALE_MSG.COL_WORKFLOW}
                      value={row.workflowName || ""}
                      onChange={(e) =>
                        patchRow(index, { workflowName: e.target.value, workflowId: undefined })
                      }
                      style={inputStyle}
                    />
                  </td>
                  <td style={{ padding: "6px" }}>
                    <input
                      list="developer-loc-set-at-community-options"
                      data-testid={`developer-loc-set-at-community-${index}`}
                      aria-label={LOC_AT_LOCALE_MSG.COL_COMMUNITY}
                      value={row.communityName || ""}
                      onChange={(e) =>
                        patchRow(index, { communityName: e.target.value, communityId: undefined })
                      }
                      style={inputStyle}
                    />
                  </td>
                  <td style={{ padding: "6px" }}>
                    <button
                      type="button"
                      data-testid={`developer-loc-set-at-remove-${index}`}
                      aria-label={`${LOC_AT_LOCALE_MSG.REMOVE} ${index + 1}`}
                      onClick={() => removeRow(index)}
                      style={smallBtnStyle}
                    >
                      {LOC_AT_LOCALE_MSG.REMOVE}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <datalist id="developer-loc-set-at-type-options">
        {types.map((ct) => (
          <option key={ct.name || ct.guidString} value={ct.name || ""} label={ct.label || ct.name} />
        ))}
      </datalist>
      <datalist id="developer-loc-set-at-workflow-options">
        {workflows.map((wf) => (
          <option key={wf.workflowName} value={wf.workflowName || ""} />
        ))}
      </datalist>
      <datalist id="developer-loc-set-at-community-options">
        {communities.map((c) => (
          <option key={c.name || String(c.id)} value={c.name || ""} label={c.label || c.name} />
        ))}
      </datalist>

      <div style={{ display: "flex", gap: "8px", marginTop: "16px", flexWrap: "wrap" }}>
        <button
          type="button"
          data-testid="developer-loc-set-at-add"
          onClick={addRow}
          disabled={busy}
          style={{
            ...smallBtnStyle,
            cursor: busy ? "not-allowed" : "pointer",
          }}
        >
          {LOC_AT_LOCALE_MSG.ADD}
        </button>
        <button
          type="button"
          data-testid="developer-loc-set-at-save"
          onClick={() => void handleSave()}
          disabled={!canSave}
          style={{
            padding: "8px 14px",
            background: canSave ? catalogColors.accent : catalogColors.disabled,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: canSave ? "pointer" : "not-allowed",
          }}
        >
          {LOC_AT_LOCALE_MSG.SAVE}
        </button>
      </div>
    </section>
  );
}
