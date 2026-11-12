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
 * Closed inventory + results-matrix row helpers for the US1 content-creation
 * matrix (#6239 / parent #6238).
 *
 * <p>The pinned {@link BUILT_IN_TYPES} list is the source of truth for
 * "every built-in content type". It was captured from a live CMS-only cell
 * ({@code GET /Rhythmyx/services/contenttypes} plus per-type
 * {@code GET /services/contenttypes/{name}} field details). Types the New-item
 * flow cannot create are not pinned; they live in {@link SYSTEM_EXCLUSIONS}
 * with a reason each.</p>
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators). File paths
 * below are joined with {@code node:path} so the suite stays portable.</p>
 */

"use strict";

const path = require("node:path");

const MATRIX_SUITE = "content-create-matrix";
const MATRIX_SPEC = "tests/content-create-matrix.spec.js";
const INVENTORY_SPEC = "tests/content-create-inventory.spec.js";
const SCHEMA_FILE = "results-matrix.schema.json";
const EXAMPLE_FILE = "results-matrix.example.json";

const CASE_CREATE = "createRoundTrip";
const CASE_NEGATIVE = "negativeRequired";
const CASE_NAMES = Object.freeze([CASE_CREATE, CASE_NEGATIVE]);

const RESULT_STATUSES = Object.freeze(["pass", "fail", "skip"]);
const MATRIX_CELLS = Object.freeze(["cms-only"]);

const CONTENT_TYPE_PATTERN = /^(perc|rff)[A-Za-z0-9]+$/;
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/** Folder used for binary/asset items in the create panel. */
const ASSET_FOLDER = "/Assets";
/** Folder used for page/site items in the create panel. */
const SITE_FOLDER = "//Sites/EnterpriseInvestments";

/**
 * Content types present in the CMS catalog that the editor New-item flow
 * cannot create. Kept explicit so the inventory spec can prove every catalog
 * row is either pinned or excluded on purpose.
 */
const SYSTEM_EXCLUSIONS = Object.freeze([
  Object.freeze({
    name: "Folder",
    reason: "system folder type, created by folder flows not the New-item content flow",
  }),
  Object.freeze({
    name: "PSWidgetProperties",
    reason: "internal widget property store, never user-created",
  }),
  Object.freeze({
    name: "percPageTemplate",
    reason: "created through the template flow; every field is a hidden input",
  }),
]);

/**
 * One entry per built-in creatable content type.
 *
 * <p>{@code roundTripField} is a visible required single-line text field used
 * for the save/reopen round-trip; {@code negativeField} is the required text
 * field blanked for the negative case (visible message, nothing persisted).
 * Both are {@code sys_EditBox} controls on the live cell, so the generic
 * React editor renders them as fillable {@code editor-field-<name>} inputs.</p>
 */
