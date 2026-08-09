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
 * Pure helpers + modern Content Explorer UI utilities for folder recycle/restore
 * companion smoke (#2542 / parent #2423; classic Finder peer #2489; REST peer #2464).
 *
 * <p>Prefer stable {@code data-testid} selectors on the React explorer shell
 * ({@code content-explorer-shell}, tree, detail list, reduced-actions delete).
 * Reuses folder-recycle-smoke probe / seed / recycle REST helpers. No machine
 * hard-coded install paths — base URL and credentials come from auth /
 * resolve-cms-env ({@code TEST_CMS_URL} or {@code DEV_PERCUSSION_*}).</p>
 *
 * <p>Surface tags: {@code @explorer-recycle-restore} {@code @folder-recycle}
 * {@code @smoke}.</p>
 *
 * <p>Does <strong>not</strong> replace classic Finder coverage (#2489 /
 * {@code finder-recycle-restore-ui}).</p>
 */

"use strict";

const {
  PATH_RESTORE_FOLDER,
  PATH_DELETE_FOLDER,
  isContextHealthyStatus,
  contextDownFailureMessage,
} = require("./folder-recycle-smoke");

const { cmsUrl } = require("./empty-recycling");

/**
 * Stable modern Content Explorer shell selectors (feature 992 / US1).
 * Prefer data-testid over aria-label (TMX fallback can leave unresolved keys).
 */
const SELECTORS = Object.freeze({
  shell: '[data-testid="content-explorer-shell"]',
  explorerTree: '[data-testid="explorer-tree"]',
  explorerTreeError: '[data-testid="explorer-tree-error"]',
  explorerTreeEmpty: '[data-testid="explorer-tree-empty"]',
  detailList: '[data-testid="detail-list"]',
  reducedActions: '[data-testid="reduced-actions"]',
  actionDelete: '[data-testid="action-delete"]',
  actionOpen: '[data-testid="action-open"]',
  actionCreateFolder: '[data-testid="action-create-folder"]',
  actionToolbar: '[data-testid="action-toolbar"]',
  contextMenu: '[data-testid="context-menu"]',
  treeNodePrefix: "tree-node-",
  detailRowPrefix: "detail-row-",
  // Classic Finder chrome that must NOT load on the modern SPA entry.
  classicWebManagement: "#perc-web-management",
  classicMillerColumn: ".perc-mcol",
  classicFinderDelete: "#perc-finder-delete",
  classicEmptyRecycling: '[data-testid="perc-finder-empty-recycling"]',
});

/** Surface filter tags for run-surface / documentation. */
const SURFACE_TAGS = Object.freeze([
  "explorer-recycle-restore",
  "folder-recycle",
  "smoke",
]);

/**
 * Modern Content Explorer SPA entry URL (cache-busted).
 *
 * @param {string} baseUrl CMS base (no trailing slash required)
 * @param {number} [nowMs]
 * @returns {string}
 */
function modernExplorerUrl(baseUrl, nowMs) {
  const base = String(baseUrl || "").replace(/\/+$/, "");
  const ts =
    nowMs != null && Number.isFinite(Number(nowMs))
      ? Number(nowMs)
      : Date.now();
  return `${base}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${ts}`;
}

/**
 * Normalize a CMS path for explorer tree/detail navigation.
 * Collapses duplicate slashes; empty → "/"; does not force trailing slash
 * (live tree testids use both {@code /Assets} and {@code /Assets/} variants).
 *
 * @param {string | null | undefined} path
 * @returns {string}
 */
function normalizeExplorerPath(path) {
  const raw = String(path || "").trim();
  if (!raw || raw === "/") {
    return "/";
  }
  const parts = raw.split("/").filter(Boolean);
  return `/${parts.join("/")}`;
}

/**
 * Candidate data-testid values for a tree node at {@code path}.
 * Live CMS path items often include a trailing slash on folder paths.
 *
 * @param {string | null | undefined} path e.g. "Assets" or "/Assets/"
 * @returns {string[]} full CSS selectors for the tree node
 */
/**
 * Escape a value for use inside a double-quoted CSS attribute selector.
 * Prevents selector breakage when path segments or action names contain
 * quotes, backslashes, or other special characters.
 *
 * @param {string | null | undefined} value
 * @returns {string}
 */
function cssAttrEscape(value) {
  return String(value ?? "")
    .replace(/\\/g, "\\\\")
    .replace(/"/g, '\\"');
}

/**
 * Fuzzy tree-node selector: any data-testid containing the path segment.
 *
 * @param {string | null | undefined} segment
 * @returns {string} CSS selector
 */
function fuzzyTreeNodeSelector(segment) {
  const safe = cssAttrEscape(segment);
  return `[data-testid*="${safe}"]`;
}

function treeNodeSelectors(path) {
  const normalized = normalizeExplorerPath(path);
  if (normalized === "/") {
    return [
      `[data-testid="${cssAttrEscape(`${SELECTORS.treeNodePrefix}/`)}"]`,
      `[data-testid="${cssAttrEscape(SELECTORS.treeNodePrefix)}"]`,
    ];
  }
  const withSlash = normalized.endsWith("/") ? normalized : `${normalized}/`;
  const withoutSlash = normalized.endsWith("/")
    ? normalized.slice(0, -1)
    : normalized;
  return [
    `[data-testid="${cssAttrEscape(SELECTORS.treeNodePrefix + withSlash)}"]`,
    `[data-testid="${cssAttrEscape(SELECTORS.treeNodePrefix + withoutSlash)}"]`,
  ];
}

/**
 * True when a button/control is enabled (not HTML-disabled and not aria-disabled).
 *
 * @param {{ disabled?: boolean | null, ariaDisabled?: string | null }} attrs
 * @returns {boolean}
 */
function isActionControlEnabled(attrs = {}) {
  if (attrs.disabled === true) {
    return false;
  }
  const aria = String(attrs.ariaDisabled || "").toLowerCase();
  if (aria === "true") {
    return false;
  }
  return true;
}

/**
 * True when the browser URL still indicates the login page (login hard-fail).
 *
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isStillOnLoginPage(url) {
  const u = String(url || "");
  return /\/Rhythmyx\/login(\?|$|\/)/i.test(u);
}

/**
 * Operator-facing hard-fail when Admin login did not leave the login page.
 *
 * @param {{ url?: string, baseUrl?: string }} [detail]
 * @returns {string}
 */
function loginContextDownFailureMessage(detail = {}) {
  const url = detail.url || "(unknown)";
  const base = detail.baseUrl ? ` base=${detail.baseUrl}` : "";
  return (
    `Admin login/context appears DOWN — still on login page after loginAsAdmin` +
    ` (url=${url}).` +
    ` Hard fail for modern Content Explorer recycle/restore UI (#2542 / parent #2423):` +
    ` do not soft-skip. Check Rhythmyx context, credentials, and qa-up health.` +
    base
  );
}

/**
 * URL fragment used when waiting for modern explorer deleteItem network calls.
 * Product pathApi posts to pathmanagement/path/delete/{encodedPath}.
 *
 * @returns {string}
 */
function deleteItemApiPathFragment() {
  return "/pathmanagement/path/delete";
}

/**
 * URL fragment for classic/REST deleteFolder (recycle seed fallback).
 * @returns {string}
 */
function deleteFolderApiPathFragment() {
  return "/pathmanagement/path/deleteFolder";
}

/**
 * URL fragment used when waiting for restoreFolder network calls.
 * @returns {string}
 */
function restoreFolderApiPathFragment() {
  return "/pathmanagement/path/restoreFolder";
}

/**
 * URL fragment for empty Recycling bulk purge (DELETE).
 * @returns {string}
 */
function emptyRecyclingApiPathFragment() {
  return "/pathmanagement/recycle/empty";
}

/**
 * Build a candidate Recycling path for a named structural-root folder seed.
 *
 * @param {string} folderName
 * @param {"Assets" | "Sites" | ""} [structuralRoot="Assets"]
 * @returns {string}
 */
function recycledFolderExplorerPath(folderName, structuralRoot = "Assets") {
  const name = String(folderName || "").trim();
  if (!name) {
    return normalizeExplorerPath(
      structuralRoot ? `/Recycling/${structuralRoot}` : "/Recycling",
    );
  }
  if (!structuralRoot) {
    return normalizeExplorerPath(`/Recycling/${name}`);
  }
  return normalizeExplorerPath(`/Recycling/${structuralRoot}/${name}`);
}

/**
 * Locator filter helper: exact text match for a detail-list / tree label (trim).
 *
 * @param {string} name
 * @returns {(text: string) => boolean}
 */
function exactExplorerItemNameMatcher(name) {
  const target = String(name || "").trim();
  return (text) => String(text || "").trim() === target;
}

/**
 * Choose restore vs empty-recycling branch when selection is uncertain.
 * Prefer restore when path is under Recycling and a restore control is enabled;
 * otherwise empty Recycling (REST or classic peer) is the cleanup path.
 *
 * @param {{ pathEligible?: boolean, restoreEnabled?: boolean }} flags
 * @returns {"restore" | "empty"}
 */
function chooseRestoreOrEmptyBranch(flags = {}) {
  if (flags.pathEligible && flags.restoreEnabled) {
    return "restore";
  }
  return "empty";
}

/**
 * Restore is path-eligible when under /Recycling with a folder depth that
 * matches classic restore rules (depth ≥ 4 including leading empty segment
 * equivalent: /Recycling/{root}/{name}).
 *
 * @param {string | null | undefined} path
 * @returns {boolean}
 */
function isRestoreEligibleExplorerPath(path) {
  const normalized = normalizeExplorerPath(path);
  if (normalized === "/") {
    return false;
  }
  const parts = normalized.split("/").filter(Boolean);
  // ["Recycling", "Assets", "seed"] → eligible
  if (parts.length < 3) {
    return false;
  }
  return String(parts[0] || "") === "Recycling";
}

/**
 * True when a server-driven action name looks like restore (not recycle empty).
 *
 * @param {string | null | undefined} actionName
 * @returns {boolean}
 */
function isRestoreActionName(actionName) {
  const n = String(actionName || "").toLowerCase();
  if (!n) {
    return false;
  }
  if (n.includes("empty")) {
    return false;
  }
  return n.includes("restore") || n.includes("undelete") || n.includes("recover");
}

/**
 * True when an action name looks like empty-recycling purge.
 *
 * @param {string | null | undefined} actionName
 * @returns {boolean}
 */
function isEmptyRecyclingActionName(actionName) {
  const n = String(actionName || "").toLowerCase();
  if (!n) {
    return false;
  }
  return (
    (n.includes("empty") && n.includes("recycl")) ||
    n === "emptyrecycling" ||
    n.includes("empty-recycling") ||
    n.includes("purge recycling")
  );
}

/**
 * CSS selector for a server action toolbar item by action name.
 *
 * @param {string} actionName
 * @returns {string}
 */
function actionToolbarItemSelector(actionName) {
  const safe = cssAttrEscape(String(actionName || "").trim());
  return `[data-testid="action-toolbar-item-${safe}"]`;
}

/**
 * CSS selector for a context-menu item by action name.
 *
 * @param {string} actionName
 * @returns {string}
 */
function contextMenuItemSelector(actionName) {
  const safe = cssAttrEscape(String(actionName || "").trim());
  return `[data-testid="context-menu-item-${safe}"]`;
}

module.exports = {
  SELECTORS,
  SURFACE_TAGS,
  modernExplorerUrl,
  normalizeExplorerPath,
  treeNodeSelectors,
  cssAttrEscape,
  fuzzyTreeNodeSelector,
  isActionControlEnabled,
  isStillOnLoginPage,
  loginContextDownFailureMessage,
  deleteItemApiPathFragment,
  deleteFolderApiPathFragment,
  restoreFolderApiPathFragment,
  emptyRecyclingApiPathFragment,
  recycledFolderExplorerPath,
  exactExplorerItemNameMatcher,
  chooseRestoreOrEmptyBranch,
  isRestoreEligibleExplorerPath,
  isRestoreActionName,
  isEmptyRecyclingActionName,
  actionToolbarItemSelector,
  contextMenuItemSelector,
  // Re-export peer constants used by the spec for convenience.
  PATH_RESTORE_FOLDER,
  PATH_DELETE_FOLDER,
  isContextHealthyStatus,
  contextDownFailureMessage,
  cmsUrl,
};
