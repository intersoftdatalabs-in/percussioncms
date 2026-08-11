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
  getDisplayFormatDetail,
  listDisplayFormats,
  normalizeDisplayFormatColumns,
  objectGuidString,
  unwrapDisplayFormat,
} from "../../../main/ts/api/contentExplorer/displayFormatsApi";
import { mockFetch } from "./setup";

/**
 * Content Explorer re-exports the shared GUID helpers — keep a dedicated
 * describe so CE and Developer cannot diverge without a red test (#2951 review).
 */
describe("objectGuidString (contentExplorer re-export)", () => {
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

describe("displayFormatsApi", () => {
  it("listDisplayFormats unwraps array envelope and passes filters", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      expect(url).toContain("/displayformats");
      expect(url).toContain("validForFolder=true");
      return new Response(
        JSON.stringify({
          DisplayFormat: [
            { name: "FolderList", displayId: 1, validForFolder: true },
          ],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    const list = await listDisplayFormats({ validForFolder: true });
    expect(list).toHaveLength(1);
    expect(list[0].name).toBe("FolderList");
  });

  it("normalizeDisplayFormatColumns unwraps nested envelope", () => {
    expect(
      normalizeDisplayFormatColumns({
        DisplayFormatColumn: [{ source: "sys_title" }, { source: "type" }],
      }),
    ).toHaveLength(2);
    expect(normalizeDisplayFormatColumns(undefined)).toEqual([]);
  });

  it("unwrapDisplayFormat exposes guid under Jackson root wrap (#2689)", () => {
    const df = unwrapDisplayFormat({
      DisplayFormat: {
        name: "By_Author",
        guid: { stringValue: "0-11-5" },
      },
    });
    expect(df.name).toBe("By_Author");
    expect(df.guid?.stringValue).toBe("0-11-5");
  });

  it("unwrapDisplayFormat synthesizes stringValue from guid parts (#2951)", () => {
    const df = unwrapDisplayFormat({
      DisplayFormat: {
        name: "By_Author",
        guid: { hostId: 0, type: 11, uuid: 5 },
      },
    });
    expect(df.guid?.stringValue).toBe("0-11-5");
  });

  it("getDisplayFormatDetail unwraps wrapped body", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      expect(url).toContain("/displayformats/By_Author");
      return new Response(
        JSON.stringify({
          DisplayFormat: {
            name: "By_Author",
            guid: { stringValue: "0-11-5" },
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    const detail = await getDisplayFormatDetail("By_Author");
    expect(detail.name).toBe("By_Author");
    expect(detail.guid?.stringValue).toBe("0-11-5");
  });
});
