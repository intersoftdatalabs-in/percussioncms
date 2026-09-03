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
  CONTROL_CHOICE_SETS,
  CONTROL_DIMENSIONS,
  createControl,
  isControlCreateReady,
  normalizeControlName,
  type ControlCreateBody,
} from "../api/developer/controlsApi";
import type { ControlDef } from "../api/developer/types";
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
      return DEV_MSG.CTL_DUPLICATE;
    }
    if (err.status === 400) {
      return DEV_MSG.CTL_INVALID_NAME;
    }
    if (err.status === 403) {
      return DEV_MSG.CTL_FORBIDDEN;
    }
  }
  return DEV_MSG.CTL_CREATE_ERROR;
}

/**
 * UI-01 create chrome: POST /services/cecontrols (name required; unique, no spaces/wildcards).
 */
export function ControlCreatePanel({
  onBack,
  onCreated,
}: {
  onBack: () => void;
  onCreated?: (detail: ControlDef) => void | Promise<void>;
}): React.ReactElement {
  const [name, setName] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [description, setDescription] = useState("");
  const [dimension, setDimension] = useState("");
  const [choiceSet, setChoiceSet] = useState("");
  const [xslSource, setXslSource] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [createdName, setCreatedName] = useState<string | null>(null);
  const inflight = useRef(false);

  const nameLocked = createdName != null;
  const canSave = !busy && !nameLocked && isControlCreateReady({ name, dimension, choiceSet });

  function writeBody(): ControlCreateBody {
    const body: ControlCreateBody = {
      name: normalizeControlName(name),
    };
    const trimmedDisplay = displayName.trim();
    if (trimmedDisplay) {
      body.displayName = trimmedDisplay;
    }
    if (description.trim()) {
      body.description = description;
    }
    if (dimension.trim()) {
      body.dimension = dimension.trim().toLowerCase();
    }
    if (choiceSet.trim()) {
      body.choiceSet = choiceSet.trim().toLowerCase();
    }
    if (xslSource.trim()) {
      body.xslSource = xslSource;
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
      const saved = await createControl(writeBody());
      const savedName = (saved.name || normalizeControlName(name)).trim();
      setCreatedName(savedName);
      setNotice(DEV_MSG.CTL_CREATED);
      await onCreated?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, createErrorFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  return (
    <div data-testid="developer-ctl-create">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-ctl-create-back"
        aria-label={DEV_MSG.CTL_BACK}
        style={backButton}
      >
        ← {DEV_MSG.CTL_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-ctl-create-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid="developer-ctl-create-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      <header style={{ marginBottom: "16px" }}>
        <h2 style={{ margin: "0 0 4px" }} data-testid="developer-ctl-create-title">
          {DEV_MSG.CTL_NEW}
        </h2>
      </header>

      <div style={fieldStyle}>
        <label htmlFor="ctl-create-name">{DEV_MSG.CTL_FORM_NAME}</label>
        <input
          id="ctl-create-name"
          data-testid="developer-ctl-create-name"
          style={{ ...inputStyle, fontFamily: "monospace" }}
          value={name}
          disabled={busy || nameLocked}
          readOnly={nameLocked}
          onChange={(e) => setName(e.target.value)}
          autoComplete="off"
        />
        <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
          {DEV_MSG.CTL_NAME_HINT}
        </span>
      </div>
      <div style={fieldStyle}>
        <label htmlFor="ctl-create-display">{DEV_MSG.CTL_FORM_DISPLAY}</label>
        <input
          id="ctl-create-display"
          data-testid="developer-ctl-create-display"
          style={inputStyle}
          value={displayName}
          disabled={busy || nameLocked}
          onChange={(e) => setDisplayName(e.target.value)}
        />
      </div>
      <div style={fieldStyle}>
        <label htmlFor="ctl-create-desc">{DEV_MSG.CTL_FORM_DESCRIPTION}</label>
        <input
          id="ctl-create-desc"
          data-testid="developer-ctl-create-description"
          style={inputStyle}
          value={description}
          disabled={busy || nameLocked}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>
      <div style={fieldStyle}>
        <label htmlFor="ctl-create-dim">{DEV_MSG.CTL_FORM_DIMENSION}</label>
        <select
          id="ctl-create-dim"
          data-testid="developer-ctl-create-dimension"
          style={inputStyle}
          value={dimension}
          disabled={busy || nameLocked}
          onChange={(e) => setDimension(e.target.value)}
        >
          <option value="">{DEV_MSG.CTL_DIM_DEFAULT}</option>
          {CONTROL_DIMENSIONS.map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
        </select>
      </div>
      <div style={fieldStyle}>
        <label htmlFor="ctl-create-choice">{DEV_MSG.CTL_FORM_CHOICESET}</label>
        <select
          id="ctl-create-choice"
          data-testid="developer-ctl-create-choiceset"
          style={inputStyle}
          value={choiceSet}
          disabled={busy || nameLocked}
          onChange={(e) => setChoiceSet(e.target.value)}
        >
          <option value="">{DEV_MSG.CTL_CHOICE_DEFAULT}</option>
          {CONTROL_CHOICE_SETS.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>
      <div style={fieldStyle}>
        <label htmlFor="ctl-create-xsl">{DEV_MSG.CTL_FORM_XSL}</label>
        <textarea
          id="ctl-create-xsl"
          data-testid="developer-ctl-create-xsl"
          style={{ ...inputStyle, fontFamily: "monospace", minHeight: "96px" }}
          value={xslSource}
          disabled={busy || nameLocked}
          onChange={(e) => setXslSource(e.target.value)}
          spellCheck={false}
        />
        <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
          {DEV_MSG.CTL_XSL_HINT}
        </span>
      </div>

      <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
        <button
          type="button"
          data-testid="developer-ctl-create-save"
          aria-label={DEV_MSG.CTL_CREATE_SAVE}
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
          {DEV_MSG.CTL_CREATE_SAVE}
        </button>
        <button
          type="button"
          data-testid="developer-ctl-create-cancel"
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
          {DEV_MSG.CTL_CANCEL}
        </button>
      </div>
    </div>
  );
}
