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
 * Fresh H2 Publishing workflow spec (#5075).
 *
 * GET /services/sites does not echo folderRoot even when the QA image seeded
 * Corporate_Investments under //Sites. Finder POST /folders/create also
 * rejects that root ("not a folder"). The real folder is created with
 * content-explorer addFolder when no image site is present.
 */

"use strict";

/** Image sites that already have a //Sites folder when demo-sites is on. */
const SEEDED_SITE_NAMES = Object.freeze([
  "Corporate_Investments",
  "Enterprise_Investments",
]);

/**
 * @param {unknown} payload
 * @returns {string[]}
 */
function siteNames(payload) {
  if (!payload || typeof payload !== "object") {
    return [];
  }
  const rec = /** @type {Record<string, unknown>} */ (payload);
  const list = rec.SiteList || rec.siteList || rec.sites;
  const rows = Array.isArray(list)
    ? list
    : Array.isArray(payload)
      ? payload
      : [];
  const names = [];
  for (const row of rows) {
    if (!row || typeof row !== "object") {
      continue;
    }
    const name = /** @type {Record<string, unknown>} */ (row).name;
    if (typeof name === "string" && name.trim()) {
      names.push(name.trim());
    }
  }
  return names;
}

/**
 * QA-image site to open, or "" when the cell has none (#5075).
 * Does not look at folderRoot — that field is omitted on the wire.
 * @param {unknown} payload
 * @returns {string}
 */
function seededSiteName(payload) {
  const names = new Set(siteNames(payload));
  for (const seeded of SEEDED_SITE_NAMES) {
    if (names.has(seeded)) {
      return seeded;
    }
  }
  return "";
}

/**
 * Path from folder-create JSON (Folder, RxFolder, or flat).
 * @param {unknown} payload
 * @returns {string}
 */
function createdFolderPath(payload) {
  if (!payload || typeof payload !== "object") {
    return "";
  }
  const rec = /** @type {Record<string, unknown>} */ (payload);
  const wrapped = rec.RxFolder || rec.Folder;
  const inner =
    wrapped && typeof wrapped === "object" && !Array.isArray(wrapped)
      ? /** @type {Record<string, unknown>} */ (wrapped)
      : rec;
  return typeof inner.path === "string" ? inner.path.trim() : "";
}

module.exports = {
  SEEDED_SITE_NAMES,
  siteNames,
  seededSiteName,
  createdFolderPath,
};
