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
  itemStageShellHref,
  mapStageAction,
  mapStageKind,
  pathItemForStage,
  spaItemStageHref,
} from "@/publishing/itemStage";

describe("item stage hrefs", () => {
  it("builds sites-workspace deep links with siteId and itemId", () => {
    expect(itemStageShellHref({ siteId: "9", itemId: "42" })).toBe(
      "/cm/app/publish/sites?siteId=9&itemId=42",
    );
    expect(spaItemStageHref({ siteId: "9", itemId: "42" })).toContain(
      "entry=publish",
    );
    expect(spaItemStageHref({ siteId: "9", itemId: "42" })).toContain(
      "section=sites",
    );
    expect(spaItemStageHref({ siteId: "9", itemId: "42" })).toContain(
      "itemId=42",
    );
  });

  it("includes the action query when present", () => {
    expect(
      itemStageShellHref({ siteId: "9", itemId: "42", action: "unstage" }),
    ).toContain("action=unstage");
    expect(
      spaItemStageHref({ siteId: "9", itemId: "42", action: "stage" }),
    ).toContain("action=stage");
  });

  it("drops unsafe ids", () => {
    expect(itemStageShellHref({ itemId: "<script>" })).toBe(
      "/cm/app/publish/sites",
    );
  });
});

describe("mapStageKind", () => {
  it("maps asset aliases to resource and defaults to page", () => {
    expect(mapStageKind("resource")).toBe("resource");
    expect(mapStageKind("Asset")).toBe("resource");
    expect(mapStageKind("page")).toBe("page");
    expect(mapStageKind("")).toBe("page");
    expect(mapStageKind("nope")).toBe("page");
  });
});

describe("mapStageAction", () => {
  it("defaults unknown / blank values to stage", () => {
    expect(mapStageAction("")).toBe("stage");
    expect(mapStageAction(null)).toBe("stage");
    expect(mapStageAction("nope")).toBe("stage");
  });

  it("maps unstage and remove_from_staging aliases", () => {
    expect(mapStageAction("unstage")).toBe("unstage");
    expect(mapStageAction("Unstage")).toBe("unstage");
    expect(mapStageAction("remove_from_staging")).toBe("unstage");
  });

  it("treats stage explicitly", () => {
    expect(mapStageAction("stage")).toBe("stage");
    expect(mapStageAction("Stage")).toBe("stage");
  });
});

describe("pathItemForStage", () => {
  it("classifies page vs asset for the shared stage contract", () => {
    expect(resolvePublishKind(pathItemForStage("42", "page"))).toBe("page");
    expect(resolvePublishKind(pathItemForStage("99", "resource"))).toBe(
      "asset",
    );
  });
});
