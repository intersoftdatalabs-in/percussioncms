/**
 * Pure helpers for File widget red-border / recycled chrome residual
 * (issue #2239 / parent #777 slice 3).
 *
 * <p>Classification (slice 1 #2237): red dotted outline is intentional
 * {@code .perc-recycled-asset} chrome when assembly {@code isInRecycler(assetId)}
 * is true. Product fix #2238 clears non-inline AA + LocalContent widget
 * relationships on recycle so pages no longer bind recycled content ids.</p>
 *
 * <p>No machine hard-coded install paths. Base URL and credentials come from
 * auth / resolve-cms-env (TEST_CMS_URL or DEV_PERCUSSION_*).</p>
 *
 * @see docs/ai-generated/issue-2237-file-widget-red-border-evidence.md
 * @see tests/bugs/bug-2239-file-widget-recycled-chrome.spec.js
 */

"use strict";

/** Parent epic + product fix + this residual. */
const PARENT_ISSUE = 777;
const PRODUCT_FIX_ISSUE = 2238;
const RESIDUAL_ISSUE = 2239;
const REPO_ISSUES =
  "https://github.com/intersoftdatalabs-in/percussioncms/issues";

/**
 * Env keys that force hard assertions when File package / widget-test page
 * fixtures are required (cell with percFileAsset + Sites page under test).
 */
const EXPECT_FILE_WIDGET_FIXTURES_ENV_KEYS = Object.freeze([
  "EXPECT_FILE_WIDGET_FIXTURES",
  "TEST_EXPECT_FILE_WIDGET_FIXTURES",
]);

/**
 * Stable DOM selectors from assembly + decoration CSS (slice 1 evidence).
 * Prefer these over i18n text for residual assertions.
 */
const SELECTORS = Object.freeze({
  /** Widget outer wrapper after assembly. */
  widget: ".perc-widget",
  /** Intentional recycled warning chrome (red dotted outline). */
  recycledWidget: ".perc-widget.perc-recycled-asset",
  /** Class token alone (for classList checks). */
  recycledClass: "perc-recycled-asset",
  /** Tooltip applied when chrome is present (assembly.vm). */
  recycledTitle: "Asset is in Recycle Bin",
});

/** Relative CMS path candidates for the #777 widget-test File page. */
const WIDGET_TEST_FILE_PAGE_PATH_CANDIDATES = Object.freeze([
  "widget-test-page/file/index.html",
  "widget-test-page/file",
  "Sites/widget-test-page/file/index.html",
  "Sites/widget-test-page/file",
]);

/**
 * Content-type name tokens that indicate File asset package is installed.
 * Case-insensitive match against contenttypes JSON text or name list.
 */
const FILE_ASSET_TYPE_TOKENS = Object.freeze([
  "percFileAsset",
  "percFile",
  "FileAsset",
]);

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
 * When true, missing File / widget-test fixtures must fail (regression gate).
 *
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} [env]
 * @returns {boolean}
 */
function shouldEnforceFileWidgetFixtures(env = process.env) {
  for (const key of EXPECT_FILE_WIDGET_FIXTURES_ENV_KEYS) {
    if (isTruthyEnvFlag(env[key])) {
      return true;
    }
  }
  return false;
}

/**
 * Skip reason when File package / widget-test page fixtures are absent and
 * enforcement is off. Durable issue URLs (skip-with-BUG pattern).
 *
 * @param {{ reason?: string }} [opts]
 * @returns {string}
 */
function fileWidgetFixturesSkipReason(opts = {}) {
  const detail = opts.reason ? ` Detail: ${opts.reason}` : "";
  return (
    `BUG: File widget recycle residual needs CMS fixtures (parent #${PARENT_ISSUE}). ` +
    `Install File asset package (percFileAsset) and a page path equivalent to ` +
    `widget-test-page/file; deploy product fix #${PRODUCT_FIX_ISSUE} ` +
    `(${REPO_ISSUES}/${PRODUCT_FIX_ISSUE}) then set EXPECT_FILE_WIDGET_FIXTURES=1. ` +
    `Residual automation: #${RESIDUAL_ISSUE} (${REPO_ISSUES}/${RESIDUAL_ISSUE}).` +
    detail
  );
}

