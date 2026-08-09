/**
 * Pure helpers + classic Finder UI utilities for folder recycle/restore
 * companion smoke (#2489 residual #2541 / parent #2423; REST peer #2464).
 *
 * <p>Reuses empty-recycling selectors and folder-recycle-smoke REST helpers.
 * No machine hard-coded install paths. Base URL and credentials come from
 * auth / resolve-cms-env ({@code TEST_CMS_URL} or {@code DEV_PERCUSSION_*}).</p>
 *
 * <p>Surface tags: {@code @finder-recycle-restore} {@code @folder-recycle}
 * {@code @smoke}.</p>
 *
 * <h3>Selection reliability (#2541)</h3>
 * <p>Classic Finder delete (#perc-finder-delete) enables only after path_changed
 * with depth &gt; 2 (see perc_delete_page_button.js). Happy path must select or
 * path-navigate to the seeded Assets folder so UI recycle works without REST
 * soft-delete fallback. Strategies cover miller column + list view skins.</p>
 *
 * <h3>Residual product chrome / testid gaps</h3>
 * <ul>
 *   <li>Miller listings use id {@code perc-finder-listing-{id}} and class
 *       {@code mcol-listing} / {@code perc-finder-item-name} — no stable
 *       {@code data-testid} on individual folder rows.</li>
 *   <li>List view rows use {@code perc-datatable-row} with jQuery row data —
 *       no {@code data-testid} per path item.</li>
 *   <li>Delete/restore enablement is class-based ({@code ui-enabled} /
 *       {@code ui-disabled}), not aria-disabled / HTML disabled.</li>
 *   <li>Only Empty Recycling has {@code data-testid="perc-finder-empty-recycling"}
 *       (#2206); recycle/restore still rely on id selectors.</li>
 * </ul>
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
  millerListing: ".mcol-listing",
  listingCategoryFolder: ".perc-listing-category-FOLDER",
  finderListingPrefix: "perc-finder-listing-",
  listViewRow: ".perc-datatable-row",
  listViewRowHighlighted: ".perc-datatable-row-highlighted",
  chooseColumnView: "#perc-finder-choose-columnview",
  chooseListView: "#perc-finder-choose-listview",
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
 * Escape a value for use inside a double-quoted CSS attribute selector.
 * Prevents selector breakage when folder names contain quotes or backslashes.
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
 * CSS id selector for a miller listing built from product idFromItem:
 * {@code #perc-finder-listing-{id}}.
 *
 * @param {string | null | undefined} itemId content id / guid postfix
 * @returns {string} empty when id missing
 */
function finderListingIdSelector(itemId) {
  const raw = String(itemId || "").trim();
  if (!raw) {
    return "";
  }
  // Product: id = "perc-finder-listing-" + item.id (ids often start with digits).
  // Full id always starts with a letter (prefix), so #id is valid when raw is
  // word/colon/hyphen characters; otherwise fall back to attribute selector.
  const fullId = `${SELECTORS.finderListingPrefix}${raw}`;
  if (/^[\w:-]+$/.test(raw)) {
    return `#${fullId}`;
  }
  return `[id="${cssAttrEscape(fullId)}"]`;
}

/**
 * Full finder path to a named folder under a structural parent (default Assets).
 *
 * @param {string} folderName
 * @param {string} [parentPath="Assets"]
 * @returns {string} e.g. "/Assets/qa-folder-1"
 */
function folderFinderPath(folderName, parentPath = "Assets") {
  const name = String(folderName || "").trim();
  const parent = normalizeFinderPathInput(parentPath);
  if (!name) {
    return parent;
  }
  if (parent === "/") {
    return normalizeFinderPathInput(`/${name}`);
  }
  return normalizeFinderPathInput(`${parent}/${name}`);
}

/**
 * True when path-bar / path_changed depth is deep enough for delete enablement.
 * Product rule: mcol_path.length &gt; 2 (e.g. ["", "Assets", "seed"]).
 *
 * @param {string | string[] | null | undefined} path
 * @returns {boolean}
 */
function isDeleteEligiblePath(path) {
  const segments = Array.isArray(path) ? path : finderPathSegments(path);
  return Array.isArray(segments) && segments.length > 2;
}

/**
 * True when the path bar value indicates the named item is the current path leaf.
 *
 * @param {string | null | undefined} pathBarValue
 * @param {string | null | undefined} folderName
 * @returns {boolean}
 */
