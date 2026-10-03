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

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  menuItem: "explorer-set-community",
  dialog: "explorer-set-community-dialog",
  select: "explorer-set-community-select",
  save: "explorer-set-community-save",
  cancel: "explorer-set-community-cancel",
  status: "explorer-set-community-status",
  dialogError: "explorer-set-community-dialog-error",
  tree: "explorer-tree",
  detailList: "detail-list",
});

/**
 * @param {string} baseUrl
 * @returns {string}
 */
function explorerSetCommunityUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
}

/**
 * @param {string} text
 * @returns {boolean}
 */
function isKnownExplorerSetCommunityConsoleNoise(text) {
  return /favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
    String(text || ""),
  );
}

/**
 * @param {{ listVisible?: boolean, rowCount?: number, emptyVisible?: boolean }} [state]
 * @returns {"loading"|"empty"|"ready"}
 */
function folderListingPhase(state = {}) {
  if (!state.listVisible) {
    return "loading";
  }
  if ((state.rowCount || 0) > 0) {
    return "ready";
  }
  if (state.emptyVisible) {
    return "empty";
  }
  return "loading";
}

/**
 * Empty listings settle only after a paginatedFolder GET. The idle
 * {@code !folderPath} paint is also {@code detail-list-empty}.
 *
 * @param {string} beforeSignature
 * @param {"loading"|"empty"|"ready"|string} phase
 * @param {string} signature
 * @param {boolean} [listingResponseSeen]
 * @returns {boolean}
 */
function listingNavigationSettled(
  beforeSignature,
  phase,
  signature,
  listingResponseSeen = false,
) {
  if (phase === "loading") {
    return false;
  }
  const next = String(signature || "");
  if (!next || next === "none") {
    return false;
  }
  if (next === String(beforeSignature || "")) {
    return false;
  }
  if (next === "empty" && !listingResponseSeen) {
    return false;
  }
  return true;
}

/**
 * @param {string} url
 * @param {string} [method]
 * @returns {boolean}
 */
function isPaginatedFolderListingUrl(url, method = "GET") {
  if (String(method || "GET").toUpperCase() !== "GET") {
    return false;
  }
  return String(url || "").includes("/pathmanagement/path/paginatedFolder/");
}

module.exports = {
  TEST_IDS,
  explorerSetCommunityUrl,
  isKnownExplorerSetCommunityConsoleNoise,
  folderListingPhase,
  listingNavigationSettled,
  isPaginatedFolderListingUrl,
};
