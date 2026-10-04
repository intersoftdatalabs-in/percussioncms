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
  gateExplorerFolderAllowedSitesChange,
  loadSetFolderAllowedSitesCatalog,
  readReloadedAllowedSites,
  saveSetFolderAllowedSites,
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
