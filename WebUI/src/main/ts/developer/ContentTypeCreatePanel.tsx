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

import React, { useRef, useState } from "react";
import { isApiError } from "../api/client";
import {
  createContentType,
  isContentTypeCreateReady,
  normalizeContentTypeName,
  type ContentTypeCreateBody,
} from "../api/developer/contentTypesApi";
import type { ContentTypeDetail } from "../api/developer/types";
import { backButton, catalogColors, errorAlert } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const fieldStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "4px",
  marginBottom: "12px",
};

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
};

function createErrorFallback(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 409) {
      return DEV_MSG.CT_DUPLICATE;
    }
    if (err.status === 400) {
      return DEV_MSG.CT_INVALID_NAME;
    }
    if (err.status === 403) {
      return DEV_MSG.CT_FORBIDDEN;
    }
  }
  return DEV_MSG.CT_CREATE_ERROR;
}

/**
 * CD-01 create chrome: POST /services/contenttypes (name required; unique, no spaces).
 */
export function ContentTypeCreatePanel({
  onBack,
  onCreated,
}: {
  onBack: () => void;
  onCreated?: (detail: ContentTypeDetail) => void;
}): React.ReactElement {
  const [name, setName] = useState("");
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [enabled, setEnabled] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const inflight = useRef(false);

  const canSave = !busy && isContentTypeCreateReady({ name });

  function writeBody(): ContentTypeCreateBody {
    const body: ContentTypeCreateBody = {
      name: normalizeContentTypeName(name),
      enabled,
    };
    const trimmedLabel = label.trim();
    if (trimmedLabel) {
      body.label = trimmedLabel;
    }
    if (description.trim()) {
      body.description = description;
    }
    return body;
  }

  async function handleCreate(): Promise<void> {
    if (!canSave || inflight.current) {
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await createContentType(writeBody());
      setNotice(DEV_MSG.CT_CREATED);
      onCreated?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, createErrorFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  return (
    <div data-testid="developer-ct-create">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-ct-create-back"
        aria-label={DEV_MSG.CT_BACK}
        style={backButton}
      >
        ← {DEV_MSG.CT_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-ct-create-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid="developer-ct-create-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      <header style={{ marginBottom: "16px" }}>
        <h2 style={{ margin: "0 0 4px" }} data-testid="developer-ct-create-title">
          {DEV_MSG.CT_NEW}
        </h2>
      </header>

      <div style={fieldStyle}>
        <label htmlFor="ct-create-name">{DEV_MSG.CT_FORM_NAME}</label>
        <input
          id="ct-create-name"
          data-testid="developer-ct-create-name"
          style={{ ...inputStyle, fontFamily: "monospace" }}
          value={name}
          disabled={busy}
          onChange={(e) => setName(e.target.value)}
          autoComplete="off"
        />
        <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
          {DEV_MSG.CT_NAME_HINT}
        </span>
      </div>
      <div style={fieldStyle}>
        <label htmlFor="ct-create-label">{DEV_MSG.CT_FORM_LABEL}</label>
        <input
          id="ct-create-label"
          data-testid="developer-ct-create-label"
          style={inputStyle}
          value={label}
          disabled={busy}
          onChange={(e) => setLabel(e.target.value)}
        />
      </div>
      <div style={fieldStyle}>
        <label htmlFor="ct-create-desc">{DEV_MSG.CT_FORM_DESCRIPTION}</label>
        <input
          id="ct-create-desc"
          data-testid="developer-ct-create-description"
          style={inputStyle}
          value={description}
          disabled={busy}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: "8px",
          marginBottom: "12px",
        }}
      >
        <input
          id="ct-create-enabled"
          type="checkbox"
          data-testid="developer-ct-create-enabled"
          checked={enabled}
          disabled={busy}
          onChange={(e) => setEnabled(e.target.checked)}
        />
        <label htmlFor="ct-create-enabled">{DEV_MSG.CT_FORM_ENABLED}</label>
      </div>

      <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
        <button
          type="button"
          data-testid="developer-ct-create-save"
          aria-label={DEV_MSG.CT_CREATE_SAVE}
          disabled={!canSave}
          onClick={() => void handleCreate()}
          style={{
            padding: "8px 16px",
            background: canSave ? catalogColors.accent : catalogColors.disabled,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: canSave ? "pointer" : "not-allowed",
          }}
        >
          {DEV_MSG.CT_CREATE_SAVE}
        </button>
        <button
          type="button"
          data-testid="developer-ct-create-cancel"
          disabled={busy}
          onClick={onBack}
          style={{
            padding: "8px 16px",
            background: "transparent",
            border: `1px solid ${catalogColors.softBorder}`,
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          {DEV_MSG.CT_CANCEL}
        </button>
      </div>
    </div>
  );
}
