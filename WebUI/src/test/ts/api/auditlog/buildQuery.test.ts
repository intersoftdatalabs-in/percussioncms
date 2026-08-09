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
  buildAuditLogQueryString,
  datetimeLocalToIso,
} from "../../../../main/ts/api/auditlog/buildQuery";

describe("buildAuditLogQueryString", () => {
  it("returns empty string when no filters", () => {
    expect(buildAuditLogQueryString({})).toBe("");
  });

  it("omits blank filters and zero offset", () => {
    const qs = buildAuditLogQueryString({
      from: "  ",
      module: "AUTH",
      actor: "",
      offset: 0,
      limit: 50,
    });
    expect(qs.startsWith("?")).toBe(true);
    const params = new URLSearchParams(qs.slice(1));
    expect(params.get("module")).toBe("AUTH");
    expect(params.get("limit")).toBe("50");
    expect(params.has("from")).toBe(false);
    expect(params.has("actor")).toBe(false);
    expect(params.has("offset")).toBe(false);
  });

  it("includes offset when positive", () => {
    const qs = buildAuditLogQueryString({ offset: 100, limit: 25 });
    const params = new URLSearchParams(qs.slice(1));
    expect(params.get("offset")).toBe("100");
    expect(params.get("limit")).toBe("25");
  });

  it("clamps negative offset to zero (omitted)", () => {
    const qs = buildAuditLogQueryString({ offset: -5 });
    expect(qs).toBe("");
  });
});

describe("datetimeLocalToIso", () => {
  it("returns undefined for blank", () => {
    expect(datetimeLocalToIso("")).toBeUndefined();
    expect(datetimeLocalToIso("   ")).toBeUndefined();
    expect(datetimeLocalToIso(null)).toBeUndefined();
  });

  it("parses datetime-local into ISO-8601", () => {
    const iso = datetimeLocalToIso("2026-08-01T12:00");
    expect(iso).toBeTruthy();
    expect(iso).toMatch(/^\d{4}-\d{2}-\d{2}T/);
    expect(new Date(iso!).getTime()).not.toBeNaN();
  });
});
