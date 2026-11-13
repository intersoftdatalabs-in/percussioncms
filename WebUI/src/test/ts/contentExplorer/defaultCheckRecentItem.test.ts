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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  defaultCheckRecentItem,
} from "../../../main/ts/contentExplorer/ContentExplorerShell";
import {
  findItemById,
  findItemByPath,
} from "../../../main/ts/api/contentExplorer/pathApi";
import type { RecentItem } from "../../../main/ts/contentExplorer/recentItems";

vi.mock("../../../main/ts/api/contentExplorer/pathApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../../main/ts/api/contentExplorer/pathApi")>();
  return {
    ...actual,
    findItemById: vi.fn(),
    findItemByPath: vi.fn(),
  };
});

const itemById = vi.mocked(findItemById);
const itemByPath = vi.mocked(findItemByPath);

function apiError(status: number): Record<string, unknown> {
  return { status, statusText: `HTTP ${status}`, body: undefined };
}

describe("defaultCheckRecentItem (#6256)", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("rejects entries with neither id nor path without calling REST", async () => {
    await expect(
      defaultCheckRecentItem({ id: null, name: "", path: "  ", kind: "item" }),
    ).resolves.toBe(false);
    expect(itemById).not.toHaveBeenCalled();
    expect(itemByPath).not.toHaveBeenCalled();
  });

  it("keeps a folder whose path read succeeds", async () => {
    itemByPath.mockResolvedValue({ id: "1", name: "Demo", path: "/Sites/Demo/" } as never);
    const entry: RecentItem = { id: null, name: "Demo", path: "/Sites/Demo/", kind: "folder" };
    await expect(defaultCheckRecentItem(entry)).resolves.toBe(true);
    expect(itemById).not.toHaveBeenCalled();
  });

  it("drops a folder whose path read 404s, keeps on other failures", async () => {
    itemByPath.mockRejectedValueOnce(apiError(404));
    await expect(
      defaultCheckRecentItem({ id: null, name: "Gone", path: "/Sites/Gone/", kind: "folder" }),
    ).resolves.toBe(false);
    itemByPath.mockRejectedValueOnce(apiError(500));
    await expect(
      defaultCheckRecentItem({ id: null, name: "Demo", path: "/Sites/Demo/", kind: "folder" }),
    ).resolves.toBe(true);
  });

  it("keeps an item whose id read succeeds", async () => {
    itemById.mockResolvedValue({ id: "55", name: "Home", path: "/Sites/Demo/Home" } as never);
    await expect(
      defaultCheckRecentItem({ id: "55", name: "Home", path: "/Sites/Demo/Home", kind: "item" }),
    ).resolves.toBe(true);
    expect(itemByPath).not.toHaveBeenCalled();
  });

  it("drops an item on a 404 id read without a path fallback", async () => {
    itemById.mockRejectedValueOnce(apiError(404));
    await expect(
      defaultCheckRecentItem({ id: "999", name: "Gone", path: "/Sites/Demo/Gone", kind: "item" }),
    ).resolves.toBe(false);
    expect(itemByPath).not.toHaveBeenCalled();
  });

  it("falls back to the path read when the id read 500s (unknown ids 500, not 404)", async () => {
    // Unknown id (500) + missing path (404) → dropped.
    itemById.mockRejectedValueOnce(apiError(500));
    itemByPath.mockRejectedValueOnce(apiError(404));
    await expect(
      defaultCheckRecentItem({
        id: "99999999-999-999",
        name: "Deleted item",
        path: "/Sites/Demo/Deleted",
        kind: "item",
      }),
    ).resolves.toBe(false);
    // Unknown id (500) + live path (200) → kept, open attempted.
    itemById.mockRejectedValueOnce(apiError(500));
    itemByPath.mockResolvedValueOnce({ id: "55", name: "Home", path: "/Sites/Demo/Home" } as never);
    await expect(
      defaultCheckRecentItem({ id: "55", name: "Home", path: "/Sites/Demo/Home", kind: "item" }),
    ).resolves.toBe(true);
    // Id-only entry whose id read fails → dropped (nothing to open by).
    itemById.mockRejectedValueOnce(apiError(500));
    await expect(
      defaultCheckRecentItem({ id: "999", name: "Id only", path: "", kind: "item" }),
    ).resolves.toBe(false);
  });
});
