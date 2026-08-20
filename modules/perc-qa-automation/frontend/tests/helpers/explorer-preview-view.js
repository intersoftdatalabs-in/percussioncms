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
 * Skip only when REST listing also has no page-type children (listing not on
 * tip / #3457 not recovered). Do not use when Pages already lists a page.
 * @returns {string}
 */
function noListedPageSkipMessage() {
  return (
    "No listed page-type child under Sites/Pages after REST walk; " +
    "Preview open requires a page row (parent #2745 / slice #3456 / #3627). " +
    "If sample-site Pages listing is on the tip (#3457), this skip is a defect."
  );
}

/**
 * QA H2 matrix ({@code TEST_DB_TYPE=h2}) must not soft-skip Preview.
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} [env]
 * @returns {boolean}
 */
function isH2QaEnv(env = process.env) {
  const db = String(env.TEST_DB_TYPE || env.TEST_DATABASE || "")
    .trim()
    .toLowerCase();
  return db === "h2";
}

/**
 * Soft-skip Preview open only when this is not H2 QA, REST found no page,
 * and the Explorer list has no previewable row. H2 demo-sites fail instead
 * of skip (#3627).
 *
 * @param {{
 *   listedPage?: unknown,
 *   previewableRowCount?: number,
 *   uiHasPreviewableRow?: boolean,
 *   h2?: boolean,
 * }} [detail]
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} [env]
 * @returns {boolean}
 */
function shouldSkipListedPagePreview(detail = {}, env = process.env) {
  if (detail.listedPage) {
    return false;
  }
  if ((detail.previewableRowCount || 0) > 0) {
    return false;
  }
  if (detail.uiHasPreviewableRow === true) {
    return false;
  }
  if (detail.h2 === true || isH2QaEnv(env)) {
    return false;
  }
  return true;
}

/**
 * Encode a CMS relative path for pathmanagement URLs (spaces in site names).
 * Logical CMS paths use {@code /}; this is URL encoding, not OS join.
 * @param {string} cmsPath
 * @returns {string}
 */
function encodeCmsRelPath(cmsPath) {
  return String(cmsPath || "")
    .replace(/^\/+/, "")
    .replace(/\/+$/, "")
    .split("/")
    .filter(Boolean)
    .map((seg) => encodeURIComponent(seg))
    .join("/");
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
  if (token.includes("folder") || token.includes("site") || path.endsWith("/")) {
    return false;
  }
  if (token.includes("page") || path.includes("/sites/")) return true;
  if (token.includes("asset") || path.includes("/assets/")) {
    return Boolean(row.id);
  }
  return Boolean(row.id);
}

/**
 * Stock FastForward asset types — same set as WebUI
 * {@code ASSET_PREVIEW_TYPE_KEYS} in {@code previewItem.ts}. These are assets
 * even when listed under {@code /Sites/}.
 */
const ASSET_PREVIEW_TYPE_KEYS = new Set([
  "asset",
  "percasset",
  "rffimage",
  "rfffile",
]);

/**
 * Listed Explorer page (percPage / Page) — folders stay false.
 * @param {{ type?: string, category?: string, path?: string, id?: string, name?: string }} row
 * @returns {boolean}
 */
function isListedPageRow(row) {
  if (!row) return false;
  const token = `${row.type || ""} ${row.category || ""}`.toLowerCase();
  const path = String(row.path || "").replace(/\\/g, "/").toLowerCase();
  if (
    token.includes("folder") ||
    token.includes("fsfolder") ||
    token.includes("site") ||
    path.endsWith("/")
  ) {
    return false;
  }
  const type = String(row.type || "").trim().toLowerCase();
  if (ASSET_PREVIEW_TYPE_KEYS.has(type)) {
    return false;
  }
  if (token.includes("page") || token.includes("rffhome") || token.includes("landing_page")) {
    return true;
  }
  if (path.includes("/pages/") && Boolean(row.id)) return true;
  // Customer types keep their own names after upgrade (#3456).
  if (Boolean(row.id) && path.includes("/sites/") && type.length > 0) {
    return true;
  }
  return false;
}

/**
 * Unwrap pathmanagement folder or paginatedFolder JSON to item objects.
 * Accepts {@code PathItem} array or single object, nested
 * {@code PSPathItemList}, and {@code PagedItemList.childrenInPage}
 * (array or single) so H2 sample pages are not treated as empty (#3627).
 * @param {unknown} body
 * @returns {object[]}
 */
function unwrapPathItems(body) {
  if (body == null) return [];
  if (Array.isArray(body)) return body;
  if (typeof body !== "object") return [];
  const root = /** @type {Record<string, unknown>} */ (body);
  const pagedWrap =
    root.PagedItemList && typeof root.PagedItemList === "object"
      ? /** @type {Record<string, unknown>} */ (root.PagedItemList)
      : null;
  if (pagedWrap) {
    const kids = pagedWrap.childrenInPage ?? pagedWrap.children;
    if (kids == null) return [];
    return Array.isArray(kids) ? kids : [kids];
  }
  const direct = root.PathItem ?? root.pathItem;
  if (direct != null) {
    return Array.isArray(direct) ? direct : [direct];
  }
  const nested = root.PSPathItemList ?? root.PathItemList ?? root.pathItemList;
  if (nested != null && nested !== root) {
    return unwrapPathItems(nested);
  }
  return [];
}

/**
 * Pathmanagement list path: prefer repository {@code folderPath}
 * ({@code //Sites/CorporateInvestments}) over finder site-name path
 * ({@code /Sites/Corporate_Investments/}) — peer of WebUI
 * {@code resolveExplorerListPath} (#3326 / #3457).
 * @param {{ path?: string, folderPath?: string }} item
 * @returns {string}
 */
function resolveExplorerListPath(item) {
  const raw = (item && (item.folderPath || item.path)) || "";
  let p = String(raw).trim().replace(/\\/g, "/");
  while (p.startsWith("//")) p = p.slice(1);
  if (p && !p.startsWith("/")) p = `/${p}`;
  if (p.length > 1 && p.endsWith("/")) p = p.replace(/\/+$/, "");
  return p;
}

/**
 * Parent CMS folder path for a listed item (logical `/` paths, not OS).
 * @param {string} itemPath
 * @returns {string}
 */
function parentFolderCmsPath(itemPath) {
  let p = String(itemPath || "")
    .trim()
    .replace(/\\/g, "/");
  while (p.startsWith("//")) p = p.slice(1);
  if (p && !p.startsWith("/")) p = `/${p}`;
  if (p.length > 1 && p.endsWith("/")) p = p.replace(/\/+$/, "");
  const idx = p.lastIndexOf("/");
  if (idx <= 0) return p || "/";
  return p.slice(0, idx) || "/";
}

/**
 * Whether a popup URL is a product page preview (render or site-path).
 * @param {string} url
 * @returns {boolean}
 */
function isProductPagePreviewUrl(url) {
  const u = String(url || "").replace(/\\/g, "/").toLowerCase();
  if (!u) return false;
  if (u.includes("/pagemanagement/render/page/")) return true;
  if (u.includes("/assembler/render")) return true;
  if (u.includes("percmobilepreview=")) return true;
  // Product editor-or-preview host (#3627) — spa.jsp?entry=editor&mode=view.
  if (u.includes("entry=editor") && u.includes("mode=view")) return true;
  // Classic CE preview requires the command — not any URL with "preview"
  // near /psx_ce (e.g. /psx_ce/admin/preview-settings).
  if (u.includes("sys_command=preview")) return true;
  return false;
}

/**
 * Fold finder / repository site names so spaces and underscores match.
 * @param {string} name
 * @returns {string}
 */
function foldSiteName(name) {
  return String(name || "")
    .toLowerCase()
    .replace(/[_\s-]+/g, "");
}

/**
 * Site folder names for a listed page (finder underscore + repository).
 * @param {{ path?: string, folderPath?: string }} listed
 * @returns {string[]}
 */
function listedPageSiteNames(listed) {
  if (!listed) return [];
  const names = [];
  const seen = new Set();
  for (const raw of [listed.folderPath, listed.path]) {
    if (!raw) continue;
    const p = resolveExplorerListPath({ path: String(raw) });
    const parts = p.replace(/^\/+/, "").split("/").filter(Boolean);
    if (parts.length < 2 || parts[0].toLowerCase() !== "sites") continue;
    const name = parts[1];
    const key = foldSiteName(name);
    if (!name || seen.has(key)) continue;
    seen.add(key);
    names.push(name);
  }
  return names;
}

/**
 * True when any line of a detail-row {@code innerText} equals {@code name}.
 * Whole-row regex {@code /^Pages$/} fails because rows also include Type
 * and Path cells (#3463).
 * @param {string} rowText
 * @param {string} name
 * @returns {boolean}
 */
function detailRowHasExactName(rowText, name) {
  const want = String(name || "").trim();
  if (!want) return false;
  return String(rowText || "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .some((line) => line === want);
}

/**
 * Folded site-name match against a detail-row's full text (finder
 * underscores vs repository names).
 * @param {string} rowText
 * @param {Iterable<string>} wantedFolded
 * @returns {boolean}
 */
function detailRowMatchesFoldedSite(rowText, wantedFolded) {
  const folded = foldSiteName(rowText);
  const wanted = [...(wantedFolded || [])].filter(Boolean);
  if (wanted.length === 0) return false;
  return wanted.some((n) => folded.includes(n));
}

module.exports = {
  TEST_IDS,
  explorerEntryUrl,
  pageRenderPreviewPath,
  sitePathPreviewUrl,
  noPreviewableItemSkipMessage,
  noListedPageSkipMessage,
  isH2QaEnv,
  shouldSkipListedPagePreview,
  encodeCmsRelPath,
  isPreviewableRow,
  isListedPageRow,
  unwrapPathItems,
  resolveExplorerListPath,
  parentFolderCmsPath,
  isProductPagePreviewUrl,
  listedPageSiteNames,
  foldSiteName,
  detailRowHasExactName,
  detailRowMatchesFoldedSite,
};
