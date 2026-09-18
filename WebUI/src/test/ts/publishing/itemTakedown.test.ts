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
import { resolvePublishKind } from "@/contentExplorer/itemPublish";
import {
  itemTakedownShellHref,
  mapTakedownKind,
  pathItemForTakedown,
  spaItemTakedownHref,
} from "@/publishing/itemTakedown";

describe("item takedown hrefs", () => {
  it("builds sites-workspace deep links with siteId and itemId", () => {
    expect(itemTakedownShellHref({ siteId: "9", itemId: "42" })).toBe(
      "/cm/app/publish/sites?siteId=9&itemId=42",
    );
    expect(spaItemTakedownHref({ siteId: "9", itemId: "42" })).toContain(
      "entry=publish",
    );
    expect(spaItemTakedownHref({ siteId: "9", itemId: "42" })).toContain(
      "section=sites",
    );
    expect(spaItemTakedownHref({ siteId: "9", itemId: "42" })).toContain(
      "itemId=42",
    );
  });

  it("drops unsafe ids", () => {
    expect(itemTakedownShellHref({ itemId: "<script>" })).toBe(
      "/cm/app/publish/sites",
    );
  });
});

describe("mapTakedownKind", () => {
  it("maps asset aliases to resource and defaults to page", () => {
    expect(mapTakedownKind("resource")).toBe("resource");
    expect(mapTakedownKind("Asset")).toBe("resource");
    expect(mapTakedownKind("page")).toBe("page");
    expect(mapTakedownKind("")).toBe("page");
    expect(mapTakedownKind("nope")).toBe("page");
  });
});

describe("pathItemForTakedown", () => {
  it("classifies page vs asset for the shared takedown contract", () => {
    expect(resolvePublishKind(pathItemForTakedown("42", "page"))).toBe("page");
    expect(resolvePublishKind(pathItemForTakedown("99", "resource"))).toBe(
      "asset",
    );
  });
});
