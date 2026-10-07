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
  deleteRole,
  isRoleCreateReady,
  isSystemRoleName,
  loadRole,
  normalizeRoleBrowseGroupFilter,
  normalizeRoleUsers,
  roleCreateUrl,
  roleDeleteUrl,
  roleReadUrl,
  roleUpdateDescriptionUrl,
  roleUpdateHomePageUrl,
  rolesInBrowseGroup,
  unwrapCreatedRole,
  unwrapRoleBrowseCatalog,
  unwrapRoleRead,
  unwrapUpdatedRoleHomePage,
  addRoleUser,
  removeRoleUser,
  copyOneRole,
  RoleCopyFailure,
  roleAddUserUrl,
  roleRemoveUserUrl,
  updateRoleDescription,
  updateRoleHomePage,
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

describe("updateRoleHomePage", () => {
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
    await expect(updateRoleHomePage({ name: "  ", homePage: "Home" })).rejects.toThrow(
      /required/,
    );
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("PUTs homePage=true with the canonical field and no description or users", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        Role: { name: "Author", description: "Keep me", homePage: "Explorer" },
      }),
    );
    const saved = await updateRoleHomePage({
      name: " Author ",
      homePage: " Explorer ",
    });
    expect(saved).toEqual({
      name: "Author",
      description: "Keep me",
      homePage: "Explorer",
    });
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(String(url)).toBe(roleUpdateHomePageUrl());
    expect(String(url)).not.toContain("update=true");
    expect(String(url)).not.toContain("create=true");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      Role: { name: "Author", homePage: "Explorer" },
    });
  });

  it("sends an empty home page to clear and does not invent one from a missing field", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ Role: { name: "Author", description: "Keep me" } }),
    );
    const saved = await updateRoleHomePage({ name: "Author", homePage: "   " });
    expect(saved).toEqual({ name: "Author", description: "Keep me" });
    expect(saved.homePage).toBeUndefined();
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(JSON.parse(String(init.body))).toEqual({
      Role: { name: "Author", homePage: "" },
    });
  });

  it("rejects HTTP 400, 403, and 404 without returning a role", async () => {
    for (const status of [400, 403, 404]) {
      fetchMock.mockResolvedValueOnce(jsonResponse({ message: "no" }, status));
      await expect(
        updateRoleHomePage({ name: "Author", homePage: "Explorer" }),
      ).rejects.toMatchObject({ status });
    }
  });

  it("unwraps a flat body and rejects an empty payload", () => {
    expect(unwrapUpdatedRoleHomePage({ name: "Author", homePage: "Home" })).toEqual({
      name: "Author",
      homePage: "Home",
    });
    expect(() => unwrapUpdatedRoleHomePage(null)).toThrow(/empty/);
    expect(() => unwrapUpdatedRoleHomePage({ homePage: "Home" })).toThrow(/name/);
  });
});

describe("addRoleUser", () => {
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

  it("does not PUT a blank role or user name", async () => {
    await expect(addRoleUser({ name: "  ", userName: "Ada" })).rejects.toThrow(/required/);
    await expect(addRoleUser({ name: "Author", userName: "   " })).rejects.toThrow(/required/);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("PUTs addUser=true with one user and no description or home page", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        Role: { name: "Author", description: "Keep me", users: ["Bea", "Ada"] },
      }),
    );
    const saved = await addRoleUser({ name: " Author ", userName: " Ada " });
    expect(saved).toEqual({
      name: "Author",
      description: "Keep me",
      users: ["Bea", "Ada"],
    });
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(String(url)).toBe(roleAddUserUrl());
    expect(String(url)).not.toContain("update=true");
    expect(String(url)).not.toContain("create=true");
    expect(String(url)).not.toContain("homePage=true");
    expect(String(url)).not.toContain("removeUser=true");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      Role: { name: "Author", users: ["Ada"] },
    });
  });

  it("rejects HTTP 400, 403, and 409 without returning users from the body", async () => {
    for (const status of [400, 403, 409]) {
      fetchMock.mockResolvedValueOnce(
        jsonResponse({ Role: { name: "Author", users: ["Invented"] } }, status),
      );
      await expect(addRoleUser({ name: "Author", userName: "Ada" })).rejects.toMatchObject({
        status,
      });
    }
  });
});

