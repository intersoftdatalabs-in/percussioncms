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
 * Developer Object Sorter surface helpers (#4344 / parent #1690).
 * No live CMS. Sort preference is session-only (no REST peer).
 */

"use strict";

const TEST_IDS = Object.freeze({
  tab: "tab-developer-object-sorter",
  panel: "developer-os-panel",
  empty: "developer-os-empty",
  error: "developer-os-error",
  loading: "developer-os-loading",
  table: "developer-os-table",
  row: "developer-os-row",
  mode: "developer-os-mode",
  moveDown: "developer-os-move-down",
  sessionNote: "developer-os-session-note",
});

/**
 * @param {string} baseUrl TEST_CMS_URL / BASE_URL
 * @returns {string}
 */
function developerObjectSorterUrl(baseUrl) {
  const base = String(baseUrl || "").replace(/\/+$/, "");
  const q = new URLSearchParams({
    entry: "developer",
    section: "object-sorter",
    _: String(Date.now()),
  });
  return `${base}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * @param {string[]} consoleErrors
 * @returns {string[]}
 */
function unexpectedConsoleErrors(consoleErrors) {
  if (!Array.isArray(consoleErrors)) return [];
  return consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
}

module.exports = {
  TEST_IDS,
  developerObjectSorterUrl,
  unexpectedConsoleErrors,
};
