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
import { unwrapFolderDisplayFormatCatalog } from "../../../main/ts/api/contentExplorer/folderDisplayFormatApi";
import type { PSFolderProperties, PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  classifySetFolderDisplayFormatSelection,
  describeSetFolderDisplayFormatMultiSave,
  loadSetFolderDisplayFormatCatalog,
  loadSetFolderDisplayFormatMultiCatalog,
  planSetFolderDisplayFormatMulti,
  readReloadedDisplayFormat,
  saveSetFolderDisplayFormat,
  saveSetFolderDisplayFormatOnSelection,
} from "../../../main/ts/contentExplorer/setFolderDisplayFormat";

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

const props = (id: string, name: string): PSFolderProperties => ({
  id: folder.id ?? "",
  name: "CI",
  displayFormatId: id,
  displayFormatName: name,
  permission: { accessLevel: "ADMIN" },
});

const catalog = async () => ({
  choices: [
    { id: "3", name: "Default" },
    { id: "12", name: "Simple" },
  ],
});

describe("set folder display format (#5131)", () => {
  it("blocks empty, pages, assets, and multi-select before any save", () => {
    expect(
      classifySetFolderDisplayFormatSelection({ item: null, selectedCount: 0 }),
    ).toMatchObject({ status: "blocked", reason: "empty" });
    expect(
      classifySetFolderDisplayFormatSelection({ item: page, selectedCount: 1 }),
    ).toMatchObject({ status: "blocked", reason: "page", name: "Home" });
    expect(
      classifySetFolderDisplayFormatSelection({ item: asset, selectedCount: 1 }),
    ).toMatchObject({ status: "blocked", reason: "asset", name: "Logo" });
    expect(
      classifySetFolderDisplayFormatSelection({ item: folder, selectedCount: 2 }),
    ).toMatchObject({ reason: "multi" });
    expect(
      classifySetFolderDisplayFormatSelection({ item: folder, selectedCount: 1 }),
    ).toMatchObject({ status: "ready", folderId: folder.id });
  });

  it("does not call the server for a page, an asset, or an empty selection", async () => {
    const loadProps = vi.fn();
    const loadCatalog = vi.fn(catalog);
    for (const item of [null, page, asset]) {
      const loaded = await loadSetFolderDisplayFormatCatalog({
        item,
        selectedCount: item ? 1 : 0,
        loadProps,
        loadCatalog,
      });
      expect(loaded.status).toBe("blocked");
    }
    const multi = await loadSetFolderDisplayFormatCatalog({
      item: folder,
      selectedCount: 2,
      loadProps,
      loadCatalog,
    });
    expect(multi).toMatchObject({ status: "blocked", reason: "multi" });
    expect(loadProps).not.toHaveBeenCalled();
    expect(loadCatalog).not.toHaveBeenCalled();
  });

  it("does not save the current format or a name that is not an id", async () => {
    const save = vi.fn();
    const loaded = await loadSetFolderDisplayFormatCatalog({
      item: folder,
      selectedCount: 1,
      loadProps: async () => props("3", "Default"),
      loadCatalog: catalog,
    });
    expect(loaded.status).toBe("ready");
    if (loaded.status !== "ready") {
      return;
    }
    const unchanged = await saveSetFolderDisplayFormat({
      folderId: loaded.folderId,
      props: loaded.props,
      selectedId: "03",
      currentId: loaded.currentId,
      allowedIds: loaded.choices.map((row) => row.id),
      displayFormatName: "Default",
      save,
    });
    expect(unchanged).toEqual({ status: "gate", reason: "unchanged" });
    const nameOnly = await saveSetFolderDisplayFormat({
      folderId: loaded.folderId,
      props: loaded.props,
      selectedId: "Simple",
      currentId: loaded.currentId,
      allowedIds: loaded.choices.map((row) => row.id),
      displayFormatName: "Simple",
      save,
    });
    expect(nameOnly).toEqual({ status: "gate", reason: "blank" });
    expect(save).not.toHaveBeenCalled();
  });

  it("saves the id and claims success only when refresh resolves the catalog name", async () => {
    const save = vi.fn(async () => undefined);
    const saved = await saveSetFolderDisplayFormat({
      folderId: "16777215-101-703",
      props: props("3", "Default"),
      selectedId: "12",
      currentId: "3",
      allowedIds: ["3", "12"],
      displayFormatName: "Simple",
      save,
      reload: async () => props("12", "Simple"),
    });
    expect(saved).toEqual({
      status: "saved",
      displayFormatId: "12",
      displayFormatName: "Simple",
    });
    const posted = save.mock.calls[0][0] as PSFolderProperties;
    expect(posted.displayFormatId).toBe("12");
    expect(posted.displayFormatName).toBe("Default");
    expect(posted.id).toBe("16777215-101-703");
  });

  it("does not claim success when refresh still has the old id or the old name", async () => {
    const staleId = await saveSetFolderDisplayFormat({
      folderId: "16777215-101-703",
      props: props("3", "Default"),
      selectedId: "12",
      currentId: "3",
      allowedIds: ["3", "12"],
      displayFormatName: "Simple",
      save: async () => undefined,
      reload: async () => props("3", "Simple"),
    });
    expect(staleId).toEqual({ status: "mismatch" });
    const nameOnlyEcho = await saveSetFolderDisplayFormat({
      folderId: "16777215-101-703",
      props: props("3", "Default"),
      selectedId: "12",
      currentId: "3",
      allowedIds: ["3", "12"],
      displayFormatName: "Simple",
      save: async () => undefined,
      reload: async () => props("12", "Default"),
    });
    expect(nameOnlyEcho).toEqual({ status: "mismatch" });
  });

  it("HTTP 400, 403, and 409 are not a saved display format", async () => {
    for (const status of [400, 403, 409] as const) {
      const result = await saveSetFolderDisplayFormat({
        folderId: "16777215-101-703",
        props: props("3", "Default"),
        selectedId: "12",
        currentId: "3",
        allowedIds: ["3", "12"],
        displayFormatName: "Simple",
        save: async () => {
          throw httpError(status);
        },
        reload: async () => props("12", "Simple"),
      });
      expect(result).toEqual({ status: "http", http: status });
    }
  });

  it("rejects an id that is not in the catalog without saving", async () => {
    const save = vi.fn();
    const result = await saveSetFolderDisplayFormat({
      folderId: "16777215-101-703",
      props: props("3", "Default"),
      selectedId: "99",
      currentId: "3",
      allowedIds: ["3", "12"],
      save,
    });
    expect(result).toEqual({ status: "gate", reason: "forbidden" });
    expect(save).not.toHaveBeenCalled();
  });

  it("reads a wrapped catalog and a refreshed id plus name", () => {
    expect(
      unwrapFolderDisplayFormatCatalog({
        FolderDisplayFormatCatalog: {
          choices: [
            { id: "3", name: "Default" },
            { id: 12, name: "Simple" },
          ],
        },
      }).choices,
    ).toEqual([
      { id: "3", name: "Default" },
      { id: "12", name: "Simple" },
    ]);
    expect(
      readReloadedDisplayFormat({
        FolderProperties: {
          id: "1",
          name: "CI",
          displayFormatId: "012",
          displayFormatName: "Simple",
        },
      }),
    ).toEqual({ id: "12", name: "Simple" });
  });
});

