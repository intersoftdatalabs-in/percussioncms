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
  ACL_LIST_ROOT,
  CREATE_ACL_REQUEST_ROOT,
  createObjectAcl,
  getAclForObject,
  normalizeAclForSave,
  saveObjectAcl,
  unwrapObjectAcl,
  wrapAclListForWire,
  wrapCreateAclRequestForWire,
} from "../../../../main/ts/api/developer/aclApi";

vi.mock("../../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}));

const get = client.get as ReturnType<typeof vi.fn>;
const post = client.post as ReturnType<typeof vi.fn>;
const put = client.put as ReturnType<typeof vi.fn>;

describe("unwrapObjectAcl", () => {
  it("unwraps Jackson Acl root so entries are reachable (#3200 / #3203)", () => {
    const acl = unwrapObjectAcl({
      Acl: {
        id: 7,
        name: "By_Author ACL",
        aclEntries: [{ name: "Admin", type: { type: "ROLE" } }],
      },
    });
    expect(acl.id).toBe(7);
    expect(acl.name).toBe("By_Author ACL");
    expect(Array.isArray(acl.aclEntries)).toBe(true);
  });

  it("unwraps camelCase acl envelope", () => {
    const acl = unwrapObjectAcl({
      acl: { id: 3, name: "site", aclEntries: [] },
    });
    expect(acl.id).toBe(3);
    expect(acl.name).toBe("site");
  });

  it("passes through flat ACL bodies", () => {
    const flat = { id: 1, name: "x", aclEntries: [] };
    expect(unwrapObjectAcl(flat)).toEqual(flat);
  });

  it("returns empty object for non-objects", () => {
    expect(unwrapObjectAcl(null)).toEqual({});
    expect(unwrapObjectAcl("acl")).toEqual({});
  });
});

describe("getAclForObject", () => {
  beforeEach(() => {
    get.mockReset();
  });

  it("unwraps wrapped GET /acls/object/{guid} body", async () => {
    get.mockResolvedValue({
      Acl: { id: 9, name: "df", aclEntries: [{ name: "Default" }] },
    });
    const acl = await getAclForObject("0-31-5");
    expect(acl.id).toBe(9);
    expect(acl.name).toBe("df");
    expect(acl.aclEntries).toHaveLength(1);
  });
});

describe("createObjectAcl", () => {
  beforeEach(() => {
    post.mockReset();
  });

  it("unwraps wrapped POST /acls body", async () => {
    post.mockResolvedValue({
      Acl: { id: 4, name: "new", aclEntries: [{ name: "Admin" }] },
    });
    const acl = await createObjectAcl("0-20-1", { name: "Admin", type: "ROLE" });
    expect(acl.id).toBe(4);
    expect(acl.aclEntries).toHaveLength(1);
  });

  it("POSTs CreateAclRequest envelope so UNWRAP_ROOT_VALUE can bind (#3378)", async () => {
    post.mockResolvedValue({ Acl: { id: 1, aclEntries: [] } });
    await createObjectAcl("0-31-5", { name: "Admin", type: "USER" });
    const [url, body] = post.mock.calls[0] as [string, Record<string, unknown>];
    expect(url).toMatch(/\/acls\/?$/);
    expect(body).toEqual(
      wrapCreateAclRequestForWire(
        { stringValue: "0-31-5" },
        { name: "Admin", type: "USER" },
      ),
    );
    expect(body[CREATE_ACL_REQUEST_ROOT]).toBeTruthy();
  });
});

describe("normalizeAclForSave / wrapAclListForWire", () => {
  it("strips principal.type and derives objectType from display-format guid", () => {
    const normalized = normalizeAclForSave({
      id: 7,
      name: "By_Author ACL",
      objectGuid: { stringValue: "0-31-5" },
      aclEntries: [
        {
          name: "Default",
          principal: { name: "Default", type: "USER" },
          type: { name: "Default", type: "USER" },
          permissions: [{ permission: "READ" }],
        },
        {
          name: "AnyCommunity",
          principal: { name: "AnyCommunity", type: "COMMUNITY" },
          type: { name: "AnyCommunity", type: "COMMUNITY" },
          permissions: [{ permission: "RUNTIME_VISIBLE" }],
        },
        {
          name: "Admin",
          principal: { name: "Admin", type: "USER" },
          type: { name: "Admin", type: "USER" },
          permissions: [{ permission: "READ" }, { permission: "OWNER" }],
        },
      ],
    });
    expect(normalized.objectType).toBe(31);
    expect(normalized.objectId).toBe(5);
    expect(normalized.name).toBe("By_Author ACL");
    const entries = normalized.aclEntries as Array<Record<string, unknown>>;
    expect(entries).toHaveLength(3);
    expect(entries[0].principal).toEqual({ name: "Default" });
    expect(entries[1].principal).toEqual({ name: "AnyCommunity" });
    expect(entries[2].principal).toEqual({ name: "Admin" });
    expect((entries[0].type as { type?: string }).type).toBe("USER");
    expect((entries[1].type as { type?: string }).type).toBe("COMMUNITY");
  });

  it("defaults missing name so PSX_ACLS.NAME is not null", () => {
    expect(normalizeAclForSave({ objectGuid: { stringValue: "0-31-5" } }).name).toBe(
      "ACL",
    );
  });

  it("wraps AclList envelope for PUT /acls/bulk", () => {
    const body = wrapAclListForWire({
      id: 7,
      name: "By_Author ACL",
      objectGuid: { stringValue: "0-31-5" },
      aclEntries: [{ name: "Admin", permissions: [{ permission: "READ" }] }],
    });
    expect(body[ACL_LIST_ROOT]).toHaveLength(1);
    expect(Array.isArray(body)).toBe(false);
    expect(body[ACL_LIST_ROOT][0].name).toBe("By_Author ACL");
  });
});

describe("saveObjectAcl", () => {
  beforeEach(() => {
    put.mockReset();
    put.mockResolvedValue(undefined);
  });

  it("PUTs /acls/bulk with AclList envelope and flattened entries (#3378)", async () => {
    await saveObjectAcl({
      id: 7,
      name: "By_Author ACL",
      objectGuid: { stringValue: "0-31-5" },
      aclEntries: {
        AclEntry: [
          {
            name: "Admin",
            type: { type: "ROLE" },
            permissions: {
              UserAccessLevel: [{ permission: "READ" }, { permission: "UPDATE" }],
            },
          },
        ],
      } as unknown as [],
    });
    expect(put).toHaveBeenCalledTimes(1);
    const [url, body] = put.mock.calls[0] as [
      string,
      { AclList: Array<Record<string, unknown>> },
    ];
    expect(url).toMatch(/\/acls\/bulk$/);
    expect(Array.isArray(body)).toBe(false);
    expect(body[ACL_LIST_ROOT]).toHaveLength(1);
    const entries = body[ACL_LIST_ROOT][0].aclEntries as Array<Record<string, unknown>>;
    expect(entries).toHaveLength(1);
    expect(entries[0].name).toBe("Admin");
    expect(entries[0].permissions).toEqual([
      { permission: "READ" },
      { permission: "UPDATE" },
    ]);
    expect(body[ACL_LIST_ROOT][0].objectType).toBe(31);
    expect(body[ACL_LIST_ROOT][0].objectId).toBe(5);
  });

  it("passes through already-flat entries inside AclList envelope", async () => {
    await saveObjectAcl({
      id: 1,
      name: "site",
      aclEntries: [
        {
          name: "Editor",
          permissions: [{ permission: "READ" }],
        },
      ],
    });
    const body = put.mock.calls[0][1] as { AclList: Array<Record<string, unknown>> };
    const entries = body[ACL_LIST_ROOT][0].aclEntries as Array<Record<string, unknown>>;
    expect(entries[0].name).toBe("Editor");
    expect(entries[0].permissions).toEqual([{ permission: "READ" }]);
  });
});
