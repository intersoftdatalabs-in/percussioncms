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
import * as client from "../../../../main/ts/api/client";
import {
  coerceDisplayString,
  listSites,
  parseSiteList,
} from "../../../../main/ts/api/developer/sitesApi";

vi.mock("../../../../main/ts/api/client", () => ({
  get: vi.fn(),
}));

const get = client.get as ReturnType<typeof vi.fn>;

describe("parseSiteList (#3198 bind)", () => {
  it("returns bare arrays", () => {
    const rows = [{ name: "Help" }];
    expect(parseSiteList(rows)).toEqual(rows);
  });

  it("returns empty for null", () => {
    expect(parseSiteList(null)).toEqual([]);
    expect(parseSiteList(undefined)).toEqual([]);
  });

  it("unwraps SiteList root wrapper", () => {
    expect(
      parseSiteList({
        SiteList: [
          { name: "Help", description: "docs" },
          { name: "Corp" },
        ],
      }),
    ).toEqual([
      { name: "Help", description: "docs" },
      { name: "Corp" },
    ]);
  });

  it("unwraps nested SiteList / Site envelopes", () => {
    expect(
      parseSiteList({
        SiteList: {
          Site: [{ name: "Help" }],
        },
      }),
    ).toEqual([{ name: "Help" }]);
  });

  it("unwraps per-item Site wrap", () => {
    expect(
      parseSiteList({
        SiteList: [{ Site: { name: "Help", baseUrl: "https://h.example" } }],
      }),
    ).toEqual([{ name: "Help", baseUrl: "https://h.example" }]);
  });

  it("unwraps a single Site object", () => {
    expect(parseSiteList({ Site: { name: "Only" } })).toEqual([{ name: "Only" }]);
  });

  it("does not treat a nested metadata bag as a Site row", () => {
    expect(() =>
      parseSiteList({
        Site: { metadata: { name: "config", env: "qa" } },
      }),
    ).toThrow(/Unexpected site list payload/);
  });

  it("still binds a name-only Site summary", () => {
    expect(parseSiteList({ Site: { name: "Only" } })).toEqual([{ name: "Only" }]);
  });

  it("synthesizes guid.stringValue from host/type/uuid parts (#3203)", () => {
    expect(
      parseSiteList({
        Site: { name: "Help", guid: { hostId: 0, type: 20, uuid: 301 } },
      }),
    ).toEqual([
      {
        name: "Help",
        guid: { hostId: 0, type: 20, uuid: 301, stringValue: "0-20-301" },
      },
    ]);
  });

  it("keeps existing guid.stringValue", () => {
    expect(
      parseSiteList([{ name: "Help", guid: { stringValue: "0-20-1" } }]),
    ).toEqual([{ name: "Help", guid: { stringValue: "0-20-1" } }]);
  });

  it("throws on unknown object shape", () => {
    expect(() => parseSiteList({ unexpected: true })).toThrow(
      /Unexpected site list payload/,
    );
  });

  it("throws on non-object non-array types", () => {
    expect(() => parseSiteList("not-json-list")).toThrow(
      /Unexpected site list payload type/,
    );
  });
});

describe("coerceDisplayString", () => {
  it("trims strings and ignores Optional beans", () => {
    expect(coerceDisplayString("  Help  ")).toBe("Help");
    expect(coerceDisplayString({ empty: false })).toBe("");
    expect(coerceDisplayString({ value: "Corp" })).toBe("Corp");
    expect(coerceDisplayString(null)).toBe("");
  });
});

describe("listSites", () => {
  beforeEach(() => {
    get.mockReset();
  });

  it("binds Jackson SiteList wrap to named rows", async () => {
    get.mockResolvedValue({
      SiteList: [{ name: "Help", description: "docs" }],
    });
    const out = await listSites();
    expect(out[0].name).toBe("Help");
    expect(out[0].designGaps?.length).toBeGreaterThan(0);
  });
});
