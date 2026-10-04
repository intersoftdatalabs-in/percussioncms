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
  describeSetFolderCommunityMultiSave,
  folderCommunityIdText,
  loadSetFolderCommunityCatalog,
  loadSetFolderCommunityMultiCatalog,
  planSetFolderCommunityMulti,
  readReloadedCommunityId,
  saveSetFolderCommunity,
  saveSetFolderCommunityOnSelection,
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

const blog: PSPathItem = {
  id: "16777215-101-704",
  name: "Blog",
  path: "/Sites/CI/Blog/",
  type: "folder",
  category: "folder",
  leaf: false,
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

const catalogChoices = [
  { id: "10", name: "Default" },
  { id: "12", name: "Enterprise" },
];

function propsFor(id: string, name: string, communityId: string, communityName = ""): PSFolderProperties {
  return {
    id,
    name,
    communityId,
    communityName,
    permission: { accessLevel: "ADMIN" },
  };
}

describe("set community on multi-selected folders (#5156)", () => {
  it("still blocks multi on the single-folder classifier", () => {
    expect(classifySetFolderCommunitySelection({ item: folder, selectedCount: 2 })).toMatchObject({
      status: "blocked",
      reason: "multi",
    });
  });

  it("plans each folder once and skips pages and assets", () => {
    const plan = planSetFolderCommunityMulti([folder, page, blog, asset, folder]);
    expect(plan).toEqual({
      status: "ready",
      targets: [
        { folderId: folder.id, name: "CI" },
        { folderId: blog.id, name: "Blog" },
      ],
      skippedPageNames: ["Home"],
      skippedAssetNames: ["Logo"],
      skippedOtherNames: [],
      noIdNames: [],
    });
    expect(planSetFolderCommunityMulti([page, page])).toMatchObject({
      status: "blocked",
      reason: "page",
      name: "Home",
    });
    expect(planSetFolderCommunityMulti([asset])).toMatchObject({
      status: "blocked",
      reason: "asset",
      name: "Logo",
    });
    expect(planSetFolderCommunityMulti([])).toMatchObject({
      status: "blocked",
      reason: "empty",
    });
  });

  it("does not open a save when the shared catalog is HTTP 403", async () => {
    const save = vi.fn();
    const loaded = await loadSetFolderCommunityMultiCatalog({
      items: [folder, blog, page],
      loadProps: async () => propsFor(String(folder.id), "CI", "10", "Default"),
      loadCatalog: vi.fn().mockRejectedValue(httpError(403)),
    });
    expect(loaded).toEqual({ status: "http", http: 403 });
    expect(save).not.toHaveBeenCalled();
  });

  it("shows each community only after that folder refresh and does not claim success on HTTP 409", async () => {
    const order: string[] = [];
    let releaseBlog: (value?: unknown) => void = () => undefined;
    const blogGate = new Promise((resolve) => {
      releaseBlog = resolve;
    });
    const save = vi.fn(async (posted: PSFolderProperties) => {
      order.push(`post:${posted.id}`);
      if (posted.id === blog.id) {
        await blogGate;
        throw httpError(409);
      }
    });
    const pending = saveSetFolderCommunityOnSelection({
      targets: [
        { folderId: String(folder.id), name: "CI" },
        { folderId: String(blog.id), name: "Blog" },
      ],
      skippedPageNames: ["Home"],
      skippedAssetNames: ["Logo"],
      selectedId: "12",
      allowedIds: ["10", "12"],
      communityName: "Enterprise",
      loadProps: async (id) => {
        order.push(`get:${id}`);
        return propsFor(id, id === blog.id ? "Blog" : "CI", "10", "Default");
      },
      save,
      reload: async (id) => propsFor(id, id === blog.id ? "Blog" : "CI", "12", "Enterprise"),
      onFolderSaved: (row) => {
        order.push(`shown:${row.folderId}`);
      },
    });
    await vi.waitFor(() => expect(order).toContain(`post:${blog.id}`));
    expect(order).toEqual([
      `get:${folder.id}`,
      `post:${folder.id}`,
      `shown:${folder.id}`,
      `get:${blog.id}`,
      `post:${blog.id}`,
    ]);
    releaseBlog();
    const result = await pending;
    expect(result.status).toBe("partial");
    expect(result.communityId).toBe("");
    expect(result.saved.map((row) => row.folderId)).toEqual([folder.id]);
    expect(result.failures).toEqual([{ folderId: blog.id, name: "Blog", http: 409 }]);
    expect(order.filter((step) => step.startsWith("shown:"))).toEqual([`shown:${folder.id}`]);
    const described = describeSetFolderCommunityMultiSave(result, "Enterprise");
    expect(described.kind).toBe("error");
    expect(described.reason).toBe("partial");
    expect(described.communityId).toBe("");
    expect(described.communityName).toBe("");
    expect(described.text).toContain("Not every selected folder had its community set");
    expect(described.text).toContain("Blog (HTTP 409)");
    expect(described.text).toContain("Pages are not given a folder community: Home");
    expect(described.text).toContain("Assets are not given a folder community: Logo");
    expect(described.text).not.toContain("Folder community saved");
  });

  it("claims success only after every folder refresh shows the new community", async () => {
    const save = vi.fn(async () => undefined);
    const shown: string[] = [];
    const result = await saveSetFolderCommunityOnSelection({
      targets: [
        { folderId: "101", name: "News" },
        { folderId: "102", name: "Blog" },
      ],
      skippedPageNames: ["Home"],
      selectedId: "12",
      allowedIds: ["10", "12"],
      communityName: "Enterprise",
      loadProps: async (id) => propsFor(id, id, "10", "Default"),
      save,
      reload: async (id) => propsFor(id, id, "12", "Enterprise"),
      onFolderSaved: (row) => shown.push(row.folderId),
    });
    expect(save).toHaveBeenCalledTimes(2);
    expect(shown).toEqual(["101", "102"]);
    expect(result.status).toBe("saved");
    const described = describeSetFolderCommunityMultiSave(result, "Enterprise");
    expect(described).toMatchObject({
      kind: "success",
      reason: "items-skipped",
      communityId: "12",
      communityName: "Enterprise",
    });
    expect(described.text).toContain("Folder community saved Enterprise");
    expect(described.text).toContain("Home");
  });

  it("does not show a community when refresh still has the old id", async () => {
    const shown = vi.fn();
    const result = await saveSetFolderCommunityOnSelection({
      targets: [{ folderId: "101", name: "News" }],
      selectedId: "12",
      allowedIds: ["10", "12"],
      communityName: "Enterprise",
      loadProps: async (id) => propsFor(id, "News", "10", "Default"),
      save: async () => undefined,
      reload: async (id) => propsFor(id, "News", "10", "Default"),
      onFolderSaved: shown,
    });
    expect(result.status).toBe("failed");
    expect(result.failures).toEqual([{ folderId: "101", name: "News", http: "mismatch" }]);
    expect(shown).not.toHaveBeenCalled();
    expect(describeSetFolderCommunityMultiSave(result, "Enterprise").text).not.toContain(
      "Folder community saved",
    );
  });

  it.each([400, 403, 409] as const)(
    "HTTP %s on one folder is not a full-selection success",
    async (status) => {
      const shown = vi.fn();
      const result = await saveSetFolderCommunityOnSelection({
        targets: [
          { folderId: "101", name: "News" },
          { folderId: "102", name: "Blog" },
        ],
        selectedId: "12",
        allowedIds: ["10", "12"],
        communityName: "Enterprise",
        loadProps: async (id) => propsFor(id, id, "10", "Default"),
        save: async (posted) => {
          if (posted.id === "102") {
            throw httpError(status);
          }
        },
        reload: async (id) => propsFor(id, id, "12", "Enterprise"),
        onFolderSaved: shown,
      });
      expect(result.status).toBe("partial");
      expect(result.saved.map((row) => row.folderId)).toEqual(["101"]);
      expect(result.failures).toEqual([{ folderId: "102", name: "Blog", http: status }]);
      expect(shown).toHaveBeenCalledTimes(1);
      expect(describeSetFolderCommunityMultiSave(result, "Enterprise").text).not.toContain(
        "Folder community saved",
      );
    },
  );

  it("does not post when the community is already on every folder", async () => {
    const save = vi.fn();
    const shown = vi.fn();
    const result = await saveSetFolderCommunityOnSelection({
      targets: [{ folderId: "101", name: "News" }],
      selectedId: "10",
      allowedIds: ["10", "12"],
      communityName: "Default",
      loadProps: async (id) => propsFor(id, "News", "10", "Default"),
      save,
      reload: async (id) => propsFor(id, "News", "12", "Enterprise"),
      onFolderSaved: shown,
    });
    expect(save).not.toHaveBeenCalled();
    expect(shown).not.toHaveBeenCalled();
    expect(result.status).toBe("unchanged");
    const described = describeSetFolderCommunityMultiSave(result, "Default");
    expect(described.kind).toBe("error");
    expect(described.communityId).toBe("");
    expect(described.text).not.toContain("Folder community saved");
  });

  it("does not load properties for a pages-only selection", async () => {
    const loadProps = vi.fn();
    const loaded = await loadSetFolderCommunityMultiCatalog({
      items: [page, asset],
      loadProps,
      loadCatalog: async () => ({ choices: catalogChoices }),
    });
    expect(loaded).toMatchObject({ status: "blocked", reason: "page" });
    expect(loadProps).not.toHaveBeenCalled();
  });
});
