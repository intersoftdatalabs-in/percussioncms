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
import type { ApiError } from "../../../main/ts/api/client";
import { unwrapFolderCommunityCatalog } from "../../../main/ts/api/contentExplorer/folderCommunityApi";
import type { PSFolderProperties, PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  classifySetFolderCommunitySelection,
  folderCommunityIdText,
  loadSetFolderCommunityCatalog,
  readReloadedCommunityId,
  saveSetFolderCommunity,
} from "../../../main/ts/contentExplorer/setFolderCommunity";

function httpError(status: number): ApiError {
  return { status, statusText: String(status), body: null };
}

const folder: PSPathItem = {
  id: "16777215-101-703",
  name: "CI",
  path: "/Sites/CI/",
  type: "folder",
  category: "folder",
  leaf: false,
};

const page: PSPathItem = {
  id: "16777215-101-551",
  name: "Home",
  path: "/Sites/CI/Home",
  type: "percPage",
  category: "page",
  leaf: true,
};

const asset: PSPathItem = {
  id: "16777215-101-900",
  name: "Logo",
  path: "/Assets/Logo",
  type: "percImage",
  category: "asset",
  leaf: true,
};

const props = (communityId: string, communityName = ""): PSFolderProperties => ({
  id: folder.id ?? "",
  name: "CI",
  communityId,
  communityName,
  permission: { accessLevel: "ADMIN" },
});

describe("set folder community (#5105)", () => {
  it("blocks empty, pages, assets, and multi-select before any save", () => {
    expect(classifySetFolderCommunitySelection({ item: null, selectedCount: 0 })).toMatchObject({
      status: "blocked",
      reason: "empty",
    });
    expect(classifySetFolderCommunitySelection({ item: page, selectedCount: 1 })).toMatchObject({
      status: "blocked",
      reason: "page",
      name: "Home",
    });
    expect(classifySetFolderCommunitySelection({ item: asset, selectedCount: 1 })).toMatchObject({
      status: "blocked",
      reason: "asset",
      name: "Logo",
    });
    expect(classifySetFolderCommunitySelection({ item: folder, selectedCount: 2 })).toMatchObject({
      reason: "multi",
    });
    expect(classifySetFolderCommunitySelection({ item: folder, selectedCount: 1 })).toMatchObject({
      status: "ready",
      folderId: folder.id,
    });
  });

  it("loads the catalog and does not save on cancel-sized gates", async () => {
    const save = vi.fn();
    const loaded = await loadSetFolderCommunityCatalog({
      item: folder,
      selectedCount: 1,
      loadProps: async () => props("10", "Default"),
      loadCatalog: async () => ({
        choices: [
          { id: "10", name: "Default" },
          { id: "12", name: "Enterprise" },
        ],
      }),
    });
    expect(loaded.status).toBe("ready");
    if (loaded.status !== "ready") {
      return;
    }
    const unchanged = await saveSetFolderCommunity({
      folderId: loaded.folderId,
      props: loaded.props,
      selectedId: "10",
      currentId: loaded.currentId,
      allowedIds: loaded.choices.map((row) => row.id),
      communityName: "Default",
      save,
    });
    expect(unchanged).toEqual({ status: "gate", reason: "unchanged" });
    expect(save).not.toHaveBeenCalled();
  });

  it("saves only when refresh shows the new community id", async () => {
    const save = vi.fn(async () => undefined);
    const reload = vi.fn(async () => props("12", "Enterprise"));
    const saved = await saveSetFolderCommunity({
      folderId: "16777215-101-703",
      props: props("10", "Default"),
      selectedId: "12",
      currentId: "10",
      allowedIds: ["10", "12"],
      communityName: "Enterprise",
      save,
      reload,
    });
    expect(saved).toEqual({
      status: "saved",
      communityId: "12",
      communityName: "Enterprise",
    });
    expect(save).toHaveBeenCalledTimes(1);
    const posted = save.mock.calls[0][0] as PSFolderProperties;
    expect(posted.communityId).toBe("12");
    expect(posted.communityName).toBe("Enterprise");
    expect(posted.id).toBe("16777215-101-703");
    expect(posted.name).toBe("CI");
  });

  it("does not claim success when refresh still has the old community", async () => {
    const result = await saveSetFolderCommunity({
      folderId: "16777215-101-703",
      props: props("10", "Default"),
      selectedId: "12",
      currentId: "10",
      allowedIds: ["10", "12"],
      communityName: "Enterprise",
      save: async () => undefined,
      reload: async () => props("10", "Default"),
    });
    expect(result).toEqual({ status: "mismatch" });
  });

  it("HTTP 400, 403, and 409 are not a saved community", async () => {
    for (const status of [400, 403, 409] as const) {
      const result = await saveSetFolderCommunity({
        folderId: "16777215-101-703",
        props: props("10"),
        selectedId: "12",
        currentId: "10",
        allowedIds: ["10", "12"],
        save: async () => {
          throw httpError(status);
        },
        reload: async () => props("12", "Enterprise"),
      });
      expect(result).toEqual({ status: "http", http: status });
    }
  });

  it("does not call the server for a page", async () => {
    const loadProps = vi.fn();
    const loaded = await loadSetFolderCommunityCatalog({
      item: page,
      selectedCount: 1,
      loadProps,
      loadCatalog: async () => ({ choices: [{ id: "10", name: "Default" }] }),
    });
    expect(loaded).toMatchObject({ status: "blocked", reason: "page" });
    expect(loadProps).not.toHaveBeenCalled();
  });

  it("reads a wrapped catalog and a numeric community id", () => {
    expect(
      unwrapFolderCommunityCatalog({
        FolderCommunityCatalog: {
          choices: [
            { id: 10, name: "Default" },
            { id: "12", name: "Enterprise" },
          ],
        },
      }).choices,
    ).toEqual([
      { id: "10", name: "Default" },
      { id: "12", name: "Enterprise" },
    ]);
    expect(folderCommunityIdText(props("-1"))).toBe("");
    expect(
      readReloadedCommunityId({
        FolderProperties: { id: "1", name: "CI", communityId: 12, communityName: "Enterprise" },
      }),
    ).toBe("12");
  });
});
