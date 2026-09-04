/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listApplicationFiles } from "../api/developer/applicationFilesApi";
import { listApplications } from "../api/developer/pipelinesApi";
import type { ApplicationFileSummary, ApplicationSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { ApplicationFileDetailPanel } from "./ApplicationFileDetailPanel";

/**
 * SY-05 — browse XML application CMS/resource files and open the Admin editor.
 * Apps come from the pipelines catalog; files from /services/applicationfiles.
 */
export function ApplicationFilesPanel(): React.ReactElement {
  const [apps, setApps] = useState<ApplicationSummary[] | null>(null);
  const [appsError, setAppsError] = useState<string | null>(null);
  const [selectedApp, setSelectedApp] = useState<string | null>(null);
  const [files, setFiles] = useState<ApplicationFileSummary[] | null>(null);
  const [filesError, setFilesError] = useState<string | null>(null);
  const [selectedPath, setSelectedPath] = useState<string | null>(null);

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
      return;
    }
    let cancelled = false;
    setFiles(null);
    setFilesError(null);
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
            ]}
            rows={sortedFiles.map((f) => {
              const openPath = (f.path || "").trim();
              const isDir = f.directory === true;
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
                ],
              };
            })}
          />
        )}
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