describe("removeRoleUser", () => {
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

  it("does not PUT a blank role or user name", async () => {
    await expect(removeRoleUser({ name: "  ", userName: "Ada" })).rejects.toThrow(/required/);
    await expect(removeRoleUser({ name: "Author", userName: "   " })).rejects.toThrow(/required/);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("PUTs removeUser=true with one user and no description or home page", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        Role: { name: "Author", description: "Keep me", users: ["Bea"] },
      }),
    );
    const saved = await removeRoleUser({ name: " Author ", userName: " Ada " });
    expect(saved).toEqual({
      name: "Author",
      description: "Keep me",
      users: ["Bea"],
    });
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(String(url)).toBe(roleRemoveUserUrl());
    expect(String(url)).not.toContain("update=true");
    expect(String(url)).not.toContain("create=true");
    expect(String(url)).not.toContain("homePage=true");
    expect(String(url)).not.toContain("addUser=true");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      Role: { name: "Author", users: ["Ada"] },
    });
  });

  it("rejects HTTP 400, 403, and 409 without returning users from the body", async () => {
    for (const status of [400, 403, 409]) {
      fetchMock.mockResolvedValueOnce(
        jsonResponse({ Role: { name: "Author", users: [] } }, status),
      );
      await expect(removeRoleUser({ name: "Author", userName: "Ada" })).rejects.toMatchObject({
        status,
      });
    }
  });
});

describe("normalizeRoleUsers", () => {
  it("keeps stored names and drops blanks and duplicates", () => {
    expect(normalizeRoleUsers([" Ada ", "", "Ada", "Bea"])).toEqual(["Ada", "Bea"]);
    expect(normalizeRoleUsers("Ada")).toEqual(["Ada"]);
    expect(normalizeRoleUsers(null)).toEqual([]);
    expect(normalizeRoleUsers({ user: ["Ada", "  "] })).toEqual(["Ada"]);
  });

  it("does not invent members from unknown object keys", () => {
    expect(normalizeRoleUsers({ message: "Invented", users: ["Ada"] })).toEqual(["Ada"]);
    expect(normalizeRoleUsers({ message: "Invented" })).toEqual([]);
    expect(normalizeRoleUsers([{ name: "Invented" }])).toEqual([]);
  });
});

describe("unwrapRoleRead", () => {
  it("reads users from a Role envelope and treats a missing list as empty", () => {
    expect(
      unwrapRoleRead({
        Role: { name: " Author ", description: "Editors", users: ["Ada", "Bea"] },
      }),
    ).toEqual({
      name: "Author",
      description: "Editors",
      users: ["Ada", "Bea"],
    });
    expect(unwrapRoleRead({ name: "Author" })).toEqual({
      name: "Author",
      users: [],
    });
    expect(unwrapRoleRead({ name: "Author", users: "Ada" })).toEqual({
      name: "Author",
      users: ["Ada"],
    });
  });

  it("rejects a nameless body so its users are not a membership list", () => {
    expect(() => unwrapRoleRead(null)).toThrow(/empty/);
    expect(() => unwrapRoleRead({ users: ["Invented"] })).toThrow(/name/);
    expect(() => unwrapRoleRead({ Role: { users: ["Invented"] } })).toThrow(/name/);
  });
});

describe("loadRole", () => {
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

  it("does not GET a blank name", async () => {
    await expect(loadRole("   ")).rejects.toThrow(/required/);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("GETs the encoded role and returns its users", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ Role: { name: "Night Role", users: ["Ada"] } }),
    );
    const read = await loadRole(" Night Role ");
    expect(read).toEqual({ name: "Night Role", users: ["Ada"] });
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(String(url)).toBe(roleReadUrl("Night Role"));
    expect(String(url)).toBe(`${PATHS.ROLES}/Night%20Role`);
    expect(init.method).toBe("GET");
  });

  it("rejects HTTP 403 and 404 without returning users from the body", async () => {
    for (const status of [403, 404]) {
      fetchMock.mockResolvedValueOnce(
        jsonResponse({ Role: { name: "Author", users: ["Invented"] } }, status),
      );
      await expect(loadRole("Author")).rejects.toMatchObject({ status });
    }
  });
});

describe("deleteRole", () => {
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

  it("recognizes system roles case-insensitively", () => {
    expect(isSystemRoleName("System")).toBe(true);
    expect(isSystemRoleName(" default ")).toBe(true);
    expect(isSystemRoleName("Author")).toBe(false);
    expect(isSystemRoleName("  ")).toBe(false);
  });

  it("does not DELETE a blank name or a system role", async () => {
    await expect(deleteRole("  ")).rejects.toThrow(/required/);
    await expect(deleteRole("Default")).rejects.toThrow(/system role/);
    await expect(deleteRole("system")).rejects.toThrow(/system role/);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("DELETEs the encoded role name", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ statusCode: 200, message: "OK" }));
    await deleteRole(" Night Role ");
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(String(url)).toBe(roleDeleteUrl("Night Role"));
    expect(String(url)).toBe(`${PATHS.ROLES}/Night%20Role`);
    expect(init.method).toBe("DELETE");
  });

  it("rejects HTTP 400, 403, and 409", async () => {
    for (const status of [400, 403, 409]) {
      fetchMock.mockResolvedValueOnce(jsonResponse({ message: "no" }, status));
      await expect(deleteRole("Author")).rejects.toMatchObject({ status });
    }
  });
});

