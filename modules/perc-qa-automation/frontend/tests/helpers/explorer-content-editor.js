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
 * Pure helpers for Explorer Open/Edit Playwright (#3638 / parent #3102).
 *
 * <p>No live CMS dependency — unit-tested via node:test.</p>
 */

"use strict";

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  tree: "explorer-tree",
  list: "detail-list",
  reducedActions: "reduced-actions",
  open: "action-open",
  edit: "action-toolbar-item-Edit",
  contextEdit: "context-menu-item-Edit",
  editorHost: "editor-host",
  editorError: "editor-error",
  editorForm: "editor-form",
  editorLoading: "editor-loading",
  editorEmpty: "editor-empty",
  editorOverlay: "editor-overlay",
});

/**
 * QA H2 matrix ({@code TEST_DB_TYPE=h2}) must not soft-skip Open/Edit.
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} [env]
 * @returns {boolean}
 */
function isH2QaEnv(env = process.env) {
  const db = String(env.TEST_DB_TYPE || env.TEST_DATABASE || "")
    .trim()
    .toLowerCase();
  return db === "h2";
}

/**
 * Skip only off H2 when REST found no item and the list has no item row.
 * H2 demo-sites fail instead of skip (#3638).
 *
 * @param {{
 *   listedPage?: unknown,
 *   itemRowCount?: number,
 *   uiHasItemRow?: boolean,
 *   h2?: boolean,
 * }} [detail]
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} [env]
 * @returns {boolean}
 */
function shouldSkipListedPageEditor(detail = {}, env = process.env) {
  if (detail.listedPage) {
    return false;
  }
  if ((detail.itemRowCount || 0) > 0) {
    return false;
  }
  if (detail.uiHasItemRow === true) {
    return false;
  }
  if (detail.h2 === true || isH2QaEnv(env)) {
    return false;
  }
  return true;
}

/**
 * Soft-skip message when a non-H2 fixture has no listed item row.
 * @returns {string}
 */
function noListedItemSkipMessage() {
  return (
    "No listed page/asset child under Sites after REST walk; " +
    "Open/Edit requires an item row (parent #3102 / slice #3638). " +
    "H2 demo-sites must not use this skip."
  );
}

/**
 * Whether a popup URL is the React Content Editor host.
 * Leftover CM1 {@code view=editor} is never a product editor URL.
 * @param {string} url
 * @returns {boolean}
 */
function isProductEditorUrl(url) {
  const u = String(url || "").replace(/\\/g, "/").toLowerCase();
  if (!u) return false;
  if (isLeftoverContentEditorUrl(u)) return false;
  if (u.includes("entry=editor")) return true;
  if (/\/cm\/app\/editor(?:\/|\?|$)/.test(u)) return true;
  return false;
}

/**
 * Leftover Data Flow / CM1 Content Editor HTML — Open/Edit must not request these.
 * @param {string} url
 * @returns {boolean}
 */
function isLeftoverContentEditorUrl(url) {
  const u = String(url || "").replace(/\\/g, "/").toLowerCase();
  if (!u) return false;
  if (u.includes("editasset.jsp")) return true;
  if (u.includes("checkoutedit.xml")) return true;
  if (u.includes("contenteditorurls.html")) return true;
  if (u.includes("sys_cesupport")) return true;
  if (/(?:^|[?&])view=editor(?:&|$)/.test(u)) return true;
  if (/\/rx_ce(?:\/|\?|$)/.test(u) || /\/psx_ce(?:\/|\?|$)/.test(u)) {
    return true;
  }
  return false;
}

/**
 * KeywordFieldWidget crash from calling {@code .trim} on a JSON number (#3968).
 * @param {unknown} message
 * @returns {boolean}
 */
function isKeywordTrimCrash(message) {
  return /trim is not a function/i.test(String(message || ""));
}

/**
 * True when the editor host still has product chrome (not a blank page).
 * Checkout failures may show {@code editor-error} instead of the form.
 * @param {{
 *   host?: boolean,
 *   overlay?: boolean,
 *   form?: boolean,
 *   error?: boolean,
 *   loading?: boolean,
 *   empty?: boolean,
 * }} [state]
 * @returns {boolean}
 */
function isEditorStayVisible(state = {}) {
  if (!state.host || !state.overlay) {
    return false;
  }
  return Boolean(state.form || state.error || state.loading || state.empty);
}

module.exports = {
  TEST_IDS,
  isH2QaEnv,
  shouldSkipListedPageEditor,
  noListedItemSkipMessage,
  isProductEditorUrl,
  isLeftoverContentEditorUrl,
  isKeywordTrimCrash,
  isEditorStayVisible,
};
