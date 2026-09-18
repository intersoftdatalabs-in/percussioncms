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
 * Helpers for React Content Editor required-field save errors (#4541).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const TEST_IDS = Object.freeze({
  host: "editor-host",
  form: "editor-form",
  save: "editor-save",
  checkin: "editor-checkin",
  saveError: "editor-save-error",
  fieldDisplayTitle: "editor-field-displaytitle",
  fieldErrorDisplayTitle: "editor-field-error-displaytitle",
});

/**
 * @param {string} baseUrl
 * @param {string} [query]
 * @returns {string}
 */
function editorSpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const params = new URLSearchParams(
    query.startsWith("?") ? query.slice(1) : query,
  );
  params.set("entry", "editor");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

/**
 * @param {string} url
 * @returns {boolean}
 */
function isItemFieldsPutUrl(url) {
  const raw = String(url || "");
  return /\/services\/itemmanagement\/item\/fields\//.test(raw);
}

/**
 * @param {string} url
 * @returns {boolean}
 */
function isWorkflowCheckinUrl(url) {
  return /\/services\/itemmanagement\/workflow\/checkIn\//.test(String(url || ""));
}

module.exports = {
  TEST_IDS,
  editorSpaUrl,
  isItemFieldsPutUrl,
  isWorkflowCheckinUrl,
};
