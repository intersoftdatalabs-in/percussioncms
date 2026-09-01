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
  normalizeDesignObjectGuid,
  normalizeDisplayFormatGuid,
  objectGuidString,
  resolveActionMenuObjectGuid,
  resolveCommunityObjectGuid,
  resolveContentTypeObjectGuid,
  resolveTemplateObjectGuid,
  resolveViewObjectGuid,
  synthesizeTypedObjectGuid,
  unwrapDisplayFormat,
} from "../../../main/ts/api/displayFormatGuid";

describe("objectGuidString", () => {
  it("reads stringValue when present", () => {
    expect(objectGuidString({ stringValue: "0-11-5" })).toBe("0-11-5");
  });

  it("accepts plain string GUID", () => {
    expect(objectGuidString(" 0-11-7 ")).toBe("0-11-7");
  });

  it("synthesizes host-type-uuid when stringValue missing (#2951)", () => {
    expect(
      objectGuidString({ hostId: 0, type: 11, uuid: 5, longValue: 5 }),
    ).toBe("0-11-5");
  });

  it("unwraps nested Guid envelope", () => {
    expect(
      objectGuidString({ Guid: { stringValue: "0-11-9", type: 11, uuid: 9 } }),
    ).toBe("0-11-9");
  });

  it("returns undefined for empty payloads", () => {
    expect(objectGuidString(null)).toBeUndefined();
    expect(objectGuidString(undefined)).toBeUndefined();
    expect(objectGuidString({})).toBeUndefined();
    expect(objectGuidString("")).toBeUndefined();
  });
});

describe("resolveContentTypeObjectGuid / resolveTemplateObjectGuid (#3319)", () => {
  it("prefers nested guid then guidString then catalog then type-uuid", () => {
    expect(
      resolveContentTypeObjectGuid({ guid: { stringValue: "0-2-1" }, guidString: "0-2-9" }),
    ).toBe("0-2-1");
    expect(resolveContentTypeObjectGuid({ guidString: "0-2-9" })).toBe("0-2-9");
    expect(resolveContentTypeObjectGuid({}, "0-2-3")).toBe("0-2-3");
    expect(resolveContentTypeObjectGuid({ guid: { uuid: 5 } })).toBe("0-2-5");
  });

  it("synthesizes template GUID from templateId", () => {
    expect(resolveTemplateObjectGuid({ templateId: 12 })).toBe("0-4-12");
    expect(resolveTemplateObjectGuid({ guidString: "0-4-8", templateId: 12 })).toBe("0-4-8");
    expect(synthesizeTypedObjectGuid(4, 12)).toBe("0-4-12");
    expect(synthesizeTypedObjectGuid(2, 0)).toBeUndefined();
    expect(synthesizeTypedObjectGuid(13, "9")).toBe("0-13-9");
    expect(synthesizeTypedObjectGuid(13, "01")).toBeUndefined();
    expect(synthesizeTypedObjectGuid(13, "1.5")).toBeUndefined();
    expect(synthesizeTypedObjectGuid(13, "1e2")).toBeUndefined();
    expect(resolveCommunityObjectGuid({ id: "01" as unknown as number })).toBeUndefined();
  });

  it("resolves action menu GUID: nested, guidString, catalog, then 0-107-{id} (#3380)", () => {
    expect(
      resolveActionMenuObjectGuid({ guid: { stringValue: "0-107-1" }, id: 9 }),
    ).toBe("0-107-1");
    expect(resolveActionMenuObjectGuid({ guidString: "0-107-2", id: 9 })).toBe("0-107-2");
    expect(resolveActionMenuObjectGuid({ id: 9 }, " 0-107-3 ")).toBe("0-107-3");
    expect(resolveActionMenuObjectGuid({ id: 9 })).toBe("0-107-9");
    expect(
      resolveActionMenuObjectGuid({ guid: { Guid: { stringValue: "0-107-4" } } }),
    ).toBe("0-107-4");
    expect(resolveActionMenuObjectGuid({}, undefined)).toBeUndefined();
  });

  it("resolves community GUID: nested, guidString, catalog, then 0-13-{id} (#4077)", () => {
    expect(
      resolveCommunityObjectGuid({ guid: { stringValue: "0-13-1" }, id: 9 }),
    ).toBe("0-13-1");
    expect(resolveCommunityObjectGuid({ guidString: "0-13-2", id: 9 })).toBe("0-13-2");
    expect(resolveCommunityObjectGuid({ id: 9 }, " 0-13-3 ")).toBe("0-13-3");
    expect(resolveCommunityObjectGuid({ id: 9 })).toBe("0-13-9");
    expect(
      resolveCommunityObjectGuid({ guid: { Guid: { stringValue: "0-13-4" } } }),
    ).toBe("0-13-4");
    expect(resolveCommunityObjectGuid({}, undefined)).toBeUndefined();
  });

  it("resolves view GUID: nested, guidString, catalog, then 0-18-{id} (#3380)", () => {
    expect(resolveViewObjectGuid({ guid: { stringValue: "0-18-1" }, id: 9 })).toBe("0-18-1");
    expect(resolveViewObjectGuid({ guidString: "0-18-2", id: 9 })).toBe("0-18-2");
    expect(resolveViewObjectGuid({ id: 9 }, " 0-18-3 ")).toBe("0-18-3");
    expect(resolveViewObjectGuid({ id: 9 })).toBe("0-18-9");
    expect(
      resolveViewObjectGuid({ guid: { hostId: 0, type: 18, uuid: 5 } }),
    ).toBe("0-18-5");
    expect(resolveViewObjectGuid({}, undefined)).toBeUndefined();
  });

  it("normalizeDesignObjectGuid fills stringValue and guidString", () => {
    const out = normalizeDesignObjectGuid({
      guid: { hostId: 0, type: 2, uuid: 301 },
    });
    expect(out.guid?.stringValue).toBe("0-2-301");
    expect(out.guidString).toBe("0-2-301");
  });
});

