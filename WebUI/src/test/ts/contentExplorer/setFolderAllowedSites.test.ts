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
import { unwrapFolderAllowedSitesCatalog } from "../../../main/ts/api/contentExplorer/folderAllowedSitesApi";
import type { PSFolderProperties, PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  canonicalAllowedSites,
  classifySetFolderAllowedSitesSelection,
  describeSetFolderAllowedSitesMultiSave,
  gateExplorerFolderAllowedSitesChange,
  loadSetFolderAllowedSitesCatalog,
  loadSetFolderAllowedSitesMultiCatalog,
  planSetFolderAllowedSitesMulti,
  readReloadedAllowedSites,
  saveSetFolderAllowedSites,
  saveSetFolderAllowedSitesOnSelection,
} from "../../../main/ts/contentExplorer/setFolderAllowedSites";

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

const props = (allowedSites: string): PSFolderProperties => ({
  id: folder.id ?? "",
  name: "CI",
  allowedSites,
  permission: { accessLevel: "ADMIN" },
});

const catalog = async () => ({
  choices: [
    { id: "301", name: "Enterprise" },
    { id: "302", name: "Corporate" },
  ],
});

const allowed = ["301", "302"];

describe("set allowed publish sites (#5132)", () => {
  it("canonicalizes ids and rejects a site name", () => {
    expect(canonicalAllowedSites(" 302, 301,0302 ")).toEqual({
      ok: true,
      canonical: "301,302",
    });
    expect(canonicalAllowedSites("")).toEqual({ ok: true, canonical: "" });
    expect(canonicalAllowedSites(null)).toEqual({ ok: true, canonical: "" });
    expect(canonicalAllowedSites(301)).toEqual({ ok: true, canonical: "301" });
    expect(canonicalAllowedSites("Enterprise").ok).toBe(false);
  });

  it("blocks empty, pages, assets, and multi-select before any save", () => {
    expect(
      classifySetFolderAllowedSitesSelection({ item: null, selectedCount: 0 }),
    ).toMatchObject({ status: "blocked", reason: "empty" });
    expect(
      classifySetFolderAllowedSitesSelection({ item: page, selectedCount: 1 }),
    ).toMatchObject({ status: "blocked", reason: "page", name: "Home" });
    expect(
      classifySetFolderAllowedSitesSelection({ item: asset, selectedCount: 1 }),
    ).toMatchObject({ status: "blocked", reason: "asset", name: "Logo" });
    expect(
      classifySetFolderAllowedSitesSelection({ item: folder, selectedCount: 2 }),
    ).toMatchObject({ reason: "multi" });
    expect(
      classifySetFolderAllowedSitesSelection({ item: folder, selectedCount: 1 }),
    ).toMatchObject({ status: "ready", folderId: folder.id });
  });

  it("does not call the server for a page, an asset, an empty selection, or multi-select", async () => {
    const loadProps = vi.fn();
    const loadCatalog = vi.fn(catalog);
    for (const item of [null, page, asset]) {
      const loaded = await loadSetFolderAllowedSitesCatalog({
        item,
        selectedCount: item ? 1 : 0,
        loadProps,
        loadCatalog,
      });
      expect(loaded.status).toBe("blocked");
    }
    const multi = await loadSetFolderAllowedSitesCatalog({
      item: folder,
      selectedCount: 2,
      loadProps,
      loadCatalog,
    });
    expect(multi).toMatchObject({ status: "blocked", reason: "multi" });
    expect(loadProps).not.toHaveBeenCalled();
    expect(loadCatalog).not.toHaveBeenCalled();
  });

  it("does not save an unchanged list or a site that is not in the catalog", async () => {
    const save = vi.fn();
    const unchanged = await saveSetFolderAllowedSites({
      folderId: folder.id ?? "",
      props: props("302,301"),
      selectedIds: ["301", "302"],
      currentSites: "301,302",
      allowedIds: allowed,
      save,
      reload: vi.fn(),
    });
    expect(unchanged).toMatchObject({ status: "gate", reason: "unchanged" });
    const forbidden = await saveSetFolderAllowedSites({
      folderId: folder.id ?? "",
      props: props("301"),
      selectedIds: ["999"],
      currentSites: "301",
      allowedIds: allowed,
      save,
      reload: vi.fn(),
    });
    expect(forbidden).toMatchObject({ status: "gate", reason: "forbidden" });
    expect(
      gateExplorerFolderAllowedSitesChange({
        selectedIds: ["Enterprise"],
        currentSites: "",
        allowedIds: allowed,
      }),
    ).toMatchObject({ reason: "invalid" });
    expect(save).not.toHaveBeenCalled();
  });

  it("saves only after refresh shows the stored list", async () => {
    const save = vi.fn(async () => undefined);
    let stored = "301";
    const reload = vi.fn(async () => props(stored));
    const early = await saveSetFolderAllowedSites({
      folderId: folder.id ?? "",
      props: props("301"),
      selectedIds: ["301", "302"],
      currentSites: "301",
      allowedIds: allowed,
      choices: [
        { id: "301", name: "Enterprise" },
        { id: "302", name: "Corporate" },
      ],
      save,
      reload,
    });
    expect(early.status).toBe("mismatch");
    expect(save).toHaveBeenCalledTimes(1);
    expect(save.mock.calls[0][0].allowedSites).toBe("301,302");
    stored = "301,302";
    const saved = await saveSetFolderAllowedSites({
      folderId: folder.id ?? "",
      props: props("301"),
      selectedIds: ["302", "301"],
      currentSites: "301",
      allowedIds: allowed,
      choices: [
        { id: "301", name: "Enterprise" },
        { id: "302", name: "Corporate" },
      ],
      save,
      reload,
    });
    expect(saved).toEqual({
      status: "saved",
      allowedSites: "301,302",
      allowedSiteNames: "Enterprise, Corporate",
      cleared: false,
    });
  });

  it("confirmed empty list clears and is success only when refresh is empty", async () => {
    const save = vi.fn(async () => undefined);
    const stillRestricted = await saveSetFolderAllowedSites({
      folderId: folder.id ?? "",
      props: props("301"),
      selectedIds: [],
      currentSites: "301",
      allowedIds: allowed,
      save,
      reload: async () => props("301"),
    });
    expect(stillRestricted.status).toBe("mismatch");
    expect(save.mock.calls[0][0].allowedSites).toBe("");
    const cleared = await saveSetFolderAllowedSites({
      folderId: folder.id ?? "",
      props: props("301"),
      selectedIds: [],
      currentSites: "301",
      allowedIds: allowed,
      save,
      reload: async () => props(""),
    });
    expect(cleared).toMatchObject({ status: "saved", allowedSites: "", cleared: true });
    const missing = await saveSetFolderAllowedSites({
      folderId: folder.id ?? "",
      props: props("301"),
      selectedIds: [],
      currentSites: "301",
      allowedIds: allowed,
      save,
      reload: async () => ({ id: folder.id ?? "", name: "CI" }),
    });
    expect(missing).toMatchObject({ status: "saved", cleared: true });
  });

  it("HTTP 400, 403, and 409 do not claim a saved list", async () => {
    for (const status of [400, 403, 409] as const) {
      const save = vi.fn(async () => {
        throw httpError(status);
      });
      const result = await saveSetFolderAllowedSites({
        folderId: folder.id ?? "",
        props: props(""),
        selectedIds: ["302"],
        currentSites: "",
        allowedIds: allowed,
        save,
        reload: vi.fn(),
      });
      expect(result).toEqual({ status: "http", http: status });
    }
  });

  it("opens when a stored list can be cleared even if the catalog is empty", async () => {
    const loaded = await loadSetFolderAllowedSitesCatalog({
      item: folder,
      selectedCount: 1,
      loadProps: async () => props("301"),
      loadCatalog: async () => ({ choices: [] }),
    });
    expect(loaded).toMatchObject({ status: "ready", currentSites: "301" });
    const none = await loadSetFolderAllowedSitesCatalog({
      item: folder,
      selectedCount: 1,
      loadProps: async () => props(""),
      loadCatalog: async () => ({ choices: [] }),
    });
    expect(none.status).toBe("none");
  });

  it("reads a wrapped properties refresh and a wrapped catalog", () => {
    expect(
      readReloadedAllowedSites({
        FolderProperties: { id: "1", name: "CI", allowedSites: "302,301" },
      }),
    ).toBe("301,302");
    expect(
      unwrapFolderAllowedSitesCatalog({
        FolderAllowedSitesCatalog: {
          choices: { FolderAllowedSiteChoice: { id: "301", name: "Enterprise" } },
        },
      }).choices,
    ).toEqual([{ id: "301", name: "Enterprise" }]);
  });
});

