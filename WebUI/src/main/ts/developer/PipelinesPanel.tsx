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

import React, { useEffect, useMemo, useState } from "react";
import { useSpaBootstrap } from "../app/bootstrap/BootstrapContext";
import { isApiError } from "../api/client";
import { listApplications } from "../api/developer/pipelinesApi";
import {
  importPipelineApplication,
  invalidPipelineApplicationImportName,
  pipelineApplicationNameFromDesignXml,
  rewritePipelineApplicationXmlName,
} from "../api/developer/pipelineImportExport";
import type { ApplicationDetail, ApplicationSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import {
  catalogColors,
  errorAlert,
  monoCell,
  mutedCell,
  mutedMonoCell,
  openButtonStyle,
} from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { PipelineDetailPanel } from "./PipelineDetailPanel";

/**
 * P0.6 / P0.6b — classic XML Application catalog + read-only detail.
 */

/** Keep a loaded catalog when a later list GET fails (#4384). */
export function retainCatalogOnListError<T>(prev: T[] | null): T[] {
  return prev ?? [];
}

async function readFileText(file: File): Promise<string> {
  if (typeof file.text === "function") {
    return file.text();
  }
  return await new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result ?? ""));
    reader.onerror = () =>
      reject(reader.error ?? new Error("Could not read the selected file"));
    reader.readAsText(file);
  });
}

function pipeImportErrMsg(err: unknown): string {
  if (isApiError(err) && err.status === 400) {
    return panelErrMsg(err, DEV_MSG.PIPE_IMPORT_INVALID);
  }
  if (isApiError(err) && err.status === 409) {
    return panelErrMsg(err, DEV_MSG.PIPE_IMPORT_DUPLICATE);
  }
  if (isApiError(err) && err.status === 403) {
    return DEV_MSG.PIPE_IMPORT_EXPORT_FORBIDDEN;
  }
  if (
    err instanceof Error &&
    /pipeline application design XML|missing name|design XML is required/i.test(err.message)
  ) {
    return panelErrMsg(err, DEV_MSG.PIPE_IMPORT_INVALID);
  }
  return panelErrMsg(err, DEV_MSG.PIPE_IMPORT_ERROR);
}

