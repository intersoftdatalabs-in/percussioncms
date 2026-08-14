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
 * Move / reparent dialog for Architecture (#3349 / parent #3092).
 * Peer: CM1 PercSectionTreeDialog Move Section (target parent + optional index).
 */

import React, { useEffect, useMemo, useState } from "react";
import {
  findNavNodeById,
  isValidMoveTargetParent,
  listMoveTargetPositions,
  omitNavSubtree,
} from "../api/architecture/sectionMutations";
import type { NavTreeNode } from "../api/architecture/types";
import { catalogColors } from "../developer/catalogStyles";
import { SectionTreePickerDialog } from "./SectionTreePickerDialog";
import { ARCH_MSG } from "./messages";
import { useDialogEscape } from "./useDialogEscape";

export interface MoveSectionDialogProps {
  open: boolean;
  sourceId: string;
  sourceTitle: string;
  treeRoot: NavTreeNode | null;
  busy: boolean;
  onCancel: () => void;
  onSubmit: (targetParentId: string, targetIndex: number) => void;
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

export function MoveSectionDialog({
  open,
  sourceId,
  sourceTitle,
  treeRoot,
  busy,
  onCancel,
  onSubmit,
}: MoveSectionDialogProps): React.ReactElement | null {
  const [targetId, setTargetId] = useState<string | null>(null);
  const [targetTitle, setTargetTitle] = useState("");
  const [targetIndex, setTargetIndex] = useState(-1);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  useDialogEscape(open && !pickerOpen, busy, onCancel);

  useEffect(() => {
    if (open) {
      setTargetId(null);
      setTargetTitle("");
      setTargetIndex(-1);
      setPickerOpen(false);
      setLocalError(null);
    }
  }, [open]);

  const targetNode = useMemo(
    () => (targetId ? findNavNodeById(treeRoot, targetId) : null),
    [treeRoot, targetId],
  );
  const positions = useMemo(
    () => listMoveTargetPositions(targetNode, sourceId),
    [targetNode, sourceId],
  );
  const pickerRoot = useMemo(
    () => omitNavSubtree(treeRoot, sourceId),
    [treeRoot, sourceId],
  );

  if (!open) {
    return null;
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetId) {
      setLocalError(ARCH_MSG.MOVE_NO_TARGET);
      return;
    }
    if (!isValidMoveTargetParent(treeRoot, sourceId, targetId)) {
      setLocalError(ARCH_MSG.MOVE_INVALID_TARGET);
      return;
    }
    onSubmit(targetId, targetIndex);
  };

  return (
    <>
      <div
        style={overlayStyle}
        role="presentation"
        data-testid="architecture-move-overlay"
        onClick={(e) => {
          if (e.target === e.currentTarget && !busy) onCancel();
        }}
      >
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="architecture-move-title"
          data-testid="architecture-move-dialog"
          style={panelStyle}
        >
          <h2
            id="architecture-move-title"
            style={{ marginTop: 0, marginBottom: 8, fontSize: "1.15rem" }}
          >
            {ARCH_MSG.MOVE_DIALOG_TITLE}
          </h2>
          <p
            style={{
              margin: "0 0 12px",
              color: catalogColors.muted,
              fontSize: "0.9rem",
            }}
            data-testid="architecture-move-source"
          >
            {ARCH_MSG.MOVE_SECTION_LABEL}: {sourceTitle}
          </p>
          <p
            style={{
              margin: "0 0 12px",
              color: catalogColors.muted,
              fontSize: "0.9rem",
            }}
          >
            {ARCH_MSG.MOVE_HINT}
          </p>
          <form onSubmit={handleSubmit}>
            <label
              htmlFor="architecture-move-parent"
              style={{ fontWeight: 600, fontSize: "0.9rem" }}
            >
              {ARCH_MSG.MOVE_PARENT_LABEL}
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
                id="architecture-move-parent"
                data-testid="architecture-move-parent"
                type="text"
                readOnly
                value={targetTitle}
                placeholder={ARCH_MSG.MOVE_PARENT_LABEL}
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
                data-testid="architecture-move-browse"
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
                {ARCH_MSG.MOVE_BROWSE}
              </button>
            </div>
            <label
              htmlFor="architecture-move-position"
              style={{ fontWeight: 600, fontSize: "0.9rem" }}
            >
              {ARCH_MSG.MOVE_POSITION_LABEL}
            </label>
            <select
              id="architecture-move-position"
              data-testid="architecture-move-position"
              disabled={busy || !targetId}
              value={String(targetIndex)}
              onChange={(ev) => {
                const next = Number(ev.target.value);
                setTargetIndex(Number.isNaN(next) ? -1 : next);
              }}
              style={{
                display: "block",
                width: "100%",
                marginTop: 4,
                marginBottom: 12,
                padding: "0.4rem 0.5rem",
                fontSize: "0.95rem",
                boxSizing: "border-box",
              }}
            >
              {positions.map((slot) => (
                <option
                  key={`${slot.targetIndex}-${slot.beforeId ?? "end"}`}
                  value={String(slot.targetIndex)}
                >
                  {slot.beforeTitle
                    ? ARCH_MSG.MOVE_POSITION_BEFORE.split("{0}").join(
                        slot.beforeTitle,
                      )
                    : ARCH_MSG.MOVE_POSITION_END}
                </option>
              ))}
            </select>
            {localError ? (
              <p
                role="alert"
                data-testid="architecture-move-error"
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
                data-testid="architecture-move-cancel"
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
                {ARCH_MSG.MOVE_CANCEL}
              </button>
              <button
                type="submit"
                data-testid="architecture-move-submit"
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
                {ARCH_MSG.MOVE_SUBMIT}
              </button>
            </div>
          </form>
        </div>
      </div>
      <SectionTreePickerDialog
        open={pickerOpen}
        root={pickerRoot}
        title={ARCH_MSG.MOVE_DIALOG_TITLE}
        busy={busy}
        onCancel={() => setPickerOpen(false)}
        onConfirm={(node) => {
          if (!isValidMoveTargetParent(treeRoot, sourceId, node.id)) {
            setLocalError(ARCH_MSG.MOVE_INVALID_TARGET);
            setPickerOpen(false);
            return;
          }
          setTargetId(node.id);
          setTargetTitle(node.title);
          setTargetIndex(-1);
          setLocalError(null);
          setPickerOpen(false);
        }}
      />
    </>
  );
}

export default MoveSectionDialog;
