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

import { describe, expect, it } from "vitest";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  canAdmin,
  canRead,
  canWrite,
  explorerMultiSelectKey,
  isAssetContentType,
  isFolder,
  isPageOrAssetContentType,
  sameExplorerItemId,
} from "../../../main/ts/contentExplorer/selection";

describe("isFolder (#3001 site nodes)", () => {
  it("treats type site as expandable folder", () => {
    const site: PSPathItem = {
      id: "1",
      name: "Demo",
      path: "/Sites/1/",
      type: "site",
      leaf: false,
    };
    expect(isFolder(site)).toBe(true);
  });

  it("treats type site as folder even when leaf is omitted", () => {
    const site: PSPathItem = {
      name: "Demo",
      path: "/Sites/1/",
      type: "site",
    };
    expect(isFolder(site)).toBe(true);
  });

  it("still treats plain folders as folders", () => {
    expect(
      isFolder({ name: "Home", path: "/Sites/1/Home", type: "folder" }),
    ).toBe(true);
  });

  it("treats trailing-slash paths as folders when type is omitted (#3330 $System$)", () => {
    expect(
      isFolder({
        id: "3",
        name: "$System$",
        path: "/Folders/$System$/",
        leaf: true,
      }),
    ).toBe(true);
  });

  it("treats pathmanagement Folder / FSFolder as folders even with id and leaf (#3330)", () => {
    expect(
      isFolder({
        id: "16777215-101-1",
        name: "New-Folder",
        path: "/Folders/New-Folder/",
        type: "Folder",
        leaf: true,
      }),
    ).toBe(true);
    expect(
      isFolder({
        id: "2",
        name: "css",
        path: "/Design/css/",
        type: "FSFolder",
        leaf: true,
      }),
    ).toBe(true);
  });

  it("does not treat leaf pages as folders", () => {
    expect(
      isFolder({
        name: "index.html",
        path: "/Sites/1/index.html",
        type: "page",
        leaf: true,
      }),
    ).toBe(false);
  });

  it("does not treat listed percPage rows as folders (#3456)", () => {
    const listed: PSPathItem = {
      id: "16777215-101-9",
      name: "About",
      path: "/Sites/Corporate_Investments/Pages/About",
      type: "percPage",
      leaf: false,
      hasFolderChildren: true,
    };
    expect(isPageOrAssetContentType(listed)).toBe(true);
    expect(isFolder(listed)).toBe(false);
    expect(
      isFolder({
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "Page",
        category: "PAGE",
      }),
    ).toBe(false);
    expect(
      isFolder({
        id: "16777215-101-551",
        name: "Corporate Investments Home",
        path: "/Sites/CorporateInvestments/Pages/Corporate Investments Home",
        type: "rffHome",
        leaf: false,
      }),
    ).toBe(false);
  });

  it("allows known FastForward item types and rejects nav / folder-like rff types", () => {
    expect(
      isPageOrAssetContentType({
        name: "Event",
        path: "/Sites/Demo/Events/Open House",
        type: "rffEvent",
      }),
    ).toBe(true);
    expect(
      isPageOrAssetContentType({
        name: "Photo",
        path: "/Sites/Demo/Assets/photo",
        type: "rffImage",
      }),
    ).toBe(true);
    expect(
      isPageOrAssetContentType({
        name: "Section",
        path: "/Sites/Demo/Section",
        type: "rffNavon",
      }),
    ).toBe(false);
    expect(
      isPageOrAssetContentType({
        name: "Nav",
        path: "/Sites/Demo/Nav",
        type: "rffNavTree",
      }),
    ).toBe(false);
    expect(
      isPageOrAssetContentType({
        name: "Section",
        path: "/Sites/Demo/Section",
        type: "rffNavon",
        category: "SECTION_FOLDER",
      }),
    ).toBe(false);
  });

  it("treats customer-defined types as items without a name allowlist (#3456)", () => {
    const custom: PSPathItem = {
      id: "16777215-101-88",
      name: "Q3 Brief",
      path: "/Sites/Demo/Pages/Q3 Brief",
      type: "NewsArticle",
      leaf: false,
      hasFolderChildren: true,
    };
    expect(isPageOrAssetContentType(custom)).toBe(true);
    expect(isFolder(custom)).toBe(false);
    expect(
      isPageOrAssetContentType({
        id: "16777215-101-89",
        name: "Widget",
        path: "/Sites/Demo/Products/Widget",
        type: "CI_products",
        category: "ASSET",
      }),
    ).toBe(true);
    expect(
      isPageOrAssetContentType({
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "CustomLanding",
        category: "LANDING_PAGE",
      }),
    ).toBe(true);
  });

  it("keeps server folder categories as folders even with a customer type name", () => {
    expect(
      isFolder({
        id: "16777215-101-20",
        name: "Campaigns",
        path: "/Sites/Demo/Campaigns",
        type: "CampaignFolder",
        category: "FOLDER",
        leaf: false,
      }),
    ).toBe(true);
    expect(
      isPageOrAssetContentType({
        id: "16777215-101-20",
        name: "Campaigns",
        path: "/Sites/Demo/Campaigns",
        type: "CampaignFolder",
        category: "FOLDER",
      }),
    ).toBe(false);
  });
});

