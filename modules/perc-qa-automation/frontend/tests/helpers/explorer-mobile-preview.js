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
 * Explorer Content → Mobile preview helpers (#5078 / parent #4530).
 */

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  menuContent: "explorer-menu-content",
  contentDropdown: "explorer-menu-content-dropdown",
  mobilePreview: "explorer-mobile-preview",
  status: "explorer-mobile-preview-status",
  tree: "explorer-tree",
  detailList: "detail-list",
  desktopPreview: "action-preview",
});

/**
 * @param {string} baseUrl
 * @returns {string}
 */
function explorerMobilePreviewUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
}

/**
 * Mobile preview window URLs carry the flag. Desktop page render does not.
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isMobilePreviewWindowUrl(url) {
  const value = String(url || "");
  if (!value.includes("percmobilepreview=true")) {
    return false;
  }
  if (/spa\.jsp\?[^#]*entry=editor|\/cm\/app\/editor/i.test(value)) {
    return false;
  }
  return (
    value.includes("/pagemanagement/render/page/") ||
    /\/Sites\//i.test(value)
  );
}

/**
 * Desktop Preview must not force the mobile flag.
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isDesktopPreviewWindowUrl(url) {
  const value = String(url || "");
  if (value.includes("percmobilepreview=true")) {
    return false;
  }
  if (/spa\.jsp\?[^#]*entry=editor|\/cm\/app\/editor/i.test(value)) {
    return false;
  }
  return (
    value.includes("/pagemanagement/render/page/") ||
    value.includes("percmobilepreview=false") ||
    value.includes("/assetmanagement/asset/")
  );
}

/**
 * @param {string} text
 * @returns {boolean}
 */
function isKnownExplorerMobilePreviewConsoleNoise(text) {
  return /favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
    String(text || ""),
  );
}

module.exports = {
  TEST_IDS,
  explorerMobilePreviewUrl,
  isMobilePreviewWindowUrl,
  isDesktopPreviewWindowUrl,
  isKnownExplorerMobilePreviewConsoleNoise,
};
