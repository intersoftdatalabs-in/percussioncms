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

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  buildSiteCopyRequestBody,
  copyManagedSite,
  deleteManagedSite,
  isSiteBeingImported,
  isSiteCopyInProgress,
  loadSiteCopyInfo,
  normalizeCopyAssetFolder,
  siteCopyInfoUrl,
  siteCopyUrl,
  siteDeleteUrl,
  siteImportingUrl,
  suggestCopySiteName,
} from "../../../../main/ts/api/architecture/siteAdminApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("siteAdminApi (#3303)", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("builds copy/delete/import URLs", () => {
    expect(siteCopyUrl()).toBe(`${PATHS.SITES_ALL}/copy`);
    expect(siteCopyInfoUrl()).toBe(`${PATHS.SITES_ALL}/copysiteinfo`);
    expect(siteDeleteUrl("Demo Site")).toBe(
      `${PATHS.SITES_ALL}/${encodeURIComponent("Demo Site")}`,
    );
    expect(siteImportingUrl("Acme")).toBe(
      `${PATHS.SITES_ALL}/isSiteImporting/Acme`,
    );
    expect(() => siteDeleteUrl("  ")).toThrow(/name/i);
  });

  it("maps explorer wizard fields onto SiteCopyRequest wire body", () => {
    expect(
      buildSiteCopyRequestBody({
        sourceSite: "Demo",
        targetSite: "Demo-copy",
        targetFolder: "/",
      }),
    ).toEqual({
      SiteCopyRequest: { srcSite: "Demo", copySite: "Demo-copy" },
    });
    expect(
      buildSiteCopyRequestBody({
        srcSite: "A",
        copySite: "B",
        assetFolder: "Shared",
      }),
    ).toEqual({
      SiteCopyRequest: { srcSite: "A", copySite: "B", assetFolder: "Shared" },
    });
    expect(() =>
      buildSiteCopyRequestBody({ sourceSite: "", targetSite: "X" }),
    ).toThrow(/source/i);
    expect(() =>
      buildSiteCopyRequestBody({ srcSite: "A", copySite: "  " }),
    ).toThrow(/copy site/i);
  });

  it("normalizes asset folders and copy name suggestion", () => {
    expect(normalizeCopyAssetFolder("/")).toBeUndefined();
    expect(normalizeCopyAssetFolder("  ")).toBeUndefined();
    expect(normalizeCopyAssetFolder("Assets/x")).toBe("Assets/x");
    expect(suggestCopySiteName(" Demo ")).toBe("Demo-copy");
    expect(suggestCopySiteName("")).toBe("");
  });

  it("detects copy-in-progress from copysiteinfo envelopes", () => {
    expect(isSiteCopyInProgress(null)).toBe(false);
    expect(isSiteCopyInProgress({})).toBe(false);
    expect(isSiteCopyInProgress({ entries: {} })).toBe(false);
    expect(isSiteCopyInProgress({ psmap: { entries: {} } })).toBe(false);
    expect(isSiteCopyInProgress({ psmap: { entries: [] } })).toBe(false);
    expect(
      isSiteCopyInProgress({ psmap: { entries: { src: "Demo" } } }),
    ).toBe(true);
    expect(isSiteCopyInProgress({ entries: [{ k: "v" }] })).toBe(true);
    expect(isSiteCopyInProgress({ PSMapWrapper: { Entries: { a: 1 } } })).toBe(
      true,
    );
  });

  it("copyManagedSite POSTs wrapped SiteCopyRequest", async () => {
    const spy = vi.spyOn(client, "post").mockResolvedValue({ name: "B" });
    await copyManagedSite({ srcSite: "A", copySite: "B" });
    expect(spy).toHaveBeenCalledWith(`${PATHS.SITES_ALL}/copy`, {
      SiteCopyRequest: { srcSite: "A", copySite: "B" },
    });
  });

  it("deleteManagedSite DELETEs the site name path", async () => {
    const spy = vi.spyOn(client, "del").mockResolvedValue(undefined);
    await deleteManagedSite("Demo");
    expect(spy).toHaveBeenCalledWith(`${PATHS.SITES_ALL}/Demo`);
  });

  it("loadSiteCopyInfo and isSiteBeingImported call GET", async () => {
    const spy = vi
      .spyOn(client, "get")
      .mockResolvedValueOnce({ entries: {} })
      .mockResolvedValueOnce("true");
    await expect(loadSiteCopyInfo()).resolves.toEqual({ entries: {} });
    await expect(isSiteBeingImported("Demo")).resolves.toBe(true);
    expect(spy).toHaveBeenNthCalledWith(1, `${PATHS.SITES_ALL}/copysiteinfo`);
    expect(spy).toHaveBeenNthCalledWith(
      2,
      `${PATHS.SITES_ALL}/isSiteImporting/Demo`,
    );
  });

  it("isSiteBeingImported treats false/empty as not importing", async () => {
    vi.spyOn(client, "get").mockResolvedValue("false");
    await expect(isSiteBeingImported("Demo")).resolves.toBe(false);
    vi.spyOn(client, "get").mockResolvedValue(false);
    await expect(isSiteBeingImported("Demo")).resolves.toBe(false);
  });
});
