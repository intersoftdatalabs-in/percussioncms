/**
 * Developer entry + critical catalog Playwright smoke inventory (#2188).
 *
 * Epic #2089 acceptance gate: each entry is expected GREEN on H2 qa-up, or
 * codified as skip-with-BUG (durable issue URL — never a silent flake).
 *
 * Used by unit tests and docs generation; live specs mirror these skip reasons
 * via {@code test.skip(..., "BUG: … issue URL")}.
 *
 * @module helpers/developer-smoke-set
 */

"use strict";

/** @typedef {"green" | "skip"} SmokeStatus */

/**
 * @typedef {object} DeveloperSmokeEntry
 * @property {string} id stable inventory id
 * @property {string} file Playwright spec under tests/
 * @property {string} title substring matching the test title (for operators)
 * @property {SmokeStatus} status expected gate status until residuals close
 * @property {number} [bugIssue] GitHub issue number when status is skip
 * @property {string} [bugUrl] durable issue URL when status is skip
 * @property {string} [notes] short operator note
 */

const REPO_ISSUES =
  "https://github.com/intersoftdatalabs-in/percussioncms/issues";

/**
 * Full smoke inventory for Developer entry + critical catalogs (#2188).
 * Keep in lockstep with @smoke tags / test.skip BUG notes in the listed specs.
 *
 * @type {DeveloperSmokeEntry[]}
 */
const DEVELOPER_SMOKE_SET = [
  {
    id: "golden-qa-env",
    file: "golden-unattended-smoke.spec.js",
    title: "QA env resolves without host install",
    status: "green",
    notes: "Env contract only; no live CMS required for this case",
  },
  {
    id: "golden-login-explorer",
    file: "golden-unattended-smoke.spec.js",
    title: "Admin login + Content Explorer shell",
    status: "green",
    notes: "H2 qa-up golden path baseline (#2065 / #2185 matrix)",
  },
  {
    id: "login-admin",
    file: "login.spec.js",
    title: "logs in and lands on a non-login Rhythmyx page",
    status: "green",
    notes: "Auth entry baseline",
  },
  {
    id: "login-base-url",
    file: "login.spec.js",
    title: "BASE_URL is auto-discovered",
    status: "green",
    notes: "Resolver contract",
  },
  {
    id: "rest-slots",
    file: "developer-catalog-smoke.spec.js",
    title: "REST: GET /services/slots returns 2xx",
    status: "green",
    notes: "Critical REST catalog (#2121)",
  },
  {
    id: "catalog-content-types",
    file: "developer-catalog-smoke.spec.js",
    title: "content-types: catalog loads without API error",
    status: "skip",
    bugIssue: 2186,
    bugUrl: `${REPO_ISSUES}/2186`,
    notes:
      "Matrix RED: bare developer-ct-row vs indexed developer-ct-row-N (selector harden PR)",
  },
  {
    id: "catalog-keywords",
    file: "developer-catalog-smoke.spec.js",
    title: "keywords: catalog loads without API error",
    status: "green",
  },
  {
    id: "catalog-locales",
    file: "developer-catalog-smoke.spec.js",
    title: "locales: catalog loads without API error",
    status: "green",
  },
  {
    id: "locale-editor",
    file: "developer-locale-editor.spec.js",
    title: "Admin can create, save, and delete a locale",
    status: "green",
    notes: "CD-18 SPA locale editor (#4005 / parent #1690)",
  },
  {
    id: "auto-translation-editor",
    file: "developer-auto-translation-editor.spec.js",
    title: "Admin can add a locale×content-type row, save, and round-trip",
    status: "green",
    notes: "CD-18 SPA auto-translation editor (#4028 / parent #1690)",
  },
  {
    id: "catalog-slots",
    file: "developer-catalog-smoke.spec.js",
    title: "slots: catalog loads without API error",
    status: "green",
  },
  {
    id: "catalog-shared-fields",
    file: "developer-catalog-smoke.spec.js",
    title: "shared-fields: catalog loads without API error",
    status: "green",
  },
  {
    id: "shared-fields-editor",
    file: "developer-shared-fields-editor.spec.js",
    title: "Admin can create, save, and delete a shared field group",
    status: "green",
    notes: "CD-15 SPA shared-field group write (#4029 / parent #1690)",
  },
  {
    id: "catalog-system-def",
    file: "developer-catalog-smoke.spec.js",
    title: "system-def: catalog loads without API error",
    status: "green",
  },
  {
    id: "system-def-writes",
    file: "developer-system-def-writes.spec.js",
    title: "system-def: REST add/save/delete durable on H2 (#4037)",
    status: "green",
  },
  {
    id: "system-def-editor",
    file: "developer-system-def-editor.spec.js",
    title: "Admin sees system-def save/add/delete chrome",
    status: "green",
    notes: "CD-16 SPA system-def field write (#4030 / parent #1690)",
  },
  {
    id: "template-source-viewer",
    file: "developer-template-source-viewer.spec.js",
    title: "template detail source shows line numbers and copy control",
    status: "skip",
    bugIssue: 2189,
    bugUrl: `${REPO_ISSUES}/2189`,
    notes:
      "Product TemplateSummary name/label empty (#2189); also selector #2186 until harden merges",
  },
];

/**
 * Skip reason string for Playwright test.skip (includes durable issue URL).
 *
 * @param {DeveloperSmokeEntry} entry
 * @returns {string}
 */
function skipReasonFor(entry) {
  if (!entry || entry.status !== "skip") {
    throw new TypeError("entry must be a skip-status DeveloperSmokeEntry");
  }
  if (!entry.bugUrl || !entry.bugIssue) {
    throw new TypeError("skip entries require bugIssue and bugUrl");
  }
  const note = entry.notes ? ` ${entry.notes}` : "";
  return `BUG: ${entry.id} blocked on #${entry.bugIssue} — ${entry.bugUrl}.${note}`;
}

/**
 * @param {string} id inventory id
 * @returns {DeveloperSmokeEntry}
 */
function getSmokeEntry(id) {
  const found = DEVELOPER_SMOKE_SET.find((e) => e.id === id);
  if (!found) {
    throw new Error(`Unknown developer smoke entry id: ${id}`);
  }
  return found;
}

/**
 * @returns {DeveloperSmokeEntry[]}
 */
function listSkipEntries() {
  return DEVELOPER_SMOKE_SET.filter((e) => e.status === "skip");
}

/**
 * @returns {DeveloperSmokeEntry[]}
 */
function listGreenEntries() {
  return DEVELOPER_SMOKE_SET.filter((e) => e.status === "green");
}

module.exports = {
  DEVELOPER_SMOKE_SET,
  REPO_ISSUES,
  skipReasonFor,
  getSmokeEntry,
  listSkipEntries,
  listGreenEntries,
};
