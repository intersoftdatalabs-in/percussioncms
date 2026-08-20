/**
 * Explorer simple/extended search submit helpers (#3617 / parent #3102).
 *
 * <p>Pure helpers for the product Explorer route
 * ({@code spa.jsp?entry=explorer}): open SearchPanel, POST free-text
 * (extendedresults), classify HTTP 200 results or empty-success. No
 * machine-hard-coded install paths; base URL comes from auth /
 * resolve-cms-env.</p>
 *
 * <p>Do not treat {@code searchModern.jsp} as the operator path. Do not
 * soft-skip when SearchPanel is mounted.</p>
 */

"use strict";

const {
  TEST_IDS: CHROME_IDS,
  explorerSpaUrl,
  openViewMenu,
} = require("./explorer-shell-chrome");

/** Stable product test ids for the search-submit surface. */
const TEST_IDS = Object.freeze({
  shell: CHROME_IDS.shell,
  menuView: CHROME_IDS.menuView,
  toggleSearch: CHROME_IDS.toggleSearch,
  viewToolSearch: CHROME_IDS.viewToolSearch,
  searchPanelHost: CHROME_IDS.searchPanelHost,
  searchPanel: CHROME_IDS.searchPanel,
  searchInput: CHROME_IDS.searchInput,
  searchSubmit: CHROME_IDS.searchSubmit,
  extended: "search-panel-extended",
  loading: "search-panel-loading",
  results: "search-panel-results",
  empty: "search-panel-empty",
  error: "search-panel-error",
  resultRow: "search-panel-result-row",
});

/** Sitemanage extended-search REST path (same as WebUI {@code PATHS.FINDER_SEARCH_EXTENDED}). */
const EXTENDED_RESULTS_PATH = "/searchmanagement/search/get/extendedresults";

/**
 * Build the modern Explorer SPA entry URL with cache-buster.
 *
 * @param {string} baseUrl CMS origin (no trailing slash required)
 * @param {{ cacheBuster?: string | number }} [opts]
 * @returns {string}
 */
function explorerEntryUrl(baseUrl, opts = {}) {
  if (opts.cacheBuster != null) {
    const root = String(baseUrl || "")
      .trim()
      .replace(/\/+$/, "");
    const bust = String(opts.cacheBuster);
    return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${encodeURIComponent(bust)}`;
  }
  return explorerSpaUrl(baseUrl);
}

/**
 * True when {@code url} is the US5 pilot JSP — not the product Explorer route.
 *
 * @param {string} url
 * @returns {boolean}
 */
function isPilotSearchJsp(url) {
  return /searchModern\.jsp/i.test(String(url || ""));
}

/**
 * True when the request is POST {@code /searchmanagement/search/get/extendedresults}.
 *
 * @param {string} url
 * @param {string} [method]
 * @returns {boolean}
 */
function isExtendedResultsPost(url, method) {
  const m = String(method || "POST").toUpperCase();
  if (m !== "POST") {
    return false;
  }
  return String(url || "").includes(EXTENDED_RESULTS_PATH);
}

/**
 * HTTP 200 (or 204) is success for this slice. 4xx/5xx are product defects.
 *
 * @param {number} status
 * @returns {boolean}
 */
function isSearchSubmitSuccessStatus(status) {
  const n = Number(status);
  return n === 200 || n === 204;
}

/**
 * @param {number} status
 * @returns {boolean}
 */
function isSearchSubmitFailureStatus(status) {
  const n = Number(status);
  return Number.isFinite(n) && n >= 400;
}

/**
 * Locator string for terminal success (results list or documented empty).
 * Error chrome is not a success for this slice.
 *
 * @returns {string}
 */
function terminalSuccessSelector() {
  return `[data-testid="${TEST_IDS.results}"], [data-testid="${TEST_IDS.empty}"]`;
}

/**
 * Classify post-submit UI + HTTP.
 *
 * @param {{
 *   status?: number | null,
 *   hasResults?: boolean,
 *   hasEmpty?: boolean,
 *   hasError?: boolean,
 * }} state
 * @returns {"results"|"empty-success"|"http-error"|"ui-error"|"unknown"}
 */
function classifySubmitOutcome(state) {
  const status = state && state.status;
  if (status != null && isSearchSubmitFailureStatus(status)) {
    return "http-error";
  }
  if (status != null && !isSearchSubmitSuccessStatus(status)) {
    return "http-error";
  }
  if (state && state.hasResults) {
    return "results";
  }
  if (state && state.hasEmpty) {
    return "empty-success";
  }
  if (state && state.hasError) {
    return "ui-error";
  }
  return "unknown";
}

/**
 * @param {string} kind
 * @returns {boolean}
 */
function isSuccessfulSubmitOutcome(kind) {
  return kind === "results" || kind === "empty-success";
}

module.exports = {
  TEST_IDS,
  EXTENDED_RESULTS_PATH,
  explorerEntryUrl,
  openViewMenu,
  isPilotSearchJsp,
  isExtendedResultsPost,
  isSearchSubmitSuccessStatus,
  isSearchSubmitFailureStatus,
  terminalSuccessSelector,
  classifySubmitOutcome,
  isSuccessfulSubmitOutcome,
};
