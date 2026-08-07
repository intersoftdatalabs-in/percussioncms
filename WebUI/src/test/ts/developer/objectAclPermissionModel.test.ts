/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import { ACL_PERMISSIONS } from "../../../main/ts/api/developer/aclApi";
import {
  DESIGN_ACCESS_PERMISSIONS,
  LAYERED_ACL_PERMISSIONS,
  RUNTIME_ACCESS_PERMISSIONS,
  RUNTIME_RELEVANT_OBJECT_KINDS,
  aclPermissionLayer,
  aclPermissionShortLabel,
  hasRuntimeAccessPermission,
  isDesignAccessPermission,
  isRuntimeAccessPermission,
  isRuntimeRelevantObjectKind,
  partitionAclPermissions,
  shouldShowRuntimeAccessColumns,
  visibleAclPermissionsForObject,
} from "../../../main/ts/developer/objectAclPermissionModel";

describe("objectAclPermissionModel (CD-19 design vs runtime)", () => {
  it("splits REST Permissions into design and runtime layers", () => {
    expect(DESIGN_ACCESS_PERMISSIONS).toEqual([
      "READ",
      "UPDATE",
      "DELETE",
      "OWNER",
    ]);
    expect(RUNTIME_ACCESS_PERMISSIONS).toEqual(["RUNTIME_VISIBLE"]);
    expect(LAYERED_ACL_PERMISSIONS).toEqual([
      "READ",
      "UPDATE",
      "DELETE",
      "OWNER",
      "RUNTIME_VISIBLE",
    ]);
    // All known REST flags covered exactly once
    expect(new Set(LAYERED_ACL_PERMISSIONS).size).toBe(ACL_PERMISSIONS.length);
    for (const p of ACL_PERMISSIONS) {
      expect(LAYERED_ACL_PERMISSIONS).toContain(p);
    }
  });

  it("classifies each permission name", () => {
    expect(aclPermissionLayer("READ")).toBe("design");
    expect(aclPermissionLayer("UPDATE")).toBe("design");
    expect(aclPermissionLayer("DELETE")).toBe("design");
    expect(aclPermissionLayer("OWNER")).toBe("design");
    expect(aclPermissionLayer("RUNTIME_VISIBLE")).toBe("runtime");
    expect(aclPermissionLayer("runtime_visible")).toBe("runtime");
    expect(aclPermissionLayer("")).toBeNull();
    expect(aclPermissionLayer(null)).toBeNull();
    expect(isDesignAccessPermission("OWNER")).toBe(true);
    expect(isRuntimeAccessPermission("RUNTIME_VISIBLE")).toBe(true);
    expect(isDesignAccessPermission("RUNTIME_VISIBLE")).toBe(false);
  });

  it("marks Workbench §5.4 runtime-relevant object kinds", () => {
    const expected = [
      "content-type",
      "display-format",
      "action-menu",
      "menu-entry",
      "search",
      "site",
      "template",
      "variant",
      "view",
      "workflow",
    ];
    for (const k of expected) {
      expect(RUNTIME_RELEVANT_OBJECT_KINDS.has(k as never)).toBe(true);
      expect(isRuntimeRelevantObjectKind(k as never)).toBe(true);
    }
    expect(isRuntimeRelevantObjectKind("keyword")).toBe(false);
    expect(isRuntimeRelevantObjectKind("slot")).toBe(false);
    expect(isRuntimeRelevantObjectKind("pipeline")).toBe(false);
    // Unknown / null → show runtime (safe default for mounts without kind)
    expect(isRuntimeRelevantObjectKind(null)).toBe(true);
    expect(isRuntimeRelevantObjectKind(undefined)).toBe(true);
    expect(isRuntimeRelevantObjectKind("unknown")).toBe(true);
  });

  it("controls runtime column visibility", () => {
    expect(shouldShowRuntimeAccessColumns("content-type")).toBe(true);
    expect(shouldShowRuntimeAccessColumns("template")).toBe(true);
    expect(shouldShowRuntimeAccessColumns("keyword")).toBe(false);
    expect(shouldShowRuntimeAccessColumns("keyword", { forceShow: true })).toBe(
      true,
    );
    expect(shouldShowRuntimeAccessColumns(null)).toBe(true);
  });

  it("returns visible permission columns for object kind", () => {
    expect(visibleAclPermissionsForObject("content-type")).toEqual(
      LAYERED_ACL_PERMISSIONS,
    );
    expect(visibleAclPermissionsForObject("keyword")).toEqual(
      DESIGN_ACCESS_PERMISSIONS,
    );
    expect(
      visibleAclPermissionsForObject("keyword", { forceShowRuntime: true }),
    ).toEqual(LAYERED_ACL_PERMISSIONS);
  });

  it("provides Workbench-aligned short labels", () => {
    expect(aclPermissionShortLabel("READ")).toBe("Read");
    expect(aclPermissionShortLabel("OWNER")).toBe("Modify ACL");
    expect(aclPermissionShortLabel("RUNTIME_VISIBLE")).toBe("Visible");
    expect(aclPermissionShortLabel("CUSTOM_FLAG")).toBe("CUSTOM FLAG");
  });

  it("partitions permission lists and detects runtime bits", () => {
    const parts = partitionAclPermissions([
      "READ",
      "RUNTIME_VISIBLE",
      "OWNER",
      "mystery",
    ]);
    expect(parts.design).toEqual(["READ", "OWNER"]);
    expect(parts.runtime).toEqual(["RUNTIME_VISIBLE"]);
    expect(parts.other).toEqual(["mystery"]);
    expect(hasRuntimeAccessPermission(["READ", "OWNER"])).toBe(false);
    expect(hasRuntimeAccessPermission(["READ", "RUNTIME_VISIBLE"])).toBe(true);
  });
});
