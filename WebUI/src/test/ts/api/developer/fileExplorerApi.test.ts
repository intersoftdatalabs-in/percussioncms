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

import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  fileExplorerChildrenUrl,
  isSafeFileExplorerRelativePath,
  isSafeFileExplorerRootId,
  listFileExplorerChildren,
  listFileExplorerRoots,
  parentFileExplorerPath,
  unwrapFileExplorerEntries,
  unwrapFileExplorerRoots,
} from "../../../../main/ts/api/developer/fileExplorerApi";
import { PATHS } from "../../../../main/ts/api/paths";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("File Explorer path-safety helpers", () => {
  it("accepts REST catalog root ids", () => {
    expect(isSafeFileExplorerRootId("rx_resources")).toBe(true);
    expect(isSafeFileExplorerRootId("drop")).toBe(true);
    expect(isSafeFileExplorerRootId("A1-b_2")).toBe(true);
  });

  it("rejects unsafe root ids", () => {
    expect(isSafeFileExplorerRootId("")).toBe(false);
    expect(isSafeFileExplorerRootId("1drop")).toBe(false);
    expect(isSafeFileExplorerRootId("../etc")).toBe(false);
    expect(isSafeFileExplorerRootId("C:Windows")).toBe(false);
    expect(isSafeFileExplorerRootId("rx/resources")).toBe(false);
  });

  it("accepts slash-separated relative paths and rejects traversal", () => {
    expect(isSafeFileExplorerRelativePath("")).toBe(true);
    expect(isSafeFileExplorerRelativePath("css")).toBe(true);
    expect(isSafeFileExplorerRelativePath("css/theme")).toBe(true);
    expect(isSafeFileExplorerRelativePath("..")).toBe(false);
    expect(isSafeFileExplorerRelativePath("css/../secret")).toBe(false);
    expect(isSafeFileExplorerRelativePath("/abs")).toBe(false);
    expect(isSafeFileExplorerRelativePath("C:/Windows")).toBe(false);
    expect(isSafeFileExplorerRelativePath("css\\theme")).toBe(false);
    expect(isSafeFileExplorerRelativePath("//unc/share")).toBe(false);
  });

  it("parentFileExplorerPath uses REST / separators", () => {
    expect(parentFileExplorerPath("css/theme")).toBe("css");
    expect(parentFileExplorerPath("css")).toBe("");
    expect(parentFileExplorerPath("")).toBe("");
  });
});

describe("unwrap File Explorer payloads", () => {
  it("unwraps a bare root array and Jackson FileExplorerRoot wrap", () => {
    expect(
      unwrapFileExplorerRoots([
        { id: "rx_resources", displayName: "rx_resources", exists: true },
      ]),
    ).toEqual([{ id: "rx_resources", displayName: "rx_resources", exists: true }]);
    expect(
      unwrapFileExplorerRoots({
        FileExplorerRoot: { id: "drop", displayName: "Drop folder", exists: false },
      }),
    ).toEqual([{ id: "drop", displayName: "Drop folder", exists: false }]);
  });

  it("skips roots with unsafe ids", () => {
    expect(
      unwrapFileExplorerRoots([{ id: "../etc", displayName: "nope" }, { id: "ok" }]),
    ).toEqual([{ id: "ok", displayName: "ok" }]);
  });

  it("unwraps children and skips unsafe relativePath", () => {
    const entries = unwrapFileExplorerEntries({
      FileExplorerEntry: [
        { name: "css", relativePath: "css", directory: true },
        { name: "bad", relativePath: "../secret", directory: true },
        { name: "logo.png", relativePath: "logo.png", directory: false, size: 12 },
      ],
    });
    expect(entries).toEqual([
      { name: "css", relativePath: "css", directory: true },
      { name: "logo.png", relativePath: "logo.png", directory: false, size: 12 },
    ]);
  });
});

describe("fileExplorerChildrenUrl", () => {
  it("omits path query at the root and encodes segments", () => {
    expect(fileExplorerChildrenUrl("rx_resources")).toBe(
      `${PATHS.FILE_EXPLORER}/rx_resources/children`,
    );
    expect(fileExplorerChildrenUrl("rx_resources", "")).toBe(
      `${PATHS.FILE_EXPLORER}/rx_resources/children`,
    );
    expect(fileExplorerChildrenUrl("rx_resources", "css/theme")).toBe(
      `${PATHS.FILE_EXPLORER}/rx_resources/children?path=css%2Ftheme`,
    );
  });
});

describe("listFileExplorer REST calls", () => {
  it("listFileExplorerRoots GETs PATHS.FILE_EXPLORER", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue([
      { id: "rx_resources", displayName: "rx_resources", exists: true },
    ]);
    const list = await listFileExplorerRoots();
    expect(spy).toHaveBeenCalledWith(PATHS.FILE_EXPLORER);
    expect(list[0].id).toBe("rx_resources");
  });

  it("listFileExplorerChildren rejects unsafe ids before GET", async () => {
    const spy = vi.spyOn(client, "get");
    await expect(listFileExplorerChildren("../etc")).rejects.toThrow(/Invalid File Explorer root/);
    await expect(listFileExplorerChildren("ok", "..")).rejects.toThrow(
      /Invalid File Explorer path/,
    );
    expect(spy).not.toHaveBeenCalled();
  });

  it("listFileExplorerChildren GETs encoded children URL", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue([]);
    await listFileExplorerChildren("rx_resources", "css");
    expect(spy).toHaveBeenCalledWith(
      `${PATHS.FILE_EXPLORER}/rx_resources/children?path=css`,
    );
  });
});
