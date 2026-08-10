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
