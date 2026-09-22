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
 * Session column overlay for the current Explorer folder (#4722).
 * PUT/GET {@code /services/explorer/list-columns}. Does not rewrite the
 * shared display format.
 */

import { get, isApiError, put } from "../client";
import { PATHS } from "../paths";
import type { DisplayFormatColumn } from "./displayFormatsApi";

export interface ExplorerListColumns {
  folderPath: string;
  columns: string[];
}

export function unwrapExplorerListColumns(payload: unknown): ExplorerListColumns {
  const root =
    payload && typeof payload === "object"
      ? (payload as Record<string, unknown>)
      : {};
  const wrapped = root.ExplorerListColumns;
  const body =
    wrapped && typeof wrapped === "object"
      ? (wrapped as Record<string, unknown>)
      : root;
  const folderPath = typeof body.folderPath === "string" ? body.folderPath : "";
  const raw = body.columns;
  const columns = Array.isArray(raw)
    ? raw.filter((c): c is string => typeof c === "string" && c.trim().length > 0)
    : [];
  return { folderPath, columns };
}

export function columnsToDisplayFormat(
  sources: readonly string[],
): DisplayFormatColumn[] {
  return sources.map((source) => ({ source, label: source }));
}

export function listColumnsHttpStatus(err: unknown): number | null {
  return isApiError(err) ? err.status : null;
}

export async function getExplorerListColumns(
  folderPath: string,
): Promise<ExplorerListColumns> {
  const q = new URLSearchParams();
  q.set("folderPath", folderPath);
  const payload = await get<unknown>(
    `${PATHS.EXPLORER_LIST_COLUMNS}?${q.toString()}`,
  );
  return unwrapExplorerListColumns(payload);
}

export async function saveExplorerListColumns(
  folderPath: string,
  columns: readonly string[],
): Promise<ExplorerListColumns> {
  const payload = await put<unknown>(PATHS.EXPLORER_LIST_COLUMNS, {
    ExplorerListColumns: { folderPath, columns },
  });
  return unwrapExplorerListColumns(payload);
}
