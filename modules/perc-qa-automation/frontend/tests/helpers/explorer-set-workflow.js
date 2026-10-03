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
  menuItem: "explorer-set-workflow",
  dialog: "explorer-set-workflow-dialog",
  select: "explorer-set-workflow-select",
  save: "explorer-set-workflow-save",
  cancel: "explorer-set-workflow-cancel",
  status: "explorer-set-workflow-status",
  dialogError: "explorer-set-workflow-dialog-error",
  tree: "explorer-tree",
  detailList: "detail-list",
});

/**
 * @param {string} baseUrl
 * @returns {string}
 */
function explorerSetWorkflowUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
}

/**
 * @param {string} text
 * @returns {boolean}
 */
function isKnownExplorerSetWorkflowConsoleNoise(text) {
  return /favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
    String(text || ""),
  );
}

/**
 * Detail list phase for Explorer folder navigation (#5086).
 *
 * <p>A mounted {@code detail-list} with no rows and no
 * {@code detail-list-empty} marker is still loading (DetailList renders
 * the loading copy without either marker). Callers must not treat that
 * as "no selectable page".</p>
 *
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
 * True once a folder click has replaced the previous listing.
 * A still-loading list, or the same row signature as before the click,
 * is not settled — cold H2 keeps the previous (often empty) paint until
 * {@code paginatedFolder} returns (#5086).
 *
 * @param {string} beforeSignature
 * @param {"loading"|"empty"|"ready"|string} phase
 * @param {string} signature
 * @returns {boolean}
 */
function listingNavigationSettled(beforeSignature, phase, signature) {
  if (phase === "loading") {
    return false;
  }
  const next = String(signature || "");
  if (!next || next === "none") {
    return false;
  }
  return next !== String(beforeSignature || "");
}

/**
 * GET pathmanagement paginated folder listing (not folder properties).
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
  explorerSetWorkflowUrl,
  isKnownExplorerSetWorkflowConsoleNoise,
  folderListingPhase,
  listingNavigationSettled,
  isPaginatedFolderListingUrl,
};
