/**
 * Explorer saved-search Playwright helpers (#2507 / parent #2409 / #2400 slice D).
 *
 * <p>Pure helpers for surface-filtered E2E: open modern Content Explorer →
 * SearchPanel → saved-search picker → assert catalog / execute result regions.
 * No machine hard-coded install paths; base URL comes from auth /
 * resolve-cms-env ({@code TEST_CMS_URL} or {@code DEV_PERCUSSION_*}).</p>
 *
 * <p>Product surface (WebUI #2506): {@code data-testid} values on
 * {@code SearchPanel} / {@code ContentExplorerShell}.</p>
 */

"use strict";

/** Stable product test ids for the saved-search surface. */
const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  /** #2731: View menu hosts nested view tools (open before toggleSearch). */
  menuView: "explorer-menu-view",
  viewDropdown: "explorer-menu-view-dropdown",
  toggleSearch: "explorer-toggle-search",
  searchPanelHost: "explorer-search-panel",
  searchPanel: "search-panel",
  savedLoading: "search-panel-saved-loading",
  savedError: "search-panel-saved-error",
  savedRetry: "search-panel-saved-retry",
  savedEmpty: "search-panel-saved-empty",
  savedPicker: "search-panel-saved-picker",
  savedSelect: "search-panel-saved-select",
  savedRun: "search-panel-saved-run",
  resultsLoading: "search-panel-loading",
  resultsError: "search-panel-error",
  resultsEmpty: "search-panel-empty",
  resultsList: "search-panel-results",
  resultRow: "search-panel-result-row",
});

/** CX design-search catalog (same path as WebUI {@code PATHS.SEARCHES}). */
const PATH_SEARCHES = "/Rhythmyx/services/searches";

/**
 * Build the modern Explorer SPA entry URL with cache-buster.
 *
 * @param {string} baseUrl CMS origin (no trailing slash required)
 * @param {{ cacheBuster?: string | number }} [opts]
 * @returns {string}
 */
