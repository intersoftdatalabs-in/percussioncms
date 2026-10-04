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
  describeSetFolderLocaleMultiSave,
  folderLocaleText,
  loadSetFolderLocaleCatalog,
  loadSetFolderLocaleMultiCatalog,
  planSetFolderLocaleMulti,
  readReloadedLocale,
  saveSetFolderLocale,
  saveSetFolderLocaleOnSelection,
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

const blog: PSPathItem = {
  id: "16777215-101-704",
  name: "Blog",
  path: "/Sites/Blog/",
  type: "folder",
  category: "folder",
  leaf: false,
};

const catalogChoices = [
  { code: "en-us", name: "English" },
  { code: "fr-fr", name: "French" },
];

function propsFor(id: string, name: string, locale: string): PSFolderProperties {
  return {
    id,
    name,
    locale,
    permission: { accessLevel: "ADMIN" },
  };
}

describe("set locale on multi-selected folders (#5157)", () => {
  it("still blocks multi on the single-folder classifier", () => {
    expect(classifySetFolderLocaleSelection({ item: folder, selectedCount: 2 })).toMatchObject({
      status: "blocked",
      reason: "multi",
    });
  });

  it("plans each folder once and skips pages and assets", () => {
    const plan = planSetFolderLocaleMulti([folder, page, blog, asset, folder]);
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
    expect(planSetFolderLocaleMulti([page, page])).toMatchObject({
      status: "blocked",
      reason: "page",
      name: "Home",
    });
    expect(planSetFolderLocaleMulti([asset])).toMatchObject({
      status: "blocked",
      reason: "asset",
      name: "Logo",
    });
    expect(planSetFolderLocaleMulti([])).toMatchObject({
      status: "blocked",
      reason: "empty",
    });
  });

  it("does not open a save when the shared catalog is HTTP 403", async () => {
    const save = vi.fn();
    const loaded = await loadSetFolderLocaleMultiCatalog({
      items: [folder, blog, page],
      loadProps: async () => propsFor(String(folder.id), "CI", "en-us"),
      loadCatalog: vi.fn().mockRejectedValue(httpError(403)),
    });
    expect(loaded).toEqual({ status: "http", http: 403 });
    expect(save).not.toHaveBeenCalled();
  });

  it("shows each locale only after that folder refresh and does not claim success on HTTP 409", async () => {
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
    const pending = saveSetFolderLocaleOnSelection({
      targets: [
        { folderId: String(folder.id), name: "CI" },
        { folderId: String(blog.id), name: "Blog" },
      ],
      skippedPageNames: ["Home"],
      skippedAssetNames: ["Logo"],
      selectedCode: "fr-fr",
      allowedCodes: ["en-us", "fr-fr"],
      localeName: "French",
      loadProps: async (id) => {
        order.push(`get:${id}`);
        return propsFor(id, id === blog.id ? "Blog" : "CI", "en-us");
      },
      save,
      reload: async (id) => propsFor(id, id === blog.id ? "Blog" : "CI", "fr-fr"),
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
    expect(result.localeCode).toBe("");
    expect(result.saved.map((row) => row.folderId)).toEqual([folder.id]);
    expect(result.failures).toEqual([{ folderId: blog.id, name: "Blog", http: 409 }]);
    expect(order.filter((step) => step.startsWith("shown:"))).toEqual([`shown:${folder.id}`]);
    const described = describeSetFolderLocaleMultiSave(result, "French");
    expect(described.kind).toBe("error");
    expect(described.reason).toBe("partial");
    expect(described.localeCode).toBe("");
    expect(described.localeName).toBe("");
    expect(described.text).toContain("Not every selected folder had its locale set");
    expect(described.text).toContain("Blog (HTTP 409)");
    expect(described.text).toContain("Pages are not given a folder locale: Home");
    expect(described.text).toContain("Assets are not given a folder locale: Logo");
    expect(described.text).not.toContain("Folder locale saved");
  });

  it("claims success only after every folder refresh shows the new locale", async () => {
    const save = vi.fn(async () => undefined);
    const shown: string[] = [];
    const result = await saveSetFolderLocaleOnSelection({
      targets: [
        { folderId: "101", name: "News" },
        { folderId: "102", name: "Blog" },
      ],
      skippedPageNames: ["Home"],
      selectedCode: "fr-fr",
      allowedCodes: ["en-us", "fr-fr"],
      localeName: "French",
      loadProps: async (id) => propsFor(id, id, "en-us"),
      save,
      reload: async (id) => propsFor(id, id, "fr-fr"),
      onFolderSaved: (row) => shown.push(row.folderId),
    });
    expect(save).toHaveBeenCalledTimes(2);
    expect(shown).toEqual(["101", "102"]);
    expect(result.status).toBe("saved");
    const described = describeSetFolderLocaleMultiSave(result, "French");
    expect(described).toMatchObject({
      kind: "success",
      reason: "items-skipped",
      localeCode: "fr-fr",
      localeName: "French",
    });
    expect(described.text).toContain("Folder locale saved French");
    expect(described.text).toContain("Home");
  });

  it("does not show a locale when refresh still has the old code", async () => {
    const shown = vi.fn();
    const result = await saveSetFolderLocaleOnSelection({
      targets: [{ folderId: "101", name: "News" }],
      selectedCode: "fr-fr",
      allowedCodes: ["en-us", "fr-fr"],
      localeName: "French",
      loadProps: async (id) => propsFor(id, "News", "en-us"),
      save: async () => undefined,
      reload: async (id) => propsFor(id, "News", "en-us"),
      onFolderSaved: shown,
    });
    expect(result.status).toBe("failed");
    expect(result.failures).toEqual([{ folderId: "101", name: "News", http: "mismatch" }]);
    expect(shown).not.toHaveBeenCalled();
    expect(describeSetFolderLocaleMultiSave(result, "French").text).not.toContain(
      "Folder locale saved",
    );
  });

  it.each([400, 403, 409] as const)(
    "HTTP %s on one folder is not a full-selection success",
    async (status) => {
      const shown = vi.fn();
      const result = await saveSetFolderLocaleOnSelection({
        targets: [
          { folderId: "101", name: "News" },
          { folderId: "102", name: "Blog" },
        ],
        selectedCode: "fr-fr",
        allowedCodes: ["en-us", "fr-fr"],
        localeName: "French",
        loadProps: async (id) => propsFor(id, id, "en-us"),
        save: async (posted) => {
          if (posted.id === "102") {
            throw httpError(status);
          }
        },
        reload: async (id) => propsFor(id, id, "fr-fr"),
        onFolderSaved: shown,
      });
      expect(result.status).toBe("partial");
      expect(result.saved.map((row) => row.folderId)).toEqual(["101"]);
      expect(result.failures).toEqual([{ folderId: "102", name: "Blog", http: status }]);
      expect(shown).toHaveBeenCalledTimes(1);
      expect(describeSetFolderLocaleMultiSave(result, "French").text).not.toContain(
        "Folder locale saved",
      );
    },
  );

  it("does not post when the locale is already on every folder", async () => {
    const save = vi.fn();
    const shown = vi.fn();
    const result = await saveSetFolderLocaleOnSelection({
      targets: [{ folderId: "101", name: "News" }],
      selectedCode: "FR-FR",
      allowedCodes: ["en-us", "fr-fr"],
      localeName: "French",
      loadProps: async (id) => propsFor(id, "News", "fr-fr"),
      save,
      reload: async (id) => propsFor(id, "News", "en-us"),
      onFolderSaved: shown,
    });
    expect(save).not.toHaveBeenCalled();
    expect(shown).not.toHaveBeenCalled();
    expect(result.status).toBe("unchanged");
    const described = describeSetFolderLocaleMultiSave(result, "French");
    expect(described.kind).toBe("error");
    expect(described.localeCode).toBe("");
    expect(described.text).not.toContain("Folder locale saved");
  });

  it("does not load properties for a pages-only selection", async () => {
    const loadProps = vi.fn();
    const loaded = await loadSetFolderLocaleMultiCatalog({
      items: [page, asset],
      loadProps,
      loadCatalog: async () => ({ choices: catalogChoices }),
    });
    expect(loaded).toMatchObject({ status: "blocked", reason: "page" });
    expect(loadProps).not.toHaveBeenCalled();
  });
});
