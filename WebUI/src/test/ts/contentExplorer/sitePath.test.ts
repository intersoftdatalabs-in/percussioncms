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
  resolveExplorerListPath,
  resolveSiteNameFromExplorerPath,
  resolveSiteNameFromSelection,
} from "../../../main/ts/contentExplorer/sitePath";

describe("resolveSiteNameFromExplorerPath (#2767)", () => {
  it("returns null for empty / non-site paths", () => {
    expect(resolveSiteNameFromExplorerPath(null)).toBeNull();
    expect(resolveSiteNameFromExplorerPath(undefined)).toBeNull();
    expect(resolveSiteNameFromExplorerPath("")).toBeNull();
    expect(resolveSiteNameFromExplorerPath("/")).toBeNull();
    expect(resolveSiteNameFromExplorerPath("/Sites")).toBeNull();
    expect(resolveSiteNameFromExplorerPath("/Sites/")).toBeNull();
    expect(resolveSiteNameFromExplorerPath("/Assets/foo")).toBeNull();
  });

  it("extracts site name under /Sites/<name>", () => {
    expect(resolveSiteNameFromExplorerPath("/Sites/Demo")).toBe("Demo");
    expect(resolveSiteNameFromExplorerPath("/Sites/Demo/Home")).toBe("Demo");
    expect(resolveSiteNameFromExplorerPath("//Sites/Corp")).toBe("Corp");
    expect(resolveSiteNameFromExplorerPath("Sites/Acme/page")).toBe("Acme");
  });

  it("is case-insensitive on the Sites segment", () => {
    expect(resolveSiteNameFromExplorerPath("/sites/Lower")).toBe("Lower");
    expect(resolveSiteNameFromExplorerPath("/SITES/Upper")).toBe("Upper");
  });

  /**
   * CMS paths always use {@code /}, but callers may pass OS-style separators
   * (Windows explorer copy/paste, UNC-ish strings). sitePath normalizes
   * backslashes to forward slashes and strips drive letters before matching.
   */
  it("normalizes Windows-style backslash paths before matching", () => {
    expect(resolveSiteNameFromExplorerPath("\\Sites\\Demo")).toBe("Demo");
    expect(resolveSiteNameFromExplorerPath("\\Sites\\Demo\\Home")).toBe("Demo");
    expect(resolveSiteNameFromExplorerPath("C:\\Sites\\Demo")).toBe("Demo");
    expect(resolveSiteNameFromExplorerPath("C:\\Sites\\Demo\\Home")).toBe(
      "Demo",
    );
    // UNC-style prefix after normalize still finds /Sites/...
    expect(
      resolveSiteNameFromExplorerPath("\\\\server\\share\\Sites\\Demo"),
    ).toBe("Demo");
    expect(resolveSiteNameFromExplorerPath("\\Sites")).toBeNull();
    expect(resolveSiteNameFromExplorerPath("\\Assets\\foo")).toBeNull();
  });
});

describe("resolveSiteNameFromSelection (#2767)", () => {
  it("prefers item path over folder path", () => {
    expect(
      resolveSiteNameFromSelection("/Sites/FolderSite/sub", "/Sites/ItemSite"),
    ).toBe("ItemSite");
  });

  it("falls back to folder path when item is not site-scoped", () => {
    expect(
      resolveSiteNameFromSelection("/Sites/FolderSite/sub", "/Assets/x"),
    ).toBe("FolderSite");
  });
});

describe("resolveExplorerListPath (#3326)", () => {
  it("prefers folderPath over finder site-name path", () => {
    expect(
      resolveExplorerListPath({
        path: "/Sites/Corporate_Investments/",
        folderPath: "//Sites/CorporateInvestments",
      }),
    ).toBe("/Sites/CorporateInvestments");
  });

  it("falls back to path then explicit fallback", () => {
    expect(
      resolveExplorerListPath({ path: "/Sites/Demo/", folderPath: undefined }),
    ).toBe("/Sites/Demo/");
    expect(resolveExplorerListPath(null, "/Sites/Fallback")).toBe(
      "/Sites/Fallback",
    );
    expect(resolveExplorerListPath(null, null)).toBeNull();
  });
});