describe("normalizeDisplayFormatGuid", () => {
  it("fills stringValue from parts without dropping other fields", () => {
    const out = normalizeDisplayFormatGuid({
      name: "By_Author",
      guid: { hostId: 0, type: 11, uuid: 301 },
    });
    expect(out.guid?.stringValue).toBe("0-11-301");
    expect(out.guid?.hostId).toBe(0);
    expect(out.guid?.type).toBe(11);
    expect(out.guid?.uuid).toBe(301);
  });

  it("returns same reference when stringValue and guidString already match", () => {
    const df = {
      name: "x",
      guid: { stringValue: "0-11-1", uuid: 1 },
      guidString: "0-11-1",
    };
    expect(normalizeDisplayFormatGuid(df)).toBe(df);
  });

  it("fills guidString from guid when companion is missing (#3200)", () => {
    const out = normalizeDisplayFormatGuid({
      name: "x",
      guid: { stringValue: "0-11-1", uuid: 1 },
    });
    expect(out.guidString).toBe("0-11-1");
    expect(out.guid?.stringValue).toBe("0-11-1");
  });
});

describe("unwrapDisplayFormat", () => {
  it("unwraps Jackson DisplayFormat root envelope so guid is reachable (#2689)", () => {
    const unwrapped = unwrapDisplayFormat({
      DisplayFormat: {
        name: "By_Author",
        label: "By Author",
        guid: { stringValue: "0-11-5", type: 11, uuid: 5 },
        columns: [{ source: "sys_title" }],
      },
    });
    expect(unwrapped.name).toBe("By_Author");
    expect(unwrapped.guid?.stringValue).toBe("0-11-5");
    expect(unwrapped.columns).toHaveLength(1);
  });

  it("synthesizes guid.stringValue from numeric parts under root wrap (#2951)", () => {
    const unwrapped = unwrapDisplayFormat({
      DisplayFormat: {
        name: "By_Author",
        guid: { hostId: 0, type: 11, uuid: 301, longValue: 301 },
      },
    });
    expect(unwrapped.name).toBe("By_Author");
    expect(unwrapped.guid?.stringValue).toBe("0-11-301");
  });

  it("unwraps camelCase displayFormat envelope", () => {
    const unwrapped = unwrapDisplayFormat({
      displayFormat: {
        name: "Default",
        guid: { stringValue: "0-11-1" },
      },
    });
    expect(unwrapped.name).toBe("Default");
    expect(unwrapped.guid?.stringValue).toBe("0-11-1");
  });

  it("passes through flat payloads without wrap", () => {
    const flat = {
      name: "FolderList",
      guid: { stringValue: "0-11-2" },
    };
    expect(unwrapDisplayFormat(flat)).toEqual({
      ...flat,
      guidString: "0-11-2",
    });
  });

  it("takes first element when envelope holds a singleton array", () => {
    const unwrapped = unwrapDisplayFormat({
      DisplayFormat: [{ name: "only", guid: { stringValue: "0-11-9" } }],
    });
    expect(unwrapped.name).toBe("only");
    expect(unwrapped.guid?.stringValue).toBe("0-11-9");
  });

  it("returns empty object for null/non-object payloads", () => {
    expect(unwrapDisplayFormat(null)).toEqual({});
    expect(unwrapDisplayFormat(undefined)).toEqual({});
    expect(unwrapDisplayFormat("x")).toEqual({});
    expect(unwrapDisplayFormat([])).toEqual({});
  });
});
