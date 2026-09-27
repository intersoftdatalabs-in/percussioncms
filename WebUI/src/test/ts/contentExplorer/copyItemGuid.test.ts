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
import { copySelectedItemGuid } from "../../../main/ts/contentExplorer/copyItemGuid";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";

const page: PSPathItem = {
  id: "16777215-101-551",
  name: "Corporate Investments Home",
  path: "/Sites/CI/Home",
  type: "percPage",
  category: "page",
  leaf: true,
};

describe("copySelectedItemGuid (#4989)", () => {
  it("writes the selected page content id", async () => {
    const writeClipboard = vi.fn().mockResolvedValue(undefined);
    const result = await copySelectedItemGuid({
      item: page,
      writeClipboard,
    });
    expect(result).toEqual({ status: "copied", guid: "16777215-101-551" });
    expect(writeClipboard).toHaveBeenCalledWith("16777215-101-551");
  });

  it("writes an asset content id", async () => {
    const writeClipboard = vi.fn().mockResolvedValue(undefined);
    const result = await copySelectedItemGuid({
      item: {
        id: "16777215-101-708",
        name: "Logo",
        path: "/Assets/Logo",
        type: "percAsset",
        category: "asset",
        leaf: true,
      },
      writeClipboard,
    });
    expect(result).toEqual({ status: "copied", guid: "16777215-101-708" });
  });

  it("does not write the clipboard when nothing is selected", async () => {
    const writeClipboard = vi.fn();
    const result = await copySelectedItemGuid({
      item: null,
      writeClipboard,
    });
    expect(result).toEqual({ status: "none" });
    expect(writeClipboard).not.toHaveBeenCalled();
  });

  it("names a folder and does not claim success", async () => {
    const writeClipboard = vi.fn();
    const result = await copySelectedItemGuid({
      item: {
        id: "16777215-101-703",
        name: "Corporate Investments",
        path: "/Sites/CI/",
        type: "site",
        category: "site",
      },
      writeClipboard,
    });
    expect(result).toEqual({
      status: "folder",
      name: "Corporate Investments",
    });
    expect(writeClipboard).not.toHaveBeenCalled();
  });

  it("does not claim success when the item has no content id", async () => {
    const writeClipboard = vi.fn();
    const result = await copySelectedItemGuid({
      item: {
        name: "Untitled",
        path: "/Sites/CI/Untitled",
        type: "percPage",
        category: "page",
        leaf: true,
      },
      writeClipboard,
    });
    expect(result).toEqual({ status: "no-id", name: "Untitled" });
    expect(writeClipboard).not.toHaveBeenCalled();
  });

  it("clipboard failure does not report success", async () => {
    const result = await copySelectedItemGuid({
      item: page,
      writeClipboard: async () => {
        throw new Error("denied");
      },
    });
    expect(result).toEqual({ status: "failed", guid: "16777215-101-551" });
  });
});
