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
  loadSetFolderDisplayFormatCatalog,
  readReloadedDisplayFormat,
  saveSetFolderDisplayFormat,
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
