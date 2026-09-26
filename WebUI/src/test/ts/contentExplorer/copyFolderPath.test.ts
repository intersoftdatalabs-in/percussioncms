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
import { copySelectedFolderPath } from "../../../main/ts/contentExplorer/copyFolderPath";

describe("copySelectedFolderPath (#4911)", () => {
  it("writes the selected folder path after confirm", async () => {
    const writeClipboard = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    const result = await copySelectedFolderPath({
      folderPath: "/Sites",
      itemPath: "/Sites/Demo",
      itemType: "folder",
      confirm,
      writeClipboard,
    });
    expect(result).toEqual({ status: "copied", path: "/Sites/Demo" });
    expect(confirm).toHaveBeenCalledWith("/Sites/Demo");
    expect(writeClipboard).toHaveBeenCalledWith("/Sites/Demo");
  });

  it("uses the containing folder when the selection is a content item", async () => {
    const writeClipboard = vi.fn().mockResolvedValue(undefined);
    const result = await copySelectedFolderPath({
      folderPath: "/Sites/Demo/Home",
      itemPath: "/Sites/Demo/Home/page1",
      itemType: "page",
      confirm: () => true,
      writeClipboard,
    });
    expect(result).toEqual({ status: "copied", path: "/Sites/Demo/Home" });
    expect(writeClipboard).toHaveBeenCalledWith("/Sites/Demo/Home");
  });

  it("treats an empty path as an error and does not write the clipboard", async () => {
    const writeClipboard = vi.fn();
    const confirm = vi.fn();
    const result = await copySelectedFolderPath({
      folderPath: "/",
      itemPath: null,
      itemType: null,
      confirm,
      writeClipboard,
    });
    expect(result).toEqual({ status: "empty" });
    expect(confirm).not.toHaveBeenCalled();
    expect(writeClipboard).not.toHaveBeenCalled();
  });

  it("cancel writes nothing", async () => {
    const writeClipboard = vi.fn();
    const result = await copySelectedFolderPath({
      folderPath: "/Assets",
      confirm: () => false,
      writeClipboard,
    });
    expect(result).toEqual({ status: "cancelled", path: "/Assets" });
    expect(writeClipboard).not.toHaveBeenCalled();
  });

  it("clipboard failure does not report success", async () => {
    const result = await copySelectedFolderPath({
      folderPath: "/Assets",
      confirm: () => true,
      writeClipboard: async () => {
        throw new Error("denied");
      },
    });
    expect(result).toEqual({ status: "failed", path: "/Assets" });
  });
});
