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
 * Developer File Explorer browse surface helpers (#4327 / parent #1690).
 * No live CMS. REST relative paths use `/` (not OS separators).
 */

"use strict";

const TEST_IDS = Object.freeze({
  tab: "tab-developer-file-explorer",
  panel: "developer-fe-panel",
  empty: "developer-fe-empty",
  error: "developer-fe-error",
  loading: "developer-fe-loading",
  rootsTable: "developer-fe-roots-table",
  rootRow: "developer-fe-root-row",
  openRoot: "developer-fe-open-root",
  browse: "developer-fe-browse",
  breadcrumb: "developer-fe-breadcrumb",
  childrenTable: "developer-fe-children-table",
  childrenEmpty: "developer-fe-children-empty",
  childrenError: "developer-fe-children-error",
  openDir: "developer-fe-open-dir",
  backRoots: "developer-fe-back-roots",
  up: "developer-fe-up",
});

/**
 * @param {string} baseUrl TEST_CMS_URL / BASE_URL
 * @returns {string}
 */
function developerFileExplorerUrl(baseUrl) {
  const base = String(baseUrl || "").replace(/\/+$/, "");
  const q = new URLSearchParams({
    entry: "developer",
    section: "file-explorer",
    _: String(Date.now()),
  });
  return `${base}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * Unwrap Jackson list or `{ FileExplorerRoot: [...] }` catalog payload.
 * @param {unknown} payload
 * @returns {Array<{ id: string, displayName?: string, exists?: boolean }>}
 */
function unwrapFileExplorerRoots(payload) {
  if (payload == null) return [];
  let raw = payload;
  if (!Array.isArray(raw) && typeof raw === "object") {
    const rec = /** @type {Record<string, unknown>} */ (raw);
    if (Array.isArray(rec.FileExplorerRoot)) {
      raw = rec.FileExplorerRoot;
    } else if (rec.FileExplorerRoot && typeof rec.FileExplorerRoot === "object") {
      raw = [rec.FileExplorerRoot];
    } else if (Array.isArray(rec.fileExplorerRoot)) {
      raw = rec.fileExplorerRoot;
    } else if (typeof rec.id === "string") {
      raw = [rec];
    } else {
      return [];
    }
  }
  if (!Array.isArray(raw)) return [];
  const out = [];
  for (const item of raw) {
    if (!item || typeof item !== "object") continue;
    const id = typeof item.id === "string" ? item.id.trim() : "";
    if (!id || !/^[A-Za-z][A-Za-z0-9_-]{0,63}$/.test(id)) continue;
    out.push(item);
  }
  return out;
}

/**
 * Browser console errors that are not favicon / failed-resource noise.
 * @param {string[]} consoleErrors
 * @returns {string[]}
 */
function unexpectedConsoleErrors(consoleErrors) {
  if (!Array.isArray(consoleErrors)) return [];
  return consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
}

module.exports = {
  TEST_IDS,
  developerFileExplorerUrl,
  unwrapFileExplorerRoots,
  unexpectedConsoleErrors,
};
