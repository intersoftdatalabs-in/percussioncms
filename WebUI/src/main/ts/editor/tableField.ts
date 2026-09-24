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
 * Wire format for a {@code sys_Table} field on the itemmanagement fields API.
 * Empty default grid is {@code ""}. Otherwise JSON
 * {@code {"columns":["…"],"rows":[["…"]]}}. A non-JSON string is one cell so
 * a value previously saved as text is not dropped.
 */

export interface EditorTableModel {
  columns: string[];
  rows: string[][];
}

const DEFAULT_COLUMN = "value";

function asColumns(raw: unknown): string[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  const names = raw
    .map((cell) => String(cell ?? "").trim())
    .filter((name) => name.length > 0);
  return names.length > 0 ? names : [];
}

function fitRow(cells: unknown, width: number): string[] {
  const list = Array.isArray(cells) ? cells : [cells];
  const out: string[] = [];
  for (let i = 0; i < width; i += 1) {
    const cell = list[i];
    out.push(cell == null ? "" : String(cell));
  }
  return out;
}

export function parseTableField(raw: string | null | undefined): EditorTableModel {
  const text = (raw ?? "").trim();
  if (!text) {
    return { columns: [DEFAULT_COLUMN], rows: [] };
  }
  if (text.startsWith("{")) {
    try {
      const parsed = JSON.parse(text) as { columns?: unknown; rows?: unknown };
      const columns = asColumns(parsed.columns);
      const cols = columns.length > 0 ? columns : [DEFAULT_COLUMN];
      const rowsIn = Array.isArray(parsed.rows) ? parsed.rows : [];
      return {
        columns: cols,
        rows: rowsIn.map((row) => fitRow(row, cols.length)),
      };
    } catch {
      /* keep the raw string as a single cell */
    }
  }
  return { columns: [DEFAULT_COLUMN], rows: [[text]] };
}

export function tableFieldHasCells(model: EditorTableModel): boolean {
  return model.rows.some((row) => row.some((cell) => cell.trim() !== ""));
}

export function tableFieldIsEmpty(raw: string | null | undefined): boolean {
  return !tableFieldHasCells(parseTableField(raw));
}

/** Stable JSON, or {@code ""} when the default one-column grid has no text. */
export function serializeTableField(model: EditorTableModel): string {
  const columns = model.columns.length > 0 ? [...model.columns] : [DEFAULT_COLUMN];
  const rows = model.rows.map((row) => fitRow(row, columns.length));
  if (
    rows.length === 0 &&
    columns.length === 1 &&
    columns[0] === DEFAULT_COLUMN
  ) {
    return "";
  }
  return JSON.stringify({ columns, rows });
}
