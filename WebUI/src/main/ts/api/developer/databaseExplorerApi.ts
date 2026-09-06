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

import { get } from "../client";
import { asJsonRecord, asObjectArray } from "../jsonList";
import { PATHS } from "../paths";

/**
 * Allow-listed Database Explorer datasource (GET /services/databaseexplorer).
 * Catalog {@code id} is the API key — never a JDBC URL.
 */
export type DatabaseExplorerDatasource = {
  id: string;
  displayName: string;
  repository?: boolean;
  available?: boolean;
};

/**
 * Table or view under an allow-listed datasource (GET …/{id}/tables).
 */
export type DatabaseExplorerTable = {
  name: string;
  type: "TABLE" | "VIEW";
  schema?: string;
};

/** Matches REST DatabaseExplorerAdaptor SAFE_CATALOG_ID. */
export const DATABASE_EXPLORER_ID_RE = /^[A-Za-z][A-Za-z0-9_-]{0,63}$/;

/** Matches REST SAFE_SQL_IDENT for table/view/schema names. */
export const DATABASE_EXPLORER_IDENT_RE = /^[A-Za-z][A-Za-z0-9_$#]{0,127}$/;

export function isSafeDatabaseExplorerId(datasourceId: string): boolean {
  return DATABASE_EXPLORER_ID_RE.test(datasourceId);
}

export function isSafeDatabaseExplorerIdent(name: string): boolean {
  return DATABASE_EXPLORER_IDENT_RE.test(name);
}

function parseNamedList(payload: unknown, names: readonly string[]): unknown[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload;
  const obj = asJsonRecord(payload);
  if (!obj) {
    return [];
  }
  for (const key of names) {
    const raw = obj[key];
    if (raw == null) continue;
    if (Array.isArray(raw)) return raw;
    if (typeof raw === "object") return [raw];
  }
  if (typeof obj.id === "string" || typeof obj.name === "string") {
    return [obj];
  }
  return asObjectArray(payload);
}

function asOptionalBoolean(value: unknown): boolean | undefined {
  if (typeof value === "boolean") return value;
  return undefined;
}

export function unwrapDatabaseExplorerDatasources(
  payload: unknown,
): DatabaseExplorerDatasource[] {
  const raw = parseNamedList(payload, [
    "DatabaseExplorerDatasource",
    "databaseExplorerDatasource",
    "datasources",
  ]);
  const out: DatabaseExplorerDatasource[] = [];
  for (const item of raw) {
    const rec = asJsonRecord(item);
    if (!rec) continue;
    const id = typeof rec.id === "string" ? rec.id.trim() : "";
    if (!id || !isSafeDatabaseExplorerId(id)) continue;
    const display =
      typeof rec.displayName === "string" && rec.displayName.trim()
        ? rec.displayName.trim()
        : id;
    out.push({
      id,
      displayName: display,
      repository: asOptionalBoolean(rec.repository),
      available: asOptionalBoolean(rec.available),
    });
  }
  return out;
}

export function unwrapDatabaseExplorerTables(payload: unknown): DatabaseExplorerTable[] {
  const raw = parseNamedList(payload, [
    "DatabaseExplorerTable",
    "databaseExplorerTable",
    "tables",
  ]);
  const out: DatabaseExplorerTable[] = [];
  for (const item of raw) {
    const rec = asJsonRecord(item);
    if (!rec) continue;
    const name = typeof rec.name === "string" ? rec.name.trim() : "";
    if (!name || !isSafeDatabaseExplorerIdent(name)) continue;
    const typeRaw = typeof rec.type === "string" ? rec.type.trim().toUpperCase() : "";
    const type = typeRaw === "VIEW" ? "VIEW" : typeRaw === "TABLE" ? "TABLE" : null;
    if (!type) continue;
    const schemaRaw = typeof rec.schema === "string" ? rec.schema.trim() : "";
    const schema =
      schemaRaw && isSafeDatabaseExplorerIdent(schemaRaw) ? schemaRaw : undefined;
    out.push({ name, type, schema });
  }
  return out;
}

export function databaseExplorerTablesUrl(datasourceId: string): string {
  return `${PATHS.DATABASE_EXPLORER}/${encodeURIComponent(datasourceId)}/tables`;
}

/** GET /services/databaseexplorer — Admin allow-listed datasources (ids only). */
export async function listDatabaseExplorerDatasources(): Promise<
  DatabaseExplorerDatasource[]
> {
  return unwrapDatabaseExplorerDatasources(await get<unknown>(PATHS.DATABASE_EXPLORER));
}

/** GET /services/databaseexplorer/{id}/tables — Admin tables/views. */
export async function listDatabaseExplorerTables(
  datasourceId: string,
): Promise<DatabaseExplorerTable[]> {
  if (!isSafeDatabaseExplorerId(datasourceId)) {
    throw new Error("Invalid Database Explorer datasource");
  }
  return unwrapDatabaseExplorerTables(
    await get<unknown>(databaseExplorerTablesUrl(datasourceId)),
  );
}
