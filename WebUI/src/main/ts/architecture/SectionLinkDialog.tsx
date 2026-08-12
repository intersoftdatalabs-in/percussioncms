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
 * Create / edit section-link dialog for Architecture (#3097).
 * Peer: PercEditSectionLinksDialog (section-link mode).
 */

import React, { useEffect, useState } from "react";
import {
  findNavNodeById,
  isValidSectionLinkTarget,
} from "../api/architecture/sectionMutations";
import type { NavTreeNode } from "../api/architecture/types";
import { catalogColors } from "../developer/catalogStyles";
import { SectionTreePickerDialog } from "./SectionTreePickerDialog";
import { ARCH_MSG } from "./messages";

export interface SectionLinkDialogProps {
  open: boolean;
  mode: "create" | "edit";
  /** Parent section that will host the link (create) or already hosts it (edit). */
  parentId: string;
  parentTitle: string;
  treeRoot: NavTreeNode | null;
  busy: boolean;
  /** Edit mode: current link section id (oldSectionId). */
  linkSectionId?: string | null;
  onCancel: () => void;
  onSubmit: (targetSectionId: string) => void;
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
  maxWidth: 440,
  width: "100%",
  padding: "1.25rem 1.5rem",
  boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
};

export function SectionLinkDialog({
  open,
  mode,
  parentId,
  parentTitle,
  treeRoot,
  busy,
  linkSectionId = null,
  onCancel,
  onSubmit,
}: SectionLinkDialogProps): React.ReactElement | null {
  const [targetId, setTargetId] = useState<string | null>(null);
  const [targetTitle, setTargetTitle] = useState("");
  const [pickerOpen, setPickerOpen] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setTargetId(null);
      setTargetTitle("");
      setPickerOpen(false);
      setLocalError(null);
    }
  }, [open]);

  if (!open) {
    return null;
  }

  const title =
    mode === "edit"
      ? ARCH_MSG.SECTION_LINK_EDIT_TITLE
      : ARCH_MSG.SECTION_LINK_DIALOG_TITLE;
  const submitLabel =
    mode === "edit"
      ? ARCH_MSG.SECTION_LINK_SAVE
      : ARCH_MSG.SECTION_LINK_SUBMIT;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetId) {
      setLocalError(ARCH_MSG.SECTION_LINK_NO_TARGET);
      return;
    }
    if (!isValidSectionLinkTarget(treeRoot, parentId, targetId)) {
      setLocalError(ARCH_MSG.SECTION_LINK_INVALID_TARGET);
      return;
    }
    if (mode === "edit" && linkSectionId && targetId === linkSectionId) {
      // Same target — no-op close
      onCancel();
      return;
    }
    onSubmit(targetId);
  };

  return (
    <>
      <div
        style={overlayStyle}
        role="presentation"
        data-testid="architecture-section-link-overlay"
        onClick={(e) => {
          if (e.target === e.currentTarget && !busy) onCancel();
        }}
      >
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="architecture-section-link-title"
          data-testid="architecture-section-link-dialog"
          style={panelStyle}
        >
          <h2
            id="architecture-section-link-title"
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
            data-testid="architecture-section-link-parent"
          >
            {ARCH_MSG.CREATE_PARENT_LABEL}: {parentTitle}
          </p>
          <p
            style={{
              margin: "0 0 12px",
              color: catalogColors.muted,
              fontSize: "0.9rem",
            }}
          >
            {ARCH_MSG.SECTION_LINK_TARGET_HINT}
          </p>
          <form onSubmit={handleSubmit}>
            <label
              htmlFor="architecture-section-link-target"
              style={{ fontWeight: 600, fontSize: "0.9rem" }}
            >
              {ARCH_MSG.SECTION_LINK_TARGET_LABEL}
            </label>
            <div
              style={{
                display: "flex",
                gap: 8,
                marginTop: 4,
                marginBottom: 12,
              }}
            >
              <input
                id="architecture-section-link-target"
                data-testid="architecture-section-link-target"
                type="text"
                readOnly
                value={targetTitle}
                placeholder={ARCH_MSG.SECTION_LINK_TARGET_LABEL}
                aria-required="true"
                style={{
                  flex: 1,
                  padding: "0.4rem 0.5rem",
                  fontSize: "0.95rem",
                  boxSizing: "border-box",
                }}
              />
              <button
                type="button"
                data-testid="architecture-section-link-browse"
                disabled={busy}
                onClick={() => setPickerOpen(true)}
                style={{
                  padding: "0.4rem 0.75rem",
                  border: `1px solid ${catalogColors.softBorder}`,
                  borderRadius: 4,
                  background: "#fff",
                  cursor: busy ? "not-allowed" : "pointer",
                  whiteSpace: "nowrap",
                }}
              >
                {ARCH_MSG.SECTION_LINK_BROWSE}
              </button>
            </div>
            {localError ? (
              <p
                role="alert"
                data-testid="architecture-section-link-error"
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
                data-testid="architecture-section-link-cancel"
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
                {ARCH_MSG.SECTION_LINK_CANCEL}
              </button>
              <button
                type="submit"
                data-testid="architecture-section-link-submit"
                disabled={busy}
                style={{
                  padding: "0.4rem 0.85rem",
                  border: `1px solid ${catalogColors.accent}`,
                  borderRadius: 4,
                  background: busy ? "#f0f0f0" : catalogColors.accent,
                  color: busy ? "#999" : "#fff",
                  cursor: busy ? "not-allowed" : "pointer",
                }}
              >
                {submitLabel}
              </button>
            </div>
          </form>
        </div>
      </div>
      <SectionTreePickerDialog
        open={pickerOpen}
        root={treeRoot}
        excludeId={parentId}
        busy={busy}
        onCancel={() => setPickerOpen(false)}
        onConfirm={(node) => {
          if (!isValidSectionLinkTarget(treeRoot, parentId, node.id)) {
            setLocalError(ARCH_MSG.SECTION_LINK_INVALID_TARGET);
            setPickerOpen(false);
            return;
          }
          setTargetId(node.id);
          setTargetTitle(node.title);
          setLocalError(null);
          setPickerOpen(false);
        }}
      />
    </>
  );
}

/** Resolve a display title for a target id (tests / host helpers). */
export function resolveTargetTitle(
  root: NavTreeNode | null,
  id: string | null,
): string {
  if (!root || !id) return "";
  return findNavNodeById(root, id)?.title ?? "";
}

export default SectionLinkDialog;
