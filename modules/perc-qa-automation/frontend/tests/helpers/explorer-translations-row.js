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
 * Pure helpers for Explorer Translations Playwright (#3871 / parent #2649).
 *
 * <p>Row identity for Translations GET must be a Percussion
 * {@code host-type-uuid} GUID (or the same adaptor key). Do not extract a
 * GUID token from a path-shaped test id — {@code /Sites/16777215-101-703/…}
 * would pick the site id, not the page.</p>
 */

"use strict";

/** Exact Percussion GUID {@code host-type-uuid}. */
const GUID_HOST_TYPE_UUID = /^(\d+-\d+-\d+)$/;

/** FastForward / sample page names used when drilling to a content row. */
const PREFERRED_CONTENT_NAMES = Object.freeze([
  "Corporate Investments Home",
  "CorporateInvestments Home",
  "Corporate_Investments Home",
]);

/**
 * GUID-shaped id from a data-testid / data-item-id value.
 * @param {unknown} raw
 * @returns {string} empty when the value is not a host-type-uuid (or
 *   {@code detail-row-<guid>})
 */
function guidShapedIdFromText(raw) {
  const s = String(raw == null ? "" : raw).trim();
  if (!s) {
    return "";
  }
  if (GUID_HOST_TYPE_UUID.test(s)) {
    return s;
  }
  const prefixed = /^detail-row-(\d+-\d+-\d+)$/.exec(s);
  if (prefixed) {
    return prefixed[1];
  }
  return "";
}

/**
 * Translations-usable id from Explorer row attributes.
 * Prefers {@code data-item-id} over {@code data-testid}.
 * @param {{ itemId?: unknown, testId?: unknown }} attrs
 * @returns {string}
 */
function translationsRowIdFromAttrs(attrs) {
  const rec = attrs && typeof attrs === "object" ? attrs : {};
  const fromItem = guidShapedIdFromText(rec.itemId);
  if (fromItem) {
    return fromItem;
  }
  return guidShapedIdFromText(rec.testId);
}

/**
 * True when the visible name matches a preferred sample content row.
 * @param {unknown} itemName
 * @param {unknown} rowText
 * @returns {boolean}
 */
function isPreferredContentRowName(itemName, rowText) {
  const name = String(itemName == null ? "" : itemName).trim();
  if (!name) {
    return false;
  }
  for (const wanted of PREFERRED_CONTENT_NAMES) {
    if (name === wanted) {
      return true;
    }
  }
  return false;
}

/**
 * Fold finder / repository names so spaces and underscores match.
 * @param {unknown} name
 * @returns {string}
 */
function foldExplorerName(name) {
  return String(name == null ? "" : name)
    .toLowerCase()
    .replace(/[_\s-]+/g, "");
}

/**
 * True when two folder/site labels are the same after folding.
 * @param {unknown} actual
 * @param {unknown} wanted
 * @returns {boolean}
 */
function foldedNamesEqual(actual, wanted) {
  const left = foldExplorerName(actual);
  const right = foldExplorerName(wanted);
  return left.length > 0 && left === right;
}

module.exports = {
  GUID_HOST_TYPE_UUID,
  PREFERRED_CONTENT_NAMES,
  guidShapedIdFromText,
  translationsRowIdFromAttrs,
  isPreferredContentRowName,
  foldExplorerName,
  foldedNamesEqual,
};