function explorerEntryUrl(baseUrl, opts = {}) {
  const root = String(baseUrl || "")
    .trim()
    .replace(/\/+$/, "");
  const bust =
    opts.cacheBuster != null ? String(opts.cacheBuster) : String(Date.now());
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${encodeURIComponent(bust)}`;
}

/**
 * Absolute URL for GET design-search catalog.
 *
 * @param {string} baseUrl
 * @returns {string}
 */
function searchesCatalogUrl(baseUrl, opts = {}) {
  const root = String(baseUrl || "")
    .trim()
    .replace(/\/+$/, "");
  const includeViews = opts.includeViews === true;
  return includeViews
    ? `${root}${PATH_SEARCHES}?includeViews=true`
    : `${root}${PATH_SEARCHES}`;
}

/**
 * Absolute URL for POST execute of one design search.
 *
 * @param {string} baseUrl
 * @param {string} idOrName
 * @returns {string}
 */
function searchesExecuteUrl(baseUrl, idOrName) {
  const root = String(baseUrl || "")
    .trim()
    .replace(/\/+$/, "");
  const key = encodeURIComponent(String(idOrName || "").trim());
  return `${root}${PATH_SEARCHES}/${key}/execute`;
}

/**
 * Unwrap Jackson / list wrappers for {@code SearchDef[]} catalog payloads.
 *
 * @param {unknown} payload
 * @returns {Record<string, unknown>[]}
 */
function unwrapSearchDefs(payload) {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload.filter((x) => x != null && typeof x === "object");
  }
  if (typeof payload === "object") {
    const obj = /** @type {Record<string, unknown>} */ (payload);
    const raw = obj.SearchDef ?? obj.searchDef ?? obj.SearchDefList ?? obj.items;
    if (raw == null) {
      return [];
    }
    if (Array.isArray(raw)) {
      return raw.filter((x) => x != null && typeof x === "object");
    }
    if (typeof raw === "object") {
      return [/** @type {Record<string, unknown>} */ (raw)];
    }
  }
  return [];
}

/**
 * Stable select option key for a SearchDef (matches WebUI searchKey).
 *
 * @param {Record<string, unknown> | null | undefined} def
 * @returns {string}
 */
function searchDefKey(def) {
  if (def == null || typeof def !== "object") {
    return "";
  }
  const name = def.name != null ? String(def.name).trim() : "";
  if (name) {
    return name;
  }
  const id = def.id != null ? String(def.id).trim() : "";
  return id;
}

/**
 * Human label for a SearchDef option.
 *
 * @param {Record<string, unknown> | null | undefined} def
 * @returns {string}
 */
function searchDefLabel(def) {
  if (def == null || typeof def !== "object") {
    return "";
  }
  const label =
    def.label != null
      ? String(def.label).trim()
      : def.displayName != null
        ? String(def.displayName).trim()
        : "";
  if (label) {
    return label;
  }
  return searchDefKey(def);
}

/**
 * True when the design search is a custom URL search (UI run button disabled).
 *
 * @param {Record<string, unknown> | null | undefined} def
 * @returns {boolean}
 */
function isCustomUrlSearch(def) {
  if (def == null || typeof def !== "object") {
    return false;
  }
  return def.customSearch === true || def.customSearch === "true";
}

/**
 * Pick the first runnable (non-custom-URL) design search from a catalog.
 *
 * @param {unknown} payload raw REST body or SearchDef[]
 * @returns {{ key: string, label: string, def: Record<string, unknown> } | null}
 */
function isDefaultAllView(def) {
  if (def == null || typeof def !== "object") {
    return false;
  }
  const name = def.name != null ? String(def.name).trim() : "";
  const label = searchDefLabel(def);
  return (
    name.toLowerCase() === "view_all" ||
    (name.toLowerCase() === "all" && String(def.type || "").toLowerCase() === "view") ||
    (label.toLowerCase() === "all" && String(def.type || "").toLowerCase() === "view")
  );
}

function pickRunnableSavedSearch(payload) {
  const defs = unwrapSearchDefs(payload);
  let first = null;
  for (const def of defs) {
    if (isCustomUrlSearch(def)) {
      continue;
    }
    const key = searchDefKey(def);
    if (!key) {
      continue;
    }
    const picked = { key, label: searchDefLabel(def) || key, def };
    if (isDefaultAllView(def)) {
      return picked;
    }
    if (first == null) {
      first = picked;
    }
  }
  return first;
}

/**
 * Classify catalog UI region after SearchPanel mounts.
 *
 * @param {"loading"|"error"|"empty"|"picker"|"unknown"} kind
 * @returns {boolean}
 */
function isCatalogSettled(kind) {
  return kind === "error" || kind === "empty" || kind === "picker";
}

/**
 * Soft-skip message when QA fixture has no runnable design searches.
 *
 * @param {{ empty?: boolean, onlyCustom?: boolean, restStatus?: number }} [detail]
 * @returns {string}
 */
function noRunnableSearchSkipMessage(detail = {}) {
  const parts = [
    "No runnable design search in fixture for Explorer saved-search E2E (#2507).",
    "Catalog empty or only custom-URL searches — soft skip after asserting catalog UI.",
  ];
  if (detail.empty) {
    parts.push("catalog empty.");
  }
  if (detail.onlyCustom) {
    parts.push("only customSearch=true entries.");
  }
  if (detail.restStatus != null) {
    parts.push(`REST status=${detail.restStatus}.`);
  }
  return parts.join(" ");
}

/**
 * CSS selector joining post-execute result regions (loading / error / empty / list).
 *
 * @returns {string}
 */
function postExecuteRegionSelector() {
  return [
    `[data-testid="${TEST_IDS.resultsLoading}"]`,
    `[data-testid="${TEST_IDS.resultsError}"]`,
    `[data-testid="${TEST_IDS.resultsEmpty}"]`,
    `[data-testid="${TEST_IDS.resultsList}"]`,
  ].join(", ");
}

/**
 * CSS selector for settled catalog chrome (not still loading).
 *
 * @returns {string}
 */
function catalogSettledSelector() {
  return [
    `[data-testid="${TEST_IDS.savedError}"]`,
    `[data-testid="${TEST_IDS.savedEmpty}"]`,
    `[data-testid="${TEST_IDS.savedPicker}"]`,
  ].join(", ");
}

module.exports = {
  TEST_IDS,
  PATH_SEARCHES,
  explorerEntryUrl,
  searchesCatalogUrl,
  searchesExecuteUrl,
  isDefaultAllView,
  unwrapSearchDefs,
  searchDefKey,
  searchDefLabel,
  isCustomUrlSearch,
  pickRunnableSavedSearch,
  isCatalogSettled,
  noRunnableSearchSkipMessage,
  postExecuteRegionSelector,
  catalogSettledSelector,
};
