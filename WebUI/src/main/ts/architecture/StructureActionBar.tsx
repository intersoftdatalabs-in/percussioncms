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
 * Structure action toolbar for Architecture nav mutations (#3096).
 */

import React from "react";
import { catalogColors } from "../developer/catalogStyles";
import { ARCH_MSG } from "./messages";

export interface StructureActionBarProps {
  busy: boolean;
  canCreate: boolean;
  canRename: boolean;
  canMoveUp: boolean;
  canMoveDown: boolean;
  canDelete: boolean;
  onCreate: () => void;
  onRename: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
  onDelete: () => void;
}

const barStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  alignItems: "center",
  gap: "0.5rem",
  marginBottom: "10px",
};

function buttonStyle(enabled: boolean): React.CSSProperties {
  return {
    padding: "0.35rem 0.75rem",
    border: `1px solid ${catalogColors.softBorder}`,
    borderRadius: 4,
    background: enabled ? "#fff" : "#f0f0f0",
    color: enabled ? "#222" : "#999",
    cursor: enabled ? "pointer" : "not-allowed",
    fontSize: "0.9rem",
  };
}

export function StructureActionBar({
  busy,
  canCreate,
  canRename,
  canMoveUp,
  canMoveDown,
  canDelete,
  onCreate,
  onRename,
  onMoveUp,
  onMoveDown,
  onDelete,
}: StructureActionBarProps): React.ReactElement {
  const createEnabled = canCreate && !busy;
  const renameEnabled = canRename && !busy;
  const upEnabled = canMoveUp && !busy;
  const downEnabled = canMoveDown && !busy;
  const deleteEnabled = canDelete && !busy;

  return (
    <div
      role="toolbar"
      aria-label={ARCH_MSG.ACTIONS_LABEL}
      data-testid="architecture-structure-actions"
      style={barStyle}
    >
      <button
        type="button"
        data-testid="architecture-action-create"
        disabled={!createEnabled}
        onClick={onCreate}
        style={buttonStyle(createEnabled)}
      >
        {ARCH_MSG.ACTION_CREATE}
      </button>
      <button
        type="button"
        data-testid="architecture-action-rename"
        disabled={!renameEnabled}
        onClick={onRename}
        style={buttonStyle(renameEnabled)}
      >
        {ARCH_MSG.ACTION_RENAME}
      </button>
      <button
        type="button"
        data-testid="architecture-action-move-up"
        disabled={!upEnabled}
        onClick={onMoveUp}
        style={buttonStyle(upEnabled)}
      >
        {ARCH_MSG.ACTION_MOVE_UP}
      </button>
      <button
        type="button"
        data-testid="architecture-action-move-down"
        disabled={!downEnabled}
        onClick={onMoveDown}
        style={buttonStyle(downEnabled)}
      >
        {ARCH_MSG.ACTION_MOVE_DOWN}
      </button>
      <button
        type="button"
        data-testid="architecture-action-delete"
        disabled={!deleteEnabled}
        onClick={onDelete}
        style={{
          ...buttonStyle(deleteEnabled),
          color: deleteEnabled ? "#a11" : "#999",
        }}
      >
        {ARCH_MSG.ACTION_DELETE}
      </button>
      {busy ? (
        <span
          data-testid="architecture-action-busy"
          style={{ color: catalogColors.muted, fontSize: "0.85rem" }}
          aria-live="polite"
        >
          {ARCH_MSG.ACTION_BUSY}
        </span>
      ) : null}
    </div>
  );
}

export default StructureActionBar;
