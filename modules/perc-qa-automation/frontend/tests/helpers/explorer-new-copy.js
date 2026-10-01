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
 * Explorer Create → New Copy (#5006 / parent #4530).
 * Same-folder itemmanagement newCopy, not copy-to-folder.
 */

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  tree: "explorer-tree",
  detailList: "detail-list",
  nav: "explorer-nav",
  createMenu: "action-toolbar-item-Create",
  newCopy: "action-toolbar-item-Workflow_NewVersion",
  serverError: "explorer-server-actions-error",
});

/**
 * @param {string} baseUrl
 * @returns {string}
 */
function explorerNewCopyUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
}

/**
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isItemNewCopyUrl(url) {
  return /\/services\/itemmanagement\/item\/newCopy\/[^/?#]+/.test(String(url || ""));
}

/**
 * Success is a folder-list refresh with no server-actions error.
 * @param {string | number | null | undefined} epochBefore
 * @param {string | number | null | undefined} epochAfter
 * @param {string | null | undefined} errorText
 * @returns {boolean}
 */
function isNewCopySuccess(epochBefore, epochAfter, errorText) {
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
function isNewCopyHttpFailure(text, status) {
  const body = String(text || "");
  return body.includes(`HTTP ${status}`) && /new copy/i.test(body);
}

/**
 * @param {string} text
 * @returns {boolean}
 */
function isKnownExplorerNewCopyConsoleNoise(text) {
  return /favicon|Download the React DevTools|ResizeObserver|Failed to load resource|net::ERR_/i.test(
    String(text || ""),
  );
}

/**
 * Next detail-list folder to open while looking for a content item.
 * Page walks prefer a folder whose name is exactly {@code Pages}. Rows
 * already opened ({@code seenIds}) are skipped so a path cell that merely
 * contains "Pages" cannot pin the walk on one folder (#5023).
 *
 * @param {readonly { id?: string, name?: string }[]} rows
 * @param {"page"|"asset"} kind
 * @param {ReadonlySet<string> | readonly string[] | null | undefined} seenIds
 * @returns {number} index into {@code rows}, or -1
 */
function pickContentFolderIndex(rows, kind, seenIds) {
  const seen =
    seenIds instanceof Set ? seenIds : new Set(seenIds || []);
  const list = Array.isArray(rows) ? rows : [];
  /** @type {{ index: number, id: string, name: string }[]} */
  const usable = [];
  for (let index = 0; index < list.length; index += 1) {
    const row = list[index];
    if (!row) continue;
    const id = String(row.id || "").trim();
    if (!id || seen.has(id)) continue;
    usable.push({
      index,
      id,
      name: String(row.name || "").trim(),
    });
  }
  if (kind === "page") {
    const pages = usable.find((row) => /^pages$/i.test(row.name));
    if (pages) return pages.index;
  }
  return usable.length > 0 ? usable[0].index : -1;
}

module.exports = {
  TEST_IDS,
  explorerNewCopyUrl,
  isItemNewCopyUrl,
  isNewCopySuccess,
  isNewCopyHttpFailure,
  isKnownExplorerNewCopyConsoleNoise,
  pickContentFolderIndex,
};
