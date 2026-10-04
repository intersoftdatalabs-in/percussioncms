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
import { unwrapFolderWorkflowCatalog } from "../../../main/ts/api/contentExplorer/folderWorkflowApi";
import type { PSFolderProperties, PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  classifySetFolderWorkflowSelection,
  describeSetFolderWorkflowMultiSave,
  folderWorkflowIdText,
  loadSetFolderWorkflowCatalog,
  loadSetFolderWorkflowMultiCatalog,
  planSetFolderWorkflowMulti,
  readReloadedWorkflowId,
  saveSetFolderWorkflow,
  saveSetFolderWorkflowOnSelection,
} from "../../../main/ts/contentExplorer/setFolderWorkflow";

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

const props = (workflowId: string): PSFolderProperties => ({
  id: folder.id ?? "",
  name: "CI",
  workflowId,
  permission: { accessLevel: "ADMIN" },
});

describe("set folder workflow (#5104)", () => {
  it("blocks empty, pages, assets, and multi-select before any save", () => {
    expect(classifySetFolderWorkflowSelection({ item: null, selectedCount: 0 })).toMatchObject({
      status: "blocked",
      reason: "empty",
    });
    expect(classifySetFolderWorkflowSelection({ item: page, selectedCount: 1 })).toMatchObject({
      status: "blocked",
      reason: "page",
      name: "Home",
    });
    expect(classifySetFolderWorkflowSelection({ item: asset, selectedCount: 1 })).toMatchObject({
      status: "blocked",
      reason: "asset",
      name: "Logo",
    });
    expect(classifySetFolderWorkflowSelection({ item: folder, selectedCount: 2 })).toMatchObject({
      reason: "multi",
    });
    expect(classifySetFolderWorkflowSelection({ item: folder, selectedCount: 1 })).toMatchObject({
      status: "ready",
      folderId: folder.id,
    });
  });

  it("loads the catalog and does not save on cancel-sized gates", async () => {
    const save = vi.fn();
    const loaded = await loadSetFolderWorkflowCatalog({
      item: folder,
      selectedCount: 1,
      loadProps: async () => props("4"),
      loadCatalog: async () => ({
        choices: [
          { id: "4", name: "Simple" },
          { id: "7", name: "Local" },
        ],
      }),
    });
    expect(loaded.status).toBe("ready");
    if (loaded.status !== "ready") {
      return;
    }
    const unchanged = await saveSetFolderWorkflow({
      folderId: loaded.folderId,
      props: loaded.props,
      selectedId: "4",
      currentId: loaded.currentId,
      allowedIds: loaded.choices.map((row) => row.id),
      save,
    });
    expect(unchanged).toEqual({ status: "gate", reason: "unchanged" });
    expect(save).not.toHaveBeenCalled();
  });

  it("saves only when refresh shows the new workflow id", async () => {
    const save = vi.fn(async () => undefined);
    const reload = vi.fn(async () => props("7"));
    const saved = await saveSetFolderWorkflow({
      folderId: "16777215-101-703",
      props: props("4"),
      selectedId: "7",
      currentId: "4",
      allowedIds: ["4", "7"],
      save,
      reload,
    });
    expect(saved).toEqual({ status: "saved", workflowId: "7" });
    expect(save).toHaveBeenCalledTimes(1);
    const posted = save.mock.calls[0][0] as PSFolderProperties;
    expect(posted.workflowId).toBe("7");
    expect(posted.id).toBe("16777215-101-703");
    expect(posted.name).toBe("CI");
  });

  it("does not claim success when refresh still has the old workflow", async () => {
    const result = await saveSetFolderWorkflow({
      folderId: "16777215-101-703",
      props: props("4"),
      selectedId: "7",
      currentId: "4",
      allowedIds: ["4", "7"],
      save: async () => undefined,
      reload: async () => props("4"),
    });
    expect(result).toEqual({ status: "mismatch" });
  });

  it("HTTP 400, 403, and 409 are not a saved workflow", async () => {
    for (const status of [400, 403, 409] as const) {
      const result = await saveSetFolderWorkflow({
        folderId: "16777215-101-703",
        props: props("4"),
        selectedId: "7",
        currentId: "4",
        allowedIds: ["4", "7"],
        save: async () => {
          throw httpError(status);
        },
        reload: async () => props("7"),
      });
      expect(result).toEqual({ status: "http", http: status });
    }
  });

  it("does not call the server for a page", async () => {
    const loadProps = vi.fn();
    const loaded = await loadSetFolderWorkflowCatalog({
      item: page,
      selectedCount: 1,
      loadProps,
      loadCatalog: async () => ({ choices: [{ id: "4", name: "Simple" }] }),
    });
    expect(loaded).toMatchObject({ status: "blocked", reason: "page" });
    expect(loadProps).not.toHaveBeenCalled();
  });

  it("reads a wrapped catalog and a numeric workflow id", () => {
    expect(
      unwrapFolderWorkflowCatalog({
        FolderWorkflowCatalog: {
          choices: [
            { id: 4, name: "Simple" },
            { id: "7", name: "Local" },
          ],
        },
      }).choices,
    ).toEqual([
      { id: "4", name: "Simple" },
      { id: "7", name: "Local" },
    ]);
    expect(folderWorkflowIdText(props("-1"))).toBe("");
    expect(
      readReloadedWorkflowId({
        FolderProperties: { id: "1", name: "CI", workflowId: 7 },
      }),
    ).toBe("7");
  });
});

