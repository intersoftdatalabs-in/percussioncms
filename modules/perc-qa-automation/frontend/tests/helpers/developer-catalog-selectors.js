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

module.exports = {
  catalogRowsSelector,
  catalogRowSelector,
};