function propsFor(
  id: string,
  name: string,
  displayFormatId: string,
  displayFormatName: string,
): PSFolderProperties {
  return {
    id,
    name,
    displayFormatId,
    displayFormatName,
    permission: { accessLevel: "ADMIN" },
  };
}

const catalogChoices = [
  { id: "3", name: "Default" },
  { id: "12", name: "Simple" },
];

describe("set display format on multi-selected folders (#5180)", () => {
  it("still blocks multi on the single-folder classifier", () => {
    expect(
      classifySetFolderDisplayFormatSelection({ item: folder, selectedCount: 2 }),
    ).toMatchObject({
      status: "blocked",
      reason: "multi",
    });
  });

  it("plans each folder once and skips pages and assets", () => {
    const plan = planSetFolderDisplayFormatMulti([folder, page, blog, asset, folder]);
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
    expect(planSetFolderDisplayFormatMulti([page, page])).toMatchObject({
      status: "blocked",
      reason: "page",
      name: "Home",
    });
    expect(planSetFolderDisplayFormatMulti([asset])).toMatchObject({
      status: "blocked",
      reason: "asset",
      name: "Logo",
    });
    expect(planSetFolderDisplayFormatMulti([])).toMatchObject({
      status: "blocked",
      reason: "empty",
    });
  });

  it("does not open a save when the shared catalog is HTTP 403", async () => {
    const save = vi.fn();
    const loaded = await loadSetFolderDisplayFormatMultiCatalog({
      items: [folder, blog, page],
      loadProps: async () => propsFor(String(folder.id), "CI", "3", "Default"),
      loadCatalog: vi.fn().mockRejectedValue(httpError(403)),
    });
    expect(loaded).toEqual({ status: "http", http: 403 });
    expect(save).not.toHaveBeenCalled();
  });

  it("shows each format name only after that folder refresh and does not claim success on HTTP 409", async () => {
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
    const pending = saveSetFolderDisplayFormatOnSelection({
      targets: [
        { folderId: String(folder.id), name: "CI" },
        { folderId: String(blog.id), name: "Blog" },
      ],
      skippedPageNames: ["Home"],
      skippedAssetNames: ["Logo"],
      selectedId: "12",
      allowedIds: ["3", "12"],
      displayFormatName: "Simple",
      loadProps: async (id) => {
        order.push(`get:${id}`);
        return propsFor(id, id === blog.id ? "Blog" : "CI", "3", "Default");
      },
      save,
      reload: async (id) => propsFor(id, id === blog.id ? "Blog" : "CI", "12", "Simple"),
      onFolderSaved: (row) => {
        order.push(`shown:${row.folderId}:${row.displayFormatName}`);
      },
    });
    await vi.waitFor(() => expect(order).toContain(`post:${blog.id}`));
    expect(order).toEqual([
      `get:${folder.id}`,
      `post:${folder.id}`,
      `shown:${folder.id}:Simple`,
      `get:${blog.id}`,
      `post:${blog.id}`,
    ]);
    releaseBlog();
    const result = await pending;
    expect(result.status).toBe("partial");
    expect(result.displayFormatId).toBe("");
    expect(result.saved.map((row) => row.folderId)).toEqual([folder.id]);
    expect(result.failures).toEqual([{ folderId: blog.id, name: "Blog", http: 409 }]);
    expect(order.filter((step) => step.startsWith("shown:"))).toEqual([
      `shown:${folder.id}:Simple`,
    ]);
    const described = describeSetFolderDisplayFormatMultiSave(result, "Simple");
    expect(described.kind).toBe("error");
    expect(described.reason).toBe("partial");
    expect(described.displayFormatId).toBe("");
    expect(described.displayFormatName).toBe("");
    expect(described.text).toContain("Not every selected folder had its display format set");
    expect(described.text).toContain("Blog (HTTP 409)");
    expect(described.text).toContain("Pages are not given a folder display format: Home");
    expect(described.text).toContain("Assets are not given a folder display format: Logo");
    expect(described.text).not.toContain("Folder display format saved");
  });

  it("claims success only after every folder refresh shows the id and catalog name", async () => {
    const save = vi.fn(async () => undefined);
    const shown: string[] = [];
    const result = await saveSetFolderDisplayFormatOnSelection({
      targets: [
        { folderId: "101", name: "News" },
        { folderId: "102", name: "Blog" },
      ],
      skippedPageNames: ["Home"],
      selectedId: "12",
      allowedIds: ["3", "12"],
      displayFormatName: "Simple",
      loadProps: async (id) => propsFor(id, id, "3", "Default"),
      save,
      reload: async (id) => propsFor(id, id, "12", "Simple"),
      onFolderSaved: (row) => shown.push(`${row.folderId}:${row.displayFormatName}`),
    });
    expect(save).toHaveBeenCalledTimes(2);
    expect(save.mock.calls[0][0].displayFormatId).toBe("12");
    expect(shown).toEqual(["101:Simple", "102:Simple"]);
    expect(result.status).toBe("saved");
    const described = describeSetFolderDisplayFormatMultiSave(result, "Simple");
    expect(described).toMatchObject({
      kind: "success",
      reason: "items-skipped",
      displayFormatId: "12",
      displayFormatName: "Simple",
    });
    expect(described.text).toContain("Folder display format saved Simple");
    expect(described.text).toContain("Home");
  });

  it("does not show a format name when refresh still has the old id", async () => {
    const shown = vi.fn();
    const result = await saveSetFolderDisplayFormatOnSelection({
      targets: [{ folderId: "101", name: "News" }],
      selectedId: "12",
      allowedIds: ["3", "12"],
      displayFormatName: "Simple",
      loadProps: async (id) => propsFor(id, "News", "3", "Default"),
      save: async () => undefined,
      reload: async (id) => propsFor(id, "News", "3", "Default"),
      onFolderSaved: shown,
    });
    expect(result.status).toBe("failed");
    expect(result.failures).toEqual([{ folderId: "101", name: "News", http: "mismatch" }]);
    expect(shown).not.toHaveBeenCalled();
    expect(describeSetFolderDisplayFormatMultiSave(result, "Simple").text).not.toContain(
      "Folder display format saved",
    );
  });

  it("does not show a format name when refresh has the id but not the catalog name", async () => {
    const shown = vi.fn();
    const result = await saveSetFolderDisplayFormatOnSelection({
      targets: [{ folderId: "101", name: "News" }],
      selectedId: "12",
      allowedIds: ["3", "12"],
      displayFormatName: "Simple",
      loadProps: async (id) => propsFor(id, "News", "3", "Default"),
      save: async () => undefined,
      reload: async (id) => propsFor(id, "News", "12", ""),
      onFolderSaved: shown,
    });
    expect(result.status).toBe("failed");
    expect(result.failures).toEqual([{ folderId: "101", name: "News", http: "mismatch" }]);
    expect(shown).not.toHaveBeenCalled();
  });

  it.each([400, 403, 409] as const)(
    "HTTP %s on one folder is not a full-selection success",
    async (status) => {
      const shown = vi.fn();
      const result = await saveSetFolderDisplayFormatOnSelection({
        targets: [
          { folderId: "101", name: "News" },
          { folderId: "102", name: "Blog" },
        ],
        selectedId: "12",
        allowedIds: ["3", "12"],
        displayFormatName: "Simple",
        loadProps: async (id) => propsFor(id, id, "3", "Default"),
        save: async (posted) => {
          if (posted.id === "102") {
            throw httpError(status);
          }
        },
        reload: async (id) => propsFor(id, id, "12", "Simple"),
        onFolderSaved: shown,
      });
      expect(result.status).toBe("partial");
      expect(result.saved.map((row) => row.folderId)).toEqual(["101"]);
      expect(result.failures).toEqual([{ folderId: "102", name: "Blog", http: status }]);
      expect(shown).toHaveBeenCalledTimes(1);
      expect(describeSetFolderDisplayFormatMultiSave(result, "Simple").text).not.toContain(
        "Folder display format saved",
      );
    },
  );

  it("does not post when the display format is already on every folder", async () => {
    const save = vi.fn();
    const shown = vi.fn();
    const result = await saveSetFolderDisplayFormatOnSelection({
      targets: [{ folderId: "101", name: "News" }],
      selectedId: "12",
      allowedIds: ["3", "12"],
      displayFormatName: "Simple",
      loadProps: async (id) => propsFor(id, "News", "12", "Simple"),
      save,
      reload: async (id) => propsFor(id, "News", "3", "Default"),
      onFolderSaved: shown,
    });
    expect(save).not.toHaveBeenCalled();
    expect(shown).not.toHaveBeenCalled();
    expect(result.status).toBe("unchanged");
    const described = describeSetFolderDisplayFormatMultiSave(result, "Simple");
    expect(described.kind).toBe("error");
    expect(described.displayFormatId).toBe("");
    expect(described.text).not.toContain("Folder display format saved");
  });

  it("does not load properties for a pages-only selection", async () => {
    const loadProps = vi.fn();
    const loaded = await loadSetFolderDisplayFormatMultiCatalog({
      items: [page, asset],
      loadProps,
      loadCatalog: async () => ({ choices: catalogChoices }),
    });
    expect(loaded).toMatchObject({ status: "blocked", reason: "page" });
    expect(loadProps).not.toHaveBeenCalled();
  });
});
