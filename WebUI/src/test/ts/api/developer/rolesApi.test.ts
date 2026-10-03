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
  createRole,
  isRoleCreateReady,
  normalizeRoleBrowseGroupFilter,
  roleCreateUrl,
  roleUpdateDescriptionUrl,
  rolesInBrowseGroup,
  unwrapCreatedRole,
  unwrapRoleBrowseCatalog,
  updateRoleDescription,
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

  it("unwraps the live RoleBrowseCatalog envelope and one-item lists", () => {
    const catalog = unwrapRoleBrowseCatalog({
      RoleBrowseCatalog: {
        roles: [
          {
            groups: "workflow",
            name: "Nr1",
            workflows: "Default Workflow",
          },
          {
            communities: "Default",
            groups: ["community", "workflow"],
            name: "RxPublisher",
            workflows: ["Simple Workflow", "Standard Workflow"],
          },
        ],
      },
    });
    expect(catalog.roles.map((r) => r.name)).toEqual(["Nr1", "RxPublisher"]);
    expect(catalog.roles[0].groups).toEqual(["workflow"]);
    expect(catalog.roles[0].workflows).toEqual(["Default Workflow"]);
    expect(catalog.roles[1].communities).toEqual(["Default"]);
    expect(catalog.roles[1].groups).toEqual(["community", "workflow"]);
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

describe("isRoleCreateReady", () => {
  it("rejects blank names", () => {
    expect(isRoleCreateReady("")).toBe(false);
    expect(isRoleCreateReady("   ")).toBe(false);
    expect(isRoleCreateReady(null)).toBe(false);
    expect(isRoleCreateReady(undefined)).toBe(false);
    expect(isRoleCreateReady("NightRole")).toBe(true);
  });
});

describe("unwrapCreatedRole", () => {
  it("accepts a flat role and a Role envelope", () => {
    expect(unwrapCreatedRole({ name: "NightRole", description: "Editors" })).toEqual({
      name: "NightRole",
      description: "Editors",
    });
    expect(unwrapCreatedRole({ Role: { name: " NightRole " } })).toEqual({
      name: "NightRole",
      description: undefined,
    });
  });

  it("rejects an empty body", () => {
    expect(() => unwrapCreatedRole(null)).toThrow(/empty/);
    expect(() => unwrapCreatedRole({ description: "x" })).toThrow(/name/);
  });
});

describe("createRole", () => {
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

  it("does not PUT a blank name", async () => {
    await expect(createRole({ name: "   " })).rejects.toThrow(/required/);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("PUTs create=true and trims the body", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ name: "NightRole", description: "Editors" }),
    );
    const created = await createRole({
      name: " NightRole ",
      description: " Editors ",
    });
    expect(created).toEqual({ name: "NightRole", description: "Editors" });
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(String(url)).toBe(roleCreateUrl());
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      Role: { name: "NightRole", description: "Editors" },
    });
  });

  it("omits a blank description", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ name: "NightRole" }));
    await createRole({ name: "NightRole", description: "  " });
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(JSON.parse(String(init.body))).toEqual({
      Role: { name: "NightRole" },
    });
  });

  it("rejects HTTP 400 without returning a role", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "invalid" }, 400));
    await expect(createRole({ name: "NightRole" })).rejects.toMatchObject({
      status: 400,
    });
  });

  it("rejects HTTP 403 without returning a role", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "forbidden" }, 403));
    await expect(createRole({ name: "NightRole" })).rejects.toMatchObject({
      status: 403,
    });
  });
});

describe("updateRoleDescription", () => {
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

  it("does not PUT a blank name", async () => {
    await expect(updateRoleDescription({ name: "  " })).rejects.toThrow(/required/);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("PUTs update=true with the trimmed description and no users", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ Role: { name: "Author", description: "Updated" } }),
    );
    const saved = await updateRoleDescription({
      name: " Author ",
      description: " Updated ",
    });
    expect(saved).toEqual({ name: "Author", description: "Updated" });
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(String(url)).toBe(roleUpdateDescriptionUrl());
    expect(String(url)).not.toContain("create=true");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      Role: { name: "Author", description: "Updated" },
    });
  });

  it("sends an empty description to clear", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ name: "Author" }));
    await updateRoleDescription({ name: "Author", description: "   " });
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(JSON.parse(String(init.body))).toEqual({
      Role: { name: "Author", description: "" },
    });
  });

  it("rejects HTTP 400, 403, and 404 without returning a role", async () => {
    for (const status of [400, 403, 404]) {
      fetchMock.mockResolvedValueOnce(jsonResponse({ message: "no" }, status));
      await expect(
        updateRoleDescription({ name: "Author", description: "Nope" }),
      ).rejects.toMatchObject({ status });
    }
  });
});
