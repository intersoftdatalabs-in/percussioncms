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

import React, { useState } from "react";
import { isApiError } from "../api/client";
import {
  contentTypeNameFromDesignXml,
  importContentType,
  invalidContentTypeImportName,
  rewriteContentTypeDesignXmlName,
} from "../api/developer/contentTypeImportExport";
import type { ContentTypeDetail } from "../api/developer/types";
import { catalogColors } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  boxSizing: "border-box",
};

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

function importErrMsg(err: unknown): string {
  if (isApiError(err) && err.status === 400) {
    return panelErrMsg(err, DEV_MSG.CT_IMPORT_INVALID);
  }
  if (isApiError(err) && err.status === 409) {
    return panelErrMsg(err, DEV_MSG.CT_IMPORT_DUPLICATE);
  }
  if (
    err instanceof Error &&
    /invalid content-type design XML|missing name|design XML is required/i.test(err.message)
  ) {
    return panelErrMsg(err, DEV_MSG.CT_IMPORT_INVALID);
  }
  return panelErrMsg(err, DEV_MSG.CT_IMPORT_ERROR);
}

/**
 * Catalog CD-14 create-only import: file picker + unique name (no overwrite).
 */
export function ContentTypeImportWizard({
  onImported,
}: {
  onImported?: (detail: ContentTypeDetail) => void;
}): React.ReactElement {
  const [xml, setXml] = useState("");
  const [fileName, setFileName] = useState("");
  const [uniqueName, setUniqueName] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  async function onFileChange(file: File | null) {
    setError(null);
    setNotice(null);
    if (!file) {
      setXml("");
      setFileName("");
      return;
    }
    const text = await readFileText(file);
    setXml(text);
    setFileName(file.name);
    const fromXml = contentTypeNameFromDesignXml(text);
    if (fromXml && !uniqueName.trim()) {
      setUniqueName(fromXml);
    }
  }

  async function handleImport() {
    setError(null);
    setNotice(null);
    const body = xml.trim();
    if (!body) {
      setError(DEV_MSG.CT_IMPORT_NO_FILE);
      return;
    }
    const name = uniqueName.trim();
    if (name) {
      const invalid = invalidContentTypeImportName(name);
      if (invalid) {
        setError(DEV_MSG.CT_IMPORT_BAD_NAME);
        return;
      }
    }
    setBusy(true);
    try {
      const payload = name ? rewriteContentTypeDesignXmlName(body, name) : body;
      const created = await importContentType(payload);
      setNotice(DEV_MSG.CT_IMPORTED);
      onImported?.(created);
    } catch (err: unknown) {
      setError(importErrMsg(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section
      data-testid="developer-ct-import"
      aria-label={DEV_MSG.CT_IMPORT}
      style={{
        marginBottom: "16px",
        padding: "12px",
        border: `1px solid ${catalogColors.softBorder}`,
        borderRadius: "4px",
      }}
    >
      <p style={{ margin: "0 0 8px", color: catalogColors.muted, fontSize: "0.9rem" }}>
        {DEV_MSG.CT_IMPORT_HINT}
      </p>
      {error ? (
        <div
          role="alert"
          data-testid="developer-ct-import-error"
          style={{ color: catalogColors.error, marginBottom: "8px" }}
        >
          {error}
        </div>
      ) : null}
      {notice ? (
        <div data-testid="developer-ct-import-notice" style={{ color: "#276749", marginBottom: "8px" }}>
          {notice}
        </div>
      ) : null}
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          gap: "8px",
          alignItems: "center",
        }}
      >
        <label style={{ display: "flex", flexDirection: "column", gap: "4px", fontSize: "0.9rem" }}>
          {DEV_MSG.CT_IMPORT_FILE}
          <input
            type="file"
            accept=".xml,application/xml,text/xml"
            data-testid="developer-ct-import-file"
            aria-label={DEV_MSG.CT_IMPORT_FILE}
            disabled={busy}
            onChange={(e) => {
              const file = e.target.files?.[0] ?? null;
              void onFileChange(file);
            }}
          />
        </label>
        <label style={{ display: "flex", flexDirection: "column", gap: "4px", fontSize: "0.9rem" }}>
          {DEV_MSG.CT_IMPORT_NAME}
          <input
            type="text"
            value={uniqueName}
            data-testid="developer-ct-import-name"
            aria-label={DEV_MSG.CT_IMPORT_NAME}
            placeholder={DEV_MSG.CT_IMPORT_NAME_PLACEHOLDER}
            disabled={busy}
            onChange={(e) => setUniqueName(e.target.value)}
            style={{ ...inputStyle, minWidth: "16rem" }}
          />
        </label>
        <button
          type="button"
          data-testid="developer-ct-import-submit"
          aria-label={DEV_MSG.CT_IMPORT_SUBMIT}
          disabled={busy}
          onClick={() => void handleImport()}
          style={{
            padding: "8px 16px",
            background: busy ? catalogColors.disabled : catalogColors.accent,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: busy ? "not-allowed" : "pointer",
            alignSelf: "flex-end",
          }}
        >
          {busy ? DEV_MSG.CT_IMPORTING : DEV_MSG.CT_IMPORT_SUBMIT}
        </button>
        {fileName ? (
          <span data-testid="developer-ct-import-filename" style={{ fontSize: "0.85rem", color: catalogColors.muted }}>
            {fileName}
          </span>
        ) : null}
      </div>
    </section>
  );
}
