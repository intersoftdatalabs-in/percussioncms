/**
 * Pure helpers for demo-sites / Sample Site residual (#1750 / #2194).
 *
 * Used by Playwright bug-1750 (Sites under Explorer after --demo-sites) and
 * covered by Node unit tests under tests/unit/ (no live CMS).
 *
 * Stock seed names (Corporate / Enterprise Investments) come from
 * installRepository installSampleSites / Rxff sample data. Folder labels under
 * pathmanagement typically use spaces; RXSITES.SITENAME may use underscores —
 * normalize before compare.
 *
 * @see tests/bugs/bug-1750-demo-sites-sample-site.spec.js
 * @see docs/ai-generated/issue-2191-demo-sites-empty-sites-repro.md
 */

"use strict";

/** Canonical folder labels for the two stock sample sites. */
const EXPECTED_SAMPLE_SITE_NAMES = Object.freeze([
  "Corporate Investments",
  "Enterprise Investments",
]);

/**
 * Env keys that force hard assertions (fail on empty Sites after demo-sites).
 * Set after a silent install with --demo-sites (or interactive Yes) once
 * product fix #2192 is in the image under test.
 */
const EXPECT_DEMO_SITES_ENV_KEYS = Object.freeze([
  "EXPECT_DEMO_SITES",
  "TEST_EXPECT_DEMO_SITES",
]);

const PRODUCT_FIX_ISSUE = 2192;
const PARENT_ISSUE = 1750;
const RESIDUAL_ISSUE = 2194;
const REPO_ISSUES =
  "https://github.com/intersoftdatalabs-in/percussioncms/issues";

/**
 * @param {string | undefined | null} value
 * @returns {string}
 */
function normalizeSiteName(value) {
  return String(value || "")
    .trim()
    .replace(/_/g, " ")
    .replace(/\s+/g, " ")
    .toLowerCase();
}

/**
 * Extract folder/item names from a pathmanagement path/folder JSON body.
 * Accepts PathItem wrapper or bare array peers of bug-1622.
 *
 * @param {unknown} body
 * @returns {string[]}
 */
/**
 * Children from a {@code PagedItemList} paginatedFolder JSON body.
 *
 * @param {unknown} body
 * @returns {unknown[]}
 */
function pagedItemListChildren(body) {
  if (body == null || typeof body !== "object") {
    return [];
  }
  const root = body.PagedItemList && typeof body.PagedItemList === "object"
    ? body.PagedItemList
    : body;
  const kids = root.childrenInPage ?? root.children;
  if (!Array.isArray(kids)) {
    return [];
  }
  return kids;
}

/**
 * @param {unknown} body
 * @returns {number}
 */
function pagedItemListCount(body) {
  if (body == null || typeof body !== "object") {
    return 0;
  }
  const root = body.PagedItemList && typeof body.PagedItemList === "object"
    ? body.PagedItemList
    : body;
  if (typeof root.childrenCount === "number") {
    return root.childrenCount;
  }
  return pagedItemListChildren(body).length;
}

/**
 * Whether a path item looks like a page (CM1 percPage or FastForward rff*).
 *
 * @param {unknown} item
 * @returns {boolean}
 */
function isPageTypeChild(item) {
  if (item == null || typeof item !== "object") {
    return false;
  }
  const type = String(item.type || "").trim().toLowerCase();
  const category = String(item.category || "").trim().toLowerCase();
  if (category === "page" || category === "landing_page") {
    return true;
  }
  if (type === "percpage" || type === "page") {
    return true;
  }
  if (type.startsWith("rff") && type !== "rfffile" && type !== "rffimage") {
    return !type.startsWith("rffnav");
  }
  return type.length > 0 && type !== "folder" && type !== "fsfolder" && type !== "site";
}

/**
 * Unwrap path/folder JSON into PathItem rows.
 * Accepts a bare array, {@code {PathItem:[...]}} / single-object wrap, or a
 * nested {@code PSPathItemList} envelope so H2 sample sites are not treated
 * as empty (false soft-skip, #3575).
 *
 * @param {unknown} body
 * @returns {unknown[]}
 */
function pathItemRows(body) {
  if (body == null) {
    return [];
  }
  if (Array.isArray(body)) {
    return body;
  }
  if (typeof body !== "object") {
    return [];
  }
  const rec = body;
  const direct = rec.PathItem ?? rec.pathItem;
  if (direct != null) {
    return Array.isArray(direct) ? direct : [direct];
  }
  const nested = rec.PSPathItemList ?? rec.PathItemList ?? rec.pathItemList;
  if (nested != null && nested !== rec) {
    return pathItemRows(nested);
  }
  return [];
}

