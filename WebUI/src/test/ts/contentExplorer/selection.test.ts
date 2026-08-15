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
  isFolder,
  isPageOrAssetContentType,
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
        type: "rffSection",
      }),
    ).toBe(false);
    expect(
      isPageOrAssetContentType({
        name: "Category",
        path: "/Sites/Demo/Category",
        type: "rffCategory",
      }),
    ).toBe(false);
    expect(
      isFolder({
        id: "16777215-101-30",
        name: "Section",
        path: "/Sites/Demo/Section",
        type: "rffSection",
        leaf: false,
      }),
    ).toBe(true);
  });

  it("rejects pagination/pagebreak/pagelet/assetmanagement as page or asset types", () => {
    expect(
      isPageOrAssetContentType({
        name: "p",
        path: "/Sites/Demo/pagination",
        type: "pagination",
      }),
    ).toBe(false);
    expect(
      isPageOrAssetContentType({
        name: "p",
        path: "/Sites/Demo/pagebreak",
        type: "pagebreak",
      }),
    ).toBe(false);
    expect(
      isPageOrAssetContentType({
        name: "p",
        path: "/Sites/Demo/pagelet",
        type: "pagelet",
      }),
    ).toBe(false);
    expect(
      isPageOrAssetContentType({
        name: "a",
        path: "/Sites/Demo/assetmanagement",
        type: "assetmanagement",
      }),
    ).toBe(false);
  });

  it("keeps custom folder types under /Sites/ as folders when leaf is false", () => {
    expect(
      isFolder({
        id: "16777215-101-20",
        name: "Campaigns",
        path: "/Sites/Demo/Campaigns",
        type: "CampaignFolder",
        leaf: false,
      }),
    ).toBe(true);
  });
});
