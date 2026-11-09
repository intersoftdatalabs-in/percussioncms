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
 * Helpers for the PublishingShell site-workspace item publishing history
 * readback (#5928).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const TEST_IDS = Object.freeze({
  shell: "publishing-shell",
  sitesSection: "publish-section-sites",
  siteCardPrefix: "publish-site-card-",
  workspace: "publish-site-workspace",
  panel: "item-publishing-history",
  input: "item-history-id",
  lookup: "item-history-lookup",
  table: "item-history-table",
  row: "item-history-row",
  empty: "item-history-empty",
  idle: "item-history-idle",
  error: "item-history-error",
});

function publishingSitesUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/+$/, "");
  return `${root}/cm/app/spa.jsp?entry=publish`;
}

/**
 * Item-history readback GET. Does not match demand-publish, stage, or
 * takedown URLs.
 * @param {string} url
 * @returns {boolean}
 */
function isItemHistoryUrl(url) {
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

function mockItemHistoryRows() {
  return {
    ItemPublishingHistory: [
      {
        server: "FTP-Prod",
        location: "/home",
        revisionId: 3,
        publishedDate: Date.now(),
        operation: "publish",
        status: "SUCCESS",
        contentId: 42,
      },
      {
        server: "FTP-Prod",
        location: "/home",
        revisionId: 2,
        publishedDate: Date.now() - 1000,
        operation: "publish",
        status: "FAILURE",
        errorMessage: "denied",
        contentId: 42,
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
  isItemHistoryUrl,
  isWritePublishUrl,
  mockItemHistoryRows,
  isKnownPublishConsoleNoise,
};
