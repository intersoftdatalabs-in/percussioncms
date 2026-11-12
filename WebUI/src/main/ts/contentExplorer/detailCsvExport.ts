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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Client-side CSV export for the modern Content Explorer detail list
 * (#6079 / parent #4530).
 *
 * <p>Exports the rows already on screen — the visible display-format
 * columns in their rendered order — with zero new REST traffic. Cell
 * text comes from {@code renderDisplayFormatCell}, so the file matches
 * the current view including the active client-side sort and the
 * operator's column-visibility choices. Only the loaded page is
 * exported; paging stays server-side.</p>
 *
 * <p>Quoting follows RFC 4180: a field containing a comma, quote,
 * carriage return, or line feed is wrapped in double quotes with inner
 * quotes doubled. Rows are joined with CRLF.</p>
 */

/**
 * Escape one CSV field per RFC 4180. Nullish values become empty
 * fields; fields containing a comma, quote, CR, or LF are wrapped in
 * double quotes with inner quotes doubled. Pure so Vitest can drive
 * it without rendering.
 */
export function csvEscapeCell(value: string | null | undefined): string {
  const text = value == null ? "" : String(value);
  if (
    text.includes(",") ||
    text.includes('"') ||
    text.includes("\r") ||
    text.includes("\n")
  ) {
    return `"${text.replace(/"/g, '""')}"`;
  }
  return text;
}

/**
 * Build the CSV document for one export. Headers are the human-readable
 * column labels in display order; each row is the rendered cell text
 * for the same columns. Rows are joined with CRLF and the document
 * ends with a trailing CRLF. Pure so Vitest can drive it without
 * rendering.
 */
export function buildDetailCsv(
  headers: readonly string[],
  rows: ReadonlyArray<ReadonlyArray<string | null | undefined>>,
): string {
  const lines = [
    headers.map(csvEscapeCell).join(","),
    ...rows.map((row) => row.map(csvEscapeCell).join(",")),
  ];
  return lines.join("\r\n") + "\r\n";
}

/**
 * Basename for the download, derived from the open folder's leaf
 * segment: control chars, quotes, and path separators become
 * underscores (never a filesystem path join). A blank or missing path
 * falls back to {@code explorer-folder.csv}. Pure so Vitest can drive
 * it without rendering.
 */
export function detailCsvFileName(folderPath: string | null | undefined): string {
  const raw = folderPath == null ? "" : String(folderPath);
  const segments = raw.split(/[\\/]/).filter((part) => part.length > 0);
  const leaf = segments.length > 0 ? segments[segments.length - 1] : "";
  let base = "";
  for (let i = 0; i < leaf.length; i++) {
    const c = leaf.charAt(i);
    const code = leaf.charCodeAt(i);
    if (
      code <= 31 ||
      code === 127 ||
      c === '"' ||
      c === "\\" ||
      c === "/" ||
      c === ":"
    ) {
      base += "_";
    } else {
      base += c;
    }
  }
  base = base.trim();
  if (!base) {
    base = "folder";
  }
  return `explorer-${base}.csv`;
}

/**
 * Trigger a client-side download of the CSV document. Mirrors the
 * developer import/export download helper: prefers an object URL and
 * falls back to a data URI; revocation is deferred so Firefox can
 * start the download. No-op outside a DOM runtime.
 */
export function downloadCsvFile(csv: string, folderPath: string): void {
  if (typeof document === "undefined") {
    return;
  }
  const name = detailCsvFileName(folderPath);
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  let href = `data:text/csv;charset=utf-8,${encodeURIComponent(csv)}`;
  let objectUrl: string | null = null;
  if (typeof URL !== "undefined" && typeof URL.createObjectURL === "function") {
    objectUrl = URL.createObjectURL(blob);
    href = objectUrl;
  }
  const a = document.createElement("a");
  a.href = href;
  a.download = name;
  a.rel = "noopener";
  a.style.display = "none";
  document.body.appendChild(a);
  a.click();
  a.remove();
  if (objectUrl && typeof URL.revokeObjectURL === "function") {
    const toRevoke = objectUrl;
    globalThis.setTimeout(() => {
      URL.revokeObjectURL(toRevoke);
    }, 1000);
  }
}
