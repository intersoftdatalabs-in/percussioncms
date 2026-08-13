/**
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
 * Explorer Inbox Playwright helpers (#3241 / parent #3118 / #3102).
 *
 * <p>Inbox is DCE {@code //Views//MyContent/Inbox} — not a CE root.
 * Selectors match planned leaf/testids from Views catalog + Inbox leaf
 * (#3116 / #3240): {@code explorer-views-leaf-Inbox}.</p>
 */

"use strict";

/** Stable product test ids for the Inbox leaf / results surface. */
const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  nav: "explorer-nav",
  viewsTree: "explorer-views-tree",
  viewsRoot: "explorer-views-root",
  myContentGroup: "explorer-views-group-1",
  myContentGroupRow: "explorer-views-group-1-row",
  inboxLeaf: "explorer-views-leaf-Inbox",
  inboxIcon: "explorer-views-inbox-icon",
  inboxMarker: "explorer-views-inbox",
  results: "explorer-view-results",
  resultsList: "explorer-view-results-list",
  resultsEmpty: "explorer-view-results-empty",
  resultsError: "explorer-view-results-error",
  resultsLoading: "explorer-view-results-loading",
});

/** DCE PARAM_PATH_INBOX. */
const PATH_MY_CONTENT_INBOX = "//Views//MyContent/Inbox";

/** Typical design INTERNALNAME / catalog name. */
const INBOX_VIEW_NAME = "Inbox";

/** CX views catalog (same path as WebUI viewsApi). */
const PATH_VIEWS = "/Rhythmyx/services/views";

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
 * Absolute URL for GET view catalog.
 *
 * @param {string} baseUrl
 * @returns {string}
 */
function viewsCatalogUrl(baseUrl) {
  const root = String(baseUrl || "")
    .trim()
    .replace(/\/+$/, "");
  return `${root}${PATH_VIEWS}`;
}

/**
 * Absolute URL for POST execute of one view (Inbox C1).
 *
 * @param {string} baseUrl
 * @param {string} idOrName
 * @returns {string}
 */
function viewsExecuteUrl(baseUrl, idOrName) {
  const root = String(baseUrl || "")
    .trim()
    .replace(/\/+$/, "");
  const key = encodeURIComponent(String(idOrName || "").trim());
  return `${root}${PATH_VIEWS}/${key}/execute`;
}

/**
 * Unwrap Jackson / list wrappers for {@code ViewDef[]} catalog payloads.
 *
 * @param {unknown} payload
 * @returns {Record<string, unknown>[]}
 */
function unwrapViewDefs(payload) {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload.filter((x) => x != null && typeof x === "object");
  }
  if (typeof payload === "object") {
    const obj = /** @type {Record<string, unknown>} */ (payload);
    const raw = obj.ViewDef ?? obj.viewDef ?? obj.ViewDefList ?? obj.items;
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
 * @param {unknown} raw
 * @returns {string}
 */
function normalizeInboxToken(raw) {
  return String(raw ?? "")
    .trim()
    .replace(/\\/g, "/");
}

/**
 * True when {@code def} is the system Inbox view.
 *
 * @param {Record<string, unknown> | null | undefined} def
 * @returns {boolean}
 */
function isInboxView(def) {
  if (def == null || typeof def !== "object") {
    return false;
  }
  const name = normalizeInboxToken(def.name);
  const label = normalizeInboxToken(def.label);
  if (name.toLowerCase() === "inbox" || label.toLowerCase() === "inbox") {
    return true;
  }
  const url = normalizeInboxToken(def.url ?? def.customUrl);
  if (/sys_cxViews\/inbox(\.xml)?$/.test(url)) {
    return true;
  }
  // Same DCE path rule as WebUI viewCatalog.ts (case-insensitive).
  return /\/\/Views\/\/MyContent\/Inbox$/i.test(name);
}

/**
 * First Inbox view from a catalog payload, or null.
 *
 * @param {unknown} payload
 * @returns {Record<string, unknown> | null}
 */
function findInboxView(payload) {
  const defs = unwrapViewDefs(payload);
  return defs.find((d) => isInboxView(d)) || null;
}

/**
 * CSS selector for the Inbox leaf (testid + optional DCE path attribute).
 *
 * @returns {string}
 */
function inboxLeafSelector() {
  return `[data-testid="${TEST_IDS.inboxLeaf}"]`;
}

/**
 * CSS selector joining Inbox post-execute regions.
 *
 * @returns {string}
 */
function inboxResultsSelector() {
  return [
    `[data-testid="${TEST_IDS.resultsLoading}"]`,
    `[data-testid="${TEST_IDS.resultsError}"]`,
    `[data-testid="${TEST_IDS.resultsEmpty}"]`,
    `[data-testid="${TEST_IDS.resultsList}"]`,
  ].join(", ");
}

/**
 * Soft-skip when Explorer has no Inbox leaf / Views catalog on this cell.
 *
 * @param {{ restStatus?: number, catalogEmpty?: boolean }} [detail]
 * @returns {string}
 */
function missingInboxSkipMessage(detail = {}) {
  const parts = [
    "Inbox leaf not on Explorer product route (Views → My Content → Inbox).",
    "Soft-skip until #3240 leaf + #3239 execute are deployed on this cell (#3241).",
  ];
  if (detail.catalogEmpty) {
    parts.push("GET /services/views returned no Inbox view.");
  }
  if (detail.restStatus != null) {
    parts.push(`REST status=${detail.restStatus}.`);
  }
  return parts.join(" ");
}

/**
 * Soft-skip when Inbox ran but the fixture has no assignment rows.
 *
 * @param {{ restStatus?: number }} [detail]
 * @returns {string}
 */
function noAssignmentsSkipMessage(detail = {}) {
  const parts = [
    "Inbox surface present but H2 fixture has no assignment rows.",
    "Empty Inbox is valid (200 children:[]); skip row-level assertions (#3241).",
  ];
  if (detail.restStatus != null) {
    parts.push(`REST status=${detail.restStatus}.`);
  }
  return parts.join(" ");
}

module.exports = {
  TEST_IDS,
  PATH_MY_CONTENT_INBOX,
  INBOX_VIEW_NAME,
  PATH_VIEWS,
  explorerEntryUrl,
  viewsCatalogUrl,
  viewsExecuteUrl,
  unwrapViewDefs,
  isInboxView,
  findInboxView,
  inboxLeafSelector,
  inboxResultsSelector,
  missingInboxSkipMessage,
  noAssignmentsSkipMessage,
};
