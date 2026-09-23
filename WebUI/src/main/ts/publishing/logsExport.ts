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

import type { PublishingLogEntry } from "./types";

/** Columns that match the Logs table (job id identifies the row). */
export const PUBLISHING_LOG_CSV_HEADERS = [
  "Job",
  "Site",
  "Server",
  "Status",
] as const;

export const PUBLISHING_LOGS_EXPORT_FILENAME = "publish-logs.csv";

function csvCell(value: string): string {
  if (/[",\r\n]/.test(value)) {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return value;
}

function cell(value: unknown): string {
  if (value == null) {
    return "";
  }
  return csvCell(String(value));
}

/**
 * CSV of the rows currently shown in Logs. An empty list is a header-only file.
 * Throws when the input is not a list so the UI can show a build failure.
 */
export function buildPublishingLogsCsv(entries: PublishingLogEntry[]): string {
  if (!Array.isArray(entries)) {
    throw new Error("publishing logs export requires a list");
  }
  const lines = [PUBLISHING_LOG_CSV_HEADERS.join(",")];
  for (const log of entries) {
    const server = log?.serverName ?? log?.pubServerName ?? "";
    lines.push(
      [cell(log?.jobId), cell(log?.siteName), cell(server), cell(log?.status)].join(
        ",",
      ),
    );
  }
  return `${lines.join("\r\n")}\r\n`;
}

/**
 * Trigger a browser download of the CSV text. Throws when the browser APIs
 * needed to build the file are missing.
 */
export function downloadPublishingLogsCsv(
  csv: string,
  filename: string = PUBLISHING_LOGS_EXPORT_FILENAME,
): void {
  if (typeof csv !== "string" || csv.length === 0) {
    throw new Error("publishing logs export is empty");
  }
  if (
    typeof document === "undefined" ||
    typeof URL === "undefined" ||
    typeof URL.createObjectURL !== "function" ||
    typeof Blob !== "function"
  ) {
    throw new Error("publishing logs download is unavailable");
  }
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const objectUrl = URL.createObjectURL(blob);
  try {
    const anchor = document.createElement("a");
    anchor.href = objectUrl;
    anchor.download = filename;
    anchor.rel = "noopener";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}