const BUILT_IN_TYPES = Object.freeze([
  Object.freeze({ name: "percArchiveList", label: "Archives", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percBlogIndexAsset", label: "percBlogIndexAsset", folder: SITE_FOLDER, roundTripField: "feed_title", negativeField: "feed_title" }),
  Object.freeze({ name: "percBlogPostAsset", label: "Blog Post", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "percCalendarAsset", label: "Calendar", folder: SITE_FOLDER, roundTripField: "calendar_title", negativeField: "calendar_title" }),
  Object.freeze({ name: "percCategoryList", label: "Categories", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percCommentsFormAsset", label: "Comments Form", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percCookieConsent", label: "percCookieConsent", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percDefaultLanguage", label: "percDefaultLanguage", folder: SITE_FOLDER, roundTripField: "configurationName", negativeField: "configurationName" }),
  Object.freeze({ name: "percDepartment", label: "percDepartment", folder: SITE_FOLDER, roundTripField: "dptName", negativeField: "dptName" }),
  Object.freeze({ name: "percDirectory", label: "percDirectory", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percEventAsset", label: "Event", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percFileAsset", label: "File", folder: ASSET_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle", note: "binary attachment required on the cell; negative blanks the required title" }),
  Object.freeze({ name: "percFileAutoList", label: "percFileAutoList", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percFormAsset", label: "Form", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percImageAsset", label: "Image Asset", folder: ASSET_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle", note: "binary image required on the cell; negative blanks the required title" }),
  Object.freeze({ name: "percImageAutoList", label: "Image Auto List", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percImageSlider", label: "percImageSlider", folder: SITE_FOLDER, roundTripField: "sliderName", negativeField: "sliderName" }),
  Object.freeze({ name: "percLocalLanguage", label: "percLocalLanguage", folder: SITE_FOLDER, roundTripField: "configurationName", negativeField: "configurationName" }),
  Object.freeze({ name: "percLoginAsset", label: "Login", folder: SITE_FOLDER, roundTripField: "email_label", negativeField: "email_label" }),
  Object.freeze({ name: "percNavImage", label: "Nav Image", folder: ASSET_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle", note: "binary nav image required on the cell; negative blanks the required title" }),
  Object.freeze({ name: "percNavon", label: "Navon", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "percNavTree", label: "NavTree", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "percOpenGraph", label: "Open Graph", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percOrganization", label: "percOrganization", folder: SITE_FOLDER, roundTripField: "orgName", negativeField: "orgName" }),
  Object.freeze({ name: "percPage", label: "page", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title", note: "page_title lives in the collapsed SEO metadata group; sys_title is always rendered" }),
  Object.freeze({ name: "percPageAutoList", label: "Page Auto List", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percPerson", label: "percPerson", folder: SITE_FOLDER, roundTripField: "personFirstName", negativeField: "personFirstName" }),
  Object.freeze({ name: "percPollAsset", label: "Polls", folder: SITE_FOLDER, roundTripField: "pollUniqueName", negativeField: "pollUniqueName" }),
  Object.freeze({ name: "percRawHtmlAsset", label: "HTML", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percRegistrationAsset", label: "Registration Asset (Deprecated)", folder: SITE_FOLDER, roundTripField: "email_label", negativeField: "email_label", note: "deprecated but still shipped and creatable" }),
  Object.freeze({ name: "percRichTextAsset", label: "Rich Text", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percRssAsset", label: "RSS", folder: SITE_FOLDER, roundTripField: "rss_url", negativeField: "rss_url" }),
  Object.freeze({ name: "percSecureLogin", label: "Secure Login (Deprecated)", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title", note: "deprecated but still shipped and creatable" }),
  Object.freeze({ name: "percSimpleTextAsset", label: "Simple Text", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percSocialButtons", label: "percSocialButtons", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percTagList", label: "Tags", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percTitleAsset", label: "percTitleAsset", folder: SITE_FOLDER, roundTripField: "text", negativeField: "text" }),
  Object.freeze({ name: "percTwitterCards", label: "Twitter Cards", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "percUserProfile", label: "percUserProfile", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "rffAutoIndex", label: "Auto Index", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "rffBrief", label: "Brief", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "rffCalendar", label: "Calendar", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "rffContacts", label: "Contacts", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "rffEvent", label: "Event", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "rffExternalLink", label: "External Link", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "rffFile", label: "File", folder: ASSET_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "rffGeneric", label: "Generic", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "rffGenericWord", label: "Generic Word", folder: SITE_FOLDER, roundTripField: "sys_title", negativeField: "sys_title" }),
  Object.freeze({ name: "rffHome", label: "Home", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "rffImage", label: "Image", folder: ASSET_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle", note: "binary image required on the cell; negative blanks the required title" }),
  Object.freeze({ name: "rffNavImage", label: "Nav Image", folder: ASSET_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle", note: "binary nav image required on the cell; negative blanks the required title" }),
  Object.freeze({ name: "rffNavon", label: "Navon", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "rffNavTree", label: "NavTree", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
  Object.freeze({ name: "rffPressRelease", label: "Press Release", folder: SITE_FOLDER, roundTripField: "displaytitle", negativeField: "displaytitle" }),
]);

/**
 * @param {string} name content-type name
 * @returns {string} sample round-trip value for the entry's text field
 */
function sampleValueFor(entry) {
  return `Matrix ${entry.name} ${entry.roundTripField}`;
}

/**
 * @param {string} name content-type name
 * @returns {string} Playwright tag for the type's cases
 */
function tagForType(name) {
  return `@content-create-${name.replace(/([a-z0-9])([A-Z])/g, "$1-$2").toLowerCase()}`;
}

/**
 * Derives the matrix status from per-case results: {@code fail} wins over
 * {@code skip} wins over {@code pass}.
 *
 * @param {{ createRoundTrip: string, negativeRequired: string }} caseResults
 * @returns {string}
 */
function statusForCases(caseResults) {
  const values = CASE_NAMES.map((k) => caseResults[k]);
  if (values.includes("fail")) {
    return "fail";
  }
  if (values.includes("skip")) {
    return "skip";
  }
  return "pass";
}

/**
 * Builds one results-matrix row for a pinned entry. The row shape mirrors
 * {@code contracts/results-matrix.schema.json}; see {@link validateResultRow}.
 *
 * @param {{ name: string, label: string }} entry pinned inventory entry
 * @param {{ createRoundTrip: string, negativeRequired: string }} caseResults
 * @param {{ cell?: string, spec?: string, updated?: string }} [options]
 * @returns {object}
 */
function buildResultRow(entry, caseResults, options = {}) {
  return {
    suite: MATRIX_SUITE,
    spec: options.spec || MATRIX_SPEC,
    contentType: entry.name,
    label: entry.label,
    cell: options.cell || MATRIX_CELLS[0],
    cases: {
      [CASE_CREATE]: caseResults[CASE_CREATE],
      [CASE_NEGATIVE]: caseResults[CASE_NEGATIVE],
    },
    status: statusForCases(caseResults),
    updated:
      options.updated || new Date().toISOString().slice(0, 10),
  };
}

/**
 * Validates one results-matrix row against the contract. Dependency-free so
 * both Playwright specs and plain {@code node --test} unit tests can use it.
 *
 * @param {unknown} row
 * @returns {string[]} validation errors, empty when the row is valid
 */
function validateResultRow(row) {
  const errors = [];
  if (row == null || typeof row !== "object" || Array.isArray(row)) {
    return ["row must be an object"];
  }
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
  if (typeof row.contentType !== "string" || !CONTENT_TYPE_PATTERN.test(row.contentType)) {
    errors.push("contentType must match ^(perc|rff)[A-Za-z0-9]+$");
  }
  if (typeof row.label !== "string" || row.label.length === 0) {
    errors.push("label must be a non-empty string");
  }
  if (!MATRIX_CELLS.includes(row.cell)) {
    errors.push(`cell must be one of ${MATRIX_CELLS.join(", ")}`);
  }
  if (row.cases == null || typeof row.cases !== "object" || Array.isArray(row.cases)) {
    errors.push("cases must be an object");
  } else {
    for (const key of Object.keys(row.cases)) {
      if (!CASE_NAMES.includes(key)) {
        errors.push(`unexpected case '${key}'`);
      }
    }
    for (const key of CASE_NAMES) {
      if (!RESULT_STATUSES.includes(row.cases[key])) {
        errors.push(`cases.${key} must be one of ${RESULT_STATUSES.join(", ")}`);
      }
    }
  }
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
  INVENTORY_SPEC,
  SCHEMA_FILE,
  EXAMPLE_FILE,
  CASE_CREATE,
  CASE_NEGATIVE,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  ASSET_FOLDER,
  SITE_FOLDER,
  SYSTEM_EXCLUSIONS,
  BUILT_IN_TYPES,
  sampleValueFor,
  tagForType,
  statusForCases,
  buildResultRow,
  validateResultRow,
  schemaPathFor,
};
