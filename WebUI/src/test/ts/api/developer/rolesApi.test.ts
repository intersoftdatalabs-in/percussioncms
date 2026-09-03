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

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  browseRoles,
  normalizeRoleBrowseGroupFilter,
  rolesInBrowseGroup,
  unwrapRoleBrowseCatalog,
} from "../../../../main/ts/api/developer/rolesApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("normalizeRoleBrowseGroupFilter", () => {
  it("accepts community / workflow / unassigned (case-insensitive)", () => {
    expect(normalizeRoleBrowseGroupFilter("community")).toBe("community");
    expect(normalizeRoleBrowseGroupFilter(" Workflow ")).toBe("workflow");
    expect(normalizeRoleBrowseGroupFilter("UNASSIGNED")).toBe("unassigned");
  });

  it("treats blank as full catalog", () => {
    expect(normalizeRoleBrowseGroupFilter(undefined)).toBeUndefined();
    expect(normalizeRoleBrowseGroupFilter(null)).toBeUndefined();
    expect(normalizeRoleBrowseGroupFilter("")).toBeUndefined();
    expect(normalizeRoleBrowseGroupFilter("   ")).toBeUndefined();
  });

  it("rejects unknown groups", () => {
    expect(() => normalizeRoleBrowseGroupFilter("other")).toThrow(/unassigned/);
  });
});

describe("unwrapRoleBrowseCatalog", () => {
  it("unwraps flat catalog body with group filter", () => {
    const catalog = unwrapRoleBrowseCatalog({
      group: "community",
      roles: [
        {
          name: "Author",
          description: "Content author",
          groups: ["community"],
          communities: ["Default"],
          workflows: [],
        },
      ],
    });
    expect(catalog.group).toBe("community");
    expect(catalog.roles).toHaveLength(1);
    expect(catalog.roles[0].name).toBe("Author");
    expect(catalog.roles[0].communities).toEqual(["Default"]);
  });

  it("accepts a flat catalog body and dual group membership", () => {
    const catalog = unwrapRoleBrowseCatalog({
      roles: [
        {
          name: "Editor",
          groups: ["community", "workflow"],
          communities: ["Default", "Corporate"],
          workflows: ["Simple Workflow"],
        },
      ],
    });
    expect(catalog.group).toBeUndefined();
    expect(catalog.roles[0].groups).toEqual(["community", "workflow"]);
  });

  it("normalizes string / missing list fields", () => {
    const catalog = unwrapRoleBrowseCatalog({
      roles: {
        name: "Admin",
        groups: "unassigned",
      },
    });
    expect(catalog.roles).toEqual([
      {
        name: "Admin",
        description: undefined,
        groups: ["unassigned"],
        communities: [],
        workflows: [],
      },
    ]);
  });

  it("returns empty catalog for null / unknown shapes", () => {
    expect(unwrapRoleBrowseCatalog(null)).toEqual({ roles: [] });
    expect(unwrapRoleBrowseCatalog("nope")).toEqual({ roles: [] });
  });
});

describe("rolesInBrowseGroup", () => {
  const roles = [
    {
      name: "Zed",
      groups: ["community"] as const,
      communities: ["A"],
      workflows: [],
    },
    {
      name: "Author",
      groups: ["community", "workflow"] as const,
      communities: ["Default"],
      workflows: ["Simple"],
    },
    {
      name: "Orphan",
      groups: ["unassigned"] as const,
      communities: [],
      workflows: [],
    },
  ].map((r) => ({
    ...r,
    groups: [...r.groups],
  }));

  it("filters and sorts by name", () => {
    expect(rolesInBrowseGroup(roles, "community").map((r) => r.name)).toEqual([
      "Author",
      "Zed",
    ]);
    expect(rolesInBrowseGroup(roles, "workflow").map((r) => r.name)).toEqual([
      "Author",
    ]);
    expect(rolesInBrowseGroup(roles, "unassigned").map((r) => r.name)).toEqual([
      "Orphan",
    ]);
  });
});

describe("browseRoles", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      statusText: status === 200 ? "OK" : "Error",
      headers: { "Content-Type": "application/json" },
    });
  }

  it("GETs full catalog without group query", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        roles: [
          {
            name: "Admin",
            groups: ["community"],
            communities: ["Default"],
            workflows: [],
          },
        ],
      }),
    );
    const catalog = await browseRoles();
    expect(String(fetchMock.mock.calls[0]?.[0])).toBe(PATHS.ROLES_CATALOG);
    expect(catalog.roles[0].name).toBe("Admin");
  });

  it("appends encoded group filter", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ roles: [] }));
    await browseRoles("unassigned");
    expect(String(fetchMock.mock.calls[0]?.[0])).toBe(
      `${PATHS.ROLES_CATALOG}?group=unassigned`,
    );
  });
});
