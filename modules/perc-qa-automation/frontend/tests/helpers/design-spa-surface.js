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
 * Design SPA surface helpers (#3307 / parent #2631).
 *
 * URL builders, stable test ids, and skip reasons for library + create/edit
 * consolidation. Sibling slices #3305 (create) and #3306 (classic list
 * redirect) may not be on the QA cell — callers must skip cleanly instead of
 * running the full Playwright suite.
 */

"use strict";

const { softVisible } = require("./explorer-shell-chrome");

/** Stable data-testid values on the Design SPA (WebUI design/*). */
const TEST_IDS = Object.freeze({
  /** Product chrome after #3514 (Design is no longer a top-nav item). */
  nav: "perc-spa-topnav",
  shell: "perc-design-shell",
  tabTemplates: "tab-design-templates",
  panel: "design-tpl-panel",
  empty: "design-tpl-empty",
  error: "design-tpl-error",
  table: "design-tpl-table",
  create: "design-tpl-create",
  createDialog: "design-tpl-create-dialog",
  createName: "design-tpl-create-name",
  createSubmit: "design-tpl-create-submit",
  deleteRow: "design-tpl-delete-0",
  deleteDialog: "design-tpl-delete-dialog",
  deleteConfirm: "design-tpl-delete-confirm",
  deleteSubmit: "design-tpl-delete-submit",
  deleteCancel: "design-tpl-delete-cancel",
  editor: "design-tpl-editor",
  editorDelete: "design-tpl-editor-delete",
  editorBack: "design-tpl-editor-back",
  editorSource: "design-tpl-editor-source-edit",
  editorName: "design-tpl-editor-name",
  editorSave: "design-tpl-editor-save",
  editorNotice: "design-tpl-editor-notice",
  editorError: "design-tpl-editor-error",
});

/** Classic CM1 Design list marker (element id, not a data-testid). */
const CLASSIC_ASSIGNED_TEMPLATES_ID = "perc-assigned-templates";

const SKIP = Object.freeze({
  SHELL:
    "Design SPA chrome not on this QA cell (perc-design-shell missing) — skip; do not run full suite. Parent #2631 / #3307.",
  CREATE:
    "Create-template chrome not on this QA cell (design-tpl-create missing; sibling #3305). Clean skip.",
  DELETE:
    "Delete-template chrome not on this QA cell (design-tpl-delete-0 missing; sibling #3580). Clean skip.",
  EDIT_EMPTY: "No templates in catalog — cannot exercise Design SPA editor.",
  REDIRECT:
    "Classic Design list still hosted (no perc-design-shell on admin.jsp / ?view=design; sibling #3306). Clean skip.",
});

/**
 * @param {string} baseUrl
 * @returns {string}
 */
function stripTrailingSlash(baseUrl) {
  return String(baseUrl || "").replace(/\/+$/, "");
}

/**
 * Design template library SPA entry.
 * @param {string} baseUrl
 * @returns {string}
 */
function designTemplatesUrl(baseUrl) {
  const root = stripTrailingSlash(baseUrl);
  const q = new URLSearchParams({
    entry: "design",
    section: "templates",
    _: String(Date.now()),
  });
  return `${root}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * Classic Design list host that #3306 redirects into the SPA.
 * @param {string} baseUrl
 * @returns {string}
 */
function designLegacyAdminUrl(baseUrl) {
  const root = stripTrailingSlash(baseUrl);
  return `${root}/Rhythmyx/cm/app/admin.jsp?_=${Date.now()}`;
}

/**
 * Legacy dispatcher bookmark for Design.
 * @param {string} baseUrl
 * @returns {string}
 */
function designLegacyViewUrl(baseUrl) {
  const root = stripTrailingSlash(baseUrl);
  return `${root}/Rhythmyx/cm/app/?view=design&_=${Date.now()}`;
}

/**
 * Decide a documented skip reason from observed chrome flags.
 *
 * @param {{
 *   shellPresent?: boolean,
 *   createPresent?: boolean,
 *   catalogEmpty?: boolean,
 *   redirectToSpa?: boolean,
 *   wantCreate?: boolean,
 *   wantDelete?: boolean,
 *   deletePresent?: boolean,
 *   wantEdit?: boolean,
 *   wantRedirect?: boolean,
 * }} flags
 * @returns {string|null} skip message, or null when the case should run
 */
function skipReasonForChrome(flags) {
  const f = flags || {};
  if (!f.shellPresent) {
    return SKIP.SHELL;
  }
  if (f.wantCreate && !f.createPresent) {
    return SKIP.CREATE;
  }
  if (f.wantDelete && !f.deletePresent) {
    return SKIP.DELETE;
  }
  if (f.wantEdit && f.catalogEmpty) {
    return SKIP.EDIT_EMPTY;
  }
  if (f.wantRedirect && !f.redirectToSpa) {
    return SKIP.REDIRECT;
  }
  return null;
}

/**
 * Filter noisy third-party / resource console lines from a pageerror/console list.
 * @param {string[]} errors
 * @returns {string[]}
 */
function filterConsoleNoise(errors) {
  const list = Array.isArray(errors) ? errors : [];
  return list.filter(
    (e) =>
      !/favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
        String(e),
      ),
  );
}

module.exports = {
  TEST_IDS,
  CLASSIC_ASSIGNED_TEMPLATES_ID,
  SKIP,
  designTemplatesUrl,
  designLegacyAdminUrl,
  designLegacyViewUrl,
  skipReasonForChrome,
  filterConsoleNoise,
  softVisible,
};
