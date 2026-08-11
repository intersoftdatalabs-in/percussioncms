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
import {
  isStrictCmsPathDescendant,
  normalizeExplorerFolderPath,
  resolveFolderPathFromSelection,
} from "../../../main/ts/contentExplorer/folderPath";

describe("normalizeExplorerFolderPath (#2792)", () => {
  it("returns null for empty / root-only paths", () => {
    expect(normalizeExplorerFolderPath(null)).toBeNull();
    expect(normalizeExplorerFolderPath(undefined)).toBeNull();
    expect(normalizeExplorerFolderPath("")).toBeNull();
    expect(normalizeExplorerFolderPath("   ")).toBeNull();
    expect(normalizeExplorerFolderPath("/")).toBeNull();
  });

  it("normalizes CMS folder paths", () => {
    expect(normalizeExplorerFolderPath("/Sites/Demo/Home")).toBe(
      "/Sites/Demo/Home",
    );
    expect(normalizeExplorerFolderPath("Assets/img")).toBe("/Assets/img");
    expect(normalizeExplorerFolderPath("//Sites/Demo")).toBe("/Sites/Demo");
  });

  it("normalizes Windows-style separators before use", () => {
    expect(normalizeExplorerFolderPath("\\Sites\\Demo\\Home")).toBe(
      "/Sites/Demo/Home",
    );
    expect(normalizeExplorerFolderPath("C:\\Sites\\Demo")).toBe("/Sites/Demo");
  });
});

describe("isStrictCmsPathDescendant (#3001)", () => {
  it("accepts site id children under /Sites with either slash form", () => {
    expect(
      isStrictCmsPathDescendant("/Sites", "/Sites/16777215-101-703/"),
    ).toBe(true);
    expect(
      isStrictCmsPathDescendant("/Sites/", "/Sites/16777215-101-703/"),
    ).toBe(true);
    expect(
      isStrictCmsPathDescendant("//Sites/", "//Sites/Corporate_Investments"),
    ).toBe(true);
    expect(
      isStrictCmsPathDescendant("/Sites", "//Sites/Enterprise_Investments"),
    ).toBe(true);
  });

  it("rejects self, siblings, and non-descendants", () => {
    expect(isStrictCmsPathDescendant("/Sites", "/Sites")).toBe(false);
    expect(isStrictCmsPathDescendant("/Sites/", "/Sites/")).toBe(false);
    expect(isStrictCmsPathDescendant("/Sites", "/Assets/x")).toBe(false);
    expect(isStrictCmsPathDescendant("/Sites/Foo", "/Sites/Bar")).toBe(false);
    expect(isStrictCmsPathDescendant("/Sites", "")).toBe(false);
    expect(isStrictCmsPathDescendant("/Sites", null)).toBe(false);
  });

  it("treats any non-root path as a child of root", () => {
    expect(isStrictCmsPathDescendant("/", "/Sites/")).toBe(true);
    expect(isStrictCmsPathDescendant("/", "/")).toBe(false);
  });

  it("accepts classic Folders root and children under / (#3044)", () => {
    expect(isStrictCmsPathDescendant("/", "/Folders/")).toBe(true);
    expect(isStrictCmsPathDescendant("/", "//Folders")).toBe(true);
    expect(isStrictCmsPathDescendant("/", "/Folders")).toBe(true);
    expect(
      isStrictCmsPathDescendant("/Folders", "/Folders/$System$"),
    ).toBe(true);
    expect(
      isStrictCmsPathDescendant("//Folders/", "//Folders/$System$/Assets"),
    ).toBe(true);
    // Self is not a strict descendant (tree cycle guard).
    expect(isStrictCmsPathDescendant("/Folders", "/Folders")).toBe(false);
    expect(isStrictCmsPathDescendant("/Folders", "/Sites")).toBe(false);
  });
});

describe("resolveFolderPathFromSelection (#2792)", () => {
  it("prefers selected folder row path over active folder", () => {
    expect(
      resolveFolderPathFromSelection(
        "/Sites/Demo",
        "/Sites/Demo/Home",
        "folder",
      ),
    ).toBe("/Sites/Demo/Home");
  });

  it("falls back to folder path when selection is a content item", () => {
    expect(
      resolveFolderPathFromSelection(
        "/Sites/Demo/Home",
        "/Sites/Demo/Home/page1",
        "page",
      ),
    ).toBe("/Sites/Demo/Home");
  });

  it("returns null when neither selection nor folder is usable", () => {
    expect(resolveFolderPathFromSelection(null, null, null)).toBeNull();
    expect(resolveFolderPathFromSelection("/", null, null)).toBeNull();
    expect(
      resolveFolderPathFromSelection(null, "/Sites/X", "page"),
    ).toBeNull();
  });
});
