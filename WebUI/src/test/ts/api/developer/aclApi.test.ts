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
  createObjectAcl,
  getAclForObject,
  unwrapObjectAcl,
} from "../../../../main/ts/api/developer/aclApi";

vi.mock("../../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
}));

const get = client.get as ReturnType<typeof vi.fn>;
const post = client.post as ReturnType<typeof vi.fn>;

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
});
