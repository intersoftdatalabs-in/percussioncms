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
import {
  bindExplorerPathItemId,
  parseExplorerContentId,
  unwrapExplorerWireId,
} from "../../../main/ts/api/contentExplorer/pathItemId";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";

describe("parseExplorerContentId", () => {
  it("parses finite numeric ids and rejects junk", () => {
    expect(parseExplorerContentId("42")).toBe(42);
    expect(parseExplorerContentId(7)).toBe(7);
    expect(parseExplorerContentId("1-101-708")).toBe(708);
    expect(parseExplorerContentId("16777215-101-551")).toBe(551);
    expect(parseExplorerContentId(undefined)).toBeNull();
    expect(parseExplorerContentId("")).toBeNull();
    expect(parseExplorerContentId("nope")).toBeNull();
    expect(parseExplorerContentId("0")).toBeNull();
    expect(parseExplorerContentId("ci-home")).toBeNull();
  });

  it("does not treat timestamped asset names as GUIDs (#3811)", () => {
    expect(
      parseExplorerContentId("New-percSimpleTextAsset-20260820165542"),
    ).toBeNull();
    expect(parseExplorerContentId("p-1")).toBeNull();
    expect(parseExplorerContentId("theme.css")).toBeNull();
    expect(parseExplorerContentId(20260820165542)).toBeNull();
  });

  it("unwraps Jackson GUID objects (#3546)", () => {
    expect(parseExplorerContentId({ stringValue: "1-101-708" })).toBe(708);
    expect(
      parseExplorerContentId({ hostId: 1, type: 101, uuid: 708 }),
    ).toBe(708);
    expect(parseExplorerContentId({ id: 42 })).toBe(42);
  });
});

describe("unwrapExplorerWireId", () => {
  it("returns scalars and GUID object forms", () => {
    expect(unwrapExplorerWireId("1-101-708")).toBe("1-101-708");
    expect(unwrapExplorerWireId(42)).toBe(42);
    expect(unwrapExplorerWireId({ stringValue: "1-101-9" })).toBe("1-101-9");
    expect(unwrapExplorerWireId({ hostId: 0, type: 101, uuid: 3 })).toBe(
      "0-101-3",
    );
    expect(unwrapExplorerWireId(null)).toBeUndefined();
    expect(unwrapExplorerWireId({})).toBeUndefined();
  });

  it("does not build a composite GUID when a part is blank (#3557)", () => {
    expect(
      unwrapExplorerWireId({ hostId: "host", type: "", uuid: "uuid" }),
    ).toBeUndefined();
    expect(
      unwrapExplorerWireId({ hostId: 1, type: 101, uuid: "   " }),
    ).toBeUndefined();
    expect(
      unwrapExplorerWireId({ hostId: "", type: "101", uuid: "708" }),
    ).toBeUndefined();
  });
});

describe("bindExplorerPathItemId (#3546)", () => {
  const page = (over: Partial<PSPathItem>): PSPathItem => ({
    name: "Home",
    path: "/Sites/Corporate_Investments/Pages/Home",
    type: "rffHome",
    category: "PAGE",
    ...over,
  });

  it("keeps a GUID-shaped id", () => {
    const item = page({ id: "1-101-708" });
    expect(bindExplorerPathItemId(item)).toBe(item);
    expect(parseExplorerContentId(item.id)).toBe(708);
  });

  it("binds sys_contentid when id is omitted or a slug", () => {
    const omitted = bindExplorerPathItemId(
      page({
        displayProperties: { sys_contentid: "708" },
      }),
    );
    expect(omitted.id).toBe("708");
    expect(parseExplorerContentId(omitted.id)).toBe(708);

    const slug = bindExplorerPathItemId(
      page({
        id: "ci-home",
        displayProperties: { sys_contentid: 708 },
      }),
    );
    expect(slug.id).toBe("708");
    expect(parseExplorerContentId(slug.id)).toBe(708);
  });

  it("unwraps a GUID object id", () => {
    const bound = bindExplorerPathItemId(
      page({
        id: { stringValue: "1-101-708" } as unknown as string,
      }),
    );
    expect(bound.id).toBe("1-101-708");
    expect(parseExplorerContentId(bound.id)).toBe(708);
  });

  it("leaves a folder slug unchanged when nothing parseable is present", () => {
    const folder: PSPathItem = {
      id: "Corporate_Investments",
      name: "Corporate_Investments",
      path: "/Sites/Corporate_Investments/",
      type: "site",
    };
    const bound = bindExplorerPathItemId(folder);
    expect(bound.id).toBe("Corporate_Investments");
    expect(parseExplorerContentId(bound.id)).toBeNull();
  });

  it("does not overwrite an object id with an unparseable unwrap (#3557)", () => {
    const item = page({
      id: { stringValue: "abc" } as unknown as string,
    });
    const bound = bindExplorerPathItemId(item);
    expect(bound).toBe(item);
    expect(bound.id).toEqual({ stringValue: "abc" });
  });

  it("keeps the original item when id already matches as string/number (#3557)", () => {
    const item = page({ id: 708 as unknown as string });
    const bound = bindExplorerPathItemId(item);
    expect(bound).toBe(item);
  });

  it("binds sys_contentid when id is a timestamped asset title (#3811)", () => {
    const asset: PSPathItem = {
      id: "New-percSimpleTextAsset-20260820165542",
      name: "New-percSimpleTextAsset-20260820165542",
      path: "/Assets/uploads/New-percSimpleTextAsset-20260820165542",
      type: "percSimpleTextAsset",
      category: "ASSET",
      displayProperties: { sys_contentid: "708" },
    };
    const bound = bindExplorerPathItemId(asset);
    expect(bound.id).toBe("708");
    expect(parseExplorerContentId(bound.id)).toBe(708);
  });
});