function pathBarReflectsFolderName(pathBarValue, folderName) {
  const name = String(folderName || "").trim();
  if (!name) {
    return false;
  }
  const segments = finderPathSegments(pathBarValue);
  if (segments.length < 2) {
    return false;
  }
  return String(segments[segments.length - 1] || "").trim() === name;
}

/**
 * Ordered pure strategies to put a named Assets (or parent) folder in selection
 * so #perc-finder-delete can enable. Spec applies these against live DOM.
 *
 * <ol>
 *   <li>{@code path-bar} — navigate path bar to /Parent/Name (most reliable;
 *       fires path_changed with depth &gt; 2 without miller click races)</li>
 *   <li>{@code listing-id} — click #perc-finder-listing-{guid} when known</li>
 *   <li>{@code miller-title} — click .mcol-listing[title=name]</li>
 *   <li>{@code miller-name} — click parent listing of matching item-name text</li>
 *   <li>{@code list-row} — click .perc-datatable-row containing exact name</li>
 * </ol>
 *
 * @param {{ name: string, parentPath?: string, guid?: string }} opts
 * @returns {Array<{
 *   kind: "path-bar" | "listing-id" | "miller-title" | "miller-name" | "list-row",
 *   path?: string,
 *   selector?: string,
 *   name?: string
 * }>}
 */
function finderRecycleSelectStrategies(opts = {}) {
  const name = String(opts.name || "").trim();
  const parentPath = opts.parentPath != null ? opts.parentPath : "Assets";
  const guid = String(opts.guid || "").trim();
  /** @type {Array<{ kind: string, path?: string, selector?: string, name?: string }>} */
  const strategies = [];

  if (name) {
    strategies.push({
      kind: "path-bar",
      path: folderFinderPath(name, parentPath),
    });
  }

  if (guid) {
    const sel = finderListingIdSelector(guid);
    if (sel) {
      strategies.push({ kind: "listing-id", selector: sel, name });
    }
  }

  if (name) {
    const safe = cssAttrEscape(name);
    strategies.push({
      kind: "miller-title",
      selector: `${SELECTORS.millerListing}[title="${safe}"]`,
      name,
    });
    strategies.push({
      kind: "miller-name",
      selector: SELECTORS.finderItemName,
      name,
    });
    strategies.push({
      kind: "list-row",
      selector: SELECTORS.listViewRow,
      name,
    });
  }

  return strategies;
}

/**
 * Whether REST soft-delete fallback is still needed after UI selection attempts.
 * Happy path (#2541): selected + delete enabled ⇒ no REST.
 *
 * @param {{ selected?: boolean, deleteEnabled?: boolean, recycledViaUi?: boolean }} flags
 * @returns {boolean}
 */
function shouldUseRestRecycleFallback(flags = {}) {
  if (flags.recycledViaUi) {
    return false;
  }
  if (flags.selected && flags.deleteEnabled) {
    // Selection + enablement succeeded; caller should click delete — not REST yet.
    return false;
  }
  return true;
}

/**
 * Operator message when UI selection never enabled delete (residual chrome gap).
 *
 * @param {{ name?: string, strategiesTried?: string[], pathBar?: string }} detail
 * @returns {string}
 */
function millerSelectionFailureMessage(detail = {}) {
  const name = detail.name || "(unknown)";
  const tried = Array.isArray(detail.strategiesTried)
    ? detail.strategiesTried.join(", ")
    : "(n/a)";
  const pathBar = detail.pathBar ? ` pathBar=${detail.pathBar}` : "";
  return (
    `Classic Finder miller/list selection did not enable #perc-finder-delete` +
    ` for folder "${name}" (strategies tried: ${tried}).` +
    ` Residual chrome gaps: no data-testid on miller listings / list rows;` +
    ` delete uses ui-enabled/ui-disabled classes only (#2541).` +
    pathBar
  );
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
  cssAttrEscape,
  finderListingIdSelector,
  folderFinderPath,
  isDeleteEligiblePath,
  pathBarReflectsFolderName,
  finderRecycleSelectStrategies,
  shouldUseRestRecycleFallback,
  millerSelectionFailureMessage,
  chooseRestoreOrEmptyBranch,
  // Re-export peer constants used by the spec for convenience.
  PATH_RESTORE_FOLDER,
  PATH_DELETE_FOLDER,
  isContextHealthyStatus,
  contextDownFailureMessage,
  cmsUrl,
};
