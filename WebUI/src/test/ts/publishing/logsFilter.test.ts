/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
  buildLogRequest,
  DEFAULT_LOG_DAYS,
  DEFAULT_LOG_MAXCOUNT,
  LOG_DAYS_OPTIONS,
  LOG_MAXCOUNT_OPTIONS,
} from "@/publishing/logsFilter";
import {
  extractLogItems,
  filterLogItems,
} from "@/publishing/logDetails";

describe("logs filter request builder (OPS-22)", () => {
  it("applies Minuet defaults", () => {
    const req = buildLogRequest({});
    expect(req.days).toBe(DEFAULT_LOG_DAYS);
    expect(req.maxcount).toBe(DEFAULT_LOG_MAXCOUNT);
    expect(req.siteId).toBeUndefined();
    expect(req.pubServerId).toBeUndefined();
  });

  it("includes site and server when provided", () => {
    const req = buildLogRequest({
      siteId: "42",
      pubServerId: "7",
      days: 10,
      maxcount: 50,
    });
    expect(req).toEqual({
      siteId: "42",
      pubServerId: "7",
      days: 10,
      maxcount: 50,
    });
  });

  it("omits blank pubServerId (all servers)", () => {
    const req = buildLogRequest({ siteId: "1", pubServerId: "  " });
    expect(req.pubServerId).toBeUndefined();
    expect(req.siteId).toBe("1");
  });

  it("exposes Minuet day and maxcount option sets", () => {
    expect([...LOG_DAYS_OPTIONS]).toEqual([3, 5, 10]);
    expect([...LOG_MAXCOUNT_OPTIONS]).toEqual([20, 30, 50]);
  });

  it("supports showOnlyFailures", () => {
    const req = buildLogRequest({ showOnlyFailures: true, days: 3 });
    expect(req.showOnlyFailures).toBe(true);
    expect(req.days).toBe(3);
  });
});

describe("structured log details (OPS-23)", () => {
  it("extracts SitePublishItem array from details payload", () => {
    const items = extractLogItems({
      SitePublishItem: [
        { status: "Success", operation: "publish", fileName: "a.html" },
        { status: "Failed", operation: "publish", fileName: "b.html" },
      ],
    });
    expect(items).toHaveLength(2);
    expect(items[0].fileName).toBe("a.html");
  });

  it("filters items by free-text query", () => {
    const items = extractLogItems({
      SitePublishItem: [
        { status: "Success", fileName: "home.html" },
        { status: "Failed", fileName: "about.html" },
      ],
    });
    expect(filterLogItems(items, "fail")).toHaveLength(1);
    expect(filterLogItems(items, "home")).toHaveLength(1);
    expect(filterLogItems(items, "")).toHaveLength(2);
  });

  it("handles empty or raw array details", () => {
    expect(extractLogItems(null)).toEqual([]);
    expect(extractLogItems([{ status: "ok" }])).toHaveLength(1);
  });
});
