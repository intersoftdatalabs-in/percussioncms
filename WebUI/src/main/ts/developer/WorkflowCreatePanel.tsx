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
  createWorkflow,
  DEFAULT_WORKFLOW_TEMPLATE_STEPS,
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

function createErrorFallback(err: unknown): string {
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
  }
  return DEV_MSG.WF_CREATE_ERROR;
}

/**
 * Slice 21 create chrome: POST /services/workflows (name required; unique,
 * workflow-admin characters; description optional, stored on the new workflow).
 */
export function WorkflowCreatePanel({
  onBack,
  onCreated,
}: {
  onBack: () => void;
  onCreated?: (created: WorkflowCreateResult) => void;
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

  async function handleCreate(): Promise<void> {
    if (!canSave || inflight.current) {
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await createWorkflow(writeBody());
      setNotice(DEV_MSG.WF_CREATED);
      onCreated?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, createErrorFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  return (
    <div data-testid="developer-wf-create">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-wf-create-back"
        aria-label={DEV_MSG.WF_BACK}
        style={backButton}
      >
        ← {DEV_MSG.WF_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-wf-create-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid="developer-wf-create-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      <header style={{ marginBottom: "16px" }}>
        <h2 style={{ margin: "0 0 4px" }} data-testid="developer-wf-create-title">
          {DEV_MSG.WF_NEW}
        </h2>
      </header>

      <div style={fieldStyle}>
        <label htmlFor="wf-create-name">{DEV_MSG.WF_FORM_NAME}</label>
        <input
          id="wf-create-name"
          data-testid="developer-wf-create-name"
          style={{ ...inputStyle, fontFamily: "monospace" }}
          value={name}
          disabled={busy}
          onChange={(e) => setName(e.target.value)}
          autoComplete="off"
        />
        <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
          {DEV_MSG.WF_NAME_HINT}
        </span>
      </div>
      <div style={fieldStyle} data-testid="developer-wf-create-steps">
        <span>{DEV_MSG.WF_DEFAULT_STEPS}</span>
        <ul style={{ margin: "4px 0 0", paddingLeft: "1.25rem" }}>
          {DEFAULT_WORKFLOW_TEMPLATE_STEPS.map((step) => (
            <li key={step} data-testid="developer-wf-create-step">
              {step}
            </li>
          ))}
        </ul>
      </div>
      <div style={fieldStyle}>
        <label htmlFor="wf-create-desc">{DEV_MSG.WF_FORM_DESCRIPTION}</label>
        <input
          id="wf-create-desc"
          data-testid="developer-wf-create-description"
          style={inputStyle}
          value={description}
          disabled={busy}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>

      <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
        <button
          type="button"
          data-testid="developer-wf-create-save"
          aria-label={DEV_MSG.WF_CREATE_SAVE}
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
          {DEV_MSG.WF_CREATE_SAVE}
        </button>
        <button
          type="button"
          data-testid="developer-wf-create-cancel"
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
          {DEV_MSG.WF_CANCEL}
        </button>
      </div>
    </div>
  );
}
