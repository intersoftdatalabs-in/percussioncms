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
  COMMUNITY_LIST_ROOT,
  COMMUNITY_NAME_LIST_ROOT,
  GUID_LIST_ROOT,
  createCommunities,
  communityGuidForWrite,
  createCommunity,
  deleteCommunities,
  deleteCommunity,
  getCommunityDetail,
  isCommunityWriteReady,
  isValidCommunityName,
  listAvailableRoles,
  listCommunities,
  normalizeCommunityName,
  saveCommunities,
  unwrapCommunityDetail,
  unwrapCommunityList,
  COMMUNITY_ROLE_LIST_ROOT,
  updateCommunityRoles,
  wrapCommunityListForWire,
  wrapCommunityNameListForWire,
  wrapCommunityRoleListForWire,
  wrapGuidListForWire,
} from "../../../../main/ts/api/developer/assemblyApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("community name validation (SE-01)", () => {
  it("trims and rejects blank / whitespace names", () => {
    expect(normalizeCommunityName("  QA  ")).toBe("QA");
    expect(normalizeCommunityName("")).toBe("");
    expect(normalizeCommunityName("   ")).toBe("");
    expect(isValidCommunityName("")).toBe(false);
    expect(isValidCommunityName("  ")).toBe(false);
    expect(isValidCommunityName("Default")).toBe(true);
    expect(isValidCommunityName("Swiss French")).toBe(true);
    expect(isCommunityWriteReady({ name: "" })).toBe(false);
    expect(isCommunityWriteReady({ name: "QA" })).toBe(true);
  });
});

describe("community bulk wire wrap", () => {
  it("wraps create names under List", () => {
    expect(wrapCommunityNameListForWire(["Comm1", "Comm2"])).toEqual({
      [COMMUNITY_NAME_LIST_ROOT]: ["Comm1", "Comm2"],
    });
  });

  it("wraps CommunityList and GuidList envelopes for bulk JSON readers", () => {
    const row = { name: "QA", guid: { stringValue: "0-13-42" } };
    expect(wrapCommunityListForWire([row])).toEqual({
      [COMMUNITY_LIST_ROOT]: [row],
    });
    expect(wrapGuidListForWire([{ stringValue: "0-13-42" }])).toEqual({
      [GUID_LIST_ROOT]: [{ stringValue: "0-13-42" }],
    });
  });

  it("unwraps CommunityList envelope and flat arrays", () => {
    expect(
      unwrapCommunityList({ CommunityList: [{ name: "Default" }] }),
    ).toEqual([{ name: "Default" }]);
    expect(unwrapCommunityList([{ name: "A" }])).toEqual([{ name: "A" }]);
    expect(unwrapCommunityList({ Community: { name: "Solo" } })).toEqual([{ name: "Solo" }]);
    expect(unwrapCommunityList(null)).toEqual([]);
  });

  it("normalizes nested Guid wraps on list items", () => {
    const list = unwrapCommunityList({
      CommunityList: [{ name: "QA", guid: { Guid: { stringValue: "0-13-42" } } }],
    });
    expect(list[0]?.guid?.stringValue).toBe("0-13-42");
  });
});

describe("unwrapCommunityDetail / communityGuidForWrite (#4077)", () => {
  it("unwraps Community root and nested Guid", () => {
    const d = unwrapCommunityDetail({
      Community: {
        name: "QA",
        id: 42,
        guid: { Guid: { stringValue: "0-13-42", uuid: 42, type: 13 } },
      },
    });
    expect(d.name).toBe("QA");
    expect(d.guid?.stringValue).toBe("0-13-42");
  });

  it("synthesizes 0-13-{id} when Guid is missing", () => {
    const d = unwrapCommunityDetail({ Community: { name: "QA", id: 1007 } });
    expect(d.guid?.stringValue).toBe("0-13-1007");
    expect(communityGuidForWrite(d)?.stringValue).toBe("0-13-1007");
  });

  it("communityGuidForWrite prefers nested Guid then fallback", () => {
    expect(
      communityGuidForWrite({ guid: { Guid: { stringValue: "0-13-9" } } })?.stringValue,
    ).toBe("0-13-9");
    expect(
      communityGuidForWrite({ name: "QA" }, { stringValue: "0-13-8" })?.stringValue,
    ).toBe("0-13-8");
    expect(communityGuidForWrite({ name: "QA" })).toBeNull();
  });
});

