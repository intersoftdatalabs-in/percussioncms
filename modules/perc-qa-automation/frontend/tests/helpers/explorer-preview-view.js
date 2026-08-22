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
 * Workflow checkIn path for a content id (mirrors WebUI ITEM_WORKFLOW_CHECKIN).
 * Bare numeric ids such as FastForward {@code 594} are valid (#3688).
 * @param {string} servicesRoot e.g. /Rhythmyx/services or /services
 * @param {string} contentId
 * @returns {string}
 */
function workflowCheckInPath(servicesRoot, contentId) {
  const root = String(servicesRoot || "/services").replace(/\/+$/, "");
  const id = String(contentId || "").trim();
  if (!id) return "";
  return `${root}/itemmanagement/workflow/checkIn/${encodeURIComponent(id)}`;
}

/**
 * Bare numeric content id from a path item id or hyphenated GUID.
 * @param {unknown} id
 * @returns {string} empty when no numeric uuid is present
 */
function numericContentIdFromItemId(id) {
  const s = String(id || "").trim();
  if (/^\d+$/.test(s)) return s;
  const parts = s.split("-");
  const last = parts[parts.length - 1];
  if (parts.length >= 3 && /^\d+$/.test(last)) {
    return last;
  }
  return "";
}

/**
 * Prefer FastForward Corporate Investments listed page (content id 594).
 * @param {object[]} pages
 * @returns {object|null}
 */
function pickPreferredListedPage(pages) {
  const list = Array.isArray(pages) ? pages.filter(Boolean) : [];
  if (list.length === 0) return null;
  const by594 = list.find((p) => numericContentIdFromItemId(p.id) === "594");
  if (by594) return by594;
  const bySite = list.find((p) =>
    listedPageSiteNames(p)
      .map(foldSiteName)
      .some((n) => n.includes("corporateinvestments")),
  );
  return bySite || list[0];
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
  return `/${encodeCmsRelPath(p)}?${q.toString()}`;
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
 * React Content Editor host — not a product page preview (#3716).
 * @param {string} url
 * @returns {boolean}
 */
function isEditorHostPreviewUrl(url) {
  const u = String(url || "").replace(/\\/g, "/").toLowerCase();
  if (!u) return false;
  if (/\/cm\/app\/editor(?:[/?#&]|$)/.test(u)) return true;
  if (/[?&]entry=editor(?:[&#]|$)/.test(u)) return true;
  return false;
}

/**
 * Whether a popup URL is a product page preview (render or site-path).
 * The React editor host is not a product preview (#3716).
 * @param {string} url
 * @returns {boolean}
 */
function isProductPagePreviewUrl(url) {
  const u = String(url || "").replace(/\\/g, "/").toLowerCase();
  if (!u) return false;
  if (isEditorHostPreviewUrl(u)) return false;
  if (u.includes("/pagemanagement/render/page/")) return true;
  if (u.includes("/assembler/render")) return true;
  if (u.includes("percmobilepreview=")) return true;
  // Classic CE preview requires the command — not any URL with "preview"
  // near /psx_ce (e.g. /psx_ce/admin/preview-settings).
  if (u.includes("sys_command=preview")) return true;
  return false;
}

/**
 * True when assembled preview HTML is usable (not NPE text, not a JSP compile
 * failure, not a blank body). Used for rffHome site-path assembly (#3719).
 * @param {string} body
 * @returns {boolean}
 */
function isAssembledPreviewHtml(body) {
  const text = String(body || "");
  if (!text.trim()) return false;
  const lower = text.toLowerCase();
  if (lower.includes("the validated object is null")) return false;
  if (lower.includes("stringescapeutils.escapehtml")) return false;
  if (lower.includes("org.apache.commons.lang3.stringescapeutils")) return false;
  if (lower.includes("unable to compile class for jsp")) return false;
  return (
    lower.includes("<html") ||
    lower.includes("<!doctype") ||
    lower.includes("<body") ||
    /corporate\s+investments/i.test(text)
  );
}

/**
 * Full Sites item path for a listed REST page (item path, not parent folder).
 * @param {{ path?: string, folderPath?: string, name?: string }} listed
 * @returns {string}
 */
function listedPagePreviewCmsPath(listed) {
  if (!listed) return "";
  const name = String(listed.name || "").trim();
  const itemPath = normalizeListedCmsPath(listed.path);
  if (
    itemPath.toLowerCase().startsWith("/sites/") &&
    (!name || itemPath.toLowerCase().endsWith(`/${name.toLowerCase()}`))
  ) {
    return itemPath;
  }
  const parent = normalizeListedCmsPath(listed.folderPath);
  if (parent.toLowerCase().startsWith("/sites/") && name) {
    return `${parent}/${name}`.replace(/\/{2,}/g, "/");
  }
  return itemPath.toLowerCase().startsWith("/sites/") ? itemPath : "";
}

function normalizeListedCmsPath(raw) {
  let p = String(raw || "")
    .trim()
    .replace(/\\/g, "/");
  while (p.startsWith("//")) p = p.slice(1);
  if (p && !p.startsWith("/")) p = `/${p}`;
  if (p.length > 1 && p.endsWith("/")) p = p.replace(/\/+$/, "");
  return p;
}

/**
 * Absolute GET URL for Finder site-path preview under the CMS context.
 * Encodes path segments (spaces in rffHome titles). CMS paths use {@code /}.
 * @param {string} baseUrl TEST_CMS_URL host (no trailing /Rhythmyx)
 * @param {string} cmsPath finder path such as /Sites/CorporateInvestments/Home
 * @returns {string} empty when not a Sites path
 */
function cmsSitePathPreviewGetUrl(baseUrl, cmsPath) {
  const site = sitePathPreviewUrl(cmsPath);
  if (!site) return "";
  const base = String(baseUrl || "").replace(/\/+$/, "");
  // sitePathPreviewUrl already percent-encodes segments (#3716); do not
  // encodeURIComponent again or spaces become %2520.
  return `${base}/Rhythmyx${site}`;
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
 * Also derives a site hint from page titles such as
 * {@code Corporate Investments Home} (#3684).
 * @param {{ path?: string, folderPath?: string, name?: string }} listed
 * @returns {string[]}
 */
function listedPageSiteNames(listed) {
  if (!listed) return [];
  const names = [];
  const seen = new Set();
  const addName = (raw) => {
    const name = String(raw || "").trim();
    if (!name) return;
    const key = foldSiteName(name);
    if (!key || seen.has(key)) return;
    seen.add(key);
    names.push(name);
  };
  for (const raw of [listed.folderPath, listed.path]) {
    if (!raw) continue;
    const p = resolveExplorerListPath({ path: String(raw) });
    const parts = p.replace(/^\/+/, "").split("/").filter(Boolean);
    if (parts.length < 2 || parts[0].toLowerCase() !== "sites") continue;
    addName(parts[1]);
  }
  const pageName = listed.name ? String(listed.name).trim() : "";
  if (pageName) {
    const stripped = pageName.replace(/\s+Home$/i, "").trim();
    if (stripped && foldSiteName(stripped).length >= 8) {
      addName(stripped);
    }
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

/**
 * True when a tree testid is a Sites <em>root</em> child
 * ({@code tree-node-/Sites/Corporate_Investments/}), not a nested
 * {@code /Sites/{site}/Pages} node.
 * @param {string} testid
 * @returns {boolean}
 */
function isExplorerSiteRootTestId(testid) {
  const rest = String(testid || "")
    .replace(/^tree-node-/i, "")
    .replace(/\\/g, "/");
  const segs = rest
    .replace(/^\/+/, "")
    .replace(/\/+$/, "")
    .split("/")
    .filter(Boolean);
  return segs.length === 2 && segs[0].toLowerCase() === "sites";
}

/**
 * Match a Sites tree node to REST-listed site names. Finder path testids
 * ({@code tree-node-/Sites/Corporate_Investments/}), GUID path + visible
 * name, {@code data-node-name}, and {@code data-folder-path} (repository
 * {@code CorporateInvestments}) all match (#3684 / #3001).
 * @param {string} testid
 * @param {string} innerText
 * @param {string} [nodeName]
 * @param {Iterable<string>} wantedFolded
 * @param {string} [folderPath]
 * @returns {boolean}
 */
function treeNodeMatchesFoldedSite(
  testid,
  innerText,
  nodeName,
  wantedFolded,
  folderPath,
) {
  const wanted = [...(wantedFolded || [])].filter(Boolean);
  if (wanted.length === 0) return true;
  const haystacks = [testid, innerText, nodeName, folderPath].map((s) =>
    foldSiteName(s),
  );
  return wanted.some((n) => haystacks.some((h) => h.includes(n)));
}

module.exports = {
  TEST_IDS,
  explorerEntryUrl,
  pageRenderPreviewPath,
  workflowCheckInPath,
  numericContentIdFromItemId,
  pickPreferredListedPage,
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
  isEditorHostPreviewUrl,
  isProductPagePreviewUrl,
  isAssembledPreviewHtml,
  listedPagePreviewCmsPath,
  cmsSitePathPreviewGetUrl,
  listedPageSiteNames,
  foldSiteName,
  detailRowHasExactName,
  detailRowMatchesFoldedSite,
  treeNodeMatchesFoldedSite,
  isExplorerSiteRootTestId,
};