const siteChoices = [
  { id: "301", name: "Enterprise" },
  { id: "302", name: "Corporate" },
];

function propsFor(id: string, name: string, allowedSites: string): PSFolderProperties {
  return {
    id,
    name,
    allowedSites,
    permission: { accessLevel: "ADMIN" },
  };
}

describe("set allowed publish sites on multi-selected folders (#5181)", () => {
  it("still blocks multi on the single-folder classifier", () => {
    expect(
      classifySetFolderAllowedSitesSelection({ item: folder, selectedCount: 2 }),
    ).toMatchObject({
      status: "blocked",
      reason: "multi",
    });
  });

  it("plans each folder once and skips pages and assets", () => {
    const plan = planSetFolderAllowedSitesMulti([folder, page, blog, asset, folder]);
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
    expect(planSetFolderAllowedSitesMulti([page, page])).toMatchObject({
      status: "blocked",
      reason: "page",
      name: "Home",
    });
    expect(planSetFolderAllowedSitesMulti([asset])).toMatchObject({
      status: "blocked",
      reason: "asset",
      name: "Logo",
    });
    expect(planSetFolderAllowedSitesMulti([])).toMatchObject({
      status: "blocked",
      reason: "empty",
    });
  });

  it("does not open a save when the shared catalog is HTTP 403", async () => {
    const save = vi.fn();
    const loaded = await loadSetFolderAllowedSitesMultiCatalog({
      items: [folder, blog, page],
      loadProps: async () => propsFor(String(folder.id), "CI", "301"),
      loadCatalog: vi.fn().mockRejectedValue(httpError(403)),
    });
    expect(loaded).toEqual({ status: "http", http: 403 });
    expect(save).not.toHaveBeenCalled();
  });

  it("shows each site list only after that folder refresh and does not claim success on HTTP 409", async () => {
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
    const pending = saveSetFolderAllowedSitesOnSelection({
      targets: [
        { folderId: String(folder.id), name: "CI" },
        { folderId: String(blog.id), name: "Blog" },
      ],
      skippedPageNames: ["Home"],
      skippedAssetNames: ["Logo"],
      selectedIds: ["302", "301"],
      allowedIds: allowed,
      choices: siteChoices,
      loadProps: async (id) => {
        order.push(`get:${id}`);
        return propsFor(id, id === blog.id ? "Blog" : "CI", "301");
      },
      save,
      reload: async (id) => propsFor(id, id === blog.id ? "Blog" : "CI", "301,302"),
      onFolderSaved: (row) => {
        order.push(`shown:${row.folderId}:${row.allowedSiteNames}`);
      },
    });
    await vi.waitFor(() => expect(order).toContain(`post:${blog.id}`));
    expect(order).toEqual([
      `get:${folder.id}`,
      `post:${folder.id}`,
      `shown:${folder.id}:Enterprise, Corporate`,
      `get:${blog.id}`,
      `post:${blog.id}`,
    ]);
    releaseBlog();
    const result = await pending;
    expect(result.status).toBe("partial");
    expect(result.allowedSites).toBe("");
    expect(result.cleared).toBe(false);
    expect(result.saved.map((row) => row.folderId)).toEqual([folder.id]);
    expect(result.failures).toEqual([{ folderId: blog.id, name: "Blog", http: 409 }]);
    expect(order.filter((step) => step.startsWith("shown:"))).toEqual([
      `shown:${folder.id}:Enterprise, Corporate`,
    ]);
    const described = describeSetFolderAllowedSitesMultiSave(result);
    expect(described.kind).toBe("error");
    expect(described.reason).toBe("partial");
    expect(described.allowedSites).toBe("");
    expect(described.allowedSiteNames).toBe("");
    expect(described.text).toContain("Not every selected folder had its allowed publish sites set");
    expect(described.text).toContain("Blog (HTTP 409)");
    expect(described.text).toContain("Pages are not given allowed publish sites: Home");
    expect(described.text).toContain("Assets are not given allowed publish sites: Logo");
    expect(described.text).not.toContain("Allowed publish sites saved");
    expect(described.text).not.toContain("Allowed publish sites cleared");
  });

  it("claims success only after every folder refresh shows the same site ids", async () => {
    const save = vi.fn(async () => undefined);
    const shown: string[] = [];
    const result = await saveSetFolderAllowedSitesOnSelection({
      targets: [
        { folderId: "101", name: "News" },
        { folderId: "102", name: "Blog" },
      ],
      skippedPageNames: ["Home"],
      selectedIds: ["301", "302"],
      allowedIds: allowed,
      choices: siteChoices,
      loadProps: async (id) => propsFor(id, id, ""),
      save,
      reload: async (id) => propsFor(id, id, "302,301"),
      onFolderSaved: (row) => shown.push(`${row.folderId}:${row.allowedSiteNames}`),
    });
    expect(save).toHaveBeenCalledTimes(2);
    expect(save.mock.calls[0][0].allowedSites).toBe("301,302");
    expect(shown).toEqual(["101:Enterprise, Corporate", "102:Enterprise, Corporate"]);
    expect(result.status).toBe("saved");
    expect(result.cleared).toBe(false);
    const described = describeSetFolderAllowedSitesMultiSave(result);
    expect(described).toMatchObject({
      kind: "success",
      reason: "items-skipped",
      allowedSites: "301,302",
      allowedSiteNames: "Enterprise, Corporate",
    });
    expect(described.text).toContain("Allowed publish sites saved Enterprise, Corporate");
    expect(described.text).toContain("Home");
  });

  it("claims a clear only after every folder refresh shows no stored list", async () => {
    const save = vi.fn(async () => undefined);
    const shown: string[] = [];
    const result = await saveSetFolderAllowedSitesOnSelection({
      targets: [
        { folderId: "101", name: "News" },
        { folderId: "102", name: "Blog" },
      ],
      selectedIds: [],
      allowedIds: allowed,
      choices: siteChoices,
      loadProps: async (id) => propsFor(id, id, "301"),
      save,
      reload: async (id) => propsFor(id, id, ""),
      onFolderSaved: (row) => shown.push(`${row.folderId}:${row.cleared}`),
    });
    expect(save).toHaveBeenCalledTimes(2);
    expect(save.mock.calls[0][0].allowedSites).toBe("");
    expect(shown).toEqual(["101:true", "102:true"]);
    expect(result).toMatchObject({ status: "saved", allowedSites: "", cleared: true });
    const described = describeSetFolderAllowedSitesMultiSave(result);
    expect(described).toMatchObject({
      kind: "success",
      reason: "cleared",
      allowedSites: "",
      allowedSiteNames: "",
    });
    expect(described.text).toContain("Allowed publish sites cleared");
    expect(described.text).not.toContain("Allowed publish sites saved");
  });

  it("does not show site names when refresh still has the old list", async () => {
    const shown = vi.fn();
    const result = await saveSetFolderAllowedSitesOnSelection({
      targets: [{ folderId: "101", name: "News" }],
      selectedIds: ["301", "302"],
      allowedIds: allowed,
      choices: siteChoices,
      loadProps: async (id) => propsFor(id, "News", "301"),
      save: async () => undefined,
      reload: async (id) => propsFor(id, "News", "301"),
      onFolderSaved: shown,
    });
    expect(result.status).toBe("failed");
    expect(result.failures).toEqual([{ folderId: "101", name: "News", http: "mismatch" }]);
    expect(shown).not.toHaveBeenCalled();
    expect(describeSetFolderAllowedSitesMultiSave(result).text).not.toContain(
      "Allowed publish sites saved",
    );
  });

  it.each([400, 403, 409] as const)(
    "HTTP %s on one folder is not a full-selection success",
    async (status) => {
      const shown = vi.fn();
      const result = await saveSetFolderAllowedSitesOnSelection({
        targets: [
          { folderId: "101", name: "News" },
          { folderId: "102", name: "Blog" },
        ],
        selectedIds: ["302"],
        allowedIds: allowed,
        choices: siteChoices,
        loadProps: async (id) => propsFor(id, id, ""),
        save: async (posted) => {
          if (posted.id === "102") {
            throw httpError(status);
          }
        },
        reload: async (id) => propsFor(id, id, "302"),
        onFolderSaved: shown,
      });
      expect(result.status).toBe("partial");
      expect(result.saved.map((row) => row.folderId)).toEqual(["101"]);
      expect(result.failures).toEqual([{ folderId: "102", name: "Blog", http: status }]);
      expect(shown).toHaveBeenCalledTimes(1);
      expect(describeSetFolderAllowedSitesMultiSave(result).text).not.toContain(
        "Allowed publish sites saved",
      );
    },
  );

  it("does not post when every folder already stores that list", async () => {
    const save = vi.fn();
    const shown = vi.fn();
    const result = await saveSetFolderAllowedSitesOnSelection({
      targets: [{ folderId: "101", name: "News" }],
      selectedIds: ["301", "302"],
      allowedIds: allowed,
      choices: siteChoices,
      loadProps: async (id) => propsFor(id, "News", "302,301"),
      save,
      reload: async (id) => propsFor(id, "News", ""),
      onFolderSaved: shown,
    });
    expect(save).not.toHaveBeenCalled();
    expect(shown).not.toHaveBeenCalled();
    expect(result.status).toBe("unchanged");
    const described = describeSetFolderAllowedSitesMultiSave(result);
    expect(described.kind).toBe("error");
    expect(described.allowedSites).toBe("");
    expect(described.text).not.toContain("Allowed publish sites saved");
    expect(described.text).not.toContain("Allowed publish sites cleared");
  });

  it("does not load properties for a pages-only selection", async () => {
    const loadProps = vi.fn();
    const loaded = await loadSetFolderAllowedSitesMultiCatalog({
      items: [page, asset],
      loadProps,
      loadCatalog: catalog,
    });
    expect(loaded.status).toBe("blocked");
    expect(loadProps).not.toHaveBeenCalled();
  });
});
