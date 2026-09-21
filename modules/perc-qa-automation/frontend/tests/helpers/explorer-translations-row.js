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
  const text = String(rowText == null ? "" : rowText).trim();
  if (!name && !text) {
    return false;
  }
  return PREFERRED_CONTENT_NAMES.some(
    (wanted) =>
      name === wanted ||
      foldedNamesEqual(name, wanted) ||
      (text.length > 0 && foldedNamesEqual(text, wanted)),
  );
}

/**
 * GUID-shaped id from a pathmanagement / list JSON object.
 * @param {unknown} item
 * @returns {string}
 */
function guidFromPathItem(item) {
  if (item == null || typeof item !== "object") {
    return "";
  }
  const rec = /** @type {Record<string, unknown>} */ (item);
  return (
    guidShapedIdFromText(rec.id) ||
    guidShapedIdFromText(rec.itemId) ||
    guidShapedIdFromText(rec.sysId)
  );
}

/**
 * Parent CMS folder of a listed item path (logical {@code /} paths).
 * @param {unknown} itemPath
 * @returns {string}
 */
function parentFolderCmsPath(itemPath) {
  let p = String(itemPath == null ? "" : itemPath)
    .trim()
    .replace(/\\/g, "/");
  while (p.startsWith("//")) {
    p = p.slice(1);
  }
  if (p && !p.startsWith("/")) {
    p = `/${p}`;
  }
  if (p.length > 1 && p.endsWith("/")) {
    p = p.replace(/\/+$/, "");
  }
  const idx = p.lastIndexOf("/");
  if (idx <= 0) {
    return p || "/";
  }
  return p.slice(0, idx) || "/";
}

/**
 * Folder walk segments after the repository root ({@code Sites}).
 * @param {unknown} folderPath
 * @returns {string[]}
 */
function cmsFolderWalkSegments(folderPath) {
  let p = String(folderPath == null ? "" : folderPath)
    .trim()
    .replace(/\\/g, "/");
  while (p.startsWith("//")) {
    p = p.slice(1);
  }
  if (p.startsWith("/")) {
    p = p.slice(1);
  }
  if (p.endsWith("/")) {
    p = p.replace(/\/+$/, "");
  }
  const parts = p.split("/").filter(Boolean);
  if (parts.length === 0) {
    return [];
  }
  if (foldedNamesEqual(parts[0], "Sites")) {
    return parts.slice(1);
  }
  return parts;
}

/**
 * Prefer a GUID-shaped listed page, then any GUID item (not folders).
 * @param {unknown[]} items
 * @returns {object|null}
 */
function pickGuidListedItem(items) {
  const list = Array.isArray(items) ? items.filter(Boolean) : [];
  const guidItems = list.filter((it) => {
    if (!guidFromPathItem(it)) {
      return false;
    }
    const type = `${it.type || ""} ${it.category || ""}`.toLowerCase();
    if (type.includes("folder") || type.includes("fsfolder") || type.includes("site")) {
      return false;
    }
    return true;
  });
  if (guidItems.length === 0) {
    return null;
  }
  const preferred = guidItems.find((it) =>
    isPreferredContentRowName(it.name, it.path),
  );
  return preferred || guidItems[0];
}

function isFolderishPathItem(item) {
  if (item == null || typeof item !== "object") {
    return false;
  }
  const type = `${item.type || ""} ${item.category || ""}`.toLowerCase();
  if (type.includes("folder") || type.includes("site")) {
    return true;
  }
  return String(item.path || "").endsWith("/");
}

/**
 * pathmanagement folders to search for a GUID page (site root + Pages).
 * @param {unknown[]} sites
 * @returns {string[]}
 */
function guidListCandidateFolders(sites) {
  const list = Array.isArray(sites) ? sites.filter(Boolean) : [];
  const folders = [];
  const seen = new Set();
  const add = (raw) => {
    const s = String(raw || "").replace(/^\/+/, "").replace(/\/+$/, "");
    if (!s || seen.has(s)) {
      return;
    }
    seen.add(s);
    folders.push(s);
  };
  for (const site of list) {
    const listPath = String(
      (site && (site.folderPath || site.path)) || "",
    )
      .trim()
      .replace(/\\/g, "/");
    let normalized = listPath;
    while (normalized.startsWith("//")) {
      normalized = normalized.slice(1);
    }
    if (normalized && !normalized.startsWith("/")) {
      normalized = `/${normalized}`;
    }
    if (normalized.length > 1 && normalized.endsWith("/")) {
      normalized = normalized.replace(/\/+$/, "");
    }
    if (normalized) {
      add(normalized);
      add(`${normalized}/Pages`);
    }
    const name = site && site.name ? String(site.name) : "";
    if (name) {
      add(`Sites/${name}`);
      add(`Sites/${name}/Pages`);
    }
  }
  return folders;
}

function nestedFolderRel(kid, parentFolder) {
  if (!isFolderishPathItem(kid)) {
    return "";
  }
  const nestedPath = String((kid && (kid.folderPath || kid.path)) || "")
    .trim()
    .replace(/\\/g, "/");
  let p = nestedPath;
  while (p.startsWith("//")) {
    p = p.slice(1);
  }
  if (p && !p.startsWith("/")) {
    p = `/${p}`;
  }
  if (p.length > 1 && p.endsWith("/")) {
    p = p.replace(/\/+$/, "");
  }
  if (p) {
    return p.replace(/^\/+/, "");
  }
  const name = kid && kid.name ? String(kid.name) : "";
  if (!name) {
    return "";
  }
  return `${parentFolder}/${name}`;
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
  guidFromPathItem,
  parentFolderCmsPath,
  cmsFolderWalkSegments,
  pickGuidListedItem,
  isFolderishPathItem,
  guidListCandidateFolders,
  nestedFolderRel,
};