function pathItemNames(body) {
  return pathItemRows(body)
    .map((it) => (it && typeof it === "object" ? it.name : null))
    .filter((n) => typeof n === "string" && n.trim().length > 0)
    .map((n) => String(n).trim());
}

/**
 * Whether {@code names} includes every expected sample site (normalized).
 *
 * @param {readonly string[]} names
 * @param {readonly string[]} [expected]
 * @returns {boolean}
 */
function hasAllExpectedSampleSites(
  names,
  expected = EXPECTED_SAMPLE_SITE_NAMES,
) {
  const normalized = new Set(
    (names || []).map(normalizeSiteName).filter(Boolean),
  );
  return expected.every((exp) => normalized.has(normalizeSiteName(exp)));
}

/**
 * Whether {@code names} includes at least one expected sample site.
 *
 * @param {readonly string[]} names
 * @param {readonly string[]} [expected]
 * @returns {boolean}
 */
function hasAnyExpectedSampleSite(
  names,
  expected = EXPECTED_SAMPLE_SITE_NAMES,
) {
  const normalized = new Set(
    (names || []).map(normalizeSiteName).filter(Boolean),
  );
  return expected.some((exp) => normalized.has(normalizeSiteName(exp)));
}

/**
 * @param {string | undefined | null} raw
 * @returns {boolean}
 */
function isTruthyEnvFlag(raw) {
  if (raw == null) {
    return false;
  }
  const v = String(raw).trim().toLowerCase();
  return v === "1" || v === "true" || v === "yes" || v === "on";
}

/**
 * When true, empty Sites / missing sample names must fail (regression gate).
 *
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} [env]
 * @returns {boolean}
 */
function shouldEnforceDemoSites(env = process.env) {
  for (const key of EXPECT_DEMO_SITES_ENV_KEYS) {
    if (isTruthyEnvFlag(env[key])) {
      return true;
    }
  }
  return false;
}

/**
 * Stock CMS+H2 QA cells pass installer {@code --demo-sites} unless
 * {@code DEMO_SITES} is an explicit falsey value.
 *
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} [env]
 * @returns {boolean}
 */
function isH2QaDemoSitesDefault(env = process.env) {
  const db = String(env.TEST_DB_TYPE || "").trim().toLowerCase();
  if (db !== "h2") {
    return false;
  }
  const raw = env.DEMO_SITES;
  if (raw == null || String(raw).trim() === "") {
    return true;
  }
  return isTruthyEnvFlag(raw);
}

/**
 * Tree+list / Sites-list must not soft-skip when sample content is present,
 * when {@code EXPECT_DEMO_SITES} is on, or on H2 QA (demo-sites default).
 *
 * @param {readonly string[] | null | undefined} names
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} [env]
 * @returns {boolean}
 */
function shouldSoftSkipSitesList(names, env = process.env) {
  if ((names || []).length > 0) {
    return false;
  }
  if (shouldEnforceDemoSites(env)) {
    return false;
  }
  if (isH2QaDemoSitesDefault(env)) {
    return false;
  }
  return true;
}

/**
 * Skip reason when demo sample sites are not present and enforcement is off.
 * Durable issue URLs required (skip-with-BUG pattern).
 *
 * @returns {string}
 */
function demoSitesSkipReason() {
  return (
    `BUG: Sample Site(s) not under Sites — empty/missing seed after install ` +
    `(parent #${PARENT_ISSUE}). Requires product fix #${PRODUCT_FIX_ISSUE} ` +
    `(${REPO_ISSUES}/${PRODUCT_FIX_ISSUE}) in the image under test and a ` +
    `demo-sites install (--demo-sites / wizard Yes), then set EXPECT_DEMO_SITES=1. ` +
    `Residual automation: #${RESIDUAL_ISSUE} (${REPO_ISSUES}/${RESIDUAL_ISSUE}).`
  );
}

module.exports = {
  EXPECTED_SAMPLE_SITE_NAMES,
  EXPECT_DEMO_SITES_ENV_KEYS,
  PRODUCT_FIX_ISSUE,
  PARENT_ISSUE,
  RESIDUAL_ISSUE,
  REPO_ISSUES,
  normalizeSiteName,
  pathItemRows,
  pathItemNames,
  pagedItemListChildren,
  pagedItemListCount,
  isPageTypeChild,
  hasAllExpectedSampleSites,
  hasAnyExpectedSampleSite,
  isTruthyEnvFlag,
  shouldEnforceDemoSites,
  isH2QaDemoSitesDefault,
  shouldSoftSkipSitesList,
  demoSitesSkipReason,
};
