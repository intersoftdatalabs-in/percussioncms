/**
 * Selectors for Developer catalog tables that use WebUI SimpleCatalogTable.
 *
 * Product rows are indexed: data-testid="${rowTestIdBase}-${index}"
 * (see WebUI CatalogTable.tsx). Bare [data-testid="developer-ct-row"] never
 * matches; Vitest uses developer-ct-row-0 etc. (#2186 / matrix #2185).
 *
 * @module helpers/developer-catalog-selectors
 */

"use strict";

/**
 * CSS selector matching all indexed rows for a catalog rowTestId base.
 * Example: catalogRowsSelector("developer-ct-row") →
 *   [data-testid^="developer-ct-row-"]
 *
 * @param {string} rowTestIdBase base without trailing index (e.g. developer-tpl-row)
 * @returns {string}
 */
function catalogRowsSelector(rowTestIdBase) {
  if (typeof rowTestIdBase !== "string" || !rowTestIdBase.trim()) {
    throw new TypeError("rowTestIdBase must be a non-empty string");
  }
  const base = rowTestIdBase.trim();
  // Require trailing "-" so bare developer-ct-row does not match accidentally
  // if a non-indexed id is ever reintroduced, and so developer-ct-row-* only
  // matches index suffixes (not developer-ct-rowfoo).
  return `[data-testid^="${base}-"]`;
}

/**
 * CSS selector for a single indexed catalog row.
 * Example: catalogRowSelector("developer-tpl-row", 0) →
 *   [data-testid="developer-tpl-row-0"]
 *
 * @param {string} rowTestIdBase
 * @param {number} index zero-based row index
 * @returns {string}
 */
function catalogRowSelector(rowTestIdBase, index) {
  if (typeof rowTestIdBase !== "string" || !rowTestIdBase.trim()) {
    throw new TypeError("rowTestIdBase must be a non-empty string");
  }
  if (!Number.isInteger(index) || index < 0) {
    throw new TypeError("index must be a non-negative integer");
  }
  return `[data-testid="${rowTestIdBase.trim()}-${index}"]`;
}

/**
 * Escape a value for use inside a double-quoted CSS attribute selector.
 * Prefer CSS.escape when present (browsers); Node unit tests use a fallback.
 *
 * @param {string} value
 * @returns {string}
 */
function cssAttrEscape(value) {
  if (typeof CSS !== "undefined" && typeof CSS.escape === "function") {
    return CSS.escape(value);
  }
  return String(value).replace(/\\/g, "\\\\").replace(/"/g, '\\"');
}

/**
 * Exact-name catalog open control (not Playwright hasText substring).
 * Example: catalogOpenByExactName("developer-df-open", "data-df-name", "By_Author")
 * → [data-testid="developer-df-open"][data-df-name="By_Author"]
 *
 * @param {string} openTestId e.g. developer-df-open
 * @param {string} dataNameAttr e.g. data-df-name
 * @param {string} name exact catalog name
 * @returns {string}
 */
function catalogOpenByExactName(openTestId, dataNameAttr, name) {
  if (typeof openTestId !== "string" || !openTestId.trim()) {
    throw new TypeError("openTestId must be a non-empty string");
  }
  if (typeof dataNameAttr !== "string" || !dataNameAttr.startsWith("data-")) {
    throw new TypeError("dataNameAttr must be a data-* attribute name");
  }
  if (typeof name !== "string" || !name) {
    throw new TypeError("name must be a non-empty string");
  }
  return `[data-testid="${openTestId.trim()}"][${dataNameAttr}="${cssAttrEscape(name)}"]`;
}

/**
 * Exact-name catalog row via data-* identity (indexed testid still present).
 * Example: catalogRowByExactName("data-df-name", "By_Author")
 * → tr[data-df-name="By_Author"]
 *
 * @param {string} dataNameAttr
 * @param {string} name
 * @returns {string}
 */
function catalogRowByExactName(dataNameAttr, name) {
  if (typeof dataNameAttr !== "string" || !dataNameAttr.startsWith("data-")) {
    throw new TypeError("dataNameAttr must be a data-* attribute name");
  }
  if (typeof name !== "string" || !name) {
    throw new TypeError("name must be a non-empty string");
  }
  return `tr[${dataNameAttr}="${cssAttrEscape(name)}"]`;
}

module.exports = {
  catalogRowsSelector,
  catalogRowSelector,
  catalogOpenByExactName,
  catalogRowByExactName,
  cssAttrEscape,
};
