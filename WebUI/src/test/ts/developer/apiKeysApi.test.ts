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
  API_KEY_ASSIGNMENT_ROOT,
  isApiKeyNameReady,
  matchPresetForScopes,
  normalizeApiKeyName,
  scopeGroupRank,
  unwrapApiKeyAssignment,
  unwrapApiKeyAssignmentList,
  unwrapApiKeyScopeList,
  unwrapApiKeyScopePresetList,
  wrapApiKeyAssignmentForWire,
} from "../../../main/ts/api/developer/apiKeysApi";

const PRESETS = [
  { id: "readonly", name: "Read-only", scopes: ["content:read", "asset:read"] },
  {
    id: "author",
    name: "Author",
    scopes: ["content:read", "content:write", "asset:read"],
  },
];

describe("unwrapApiKeyScopeList", () => {
  it("accepts a flat array", () => {
    const out = unwrapApiKeyScopeList([
      { key: "content:read", name: "Read content", group: "content" },
    ]);
    expect(out).toHaveLength(1);
    expect(out[0]?.key).toBe("content:read");
  });

  it("unwraps Jackson root ApiKeyScope", () => {
    const out = unwrapApiKeyScopeList({
      ApiKeyScope: [{ key: "publish", name: "Publish", group: "publishing" }],
    });
    expect(out).toHaveLength(1);
    expect(out[0]?.group).toBe("publishing");
  });

  it("drops keyless entries and defaults the group", () => {
    const out = unwrapApiKeyScopeList([{ name: "No key" }, { key: "a:b" }]);
    expect(out).toHaveLength(1);
    expect(out[0]?.name).toBe("a:b");
    expect(out[0]?.group).toBe("other");
  });

  it("returns empty for null / non-object", () => {
    expect(unwrapApiKeyScopeList(null)).toEqual([]);
    expect(unwrapApiKeyScopeList("x")).toEqual([]);
  });
});

describe("unwrapApiKeyScopePresetList", () => {
  it("unwraps Jackson root ApiKeyScopePreset", () => {
    const out = unwrapApiKeyScopePresetList({
      ApiKeyScopePreset: [{ id: "readonly", name: "Read-only", scopes: ["content:read"] }],
    });
    expect(out).toHaveLength(1);
    expect(out[0]?.scopes).toEqual(["content:read"]);
  });

  it("drops id-less entries", () => {
    expect(unwrapApiKeyScopePresetList([{ name: "No id" }])).toEqual([]);
  });
});

describe("unwrapApiKeyAssignmentList", () => {
  it("accepts a flat array and normalizes presetId", () => {
    const out = unwrapApiKeyAssignmentList([
      { name: "sync", scopes: ["content:read"], presetId: "readonly" },
      { name: "custom", scopes: [] },
    ]);
    expect(out).toHaveLength(2);
    expect(out[0]?.presetId).toBe("readonly");
    expect(out[1]?.presetId).toBeNull();
  });

  it("accepts a wrapped list and a single object", () => {
    expect(
      unwrapApiKeyAssignmentList({
        ApiKeyAssignment: [{ name: "a", scopes: [] }],
      }),
    ).toHaveLength(1);
    expect(
      unwrapApiKeyAssignmentList({ name: "solo", scopes: ["publish"] }),
    ).toHaveLength(1);
  });

  it("drops nameless entries", () => {
    expect(unwrapApiKeyAssignmentList([{ scopes: [] }])).toEqual([]);
  });
});

describe("unwrapApiKeyAssignment", () => {
  it("unwraps the Jackson root", () => {
    const out = unwrapApiKeyAssignment({
      ApiKeyAssignment: { name: "sync", scopes: ["content:read"] },
    });
    expect(out.name).toBe("sync");
  });

  it("throws when the payload has no name", () => {
    expect(() => unwrapApiKeyAssignment({ scopes: [] })).toThrow();
    expect(() => unwrapApiKeyAssignment(null)).toThrow();
  });
});

describe("wrapApiKeyAssignmentForWire", () => {
  it("wraps under the JAXB root name", () => {
    const body = { name: "sync", scopes: ["content:read"] };
    expect(wrapApiKeyAssignmentForWire(body)).toEqual({
      [API_KEY_ASSIGNMENT_ROOT]: body,
    });
  });
});

describe("isApiKeyNameReady / normalizeApiKeyName", () => {
  it("accepts safe names and rejects blank/unsafe", () => {
    expect(isApiKeyNameReady("sync-job_1.0")).toBe(true);
    expect(isApiKeyNameReady("  ")).toBe(false);
    expect(isApiKeyNameReady("../evil")).toBe(false);
    expect(isApiKeyNameReady("9lives")).toBe(false);
    expect(isApiKeyNameReady(null)).toBe(false);
  });

  it("trims for the wire", () => {
    expect(normalizeApiKeyName("  sync  ")).toBe("sync");
    expect(normalizeApiKeyName(null)).toBe("");
  });
});

describe("matchPresetForScopes", () => {
  it("matches order-insensitively", () => {
    expect(matchPresetForScopes(PRESETS, ["asset:read", "content:read"])).toBe("readonly");
  });

  it("returns null for custom combinations", () => {
    expect(matchPresetForScopes(PRESETS, ["content:read"])).toBeNull();
    expect(matchPresetForScopes(PRESETS, [])).toBeNull();
  });
});

describe("scopeGroupRank", () => {
  it("orders known groups before unknown", () => {
    expect(scopeGroupRank("content")).toBeLessThan(scopeGroupRank("admin"));
    expect(scopeGroupRank("zzz")).toBeGreaterThan(scopeGroupRank("admin"));
  });
});
