/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { useSpaBootstrap } from "../app/bootstrap/BootstrapContext";
import { isApiError } from "../api/client";
import {
  createApplicationFolder,
  deleteApplicationPath,
  listApplicationFiles,
  moveApplicationPath,
} from "../api/developer/applicationFilesApi";
import { listApplications } from "../api/developer/pipelinesApi";
import type { ApplicationFileSummary, ApplicationSummary } from "../api/developer/types";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { ApplicationFileDetailPanel } from "./ApplicationFileDetailPanel";

/** API relative path: {@code /} separators, no traversal or absolute form. */
export function isSafeApplicationFileApiPath(path: string): boolean {
  const p = (path || "").trim().replace(/\\/g, "/");
  if (!p || p.startsWith("/") || p.startsWith("~") || p.startsWith("//")) {
    return false;
  }
  if (p.length >= 2 && p.charAt(1) === ":") {
    return false;
  }
  return p.split("/").every((seg) => seg.length > 0 && seg !== "." && seg !== "..");
}

const actionButtonStyle: React.CSSProperties = {
  ...openButtonStyle,
  marginRight: "8px",
};

const toolbarInputStyle: React.CSSProperties = {
  minWidth: "16rem",
  padding: "6px 8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  fontFamily: "monospace",
};

/**
 * SY-05 — browse XML application CMS/resource files and open the Admin editor.
 * Apps come from the pipelines catalog; files from /services/applicationfiles.
 */
