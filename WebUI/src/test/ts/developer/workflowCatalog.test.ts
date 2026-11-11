/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  filterWorkflowCatalog,
  markDefaultWorkflowInCatalog,
  parseWorkflowSelection,
  renameWorkflowInCatalog,
  sortWorkflowCatalog,
  workflowOpenKey,
  workflowRowKey,
  workflowStepCount,
} from "../../../main/ts/developer/workflowCatalog";

describe("workflowCatalog helpers (#6028)", () => {
  it("parses new/copy/detail/catalog selections", () => {
    expect(parseWorkflowSelection("new")).toEqual({ kind: "create" });
    expect(parseWorkflowSelection("copy:Simple Workflow")).toEqual({
      kind: "copy",
      sourceName: "Simple Workflow",
    });
    expect(parseWorkflowSelection("Beta Flow")).toEqual({ kind: "detail", name: "Beta Flow" });
    expect(parseWorkflowSelection(null)).toEqual({ kind: "catalog" });
  });

  it("sorts by name and drops blank names", () => {
    expect(sortWorkflowCatalog(null)).toEqual([]);
    const sorted = sortWorkflowCatalog([
      { workflowName: "beta" },
      { workflowName: "  " },
      { workflowName: "Alpha" },
      {},
    ]);
    expect(sorted.map((w) => w.workflowName)).toEqual(["Alpha", "beta"]);
  });

  it("filters by name or description without mutating", () => {
    const rows = [
      { workflowName: "Alpha Flow", workflowDescription: "fast" },
      { workflowName: "Beta Flow", workflowDescription: "slow" },
    ];
    expect(filterWorkflowCatalog(rows, "")).toHaveLength(2);
    expect(filterWorkflowCatalog(rows, "beta").map((w) => w.workflowName)).toEqual(["Beta Flow"]);
    expect(filterWorkflowCatalog(rows, "FAST").map((w) => w.workflowName)).toEqual(["Alpha Flow"]);
    expect(filterWorkflowCatalog(rows, "zzz")).toEqual([]);
  });

  it("renames the matching row case-insensitively", () => {
    expect(renameWorkflowInCatalog(null, "a", "b")).toBeNull();
    const rows = [{ workflowName: "Nightly QA" }, { workflowName: "Other" }];
    const renamed = renameWorkflowInCatalog(rows, "nightly qa", "Nightly QA 2");
    expect(renamed!.map((w) => w.workflowName)).toEqual(["Nightly QA 2", "Other"]);
    expect(rows[0].workflowName).toBe("Nightly QA");
  });

  it("moves the default flag to the named workflow", () => {
    expect(markDefaultWorkflowInCatalog(null, "a")).toBeNull();
    const rows = [
      { workflowName: "Alpha Flow", defaultWorkflow: true },
      { workflowName: "Beta Flow", defaultWorkflow: false },
    ];
    const marked = markDefaultWorkflowInCatalog(rows, "beta flow");
    expect(marked!.map((w) => w.defaultWorkflow)).toEqual([false, true]);
  });

  it("builds row identity and step counts", () => {
    expect(workflowOpenKey({ workflowName: "  Simple Workflow " })).toBe("Simple Workflow");
    expect(workflowOpenKey({})).toBe("");
    expect(workflowRowKey({ workflowName: "A" }, 2)).toBe("A-2");
    expect(workflowStepCount({ workflowSteps: [{ stepName: "Draft" }] })).toBe(1);
    expect(workflowStepCount({})).toBe(0);
  });
});