/**
 * True when class string includes recycled chrome class token.
 *
 * @param {string | undefined | null} className
 * @returns {boolean}
 */
function hasRecycledAssetChrome(className) {
  if (className == null || className === "") {
    return false;
  }
  const tokens = String(className)
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  return tokens.includes(SELECTORS.recycledClass);
}

/**
 * True when widget chrome is clean (post-#2238 unbound or valid live asset).
 * Valid published non-recycled widgets use class {@code perc-widget} only.
 *
 * @param {string | undefined | null} className
 * @returns {boolean}
 */
function isCleanWidgetChrome(className) {
  return !hasRecycledAssetChrome(className);
}

/**
 * Classify assembly widget chrome from outer div attributes.
 *
 * @param {{ className?: string, title?: string, assetId?: string }} attrs
 * @returns {{ recycled: boolean, clean: boolean, assetId: string, title: string }}
 */
function classifyWidgetChrome(attrs) {
  const a = attrs || {};
  const className = a.className != null ? String(a.className) : "";
  const title = a.title != null ? String(a.title) : "";
  const assetId = a.assetId != null ? String(a.assetId) : "";
  const recycled = hasRecycledAssetChrome(className);
  return {
    recycled,
    clean: !recycled,
    assetId,
    title,
  };
}

/**
 * True when decoration CSS body still defines intentional recycled chrome
 * (product fix must not only delete the CSS class).
 *
 * @param {string | undefined | null} cssText
 * @returns {boolean}
 */
function decorationCssDefinesRecycledChrome(cssText) {
  const css = String(cssText || "");
  if (!/\.perc-recycled-asset\b/.test(css)) {
    return false;
  }
  // Loose match: outline style/color near the rule (comments allowed).
  const blockMatch = css.match(
    /\.perc-recycled-asset\s*\{[^}]*\}/i,
  );
  if (!blockMatch) {
    // Rule present but not a simple block — still count as defined.
    return true;
  }
  const block = blockMatch[0];
  return /outline/i.test(block);
}

/**
 * Detect File asset content types from contenttypes service body (JSON or text).
 *
 * @param {unknown} body
 * @returns {{ hasFileAssetType: boolean, matchedTokens: string[] }}
 */
function detectFileAssetTypes(body) {
  let text = "";
  if (body == null) {
    text = "";
  } else if (typeof body === "string") {
    text = body;
  } else {
    try {
      text = JSON.stringify(body);
    } catch {
      text = String(body);
    }
  }
  const matched = [];
  for (const token of FILE_ASSET_TYPE_TOKENS) {
    if (text.toLowerCase().includes(String(token).toLowerCase())) {
      matched.push(token);
    }
  }
  return {
    hasFileAssetType: matched.length > 0,
    matchedTokens: matched,
  };
}

/**
 * Extract site summary names from sitemanage site list JSON.
 *
 * @param {unknown} body
 * @returns {string[]}
 */
function siteSummaryNames(body) {
  if (body == null) {
    return [];
  }
  if (Array.isArray(body)) {
    return body
      .map((s) => (s && typeof s === "object" ? s.name || s.siteName : null))
      .filter((n) => typeof n === "string" && n.trim().length > 0)
      .map((n) => String(n).trim());
  }
  if (typeof body !== "object") {
    return [];
  }
  const o = /** @type {Record<string, unknown>} */ (body);
  const list =
    (Array.isArray(o.SiteSummary) && o.SiteSummary) ||
    (Array.isArray(o.siteSummary) && o.siteSummary) ||
    (Array.isArray(o.Summary) && o.Summary) ||
    [];
  return list
    .map((s) =>
      s && typeof s === "object"
        ? /** @type {Record<string, unknown>} */ (s).name ||
          /** @type {Record<string, unknown>} */ (s).siteName
        : null,
    )
    .filter((n) => typeof n === "string" && n.trim().length > 0)
    .map((n) => String(n).trim());
}

/**
 * True when a path/folder listing (or path string list) looks like the
 * widget-test File page tree (name contains widget-test or file page).
 *
 * @param {string[]} names
 * @returns {boolean}
 */
