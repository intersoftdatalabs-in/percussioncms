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
  listDatabaseExplorerDatasources,
  listDatabaseExplorerTables,
  type DatabaseExplorerDatasource,
  type DatabaseExplorerTable,
} from "../api/developer/databaseExplorerApi";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { backButton, catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

/**
 * Developer Database Explorer — read-only allow-listed JDBC catalog browse (#4343).
 * Distinct from File Explorer (§12.1).
 */
export function DatabaseExplorerPanel(): React.ReactElement {
  const [datasources, setDatasources] = useState<DatabaseExplorerDatasource[] | null>(
    null,
  );
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<DatabaseExplorerDatasource | null>(null);
  const [tables, setTables] = useState<DatabaseExplorerTable[] | null>(null);
  const [tablesError, setTablesError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listDatabaseExplorerDatasources()
      .then((list) => {
        if (!cancelled) setDatasources(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.DBX_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!selected) {
      setTables(null);
      setTablesError(null);
      return;
    }
    let cancelled = false;
    setTables(null);
    setTablesError(null);
    listDatabaseExplorerTables(selected.id)
      .then((list) => {
        if (!cancelled) setTables(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setTablesError(panelErrMsg(e, DEV_MSG.DBX_TABLES_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [selected]);

  const sortedDatasources = useMemo(() => {
    if (!datasources) return [];
    return [...datasources].sort((a, b) =>
      a.displayName.localeCompare(b.displayName, undefined, { sensitivity: "base" }),
    );
  }, [datasources]);

  const sortedTables = useMemo(() => {
    if (!tables) return [];
    return [...tables].sort((a, b) => {
      if (a.type !== b.type) return a.type === "TABLE" ? -1 : 1;
      return a.name.localeCompare(b.name, undefined, { sensitivity: "base" });
    });
  }, [tables]);

  if (error) {
    return (
      <CatalogStatus testId="developer-dbx-error" error>
        {error}
      </CatalogStatus>
    );
  }
  if (datasources == null) {
    return (
      <CatalogStatus testId="developer-dbx-loading">{DEV_MSG.DBX_LOADING}</CatalogStatus>
    );
  }

  if (selected) {
    return (
      <div data-testid="developer-dbx-browse" data-dbx-ds={selected.id}>
        <button
          type="button"
          data-testid="developer-dbx-back-datasources"
          onClick={() => setSelected(null)}
          style={backButton}
        >
          {DEV_MSG.DBX_BACK_DS}
        </button>
        <CatalogHint>{DEV_MSG.DBX_BROWSE_HINT}</CatalogHint>
        <p
          data-testid="developer-dbx-selected"
          style={{ color: catalogColors.muted, marginBottom: "12px" }}
        >
          {selected.displayName}
        </p>
        {tablesError ? (
          <CatalogStatus testId="developer-dbx-tables-error" error>
            {tablesError}
          </CatalogStatus>
        ) : tables == null ? (
          <CatalogStatus testId="developer-dbx-tables-loading">
            {DEV_MSG.DBX_TABLES_LOADING}
          </CatalogStatus>
        ) : sortedTables.length === 0 ? (
          <CatalogStatus testId="developer-dbx-tables-empty">
            {DEV_MSG.DBX_TABLES_EMPTY}
          </CatalogStatus>
        ) : (
          <SimpleCatalogTable
            tableTestId="developer-dbx-tables-table"
            rowTestId="developer-dbx-table-row"
            columns={[DEV_MSG.DBX_COL_NAME, DEV_MSG.DBX_COL_TYPE, DEV_MSG.DBX_COL_SCHEMA]}
            rows={sortedTables.map((t) => ({
              key: `${t.schema ?? ""}:${t.name}`,
              dataAttrs: {
                "data-dbx-name": t.name,
                "data-dbx-type": t.type,
              },
              cells: [
                <span key="n" data-testid="developer-dbx-table-name" style={monoCell}>
                  {t.name}
                </span>,
                <span key="t" style={mutedCell}>
                  {t.type === "VIEW" ? DEV_MSG.DBX_TYPE_VIEW : DEV_MSG.DBX_TYPE_TABLE}
                </span>,
                <span key="s" style={mutedCell}>
                  {t.schema ?? "—"}
                </span>,
              ],
            }))}
          />
        )}
      </div>
    );
  }

  if (sortedDatasources.length === 0) {
    return (
      <div data-testid="developer-dbx-panel">
        <CatalogHint>{DEV_MSG.DBX_HINT}</CatalogHint>
        <CatalogStatus testId="developer-dbx-empty">{DEV_MSG.DBX_EMPTY}</CatalogStatus>
      </div>
    );
  }

  return (
    <div data-testid="developer-dbx-panel">
      <CatalogHint>{DEV_MSG.DBX_HINT}</CatalogHint>
      <SimpleCatalogTable
        tableTestId="developer-dbx-datasources-table"
        rowTestId="developer-dbx-datasource-row"
        columns={[
          DEV_MSG.DBX_COL_DISPLAY,
          DEV_MSG.DBX_COL_ID,
          DEV_MSG.DBX_COL_KIND,
          DEV_MSG.DBX_COL_AVAILABLE,
        ]}
        rows={sortedDatasources.map((d) => ({
          key: d.id,
          dataAttrs: { "data-dbx-ds": d.id },
          onClick: () => setSelected(d),
          cells: [
            <button
              key="open"
              type="button"
              data-testid="developer-dbx-open-datasource"
              data-dbx-ds={d.id}
              aria-label={`${DEV_MSG.DBX_OPEN_DS} ${d.displayName}`}
              onClick={(ev) => {
                ev.stopPropagation();
                setSelected(d);
              }}
              style={openButtonStyle}
            >
              {d.displayName}
            </button>,
            <span key="id" style={monoCell}>
              {d.id}
            </span>,
            <span key="k" style={mutedCell}>
              {d.repository ? DEV_MSG.DBX_REPOSITORY : DEV_MSG.DBX_OTHER}
            </span>,
            <span key="av" style={mutedCell}>
              {d.available === false ? DEV_MSG.DBX_MISSING : DEV_MSG.DBX_PRESENT}
            </span>,
          ],
        }))}
      />
    </div>
  );
}
