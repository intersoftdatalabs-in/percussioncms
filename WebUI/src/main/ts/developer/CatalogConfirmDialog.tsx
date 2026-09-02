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
import { catalogColors } from "./catalogStyles";
import { DEV_MSG } from "./messages";

export interface CatalogConfirmDialogProps {
  open: boolean;
  busy: boolean;
  /** Destructive confirm sentence (catalog-specific). */
  message: string;
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
 * In-app confirm for Developer catalog deletes (508/WCAG).
 * Peer: Design {@code DeleteTemplateDialog} + {@code useDialogEscape}.
 */
export function CatalogConfirmDialog({
  open,
  busy,
  message,
  onCancel,
  onConfirm,
}: CatalogConfirmDialogProps): React.ReactElement | null {
  useDialogEscape(open, busy, onCancel);

  if (!open) {
    return null;
  }

  return (
    <div
      style={overlayStyle}
      data-testid="developer-catalog-confirm-dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="developer-catalog-confirm-title"
      aria-describedby="developer-catalog-confirm-hint developer-catalog-confirm-body"
    >
      <div style={panelStyle}>
        <h2 id="developer-catalog-confirm-title" style={{ marginTop: 0 }}>
          {DEV_MSG.CATALOG_CONFIRM_TITLE}
        </h2>
        <p
          id="developer-catalog-confirm-hint"
          style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
        >
          {DEV_MSG.CATALOG_CONFIRM_HINT}
        </p>
        <p id="developer-catalog-confirm-body" data-testid="developer-catalog-confirm-body">
          {message}
        </p>
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <button
            type="button"
            data-testid="developer-catalog-confirm-cancel"
            disabled={busy}
            onClick={onCancel}
          >
            {DEV_MSG.CATALOG_CONFIRM_CANCEL}
          </button>
          <button
            type="button"
            data-testid="developer-catalog-confirm-submit"
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
            {DEV_MSG.CATALOG_CONFIRM_SUBMIT}
          </button>
        </div>
      </div>
    </div>
  );
}