describe("sameExplorerItemId / canRead (#3467)", () => {
  it("matches string and numeric content ids", () => {
    expect(sameExplorerItemId("42", 42)).toBe(true);
    expect(sameExplorerItemId(42, "42")).toBe(true);
    expect(sameExplorerItemId("42", "43")).toBe(false);
    expect(sameExplorerItemId(null, "42")).toBe(false);
  });

  it("treats listed rows without an ACL token as readable", () => {
    expect(
      canRead({
        id: "42",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
      }),
    ).toBe(true);
    expect(
      canRead({
        id: "42",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        accessLevel: "WRITE",
      }),
    ).toBe(true);
    expect(
      canRead({
        id: "42",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        accessLevel: "NONE",
      }),
    ).toBe(false);
  });

  it("normalizes mixed-case ACL tokens for read, write, and admin", () => {
    const page = (
      accessLevel: string,
    ): PSPathItem => ({
      id: "42",
      name: "Home",
      path: "/Sites/Demo/Home",
      type: "percPage",
      accessLevel: accessLevel as PSPathItem["accessLevel"],
    });
    expect(canRead(page("write"))).toBe(true);
    expect(canRead(page("view"))).toBe(true);
    expect(canWrite(page("write"))).toBe(true);
    expect(canWrite(page("admin"))).toBe(true);
    expect(canWrite(page("read"))).toBe(false);
    expect(canAdmin(page("admin"))).toBe(true);
    expect(canAdmin(page("write"))).toBe(false);
  });
});

describe("explorerMultiSelectKey (#3552 review)", () => {
  it("prefers id then path and never falls back to name", () => {
    expect(
      explorerMultiSelectKey({
        id: "42",
        name: "Home",
        path: "/Sites/Demo/Home",
      }),
    ).toBe("42");
    expect(
      explorerMultiSelectKey({
        name: "Home",
        path: "/Sites/Demo/Home",
      }),
    ).toBe("/Sites/Demo/Home");
    expect(
      explorerMultiSelectKey({
        id: "   ",
        name: "Home",
        path: "/Sites/Other/Home",
      }),
    ).toBe("/Sites/Other/Home");
    expect(
      explorerMultiSelectKey({
        name: "Home",
        path: "",
      }),
    ).toBeNull();
  });

  it("does not collide two nameless-id rows that share a display name", () => {
    const a = explorerMultiSelectKey({ name: "Home", path: "" });
    const b = explorerMultiSelectKey({ name: "Home", path: "  " });
    expect(a).toBeNull();
    expect(b).toBeNull();
  });
});

describe("isAssetContentType (#3552 review)", () => {
  it("treats stock asset types and /Assets paths as assets", () => {
    expect(
      isAssetContentType({
        id: "a-1",
        name: "hero.png",
        path: "/Assets/hero.png",
        type: "rffImage",
      }),
    ).toBe(true);
    expect(
      isAssetContentType({
        id: "a-2",
        name: "file",
        path: "/Assets/docs/a.pdf",
        type: "percAsset",
      }),
    ).toBe(true);
    expect(
      isAssetContentType({
        id: "a-3",
        name: "banner",
        path: "/Assets/banner",
        type: "asset",
      }),
    ).toBe(true);
  });

  it("does not treat category resource or percPage as assets", () => {
    expect(
      isAssetContentType({
        id: "p-1",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "RESOURCE",
      }),
    ).toBe(false);
    expect(
      isAssetContentType({
        id: "p-2",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "PAGE",
      }),
    ).toBe(false);
    expect(
      isAssetContentType({
        id: "f-1",
        name: "Files",
        path: "/Assets/Files/",
        type: "FSFolder",
      }),
    ).toBe(false);
  });
});
