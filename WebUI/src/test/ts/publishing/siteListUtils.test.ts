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
  filterSitesByName,
  nextViewMode,
  siteKey,
} from "@/publishing/siteListUtils";

describe("filterSitesByName", () => {
  const sites = [
    { name: "Alpha" },
    { name: "Beta Site" },
    { name: "gamma" },
  ];

  it("returns all when filter empty", () => {
    expect(filterSitesByName(sites, "")).toHaveLength(3);
    expect(filterSitesByName(sites, "  ")).toHaveLength(3);
  });

  it("filters case-insensitively", () => {
    expect(filterSitesByName(sites, "beta")).toEqual([{ name: "Beta Site" }]);
    expect(filterSitesByName(sites, "ALPHA")).toEqual([{ name: "Alpha" }]);
    expect(filterSitesByName(sites, "site")).toEqual([{ name: "Beta Site" }]);
  });
});

describe("nextViewMode", () => {
  it("toggles card and list", () => {
    expect(nextViewMode("card")).toBe("list");
    expect(nextViewMode("list")).toBe("card");
  });
});

describe("siteKey", () => {
  it("prefers siteId then id then name", () => {
    expect(siteKey({ name: "n", siteId: "s1" })).toBe("s1");
    expect(siteKey({ name: "n", id: 9 })).toBe("9");
    expect(siteKey({ name: "only" })).toBe("only");
  });
});