export function ApplicationFilesPanel(): React.ReactElement {
  const { isAdmin } = useSpaBootstrap();
  const [apps, setApps] = useState<ApplicationSummary[] | null>(null);
  const [appsError, setAppsError] = useState<string | null>(null);
  const [selectedApp, setSelectedApp] = useState<string | null>(null);
  const [files, setFiles] = useState<ApplicationFileSummary[] | null>(null);
  const [filesError, setFilesError] = useState<string | null>(null);
  const [selectedPath, setSelectedPath] = useState<string | null>(null);
  const [folderDraft, setFolderDraft] = useState("ApplicationFiles/");
  const [notice, setNotice] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [renamingPath, setRenamingPath] = useState<string | null>(null);
  const [renameDraft, setRenameDraft] = useState("");
  const [pendingDelete, setPendingDelete] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listApplications()
      .then((list) => {
        if (!cancelled) setApps(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setAppsError(panelErrMsg(e, DEV_MSG.APPFILE_APPS_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!selectedApp) {
      setFiles(null);
      setFilesError(null);
      setNotice(null);
      setActionError(null);
      setRenamingPath(null);
      setPendingDelete(null);
      return;
    }
    let cancelled = false;
    setFiles(null);
    setFilesError(null);
    setNotice(null);
    setActionError(null);
    listApplicationFiles(selectedApp)
      .then((list) => {
        if (!cancelled) setFiles(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setFilesError(panelErrMsg(e, DEV_MSG.APPFILE_FILES_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [selectedApp]);

  function refreshFiles(app: string): Promise<void> {
    return listApplicationFiles(app)
      .then((list) => {
        setFiles(list);
        setFilesError(null);
      })
      .catch((e: unknown) => {
        setFilesError(panelErrMsg(e, DEV_MSG.APPFILE_FILES_ERROR));
      });
  }

  function writeErrorFallback(err: unknown, fallback: string): string {
    if (isApiError(err) && err.status === 403) {
      return DEV_MSG.APPFILE_FORBIDDEN;
    }
    if (isApiError(err) && err.status === 404) {
      return DEV_MSG.APPFILE_NOT_FOUND;
    }
    if (isApiError(err) && err.status === 400) {
      return DEV_MSG.APPFILE_PATH_INVALID;
    }
    return fallback;
  }

  async function handleCreateFolder(): Promise<void> {
    if (!selectedApp || busy || !isAdmin) return;
    const path = folderDraft.trim();
    if (!isSafeApplicationFileApiPath(path)) {
      setActionError(DEV_MSG.APPFILE_PATH_INVALID);
      return;
    }
    setBusy(true);
    setActionError(null);
    setNotice(null);
    try {
      await createApplicationFolder(selectedApp, path);
      setNotice(DEV_MSG.APPFILE_FOLDER_CREATED);
      setFolderDraft("ApplicationFiles/");
      await refreshFiles(selectedApp);
    } catch (err: unknown) {
      setActionError(panelErrMsg(err, writeErrorFallback(err, DEV_MSG.APPFILE_FOLDER_CREATE_ERROR)));
    } finally {
      setBusy(false);
    }
  }

  async function handleRename(): Promise<void> {
    if (!selectedApp || !renamingPath || busy || !isAdmin) return;
    const toPath = renameDraft.trim();
    if (!isSafeApplicationFileApiPath(toPath)) {
      setActionError(DEV_MSG.APPFILE_PATH_INVALID);
      return;
    }
    setBusy(true);
    setActionError(null);
    setNotice(null);
    try {
      await moveApplicationPath(selectedApp, renamingPath, toPath);
      setNotice(DEV_MSG.APPFILE_RENAMED);
      setRenamingPath(null);
      await refreshFiles(selectedApp);
    } catch (err: unknown) {
      setActionError(panelErrMsg(err, writeErrorFallback(err, DEV_MSG.APPFILE_RENAME_ERROR)));
    } finally {
      setBusy(false);
    }
  }

  async function handleDeleteConfirm(): Promise<void> {
    if (!selectedApp || !pendingDelete || busy || !isAdmin) return;
    const path = pendingDelete;
    setBusy(true);
    setActionError(null);
    setNotice(null);
    try {
      await deleteApplicationPath(selectedApp, path);
      setNotice(DEV_MSG.APPFILE_DELETED);
      setPendingDelete(null);
      await refreshFiles(selectedApp);
    } catch (err: unknown) {
      setActionError(panelErrMsg(err, writeErrorFallback(err, DEV_MSG.APPFILE_DELETE_ERROR)));
    } finally {
      setBusy(false);
    }
  }

  const sortedApps = useMemo(() => {
    if (!apps) return [];
    return [...apps]
      .filter((a) => (a.name || "").trim().length > 0)
      .sort((a, b) =>
        (a.name || "").localeCompare(b.name || "", undefined, { sensitivity: "base" }),
      );
  }, [apps]);

  const sortedFiles = useMemo(() => {
    if (!files) return [];
    return [...files]
      .filter((f) => (f.path || "").trim().length > 0)
      .sort((a, b) =>
        (a.path || "").localeCompare(b.path || "", undefined, { sensitivity: "base" }),
      );
  }, [files]);

  if (selectedApp && selectedPath) {
    return (
      <ApplicationFileDetailPanel
        applicationName={selectedApp}
        path={selectedPath}
        onBack={() => setSelectedPath(null)}
      />
    );
  }

  if (selectedApp) {
    if (filesError) {
      return (
        <div data-testid="developer-appfile-files">
          <button
            type="button"
            data-testid="developer-appfile-back-apps"
            aria-label={DEV_MSG.APPFILE_BACK_APPS}
            onClick={() => {
              setSelectedApp(null);
              setSelectedPath(null);
            }}
            style={{ marginBottom: "12px", background: "transparent", border: "none", cursor: "pointer", color: "#007ea8" }}
          >
            ← {DEV_MSG.APPFILE_BACK_APPS}
          </button>
          <CatalogStatus testId="developer-appfile-files-error" error>
            {filesError}
          </CatalogStatus>
        </div>
      );
    }
    if (files == null) {
      return (
        <CatalogStatus testId="developer-appfile-files-loading">
          {DEV_MSG.APPFILE_FILES_LOADING}
        </CatalogStatus>
      );
    }
    return (
      <div data-testid="developer-appfile-files">
        <button
          type="button"
          data-testid="developer-appfile-back-apps"
          aria-label={DEV_MSG.APPFILE_BACK_APPS}
          onClick={() => {
            setSelectedApp(null);
            setSelectedPath(null);
          }}
          style={{
            marginBottom: "12px",
            background: "transparent",
            border: "none",
            cursor: "pointer",
            color: "#007ea8",
          }}
        >
          ← {DEV_MSG.APPFILE_BACK_APPS}
        </button>
        <h2
          style={{ margin: "0 0 8px", fontSize: "1.1rem" }}
          data-testid="developer-appfile-app-title"
        >
          {selectedApp}
        </h2>
        <CatalogHint>{DEV_MSG.APPFILE_FILES_HINT}</CatalogHint>
        {isAdmin ? (
          <form
            data-testid="developer-appfile-folder-form"
            onSubmit={(ev) => {
              ev.preventDefault();
              void handleCreateFolder();
            }}
            style={{
              display: "flex",
              gap: "8px",
              alignItems: "center",
              flexWrap: "wrap",
              margin: "12px 0",
            }}
          >
            <label htmlFor="developer-appfile-folder-path" style={mutedCell}>
              {DEV_MSG.APPFILE_NEW_FOLDER_LABEL}
            </label>
            <input
              id="developer-appfile-folder-path"
              data-testid="developer-appfile-folder-path"
              value={folderDraft}
              disabled={busy}
              onChange={(ev) => setFolderDraft(ev.target.value)}
              placeholder={DEV_MSG.APPFILE_NEW_FOLDER_PLACEHOLDER}
              style={toolbarInputStyle}
            />
            <button
              type="submit"
              data-testid="developer-appfile-create-folder"
              disabled={busy || !folderDraft.trim()}
            >
              {DEV_MSG.APPFILE_NEW_FOLDER}
            </button>
          </form>
        ) : (
          <CatalogHint>
            <span data-testid="developer-appfile-write-admin-hint">
              {DEV_MSG.APPFILE_WRITE_ADMIN_ONLY}
            </span>
          </CatalogHint>
        )}
        {notice ? (
          <div
            role="status"
            data-testid="developer-appfile-files-notice"
            style={{ color: "#276749", marginBottom: "8px" }}
          >
            {notice}
          </div>
        ) : null}
        {actionError ? (
          <div
            role="alert"
            data-testid="developer-appfile-files-action-error"
            style={{ color: catalogColors.error, marginBottom: "8px" }}
          >
            {actionError}
          </div>
        ) : null}
        {sortedFiles.length === 0 ? (
          <CatalogStatus testId="developer-appfile-files-empty">
            {DEV_MSG.APPFILE_FILES_EMPTY}
          </CatalogStatus>
        ) : (
          <SimpleCatalogTable
            tableTestId="developer-appfile-table"
            rowTestId="developer-appfile-row"
            columns={[
              DEV_MSG.APPFILE_COL_NAME,
              DEV_MSG.APPFILE_COL_PATH,
              DEV_MSG.APPFILE_COL_KIND,
              ...(isAdmin ? [DEV_MSG.APPFILE_COL_ACTIONS] : []),
            ]}
            rows={sortedFiles.map((f) => {
              const openPath = (f.path || "").trim();
              const isDir = f.directory === true;
              const renaming = renamingPath === openPath;
              const actionCell = isAdmin ? (
                <span key="a">
                  {renaming ? (
                    <span style={{ display: "inline-flex", gap: "6px", flexWrap: "wrap" }}>
                      <input
                        data-testid="developer-appfile-rename-path"
                        value={renameDraft}
                        disabled={busy}
                        onChange={(ev) => setRenameDraft(ev.target.value)}
                        aria-label={DEV_MSG.APPFILE_RENAME_LABEL}
                        style={toolbarInputStyle}
                      />
                      <button
                        type="button"
                        data-testid="developer-appfile-rename-save"
                        disabled={busy || !renameDraft.trim()}
                        onClick={(ev) => {
                          ev.stopPropagation();
                          void handleRename();
                        }}
                      >
                        {DEV_MSG.APPFILE_RENAME_SAVE}
                      </button>
                      <button
                        type="button"
                        data-testid="developer-appfile-rename-cancel"
                        disabled={busy}
                        onClick={(ev) => {
                          ev.stopPropagation();
                          setRenamingPath(null);
                        }}
                      >
                        {DEV_MSG.APPFILE_RENAME_CANCEL}
                      </button>
                    </span>
                  ) : (
                    <>
                      <button
                        type="button"
                        data-testid="developer-appfile-rename"
                        aria-label={`Rename ${openPath}`}
                        disabled={busy}
                        onClick={(ev) => {
                          ev.stopPropagation();
                          setRenamingPath(openPath);
                          setRenameDraft(openPath);
                          setActionError(null);
                          setNotice(null);
                        }}
                        style={actionButtonStyle}
                      >
                        {DEV_MSG.APPFILE_RENAME}
                      </button>
                      <button
                        type="button"
                        data-testid="developer-appfile-delete"
                        aria-label={`Delete ${openPath}`}
                        disabled={busy}
                        onClick={(ev) => {
                          ev.stopPropagation();
                          setPendingDelete(openPath);
                        }}
                        style={actionButtonStyle}
                      >
                        {DEV_MSG.APPFILE_DELETE}
                      </button>
                    </>
                  )}
                </span>
              ) : null;
              return {
                key: openPath,
                onClick: isDir ? undefined : () => setSelectedPath(openPath),
                cells: [
                  isDir ? (
                    <span key="n" style={mutedCell}>
                      {f.name || openPath}
                    </span>
                  ) : (
                    <button
                      key="open"
                      type="button"
                      data-testid="developer-appfile-open"
                      aria-label={`Open ${f.name || openPath}`}
                      onClick={(ev) => {
                        ev.stopPropagation();
                        setSelectedPath(openPath);
                      }}
                      style={openButtonStyle}
                    >
                      {f.name || openPath}
                    </button>
                  ),
                  <span key="p" style={monoCell}>
                    {openPath}
                  </span>,
                  <span key="k" style={mutedCell}>
                    {isDir ? DEV_MSG.APPFILE_KIND_DIR : DEV_MSG.APPFILE_KIND_FILE}
                  </span>,
                  ...(actionCell ? [actionCell] : []),
                ],
              };
            })}
          />
        )}
        <CatalogConfirmDialog
          open={pendingDelete != null}
          busy={busy}
          message={DEV_MSG.APPFILE_DELETE_CONFIRM}
          onCancel={() => setPendingDelete(null)}
          onConfirm={() => {
            void handleDeleteConfirm();
          }}
        />
      </div>
    );
  }

  if (appsError) {
    return (
      <CatalogStatus testId="developer-appfile-apps-error" error>
        {appsError}
      </CatalogStatus>
    );
  }
  if (apps == null) {
    return (
      <CatalogStatus testId="developer-appfile-apps-loading">
        {DEV_MSG.APPFILE_APPS_LOADING}
      </CatalogStatus>
    );
  }
  if (sortedApps.length === 0) {
    return (
      <CatalogStatus testId="developer-appfile-apps-empty">
        {DEV_MSG.APPFILE_APPS_EMPTY}
      </CatalogStatus>
    );
  }

  return (
    <div data-testid="developer-appfile-panel">
      <CatalogHint>{DEV_MSG.APPFILE_APPS_HINT}</CatalogHint>
      <SimpleCatalogTable
        tableTestId="developer-appfile-apps-table"
        rowTestId="developer-appfile-app-row"
        columns={[
          DEV_MSG.APPFILE_COL_APP,
          DEV_MSG.APPFILE_COL_ROOT,
          DEV_MSG.APPFILE_COL_DESCRIPTION,
        ]}
        rows={sortedApps.map((a) => {
          const openKey = (a.name || "").trim();
          return {
            key: openKey,
            onClick: () => setSelectedApp(openKey),
            cells: [
              <button
                key="open"
                type="button"
                data-testid="developer-appfile-app-open"
                aria-label={`Open ${openKey}`}
                onClick={(ev) => {
                  ev.stopPropagation();
                  setSelectedApp(openKey);
                }}
                style={openButtonStyle}
              >
                {openKey}
              </button>,
              <span key="r" style={monoCell}>
                {a.appRoot || "—"}
              </span>,
              <span key="d" style={mutedCell}>
                {a.description || "—"}
              </span>,
            ],
          };
        })}
      />
    </div>
  );
}
