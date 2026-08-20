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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Explorer Workflow transition no-skip helpers (#3639 / parent #3102 / #2732).
 *
 * <p>Pure helpers for surface-filtered Playwright: Jackson unwrap of
 * {@code GET .../itemmanagement/workflow/getTransitions/{id}}, skip policy
 * (never skip on H2 when a listed item exists), and folder vs item
 * eligibility. No live CMS dependency.</p>
 *
 * @see tests/explorer-workflow-transitions.spec.js
 */

"use strict";

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  tree: "explorer-tree",
  list: "detail-list",
  actionToolbar: "action-toolbar",
  serverActions: "explorer-server-actions",
  workflowGroup: "action-toolbar-group-workflow",
  workflowTransitionPrefix: "action-toolbar-item-workflow-transition:",
});

const PRODUCT_ISSUES = Object.freeze({
  parent: 3102,
  slice: 3639,
  workflowChrome: 2732,
  repo: "https://github.com/intersoftdatalabs-in/percussioncms/issues",
});

/** JAX-RS getTransitions defaults to XML without this. */
const JSON_ACCEPT_HEADERS = Object.freeze({
  Accept: "application/json",
});

/** Jackson / JAXB roots for {@code PSItemStateTransition}. */
const ITEM_STATE_TRANSITION_ROOTS = Object.freeze([
  "ItemStateTransition",
  "PSItemStateTransition",
]);

/**
 * @param {string} baseUrl
 * @param {{ cacheBuster?: string|number }} [opts]
 * @returns {string}
 */
function explorerEntryUrl(baseUrl, opts = {}) {
  const base = String(baseUrl || "").replace(/\/+$/, "");
  const bust =
    opts.cacheBuster != null
      ? encodeURIComponent(String(opts.cacheBuster))
      : String(Date.now());
  return `${base}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${bust}`;
}

/**
 * {@code GET} getTransitions URL (logical {@code /} path, not OS).
 * @param {string} baseUrl
 * @param {string} itemId
 * @returns {string}
 */
function workflowTransitionsUrl(baseUrl, itemId) {
  const base = String(baseUrl || "").replace(/\/+$/, "");
  const id = String(itemId || "").trim();
  if (!id) {
    return "";
  }
  return `${base}/Rhythmyx/services/itemmanagement/workflow/getTransitions/${encodeURIComponent(id)}`;
}

/**
 * {@code GET} transitionWithComments URL.
 * @param {string} baseUrl
 * @param {string} itemId
 * @param {string} trigger
 * @returns {string}
 */
function workflowTransitionInvokeUrl(baseUrl, itemId, trigger) {
  const base = String(baseUrl || "").replace(/\/+$/, "");
  const id = String(itemId || "").trim();
  const trig = String(trigger || "").trim();
  if (!id || !trig) {
    return "";
  }
  return (
    `${base}/Rhythmyx/services/itemmanagement/workflow/transitionWithComments/` +
    `${encodeURIComponent(id)}/${encodeURIComponent(trig)}`
  );
}

/**
 * @param {unknown} value
 * @returns {Record<string, unknown>|null}
 */
function asRecord(value) {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return /** @type {Record<string, unknown>} */ (value);
  }
  return null;
}

/**
 * Coerce Jackson list / single-string / JAXB {@code string} wrappers.
 * @param {unknown} raw
 * @returns {string[]}
 */
function coerceTransitionTriggers(raw) {
  if (raw == null) {
    return [];
  }
  if (typeof raw === "string") {
    const t = raw.trim();
    return t.length > 0 ? [t] : [];
  }
  if (Array.isArray(raw)) {
    return raw
      .map((entry) => String(entry ?? "").trim())
      .filter((entry) => entry.length > 0);
  }
  const obj = asRecord(raw);
  if (!obj) {
    return [];
  }
  for (const key of [
    "string",
    "String",
    "transitionTriggers",
    "transitionTrigger",
  ]) {
    if (!(key in obj)) {
      continue;
    }
    const inner = coerceTransitionTriggers(obj[key]);
    if (inner.length > 0) {
      return inner;
    }
  }
  return [];
}

/**
 * Unwrap Jackson WRAP_ROOT {@code ItemStateTransition} or a flat DTO.
 * @param {unknown} data
 * @returns {{
 *   itemId?: string,
 *   stateName?: string,
 *   workflowId?: string,
 *   transitionTriggers: string[],
 * }}
 */
function unwrapItemStateTransition(data) {
  const root = asRecord(data);
  if (!root) {
    return { transitionTriggers: [] };
  }
  let body = root;
  for (const name of ITEM_STATE_TRANSITION_ROOTS) {
    const nested = asRecord(root[name]);
    if (nested) {
      body = nested;
      break;
    }
  }
  const itemId =
    body.itemId != null && String(body.itemId).trim()
      ? String(body.itemId).trim()
      : undefined;
  const stateName =
    body.stateName != null && String(body.stateName).trim()
      ? String(body.stateName).trim()
      : undefined;
  return {
    itemId,
    stateName,
    workflowId:
      body.workflowId != null && String(body.workflowId).trim()
        ? String(body.workflowId).trim()
        : undefined,
    transitionTriggers: coerceTransitionTriggers(body.transitionTriggers),
  };
}

