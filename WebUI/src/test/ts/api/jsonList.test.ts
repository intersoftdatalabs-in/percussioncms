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
  asObjectArray,
  asStringArray,
  parseRoleNameList,
  parseUserNameList,
} from "../../../main/ts/api/jsonList";

describe("asStringArray", () => {
  it("returns empty for null / non-list", () => {
    expect(asStringArray(null)).toEqual([]);
    expect(asStringArray(undefined)).toEqual([]);
    expect(asStringArray(1)).toEqual([]);
  });

  it("wraps a single Jackson string item", () => {
    expect(asStringArray("Admin")).toEqual(["Admin"]);
  });

  it("filters blank array entries", () => {
    expect(asStringArray(["Admin", "  ", "Editor"])).toEqual([
      "Admin",
      "Editor",
    ]);
  });
});

describe("parseRoleNameList", () => {
  it("unwraps RoleList.roles array", () => {
    expect(
      parseRoleNameList({ RoleList: { roles: ["Admin", "Editor"] } }),
    ).toEqual(["Admin", "Editor"]);
  });

  it("unwraps single-string roles (failing #3202 payload shape)", () => {
    expect(parseRoleNameList({ RoleList: { roles: "Admin" } })).toEqual([
      "Admin",
    ]);
  });

  it("unwraps JAXB item wrap", () => {
    expect(
      parseRoleNameList({ RoleList: { roles: { role: ["Admin", "Author"] } } }),
    ).toEqual(["Admin", "Author"]);
  });

  it("accepts already-unwrapped { roles }", () => {
    expect(parseRoleNameList({ roles: ["Contributor"] })).toEqual([
      "Contributor",
    ]);
  });

  it("returns empty for null / unknown object", () => {
    expect(parseRoleNameList(null)).toEqual([]);
    expect(parseRoleNameList({ unexpected: true })).toEqual([]);
  });
});

describe("parseUserNameList", () => {
  it("unwraps UserList.users and single-string users", () => {
    expect(parseUserNameList({ UserList: { users: ["admin"] } })).toEqual([
      "admin",
    ]);
    expect(parseUserNameList({ UserList: { users: "admin" } })).toEqual([
      "admin",
    ]);
  });
});

describe("asObjectArray", () => {
  it("returns bare arrays and unwraps first array property", () => {
    expect(asObjectArray([{ id: "1" }])).toEqual([{ id: "1" }]);
    expect(asObjectArray({ ScheduledTask: [{ id: "1" }] })).toEqual([
      { id: "1" },
    ]);
  });

  it("returns empty for null / non-list objects", () => {
    expect(asObjectArray(null)).toEqual([]);
    expect(asObjectArray({ empty: false })).toEqual([]);
  });
});
