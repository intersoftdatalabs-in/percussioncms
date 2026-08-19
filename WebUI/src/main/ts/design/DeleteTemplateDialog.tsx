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

import React from "react";
import { useDialogEscape } from "../architecture/useDialogEscape";
import { catalogColors } from "../developer/catalogStyles";
import { DESIGN_MSG } from "./messages";

export interface DeleteTemplateDialogProps {
  open: boolean;
  busy: boolean;
  error: string | null;
  /** Operator-facing label or name shown in the confirm sentence. */
  label: string;
  onCancel: () => void;
  onConfirm: () => void;
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

/**
 * Confirm delete of a modern assembly template (no Widget XML).
 */
export function DeleteTemplateDialog({
  open,
  busy,
  error,
  label,
  onCancel,
  onConfirm,
}: DeleteTemplateDialogProps): React.ReactElement | null {
  useDialogEscape(open, busy, onCancel);

  if (!open) {
    return null;
  }

  return (
    <div
      style={overlayStyle}
      data-testid="design-tpl-delete-dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="design-tpl-delete-title"
      aria-describedby="design-tpl-delete-hint"
    >
      <div style={panelStyle}>
        <h2 id="design-tpl-delete-title" style={{ marginTop: 0 }}>
          {DESIGN_MSG.TPL_DELETE_TITLE}
        </h2>
        <p
          id="design-tpl-delete-hint"
          style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
        >
          {DESIGN_MSG.TPL_DELETE_HINT}
        </p>
        <p data-testid="design-tpl-delete-confirm">
          {DESIGN_MSG.TPL_DELETE_CONFIRM.replace("{0}", label)}
        </p>
        {error ? (
          <div
            role="alert"
            data-testid="design-tpl-delete-error"
            style={{ color: catalogColors.error, marginBottom: 12 }}
          >
            {error}
          </div>
        ) : null}
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <button
            type="button"
            data-testid="design-tpl-delete-cancel"
            disabled={busy}
            onClick={onCancel}
          >
            {DESIGN_MSG.TPL_DELETE_CANCEL}
          </button>
          <button
            type="button"
            data-testid="design-tpl-delete-submit"
            disabled={busy}
            onClick={onConfirm}
            style={{
              background: catalogColors.error,
              color: "#fff",
              border: "none",
              borderRadius: 4,
              padding: "6px 12px",
              cursor: busy ? "not-allowed" : "pointer",
              font: "inherit",
            }}
          >
            {DESIGN_MSG.TPL_DELETE_SUBMIT}
          </button>
        </div>
      </div>
    </div>
  );
}
