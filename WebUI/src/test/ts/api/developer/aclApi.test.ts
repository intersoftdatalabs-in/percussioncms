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

import { afterEach, describe, expect, it, vi } from "vitest";
import { getAclForObject, unwrapObjectAcl } from "../../../../main/ts/api/developer/aclApi";

describe("unwrapObjectAcl", () => {
  it("unwraps Jackson Acl root so entries are reachable (#3200)", () => {
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

  it("passes through flat ACL bodies", () => {
    const flat = { id: 1, name: "x", aclEntries: [] };
    expect(unwrapObjectAcl(flat)).toEqual(flat);
  });
});

describe("getAclForObject", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("unwraps wrapped GET /acls/object/{guid} body", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        return new Response(
          JSON.stringify({
            Acl: { id: 9, name: "df", aclEntries: [{ name: "Default" }] },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }),
    );
    const acl = await getAclForObject("0-31-5");
    expect(acl.id).toBe(9);
    expect(acl.name).toBe("df");
    expect(acl.aclEntries).toHaveLength(1);
  });
});
