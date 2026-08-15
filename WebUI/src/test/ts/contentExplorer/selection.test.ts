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
  });
});
