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
  folderWorkflowIdText,
  loadSetFolderWorkflowCatalog,
  readReloadedWorkflowId,
  saveSetFolderWorkflow,
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
