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
 * Section tree picker dialog for Architecture section-link targets (#3097).
 * Peer: legacy PercSectionTreeDialog.
 */

import React, { useEffect, useState } from "react";
import type { NavTreeNode } from "../api/architecture/types";
import { catalogColors } from "../developer/catalogStyles";
import { NavTree } from "./NavTree";
import { ARCH_MSG } from "./messages";
import { useDialogEscape } from "./useDialogEscape";

export interface SectionTreePickerDialogProps {
  open: boolean;
  root: NavTreeNode | null;
  /** Optional id that should not be choosable (e.g. parent). */
  excludeId?: string | null;
  busy?: boolean;
  title?: string;
  onCancel: () => void;
  onConfirm: (node: NavTreeNode) => void;
}

const overlayStyle: React.CSSProperties = {
  position: "fixed",
  inset: 0,
  background: "rgba(15, 23, 42, 0.45)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  zIndex: 1100,
  padding: 16,
};

const panelStyle: React.CSSProperties = {
  background: "#fff",
  borderRadius: 8,
  border: `1px solid ${catalogColors.headerBorder}`,
  maxWidth: 520,
  width: "100%",
  padding: "1.25rem 1.5rem",
  boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
  maxHeight: "90vh",
  overflow: "auto",
};

export function SectionTreePickerDialog({
  open,
  root,
  excludeId = null,
  busy = false,
  title = ARCH_MSG.TREE_PICKER_TITLE,
  onCancel,
  onConfirm,
}: SectionTreePickerDialogProps): React.ReactElement | null {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [localError, setLocalError] = useState<string | null>(null);

  useDialogEscape(open, busy, onCancel);

  useEffect(() => {
    if (open) {
      setSelectedId(null);
      setLocalError(null);
    }
  }, [open]);

  if (!open) {
    return null;
  }

  const handleConfirm = () => {
    if (!selectedId || !root) {
      setLocalError(ARCH_MSG.SECTION_LINK_NO_TARGET);
      return;
    }
    if (excludeId && selectedId === excludeId) {
      setLocalError(ARCH_MSG.SECTION_LINK_INVALID_TARGET);
      return;
    }
    const walk = (n: NavTreeNode): NavTreeNode | null => {
      if (n.id === selectedId) return n;
      for (const c of n.children) {
        const f = walk(c);
        if (f) return f;
      }
      return null;
    };
    const node = walk(root);
    if (!node) {
      setLocalError(ARCH_MSG.SECTION_LINK_NO_TARGET);
      return;
    }
    onConfirm(node);
  };

  return (
    <div
      style={overlayStyle}
      role="presentation"
      data-testid="architecture-tree-picker-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onCancel();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="architecture-tree-picker-title"
        data-testid="architecture-tree-picker-dialog"
        style={panelStyle}
      >
        <h2
          id="architecture-tree-picker-title"
          style={{ marginTop: 0, marginBottom: 8, fontSize: "1.15rem" }}
        >
          {title}
        </h2>
        <p
          style={{
            margin: "0 0 12px",
            color: catalogColors.muted,
            fontSize: "0.9rem",
          }}
        >
          {ARCH_MSG.TREE_PICKER_HINT}
        </p>
        <NavTree
          root={root}
          selectedId={selectedId}
          onSelect={(node) => {
            setSelectedId(node.id);
            setLocalError(null);
          }}
        />
        {localError ? (
          <p
            role="alert"
            data-testid="architecture-tree-picker-error"
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
            marginTop: 16,
          }}
        >
          <button
            type="button"
            data-testid="architecture-tree-picker-cancel"
            disabled={busy}
            onClick={onCancel}
            style={{
              padding: "0.4rem 0.85rem",
              border: `1px solid ${catalogColors.softBorder}`,
              borderRadius: 4,
              background: "#fff",
              cursor: busy ? "not-allowed" : "pointer",
            }}
          >
            {ARCH_MSG.TREE_PICKER_CANCEL}
          </button>
          <button
            type="button"
            data-testid="architecture-tree-picker-confirm"
            disabled={busy || !selectedId}
            onClick={handleConfirm}
            style={{
              padding: "0.4rem 0.85rem",
              border: `1px solid ${catalogColors.accent}`,
              borderRadius: 4,
              background: selectedId && !busy ? catalogColors.accent : "#f0f0f0",
              color: selectedId && !busy ? "#fff" : "#999",
              cursor: selectedId && !busy ? "pointer" : "not-allowed",
            }}
          >
            {ARCH_MSG.TREE_PICKER_CONFIRM}
          </button>
        </div>
      </div>
    </div>
  );
}

export default SectionTreePickerDialog;
