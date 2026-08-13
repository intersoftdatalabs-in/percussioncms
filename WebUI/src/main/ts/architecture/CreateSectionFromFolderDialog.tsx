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
 * Create section from folder dialog for Architecture (#3302 / parent #3092).
 * Folder + landing page fields; optional ContentBrowser picker.
 */

import React, { useEffect, useState } from "react";
import type { SelectionResult } from "../api/contentExplorer/types";
import {
  splitCmsPagePath,
  validateLandingPageName,
  validateSourceFolderPath,
} from "../api/architecture/sectionMutations";
import { ContentBrowser } from "../contentBrowser/ContentBrowser";
import { catalogColors } from "../developer/catalogStyles";
import { ARCH_MSG } from "./messages";
import { useDialogEscape } from "./useDialogEscape";

export interface CreateSectionFromFolderDialogProps {
  open: boolean;
  siteName: string;
  parentTitle: string;
  busy: boolean;
  /**
   * When false, skip ContentBrowser (unit tests) and use path / name fields.
   * Default true in product shell.
   */
  useContentBrowser?: boolean;
  onCancel: () => void;
  onSubmit: (input: { sourceFolderPath: string; pageName: string }) => void;
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
  maxWidth: 900,
  width: "100%",
  padding: "1.25rem 1.5rem",
  boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
  maxHeight: "92vh",
  overflow: "auto",
};

const fieldStyle: React.CSSProperties = {
  display: "block",
  width: "100%",
  marginTop: 4,
  marginBottom: 12,
  padding: "0.4rem 0.5rem",
  fontSize: "0.95rem",
  boxSizing: "border-box",
};

type BrowserMode = "folder" | "page" | null;

export function CreateSectionFromFolderDialog({
  open,
  siteName,
  parentTitle,
  busy,
  useContentBrowser = true,
  onCancel,
  onSubmit,
}: CreateSectionFromFolderDialogProps): React.ReactElement | null {
  const [folderPath, setFolderPath] = useState("");
  const [pageName, setPageName] = useState("");
  const [localError, setLocalError] = useState<string | null>(null);
  const [browserMode, setBrowserMode] = useState<BrowserMode>(null);

  useDialogEscape(open && browserMode == null, busy, onCancel);

  useEffect(() => {
    if (open) {
      setFolderPath("");
      setPageName("");
      setLocalError(null);
      setBrowserMode(null);
    }
  }, [open]);

  if (!open) {
    return null;
  }

  const initialPath = siteName.trim()
    ? `//Sites/${siteName.trim()}`
    : "//Sites";

  const handleBrowserConfirm = (selection: SelectionResult) => {
    const item = selection.items?.[0];
    if (!item) {
      setLocalError(
        browserMode === "folder"
          ? ARCH_MSG.CREATE_FROM_FOLDER_NO_FOLDER
          : ARCH_MSG.CREATE_FROM_FOLDER_NO_PAGE,
      );
      return;
    }
    if (browserMode === "folder") {
      const path = item.path || "";
      if (!path.trim()) {
        setLocalError(ARCH_MSG.CREATE_FROM_FOLDER_NO_FOLDER);
        return;
      }
      setFolderPath(path);
      setBrowserMode(null);
      setLocalError(null);
      return;
    }
    const fromPage = splitCmsPagePath(item.path || "");
    if (fromPage) {
      setFolderPath(fromPage.folderPath);
      setPageName(fromPage.pageName);
    } else if (item.name) {
      setPageName(item.name);
    } else {
      setLocalError(ARCH_MSG.CREATE_FROM_FOLDER_NO_PAGE);
      return;
    }
    setBrowserMode(null);
    setLocalError(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const folderErr = validateSourceFolderPath(folderPath);
    if (folderErr) {
      setLocalError(folderErr);
      return;
    }
    const pageErr = validateLandingPageName(pageName);
    if (pageErr) {
      setLocalError(pageErr);
      return;
    }
    onSubmit({
      sourceFolderPath: folderPath.trim(),
      pageName: pageName.trim(),
    });
  };

  return (
    <div
      style={overlayStyle}
      role="presentation"
      data-testid="architecture-from-folder-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onCancel();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="architecture-from-folder-title"
        data-testid="architecture-from-folder-dialog"
        style={panelStyle}
      >
        <h2
          id="architecture-from-folder-title"
          style={{ marginTop: 0, marginBottom: 8, fontSize: "1.15rem" }}
        >
          {ARCH_MSG.CREATE_FROM_FOLDER_DIALOG_TITLE}
        </h2>
        <p
          style={{
            margin: "0 0 8px",
            color: catalogColors.muted,
            fontSize: "0.9rem",
          }}
          data-testid="architecture-from-folder-parent"
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
          {ARCH_MSG.CREATE_FROM_FOLDER_HINT}
        </p>

        {useContentBrowser && browserMode != null ? (
          <div data-testid="architecture-from-folder-browser">
            <ContentBrowser
              mode="select"
              multiSelect={false}
              allowFolderSelect={browserMode === "folder"}
              allowItemSelect={browserMode === "page"}
              roots="sites"
              initialPath={
                browserMode === "page" && folderPath.trim()
                  ? folderPath.trim()
                  : initialPath
              }
              enablePreview={false}
              enableSearch={false}
              title={
                browserMode === "folder"
                  ? ARCH_MSG.CREATE_FROM_FOLDER_PICKER_FOLDER
                  : ARCH_MSG.CREATE_FROM_FOLDER_PICKER_PAGE
              }
              onConfirm={handleBrowserConfirm}
              onCancel={() => setBrowserMode(null)}
            />
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <label
              htmlFor="architecture-from-folder-path"
              style={{ fontWeight: 600, fontSize: "0.9rem" }}
            >
              {ARCH_MSG.CREATE_FROM_FOLDER_FOLDER_LABEL}
            </label>
            <input
              id="architecture-from-folder-path"
              data-testid="architecture-from-folder-path"
              type="text"
              value={folderPath}
              onChange={(e) => setFolderPath(e.target.value)}
              disabled={busy}
              aria-required="true"
              style={fieldStyle}
            />
            {useContentBrowser ? (
              <button
                type="button"
                data-testid="architecture-from-folder-browse-folder"
                disabled={busy}
                onClick={() => setBrowserMode("folder")}
                style={{
                  marginBottom: 12,
                  marginRight: 8,
                  padding: "0.4rem 0.85rem",
                  border: `1px solid ${catalogColors.softBorder}`,
                  borderRadius: 4,
                  background: "#fff",
                  cursor: busy ? "not-allowed" : "pointer",
                }}
              >
                {ARCH_MSG.CREATE_FROM_FOLDER_BROWSE_FOLDER}
              </button>
            ) : null}

            <label
              htmlFor="architecture-from-folder-page"
              style={{ fontWeight: 600, fontSize: "0.9rem" }}
            >
              {ARCH_MSG.CREATE_FROM_FOLDER_PAGE_LABEL}
            </label>
            <input
              id="architecture-from-folder-page"
              data-testid="architecture-from-folder-page"
              type="text"
              value={pageName}
              onChange={(e) => setPageName(e.target.value)}
              disabled={busy}
              aria-required="true"
              style={fieldStyle}
            />
            {useContentBrowser ? (
              <button
                type="button"
                data-testid="architecture-from-folder-browse-page"
                disabled={busy}
                onClick={() => setBrowserMode("page")}
                style={{
                  marginBottom: 12,
                  padding: "0.4rem 0.85rem",
                  border: `1px solid ${catalogColors.softBorder}`,
                  borderRadius: 4,
                  background: "#fff",
                  cursor: busy ? "not-allowed" : "pointer",
                }}
              >
                {ARCH_MSG.CREATE_FROM_FOLDER_BROWSE_PAGE}
              </button>
            ) : null}

            {localError ? (
              <p
                role="alert"
                data-testid="architecture-from-folder-error"
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
                data-testid="architecture-from-folder-cancel"
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
                {ARCH_MSG.CREATE_FROM_FOLDER_CANCEL}
              </button>
              <button
                type="submit"
                data-testid="architecture-from-folder-submit"
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
                {ARCH_MSG.CREATE_FROM_FOLDER_SUBMIT}
              </button>
            </div>
          </form>
        )}

        {useContentBrowser && browserMode != null ? (
          <div
            style={{
              display: "flex",
              justifyContent: "flex-end",
              marginTop: 12,
            }}
          >
            <button
              type="button"
              data-testid="architecture-from-folder-cancel"
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
              {ARCH_MSG.CREATE_FROM_FOLDER_CANCEL}
            </button>
          </div>
        ) : null}
      </div>
    </div>
  );
}

export default CreateSectionFromFolderDialog;
