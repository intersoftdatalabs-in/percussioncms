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
 * Architecture Create-section no-skip helpers (#3589 / parent #3092).
 *
 * H2 QA (demo-sites default / #3352 sample NavTree) must not soft-skip the
 * Create section dialog. Do not seed a second NavTree here.
 *
 * @see tests/architecture-create-section-noskip.spec.js
 * @see tests/architecture-nav-mutations-smoke.spec.js
 */

"use strict";

const {
  isH2QaDemoSitesDefault,
  shouldEnforceDemoSites,
} = require("./demo-sites");
const {
  SAMPLE_DEMO_SITE_NAMES,
  isSampleDemoSite,
  siteNamesFromPayload,
  isEmptyTreePayload,
} = require("./nav-tree-live");

const TEST_IDS = Object.freeze({
  shell: "perc-architecture-shell",
  topnav: "perc-spa-topnav",
  navArchitecture: "nav-architecture",
  sitePicker: "architecture-site-picker",
  treePanel: "architecture-tree-panel",
  navTree: "architecture-nav-tree",
  treeEmpty: "architecture-nav-tree-empty",
  treeError: "architecture-nav-tree-error",
  actionCreate: "architecture-action-create",
  createDialog: "architecture-create-dialog",
  createTitle: "architecture-create-title-input",
  createUrl: "architecture-create-url-input",
  createPageName: "architecture-create-page-name-input",
  createTemplate: "architecture-create-template-select",
  createCancel: "architecture-create-cancel",
  createTemplatesLoading: "architecture-create-templates-loading",
  createSubmit: "architecture-create-submit",
  createError: "architecture-create-error",
});

const SECTION_TITLE_PREFIX = "QA3589";

/**
 * @param {string} baseUrl
 * @param {Record<string, string>} [extra]
 * @returns {string}
 */
function architectureSpaUrl(baseUrl, extra = {}) {
  const root = String(baseUrl || "").replace(/\/+$/, "");
  const q = new URLSearchParams({
    entry: "architecture",
    _: String(Date.now()),
    ...extra,
  });
  return `${root}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * @param {string} baseUrl
 * @returns {string}
 */
function siteListUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/+$/, "");
  return `${root}/Rhythmyx/services/sitemanage/site/`;
}

/**
 * @param {string} baseUrl
 * @param {string} siteName
 * @returns {string}
 */
function sectionTreeUrl(baseUrl, siteName) {
  const root = String(baseUrl || "").replace(/\/+$/, "");
  return `${root}/Rhythmyx/services/sitemanage/section/tree/${encodeURIComponent(
    String(siteName || ""),
  )}`;
}

/**
 * POST create-section endpoint used by the Navigation SPA.
 *
 * @param {string} baseUrl
 * @returns {string}
 */
function sectionCreateUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/+$/, "");
  return `${root}/Rhythmyx/services/sitemanage/section/create`;
}

/**
 * True for {@code POST …/section/create} (not createSectionLink / createSectionFromFolder).
 *
 * @param {unknown} url
 * @param {unknown} method
 * @returns {boolean}
 */
function isCreateSiteSectionRequest(url, method) {
  if (String(method || "").toUpperCase() !== "POST") {
    return false;
  }
  const path = String(url || "");
  return /\/section\/create(\/|\?|$)/i.test(path) && !/createSection/i.test(path);
}

/**
 * H2 QA with sample sites (#3352) must hard-fail when NavTree is absent.
 *
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} [env]
 * @returns {boolean}
 */
function shouldRequireNavTree(env = process.env) {
  if (shouldEnforceDemoSites(env)) {
    return true;
  }
  return isH2QaDemoSitesDefault(env);
}

/**
 * First seeded demo site name from a site-list payload (underscore form).
 *
 * @param {readonly string[]} names
 * @returns {string | null}
 */
function firstSampleDemoSite(names) {
  const list = Array.isArray(names) ? names : [];
  for (const expected of SAMPLE_DEMO_SITE_NAMES) {
    const match = list.find(
      (n) => String(n).trim().toLowerCase() === expected.toLowerCase(),
    );
    if (match) {
      return String(match).trim();
    }
  }
  return null;
}

/**
 * Unique section title that stays within validation (letters/digits/dash).
 *
 * @param {number} [now]
 * @param {string} [prefix]
 * @returns {string}
 */
function uniqueSectionTitle(now = Date.now(), prefix = SECTION_TITLE_PREFIX) {
  const stamp = String(Number(now) || 0);
  return `${prefix}-${stamp}`;
}

/**
 * URL/folder name for {@link uniqueSectionTitle} (no spaces).
 *
 * @param {string} title
 * @returns {string}
 */
function uniqueSectionUrlName(title) {
  return String(title || "")
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9._-]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

/**
 * Landing page file name for {@link uniqueSectionTitle} (adds .html).
 *
 * @param {string} title
 * @returns {string}
 */
function uniqueLandingPageName(title) {
  const base = uniqueSectionUrlName(title);
  if (!base) {
    return "";
  }
  return /\.html$/i.test(base) ? base : `${base}.html`;
}

/**
 * @param {unknown} err
 * @returns {boolean}
 */
function isKnownArchitectureConsoleNoise(err) {
  return /favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
    String(err || ""),
  );
}

/**
 * Durable fail text when H2 / demo-sites has no NavTree.
 *
 * @returns {string}
 */
function missingNavTreeFailMessage() {
  return (
    "H2 QA must expose a NavTree on a #3352 sample site " +
    `(${SAMPLE_DEMO_SITE_NAMES.join(", ")}). Do not skip Create section; ` +
    "do not seed a second NavTree from this spec."
  );
}

module.exports = {
  TEST_IDS,
  SECTION_TITLE_PREFIX,
  architectureSpaUrl,
  siteListUrl,
  sectionTreeUrl,
  sectionCreateUrl,
  isCreateSiteSectionRequest,
  shouldRequireNavTree,
  firstSampleDemoSite,
  uniqueSectionTitle,
  uniqueSectionUrlName,
  uniqueLandingPageName,
  isKnownArchitectureConsoleNoise,
  missingNavTreeFailMessage,
  SAMPLE_DEMO_SITE_NAMES,
  isSampleDemoSite,
  siteNamesFromPayload,
  isEmptyTreePayload,
};
