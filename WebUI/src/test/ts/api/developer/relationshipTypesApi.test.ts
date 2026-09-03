/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  RELATIONSHIP_TYPE_DESIGN_GAPS,
  RELATIONSHIP_TYPE_ROOT,
  createRelationshipType,
  deleteRelationshipType,
  getRelationshipTypeDetail,
  isRelationshipTypeWriteReady,
  isSystemRelationshipType,
  isValidRelationshipTypeName,
  listRelationshipTypes,
  updateRelationshipType,
  wrapRelationshipTypeForWire,
} from "../../../../main/ts/api/developer/relationshipTypesApi";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("relationshipTypesApi helpers", () => {
  it("wraps write body under RelationshipType root", () => {
    expect(wrapRelationshipTypeForWire({ name: "A", category: "rs_generic" })).toEqual({
      [RELATIONSHIP_TYPE_ROOT]: { name: "A", category: "rs_generic" },
    });
  });

  it("validates names like the REST adaptor", () => {
    expect(isValidRelationshipTypeName("MyUserRel")).toBe(true);
    // Leading/trailing whitespace is trimmed before validation (requireValidName).
    expect(isValidRelationshipTypeName(" bad")).toBe(true);
    expect(isValidRelationshipTypeName("has space")).toBe(false);
    expect(isValidRelationshipTypeName("star*")).toBe(false);
    expect(isValidRelationshipTypeName("a/b")).toBe(false);
    expect(isValidRelationshipTypeName("")).toBe(false);
  });

  it("requires category or copyFrom on create", () => {
    expect(
      isRelationshipTypeWriteReady({
        isNew: true,
        name: "MyUserRel",
        category: "",
        copyFrom: "",
      }),
    ).toBe(false);
    expect(
      isRelationshipTypeWriteReady({
        isNew: true,
        name: "MyUserRel",
        category: "rs_generic",
        copyFrom: "",
      }),
    ).toBe(true);
    expect(
      isRelationshipTypeWriteReady({
        isNew: true,
        name: "MyUserRel",
        category: "",
        copyFrom: "ActiveAssembly",
      }),
    ).toBe(true);
    expect(
      isRelationshipTypeWriteReady({
        isNew: false,
        name: "",
        category: "",
        copyFrom: "",
      }),
    ).toBe(true);
  });

  it("detects system types for chrome immutability", () => {
    expect(isSystemRelationshipType({ systemType: true, userType: false })).toBe(true);
    expect(isSystemRelationshipType({ systemType: false, userType: true })).toBe(false);
    expect(isSystemRelationshipType({ type: "system" })).toBe(true);
    expect(isSystemRelationshipType(null)).toBe(false);
  });

  it("omits create/update/delete from remaining design gaps constant", () => {
    expect(RELATIONSHIP_TYPE_DESIGN_GAPS.join(" ")).not.toMatch(/create|update|delete/i);
    expect(RELATIONSHIP_TYPE_DESIGN_GAPS.length).toBeGreaterThan(0);
  });
});

describe("relationshipTypesApi REST", () => {
  it("listRelationshipTypes unwraps array envelopes", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue({
      RelationshipType: [{ name: "A" }, { name: "B" }],
    });
    const list = await listRelationshipTypes();
    expect(list.map((t) => t.name)).toEqual(["A", "B"]);
    spy.mockRestore();
  });

  it("getRelationshipTypeDetail unwraps and fills gaps", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue({
      RelationshipType: { name: "rs_folder" },
    });
    const detail = await getRelationshipTypeDetail("rs_folder");
    expect(detail.name).toBe("rs_folder");
    expect(detail.designGaps).toEqual(RELATIONSHIP_TYPE_DESIGN_GAPS);
    spy.mockRestore();
  });

  it("createRelationshipType posts wrapped body", async () => {
    const spy = vi.spyOn(client, "post").mockResolvedValue({
      RelationshipType: { name: "MyUserRel", userType: true },
    });
    const created = await createRelationshipType({
      name: "MyUserRel",
      category: "rs_generic",
    });
    expect(spy).toHaveBeenCalledWith(
      expect.any(String),
      wrapRelationshipTypeForWire({ name: "MyUserRel", category: "rs_generic" }),
    );
    expect(created.name).toBe("MyUserRel");
    spy.mockRestore();
  });

  it("updateRelationshipType puts wrapped body", async () => {
    const spy = vi.spyOn(client, "put").mockResolvedValue({
      name: "MyUserRel",
      label: "Updated",
      designGaps: ["server-gap"],
    });
    const updated = await updateRelationshipType("MyUserRel", {
      label: "Updated",
      allowCloning: true,
    });
    expect(spy.mock.calls[0][0]).toContain("MyUserRel");
    expect(updated.label).toBe("Updated");
    expect(updated.designGaps).toEqual(["server-gap"]);
    spy.mockRestore();
  });

  it("deleteRelationshipType calls del", async () => {
    const spy = vi.spyOn(client, "del").mockResolvedValue(undefined);
    await deleteRelationshipType("MyUserRel");
    expect(spy.mock.calls[0][0]).toContain("MyUserRel");
    spy.mockRestore();
  });
});