describe("createCommunities / saveCommunities / deleteCommunities (SE-01)", () => {
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

  it("lists communities from GET find", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ CommunityList: [{ name: "Default", id: 10 }] }),
    );
    const list = await listCommunities();
    expect(list).toEqual([
      {
        name: "Default",
        id: 10,
        guid: { stringValue: "0-13-10" },
        guidString: "0-13-10",
      },
    ]);
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain(`${PATHS.COMMUNITIES}/find`);
  });

  it("POSTs create names to /services/communities/bulk", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ CommunityList: [{ name: "QA", guid: { stringValue: "0-13-42" } }] }),
    );
    const created = await createCommunities(["QA"]);
    expect(created[0]?.name).toBe("QA");
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain(`${PATHS.COMMUNITIES}/bulk`);
    expect(JSON.parse(String(init.body))).toEqual({ List: ["QA"] });
  });

  it("getCommunityDetail unwraps Community root", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        Community: {
          name: "QA",
          id: 42,
          guid: { Guid: { stringValue: "0-13-42" } },
        },
      }),
    );
    const d = await getCommunityDetail("QA");
    expect(d.name).toBe("QA");
    expect(d.guid?.stringValue).toBe("0-13-42");
  });

  it("createCommunity POSTs bulk names and does not PUT save", async () => {
    const created = { name: "QA", guid: { stringValue: "0-13-42" } };
    fetchMock.mockResolvedValueOnce(jsonResponse({ CommunityList: [created] }));
    const out = await createCommunity(" QA ");
    expect(out.name).toBe("QA");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const postInit = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(postInit.method).toBe("POST");
    expect(JSON.parse(String(postInit.body))).toEqual({ List: ["QA"] });
  });

  it("saveCommunities PUTs CommunityList with release header", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await saveCommunities([{ name: "QA" }], true);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({ CommunityList: [{ name: "QA" }] });
  });

  it("createCommunities blank name is 400", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: "name cannot be null or empty" }, 400),
    );
    await expect(createCommunities([" "])).rejects.toMatchObject({ status: 400 });
  });

  it("createCommunities duplicate is 409", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: "Community already exists: Default" }, 409),
    );
    await expect(createCommunities(["Default"])).rejects.toMatchObject({ status: 409 });
  });

  it("createCommunities non-Admin is 403", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: "Admin role required" }, 403),
    );
    await expect(createCommunities(["QA"])).rejects.toMatchObject({ status: 403 });
  });

  it("DELETEs GuidList without ignoredependencies by default", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await deleteCommunity({ stringValue: "0-13-42" });
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain(`${PATHS.COMMUNITIES}/bulk`);
    expect(JSON.parse(String(init.body))).toEqual({
      GuidList: [{ stringValue: "0-13-42" }],
    });
    const headers = new Headers(init.headers);
    expect(headers.get("ignoredependencies")).toBe("false");
  });

  it("deleteCommunities in-use without ignore is 409", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: "Community has dependencies" }, 409),
    );
    await expect(
      deleteCommunities([{ stringValue: "0-13-10" }], false),
    ).rejects.toMatchObject({ status: 409 });
    const headers = new Headers(fetchMock.mock.calls[0]?.[1]?.headers as HeadersInit);
    expect(headers.get("ignoredependencies")).toBe("false");
  });

  it("deleteCommunities missing is 404", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "not found" }, 404));
    await expect(deleteCommunities([{ stringValue: "0-13-0" }])).rejects.toMatchObject({
      status: 404,
    });
  });

  it("deleteCommunities non-Admin is 403", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: "Admin role required" }, 403),
    );
    await expect(deleteCommunities([{ stringValue: "0-13-42" }])).rejects.toMatchObject({
      status: 403,
    });
  });
});

describe("listAvailableRoles / updateCommunityRoles (SE-02)", () => {
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

  it("lists available roles from GET /communities/roles envelope", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        CommunityRoleList: [
          { roleName: "Admin", roleId: 1, roleGuid: { stringValue: "0-6-1" } },
          { roleName: "Editor", roleId: 2, roleGuid: { stringValue: "0-6-2" } },
        ],
      }),
    );
    const roles = await listAvailableRoles();
    expect(roles).toEqual([
      { roleName: "Admin", roleId: 1, roleGuid: { stringValue: "0-6-1" } },
      { roleName: "Editor", roleId: 2, roleGuid: { stringValue: "0-6-2" } },
    ]);
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain(`${PATHS.COMMUNITIES}/roles`);
    expect((fetchMock.mock.calls[0]?.[1] as RequestInit).method ?? "GET").toMatch(/GET/i);
  });

  it("wrapCommunityRoleListForWire uses CommunityRoleList root", () => {
    const roles = [{ roleName: "Editor", roleId: 2, roleGuid: { stringValue: "0-6-2" } }];
    expect(wrapCommunityRoleListForWire(roles)).toEqual({
      [COMMUNITY_ROLE_LIST_ROOT]: roles,
    });
    expect(wrapCommunityRoleListForWire([])).toEqual({ [COMMUNITY_ROLE_LIST_ROOT]: [] });
  });

  it("PUTs role membership under CommunityRoleList and unwraps Community WRAP_ROOT", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        Community: {
          name: "Default",
          id: 1001,
          guid: { Guid: { stringValue: "0-13-1001" } },
          roleList: [
            { roleName: "Editor", roleId: 2, roleGuid: { stringValue: "0-6-2" } },
          ],
        },
      }),
    );
    const body = [{ roleName: "Editor", roleId: 2, roleGuid: { stringValue: "0-6-2" } }];
    const saved = await updateCommunityRoles("Default", body);
    expect(saved.name).toBe("Default");
    expect(saved.guid?.stringValue).toBe("0-13-1001");
    expect(saved.roleList).toEqual(body);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain(
      `${PATHS.COMMUNITIES}/${encodeURIComponent("Default")}/roles`,
    );
    expect(JSON.parse(String(init.body))).toEqual({
      [COMMUNITY_ROLE_LIST_ROOT]: body,
    });
  });

  it("empty PUT body clears all role associations via wrapped list", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        Community: { name: "Default", id: 1001, roleList: [] },
      }),
    );
    const saved = await updateCommunityRoles("Default", []);
    expect(saved.name).toBe("Default");
    expect(saved.roleList).toEqual([]);
    expect(JSON.parse(String((fetchMock.mock.calls[0]?.[1] as RequestInit).body))).toEqual({
      [COMMUNITY_ROLE_LIST_ROOT]: [],
    });
  });

  it("updateCommunityRoles missing community is 404", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Community not found" }, 404));
    await expect(updateCommunityRoles("missing", [])).rejects.toMatchObject({ status: 404 });
  });
});
