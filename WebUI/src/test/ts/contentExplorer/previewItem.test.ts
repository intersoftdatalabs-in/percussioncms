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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it, vi } from "vitest";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  buildAssetViewUrlRequestPath,
  buildPageRenderPreviewUrl,
  buildSitePathPreviewUrl,
  isPreviewableItem,
  normalizeCmsPath,
  openPreviewItem,
  resolvePagePreviewPath,
  resolvePreviewKind,
  resolvePreviewTarget,
} from "../../../main/ts/contentExplorer/previewItem";

const PAGE: PSPathItem = {
  id: "16777215-101-1",
  name: "Home",
  path: "/Sites/Demo/Home",
  type: "page",
  accessLevel: "READ",
};

const ASSET: PSPathItem = {
  id: "16777216-101-2",
  name: "logo.png",
  path: "/Assets/uploads/logo.png",
  type: "asset",
  category: "asset",
  accessLevel: "READ",
};

const FOLDER: PSPathItem = {
  id: "f1",
  name: "Demo",
  path: "/Sites/Demo",
  type: "folder",
  accessLevel: "ADMIN",
};

describe("previewItem pure helpers (#2733)", () => {
  it("normalizeCmsPath collapses double-slash and backslashes", () => {
    expect(normalizeCmsPath("//Sites/Foo")).toBe("/Sites/Foo");
    expect(normalizeCmsPath("Sites/Foo")).toBe("/Sites/Foo");
    expect(normalizeCmsPath("/Sites/Foo/")).toBe("/Sites/Foo");
    expect(normalizeCmsPath("\\\\Sites\\\\Foo")).toBe("/Sites/Foo");
    expect(normalizeCmsPath("")).toBe("");
    expect(normalizeCmsPath(null)).toBe("");
  });

  it("buildSitePathPreviewUrl only for /Sites paths", () => {
    expect(buildSitePathPreviewUrl("/Sites/Demo/Home")).toBe(
      "/Sites/Demo/Home?percmobilepreview=false",
    );
    expect(buildSitePathPreviewUrl("//Sites/Demo/Home", { revisionId: 3 })).toBe(
      "/Sites/Demo/Home?sys_revision=3&percmobilepreview=false",
    );
    expect(buildSitePathPreviewUrl("/Assets/x")).toBe("");
    expect(buildSitePathPreviewUrl("")).toBe("");
  });

  it("buildSitePathPreviewUrl percent-encodes spaces and query/hash in names", () => {
    expect(
      buildSitePathPreviewUrl(
        "/Sites/Corporate Investments/Corporate Investments Home",
      ),
    ).toBe(
      "/Sites/Corporate%20Investments/Corporate%20Investments%20Home?percmobilepreview=false",
    );
    expect(buildSitePathPreviewUrl("/Sites/Demo/Q?A")).toBe(
      "/Sites/Demo/Q%3FA?percmobilepreview=false",
    );
    expect(buildSitePathPreviewUrl("/Sites/Demo/A#B")).toBe(
      "/Sites/Demo/A%23B?percmobilepreview=false",
    );
  });

  it("buildPageRenderPreviewUrl and asset request path encode id", () => {
    expect(buildPageRenderPreviewUrl("a-b", "/services")).toBe(
      "/services/pagemanagement/render/page/a-b",
    );
    expect(buildPageRenderPreviewUrl("a/b", "/services")).toBe(
      "/services/pagemanagement/render/page/a%2Fb",
    );
    expect(buildPageRenderPreviewUrl("", "/services")).toBe("");
    expect(buildAssetViewUrlRequestPath("x-1", "/services/")).toBe(
      "/services/assetmanagement/asset/assetViewUrl/x-1",
    );
  });

  it("resolvePreviewKind distinguishes page / asset / folder", () => {
    expect(resolvePreviewKind(null)).toBe("none");
    expect(resolvePreviewKind(FOLDER)).toBe("none");
    expect(resolvePreviewKind(PAGE)).toBe("page");
    expect(resolvePreviewKind(ASSET)).toBe("asset");
    expect(
      resolvePreviewKind({
        name: "x",
        path: "/Sites/S/p",
        type: "page",
      }),
    ).toBe("page");
    expect(
      resolvePreviewKind({
        name: "orphan",
        path: "/Other/x",
        type: "file",
      }),
    ).toBe("none");
    expect(isPreviewableItem(PAGE)).toBe(true);
    expect(isPreviewableItem(FOLDER)).toBe(false);
  });

  it("enables preview for listed percPage rows even when leaf is false (#3456)", () => {
    const listed: PSPathItem = {
      id: "16777215-101-9",
      name: "About",
      path: "/Sites/Corporate_Investments/Pages/About",
      type: "percPage",
      leaf: false,
      hasFolderChildren: true,
    };
    expect(resolvePreviewKind(listed)).toBe("page");
    expect(isPreviewableItem(listed)).toBe(true);
    const target = resolvePreviewTarget(listed, "/services");
    expect(target.kind).toBe("page");
    expect(target.url).toBe(
      "/services/pagemanagement/render/page/16777215-101-9",
    );
    expect(target.url).not.toContain("entry=editor");
    expect(target.url).not.toContain("/cm/app/editor");
    expect(
      isPreviewableItem({
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "Page",
      }),
    ).toBe(true);
    expect(
      resolvePreviewKind({
        id: "16777215-101-88",
        name: "Q3 Brief",
        path: "/Sites/Demo/Pages/Q3 Brief",
        type: "NewsArticle",
        category: "ASSET",
        leaf: false,
        hasFolderChildren: true,
      }),
    ).toBe("page");
    expect(
      resolvePreviewTarget(
        { name: "Home", path: "/Sites/Demo/Home", type: "Page" },
        "/services",
      ).url,
    ).toContain("/Sites/Demo/Home?");
  });

  it("resolvePagePreviewPath prefers Finder folderPaths + name (#3627)", () => {
    expect(
      resolvePagePreviewPath({
        id: "1-101-551",
        name: "Home",
        path: "/Sites/Corporate_Investments/Home",
        folderPath: "//Sites/CorporateInvestments",
        type: "rffHome",
      }),
    ).toBe("/Sites/CorporateInvestments/Home");
    expect(
      resolvePagePreviewPath({
        id: "1-101-552",
        name: "About",
        path: "/Sites/Corporate_Investments/About",
        folderPaths: ["//Sites/CorporateInvestments"],
        type: "rffGeneric",
      }),
    ).toBe("/Sites/CorporateInvestments/About");
    expect(
      resolvePagePreviewPath({
        name: "About",
        path: "/Sites/Demo/Pages/About",
        type: "percPage",
      }),
    ).toBe("/Sites/Demo/Pages/About");
    expect(
      resolvePagePreviewPath({
        id: "9",
        name: "logo.png",
        path: "/Assets/logo.png",
        type: "asset",
      }),
    ).toBe("");
  });

  it("resolvePreviewTarget prefers page render then site-path, not editor host (#3716)", () => {
    const t = resolvePreviewTarget(PAGE, "/services");
    expect(t.kind).toBe("page");
    expect(t.needsFetch).toBe(false);
    expect(t.url).toBe(
      "/services/pagemanagement/render/page/16777215-101-1",
    );
    expect(t.url).not.toContain("entry=editor");
    expect(t.url).not.toContain("/cm/app/editor");

    const pathOnly = resolvePreviewTarget(
      { name: "Home", path: "/Sites/Demo/Home", type: "page" },
      "/services",
    );
    expect(pathOnly.url).toContain("/Sites/Demo/Home?");
    expect(pathOnly.url).toContain("percmobilepreview=");
    expect(pathOnly.needsFetch).toBe(false);

    const guidPage = resolvePreviewTarget(
      {
        id: "16777215-101-88",
        name: "Home",
        path: "/Other/Home",
        type: "percPage",
      },
      "/services",
    );
    expect(guidPage.url).toBe(
      "/services/pagemanagement/render/page/16777215-101-88",
    );
    expect(guidPage.url).not.toContain("entry=editor");
    expect(guidPage.url).not.toContain("/cm/app/editor");

    const assetT = resolvePreviewTarget(ASSET, "/services");
    expect(assetT.kind).toBe("asset");
    expect(assetT.needsFetch).toBe(true);
    expect(assetT.url).toContain("assetViewUrl");
  });

  it("openPreviewItem opens page render without fetch when id is present (#3716)", async () => {
    const openWindow = vi.fn(() => null);
    const fetchText = vi.fn();
    await openPreviewItem(PAGE, {
      servicesRoot: "/services",
      openWindow,
      fetchText,
    });
    expect(fetchText).not.toHaveBeenCalled();
    expect(openWindow).toHaveBeenCalledTimes(1);
    const url = openWindow.mock.calls[0][0] as string;
    expect(url).toBe("/services/pagemanagement/render/page/16777215-101-1");
    expect(url).not.toContain("entry=editor");
    expect(url).not.toContain("/cm/app/editor");
  });

  it("openPreviewItem opens encoded site-path when there is no id (#3716)", async () => {
    const openWindow = vi.fn(() => null);
    const fetchText = vi.fn();
    await openPreviewItem(
      {
        name: "Corporate Investments Home",
        path: "/Sites/Corporate Investments/Corporate Investments Home",
        type: "page",
      },
      {
        servicesRoot: "/services",
        openWindow,
        fetchText,
      },
    );
    expect(fetchText).not.toHaveBeenCalled();
    expect(openWindow.mock.calls[0][0]).toBe(
      "/Sites/Corporate%20Investments/Corporate%20Investments%20Home?percmobilepreview=false",
    );
  });

  it("openPreviewItem prefixes /Rhythmyx on page URLs when the SPA is under that context", async () => {
    const original = window.location.pathname;
    window.history.replaceState({}, "", "/Rhythmyx/cm/app/explorer");
    try {
      const openWindow = vi.fn(() => null);
      await openPreviewItem(
        {
          name: "Home",
          path: "/Sites/Demo/Home",
          type: "page",
        },
        {
          servicesRoot: "/services",
          openWindow,
          fetchText: vi.fn(),
        },
      );
      expect(openWindow.mock.calls[0][0]).toBe(
        "/Rhythmyx/Sites/Demo/Home?percmobilepreview=false",
      );

      const renderOpen = vi.fn(() => null);
      await openPreviewItem(PAGE, {
        servicesRoot: "/services",
        openWindow: renderOpen,
        fetchText: vi.fn(),
      });
      expect(renderOpen.mock.calls[0][0]).toBe(
        "/Rhythmyx/services/pagemanagement/render/page/16777215-101-1",
      );
    } finally {
      window.history.replaceState({}, "", original || "/");
    }
  });

  it("openPreviewItem opens page render when the item is not under Sites (#3716)", async () => {
    const openWindow = vi.fn(() => null);
    const fetchText = vi.fn();
    await openPreviewItem(
      {
        id: "16777215-101-88",
        name: "Home",
        path: "/Other/Home",
        type: "percPage",
      },
      {
        servicesRoot: "/services",
        openWindow,
        fetchText,
      },
    );
    expect(fetchText).not.toHaveBeenCalled();
    expect(openWindow.mock.calls[0][0]).toBe(
      "/services/pagemanagement/render/page/16777215-101-88",
    );
  });

  it("openPreviewItem fetches asset view URL then opens body", async () => {
    const openWindow = vi.fn(() => null);
    const fetchText = vi.fn(async () => "/cm/app/asset-view?id=1");
    await openPreviewItem(ASSET, {
      servicesRoot: "/services",
      openWindow,
      fetchText,
    });
    expect(fetchText).toHaveBeenCalledWith(
      `/services/assetmanagement/asset/assetViewUrl/${encodeURIComponent(ASSET.id!)}`,
    );
    expect(openWindow).toHaveBeenCalledWith(
      "/cm/app/asset-view?id=1",
      expect.stringContaining("percAssetPreview_"),
    );
  });

  it("openPreviewItem rejects non-previewable folders", async () => {
    await expect(
      openPreviewItem(FOLDER, {
        openWindow: vi.fn(),
        fetchText: vi.fn(),
      }),
    ).rejects.toThrow(/not available/i);
  });
});
