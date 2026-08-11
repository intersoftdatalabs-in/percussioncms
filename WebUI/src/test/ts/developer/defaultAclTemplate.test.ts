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
import type { ObjectAclEntry } from "../../../main/ts/api/developer/types";
import {
  cloneDefaultAclTemplate,
  defaultAclTemplatesEqual,
  DEFAULT_ACL_TEMPLATE_PREF_NAME,
  mergeTemplateOntoAclEntries,
  parseDefaultAclTemplate,
  serializeDefaultAclTemplate,
  shouldApplyDefaultAclTemplate,
  systemDefaultAclTemplate,
  templateEntryToObjectAclEntry,
} from "../../../main/ts/developer/defaultAclTemplate";

describe("defaultAclTemplate helpers", () => {
  it("exposes a stable preference name for REST storage", () => {
    expect(DEFAULT_ACL_TEMPLATE_PREF_NAME).toBe(
      "developer.defaultObjectAclTemplate",
    );
  });

  it("system default includes Default USER and AnyCommunity COMMUNITY", () => {
    const t = systemDefaultAclTemplate();
    expect(t.version).toBe(1);
    expect(t.entries).toHaveLength(2);
    expect(t.entries[0]).toMatchObject({
      name: "Default",
      type: "USER",
    });
    expect(t.entries[0].permissions).toEqual(
      expect.arrayContaining(["READ", "UPDATE", "DELETE", "OWNER"]),
    );
    expect(t.entries[1]).toMatchObject({
      name: "AnyCommunity",
      type: "COMMUNITY",
      permissions: ["RUNTIME_VISIBLE"],
    });
  });

  it("round-trips serialize / parse", () => {
    const sys = systemDefaultAclTemplate();
    const raw = serializeDefaultAclTemplate(sys);
    const parsed = parseDefaultAclTemplate(raw);
    expect(parsed).not.toBeNull();
    expect(defaultAclTemplatesEqual(parsed!, sys)).toBe(true);
  });

  /**
   * #2948 — Runtime visibility on Default must survive serialize → parse
   * (preference store round-trip). Client helpers already preserve the token;
   * this guards against accidental allowlist filtering of RUNTIME_VISIBLE.
   */
  it("round-trips RUNTIME_VISIBLE on Default USER entry (#2948)", () => {
    const template = cloneDefaultAclTemplate(systemDefaultAclTemplate());
    expect(template.entries[0].permissions).not.toContain("RUNTIME_VISIBLE");
    template.entries[0].permissions.push("RUNTIME_VISIBLE");

    const raw = serializeDefaultAclTemplate(template);
    expect(raw).toContain("RUNTIME_VISIBLE");

    const parsed = parseDefaultAclTemplate(raw);
    expect(parsed).not.toBeNull();
    expect(parsed!.entries[0].name).toBe("Default");
    expect(parsed!.entries[0].permissions).toEqual(
      expect.arrayContaining([
        "READ",
        "UPDATE",
        "DELETE",
        "OWNER",
        "RUNTIME_VISIBLE",
      ]),
    );
    expect(defaultAclTemplatesEqual(parsed!, template)).toBe(true);
  });

  it("parse returns null for empty or invalid payloads", () => {
    expect(parseDefaultAclTemplate(null)).toBeNull();
    expect(parseDefaultAclTemplate("")).toBeNull();
    expect(parseDefaultAclTemplate("not-json")).toBeNull();
    expect(parseDefaultAclTemplate("{}")).toBeNull();
    expect(parseDefaultAclTemplate(JSON.stringify({ entries: "x" }))).toBeNull();
  });

  it("parse drops invalid entries and normalizes types/permissions", () => {
    const parsed = parseDefaultAclTemplate(
      JSON.stringify({
        version: 9,
        entries: [
          { name: "  Admin  ", type: "role", permissions: ["read", "READ", ""] },
          { name: "", type: "USER", permissions: ["READ"] },
          { name: "Bad", type: "WIDGET", permissions: ["READ"] },
          { name: "Editors", type: "GROUP", permissions: 1 },
        ],
      }),
    );
    expect(parsed).toEqual({
      version: 1,
      entries: [
        { name: "Admin", type: "ROLE", permissions: ["READ"] },
        { name: "Editors", type: "GROUP", permissions: [] },
      ],
    });
  });

  it("clone is deep for permissions arrays", () => {
    const a = systemDefaultAclTemplate();
    const b = cloneDefaultAclTemplate(a);
    b.entries[0].permissions.push("RUNTIME_VISIBLE");
    expect(a.entries[0].permissions).not.toContain("RUNTIME_VISIBLE");
  });

  it("templateEntryToObjectAclEntry maps name/type/permissions", () => {
    const e = templateEntryToObjectAclEntry(
      { name: "Editors", type: "ROLE", permissions: ["READ", "UPDATE"] },
      42,
    );
    expect(e).toMatchObject({
      name: "Editors",
      aclId: 42,
      principal: { name: "Editors", type: "ROLE" },
      type: { type: "ROLE", name: "Editors" },
    });
    expect(e.permissions).toEqual([
      { permission: "READ" },
      { permission: "UPDATE" },
    ]);
  });

  it("mergeTemplateOntoAclEntries preserves owner and adds missing template rows", () => {
    const existing: ObjectAclEntry[] = [
      {
        id: 1,
        name: "admin",
        type: { type: "USER", name: "admin" },
        permissions: [{ permission: "OWNER" }],
      },
    ];
    const template = systemDefaultAclTemplate();
    const { entries, added } = mergeTemplateOntoAclEntries(existing, template, 7);
    expect(added).toBe(2);
    expect(entries).toHaveLength(3);
    expect(entries[0].name).toBe("admin");
    expect(entries.map((e) => e.name)).toEqual([
      "admin",
      "Default",
      "AnyCommunity",
    ]);
    expect(entries[1].aclId).toBe(7);
  });

  it("merge skips template principals that already exist (case-insensitive)", () => {
    const existing: ObjectAclEntry[] = [
      {
        name: "default",
        type: { type: "USER", name: "default" },
        permissions: [{ permission: "READ" }],
      },
    ];
    const { entries, added } = mergeTemplateOntoAclEntries(
      existing,
      systemDefaultAclTemplate(),
    );
    expect(added).toBe(1);
    expect(entries.map((e) => e.name)).toEqual(["default", "AnyCommunity"]);
    // Existing Default-like row is not replaced
    expect(entries[0].permissions).toEqual([{ permission: "READ" }]);
  });

  it("shouldApplyDefaultAclTemplate requires non-empty entries", () => {
    expect(shouldApplyDefaultAclTemplate(null)).toBe(false);
    expect(shouldApplyDefaultAclTemplate({ version: 1, entries: [] })).toBe(
      false,
    );
    expect(shouldApplyDefaultAclTemplate(systemDefaultAclTemplate())).toBe(true);
  });
});
