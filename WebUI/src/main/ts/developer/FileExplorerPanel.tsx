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

import React, { useEffect, useMemo, useState } from "react";
import {
  listFileExplorerChildren,
  listFileExplorerRoots,
  parentFileExplorerPath,
  type FileExplorerEntry,
  type FileExplorerRoot,
} from "../api/developer/fileExplorerApi";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { backButton, catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

function formatSize(size: number | undefined, directory: boolean): string {
  if (directory || size == null) return "—";
  return String(size);
}

function crumbStyle(active: boolean): React.CSSProperties {
  return {
    ...openButtonStyle,
    textDecoration: active ? "none" : "underline",
    fontWeight: active ? 600 : 400,
    cursor: active ? "default" : "pointer",
    color: active ? catalogColors.text : catalogColors.accent,
  };
}

/**
 * Developer File Explorer — read-only allow-listed browse (#4326).
 * Distinct from Server Configs (SY-02) and application CMS files (SY-05).
 */
export function FileExplorerPanel(): React.ReactElement {
  const [roots, setRoots] = useState<FileExplorerRoot[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<FileExplorerRoot | null>(null);
  const [relPath, setRelPath] = useState("");
  const [entries, setEntries] = useState<FileExplorerEntry[] | null>(null);
  const [childrenError, setChildrenError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listFileExplorerRoots()
      .then((list) => {
        if (!cancelled) setRoots(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.FE_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!selected) {
      setEntries(null);
      setChildrenError(null);
      return;
    }
    let cancelled = false;
    setEntries(null);
    setChildrenError(null);
    listFileExplorerChildren(selected.id, relPath)
      .then((list) => {
        if (!cancelled) setEntries(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setChildrenError(panelErrMsg(e, DEV_MSG.FE_CHILDREN_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [selected, relPath]);

  const sortedRoots = useMemo(() => {
    if (!roots) return [];
    return [...roots].sort((a, b) =>
      a.displayName.localeCompare(b.displayName, undefined, { sensitivity: "base" }),
    );
  }, [roots]);

  const sortedEntries = useMemo(() => {
    if (!entries) return [];
    return [...entries].sort((a, b) => {
      if (a.directory !== b.directory) return a.directory ? -1 : 1;
      return a.name.localeCompare(b.name, undefined, { sensitivity: "base" });
    });
  }, [entries]);

  const pathCrumbs = useMemo(
    () => relPath.split("/").filter((p) => p.length > 0),
    [relPath],
  );

  if (error) {
    return (
      <CatalogStatus testId="developer-fe-error" error>
        {error}
      </CatalogStatus>
    );
  }
  if (roots == null) {
    return (
      <CatalogStatus testId="developer-fe-loading">{DEV_MSG.FE_LOADING}</CatalogStatus>
    );
  }

  if (selected) {
    return (
      <div data-testid="developer-fe-browse" data-fe-root={selected.id}>
        <button
          type="button"
          data-testid="developer-fe-back-roots"
          onClick={() => {
            setSelected(null);
            setRelPath("");
          }}
          style={backButton}
        >
          {DEV_MSG.FE_BACK_ROOTS}
        </button>
        <CatalogHint>{DEV_MSG.FE_BROWSE_HINT}</CatalogHint>
        <nav
          aria-label={DEV_MSG.FE_BREADCRUMB}
          data-testid="developer-fe-breadcrumb"
          style={{
            display: "flex",
            flexWrap: "wrap",
            alignItems: "center",
            gap: "6px",
            marginBottom: "12px",
            fontSize: "0.95rem",
          }}
        >
          <button
            type="button"
            data-testid="developer-fe-crumb-root"
            onClick={() => setRelPath("")}
            style={crumbStyle(relPath === "")}
          >
            {selected.displayName}
          </button>
          {pathCrumbs.map((seg, i) => {
            const target = pathCrumbs.slice(0, i + 1).join("/");
            const last = i === pathCrumbs.length - 1;
            return (
              <React.Fragment key={target}>
                <span aria-hidden="true" style={mutedCell}>
                  /
                </span>
                <button
                  type="button"
                  data-testid={`developer-fe-crumb-${i}`}
                  data-fe-path={target}
                  disabled={last}
                  onClick={() => {
                    if (!last) setRelPath(target);
                  }}
                  style={crumbStyle(last)}
                >
                  {seg}
                </button>
              </React.Fragment>
            );
          })}
        </nav>
        {relPath ? (
          <button
            type="button"
            data-testid="developer-fe-up"
            onClick={() => setRelPath(parentFileExplorerPath(relPath))}
            style={{ ...backButton, marginBottom: "12px" }}
          >
            {DEV_MSG.FE_UP}
          </button>
        ) : null}
        {childrenError ? (
          <CatalogStatus testId="developer-fe-children-error" error>
            {childrenError}
          </CatalogStatus>
        ) : entries == null ? (
          <CatalogStatus testId="developer-fe-children-loading">
            {DEV_MSG.FE_CHILDREN_LOADING}
          </CatalogStatus>
        ) : sortedEntries.length === 0 ? (
          <CatalogStatus testId="developer-fe-children-empty">
            {DEV_MSG.FE_CHILDREN_EMPTY}
          </CatalogStatus>
        ) : (
          <SimpleCatalogTable
            tableTestId="developer-fe-children-table"
            rowTestId="developer-fe-child-row"
            columns={[
              DEV_MSG.FE_COL_NAME,
              DEV_MSG.FE_COL_TYPE,
              DEV_MSG.FE_COL_SIZE,
            ]}
            rows={sortedEntries.map((e) => ({
              key: e.relativePath,
              dataAttrs: {
                "data-fe-name": e.name,
                "data-fe-path": e.relativePath,
                "data-fe-dir": e.directory ? "true" : "false",
              },
              onClick: e.directory ? () => setRelPath(e.relativePath) : undefined,
              cells: [
                e.directory ? (
                  <button
                    key="n"
                    type="button"
                    data-testid="developer-fe-open-dir"
                    data-fe-path={e.relativePath}
                    aria-label={`${DEV_MSG.FE_OPEN_DIR} ${e.name}`}
                    onClick={(ev) => {
                      ev.stopPropagation();
                      setRelPath(e.relativePath);
                    }}
                    style={openButtonStyle}
                  >
                    {e.name}
                  </button>
                ) : (
                  <span key="n" data-testid="developer-fe-file-name">
                    {e.name}
                  </span>
                ),
                <span key="t" style={mutedCell}>
                  {e.directory ? DEV_MSG.FE_TYPE_DIR : DEV_MSG.FE_TYPE_FILE}
                </span>,
                <span key="s" style={monoCell}>
                  {formatSize(e.size, e.directory)}
                </span>,
              ],
            }))}
          />
        )}
      </div>
    );
  }

  if (sortedRoots.length === 0) {
    return (
      <div data-testid="developer-fe-panel">
        <CatalogHint>{DEV_MSG.FE_HINT}</CatalogHint>
        <CatalogStatus testId="developer-fe-empty">{DEV_MSG.FE_EMPTY}</CatalogStatus>
      </div>
    );
  }

  return (
    <div data-testid="developer-fe-panel">
      <CatalogHint>{DEV_MSG.FE_HINT}</CatalogHint>
      <SimpleCatalogTable
        tableTestId="developer-fe-roots-table"
        rowTestId="developer-fe-root-row"
        columns={[
          DEV_MSG.FE_COL_DISPLAY,
          DEV_MSG.FE_COL_ID,
          DEV_MSG.FE_COL_EXISTS,
        ]}
        rows={sortedRoots.map((r) => ({
          key: r.id,
          dataAttrs: { "data-fe-root": r.id },
          onClick: () => {
            setRelPath("");
            setSelected(r);
          },
          cells: [
            <button
              key="open"
              type="button"
              data-testid="developer-fe-open-root"
              data-fe-root={r.id}
              aria-label={`${DEV_MSG.FE_OPEN_ROOT} ${r.displayName}`}
              onClick={(ev) => {
                ev.stopPropagation();
                setRelPath("");
                setSelected(r);
              }}
              style={openButtonStyle}
            >
              {r.displayName}
            </button>,
            <span key="id" style={monoCell}>
              {r.id}
            </span>,
            <span key="ex" style={mutedCell}>
              {r.exists === false ? DEV_MSG.FE_MISSING : DEV_MSG.FE_PRESENT}
            </span>,
          ],
        }))}
      />
    </div>
  );
}
