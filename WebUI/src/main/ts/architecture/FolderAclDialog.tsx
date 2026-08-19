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
 * Folder ACL dialog for Navigation (#3588 / parent #3092).
 * Hosts Explorer {@link FolderSecurityPanel} so operators can add/remove
 * principals via {@code pathApi.saveFolderProperties}.
 */

import React from "react";
import type { PSFolderProperties } from "../api/contentExplorer/types";
import { FolderSecurityPanel } from "../contentExplorer/FolderSecurityPanel";
import { catalogColors } from "../developer/catalogStyles";
import { ARCH_MSG } from "./messages";
import { useDialogEscape } from "./useDialogEscape";

export interface FolderAclDialogProps {
  open: boolean;
  busy: boolean;
  folderId: string | null;
  loadError: string | null;
  sectionTitle: string;
  currentUserIdentities: ReadonlyArray<string>;
  onCancel: () => void;
  load?: (folderId: string) => Promise<PSFolderProperties>;
  save?: (props: PSFolderProperties) => Promise<void>;
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
  maxWidth: 720,
  width: "100%",
  padding: "1.25rem 1.5rem",
  boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
  maxHeight: "90vh",
  overflowY: "auto",
};

export function FolderAclDialog({
  open,
  busy,
  folderId,
  loadError,
  sectionTitle,
  currentUserIdentities,
  onCancel,
  load,
  save,
}: FolderAclDialogProps): React.ReactElement | null {
  useDialogEscape(open, busy, onCancel);

  if (!open) {
    return null;
  }

  const id = folderId != null ? folderId.trim() : "";

  return (
    <div
      style={overlayStyle}
      data-testid="architecture-folder-acl-dialog"
      role="presentation"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onCancel();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="architecture-folder-acl-title"
        style={panelStyle}
        onClick={(e) => e.stopPropagation()}
      >
        <h2
          id="architecture-folder-acl-title"
          style={{ marginTop: 0, marginBottom: 8, fontSize: "1.1rem" }}
        >
          {ARCH_MSG.FOLDER_ACL_DIALOG_TITLE}
        </h2>
        {sectionTitle ? (
          <p
            data-testid="architecture-folder-acl-section"
            style={{
              margin: "0 0 8px",
              color: catalogColors.muted,
              fontSize: "0.85rem",
            }}
          >
            {sectionTitle}
          </p>
        ) : null}
        <p
          style={{
            margin: "0 0 12px",
            color: catalogColors.muted,
            fontSize: "0.85rem",
          }}
        >
          {ARCH_MSG.FOLDER_ACL_HINT}
        </p>
        {busy ? (
          <p
            data-testid="architecture-folder-acl-loading"
            aria-live="polite"
            style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
          >
            {ARCH_MSG.FOLDER_ACL_LOADING}
          </p>
        ) : null}
        {loadError ? (
          <p
            role="alert"
            data-testid="architecture-folder-acl-load-error"
            style={{ color: catalogColors.error, fontSize: "0.9rem" }}
          >
            {loadError}
          </p>
        ) : null}
        {!busy && !loadError && id ? (
          <FolderSecurityPanel
            folderId={id}
            currentUserIdentities={currentUserIdentities}
            load={load}
            save={save}
          />
        ) : null}
        {!busy && !loadError && !id ? (
          <p
            role="status"
            data-testid="architecture-folder-acl-no-folder"
            style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
          >
            {ARCH_MSG.FOLDER_ACL_NO_FOLDER}
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
            data-testid="architecture-folder-acl-cancel"
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
            {ARCH_MSG.FOLDER_ACL_CANCEL}
          </button>
        </div>
      </div>
    </div>
  );
}

export default FolderAclDialog;
