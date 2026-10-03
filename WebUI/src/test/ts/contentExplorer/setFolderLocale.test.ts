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
import { unwrapFolderLocaleCatalog } from "../../../main/ts/api/contentExplorer/folderLocaleApi";
import type { PSFolderProperties, PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  classifySetFolderLocaleSelection,
  folderLocaleText,
  loadSetFolderLocaleCatalog,
  readReloadedLocale,
  saveSetFolderLocale,
} from "../../../main/ts/contentExplorer/setFolderLocale";

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

const props = (locale: string): PSFolderProperties => ({
  id: folder.id ?? "",
  name: "CI",
  locale,
  permission: { accessLevel: "ADMIN" },
});

describe("set folder locale (#5106)", () => {
  it("blocks empty, pages, assets, and multi-select before any save", () => {
    expect(classifySetFolderLocaleSelection({ item: null, selectedCount: 0 })).toMatchObject({
      status: "blocked",
      reason: "empty",
    });
    expect(classifySetFolderLocaleSelection({ item: page, selectedCount: 1 })).toMatchObject({
      status: "blocked",
      reason: "page",
      name: "Home",
    });
    expect(classifySetFolderLocaleSelection({ item: asset, selectedCount: 1 })).toMatchObject({
      status: "blocked",
      reason: "asset",
      name: "Logo",
    });
    expect(classifySetFolderLocaleSelection({ item: folder, selectedCount: 2 })).toMatchObject({
      reason: "multi",
    });
    expect(classifySetFolderLocaleSelection({ item: folder, selectedCount: 1 })).toMatchObject({
      status: "ready",
      folderId: folder.id,
    });
  });

  it("loads the catalog and does not save the current locale again", async () => {
    const save = vi.fn();
    const loaded = await loadSetFolderLocaleCatalog({
      item: folder,
      selectedCount: 1,
      loadProps: async () => props("en-us"),
      loadCatalog: async () => ({
        choices: [
          { code: "en-us", name: "English" },
          { code: "fr-fr", name: "French" },
        ],
      }),
    });
    expect(loaded.status).toBe("ready");
    if (loaded.status !== "ready") {
      return;
    }
    const unchanged = await saveSetFolderLocale({
      folderId: loaded.folderId,
      props: loaded.props,
      selectedCode: "EN-US",
      currentCode: loaded.currentCode,
      allowedCodes: loaded.choices.map((row) => row.code),
      localeName: "English",
      save,
    });
    expect(unchanged).toEqual({ status: "gate", reason: "unchanged" });
    expect(save).not.toHaveBeenCalled();
  });

  it("saves only when refresh shows the new locale", async () => {
    const save = vi.fn(async () => undefined);
    const reload = vi.fn(async () => props("fr-fr"));
    const saved = await saveSetFolderLocale({
      folderId: "16777215-101-703",
      props: props("en-us"),
      selectedCode: "fr-fr",
      currentCode: "en-us",
      allowedCodes: ["en-us", "fr-fr"],
      localeName: "French",
      save,
      reload,
    });
    expect(saved).toEqual({
      status: "saved",
      localeCode: "fr-fr",
      localeName: "French",
    });
    expect(save).toHaveBeenCalledTimes(1);
    const posted = save.mock.calls[0][0] as PSFolderProperties;
    expect(posted.locale).toBe("fr-fr");
    expect(posted.id).toBe("16777215-101-703");
    expect(posted.name).toBe("CI");
  });

  it("does not claim success when refresh still has the old locale", async () => {
    const result = await saveSetFolderLocale({
      folderId: "16777215-101-703",
      props: props("en-us"),
      selectedCode: "fr-fr",
      currentCode: "en-us",
      allowedCodes: ["en-us", "fr-fr"],
      localeName: "French",
      save: async () => undefined,
      reload: async () => props("en-us"),
    });
    expect(result).toEqual({ status: "mismatch" });
  });

  it("HTTP 400, 403, and 409 are not a saved locale", async () => {
    for (const status of [400, 403, 409] as const) {
      const result = await saveSetFolderLocale({
        folderId: "16777215-101-703",
        props: props("en-us"),
        selectedCode: "fr-fr",
        currentCode: "en-us",
        allowedCodes: ["en-us", "fr-fr"],
        save: async () => {
          throw httpError(status);
        },
        reload: async () => props("fr-fr"),
      });
      expect(result).toEqual({ status: "http", http: status });
    }
  });

  it("does not call the server for a page", async () => {
    const loadProps = vi.fn();
    const loaded = await loadSetFolderLocaleCatalog({
      item: page,
      selectedCount: 1,
      loadProps,
      loadCatalog: async () => ({ choices: [{ code: "en-us", name: "English" }] }),
    });
    expect(loaded).toMatchObject({ status: "blocked", reason: "page" });
    expect(loadProps).not.toHaveBeenCalled();
  });

  it("rejects a code that is not in the catalog without saving", async () => {
    const save = vi.fn();
    const result = await saveSetFolderLocale({
      folderId: "16777215-101-703",
      props: props("en-us"),
      selectedCode: "zz-zz",
      currentCode: "en-us",
      allowedCodes: ["en-us", "fr-fr"],
      save,
    });
    expect(result).toEqual({ status: "gate", reason: "forbidden" });
    expect(save).not.toHaveBeenCalled();
  });

  it("reads a wrapped catalog and a refreshed locale", () => {
    expect(
      unwrapFolderLocaleCatalog({
        FolderLocaleCatalog: {
          choices: [
            { code: "en-us", name: "English" },
            { code: "fr-fr", name: "French" },
          ],
        },
      }).choices,
    ).toEqual([
      { code: "en-us", name: "English" },
      { code: "fr-fr", name: "French" },
    ]);
    expect(folderLocaleText(props("  "))).toBe("");
    expect(
      readReloadedLocale({
        FolderProperties: { id: "1", name: "CI", locale: "fr-fr" },
      }),
    ).toBe("fr-fr");
  });
});