describe("copyOneRole", () => {
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

  function putBodies(): { url: string; body: Record<string, unknown> }[] {
    return fetchMock.mock.calls.map((call) => {
      const [url, init] = call as [string, RequestInit];
      const parsed = JSON.parse(String(init.body)) as { Role?: Record<string, unknown> };
      return { url: String(url), body: parsed.Role ?? parsed };
    });
  }

  it("does not call the server for a blank name", async () => {
    await expect(
      copyOneRole({ description: "Editors", homePage: "Home", users: ["Ada"] }, "  "),
    ).rejects.toThrow(/required/);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("creates the new name, then writes description, home page, and each user", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ Role: { name: "NightCopy" } }))
      .mockResolvedValueOnce(
        jsonResponse({ Role: { name: "NightCopy", description: "Editors" } }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          Role: { name: "NightCopy", description: "Editors", homePage: "Developer" },
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({ Role: { name: "NightCopy", users: ["Ada"] } }),
      )
      .mockResolvedValueOnce(
        jsonResponse({ Role: { name: "NightCopy", users: ["Ada", "Bea"] } }),
      );
    const saved = await copyOneRole(
      {
        description: " Editors ",
        homePage: " Developer ",
        users: [" Ada ", "Ada", "ada", "Bea", ""],
      },
      " NightCopy ",
    );
    expect(saved).toEqual({ name: "NightCopy" });
    const calls = putBodies();
    expect(calls).toHaveLength(5);
    expect(calls[0].url).toContain("create=true");
    expect(calls[0].url).not.toContain("addUser=true");
    expect(calls[0].body).toEqual({ name: "NightCopy" });
    expect(calls[1].url).toContain("update=true");
    expect(calls[1].body).toEqual({ name: "NightCopy", description: "Editors" });
    expect(calls[1].body.users).toBeUndefined();
    expect(calls[2].url).toContain("homePage=true");
    expect(calls[2].body).toEqual({ name: "NightCopy", homePage: "Developer" });
    expect(calls[3].url).toContain("addUser=true");
    expect(calls[3].body).toEqual({ name: "NightCopy", users: ["Ada"] });
    expect(calls[4].body).toEqual({ name: "NightCopy", users: ["Bea"] });
    expect(calls.every((call) => call.body.name !== "Author")).toBe(true);
  });

  it("skips description, home page, and users when the source has none", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ Role: { name: "NightCopy" } }));
    await copyOneRole({ description: "  ", homePage: "", users: [] }, "NightCopy");
    expect(putBodies()).toEqual([{ url: expect.stringContaining("create=true"), body: { name: "NightCopy" } }]);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("does not continue after create fails with 400, 403, or 409", async () => {
    for (const status of [400, 403, 409]) {
      fetchMock.mockReset();
      fetchMock.mockResolvedValueOnce(jsonResponse({ message: "no" }, status));
      await expect(
        copyOneRole({ description: "Editors", homePage: "Home", users: ["Ada"] }, "NightCopy"),
      ).rejects.toMatchObject({ partial: false, cause: { status } });
      expect(fetchMock).toHaveBeenCalledTimes(1);
      expect(String(fetchMock.mock.calls[0][0])).toContain("create=true");
    }
  });

  it("stops after a later step fails and does not report success", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ Role: { name: "NightCopy" } }))
      .mockResolvedValueOnce(jsonResponse({ message: "too long" }, 400));
    const error = await copyOneRole(
      { description: "Editors", homePage: "Home", users: ["Ada"] },
      "NightCopy",
    ).catch((err: unknown) => err);
    expect(error).toBeInstanceOf(RoleCopyFailure);
    expect(error).toMatchObject({ partial: true, cause: { status: 400 } });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(String(fetchMock.mock.calls[1][0])).toContain("update=true");

    fetchMock.mockReset();
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ Role: { name: "NightCopy" } }))
      .mockResolvedValueOnce(
        jsonResponse({ Role: { name: "NightCopy", description: "Editors" } }),
      )
      .mockResolvedValueOnce(jsonResponse({ message: "bad home" }, 409));
    await expect(
      copyOneRole({ description: "Editors", homePage: "Home", users: ["Ada"] }, "NightCopy"),
    ).rejects.toMatchObject({ partial: true, cause: { status: 409 } });
    expect(fetchMock).toHaveBeenCalledTimes(3);

    fetchMock.mockReset();
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ Role: { name: "NightCopy" } }))
      .mockResolvedValueOnce(
        jsonResponse({ Role: { name: "NightCopy", description: "Editors" } }),
      )
      .mockResolvedValueOnce(
        jsonResponse({ Role: { name: "NightCopy", homePage: "Home" } }),
      )
      .mockResolvedValueOnce(jsonResponse({ message: "forbidden" }, 403));
    await expect(
      copyOneRole({ description: "Editors", homePage: "Home", users: ["Ada"] }, "NightCopy"),
    ).rejects.toMatchObject({ partial: true, cause: { status: 403 } });
    expect(String(fetchMock.mock.calls[3][0])).toContain("addUser=true");
  });
});