export function PipelinesPanel(): React.ReactElement {
  const { isAdmin } = useSpaBootstrap();
  const [items, setItems] = useState<ApplicationSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [importXml, setImportXml] = useState("");
  const [importFileName, setImportFileName] = useState("");
  const [importName, setImportName] = useState("");
  const [importBusy, setImportBusy] = useState(false);
  const [importError, setImportError] = useState<string | null>(null);
  const [importNotice, setImportNotice] = useState<string | null>(null);

  async function refresh(): Promise<void> {
    try {
      const list = await listApplications();
      setItems(list);
    } catch (e: unknown) {
      setError(panelErrMsg(e, DEV_MSG.PIPE_ERROR));
      // Keep a previously loaded catalog (#4384) instead of wiping to empty.
      setItems((prev) => retainCatalogOnListError(prev));
    }
  }

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const list = await listApplications();
        if (!cancelled) setItems(list);
      } catch (e: unknown) {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.PIPE_ERROR));
        // Keep a previously loaded catalog (#4384) instead of wiping to empty.
        setItems((prev) => retainCatalogOnListError(prev));
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  async function onImportFileChange(file: File | null): Promise<void> {
    setImportError(null);
    setImportNotice(null);
    if (!file) {
      setImportXml("");
      setImportFileName("");
      return;
    }
    try {
      const text = await readFileText(file);
      setImportXml(text);
      setImportFileName(file.name);
      const fromXml = pipelineApplicationNameFromDesignXml(text);
      if (fromXml && !importName.trim()) {
        setImportName(fromXml);
      }
    } catch (e: unknown) {
      setImportXml("");
      setImportFileName("");
      setImportError(panelErrMsg(e, DEV_MSG.PIPE_IMPORT_ERROR));
    }
  }

  async function handleImport(): Promise<void> {
    setImportError(null);
    setImportNotice(null);
    const body = importXml.trim();
    if (!body) {
      setImportError(DEV_MSG.PIPE_IMPORT_NO_FILE);
      return;
    }
    const name = importName.trim();
    if (name) {
      const invalid = invalidPipelineApplicationImportName(name);
      if (invalid) {
        setImportError(DEV_MSG.PIPE_IMPORT_BAD_NAME);
        return;
      }
    }
    setImportBusy(true);
    try {
      const payload = name ? rewritePipelineApplicationXmlName(body, name) : body;
      const created: ApplicationDetail = await importPipelineApplication(payload);
      setImportNotice(
        created?.name ? `${DEV_MSG.PIPE_IMPORTED} ${created.name}` : DEV_MSG.PIPE_IMPORTED,
      );
      setImportXml("");
      setImportFileName("");
      setImportName("");
      await refresh();
    } catch (e: unknown) {
      setImportError(pipeImportErrMsg(e));
    } finally {
      setImportBusy(false);
    }
  }

  const importSection =
    isAdmin ? (
      <section
        data-testid="developer-pipe-import"
        style={{
          marginBottom: "16px",
          padding: "12px",
          border: `1px solid ${catalogColors.softBorder}`,
          borderRadius: "4px",
        }}
      >
        <h3 style={{ fontSize: "1rem", margin: "0 0 4px" }}>{DEV_MSG.PIPE_IMPORT_SECTION}</h3>
        <p style={{ color: catalogColors.muted, fontSize: "0.9rem", margin: "0 0 8px" }}>
          {DEV_MSG.PIPE_IMPORT_HINT}
        </p>
        <div style={{ display: "flex", flexWrap: "wrap", gap: "8px", alignItems: "center" }}>
          <label style={{ fontSize: "0.9rem" }}>
            {DEV_MSG.PIPE_IMPORT_FILE}:{" "}
            <input
              type="file"
              accept=".xml,application/xml,text/xml"
              data-testid="developer-pipe-import-file"
              disabled={importBusy}
              onChange={(e) => void onImportFileChange(e.target.files?.[0] ?? null)}
            />
          </label>
          <label style={{ fontSize: "0.9rem" }}>
            {DEV_MSG.PIPE_IMPORT_NAME}:{" "}
            <input
              type="text"
              data-testid="developer-pipe-import-name"
              placeholder={DEV_MSG.PIPE_IMPORT_NAME_PLACEHOLDER}
              value={importName}
              disabled={importBusy}
              onChange={(e) => setImportName(e.target.value)}
              style={{ padding: "6px", font: "inherit" }}
            />
          </label>
          <button
            type="button"
            data-testid="developer-pipe-import-submit"
            disabled={importBusy}
            onClick={() => void handleImport()}
            style={openButtonStyle}
          >
            {importBusy ? DEV_MSG.PIPE_IMPORTING : DEV_MSG.PIPE_IMPORT_SUBMIT}
          </button>
        </div>
        {importFileName ? (
          <p style={{ color: catalogColors.muted, fontSize: "0.85rem", margin: "8px 0 0" }}>
            {importFileName}
          </p>
        ) : null}
        {importNotice ? (
          <div role="status" data-testid="developer-pipe-import-notice" style={{ marginTop: "8px" }}>
            {importNotice}
          </div>
        ) : null}
        {importError ? (
          <div
            role="alert"
            data-testid="developer-pipe-import-error"
            style={{ ...errorAlert, marginTop: "8px" }}
          >
            {importError}
          </div>
        ) : null}
      </section>
    ) : null;

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.name || "").localeCompare(b.name || "", undefined, { sensitivity: "base" }),
    );
  }, [items]);

  if (selected) {
    return (
      <PipelineDetailPanel idOrName={selected} onBack={() => setSelected(null)} />
    );
  }

  if (error && (items == null || items.length === 0))
    return (
      <CatalogStatus testId="developer-pipe-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return (
      <CatalogStatus testId="developer-pipe-loading">{DEV_MSG.PIPE_LOADING}</CatalogStatus>
    );
  if (items.length === 0)
    return (
      <div data-testid="developer-pipe-panel">
        {importSection}
        <CatalogStatus testId="developer-pipe-empty">{DEV_MSG.PIPE_EMPTY}</CatalogStatus>
      </div>
    );

  return (
    <div data-testid="developer-pipe-panel">
      {importSection}
      <CatalogHint>{DEV_MSG.PIPE_HINT}</CatalogHint>
      <SimpleCatalogTable
        tableTestId="developer-pipe-table"
        rowTestId="developer-pipe-row"
        columns={[
          DEV_MSG.PIPE_COL_NAME,
          DEV_MSG.PIPE_COL_ID,
          DEV_MSG.PIPE_COL_TYPE,
          DEV_MSG.PIPE_COL_ENABLED,
          DEV_MSG.PIPE_COL_ROOT,
          DEV_MSG.PIPE_COL_DESCRIPTION,
        ]}
        rows={sorted.map((app, index) => {
          const openKey = app.name || (app.id != null ? String(app.id) : "");
          const interactive = openKey.length > 0;
          return {
            key: String(app.id ?? app.name ?? `pipe-${index}`),
            onClick: interactive ? () => setSelected(openKey) : undefined,
            cells: [
              interactive ? (
                <button
                  key="open"
                  type="button"
                  data-testid="developer-pipe-open"
                  data-pipe-name={app.name || openKey}
                  aria-label={`Open ${app.name || openKey}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    setSelected(openKey);
                  }}
                  style={{ ...openButtonStyle, fontFamily: "monospace" }}
                >
                  {app.name || "—"}
                </button>
              ) : (
                <span key="n" style={monoCell}>
                  {app.name || "—"}
                </span>
              ),
              <span key="i" style={monoCell}>
                {app.id != null ? String(app.id) : "—"}
              </span>,
              app.appType || "—",
              app.enabled == null ? "—" : app.enabled ? DEV_MSG.YES : DEV_MSG.NO,
              <span key="r" style={mutedMonoCell}>
                {app.appRoot || ""}
              </span>,
              <span key="d" style={mutedCell}>
                {app.description || ""}
              </span>,
            ],
          };
        })}
      />
    </div>
  );
}
