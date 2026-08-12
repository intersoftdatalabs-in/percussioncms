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

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  getDisplayFormatDetail,
  listDisplayFormats,
  objectGuidString,
  resolveDisplayFormatObjectGuid,
  unwrapDisplayFormat,
  unwrapDisplayFormatList,
} from "../../../../main/ts/api/developer/displayFormatsApi";

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

  it("reads Optional-like and snake_case stringValue (#3200)", () => {
    expect(objectGuidString({ stringValue: { value: "0-31-8" } })).toBe("0-31-8");
    expect(objectGuidString({ string_value: "0-31-7" })).toBe("0-31-7");
  });
});

describe("resolveDisplayFormatObjectGuid", () => {
  it("prefers nested guid then guidString then catalog then displayId (#3200)", () => {
    expect(
      resolveDisplayFormatObjectGuid({ guid: { stringValue: "0-31-1" }, displayId: 9 }),
    ).toBe("0-31-1");
    expect(resolveDisplayFormatObjectGuid({ guidString: "0-31-2", displayId: 9 })).toBe("0-31-2");
    expect(resolveDisplayFormatObjectGuid({ displayId: 9 }, " 0-31-3 ")).toBe("0-31-3");
    expect(resolveDisplayFormatObjectGuid({ displayId: 9 })).toBe("0-31-9");
    expect(resolveDisplayFormatObjectGuid({}, undefined)).toBeUndefined();
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

  it("copies guidString onto guid.stringValue (#3200)", () => {
    const unwrapped = unwrapDisplayFormat({
      DisplayFormat: { name: "By_Author", guidString: "0-31-4" },
    });
    expect(unwrapped.guidString).toBe("0-31-4");
    expect(unwrapped.guid?.stringValue).toBe("0-31-4");
  });
});

describe("unwrapDisplayFormatList", () => {
  it("flattens nested DisplayFormatList envelope (#3200)", () => {
    const list = unwrapDisplayFormatList({
      DisplayFormatList: {
        DisplayFormat: [
          { name: "By_Author", guid: { hostId: 0, type: 31, uuid: 5 } },
          { name: "Default", displayId: 2 },
        ],
      },
    });
    expect(list).toHaveLength(2);
    expect(list[0].name).toBe("By_Author");
    expect(list[0].guid?.stringValue).toBe("0-31-5");
    expect(list[1].guid?.stringValue).toBe("0-31-2");
  });
});

describe("getDisplayFormatDetail", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("unwraps wrapped REST body before returning detail", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        return new Response(
          JSON.stringify({
            DisplayFormat: {
              name: "By_Author",
              guid: { stringValue: "0-11-5" },
            },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }),
    );

    const detail = await getDisplayFormatDetail("By_Author");
    expect(detail.name).toBe("By_Author");
    expect(detail.guid?.stringValue).toBe("0-11-5");
    expect(fetch).toHaveBeenCalled();
    const calledUrl = String((fetch as ReturnType<typeof vi.fn>).mock.calls[0][0]);
    expect(calledUrl).toContain("/displayformats/");
    expect(calledUrl).toContain("By_Author");
  });

  it("fills stringValue when detail guid has only parts (#2951)", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        return new Response(
          JSON.stringify({
            DisplayFormat: {
              name: "By_Author",
              guid: { hostId: 0, type: 11, uuid: 5 },
            },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }),
    );

    const detail = await getDisplayFormatDetail("By_Author");
    expect(detail.guid?.stringValue).toBe("0-11-5");
  });
});

describe("listDisplayFormats", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("normalizes guid on each list item", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        return new Response(
          JSON.stringify({
            DisplayFormat: [
              { name: "By_Author", guid: { hostId: 0, type: 11, uuid: 5 } },
            ],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }),
    );

    const list = await listDisplayFormats();
    expect(list).toHaveLength(1);
    expect(list[0].name).toBe("By_Author");
    expect(list[0].guid?.stringValue).toBe("0-11-5");
  });
});
