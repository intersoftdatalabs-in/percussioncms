/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import type { ObjectAclEntry } from "../../../main/ts/api/developer/types";
import {
  SPECIAL_ACL_ANY_COMMUNITY_NAME,
  SPECIAL_ACL_DEFAULT_NAME,
  canRemoveAclEntry,
  createSpecialAclEntryTemplate,
  entryPrincipalName,
  hasSpecialAclEntry,
  isDuplicateAclEntry,
  isProtectedSpecialAclEntry,
  missingSpecialAclKinds,
  orderAclEntriesWithSpecialsFirst,
  specialAclKind,
  specialAclKindFromName,
  specialAclPrincipalName,
  specialAclPrincipalType,
} from "../../../main/ts/developer/objectAclSpecialEntries";

const roleAdmin: ObjectAclEntry = {
  id: 1,
  name: "Admin",
  type: { type: "ROLE", name: "Admin" },
  permissions: [{ permission: "OWNER" }],
};

const defaultAsUser: ObjectAclEntry = {
  id: 2,
  name: SPECIAL_ACL_DEFAULT_NAME,
  type: { type: "USER", name: SPECIAL_ACL_DEFAULT_NAME },
  permissions: [{ permission: "READ" }],
};

/** Historical/mis-typed payload still counts as Default special. */
const defaultAsRole: ObjectAclEntry = {
  id: 3,
  name: SPECIAL_ACL_DEFAULT_NAME,
  type: { type: "ROLE", name: SPECIAL_ACL_DEFAULT_NAME },
};

const anyCommunity: ObjectAclEntry = {
  id: 4,
  name: SPECIAL_ACL_ANY_COMMUNITY_NAME,
  type: { type: "COMMUNITY", name: SPECIAL_ACL_ANY_COMMUNITY_NAME },
  permissions: [{ permission: "RUNTIME_VISIBLE" }],
};

describe("objectAclSpecialEntries", () => {
  it("exports canonical special principal names matching PSTypedPrincipal", () => {
    expect(SPECIAL_ACL_DEFAULT_NAME).toBe("Default");
    expect(SPECIAL_ACL_ANY_COMMUNITY_NAME).toBe("AnyCommunity");
    expect(specialAclPrincipalName("default")).toBe("Default");
    expect(specialAclPrincipalName("any-community")).toBe("AnyCommunity");
    expect(specialAclPrincipalType("default")).toBe("USER");
    expect(specialAclPrincipalType("any-community")).toBe("COMMUNITY");
  });

  it("classifies Default and AnyCommunity by principal name", () => {
    expect(specialAclKind(defaultAsUser)).toBe("default");
    expect(specialAclKind(defaultAsRole)).toBe("default");
    expect(specialAclKind(anyCommunity)).toBe("any-community");
    expect(specialAclKind(roleAdmin)).toBeNull();
    expect(specialAclKindFromName("Default")).toBe("default");
    expect(specialAclKindFromName("AnyCommunity")).toBe("any-community");
    expect(specialAclKindFromName("Admin")).toBeNull();
    expect(specialAclKindFromName("default")).toBeNull(); // case-sensitive server contract
  });

  it("protects specials from removal", () => {
    expect(isProtectedSpecialAclEntry(defaultAsUser)).toBe(true);
    expect(isProtectedSpecialAclEntry(anyCommunity)).toBe(true);
    expect(isProtectedSpecialAclEntry(roleAdmin)).toBe(false);
    expect(canRemoveAclEntry(defaultAsUser)).toBe(false);
    expect(canRemoveAclEntry(anyCommunity)).toBe(false);
    expect(canRemoveAclEntry(roleAdmin)).toBe(true);
  });

  it("detects missing specials and hasSpecialAclEntry", () => {
    expect(missingSpecialAclKinds([])).toEqual(["default", "any-community"]);
    expect(missingSpecialAclKinds([defaultAsUser])).toEqual(["any-community"]);
    expect(missingSpecialAclKinds([anyCommunity])).toEqual(["default"]);
    expect(missingSpecialAclKinds([defaultAsUser, anyCommunity])).toEqual([]);
    expect(hasSpecialAclEntry([defaultAsRole], "default")).toBe(true);
    expect(hasSpecialAclEntry([roleAdmin], "default")).toBe(false);
  });

  it("createSpecialAclEntryTemplate matches REST TypedPrincipal shapes", () => {
    const d = createSpecialAclEntryTemplate("default", 42);
    expect(d.name).toBe("Default");
    expect(d.principal).toEqual({ name: "Default", type: "USER" });
    expect(d.type).toEqual({ type: "USER", name: "Default" });
    expect(d.aclId).toBe(42);
    expect(d.permissions).toEqual([{ permission: "READ" }]);

    const a = createSpecialAclEntryTemplate("any-community");
    expect(a.name).toBe("AnyCommunity");
    expect(a.principal).toEqual({ name: "AnyCommunity", type: "COMMUNITY" });
    expect(a.type).toEqual({ type: "COMMUNITY", name: "AnyCommunity" });
  });

  it("isDuplicateAclEntry treats specials as unique by kind", () => {
    const entries = [defaultAsRole, roleAdmin];
    expect(isDuplicateAclEntry(entries, "Default", "USER")).toBe(true);
    expect(isDuplicateAclEntry(entries, "Default", "ROLE")).toBe(true);
    expect(isDuplicateAclEntry(entries, "Admin", "ROLE")).toBe(true);
    expect(isDuplicateAclEntry(entries, "admin", "ROLE")).toBe(true);
    expect(isDuplicateAclEntry(entries, "Admin", "USER")).toBe(false);
    expect(isDuplicateAclEntry(entries, "AnyCommunity", "COMMUNITY")).toBe(false);
    expect(isDuplicateAclEntry(entries, "Editor", "ROLE")).toBe(false);
  });

  it("orderAclEntriesWithSpecialsFirst puts Default then AnyCommunity first", () => {
    const ordered = orderAclEntriesWithSpecialsFirst([
      roleAdmin,
      anyCommunity,
      defaultAsUser,
    ]);
    expect(ordered.map((e) => entryPrincipalName(e))).toEqual([
      "Default",
      "AnyCommunity",
      "Admin",
    ]);
  });
});
