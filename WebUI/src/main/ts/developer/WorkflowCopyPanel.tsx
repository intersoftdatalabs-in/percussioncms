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
  copyWorkflow,
  isWorkflowCreateReady,
  normalizeWorkflowName,
  type WorkflowCreateBody,
  type WorkflowCreateResult,
} from "../api/developer/workflowsApi";
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

function copyErrorFallback(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 409) {
      return DEV_MSG.WF_DUPLICATE;
    }
    if (err.status === 400) {
      return DEV_MSG.WF_INVALID_NAME;
    }
    if (err.status === 403) {
      return DEV_MSG.WF_FORBIDDEN;
    }
    if (err.status === 404) {
      return DEV_MSG.WF_NOT_FOUND;
    }
  }
  return DEV_MSG.WF_COPY_ERROR;
}

/**
 * Slice 36: POST /services/workflows/{source}/copy. A name collision stays on
 * this form and does not overwrite the existing workflow.
 */
export function WorkflowCopyPanel({
  sourceName,
  onBack,
  onCopied,
}: {
  sourceName: string;
  onBack: () => void;
  onCopied?: (created: WorkflowCreateResult) => void;
}): React.ReactElement {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const inflight = useRef(false);

  const canSave = !busy && isWorkflowCreateReady({ name });

  function writeBody(): WorkflowCreateBody {
    const body: WorkflowCreateBody = {
      name: normalizeWorkflowName(name),
    };
    if (description.trim()) {
      body.description = description.trim();
    }
    return body;
  }

  async function handleCopy(): Promise<void> {
    if (!canSave || inflight.current) {
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await copyWorkflow(sourceName, writeBody());
      setNotice(DEV_MSG.WF_COPIED);
      onCopied?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, copyErrorFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  return (
    <div data-testid="developer-wf-copy-panel">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-wf-copy-back"
        aria-label={DEV_MSG.WF_BACK}
        style={backButton}
      >
        ← {DEV_MSG.WF_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-wf-copy-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid="developer-wf-copy-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      <header style={{ marginBottom: "16px" }}>
        <h2 style={{ margin: "0 0 4px" }} data-testid="developer-wf-copy-title">
          {DEV_MSG.WF_COPY_TITLE}
        </h2>
        <p style={{ margin: 0, color: catalogColors.muted }} data-testid="developer-wf-copy-source">
          {DEV_MSG.WF_COPY_SOURCE}: {sourceName}
        </p>
        <p style={{ margin: "8px 0 0", color: catalogColors.muted }}>{DEV_MSG.WF_COPY_HINT}</p>
      </header>

      <div style={fieldStyle}>
        <label htmlFor="wf-copy-name">{DEV_MSG.WF_FORM_NAME}</label>
        <input
          id="wf-copy-name"
          data-testid="developer-wf-copy-name"
          style={{ ...inputStyle, fontFamily: "monospace" }}
          value={name}
          disabled={busy}
          onChange={(e) => setName(e.target.value)}
          autoComplete="off"
        />
      </div>
      <div style={fieldStyle}>
        <label htmlFor="wf-copy-desc">{DEV_MSG.WF_FORM_DESCRIPTION}</label>
        <input
          id="wf-copy-desc"
          data-testid="developer-wf-copy-description"
          style={inputStyle}
          value={description}
          disabled={busy}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>

      <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
        <button
          type="button"
          data-testid="developer-wf-copy-save"
          disabled={!canSave}
          onClick={() => {
            void handleCopy();
          }}
          style={{
            padding: "8px 14px",
            background: canSave ? catalogColors.accent : catalogColors.softBorder,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: canSave ? "pointer" : "default",
          }}
        >
          {busy ? DEV_MSG.WF_COPYING : DEV_MSG.WF_COPY_SAVE}
        </button>
        <button type="button" data-testid="developer-wf-copy-cancel" onClick={onBack} style={backButton}>
          {DEV_MSG.WF_CANCEL}
        </button>
      </div>
    </div>
  );
}
