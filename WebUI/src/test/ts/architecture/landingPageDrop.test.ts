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

import { describe, expect, it } from "vitest";
import type { NavTreeNode } from "../../../main/ts/api/architecture/types";
import {
  FINDER_FOLDER_MIME,
  FINDER_ITEM_MIME,
  FINDER_PAGE_MIME,
  canAcceptLandingPageDragOver,
  finderDragMimeForItem,
  mapLandingPageDrop,
  serializeFinderItemDrag,
  siteNameFromItemPath,
} from "../../../main/ts/architecture/landingPageDrop";

function node(
  id: string,
  sectionType: string = "section",
): NavTreeNode {
  return {
    id,
    title: id,
    folderPath: `//Sites/Demo/${id}`,
    sectionType,
    requiresLogin: false,
    children: [],
  };
}

function data(
  map: Record<string, string>,
  types: string[] = Object.keys(map),
) {
  return {
    types,
    getData: (format: string) => map[format] ?? "",
  };
}

const section = node("c1");

describe("landingPageDrop (#3660)", () => {
  it("maps a Finder PAGE drop onto a regular section", () => {
    const payload = serializeFinderItemDrag({
      id: "  page-1  ",
      name: "About",
      path: "//Sites/Demo/About",
      type: "page",
      category: "PAGE",
    });
    expect(
      mapLandingPageDrop(
        data({ [FINDER_PAGE_MIME]: payload }),
        section,
        { selectedSite: "Demo" },
      ),
    ).toEqual({
      ok: true,
      sectionId: "c1",
      pageId: "page-1",
      pageLabel: "About",
    });
  });

  it("accepts FastForward rffHome with category PAGE", () => {
    const payload = serializeFinderItemDrag({
      id: "ci-home",
      name: "Home",
      path: "/Sites/Corporate_Investments/Pages/Home",
      type: "rffHome",
      category: "PAGE",
    });
    expect(
      mapLandingPageDrop(data({ [FINDER_PAGE_MIME]: payload }), section, {
        selectedSite: "Corporate_Investments",
      }),
    ).toEqual({
      ok: true,
      sectionId: "c1",
      pageId: "ci-home",
      pageLabel: "Home",
    });
  });

  it("does not POST for folder, asset, empty, or invalid MIME", () => {
    const folder = serializeFinderItemDrag({
      id: "f1",
      name: "Folder",
      type: "folder",
      category: "folder",
    });
    expect(
      mapLandingPageDrop(
        data({ [FINDER_FOLDER_MIME]: folder }),
        section,
      ),
    ).toEqual({ ok: false, reason: "notPage" });

    const asset = serializeFinderItemDrag({
      id: "a1",
      name: "Image",
      type: "percAsset",
      category: "ASSET",
    });
    expect(
      mapLandingPageDrop(data({ [FINDER_ITEM_MIME]: asset }), section),
    ).toEqual({ ok: false, reason: "notPage" });

    expect(mapLandingPageDrop(data({}, []), section)).toEqual({
      ok: false,
      reason: "empty",
    });

    expect(
      mapLandingPageDrop(
        data({ "text/html": "<div>nope</div>" }, ["text/html"]),
        section,
      ),
    ).toEqual({ ok: false, reason: "invalidMime" });

    expect(
      mapLandingPageDrop(
        data({
          [FINDER_PAGE_MIME]: serializeFinderItemDrag({
            id: "  ",
            type: "page",
          }),
        }),
        section,
      ),
    ).toEqual({ ok: false, reason: "empty" });
  });

  it("skips invalid section types and busy targets", () => {
    const payload = serializeFinderItemDrag({
      id: "p1",
      name: "P",
      type: "page",
    });
    const dt = data({ [FINDER_PAGE_MIME]: payload });
    expect(mapLandingPageDrop(dt, node("l1", "sectionlink"))).toEqual({
      ok: false,
      reason: "invalidTarget",
    });
    expect(mapLandingPageDrop(dt, node("e1", "externallink"))).toEqual({
      ok: false,
      reason: "invalidTarget",
    });
    expect(mapLandingPageDrop(dt, node("b1", "blog"))).toEqual({
      ok: false,
      reason: "invalidTarget",
    });
    expect(mapLandingPageDrop(dt, null)).toEqual({
      ok: false,
      reason: "invalidTarget",
    });
    expect(mapLandingPageDrop(dt, section, { busy: true })).toEqual({
      ok: false,
      reason: "busy",
    });
  });

  it("rejects a page from a different site when path is present", () => {
    const payload = serializeFinderItemDrag({
      id: "p1",
      name: "Other",
      path: "//Sites/OtherSite/Page",
      type: "page",
    });
    expect(
      mapLandingPageDrop(data({ [FINDER_PAGE_MIME]: payload }), section, {
        selectedSite: "Demo",
      }),
    ).toEqual({ ok: false, reason: "wrongSite" });
  });

  it("accepts dragover for page MIME on a regular section without getData", () => {
    const over = {
      types: [FINDER_PAGE_MIME],
      getData: () => "",
    };
    expect(canAcceptLandingPageDragOver(over, section)).toBe(true);
    expect(
      canAcceptLandingPageDragOver(over, node("l1", "sectionlink")),
    ).toBe(false);
    expect(
      canAcceptLandingPageDragOver(
        { types: [FINDER_FOLDER_MIME], getData: () => "" },
        section,
      ),
    ).toBe(false);
    expect(canAcceptLandingPageDragOver(over, section, { busy: true })).toBe(
      false,
    );
  });

  it("classifies Explorer listing MIME from type/category", () => {
    expect(finderDragMimeForItem({ type: "page" })).toBe(FINDER_PAGE_MIME);
    expect(
      finderDragMimeForItem({ type: "rffHome", category: "PAGE" }),
    ).toBe(FINDER_PAGE_MIME);
    expect(finderDragMimeForItem({ type: "folder" })).toBe(FINDER_FOLDER_MIME);
    expect(finderDragMimeForItem({ type: "percAsset" })).toBe(FINDER_ITEM_MIME);
  });

  it("parses site name from Finder / Explorer paths", () => {
    expect(siteNameFromItemPath("//Sites/Demo/About")).toBe("Demo");
    expect(siteNameFromItemPath("/Sites/Corporate_Investments/Pages")).toBe(
      "Corporate_Investments",
    );
    expect(siteNameFromItemPath("")).toBeNull();
  });
});
