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

/**
 * 008 US1 (T008): offer-set matrix for workflow transition menus
 * (FR-001/002/010). The server is authoritative — these pure helpers must
 * project exactly the permitted triggers, omit the group when empty, and
 * recompute cleanly after execute.
 */
import { describe, expect, it } from "vitest";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  buildWorkflowTransitionMenu,
  intersectWorkflowMenus,
  isWorkflowTransitionActionName,
  mergeWorkflowMenuActions,
  parseWorkflowTransitionTrigger,
  triggerCommentRequired,
} from "../../../main/ts/contentExplorer/workflowMenuActions";
import {
  isNonWorkflowedItem,
  isWorkflowEligibleItem,
} from "../../../main/ts/contentExplorer/workflowEligibility";

function item(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    id: "42",
    name: "page1",
    type: "percPage",
    path: "/Sites/site1/page1",
    ...overrides,
  } as PSPathItem;
}

describe("transition offer projection (FR-001/002)", () => {
  it("offers exactly the permitted triggers, nothing more", () => {
    const menu = buildWorkflowTransitionMenu(["Approve", "Reject"]);
    expect(menu).not.toBeNull();
    expect(menu!.children!.map((c) => c.label)).toEqual(["Approve", "Reject"]);
    // Tagged names route to the client invoke handler.
    for (const child of menu!.children!) {
      expect(isWorkflowTransitionActionName(child.name)).toBe(true);
    }
  });

  it("omits the group when no usable triggers exist (empty offer is valid)", () => {
    expect(buildWorkflowTransitionMenu([])).toBeNull();
    expect(buildWorkflowTransitionMenu(null)).toBeNull();
    expect(buildWorkflowTransitionMenu(["  ", ""])).toBeNull();
  });

  it("de-dupes triggers, preserving first-seen order", () => {
    const menu = buildWorkflowTransitionMenu(["Approve", "Approve", "Reject"]);
    expect(menu!.children!.map((c) => c.label)).toEqual(["Approve", "Reject"]);
  });

  it("round-trips trigger names through tag/parse", () => {
    expect(parseWorkflowTransitionTrigger("workflow-transition:Approve")).toBe("Approve");
    expect(parseWorkflowTransitionTrigger("other-action")).toBeNull();
    expect(parseWorkflowTransitionTrigger(undefined)).toBeNull();
  });

  it("folds case/space for comment-required matching", () => {
    expect(triggerCommentRequired("Force Check In", ["forcecheckin"])).toBe(true);
    expect(triggerCommentRequired("Approve", ["Reject"])).toBe(false);
    expect(triggerCommentRequired("", ["Approve"])).toBe(false);
  });
});

describe("multi-select offer intersection (FR-001 bulk)", () => {
  it("offers only triggers shared by every selected item", () => {
    const a = buildWorkflowTransitionMenu(["Approve", "Reject"]);
    const b = buildWorkflowTransitionMenu(["Approve", "Request Changes"]);
    const shared = intersectWorkflowMenus([a, b]);
    expect(shared!.children!.map((c) => c.label)).toEqual(["Approve"]);
  });

  it("offers nothing when one item has no transitions", () => {
    const a = buildWorkflowTransitionMenu(["Approve"]);
    expect(intersectWorkflowMenus([a, null])).toBeNull();
    expect(intersectWorkflowMenus([])).toBeNull();
  });

  it("propagates comment-required when any item marks the shared trigger", () => {
    const a = buildWorkflowTransitionMenu(["Approve"], {
      commentRequiredTriggers: ["Approve"],
    });
    const b = buildWorkflowTransitionMenu(["Approve"]);
    const shared = intersectWorkflowMenus([a, b]);
    expect(shared!.children![0].commentRequired).toBe(true);
  });
});

describe("post-execute recompute (FR-010)", () => {
  it("replaces the prior group and re-sorts instead of stacking", () => {
    const before = buildWorkflowTransitionMenu(["Approve", "Reject"])!;
    const after = buildWorkflowTransitionMenu(["Publish"])!;
    const base = [{ ...before, sortRank: 1 }];
    const merged = mergeWorkflowMenuActions(base, after);
    expect(merged.filter((a) => a.name === "workflow")).toHaveLength(1);
    expect(merged.find((a) => a.name === "workflow")!.children!.map((c) => c.label)).toEqual([
      "Publish",
    ]);
  });

  it("removes the group when the new state offers nothing", () => {
    const before = buildWorkflowTransitionMenu(["Approve"])!;
    const merged = mergeWorkflowMenuActions([before], null);
    expect(merged.some((a) => a.name === "workflow")).toBe(false);
  });
});

describe("eligibility gating (FR-002)", () => {
  it("treats folders and workflow-less items as non-workflowed", () => {
    expect(isNonWorkflowedItem(null)).toBe(true);
    expect(isNonWorkflowedItem(item({ type: "folder", path: "/Sites/site1/folder/" }))).toBe(true);
    expect(isNonWorkflowedItem(item({ workflowId: -1 } as Partial<PSPathItem>))).toBe(true);
    expect(isWorkflowEligibleItem(item({ workflowId: 5 } as Partial<PSPathItem>))).toBe(true);
    expect(isWorkflowEligibleItem(item({ workflowId: -1 } as Partial<PSPathItem>))).toBe(false);
  });
});
