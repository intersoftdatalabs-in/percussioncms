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
 * Pure helpers for Explorer preview + View residual Playwright (#2733).
 *
 * <p>No live CMS dependency — unit-tested via node:test.</p>
 */

"use strict";

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  tree: "explorer-tree",
  list: "detail-list",
  preview: "action-preview",
  refresh: "explorer-refresh-list",
  viewTools: "explorer-view-tools",
  reducedActions: "reduced-actions",
});

/**
 * Build spa.jsp explorer entry URL with optional cache buster.
 * @param {string} baseUrl CMS base (with or without trailing slash)
 * @param {{ cacheBuster?: string|number }} [opts]
 * @returns {string}
 */
function explorerEntryUrl(baseUrl, opts = {}) {
  const base = String(baseUrl || "").replace(/\/+$/, "");
  const bust =
    opts.cacheBuster != null
      ? encodeURIComponent(String(opts.cacheBuster))
      : String(Date.now());
  return `${base}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${bust}`;
}

/**
 * Page render preview path for a content id (mirrors WebUI PAGE_PREVIEW).
 * @param {string} servicesRoot e.g. /Rhythmyx/services or /services
 * @param {string} contentId
 * @returns {string}
 */
function pageRenderPreviewPath(servicesRoot, contentId) {
  const root = String(servicesRoot || "/services").replace(/\/+$/, "");
  const id = String(contentId || "").trim();
  if (!id) return "";
  return `${root}/pagemanagement/render/page/${encodeURIComponent(id)}`;
}

/**
 * Site-path friendly preview URL (Finder selection path).
 * @param {string} cmsPath
 * @param {{ mobilePreview?: boolean, revisionId?: string|number }} [opts]
 * @returns {string}
 */
function sitePathPreviewUrl(cmsPath, opts = {}) {
  let p = String(cmsPath || "")
    .trim()
    .replace(/\\/g, "/");
  while (p.startsWith("//")) p = p.slice(1);
  if (p && !p.startsWith("/")) p = `/${p}`;
  if (!p.toLowerCase().startsWith("/sites/")) return "";
  const q = new URLSearchParams();
  if (opts.revisionId != null && String(opts.revisionId).length > 0) {
    q.set("sys_revision", String(opts.revisionId));
  }
  q.set(
    "percmobilepreview",
    String(opts.mobilePreview === undefined ? false : Boolean(opts.mobilePreview)),
  );
  return `${p}?${q.toString()}`;
}

/**
 * Soft-skip message when H2 fixture has no previewable list row.
 * @returns {string}
 */
function noPreviewableItemSkipMessage() {
  return "H2 fixture has no previewable content row for Explorer Preview; chrome + unit coverage still apply (#2733)";
}

/**
 * Heuristic: row looks like a non-folder content item (page/asset).
 * @param {{ type?: string, category?: string, path?: string, id?: string }} row
 * @returns {boolean}
 */
function isPreviewableRow(row) {
  if (!row) return false;
  const token = `${row.type || ""} ${row.category || ""}`.toLowerCase();
  const path = String(row.path || "").toLowerCase();
  if (token.includes("folder") || path.endsWith("/")) return false;
  if (token.includes("page") || path.includes("/sites/")) return true;
  if (token.includes("asset") || path.includes("/assets/")) {
    return Boolean(row.id);
  }
  return Boolean(row.id);
}

module.exports = {
  TEST_IDS,
  explorerEntryUrl,
  pageRenderPreviewPath,
  sitePathPreviewUrl,
  noPreviewableItemSkipMessage,
  isPreviewableRow,
};
