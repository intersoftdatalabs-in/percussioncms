/**
 * Pure helpers + classic Finder UI utilities for folder recycle/restore
 * companion smoke (#2489 / parent #2423; REST peer #2464).
 *
 * <p>Reuses empty-recycling selectors and folder-recycle-smoke REST helpers.
 * No machine hard-coded install paths. Base URL and credentials come from
 * auth / resolve-cms-env ({@code TEST_CMS_URL} or {@code DEV_PERCUSSION_*}).</p>
 *
 * <p>Surface tags: {@code @finder-recycle-restore} {@code @folder-recycle}
 * {@code @smoke}.</p>
 */

"use strict";

const {
  SELECTORS: EMPTY_SELECTORS,
  cmsUrl,
} = require("./empty-recycling");

const {
  PATH_RESTORE_FOLDER,
  PATH_DELETE_FOLDER,
  isContextHealthyStatus,
  contextDownFailureMessage,
} = require("./folder-recycle-smoke");

/** Stable classic Finder chrome used by recycle / restore / empty flows. */
const SELECTORS = Object.freeze({
  ...EMPTY_SELECTORS,
  deleteButton: "#perc-finder-delete",
  restoreItem: "#perc-finder-restore-item",
  restoreItemInActions: "#perc-finder-actions #perc-finder-restore-item",
  deleteConfirm: "#perc-finder-delete-confirm",
  confirmOk: "#perc-confirm-generic-ok",
  confirmCancel: "#perc-confirm-generic-cancel",
  finderItemName: ".perc-finder-item-name",
  lastSelected: ".mcol-opened.perc_last_selected",
  listingCategoryFolder: ".perc-listing-category-FOLDER",
  finderListingPrefix: "perc-finder-listing-",
  finderOuter: ".perc-finder-outer",
  pathSummary: "#mcol-path-summary",
  pathGo: "#perc-finder-go-action",
  finderExpander: "#perc-finder-expander",
  actionsButton: "#perc-finder-actions-button",
  emptyAction: '[data-testid="perc-finder-empty-recycling"]',
});

/** Surface filter tags for run-surface / documentation. */
const SURFACE_TAGS = Object.freeze([
  "finder-recycle-restore",
  "folder-recycle",
  "smoke",
]);

/**
 * Classic Finder shell URL (dashboard still loads finder_js + Actions menu).
 * Cache-bust query avoids stale dashboard chrome after deploy.
 *
 * @param {string} baseUrl CMS base (no trailing slash required)
 * @param {number} [nowMs]
 * @returns {string}
 */
function classicFinderDashboardUrl(baseUrl, nowMs) {
  const base = String(baseUrl || "").replace(/\/+$/, "");
  const ts =
    nowMs != null && Number.isFinite(Number(nowMs))
      ? Number(nowMs)
      : Date.now();
  return `${base}/Rhythmyx/cm/app/dashboard.jsp?_=${ts}`;
}

/**
 * True when a finder control class list looks enabled (not permanently disabled).
 * Matches #2207 enablement polling: ui-enabled present or ui-disabled absent.
 *
 * @param {string | null | undefined} classAttr
 * @returns {boolean}
 */
function isFinderControlEnabled(classAttr) {
  const cls = String(classAttr || "");
  if (!cls.trim()) {
    // No class yet — treat as unknown/not enabled for strict polls.
    return false;
  }
  if (cls.includes("ui-enabled")) {
    return true;
  }
  return !cls.includes("ui-disabled");
}

/**
 * Normalize a user/path-bar input into a leading-slash finder path string.
 * Collapses duplicate slashes; empty → "/".
 *
 * @param {string | null | undefined} path
 * @returns {string}
 */
function normalizeFinderPathInput(path) {
  const raw = String(path || "").trim();
  if (!raw || raw === "/") {
    return "/";
  }
  const parts = raw.split("/").filter(Boolean);
  return `/${parts.join("/")}`;
}

/**
 * Split a finder path into segments matching classic finder path arrays
 * (leading empty string for root).
 *
 * @param {string | null | undefined} path e.g. "/Recycling/Assets/foo"
 * @returns {string[]} e.g. ["", "Recycling", "Assets", "foo"]
 */
function finderPathSegments(path) {
  const normalized = normalizeFinderPathInput(path);
  if (normalized === "/") {
    return [""];
  }
  return ["", ...normalized.split("/").filter(Boolean)];
}

/**
 * Restore button enablement rule from {@code perc_restore_button.js}:
 * under Recycling with depth ≥ 4 (e.g. /Recycling/Assets/folderName).
 *
 * @param {string | string[] | null | undefined} path path string or segments
 * @returns {boolean}
 */
function isRestoreEligiblePath(path) {
  const segments = Array.isArray(path) ? path : finderPathSegments(path);
  if (!segments || segments.length < 4) {
    return false;
  }
  // path[1] is first real segment (index 0 is leading empty).
  return String(segments[1] || "") === "Recycling";
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
    ` Hard fail for classic Finder recycle/restore UI (#2489 / parent #2423):` +
    ` do not soft-skip. Check Rhythmyx context, credentials, and qa-up health.` +
    base
  );
}

/**
 * URL fragment used when waiting for deleteFolder (recycle) network calls.
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
 * Build a candidate path under Recycling for a named Assets folder seed.
 * Product often places recycled Assets folders under /Recycling/Assets/{name}.
 *
 * @param {string} folderName
 * @param {"Assets" | "Sites" | ""} [structuralRoot="Assets"]
 * @returns {string}
 */
function recycledFolderFinderPath(folderName, structuralRoot = "Assets") {
  const name = String(folderName || "").trim();
  if (!name) {
    return normalizeFinderPathInput(
      structuralRoot ? `/Recycling/${structuralRoot}` : "/Recycling",
    );
  }
  if (!structuralRoot) {
    return normalizeFinderPathInput(`/Recycling/${name}`);
  }
  return normalizeFinderPathInput(`/Recycling/${structuralRoot}/${name}`);
}

/**
 * Locator filter helper: exact text match for a finder item name (trim).
 * Pure: returns a predicate for Playwright filter({ hasText }) is not enough
 * for exact match — tests use this to compare textContents.
 *
 * @param {string} name
 * @returns {(text: string) => boolean}
 */
function exactFinderItemNameMatcher(name) {
  const target = String(name || "").trim();
  return (text) => String(text || "").trim() === target;
}

/**
 * Choose restore vs empty-recycling UI branch when selection is uncertain.
 * Prefer restore when path is restore-eligible and restore control is enabled;
 * otherwise empty Recycling (Admin bulk purge) is the proven #2207 path.
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

module.exports = {
  SELECTORS,
  SURFACE_TAGS,
  classicFinderDashboardUrl,
  isFinderControlEnabled,
  normalizeFinderPathInput,
  finderPathSegments,
  isRestoreEligiblePath,
  isStillOnLoginPage,
  loginContextDownFailureMessage,
  deleteFolderApiPathFragment,
  restoreFolderApiPathFragment,
  emptyRecyclingApiPathFragment,
  recycledFolderFinderPath,
  exactFinderItemNameMatcher,
  chooseRestoreOrEmptyBranch,
  // Re-export peer constants used by the spec for convenience.
  PATH_RESTORE_FOLDER,
  PATH_DELETE_FOLDER,
  isContextHealthyStatus,
  contextDownFailureMessage,
  cmsUrl,
};
