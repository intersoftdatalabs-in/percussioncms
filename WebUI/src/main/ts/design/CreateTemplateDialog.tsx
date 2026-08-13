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

import React, { useEffect, useState } from "react";
import { useDialogEscape } from "../architecture/useDialogEscape";
import { catalogColors } from "../developer/catalogStyles";
import {
  ASSEMBLER_OPTIONS,
  DEFAULT_CREATE_ASSEMBLER,
} from "./assemblerOptions";
import { DESIGN_MSG } from "./messages";
import { validateTemplateCreateInput } from "./templateCreate";

export interface CreateTemplateDialogProps {
  open: boolean;
  busy: boolean;
  error: string | null;
  onCancel: () => void;
  onSubmit: (input: {
    name: string;
    label: string;
    description: string;
    assembler: string;
  }) => void;
}

const overlayStyle: React.CSSProperties = {
  position: "fixed",
  inset: 0,
  background: "rgba(15, 23, 42, 0.45)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  zIndex: 1000,
  padding: 16,
};

const panelStyle: React.CSSProperties = {
  background: "#fff",
  borderRadius: 8,
  border: `1px solid ${catalogColors.headerBorder}`,
  maxWidth: 480,
  width: "100%",
  padding: "1.25rem 1.5rem",
  boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
};

const fieldStyle: React.CSSProperties = {
  display: "block",
  width: "100%",
  marginTop: 4,
  marginBottom: 12,
  padding: "0.4rem 0.5rem",
  fontSize: "0.95rem",
  boxSizing: "border-box",
};

/**
 * Design SPA create-template dialog (#3305). Persists via POST /services/templates.
 */
export function CreateTemplateDialog({
  open,
  busy,
  error,
  onCancel,
  onSubmit,
}: CreateTemplateDialogProps): React.ReactElement | null {
  const [name, setName] = useState("");
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [assembler, setAssembler] = useState(DEFAULT_CREATE_ASSEMBLER);
  const [localError, setLocalError] = useState<string | null>(null);

  useDialogEscape(open, busy, onCancel);

  useEffect(() => {
    if (!open) {
      return;
    }
    setName("");
    setLabel("");
    setDescription("");
    setAssembler(DEFAULT_CREATE_ASSEMBLER);
    setLocalError(null);
  }, [open]);

  if (!open) {
    return null;
  }

  const shownError = localError || error;

  return (
    <div
      style={overlayStyle}
      data-testid="design-tpl-create-dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="design-tpl-create-title"
      aria-describedby="design-tpl-create-hint"
    >
      <div style={panelStyle}>
        <h2 id="design-tpl-create-title" style={{ marginTop: 0 }}>
          {DESIGN_MSG.TPL_CREATE_TITLE}
        </h2>
        <p
          id="design-tpl-create-hint"
          style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
        >
          {DESIGN_MSG.TPL_CREATE_HINT}
        </p>
        <label htmlFor="design-tpl-create-name">
          {DESIGN_MSG.TPL_CREATE_NAME}
        </label>
        <input
          id="design-tpl-create-name"
          data-testid="design-tpl-create-name"
          style={fieldStyle}
          value={name}
          disabled={busy}
          autoComplete="off"
          onChange={(e) => setName(e.target.value)}
        />
        <label htmlFor="design-tpl-create-label">
          {DESIGN_MSG.TPL_CREATE_LABEL}
        </label>
        <input
          id="design-tpl-create-label"
          data-testid="design-tpl-create-label"
          style={fieldStyle}
          value={label}
          disabled={busy}
          autoComplete="off"
          onChange={(e) => setLabel(e.target.value)}
        />
        <label htmlFor="design-tpl-create-description">
          {DESIGN_MSG.TPL_CREATE_DESCRIPTION}
        </label>
        <input
          id="design-tpl-create-description"
          data-testid="design-tpl-create-description"
          style={fieldStyle}
          value={description}
          disabled={busy}
          autoComplete="off"
          onChange={(e) => setDescription(e.target.value)}
        />
        <label htmlFor="design-tpl-create-assembler">
          {DESIGN_MSG.TPL_CREATE_ASSEMBLER}
        </label>
        <select
          id="design-tpl-create-assembler"
          data-testid="design-tpl-create-assembler"
          style={fieldStyle}
          value={assembler}
          disabled={busy}
          onChange={(e) => setAssembler(e.target.value)}
        >
          {ASSEMBLER_OPTIONS.map((o) => (
            <option key={o.value} value={o.value} title={o.hint}>
              {o.label}
              {o.recommended ? " ★" : ""}
            </option>
          ))}
        </select>
        {shownError ? (
          <div
            role="alert"
            data-testid="design-tpl-create-error"
            style={{ color: catalogColors.error, marginBottom: 12 }}
          >
            {shownError}
          </div>
        ) : null}
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <button
            type="button"
            data-testid="design-tpl-create-cancel"
            disabled={busy}
            onClick={onCancel}
          >
            {DESIGN_MSG.TPL_CREATE_CANCEL}
          </button>
          <button
            type="button"
            data-testid="design-tpl-create-submit"
            disabled={busy}
            onClick={() => {
              const v = validateTemplateCreateInput(name, assembler);
              if (!v.ok) {
                setLocalError(v.message);
                return;
              }
              setLocalError(null);
              onSubmit({
                name: v.name,
                label: label.trim(),
                description: description.trim(),
                assembler,
              });
            }}
          >
            {DESIGN_MSG.TPL_CREATE_SUBMIT}
          </button>
        </div>
      </div>
    </div>
  );
}
