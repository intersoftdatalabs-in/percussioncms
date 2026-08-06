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
function pathItemNames(body) {
  if (body == null) {
    return [];
  }
  const items = Array.isArray(body.PathItem)
    ? body.PathItem
    : Array.isArray(body)
      ? body
      : [];
  return items
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
  pathItemNames,
  hasAllExpectedSampleSites,
  hasAnyExpectedSampleSite,
  isTruthyEnvFlag,
  shouldEnforceDemoSites,
  demoSitesSkipReason,
};
