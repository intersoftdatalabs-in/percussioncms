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
 * Closed inventory + results-matrix row helpers for the US2 published-widget
 * static-render matrix (#6240 / parent #6238).
 *
 * <p>The pinned {@link WIDGETS} list is the source of truth for "every widget
 * type". It was captured from the shipped widget snippet templates
 * ({@code modules/perc-packages/.../widgets/<Name>/templates/<Name>Snippet.vm}):
 * 48 template widgets minus the three {@link WIDGET_EXCLUSIONS} (test-only
 * sample, jQuery script includes with no published markup). Each entry pins
 * the stable published-output hook the matrix spec asserts on that widget's
 * fixture page: a body CSS marker, a {@code <head>} marker for head-only
 * widgets, or {@code null} for passthrough widgets (raw HTML / rich text /
 * simple text / title) whose output is the authored content itself.</p>
 *
 * <p>Widgets backed by a DTS service ({@code kind: "dynamic"}) still get a
 * static shell case here; their service interaction matrix is US3 (#6241).
 * The DTS service name is pinned so the US3 slice can cross-check that every
 * dynamic widget has an interaction case.</p>
 *
 * <p>URL paths below use {@code /} (web origins, not OS file paths). File
 * paths are joined with {@code node:path} so the suite stays portable.</p>
 */

"use strict";

const path = require("node:path");

const MATRIX_SUITE = "published-widget-matrix";
const MATRIX_SPEC = "tests/published-widget-matrix.spec.js";
const SCHEMA_FILE = "results-matrix.schema.json";
const EXAMPLE_FILE = "results-matrix-widget.example.json";

const CASE_STATIC = "staticRender";
const CASE_NAMES = Object.freeze([CASE_STATIC]);

const RESULT_STATUSES = Object.freeze(["pass", "fail", "skip"]);
const MATRIX_CELLS = Object.freeze(["published-site"]);

const WIDGET_NAME_PATTERN = /^(perc|simple)[A-Za-z0-9]+$/;
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/** Skip reason when no published site is served (default H2 QA cell). */
const SKIP_NO_SITE =
  "needs PUBLISHED_SITE_URL=(in-cell published site origin); " +
  "default H2 QA cell has no DTS publish path (#6237)";

/**
 * Skip reason when the cell serves a site but the widget's fixture page is
 * not published yet. Each skip names the exact gap so the fixture slice can
 * close them one by one without touching the spec.
 *
 * @param {{ name: string }} entry widget inventory entry
 * @param {string} pageUrl fixture page URL that was probed
 * @returns {string}
 */
function skipNoFixture(entry, pageUrl) {
  return (
    `fixture page ${pageUrl} for ${entry.name} is not published yet; ` +
    "publish the US2 fixture site (parent #6238) to turn this case green"
  );
}

/**
 * Widget templates that ship in the repo but are never published output, so
 * the matrix neither pins nor asserts them. Kept explicit so the closure
 * test can prove every template directory is either pinned or excluded on
 * purpose.
 */
const WIDGET_EXCLUSIONS = Object.freeze([
  Object.freeze({
    name: "PSWidget_TestProperties",
    reason: "test-only sample widget in the perc.Test package, never published",
  }),
  Object.freeze({
    name: "percJQueryWidget",
    reason: "infrastructure script include, renders no published markup",
  }),
  Object.freeze({
    name: "percJQueryUIWidget",
    reason: "infrastructure script include, renders no published markup",
  }),
]);

/**
 * One entry per published widget type.
 *
 * <p>{@code kind} is {@code static} (visible body output), {@code dynamic}
 * (DTS-backed; static shell asserted here, interaction deferred to US3) or
 * {@code head} (published output lives in {@code <head>}: meta/link tags).
 * {@code marker} is the CSS selector asserted on the widget's fixture page;
 * {@code null} means the widget passes content through, so the spec asserts
 * non-empty visible text instead. {@code dts} names the backing DTS service
 * for dynamic widgets and is {@code null} otherwise.</p>
 */
const WIDGETS = Object.freeze([
  Object.freeze({ name: "percArchiveList", label: "Archive List", kind: "static", marker: ".perc-archive-list", dts: null }),
  Object.freeze({ name: "percBlogIndexPage", label: "Blog Index", kind: "static", marker: ".perc-blog-list", dts: null }),
  Object.freeze({ name: "percBlogPost", label: "Blog Post", kind: "static", marker: ".perc-blog", dts: null }),
  Object.freeze({ name: "percCalendar", label: "Calendar", kind: "static", marker: ".perc-calendar", dts: null }),
  Object.freeze({ name: "percCalendarTwo", label: "Calendar Two", kind: "static", marker: ".perc-calendar", dts: null }),
  Object.freeze({ name: "percCategoryList", label: "Category List", kind: "static", marker: ".perc-category-list", dts: null }),
  Object.freeze({ name: "percComments", label: "Comments View", kind: "dynamic", marker: ".perc-comments-view", dts: "comments" }),
  Object.freeze({ name: "percCommentsForm", label: "Comments Form", kind: "dynamic", marker: ".perc-comments-form", dts: "comments" }),
  Object.freeze({ name: "percCookieConsent", label: "Cookie Consent", kind: "static", marker: ".perc-consent-window", dts: null }),
  Object.freeze({ name: "percDefaultLang", label: "Default Language", kind: "head", marker: 'link[rel="alternate"][hreflang]', dts: null }),
  Object.freeze({ name: "percDepartment", label: "Department", kind: "static", marker: ".perc-dpt-name", dts: null }),
  Object.freeze({ name: "percDirectory", label: "Directory", kind: "static", marker: ".perc-directory-name", dts: null }),
  Object.freeze({ name: "percEvent", label: "Event", kind: "static", marker: ".perc-event", dts: null }),
  Object.freeze({ name: "percFile", label: "File", kind: "static", marker: ".perc-file", dts: null }),
  Object.freeze({ name: "percFileAutoList", label: "File Auto List", kind: "static", marker: ".perc-file-auto-list", dts: null }),
  Object.freeze({ name: "percForm", label: "Form", kind: "dynamic", marker: ".perc-form", dts: "forms" }),
  Object.freeze({ name: "percIframe", label: "IFrame", kind: "static", marker: ".perc-iframe", dts: null }),
  Object.freeze({ name: "percImage", label: "Image", kind: "static", marker: "img", dts: null }),
  Object.freeze({ name: "percImageAutoList", label: "Image Auto List", kind: "static", marker: ".perc-image-auto-list", dts: null }),
  Object.freeze({ name: "percImageSlider", label: "Image Slider", kind: "static", marker: ".perc-image-slider", dts: null }),
  Object.freeze({ name: "percLiked", label: "Liked", kind: "static", marker: ".perc-like-widget", dts: null }),
  Object.freeze({ name: "percLocalLang", label: "Local Language", kind: "head", marker: 'link[rel="alternate"][hreflang]', dts: null }),
  Object.freeze({ name: "percLogin", label: "Login", kind: "dynamic", marker: "form#perc-login-form", dts: "membership" }),
  Object.freeze({ name: "percMostReadBlogPosts", label: "Most Read Blog Posts", kind: "static", marker: ".perc-most-read-blog-posts-view", dts: null }),
  Object.freeze({ name: "percNavBar", label: "Nav Bar", kind: "static", marker: ".perc-navbar", dts: null }),
  Object.freeze({ name: "percNavBreadcrumb", label: "Nav Breadcrumb", kind: "static", marker: ".perc-breadcrumb-main", dts: null }),
  Object.freeze({ name: "percOpenGraph", label: "Open Graph", kind: "head", marker: 'meta[property^="og:"]', dts: null }),
  Object.freeze({ name: "percOrganization", label: "Organization", kind: "static", marker: ".perc-org-name", dts: null }),
  Object.freeze({ name: "percPageAutoList", label: "Page Auto List", kind: "static", marker: ".perc-page-auto-list", dts: null }),
  Object.freeze({ name: "percPerson", label: "Person", kind: "static", marker: ".perc-person", dts: null }),
  Object.freeze({ name: "percPoll", label: "Poll", kind: "dynamic", marker: ".perc-poll-render-container", dts: "polls" }),
  Object.freeze({ name: "percRawHtml", label: "Raw HTML", kind: "static", marker: null, dts: null }),
  Object.freeze({ name: "percRedirect", label: "Redirect", kind: "head", marker: 'meta[http-equiv="refresh"]', dts: null }),
  Object.freeze({ name: "percRegistration", label: "Registration", kind: "dynamic", marker: ".perc-registration-mode", dts: "membership" }),
  Object.freeze({ name: "percResult", label: "Result View", kind: "static", marker: ".perc-result-view", dts: null }),
  Object.freeze({ name: "percRichText", label: "Rich Text", kind: "static", marker: null, dts: null }),
  Object.freeze({ name: "percRss", label: "RSS Feed", kind: "dynamic", marker: ".perc-feed-widget", dts: "feeds" }),
  Object.freeze({ name: "percSecureLogin", label: "Secure Login", kind: "dynamic", marker: "form#login-form", dts: "membership" }),
  Object.freeze({ name: "percSimpleText", label: "Simple Text", kind: "static", marker: null, dts: null }),
  Object.freeze({ name: "percSocialButtons", label: "Social Buttons", kind: "static", marker: ".perc-social-button-container", dts: null }),
  Object.freeze({ name: "percTagList", label: "Tag List", kind: "static", marker: ".perc-tag-list", dts: null }),
  Object.freeze({ name: "percTitle", label: "Title", kind: "static", marker: null, dts: null }),
  Object.freeze({ name: "percTwitterSummaryCards", label: "Twitter Summary Cards", kind: "head", marker: 'meta[name^="twitter:"]', dts: null }),
  Object.freeze({ name: "simplePageAutoList", label: "Simple Page Auto List", kind: "static", marker: ".ui-perc-list-widget", dts: null }),
  Object.freeze({ name: "simpleTextAutoList", label: "Simple Text Auto List", kind: "static", marker: ".ui-perc-list-widget", dts: null }),
]);

/**
 * DTS services with a dynamic widget on the published site. US3 (#6241)
 * owns one interaction case per service; kept here so the interaction
 * matrix can prove every dynamic widget maps to a covered service.
 */
const DYNAMIC_SERVICES = Object.freeze(["polls", "feeds", "forms", "comments", "membership"]);

/**
 * @param {string} name widget name
 * @returns {string} Playwright tag for the widget's case
 */
function tagForWidget(name) {
  return `@widget-${name.replace(/([a-z0-9])([A-Z])/g, "$1-$2").toLowerCase()}`;
}

/**
 * @param {URL} siteOrigin published site origin from {@link publishedSiteUrl}
 * @param {{ name: string }} entry widget inventory entry
 * @returns {string} fixture page URL for the widget's static case
 */
function fixtureUrlFor(siteOrigin, entry) {
  return `${siteOrigin.origin}/widgets/${entry.name}.html`;
}

/**
 * @returns {URL|null} published site origin, or null when the cell serves
 * no published site (default H2 QA cell: every case skips named)
 */
function publishedSiteUrl() {
  const siteUrl = process.env.PUBLISHED_SITE_URL || "";
  if (!siteUrl) {
    return null;
  }
  return new URL(siteUrl);
}

/**
 * Builds one results-matrix row for a pinned widget. The row shape mirrors
 * {@code contracts/results-matrix.schema.json}; see {@link validateWidgetRow}.
 *
 * @param {{ name: string, label: string }} entry widget inventory entry
 * @param {{ staticRender: string }} caseResults
 * @param {{ spec?: string, updated?: string }} [options]
 * @returns {object}
 */
function buildWidgetRow(entry, caseResults, options = {}) {
  return {
    suite: MATRIX_SUITE,
    spec: options.spec || MATRIX_SPEC,
    contentType: entry.name,
    label: entry.label,
    cell: MATRIX_CELLS[0],
    cases: {
      [CASE_STATIC]: caseResults[CASE_STATIC],
    },
    status: caseResults[CASE_STATIC],
    updated: options.updated || new Date().toISOString().slice(0, 10),
  };
}

/**
 * Validates the scalar header of one published-widget results-matrix row.
 *
 * @param {object} row
 * @returns {string[]} validation errors
 */
function validateWidgetRowHeader(row) {
  const errors = [];
  const allowed = ["suite", "spec", "contentType", "label", "cell", "cases", "status", "updated"];
  for (const key of Object.keys(row)) {
    if (!allowed.includes(key)) {
      errors.push(`unexpected property '${key}'`);
    }
  }
  if (row.suite !== MATRIX_SUITE) {
    errors.push(`suite must be '${MATRIX_SUITE}'`);
  }
  if (typeof row.spec !== "string" || row.spec.length === 0) {
    errors.push("spec must be a non-empty string");
  }
  if (typeof row.contentType !== "string" || !WIDGET_NAME_PATTERN.test(row.contentType)) {
    errors.push("contentType must match ^(perc|simple)[A-Za-z0-9]+$");
  }
  if (typeof row.label !== "string" || row.label.length === 0) {
    errors.push("label must be a non-empty string");
  }
  if (!MATRIX_CELLS.includes(row.cell)) {
    errors.push(`cell must be one of ${MATRIX_CELLS.join(", ")}`);
  }
  return errors;
}

/**
 * Validates the per-case block of one published-widget results-matrix row.
 *
 * @param {unknown} cases
 * @returns {string[]} validation errors
 */
function validateWidgetRowCases(cases) {
  if (cases == null || typeof cases !== "object" || Array.isArray(cases)) {
    return ["cases must be an object"];
  }
  const errors = [];
  for (const key of Object.keys(cases)) {
    if (!CASE_NAMES.includes(key)) {
      errors.push(`unexpected case '${key}'`);
    }
  }
  for (const key of CASE_NAMES) {
    if (!RESULT_STATUSES.includes(cases[key])) {
      errors.push(`cases.${key} must be one of ${RESULT_STATUSES.join(", ")}`);
    }
  }
  return errors;
}

/**
 * Validates one published-widget results-matrix row against the contract.
 * Dependency-free so both Playwright specs and plain {@code node --test}
 * unit tests can use it.
 *
 * @param {unknown} row
 * @returns {string[]} validation errors, empty when the row is valid
 */
function validateWidgetRow(row) {
  if (row == null || typeof row !== "object" || Array.isArray(row)) {
    return ["row must be an object"];
  }
  const errors = [...validateWidgetRowHeader(row), ...validateWidgetRowCases(row.cases)];
  if (!RESULT_STATUSES.includes(row.status)) {
    errors.push(`status must be one of ${RESULT_STATUSES.join(", ")}`);
  }
  if (typeof row.updated !== "string" || !DATE_PATTERN.test(row.updated)) {
    errors.push("updated must be a YYYY-MM-DD date");
  }
  return errors;
}

/**
 * @param {string} moduleDir module root (has {@code contracts/} and {@code frontend/})
 * @returns {string} absolute path of the results-matrix JSON schema
 */
function schemaPathFor(moduleDir) {
  return path.join(String(moduleDir), "contracts", SCHEMA_FILE);
}

module.exports = {
  MATRIX_SUITE,
  MATRIX_SPEC,
  SCHEMA_FILE,
  EXAMPLE_FILE,
  CASE_STATIC,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  WIDGET_NAME_PATTERN,
  SKIP_NO_SITE,
  skipNoFixture,
  WIDGET_EXCLUSIONS,
  WIDGETS,
  DYNAMIC_SERVICES,
  tagForWidget,
  fixtureUrlFor,
  publishedSiteUrl,
  buildWidgetRow,
  validateWidgetRow,
  schemaPathFor,
};
