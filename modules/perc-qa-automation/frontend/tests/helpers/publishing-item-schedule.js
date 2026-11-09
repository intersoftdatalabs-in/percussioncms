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
 * Helpers for the PublishingShell site-workspace item schedule dates
 * set/clear surface (#5929).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const TEST_IDS = Object.freeze({
  shell: "publishing-shell",
  sitesSection: "publish-section-sites",
  siteCardPrefix: "publish-site-card-",
  workspace: "publish-site-workspace",
  panel: "item-schedule-dates",
  input: "item-schedule-id",
  lookup: "item-schedule-load",
  start: "item-schedule-start",
  end: "item-schedule-end",
  comments: "item-schedule-comments",
  save: "item-schedule-save",
  clearSubmit: "item-schedule-clear-submit",
  clearConfirm: "item-schedule-clear-confirm",
  clearYes: "item-schedule-clear-yes",
  clearCancel: "item-schedule-clear-cancel",
  success: "item-schedule-success",
  error: "item-schedule-error",
});

function publishingSitesUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/+$/, "");
  return `${root}/cm/app/spa.jsp?entry=publish`;
}

/**
 * Schedule-dates readback GET. Does not match the setitemdates write.
 * @param {string} url
 * @returns {boolean}
 */
function isGetItemDatesUrl(url) {
  const raw = String(url || "");
  return (
    /\/services\/itemmanagement\/item\/getitemdates\/[^/?#]+/.test(raw) &&
    !/setitemdates/i.test(raw)
  );
}

/**
 * Schedule-dates write POST (existing setitemdates route).
 * @param {string} url
 * @returns {boolean}
 */
function isSetItemDatesUrl(url) {
  return /\/services\/itemmanagement\/item\/setitemdates/i.test(
    String(url || ""),
  );
}

function isWritePublishUrl(url) {
  const raw = String(url || "");
  return (
    /\/sitemanage\/publish\//i.test(raw) ||
    /\/publishmanagement\/servers\//i.test(raw) ||
    /\/itemmanagement\/item\/publish/i.test(raw) ||
    isSetItemDatesUrl(raw)
  );
}

function mockItemDates(itemId, startDate, endDate) {
  return {
    ItemDates: {
      itemId: String(itemId),
      startDate: startDate || "",
      endDate: endDate || "",
      comments: "",
    },
  };
}

function isKnownPublishConsoleNoise(text) {
  const t = String(text || "");
  return /Download the React DevTools/i.test(t);
}

module.exports = {
  TEST_IDS,
  publishingSitesUrl,
  isGetItemDatesUrl,
  isSetItemDatesUrl,
  isWritePublishUrl,
  mockItemDates,
  isKnownPublishConsoleNoise,
};
