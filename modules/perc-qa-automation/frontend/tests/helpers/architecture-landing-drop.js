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
 * Architecture Finder-drop landing replace helpers (#3660 / parent #3092).
 * No I/O — unit-tested without a CMS.
 */

"use strict";

const FINDER_PAGE_MIME = "application/x-percussion-finder-page";
const FINDER_FOLDER_MIME = "application/x-percussion-finder-folder";
const FINDER_ITEM_MIME = "application/x-percussion-finder-item";

/**
 * @param {{ id?: string, name?: string, path?: string, type?: string, category?: string }} item
 * @returns {string}
 */
function serializeFinderItemDrag(item) {
  const src = item && typeof item === "object" ? item : {};
  return JSON.stringify({
    id: src.id ?? "",
    name: src.name ?? "",
    path: src.path ?? "",
    type: src.type ?? "",
    category: src.category ?? "",
  });
}

/**
 * Browser-side drop of a Finder listing onto a Navigation treeitem.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} testId
 * @param {{ mime: string, payload: string }} drop
 */
async function dispatchFinderDrop(page, testId, drop) {
  await page.evaluate(
    ({ targetTestId, mime, payload }) => {
      const el = document.querySelector(`[data-testid="${targetTestId}"]`);
      if (!el) {
        throw new Error(`drop target missing: ${targetTestId}`);
      }
      const dt = new DataTransfer();
      dt.setData(mime, payload);
      dt.setData("text/plain", payload);
      dt.effectAllowed = "copy";
      const fire = (type) => {
        let ev;
        try {
          ev = new DragEvent(type, {
            bubbles: true,
            cancelable: true,
            dataTransfer: dt,
          });
        } catch {
          ev = new Event(type, { bubbles: true, cancelable: true });
        }
        if (!ev.dataTransfer) {
          Object.defineProperty(ev, "dataTransfer", {
            value: dt,
            configurable: true,
          });
        }
        el.dispatchEvent(ev);
      };
      fire("dragenter");
      fire("dragover");
      fire("drop");
    },
    { targetTestId: testId, mime: drop.mime, payload: drop.payload },
  );
}

module.exports = {
  FINDER_FOLDER_MIME,
  FINDER_ITEM_MIME,
  FINDER_PAGE_MIME,
  dispatchFinderDrop,
  serializeFinderItemDrag,
};