function pathNamesSuggestWidgetTestFile(names) {
  const joined = (names || []).map((n) => String(n).toLowerCase());
  const hasWidgetTest = joined.some((n) => n.includes("widget-test"));
  const hasFile = joined.some(
    (n) => n === "file" || n.includes("file/") || n.endsWith("/file"),
  );
  return hasWidgetTest || hasFile;
}

/**
 * Join CMS base URL with a path (no double slash).
 *
 * @param {string} baseUrl
 * @param {string} path
 * @returns {string}
 */
function cmsUrl(baseUrl, path) {
  const base = String(baseUrl || "").replace(/\/+$/, "");
  const p = path.startsWith("/") ? path : `/${path}`;
  return `${base}${p}`;
}

/**
 * Build editor / classic CMS URL candidates for the widget-test File page.
 * Callers try in order; first navigable wins.
 *
 * @param {string} baseUrl
 * @param {string} [relativePagePath] e.g. widget-test-page/file/index.html
 * @returns {string[]}
 */
function widgetTestFilePageUrls(baseUrl, relativePagePath) {
  const rel = String(
    relativePagePath || WIDGET_TEST_FILE_PAGE_PATH_CANDIDATES[0],
  ).replace(/^\/+/, "");
  const base = String(baseUrl || "").replace(/\/+$/, "");
  // Editor SPA entry and classic finder paths used in QA docs / #777 repro.
  return [
    `${base}/Rhythmyx/cm/app/spa.jsp?entry=editor&path=//Sites/${rel}`,
    `${base}/Rhythmyx/cm/app/index.jsp?view=editor&path=//Sites/${rel}`,
    `${base}/Rhythmyx/assembler/render?sys_path=//Sites/${rel}`,
  ];
}

/**
 * Gate: skip with BUG when fixtures missing and not enforcing; otherwise
 * return false so caller hard-asserts.
 *
 * @param {{ hasFileAssetType: boolean, hasSites: boolean, hasWidgetTestPath?: boolean }} probe
 * @param {{ enforce?: boolean }} [opts]
 * @returns {{ skip: boolean, reason: string }}
 */
function gateFileWidgetFixtures(probe, opts = {}) {
  const enforce =
    opts.enforce !== undefined
      ? Boolean(opts.enforce)
      : shouldEnforceFileWidgetFixtures();
  const p = probe || {};
  const ready =
    Boolean(p.hasFileAssetType) &&
    Boolean(p.hasSites) &&
    (p.hasWidgetTestPath === undefined || Boolean(p.hasWidgetTestPath));
  if (ready) {
    return { skip: false, reason: "" };
  }
  const bits = [];
  if (!p.hasFileAssetType) {
    bits.push("no percFileAsset content type");
  }
  if (!p.hasSites) {
    bits.push("no Sites / empty site list");
  }
  if (p.hasWidgetTestPath === false) {
    bits.push("no widget-test-page/file path");
  }
  const reason = fileWidgetFixturesSkipReason({
    reason: bits.join("; ") || "fixtures incomplete",
  });
  if (!enforce) {
    return { skip: true, reason };
  }
  return { skip: false, reason };
}

module.exports = {
  PARENT_ISSUE,
  PRODUCT_FIX_ISSUE,
  RESIDUAL_ISSUE,
  REPO_ISSUES,
  EXPECT_FILE_WIDGET_FIXTURES_ENV_KEYS,
  SELECTORS,
  WIDGET_TEST_FILE_PAGE_PATH_CANDIDATES,
  FILE_ASSET_TYPE_TOKENS,
  isTruthyEnvFlag,
  shouldEnforceFileWidgetFixtures,
  fileWidgetFixturesSkipReason,
  hasRecycledAssetChrome,
  isCleanWidgetChrome,
  classifyWidgetChrome,
  decorationCssDefinesRecycledChrome,
  detectFileAssetTypes,
  siteSummaryNames,
  pathNamesSuggestWidgetTestFile,
  cmsUrl,
  widgetTestFilePageUrls,
  gateFileWidgetFixtures,
};
