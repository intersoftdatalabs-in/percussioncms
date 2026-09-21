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
import {
  emptyInitParamRow,
  type InitParamRow,
} from "../api/developer/extensionInitParams";
import { catalogColors } from "./catalogStyles";
import { DEV_MSG } from "./messages";

export interface ExtensionInitParamDialogProps {
  open: boolean;
  readOnly: boolean;
  rows: InitParamRow[];
  onCancel: () => void;
  onApply: (rows: InitParamRow[]) => void;
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
  maxWidth: 640,
  width: "100%",
  padding: "1.25rem 1.5rem",
  boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
  maxHeight: "80vh",
  overflow: "auto",
};

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
};

/**
 * Workbench-parity init-parameter dialog (name/value pairs beyond className).
 */
export function ExtensionInitParamDialog({
  open,
  readOnly,
  rows,
  onCancel,
  onApply,
}: ExtensionInitParamDialogProps): React.ReactElement | null {
  const [draft, setDraft] = useState<InitParamRow[]>(rows);
  useDialogEscape(open, false, onCancel);

  useEffect(() => {
    if (open) {
      setDraft(rows.map((r) => ({ key: r.key, value: r.value })));
    }
  }, [open, rows]);

  if (!open) {
    return null;
  }

  return (
    <div
      style={overlayStyle}
      data-testid="developer-ex-init-dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="developer-ex-init-dialog-title"
    >
      <div style={panelStyle}>
        <h2 id="developer-ex-init-dialog-title" style={{ marginTop: 0 }}>
          {DEV_MSG.EX_INIT_DIALOG_TITLE}
        </h2>
        <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
          {DEV_MSG.EX_INIT_DIALOG_HINT}
        </p>
        {draft.length === 0 ? (
          <p data-testid="developer-ex-init-dialog-empty" style={{ color: catalogColors.empty }}>
            {DEV_MSG.EX_INIT_EMPTY}
          </p>
        ) : (
          draft.map((row, i) => (
            <div
              key={`init-${i}`}
              data-testid={`developer-ex-init-row-${i}`}
              style={{ display: "flex", gap: "8px", marginBottom: "8px", flexWrap: "wrap" }}
            >
              <input
                data-testid={`developer-ex-init-key-${i}`}
                aria-label={DEV_MSG.EX_INIT_KEY}
                style={{ ...inputStyle, fontFamily: "monospace", flex: "1 1 10rem" }}
                value={row.key}
                disabled={readOnly}
                onChange={(e) =>
                  setDraft((rows) =>
                    rows.map((r, j) => (j === i ? { ...r, key: e.target.value } : r)),
                  )
                }
                autoComplete="off"
              />
              <input
                data-testid={`developer-ex-init-value-${i}`}
                aria-label={DEV_MSG.EX_INIT_VALUE}
                style={{ ...inputStyle, fontFamily: "monospace", flex: "2 1 12rem" }}
                value={row.value}
                disabled={readOnly}
                onChange={(e) =>
                  setDraft((rows) =>
                    rows.map((r, j) => (j === i ? { ...r, value: e.target.value } : r)),
                  )
                }
                autoComplete="off"
              />
              <button
                type="button"
                data-testid={`developer-ex-init-remove-${i}`}
                disabled={readOnly}
                onClick={() => setDraft((rows) => rows.filter((_, j) => j !== i))}
                style={{
                  padding: "8px",
                  border: `1px solid ${catalogColors.softBorder}`,
                  borderRadius: "4px",
                  background: "transparent",
                  cursor: readOnly ? "not-allowed" : "pointer",
                }}
              >
                {DEV_MSG.EX_INIT_REMOVE}
              </button>
            </div>
          ))
        )}
        <button
          type="button"
          data-testid="developer-ex-init-add"
          disabled={readOnly}
          onClick={() => setDraft((rows) => [...rows, emptyInitParamRow()])}
          style={{
            padding: "6px 12px",
            marginBottom: "16px",
            border: `1px solid ${catalogColors.softBorder}`,
            borderRadius: "4px",
            background: "transparent",
            cursor: readOnly ? "not-allowed" : "pointer",
          }}
        >
          {DEV_MSG.EX_INIT_ADD}
        </button>
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <button
            type="button"
            data-testid="developer-ex-init-cancel"
            onClick={onCancel}
          >
            {DEV_MSG.EX_INIT_CANCEL}
          </button>
          <button
            type="button"
            data-testid="developer-ex-init-apply"
            disabled={readOnly}
            onClick={() => onApply(draft)}
            style={{
              background: readOnly ? catalogColors.disabled : catalogColors.accent,
              color: "#fff",
              border: "none",
              borderRadius: 4,
              padding: "6px 12px",
              cursor: readOnly ? "not-allowed" : "pointer",
              font: "inherit",
            }}
          >
            {DEV_MSG.EX_INIT_APPLY}
          </button>
        </div>
      </div>
    </div>
  );
}
