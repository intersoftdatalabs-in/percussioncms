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
 * Structure action toolbar for Architecture nav mutations (#3096 / #3097).
 */

import React from "react";
import { catalogColors } from "../developer/catalogStyles";
import { ARCH_MSG } from "./messages";
import { captureDialogOpener } from "./useDialogEscape";

export interface StructureActionBarProps {
  busy: boolean;
  canCreate: boolean;
  canCreateFromFolder: boolean;
  canConvertToFolder: boolean;
  canCreateSectionLink: boolean;
  canCreateExternalLink: boolean;
  canLanding: boolean;
  canEditLink: boolean;
  canRename: boolean;
  canProperties: boolean;
  canMove: boolean;
  canMoveUp: boolean;
  canMoveDown: boolean;
  canDelete: boolean;
  onCreate: () => void;
  onCreateFromFolder: () => void;
  onConvertToFolder: () => void;
  onCreateSectionLink: () => void;
  onCreateExternalLink: () => void;
  onLanding: () => void;
  onEditLink: () => void;
  onRename: () => void;
  onProperties: () => void;
  onMove: () => void;
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
  canCreateFromFolder,
  canConvertToFolder,
  canCreateSectionLink,
  canCreateExternalLink,
  canLanding,
  canEditLink,
  canRename,
  canProperties,
  canMove,
  canMoveUp,
  canMoveDown,
  canDelete,
  onCreate,
  onCreateFromFolder,
  onConvertToFolder,
  onCreateSectionLink,
  onCreateExternalLink,
  onLanding,
  onEditLink,
  onRename,
  onProperties,
  onMove,
  onMoveUp,
  onMoveDown,
  onDelete,
}: StructureActionBarProps): React.ReactElement {
  const createEnabled = canCreate && !busy;
  const createFromFolderEnabled = canCreateFromFolder && !busy;
  const convertEnabled = canConvertToFolder && !busy;
  const sectionLinkEnabled = canCreateSectionLink && !busy;
  const externalLinkEnabled = canCreateExternalLink && !busy;
  const landingEnabled = canLanding && !busy;
  const editLinkEnabled = canEditLink && !busy;
  const renameEnabled = canRename && !busy;
  const propertiesEnabled = canProperties && !busy;
  const moveEnabled = canMove && !busy;
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
        onClick={(e) => {
          captureDialogOpener(e.currentTarget);
          onCreate();
        }}
        style={buttonStyle(createEnabled)}
      >
        {ARCH_MSG.ACTION_CREATE}
      </button>
      <button
        type="button"
        data-testid="architecture-action-create-from-folder"
        disabled={!createFromFolderEnabled}
        onClick={(e) => {
          captureDialogOpener(e.currentTarget);
          onCreateFromFolder();
        }}
        style={buttonStyle(createFromFolderEnabled)}
      >
        {ARCH_MSG.ACTION_CREATE_FROM_FOLDER}
      </button>
      <button
        type="button"
        data-testid="architecture-action-create-section-link"
        disabled={!sectionLinkEnabled}
        onClick={(e) => {
          captureDialogOpener(e.currentTarget);
          onCreateSectionLink();
        }}
        style={buttonStyle(sectionLinkEnabled)}
      >
        {ARCH_MSG.ACTION_CREATE_SECTION_LINK}
      </button>
      <button
        type="button"
        data-testid="architecture-action-create-external-link"
        disabled={!externalLinkEnabled}
        onClick={(e) => {
          captureDialogOpener(e.currentTarget);
          onCreateExternalLink();
        }}
        style={buttonStyle(externalLinkEnabled)}
      >
        {ARCH_MSG.ACTION_CREATE_EXTERNAL_LINK}
      </button>
      <button
        type="button"
        data-testid="architecture-action-landing"
        disabled={!landingEnabled}
        onClick={(e) => {
          captureDialogOpener(e.currentTarget);
          onLanding();
        }}
        style={buttonStyle(landingEnabled)}
      >
        {ARCH_MSG.ACTION_LANDING}
      </button>
      <button
        type="button"
        data-testid="architecture-action-edit-link"
        disabled={!editLinkEnabled}
        onClick={(e) => {
          captureDialogOpener(e.currentTarget);
          onEditLink();
        }}
        style={buttonStyle(editLinkEnabled)}
      >
        {ARCH_MSG.ACTION_EDIT_LINK}
      </button>
      <button
        type="button"
        data-testid="architecture-action-rename"
        disabled={!renameEnabled}
        onClick={(e) => {
          captureDialogOpener(e.currentTarget);
          onRename();
        }}
        style={buttonStyle(renameEnabled)}
      >
        {ARCH_MSG.ACTION_RENAME}
      </button>
      <button
        type="button"
        data-testid="architecture-action-properties"
        disabled={!propertiesEnabled}
        onClick={(e) => {
          captureDialogOpener(e.currentTarget);
          onProperties();
        }}
        style={buttonStyle(propertiesEnabled)}
      >
        {ARCH_MSG.ACTION_PROPERTIES}
      </button>
      <button
        type="button"
        data-testid="architecture-action-move"
        disabled={!moveEnabled}
        onClick={(e) => {
          captureDialogOpener(e.currentTarget);
          onMove();
        }}
        style={buttonStyle(moveEnabled)}
      >
        {ARCH_MSG.ACTION_MOVE}
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
        data-testid="architecture-action-convert-to-folder"
        disabled={!convertEnabled}
        onClick={onConvertToFolder}
        style={buttonStyle(convertEnabled)}
      >
        {ARCH_MSG.ACTION_CONVERT_TO_FOLDER}
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
