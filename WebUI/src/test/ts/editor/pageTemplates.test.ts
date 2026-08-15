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

import { beforeEach, describe, expect, it, vi } from "vitest";
import { getContentTypeDetail } from "../../../main/ts/api/developer/contentTypesApi";
import { fetchTemplatesForSite } from "../../../main/ts/api/home/homeApi";
import {
  isExplorerPageType,
  loadPageTemplates,
  siteNameFromFolderPath,
  templatesFromContentType,
} from "../../../main/ts/editor/pageTemplates";

vi.mock("../../../main/ts/api/developer/contentTypesApi", () => ({
  getContentTypeDetail: vi.fn(),
}));

vi.mock("../../../main/ts/api/home/homeApi", () => ({
  fetchTemplatesForSite: vi.fn(),
}));

describe("pageTemplates", () => {
  beforeEach(() => {
    vi.mocked(getContentTypeDetail).mockReset();
    vi.mocked(fetchTemplatesForSite).mockReset();
  });

  it("detects percPage", () => {
    expect(isExplorerPageType("percPage")).toBe(true);
    expect(isExplorerPageType("perc_page")).toBe(true);
    expect(isExplorerPageType("rffEvent")).toBe(false);
  });

  it("reads site name from CMS folder paths", () => {
    expect(siteNameFromFolderPath("/Sites/Demo/Home")).toBe("Demo");
    expect(siteNameFromFolderPath("//Sites/Demo")).toBe("Demo");
    expect(siteNameFromFolderPath("/Folders/Assets")).toBeNull();
  });

  it("maps allowed template refs", () => {
    const rows = templatesFromContentType([
      { name: "t1", label: "Home", guid: { stringValue: "1-101-7" } },
      { name: "t1", guid: { stringValue: "1-101-7" } },
    ]);
    expect(rows).toEqual([{ id: "1-101-7", name: "Home" }]);
  });

  it("prefers content-type allowed templates", async () => {
    vi.mocked(getContentTypeDetail).mockResolvedValue({
      allowedTemplates: [{ name: "t1", label: "Home", guid: { stringValue: "1-101-7" } }],
    });
    const rows = await loadPageTemplates("/Sites/Demo", "percPage");
    expect(rows).toEqual([{ id: "1-101-7", name: "Home" }]);
    expect(fetchTemplatesForSite).not.toHaveBeenCalled();
  });

  it("falls back to site templates when the type has none", async () => {
    vi.mocked(getContentTypeDetail).mockResolvedValue({ allowedTemplates: [] });
    vi.mocked(fetchTemplatesForSite).mockResolvedValue([
      { id: "site-1", name: "Base" },
    ]);
    const rows = await loadPageTemplates("/Sites/Demo/Home", "percPage");
    expect(rows).toEqual([{ id: "site-1", name: "Base" }]);
    expect(fetchTemplatesForSite).toHaveBeenCalledWith("Demo");
  });
});
