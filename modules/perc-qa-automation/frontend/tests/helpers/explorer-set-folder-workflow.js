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
  menuItem: "explorer-set-folder-workflow",
  dialog: "explorer-set-folder-workflow-dialog",
  select: "explorer-set-folder-workflow-select",
  save: "explorer-set-folder-workflow-save",
  cancel: "explorer-set-folder-workflow-cancel",
  status: "explorer-set-folder-workflow-status",
  dialogError: "explorer-set-folder-workflow-dialog-error",
  tree: "explorer-tree",
  detailList: "detail-list",
});

/**
 * @param {string} baseUrl
 * @returns {string}
 */
function explorerSetFolderWorkflowUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
}

/**
 * @param {string} text
 * @returns {boolean}
 */
function isKnownExplorerSetFolderWorkflowConsoleNoise(text) {
  return /favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
    String(text || ""),
  );
}

module.exports = {
  TEST_IDS,
  explorerSetFolderWorkflowUrl,
  isKnownExplorerSetFolderWorkflowConsoleNoise,
};
