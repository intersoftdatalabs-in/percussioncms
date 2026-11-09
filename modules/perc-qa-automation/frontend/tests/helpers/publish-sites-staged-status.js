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
 * Helpers for the PublishingShell staged-item status/log readback (#5838).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const TEST_IDS = Object.freeze({
  shell: "publishing-shell",
  sitesSection: "publish-section-sites",
  siteCardPrefix: "publish-site-card-",
  workspace: "publish-site-workspace",
  panel: "item-staged-status",
  input: "item-staged-status-id",
  lookup: "item-staged-status-lookup",
  summary: "item-staged-status-summary",
  table: "item-staged-status-table",
  row: "item-staged-status-row",
  severity: "item-staged-status-severity",
  filterEmpty: "item-staged-status-filter-empty",
  empty: "item-staged-status-empty",
  error: "item-staged-status-error",
});

function publishingSitesUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/+$/, "");
  return `${root}/cm/app/spa.jsp?entry=publish`;
}

/**
 * Staged-status readback GET. Does not match demand-publish, stage, or
 * takedown URLs.
 * @param {string} url
 * @returns {boolean}
 */
function isStagedStatusUrl(url) {
  return /\/services\/itemmanagement\/item\/pubhistory\/[^/?#]+/.test(
    String(url || ""),
  );
}

function isWritePublishUrl(url) {
  const raw = String(url || "");
  return (
    /\/sitemanage\/publish\//i.test(raw) ||
    /\/publishmanagement\/servers\//i.test(raw) ||
    /\/itemmanagement\/item\/publish/i.test(raw)
  );
}

function mockStagedStatusLines() {
  return {
    ItemPublishingHistory: [
      {
        server: "FTP-Prod",
        location: "/home",
        revisionId: 3,
        publishedDate: 200,
        operation: "publish",
        status: "SUCCESS",
      },
      {
        server: "FTP-Prod",
        location: "/home",
        revisionId: 2,
        publishedDate: 100,
        operation: "publish",
        status: "FAILURE",
        errorMessage: "denied",
      },
    ],
  };
}

function isKnownPublishConsoleNoise(text) {
  const t = String(text || "");
  return /Download the React DevTools/i.test(t);
}

module.exports = {
  TEST_IDS,
  publishingSitesUrl,
  isStagedStatusUrl,
  isWritePublishUrl,
  mockStagedStatusLines,
  isKnownPublishConsoleNoise,
};
