/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  extractIncludeCatalogFields,
  includedFieldOrigin,
  isFieldIncluded,
  isIncludeLockConflict,
  parseIncludeFieldOrigin,
  toIncludeCandidates,
  unusedIncludeCandidates,
} from "../../../main/ts/developer/contentTypeIncludeField";

describe("contentTypeIncludeField helpers (CD-04)", () => {
  it("parses only system or shared origins", () => {
    expect(parseIncludeFieldOrigin("system")).toBe("system");
    expect(parseIncludeFieldOrigin("SHARED")).toBe("shared");
    expect(parseIncludeFieldOrigin("local")).toBeNull();
    expect(parseIncludeFieldOrigin("")).toBeNull();
  });

  it("detects already-included fields case-insensitively", () => {
    const fields = [{ name: "sys_title", fieldType: "system" }];
    expect(isFieldIncluded(fields, "SYS_TITLE")).toBe(true);
    expect(isFieldIncluded(fields, "sys_suffix")).toBe(false);
    expect(includedFieldOrigin(fields, "sys_title")).toBe("system");
    expect(includedFieldOrigin(fields, "missing")).toBe("");
  });

  it("treats 409 lock messages as lock conflicts and duplicates as not", () => {
    expect(
      isIncludeLockConflict({
        status: 409,
        body: { message: "design lock required" },
      }),
    ).toBe(true);
    expect(
      isIncludeLockConflict({
        status: 409,
        body: { message: "Field already included: sys_title" },
      }),
    ).toBe(false);
    expect(isIncludeLockConflict({ status: 404, body: { message: "lock" } })).toBe(false);
    expect(
      isIncludeLockConflict({
        status: 409,
        body: { message: "Field already included; lock kept" },
      }),
    ).toBe(false);
    expect(
      isIncludeLockConflict({
        status: 409,
        body: { message: "conflict: field not found" },
      }),
    ).toBe(false);
  });

  it("unwraps WRAP_ROOT systemdef and shared group field catalogs", () => {
    expect(
      extractIncludeCatalogFields({
        SystemDefDetail: { fields: [{ name: "sys_title" }, { name: "sys_suffix" }] },
      }).map((f) => f.name),
    ).toEqual(["sys_title", "sys_suffix"]);
    expect(
      extractIncludeCatalogFields({
        SharedFieldGroupDetail: { fields: [{ name: "displaytitle" }] },
      }).map((f) => f.name),
    ).toEqual(["displaytitle"]);
  });

  it("builds unused catalog candidates for the picker", () => {
    const candidates = toIncludeCandidates(
      [{ name: "sys_title" }, { name: "sys_suffix" }, { name: "sys_title" }],
      "system",
    );
    expect(candidates.map((c) => c.name)).toEqual(["sys_suffix", "sys_title"]);
    expect(
      unusedIncludeCandidates(candidates, [{ name: "sys_title", fieldType: "system" }]).map(
        (c) => c.name,
      ),
    ).toEqual(["sys_suffix"]);
  });
});
