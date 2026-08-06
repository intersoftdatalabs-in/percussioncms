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
import type {
  PSFolderPermission,
  PSPrincipal,
} from "../../../main/ts/api/contentExplorer/types";
import {
  ACCESS_RANK,
  canEditSecurityPanel,
  canViewSecurityPanel,
  detectSelfLockout,
  wouldSelfLockout,
} from "../../../main/ts/contentExplorer/aclLockout";

function p(name: string, type: "USER" | "ROLE" = "USER"): PSPrincipal {
  return { type, name };
}

function permission(
  admin: string[] = [],
  write: string[] = [],
  read: string[] = [],
  view: string[] = [],
  accessLevel: "ADMIN" | "WRITE" | "READ" | "VIEW" = "ADMIN",
): PSFolderPermission {
  return {
    accessLevel,
    adminPrincipals: admin.map((n) => p(n)),
    writePrincipals: write.map((n) => p(n)),
    readPrincipals: read.map((n) => p(n)),
    viewPrincipals: view.map((n) => p(n)),
  };
}

describe("detectSelfLockout", () => {
  it("returns the ADMIN level when the user is removed from admin only", () => {
    const before = permission(["Admin"]);
    const after = permission([]);
    const result = detectSelfLockout(before, after, ["Admin"]);
    expect(result).toEqual([{ level: "adminPrincipals", rank: ACCESS_RANK.ADMIN }]);
  });

  it("returns both ADMIN + WRITE levels when both contain the user and both lose them", () => {
    const before = permission(["Admin"], ["Admin"]);
    const after = permission([], []);
    const result = detectSelfLockout(before, after, ["Admin"]);
    expect(result.map((r) => r.level)).toEqual([
      "adminPrincipals",
      "writePrincipals",
    ]);
    expect(result.map((r) => r.rank)).toEqual([
      ACCESS_RANK.ADMIN,
      ACCESS_RANK.WRITE,
    ]);
  });

  it("returns the higher-ranked level first (ADMIN before WRITE)", () => {
    const before = permission(["Admin"], ["Admin"]);
    const after = permission([], []);
    const [first] = detectSelfLockout(before, after, ["Admin"]);
    expect(first?.level).toBe("adminPrincipals");
    expect(first?.rank).toBe(0);
  });

  it("does NOT detect lockout when the user is NOT in any before list", () => {
    const before = permission(["SomeoneElse"], [], []);
    const after = permission([], [], []);
    const result = detectSelfLockout(before, after, ["Admin"]);
    expect(result).toEqual([]);
  });

  it("does NOT detect lockout when the user IS still present in the after list", () => {
    const before = permission(["Admin"], ["Admin"]);
    // Keep Admin in BOTH admin + write principals after the edit;
    // append one new write principal. No removal anywhere → no lockout.
    const after = permission(["Admin"], ["Admin", "SomeoneElse"]);
    const result = detectSelfLockout(before, after, ["Admin"]);
    expect(result).toEqual([]);
  });

  it("matches by ROLE name as well as USER name", () => {
    const before = permission([], ["Editor"]);
    const after = permission([], []);
    const result = detectSelfLockout(before, after, ["Editor"]);
    expect(result.map((r) => r.level)).toEqual(["writePrincipals"]);
  });

  it("matches multiple roles the user holds (any one is enough to detect)", () => {
    const before = permission(["Editor"]);
    const after = permission([]);
    const result = detectSelfLockout(before, after, ["Admin", "Editor"]);
    expect(result.map((r) => r.level)).toEqual(["adminPrincipals"]);
  });

  it("returns empty when identities argument is empty", () => {
    const before = permission(["Admin"]);
    const after = permission([]);
    expect(detectSelfLockout(before, after, [])).toEqual([]);
  });

  it("treats null principal lists as empty (defensive against server omissions)", () => {
    const before = permission();
    delete before.adminPrincipals;
    const after = permission();
    delete after.adminPrincipals;
    const result = detectSelfLockout(before, after, ["Admin"]);
    expect(result).toEqual([]);
  });

  it("matches the principal NAME only — TYPE (USER vs ROLE) does not matter", () => {
    const beforeUser: PSFolderPermission = {
      accessLevel: "ADMIN",
      adminPrincipals: [p("Admin", "USER")],
    };
    const afterUser: PSFolderPermission = {
      accessLevel: "ADMIN",
      adminPrincipals: [p("Admin", "ROLE")],
    };
    // Type changed but name still matches → no removal detected.
    expect(detectSelfLockout(beforeUser, afterUser, ["Admin"])).toEqual([]);
  });
});

describe("wouldSelfLockout", () => {
  it("returns true when the user is removed from any level", () => {
    const before = permission(["Admin"]);
    const after = permission([]);
    expect(wouldSelfLockout(before, after, ["Admin"])).toBe(true);
  });

  it("returns false when the user is not removed", () => {
    const before = permission(["Admin"], ["Admin"]);
    const after = permission(["Admin"], ["Admin"]);
    expect(wouldSelfLockout(before, after, ["Admin"])).toBe(false);
  });
});

describe("canViewSecurityPanel (FR-016 read-only gate)", () => {
  it("allows when accessLevel is ADMIN", () => {
    expect(canViewSecurityPanel(permission())).toBe(true);
  });
  it("allows when accessLevel is WRITE", () => {
    expect(canViewSecurityPanel(permission([], [], [], [], "WRITE"))).toBe(
      true,
    );
  });
  it("allows when accessLevel is READ (panel renders read-only)", () => {
    expect(canViewSecurityPanel(permission([], [], [], [], "READ"))).toBe(
      true,
    );
  });
  it("denies when accessLevel is VIEW (insufficient rights to see the panel)", () => {
    expect(canViewSecurityPanel(permission([], [], [], [], "VIEW"))).toBe(
      false,
    );
  });
  it("denies when permission is undefined", () => {
    expect(canViewSecurityPanel(undefined)).toBe(false);
  });
});

describe("canEditSecurityPanel (edit controls gate)", () => {
  it("allows only when accessLevel is ADMIN", () => {
    expect(canEditSecurityPanel(permission())).toBe(true);
    expect(canEditSecurityPanel(permission([], [], [], [], "WRITE"))).toBe(
      false,
    );
    expect(canEditSecurityPanel(permission([], [], [], [], "READ"))).toBe(
      false,
    );
    expect(canEditSecurityPanel(permission([], [], [], [], "VIEW"))).toBe(
      false,
    );
  });
  it("denies when permission is undefined", () => {
    expect(canEditSecurityPanel(undefined)).toBe(false);
  });
});

describe("ACCESS_RANK ordering", () => {
  it("ranks ADMIN lowest, VIEW highest", () => {
    expect(ACCESS_RANK.ADMIN).toBe(0);
    expect(ACCESS_RANK.WRITE).toBe(1);
    expect(ACCESS_RANK.READ).toBe(2);
    expect(ACCESS_RANK.VIEW).toBe(3);
  });
});
