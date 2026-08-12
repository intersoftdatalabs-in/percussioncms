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

/**
 * Rename section dialog for Architecture (#3096).
 */

import React, { useEffect, useState } from "react";
import { catalogColors } from "../developer/catalogStyles";
import { validateSectionTitle } from "../api/architecture/sectionMutations";
import { ARCH_MSG } from "./messages";

export interface RenameSectionDialogProps {
  open: boolean;
  initialTitle: string;
  busy: boolean;
  onCancel: () => void;
  onSubmit: (title: string) => void;
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
  maxWidth: 400,
  width: "100%",
  padding: "1.25rem 1.5rem",
  boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
};

export function RenameSectionDialog({
  open,
  initialTitle,
  busy,
  onCancel,
  onSubmit,
}: RenameSectionDialogProps): React.ReactElement | null {
  const [title, setTitle] = useState(initialTitle);
  const [localError, setLocalError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setTitle(initialTitle);
      setLocalError(null);
    }
  }, [open, initialTitle]);

  if (!open) {
    return null;
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const err = validateSectionTitle(title);
    if (err) {
      setLocalError(err);
      return;
    }
    onSubmit(title.trim());
  };

  return (
    <div
      style={overlayStyle}
      data-testid="architecture-rename-dialog"
      role="presentation"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onCancel();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="architecture-rename-title"
        style={panelStyle}
        onClick={(e) => e.stopPropagation()}
      >
        <h2
          id="architecture-rename-title"
          style={{ marginTop: 0, marginBottom: 12, fontSize: "1.1rem" }}
        >
          {ARCH_MSG.RENAME_DIALOG_TITLE}
        </h2>
        <form onSubmit={handleSubmit}>
          <label style={{ display: "block", fontSize: "0.9rem" }}>
            {ARCH_MSG.RENAME_TITLE_LABEL}
            <input
              data-testid="architecture-rename-title-input"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              disabled={busy}
              autoFocus
              style={{
                display: "block",
                width: "100%",
                marginTop: 4,
                marginBottom: 12,
                padding: "0.4rem 0.5rem",
                fontSize: "0.95rem",
                boxSizing: "border-box",
              }}
            />
          </label>
          {localError ? (
            <p
              role="alert"
              data-testid="architecture-rename-error"
              style={{ color: catalogColors.error, fontSize: "0.9rem" }}
            >
              {localError}
            </p>
          ) : null}
          <div
            style={{
              display: "flex",
              justifyContent: "flex-end",
              gap: 8,
              marginTop: 8,
            }}
          >
            <button
              type="button"
              data-testid="architecture-rename-cancel"
              onClick={onCancel}
              disabled={busy}
              style={{
                padding: "0.4rem 0.85rem",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: 4,
                background: "#fff",
                cursor: busy ? "not-allowed" : "pointer",
              }}
            >
              {ARCH_MSG.RENAME_CANCEL}
            </button>
            <button
              type="submit"
              data-testid="architecture-rename-submit"
              disabled={busy}
              style={{
                padding: "0.4rem 0.85rem",
                border: `1px solid ${catalogColors.accent}`,
                borderRadius: 4,
                background: catalogColors.accent,
                color: "#fff",
                cursor: busy ? "not-allowed" : "pointer",
              }}
            >
              {busy ? ARCH_MSG.ACTION_BUSY : ARCH_MSG.RENAME_SUBMIT}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default RenameSectionDialog;
