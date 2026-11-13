/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  buildAllowedWorkflowsReplaceBody,
  cloneNamedObjectRefs,
  compareWorkflowRefs,
  namedObjectRefsEqual,
  refKey,
  sortWorkflowRefs,
  stageWorkflowRef,
  toNamedObjectRefPayload,
  withDefaultWorkflowFlags,
} from "../../../main/ts/developer/contentTypeWorkflows";

describe("contentTypeWorkflows helpers (CD-08)", () => {
  it("refKey prefers name then guid then index", () => {
    expect(refKey({ name: "percPage" }, 3)).toBe("name:percPage");
    expect(refKey({ guid: { stringValue: "2-1-9" } }, 3)).toBe("guid:2-1-9");
    expect(refKey({ guid: { uuid: 9 } }, 3)).toBe("uuid:9");
    expect(refKey({}, 3)).toBe("idx:3");
  });

  it("clones refs without sharing guid objects", () => {
    const guid = { stringValue: "0-23-4", uuid: 4 };
    const src = [{ name: "Simple Workflow", label: "Simple", guid, isDefault: true }];
    const cloned = cloneNamedObjectRefs(src);
    expect(cloned).toEqual(src);
    expect(cloned[0].guid).not.toBe(guid);
    cloned[0].guid!.uuid = 99;
    expect(guid.uuid).toBe(4);
  });

  it("treats default-flag mismatch as dirty", () => {
    const a = [{ name: "Simple Workflow", isDefault: true }];
    const b = [{ name: "Simple Workflow", isDefault: false }];
    expect(namedObjectRefsEqual(a, b)).toBe(false);
    expect(namedObjectRefsEqual(a, [{ name: "Simple Workflow", isDefault: true }])).toBe(true);
  });

  it("marks the matching defaultWorkflow row and falls back to first", () => {
    const list = [{ name: "Simple Workflow" }, { name: "Standard Workflow" }];
    expect(
      withDefaultWorkflowFlags(list, { name: "Standard Workflow" }).map((w) => ({
        name: w.name,
        isDefault: w.isDefault,
      })),
    ).toEqual([
      { name: "Simple Workflow", isDefault: false },
      { name: "Standard Workflow", isDefault: true },
    ]);
    expect(withDefaultWorkflowFlags([{ name: "Simple Workflow" }])[0].isDefault).toBe(true);
  });

  it("builds a full-replace body with payload refs and defaultWorkflow", () => {
    const body = buildAllowedWorkflowsReplaceBody([
      {
        name: "Simple Workflow",
        label: "Simple Workflow",
        isDefault: true,
        guid: { stringValue: "0-23-4", uuid: 4 },
      },
      { name: "Standard Workflow", label: "Standard Workflow" },
    ]);
    expect(body.allowedWorkflows).toEqual([
      {
        name: "Simple Workflow",
        isDefault: true,
        guid: { stringValue: "0-23-4", uuid: 4 },
      },
      { name: "Standard Workflow" },
    ]);
    expect(body.defaultWorkflow).toEqual({
      name: "Simple Workflow",
      isDefault: true,
      guid: { stringValue: "0-23-4", uuid: 4 },
    });
    expect(toNamedObjectRefPayload(body.allowedWorkflows)[1]).toEqual({
      name: "Standard Workflow",
    });
  });

  it("omits defaultWorkflow when the allowed set is empty", () => {
    expect(buildAllowedWorkflowsReplaceBody([])).toEqual({ allowedWorkflows: [] });
  });

  it("sorts staged edges by label case-insensitively (server GET order)", () => {
    const staged = sortWorkflowRefs([
      { name: "Standard Workflow", label: "Standard Workflow" },
      { name: "default workflow", label: "default workflow" },
    ]);
    expect(staged.map((w) => w.name)).toEqual(["default workflow", "Standard Workflow"]);
    expect(compareWorkflowRefs({ name: "b" }, { name: "A" })).toBeGreaterThan(0);
  });

  it("stages the workflow name as label and restores remove order (#6289)", () => {
    const initial = [
      { name: "Simple Workflow", label: "Simple Workflow", isDefault: true },
      { name: "Standard Workflow", label: "Standard Workflow", isDefault: false },
    ];
    // Remove index 0 (promotes Standard like removeWorkflow does), then
    // re-associate Simple: the exact edge list round-trips.
    const afterRemove = [
      { name: "Standard Workflow", label: "Standard Workflow", isDefault: true },
    ];
    const restored = stageWorkflowRef(afterRemove, "Simple Workflow", "Simple Workflow");
    expect(restored).toEqual(initial);
    expect(namedObjectRefsEqual(restored, initial)).toBe(true);
  });

  it("restores the persisted default flag when re-adding the default edge (#6289)", () => {
    const restored = stageWorkflowRef(
      [{ name: "Standard Workflow", label: "Standard Workflow", isDefault: true }],
      "Simple Workflow",
      "Simple Workflow",
    );
    expect(restored).toEqual([
      { name: "Simple Workflow", label: "Simple Workflow", isDefault: true },
      { name: "Standard Workflow", label: "Standard Workflow", isDefault: false },
    ]);
  });

  it("ignores duplicates and blanks when staging", () => {
    const prev = [{ name: "Simple Workflow", label: "Simple Workflow", isDefault: true }];
    expect(stageWorkflowRef(prev, "simple workflow", "Simple Workflow")).toBe(prev);
    expect(stageWorkflowRef(prev, "   ", "Simple Workflow")).toBe(prev);
  });
});
