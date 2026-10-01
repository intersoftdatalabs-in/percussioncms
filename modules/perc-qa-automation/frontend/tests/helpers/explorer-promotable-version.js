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

"use strict";

/**
 * Explorer Create → Promotable Version (#5007 / parent #4530).
 * Same-folder itemmanagement promotableVersion, not newCopy.
 */

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  tree: "explorer-tree",
  detailList: "detail-list",
  nav: "explorer-nav",
  createMenu: "action-toolbar-item-Create",
  promotable: "action-toolbar-item-Edit_PromotableVersion",
  serverError: "explorer-server-actions-error",
});

/**
 * @param {string} baseUrl
 * @returns {string}
 */
function explorerPromotableUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
}

/**
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isItemPromotableUrl(url) {
  return /\/services\/itemmanagement\/item\/promotableVersion\/[^/?#]+/.test(
    String(url || ""),
  );
}

/**
 * Success is a folder-list refresh with no server-actions error.
 * @param {string | number | null | undefined} epochBefore
 * @param {string | number | null | undefined} epochAfter
 * @param {string | null | undefined} errorText
 * @returns {boolean}
 */
function isPromotableSuccess(epochBefore, epochAfter, errorText) {
  const before = Number(epochBefore);
  const after = Number(epochAfter);
  if (!Number.isFinite(before) || !Number.isFinite(after) || after <= before) {
    return false;
  }
  return String(errorText || "").trim().length === 0;
}

/**
 * @param {string | null | undefined} text
 * @param {number} status
 * @returns {boolean}
 */
function isPromotableHttpFailure(text, status) {
  const body = String(text || "");
  return body.includes(`HTTP ${status}`) && /promotable version/i.test(body);
}

/**
 * @param {string} text
 * @returns {boolean}
 */
function isKnownExplorerPromotableConsoleNoise(text) {
  return /favicon|Download the React DevTools|ResizeObserver|Failed to load resource|net::ERR_/i.test(
    String(text || ""),
  );
}

module.exports = {
  TEST_IDS,
  explorerPromotableUrl,
  isItemPromotableUrl,
  isPromotableSuccess,
  isPromotableHttpFailure,
  isKnownExplorerPromotableConsoleNoise,
};
