/**
 * Explorer display-format switch helpers (#3618 / parent #3102).
 *
 * <p>Product-route proof that changing the Explorer display-format selector
 * reloads {@code detail-list} with pathmanagement {@code displayFormatId}.
 * Must not soft-skip when the selector has more than one option or the
 * catalog has two or more {@code validForFolder} formats.</p>
 *
 * @see tests/explorer-display-format-switch.spec.js
 */

"use strict";

const {
  TEST_IDS: SHELL_TEST_IDS,
  explorerSpaUrl,
} = require("./explorer-shell-chrome");
const {
  sitesFolderUrl,
  assetsFolderUrl,
  pathItemNames,
  sitesTreeRootLocator,
  expandExplorerTreeNode,
  sitesTreeDescendantsLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./explorer-sites-assets-tree-list");

const TEST_IDS = Object.freeze({
  ...SHELL_TEST_IDS,
  displayFormat: "explorer-display-format",
  detailList: "detail-list",
  detailEmpty: "detail-list-empty",
  detailRowPrefix: "detail-row-",
  colHeaderPrefix: "detail-col-header-",
});

/**
 * Catalog URL for display formats.
 *
 * @param {string} baseUrl
 * @param {{ validForFolder?: boolean }} [opts]
 * @returns {string}
 */
function displayFormatsCatalogUrl(baseUrl, opts = {}) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const path = `${root}/Rhythmyx/services/displayformats`;
  if (opts.validForFolder === true) {
    return `${path}?validForFolder=true`;
  }
  return path;
}

/**
 * Unwrap Jackson {@code DisplayFormat} list / single / array payloads.
 *
 * @param {unknown} payload
 * @returns {object[]}
 */
function unwrapDisplayFormatCatalog(payload) {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload.filter((row) => row && typeof row === "object");
  }
  if (typeof payload !== "object") {
    return [];
  }
  const rec = /** @type {Record<string, unknown>} */ (payload);
  const nested =
    rec.DisplayFormatList ??
    rec.displayFormatList ??
    rec.DisplayFormat ??
    rec.displayFormat;
  if (Array.isArray(nested)) {
    return nested.filter((row) => row && typeof row === "object");
  }
  if (nested && typeof nested === "object") {
    const inner = /** @type {Record<string, unknown>} */ (nested);
    const innerList = inner.DisplayFormat ?? inner.displayFormat;
    if (Array.isArray(innerList)) {
      return innerList.filter((row) => row && typeof row === "object");
    }
    return [nested];
  }
  return [];
}

/**
 * Pathmanagement {@code displayFormatId} must be a positive integer.
 *
 * @param {string | null | undefined} id
 * @returns {boolean}
 */
function isNumericDisplayFormatId(id) {
  if (id == null) {
    return false;
  }
  return /^[1-9]\d*$/.test(String(id).trim());
}

/**
 * Option values from a &lt;select&gt; excluding the empty default.
 *
 * @param {readonly string[] | undefined} values
 * @returns {string[]}
 */
function nonEmptySelectOptionValues(values) {
  return (values || [])
    .map((v) => String(v || "").trim())
    .filter((v) => v.length > 0);
}

/**
 * True when {@code url} is a paginatedFolder request for {@code formatId}.
 *
 * @param {string | null | undefined} url
 * @param {string} formatId
 * @returns {boolean}
 */
function isPaginatedFolderDisplayFormatRequest(url, formatId) {
  const u = String(url || "");
  if (!u.includes("/paginatedFolder/")) {
    return false;
  }
  const id = String(formatId || "").trim();
  if (!id) {
    return false;
  }
  return (
    u.includes(`displayFormatId=${encodeURIComponent(id)}`) ||
    u.includes(`displayFormatId=${id}`)
  );
}

/**
 * Display-format switch (#3618) must not soft-skip when the selector has
 * more than one option or the catalog lists two or more formats.
 *
 * @param {{
 *   optionCount?: number,
 *   formatCount?: number,
 * }} [detail]
 * @returns {boolean}
 */
function shouldSkipDisplayFormatSwitch(detail = {}) {
  const options = Number(detail.optionCount) || 0;
  const formats = Number(detail.formatCount) || 0;
  if (options > 1 || formats >= 2) {
    return false;
  }
  return false;
}

module.exports = {
  TEST_IDS,
  explorerSpaUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  pathItemNames,
  sitesTreeRootLocator,
  expandExplorerTreeNode,
  sitesTreeDescendantsLocator,
  isKnownExplorerSitesConsoleNoise,
  displayFormatsCatalogUrl,
  unwrapDisplayFormatCatalog,
  isNumericDisplayFormatId,
  nonEmptySelectOptionValues,
  isPaginatedFolderDisplayFormatRequest,
  shouldSkipDisplayFormatSwitch,
};
