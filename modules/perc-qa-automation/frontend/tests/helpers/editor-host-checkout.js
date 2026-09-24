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
 * Helpers for React Content Editor host check-out / check-in (#4603).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const { editorSpaUrl } = require("./editor-host-workflow");

const TEST_IDS = Object.freeze({
  host: "editor-host",
  form: "editor-form",
  checkout: "editor-checkout",
  checkin: "editor-checkin",
  checkinComment: "editor-checkin-comment",
  checkinCommentInput: "editor-checkin-comment-input",
  checkinConfirm: "editor-checkin-confirm",
  checkinCancel: "editor-checkin-cancel",
  lockError: "editor-lock-error",
  locked: "editor-locked",
  save: "editor-save",
});

/**
 * @param {string} url
 * @returns {boolean}
 */
function isWorkflowCheckoutUrl(url) {
  const u = String(url || "");
  return (
    /\/services\/itemmanagement\/workflow\/checkOut\//.test(u) ||
    /\/rest\/editor\/items\/[^/]+\/checkout/.test(u)
  );
}

/**
 * @param {string} url
 * @returns {boolean}
 */
function isWorkflowCheckinUrl(url) {
  const u = String(url || "");
  return (
    /\/services\/itemmanagement\/workflow\/checkIn\//.test(u) ||
    /\/rest\/editor\/items\/[^/]+\/checkin/.test(u)
  );
}

module.exports = {
  TEST_IDS,
  editorSpaUrl,
  isWorkflowCheckoutUrl,
  isWorkflowCheckinUrl,
};
