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

import { describe, expect, it, vi } from "vitest";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import {
  copyCheckedItems,
  describeMultiNewCopy,
  runMultiNewCopy,
} from "../../../main/ts/contentExplorer/multiSelectNewCopy";

function page(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    name: "Home",
    path: "/Sites/Demo/Home",
    type: "percPage",
    category: "page",
    id: "42",
    ...overrides,
  };
}

function folder(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    name: "News",
    path: "/Sites/Demo/News/",
    type: "Folder",
    category: "folder",
    id: "7",
    ...overrides,
  };
}

describe("multiSelectNewCopy", () => {
  it("shows each copy only after that copy succeeds and skips folders", async () => {
    const order: string[] = [];
    const result = await copyCheckedItems(
      [
        page(),
        folder(),
        page({
          id: "43",
          name: "About",
          path: "/Sites/Demo/About",
        }),
      ],
      async (itemId) => {
        order.push(`copy:${itemId}`);
        if (itemId === "43") {
          throw { status: 409, statusText: "conflict", body: {} };
        }
        return { itemId: "9001" };
      },
      async (copied) => {
        order.push(`shown:${copied.sourceId}:${copied.copyId}`);
      },
    );
    expect(order).toEqual(["copy:42", "shown:42:9001", "copy:43"]);
    expect(result.copied.map((row) => row.sourceId)).toEqual(["42"]);
    expect(result.skippedFolderNames).toEqual(["News"]);
    expect(result.failures).toEqual([
      { name: "About", status: 409, message: "HTTP 409" },
    ]);
    const text = describeMultiNewCopy(result) ?? "";
    expect(text).toContain("Folders are not copied: News");
    expect(text).toContain("About (HTTP 409)");
    expect(text).toContain("Not every selected item got a new copy");
    expect(text).not.toMatch(/selection was copied/i);
  });

  it("does not copy when confirm is cancelled", async () => {
    const copyOne = vi.fn();
    const onItemCopied = vi.fn();
    const result = await runMultiNewCopy({
      items: [page(), page({ id: "43", name: "About" })],
      confirm: () => false,
      copyOne,
      onItemCopied,
    });
    expect(copyOne).not.toHaveBeenCalled();
    expect(onItemCopied).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
    expect(result.messageKey).toBeUndefined();
  });

  it("names folders and copies nothing when no page or asset is checked", async () => {
    const copyOne = vi.fn();
    const result = await runMultiNewCopy({
      items: [folder(), folder({ id: "8", name: "Blog", path: "/Sites/Demo/Blog/" })],
      confirm: () => true,
      copyOne,
    });
    expect(copyOne).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
    expect(result.messageKey).toBe(EXPLORER_MSG.NEW_COPY_NOTHING_ELIGIBLE);
    expect(result.messageText).toContain("News");
    expect(result.messageText).toContain("Blog");
  });

  it.each([400, 403, 409])(
    "HTTP %s on one item is not a full-selection copy",
    async (status) => {
      const shown: string[] = [];
      const result = await runMultiNewCopy({
        items: [page(), page({ id: "43", name: "About", path: "/Sites/Demo/About" })],
        confirm: (body) => {
          expect(body).toContain("Create a new copy of 2 selected items");
          return true;
        },
        copyOne: async (itemId) => {
          if (itemId === "43") {
            throw { status, statusText: "no", body: {} };
          }
          return { itemId: "9001" };
        },
        onItemCopied: (copied) => {
          shown.push(copied.sourceId);
        },
      });
      expect(shown).toEqual(["42"]);
      expect(result.refresh).toBe(true);
      expect(result.messageKey).toBe(EXPLORER_MSG.NEW_COPY_BATCH_INCOMPLETE);
      expect(result.messageText).toContain(`About (HTTP ${status})`);
      expect(result.messageText).toContain("Not every selected item got a new copy");
    },
  );
});
