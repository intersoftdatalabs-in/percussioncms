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
  duplicateAutoTranslationKey,
  isAutoTranslationSetReady,
  listAutoTranslations,
  saveAutoTranslations,
} from "../api/developer/autoTranslationsApi";
import { listCommunities } from "../api/developer/assemblyApi";
import { listContentTypes } from "../api/developer/contentTypesApi";
import { listLocales } from "../api/developer/localesApi";
import { listWorkflows } from "../api/developer/workflowsApi";
import type {
  AutoTranslationRow,
  CommunitySummary,
  ContentTypeSummary,
  LocaleSummary,
  WorkflowDef,
} from "../api/developer/types";
import { catalogColors, backButton, errorAlert } from "./catalogStyles";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

type DraftRow = AutoTranslationRow & { clientKey: string };

let rowSeq = 0;
function nextClientKey(): string {
  rowSeq += 1;
  return `at-${rowSeq}`;
}

function toDraft(row: AutoTranslationRow): DraftRow {
  return { ...row, clientKey: nextClientKey() };
}

function emptyDraft(defaults?: Partial<AutoTranslationRow>): DraftRow {
  return {
    clientKey: nextClientKey(),
    locale: defaults?.locale || "",
    contentTypeName: defaults?.contentTypeName || "",
    workflowName: defaults?.workflowName || "",
    communityName: defaults?.communityName || "",
  };
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
    return panelErrMsg(err, DEV_MSG.AT_UNKNOWN);
  }
  if (kind === "lock") {
    return panelErrMsg(err, DEV_MSG.AT_LOCK);
  }
  if (isApiError(err)) {
    const fromBody = extractRestErrorMessage(err.body);
    if (fromBody) {
      return panelErrMsg(err, DEV_MSG.AT_SAVE_ERROR);
    }
  }
  return panelErrMsg(err, DEV_MSG.AT_SAVE_ERROR);
}

/**
 * CD-18 remainder — singleton auto-translation set editor on Developer Locales.
 */