const blog: PSPathItem = {
  id: "16777215-101-704",
  name: "Blog",
  path: "/Sites/Blog/",
  type: "folder",
  category: "folder",
  leaf: false,
};

const catalogChoices = [
  { id: "4", name: "Simple" },
  { id: "7", name: "Local" },
];

function propsFor(id: string, name: string, workflowId: string): PSFolderProperties {
  return {
    id,
    name,
    workflowId,
    permission: { accessLevel: "ADMIN" },
  };
}

describe("set workflow on multi-selected folders (#5179)", () => {
  it("still blocks multi on the single-folder classifier", () => {
    expect(classifySetFolderWorkflowSelection({ item: folder, selectedCount: 2 })).toMatchObject({
      status: "blocked",
      reason: "multi",
    });
  });

  it("plans each folder once and skips pages and assets", () => {
    const plan = planSetFolderWorkflowMulti([folder, page, blog, asset, folder]);
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
    expect(planSetFolderWorkflowMulti([page, page])).toMatchObject({
      status: "blocked",
      reason: "page",
      name: "Home",
    });
    expect(planSetFolderWorkflowMulti([asset])).toMatchObject({
      status: "blocked",
      reason: "asset",
      name: "Logo",
    });
    expect(planSetFolderWorkflowMulti([])).toMatchObject({
      status: "blocked",
      reason: "empty",
    });
  });

  it("does not open a save when the shared catalog is HTTP 403", async () => {
    const save = vi.fn();
    const loaded = await loadSetFolderWorkflowMultiCatalog({
      items: [folder, blog, page],
      loadProps: async () => propsFor(String(folder.id), "CI", "4"),
      loadCatalog: vi.fn().mockRejectedValue(httpError(403)),
    });
    expect(loaded).toEqual({ status: "http", http: 403 });
    expect(save).not.toHaveBeenCalled();
  });

  it("shows each workflow only after that folder refresh and does not claim success on HTTP 409", async () => {
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
    const pending = saveSetFolderWorkflowOnSelection({
      targets: [
        { folderId: String(folder.id), name: "CI" },
        { folderId: String(blog.id), name: "Blog" },
      ],
      skippedPageNames: ["Home"],
      skippedAssetNames: ["Logo"],
      selectedId: "7",
      allowedIds: ["4", "7"],
      workflowName: "Local",
      loadProps: async (id) => {
        order.push(`get:${id}`);
        return propsFor(id, id === blog.id ? "Blog" : "CI", "4");
      },
      save,
      reload: async (id) => propsFor(id, id === blog.id ? "Blog" : "CI", "7"),
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
    expect(result.workflowId).toBe("");
    expect(result.saved.map((row) => row.folderId)).toEqual([folder.id]);
    expect(result.failures).toEqual([{ folderId: blog.id, name: "Blog", http: 409 }]);
    expect(order.filter((step) => step.startsWith("shown:"))).toEqual([`shown:${folder.id}`]);
    const described = describeSetFolderWorkflowMultiSave(result, "Local");
    expect(described.kind).toBe("error");
    expect(described.reason).toBe("partial");
    expect(described.workflowId).toBe("");
    expect(described.workflowName).toBe("");
    expect(described.text).toContain("Not every selected folder had its workflow set");
    expect(described.text).toContain("Blog (HTTP 409)");
    expect(described.text).toContain("Pages are not given a folder workflow: Home");
    expect(described.text).toContain("Assets are not given a folder workflow: Logo");
    expect(described.text).not.toContain("Folder workflow saved");
  });

  it("claims success only after every folder refresh shows the new workflow", async () => {
    const save = vi.fn(async () => undefined);
    const shown: string[] = [];
    const result = await saveSetFolderWorkflowOnSelection({
      targets: [
        { folderId: "101", name: "News" },
        { folderId: "102", name: "Blog" },
      ],
      skippedPageNames: ["Home"],
      selectedId: "7",
      allowedIds: ["4", "7"],
      workflowName: "Local",
      loadProps: async (id) => propsFor(id, id, "4"),
      save,
      reload: async (id) => propsFor(id, id, "7"),
      onFolderSaved: (row) => shown.push(row.folderId),
    });
    expect(save).toHaveBeenCalledTimes(2);
    expect(shown).toEqual(["101", "102"]);
    expect(result.status).toBe("saved");
    const described = describeSetFolderWorkflowMultiSave(result, "Local");
    expect(described).toMatchObject({
      kind: "success",
      reason: "items-skipped",
      workflowId: "7",
      workflowName: "Local",
    });
    expect(described.text).toContain("Folder workflow saved Local");
    expect(described.text).toContain("Home");
  });

  it("does not show a workflow when refresh still has the old id", async () => {
    const shown = vi.fn();
    const result = await saveSetFolderWorkflowOnSelection({
      targets: [{ folderId: "101", name: "News" }],
      selectedId: "7",
      allowedIds: ["4", "7"],
      workflowName: "Local",
      loadProps: async (id) => propsFor(id, "News", "4"),
      save: async () => undefined,
      reload: async (id) => propsFor(id, "News", "4"),
      onFolderSaved: shown,
    });
    expect(result.status).toBe("failed");
    expect(result.failures).toEqual([{ folderId: "101", name: "News", http: "mismatch" }]);
    expect(shown).not.toHaveBeenCalled();
    expect(describeSetFolderWorkflowMultiSave(result, "Local").text).not.toContain(
      "Folder workflow saved",
    );
  });

  it.each([400, 403, 409] as const)(
    "HTTP %s on one folder is not a full-selection success",
    async (status) => {
      const shown = vi.fn();
      const result = await saveSetFolderWorkflowOnSelection({
        targets: [
          { folderId: "101", name: "News" },
          { folderId: "102", name: "Blog" },
        ],
        selectedId: "7",
        allowedIds: ["4", "7"],
        workflowName: "Local",
        loadProps: async (id) => propsFor(id, id, "4"),
        save: async (posted) => {
          if (posted.id === "102") {
            throw httpError(status);
          }
        },
        reload: async (id) => propsFor(id, id, "7"),
        onFolderSaved: shown,
      });
      expect(result.status).toBe("partial");
      expect(result.saved.map((row) => row.folderId)).toEqual(["101"]);
      expect(result.failures).toEqual([{ folderId: "102", name: "Blog", http: status }]);
      expect(shown).toHaveBeenCalledTimes(1);
      expect(describeSetFolderWorkflowMultiSave(result, "Local").text).not.toContain(
        "Folder workflow saved",
      );
    },
  );

  it("does not post when the workflow is already on every folder", async () => {
    const save = vi.fn();
    const shown = vi.fn();
    const result = await saveSetFolderWorkflowOnSelection({
      targets: [{ folderId: "101", name: "News" }],
      selectedId: "7",
      allowedIds: ["4", "7"],
      workflowName: "Local",
      loadProps: async (id) => propsFor(id, "News", "7"),
      save,
      reload: async (id) => propsFor(id, "News", "4"),
      onFolderSaved: shown,
    });
    expect(save).not.toHaveBeenCalled();
    expect(shown).not.toHaveBeenCalled();
    expect(result.status).toBe("unchanged");
    const described = describeSetFolderWorkflowMultiSave(result, "Local");
    expect(described.kind).toBe("error");
    expect(described.workflowId).toBe("");
    expect(described.text).not.toContain("Folder workflow saved");
  });

  it("does not load properties for a pages-only selection", async () => {
    const loadProps = vi.fn();
    const loaded = await loadSetFolderWorkflowMultiCatalog({
      items: [page, asset],
      loadProps,
      loadCatalog: async () => ({ choices: catalogChoices }),
    });
    expect(loaded).toMatchObject({ status: "blocked", reason: "page" });
    expect(loadProps).not.toHaveBeenCalled();
  });
});
