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
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  matchesAllowedTypes,
  passesFilters,
} from "../../../main/ts/contentBrowser/passesFilters";

function item(
  type: string,
  extra?: Partial<PSPathItem>,
): PSPathItem {
  return {
    id: extra?.id ?? "1",
    name: extra?.name ?? "row",
    path: extra?.path ?? "/Assets/row",
    type,
    category: extra?.category,
    ...extra,
  };
}

describe("matchesAllowedTypes (#3714)", () => {
  it("allows any item when the host lists no types", () => {
    expect(matchesAllowedTypes(item("Image"), null)).toBe(true);
    expect(matchesAllowedTypes(item("folder"), [])).toBe(true);
  });

  it("maps CMS Image / File / rffImage to host asset", () => {
    const assetOnly = ["asset"];
    expect(matchesAllowedTypes(item("Image"), assetOnly)).toBe(true);
    expect(matchesAllowedTypes(item("File"), assetOnly)).toBe(true);
    expect(matchesAllowedTypes(item("rffImage"), assetOnly)).toBe(true);
    expect(matchesAllowedTypes(item("rffFile"), assetOnly)).toBe(true);
    expect(matchesAllowedTypes(item("percAsset"), assetOnly)).toBe(true);
  });

  it("maps percPage / page / rffHome to host page", () => {
    const pageOnly = ["page"];
    expect(matchesAllowedTypes(item("percPage"), pageOnly)).toBe(true);
    expect(matchesAllowedTypes(item("page"), pageOnly)).toBe(true);
    expect(matchesAllowedTypes(item("rffHome"), pageOnly)).toBe(true);
    expect(
      matchesAllowedTypes(item("CustomLanding", { category: "LANDING_PAGE" }), pageOnly),
    ).toBe(true);
  });

  it("accepts Image when the asset picker allows page and asset", () => {
    const picker = ["page", "asset"];
    expect(matchesAllowedTypes(item("Image"), picker)).toBe(true);
    expect(matchesAllowedTypes(item("percPage"), picker)).toBe(true);
    expect(matchesAllowedTypes(item("rffImage"), picker)).toBe(true);
    expect(matchesAllowedTypes(item("File"), picker)).toBe(true);
    expect(
      matchesAllowedTypes(
        item("NewsArticle", { path: "/Sites/Demo/News/Q3" }),
        picker,
      ),
    ).toBe(true);
  });

  it("rejects folders and nav types for page/asset hosts", () => {
    const picker = ["page", "asset"];
    expect(matchesAllowedTypes(item("folder"), picker)).toBe(false);
    expect(matchesAllowedTypes(item("Folder"), picker)).toBe(false);
    expect(matchesAllowedTypes(item("site"), picker)).toBe(false);
    expect(matchesAllowedTypes(item("rffNavon"), picker)).toBe(false);
    expect(matchesAllowedTypes(item("rffNavTree"), picker)).toBe(false);
  });

  it("does not treat Image as a page-only pick", () => {
    expect(matchesAllowedTypes(item("Image"), ["page"])).toBe(false);
    expect(matchesAllowedTypes(item("rffImage"), ["page"])).toBe(false);
    expect(matchesAllowedTypes(item("File"), ["page"])).toBe(false);
  });

  it("does not treat percPage as an asset-only pick", () => {
    expect(matchesAllowedTypes(item("percPage"), ["asset"])).toBe(false);
    expect(matchesAllowedTypes(item("page"), ["asset"])).toBe(false);
  });

  it("keeps exact CMS type matches when the host lists that name", () => {
    expect(matchesAllowedTypes(item("Image"), ["Image"])).toBe(true);
    expect(matchesAllowedTypes(item("percPage"), ["percPage"])).toBe(true);
  });
});

describe("passesFilters", () => {
  it("still applies allowedCategories as an exact match", () => {
    expect(
      passesFilters(item("Image", { category: "ASSET" }), ["page", "asset"], ["asset"]),
    ).toBe(true);
    expect(
      passesFilters(item("Image", { category: "ASSET" }), ["page", "asset"], ["page"]),
    ).toBe(false);
  });

  it("passes Image with no category when allowedTypes is page+asset", () => {
    expect(passesFilters(item("Image"), ["page", "asset"], null)).toBe(true);
  });
});