export function AutoTranslationsPanel({
  onBack,
}: {
  onBack: () => void;
}): React.ReactElement {
  const [rows, setRows] = useState<DraftRow[] | null>(null);
  const [locales, setLocales] = useState<LocaleSummary[]>([]);
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
    setRows(null);
    return Promise.allSettled([
      listAutoTranslations(),
      listLocales(),
      listContentTypes(),
      listWorkflows(),
      listCommunities(),
    ]).then((results) => {
      if (!mountedRef.current) return;
      const [at, loc, ct, wf, comm] = results;
      if (at.status === "rejected") {
        setError(panelErrMsg(at.reason, DEV_MSG.AT_ERROR));
        return;
      }
      setRows(at.value.map(toDraft));
      const catalogFailed =
        loc.status === "rejected" ||
        ct.status === "rejected" ||
        wf.status === "rejected" ||
        comm.status === "rejected";
      if (catalogFailed) {
        setCatalogWarning(DEV_MSG.AT_CATALOG_ERROR);
      }
      if (loc.status === "fulfilled") setLocales(loc.value);
      if (ct.status === "fulfilled") setTypes(ct.value);
      if (wf.status === "fulfilled") setWorkflows(wf.value);
      if (comm.status === "fulfilled") setCommunities(comm.value);
    });
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const writeRows: AutoTranslationRow[] = useMemo(
    () =>
      (rows || []).map(({ clientKey: _k, ...rest }) => rest),
    [rows],
  );

  const duplicateKey = rows == null ? null : duplicateAutoTranslationKey(writeRows);
  const canSave =
    !busy && rows != null && isAutoTranslationSetReady(writeRows) && duplicateKey == null;

  function patchRow(index: number, patch: Partial<AutoTranslationRow>): void {
    setRows((prev) => {
      if (!prev) return prev;
      return prev.map((row, i) => (i === index ? { ...row, ...patch } : row));
    });
  }

  function addRow(): void {
    setNotice(null);
    setError(null);
    const defaults: Partial<AutoTranslationRow> = {
      locale: locales[0]?.languageString || "",
      contentTypeName: types[0]?.name || "",
      workflowName: workflows[0]?.workflowName || "",
      communityName: communities[0]?.name || "",
    };
    setRows((prev) => [...(prev || []), emptyDraft(defaults)]);
  }

  function removeRow(index: number): void {
    setNotice(null);
    setError(null);
    setRows((prev) => (prev || []).filter((_, i) => i !== index));
  }

  async function handleSave(): Promise<void> {
    if (!canSave || inflight.current || rows == null) {
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await saveAutoTranslations(writeRows);
      if (!mountedRef.current) return;
      setRows(saved.map(toDraft));
      setNotice(DEV_MSG.AT_SAVED);
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

  if (error && rows == null) {
    return (
      <div data-testid="developer-at-panel">
        <button type="button" data-testid="developer-at-back" onClick={onBack} style={backButton}>
          {DEV_MSG.AT_BACK}
        </button>
        <CatalogStatus testId="developer-at-error" error>
          {error}
        </CatalogStatus>
      </div>
    );
  }

  if (rows == null) {
    return (
      <div data-testid="developer-at-panel">
        <CatalogStatus testId="developer-at-loading">{DEV_MSG.AT_LOADING}</CatalogStatus>
      </div>
    );
  }

  return (
    <div data-testid="developer-at-panel">
      <button type="button" data-testid="developer-at-back" onClick={onBack} style={backButton}>
        {DEV_MSG.AT_BACK}
      </button>
      <h2 data-testid="developer-at-title" style={{ fontSize: "1.1rem", margin: "0 0 8px" }}>
        {DEV_MSG.AT_TITLE}
      </h2>
      <CatalogHint>{DEV_MSG.AT_HINT}</CatalogHint>
      {catalogWarning ? (
        <CatalogStatus testId="developer-at-catalog-warning">{catalogWarning}</CatalogStatus>
      ) : null}
      {error ? (
        <div data-testid="developer-at-error" role="alert" style={{ ...errorAlert, marginBottom: "12px" }}>
          {error}
        </div>
      ) : null}
      {notice ? (
        <div data-testid="developer-at-notice" style={{ marginBottom: "12px", color: catalogColors.accent }}>
          {notice}
        </div>
      ) : null}
      {duplicateKey ? (
        <div data-testid="developer-at-duplicate" role="alert" style={{ ...errorAlert, marginBottom: "12px" }}>
          {DEV_MSG.AT_DUPLICATE}
        </div>
      ) : null}

      {rows.length === 0 ? (
        <CatalogStatus testId="developer-at-empty">{DEV_MSG.AT_EMPTY}</CatalogStatus>
      ) : (
        <div style={{ overflowX: "auto" }}>
          <table
            data-testid="developer-at-table"
            style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
          >
            <thead>
              <tr style={{ textAlign: "left", borderBottom: `2px solid ${catalogColors.headerBorder}` }}>
                <th style={{ padding: "8px" }}>{DEV_MSG.AT_COL_LOCALE}</th>
                <th style={{ padding: "8px" }}>{DEV_MSG.AT_COL_TYPE}</th>
                <th style={{ padding: "8px" }}>{DEV_MSG.AT_COL_WORKFLOW}</th>
                <th style={{ padding: "8px" }}>{DEV_MSG.AT_COL_COMMUNITY}</th>
                <th style={{ padding: "8px" }} />
              </tr>
            </thead>
            <tbody>
              {rows.map((row, index) => (
                <tr
                  key={row.clientKey}
                  data-testid={`developer-at-row-${index}`}
                  data-at-locale={row.locale || ""}
                  data-at-type={row.contentTypeName || ""}
                  style={{ borderBottom: `1px solid ${catalogColors.rowBorder}` }}
                >
                  <td style={{ padding: "6px" }}>
                    <input
                      list="developer-at-locale-options"
                      data-testid={`developer-at-locale-${index}`}
                      aria-label={DEV_MSG.AT_COL_LOCALE}
                      value={row.locale || ""}
                      onChange={(e) => patchRow(index, { locale: e.target.value })}
                      style={{ ...inputStyle, fontFamily: "monospace" }}
                    />
                  </td>
                  <td style={{ padding: "6px" }}>
                    <input
                      list="developer-at-type-options"
                      data-testid={`developer-at-type-${index}`}
                      aria-label={DEV_MSG.AT_COL_TYPE}
                      value={row.contentTypeName || ""}
                      onChange={(e) =>
                        patchRow(index, { contentTypeName: e.target.value, contentTypeId: undefined })
                      }
                      style={{ ...inputStyle, fontFamily: "monospace" }}
                    />
                  </td>
                  <td style={{ padding: "6px" }}>
                    <input
                      list="developer-at-workflow-options"
                      data-testid={`developer-at-workflow-${index}`}
                      aria-label={DEV_MSG.AT_COL_WORKFLOW}
                      value={row.workflowName || ""}
                      onChange={(e) =>
                        patchRow(index, { workflowName: e.target.value, workflowId: undefined })
                      }
                      style={inputStyle}
                    />
                  </td>
                  <td style={{ padding: "6px" }}>
                    <input
                      list="developer-at-community-options"
                      data-testid={`developer-at-community-${index}`}
                      aria-label={DEV_MSG.AT_COL_COMMUNITY}
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
                      data-testid={`developer-at-remove-${index}`}
                      aria-label={`${DEV_MSG.AT_REMOVE} ${index + 1}`}
                      onClick={() => removeRow(index)}
                      style={smallBtnStyle}
                    >
                      {DEV_MSG.AT_REMOVE}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <datalist id="developer-at-locale-options">
        {locales.map((loc) => (
          <option
            key={loc.languageString || String(loc.id)}
            value={loc.languageString || ""}
            label={loc.label || loc.languageString}
          />
        ))}
      </datalist>
      <datalist id="developer-at-type-options">
        {types.map((ct) => (
          <option key={ct.name || ct.guidString} value={ct.name || ""} label={ct.label || ct.name} />
        ))}
      </datalist>
      <datalist id="developer-at-workflow-options">
        {workflows.map((wf) => (
          <option key={wf.workflowName} value={wf.workflowName || ""} />
        ))}
      </datalist>
      <datalist id="developer-at-community-options">
        {communities.map((c) => (
          <option key={c.name || String(c.id)} value={c.name || ""} label={c.label || c.name} />
        ))}
      </datalist>

      <div style={{ display: "flex", gap: "8px", marginTop: "16px", flexWrap: "wrap" }}>
        <button
          type="button"
          data-testid="developer-at-add"
          onClick={addRow}
          disabled={busy}
          style={{
            ...smallBtnStyle,
            cursor: busy ? "not-allowed" : "pointer",
          }}
        >
          {DEV_MSG.AT_ADD}
        </button>
        <button
          type="button"
          data-testid="developer-at-save"
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
          {DEV_MSG.AT_SAVE}
        </button>
      </div>
    </div>
  );
}