/**
 * Path item content id (string, number, or Jackson GUID object).
 * @param {unknown} item
 * @returns {string}
 */
function listedItemContentId(item) {
  const rec = asRecord(item);
  if (!rec) {
    return "";
  }
  const rawId = rec.id;
  if (typeof rawId === "string" || typeof rawId === "number") {
    const s = String(rawId).trim();
    if (s) {
      return s;
    }
  } else {
    const guid = asRecord(rawId);
    if (guid) {
      const sv = guid.stringValue ?? guid.string;
      if (sv != null && String(sv).trim()) {
        return String(sv).trim();
      }
    }
  }
  const extras = asRecord(rec.displayProperties) || {};
  for (const key of ["sys_contentid", "sys_contentId", "contentId", "contentid"]) {
    if (extras[key] != null && String(extras[key]).trim()) {
      return String(extras[key]).trim();
    }
  }
  return "";
}

/**
 * True when a path row is a folder/site (must not show Workflow transitions).
 * @param {{ type?: string, category?: string, path?: string }} row
 * @returns {boolean}
 */
function isFolderishRow(row) {
  if (!row) {
    return true;
  }
  const token = `${row.type || ""} ${row.category || ""}`.toLowerCase();
  const path = String(row.path || "").replace(/\\/g, "/");
  if (
    token.includes("folder") ||
    token.includes("fsfolder") ||
    token.includes("site")
  ) {
    return true;
  }
  return path.length > 1 && path.endsWith("/");
}

/**
 * True when a listed path item can receive workflow transitions.
 * @param {{ type?: string, category?: string, path?: string, id?: unknown }} row
 * @returns {boolean}
 */
function isWorkflowEligibleRow(row) {
  if (!row || isFolderishRow(row)) {
    return false;
  }
  return listedItemContentId(row).length > 0;
}

/**
 * @param {string|undefined|null} dbType
 * @returns {boolean}
 */
function isH2Qa(dbType) {
  const envType = String(
    dbType != null && String(dbType).length > 0
      ? dbType
      : process.env.TEST_DB_TYPE || "",
  )
    .trim()
    .toLowerCase();
  return envType === "h2" || envType === "";
}

/**
 * Product-route proof must not fixture-skip when REST listed an eligible
 * item, or when H2 QA (demo-sites default) is under test.
 *
 * @param {{
 *   restEligibleCount?: number,
 *   dbType?: string,
 * }} [detail]
 * @returns {boolean}
 */
function shouldSkipWorkflowTransitionProof(detail = {}) {
  const eligible = Number(detail.restEligibleCount || 0);
  if (eligible > 0) {
    return false;
  }
  if (isH2Qa(detail.dbType)) {
    return false;
  }
  return true;
}

/**
 * @returns {string}
 */
function noEligibleItemSkipMessage() {
  return (
    `No listed transition-eligible content item under Sites after REST walk ` +
    `(#${PRODUCT_ISSUES.slice} / parent #${PRODUCT_ISSUES.parent}). ` +
    `H2 QA must not take this skip.`
  );
}

/**
 * @returns {string}
 */
function h2MissingEligibleMessage() {
  return (
    `H2 QA listed no transition-eligible content item under Sites/Pages ` +
    `(#${PRODUCT_ISSUES.slice}). Demo sites must include a workflowed page — ` +
    `do not fixture-skip.`
  );
}

/**
 * Honest transition invoke: HTTP 200, 4xx, or workflow-engine 500 that the
 * Explorer surfaces as {@code explorer-server-actions-error} (invalid
 * transition / checkout). Other 5xx remain product failures.
 * @param {number} status
 * @returns {boolean}
 */
function isHonestTransitionStatus(status) {
  const n = Number(status);
  if (!Number.isFinite(n)) {
    return false;
  }
  if (n === 200 || n === 500) {
    return true;
  }
  return n >= 400 && n < 500;
}

/**
 * True when a failed GET URL is the itemmanagement transition invoke.
 * @param {string} url
 * @returns {boolean}
 */
function isWorkflowTransitionInvokeUrl(url) {
  const u = String(url || "").replace(/\\/g, "/").toLowerCase();
  return (
    u.includes("/itemmanagement/workflow/transitionwithcomments/") ||
    u.includes("/itemmanagement/workflow/transition/")
  );
}

module.exports = {
  TEST_IDS,
  PRODUCT_ISSUES,
  ITEM_STATE_TRANSITION_ROOTS,
  JSON_ACCEPT_HEADERS,
  explorerEntryUrl,
  workflowTransitionsUrl,
  workflowTransitionInvokeUrl,
  coerceTransitionTriggers,
  unwrapItemStateTransition,
  listedItemContentId,
  isFolderishRow,
  isWorkflowEligibleRow,
  isH2Qa,
  shouldSkipWorkflowTransitionProof,
  noEligibleItemSkipMessage,
  h2MissingEligibleMessage,
  isHonestTransitionStatus,
  isWorkflowTransitionInvokeUrl,
};
