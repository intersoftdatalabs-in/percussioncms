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
 * Helpers for React Content Editor host workflow transitions (#4539).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const TEST_IDS = Object.freeze({
  host: "editor-host",
  form: "editor-form",
  workflow: "editor-workflow",
  comment: "editor-workflow-comment",
  error: "editor-workflow-error",
  done: "editor-workflow-done",
  triggerPrefix: "editor-workflow-trigger-",
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
function isGetTransitionsUrl(url) {
  return /\/services\/itemmanagement\/workflow\/getTransitions\//.test(
    String(url || ""),
  );
}

/**
 * @param {string} url
 * @returns {boolean}
 */
function isTransitionWithCommentsUrl(url) {
  return /\/services\/itemmanagement\/workflow\/transitionWithComments\//.test(
    String(url || ""),
  );
}

/**
 * @param {string} url
 * @returns {{ itemId: string, trigger: string, comment: string }}
 */
function parseTransitionWithCommentsUrl(url) {
  const raw = String(url || "");
  const match = raw.match(
    /\/transitionWithComments\/([^/?#]+)\/([^/?#]+)/,
  );
  let comment = "";
  try {
    const parsed = new URL(raw, "http://cms.example");
    comment = parsed.searchParams.get("comment") || "";
  } catch {
    comment = "";
  }
  return {
    itemId: match ? decodeURIComponent(match[1]) : "",
    trigger: match ? decodeURIComponent(match[2]) : "",
    comment,
  };
}

/**
 * @param {string} trigger
 * @returns {string}
 */
function triggerTestId(trigger) {
  return `${TEST_IDS.triggerPrefix}${trigger}`;
}

module.exports = {
  TEST_IDS,
  editorSpaUrl,
  isGetTransitionsUrl,
  isTransitionWithCommentsUrl,
  parseTransitionWithCommentsUrl,
  triggerTestId,
};
