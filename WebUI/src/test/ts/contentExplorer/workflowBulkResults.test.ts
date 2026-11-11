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
 * 008 US4 bulk handling (T028/T029, FR-011/FR-009): permissions evaluate per
 * item (denied items never reach apply), every invoked item gets exactly one
 * reported outcome, successes stand when siblings fail.
 */
import { describe, expect, it, vi } from "vitest";
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  dispatchAction,
  partitionWorkflowSelection,
  type ActionDispatchContext,
} from "../../../main/ts/contentExplorer/actionDispatch";

function item(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    id: "42",
    name: "page1",
    type: "percPage",
    path: "/Sites/site1/page1",
    ...overrides,
  } as PSPathItem;
}

function transitionAction(): MenuAction {
  return {
    name: "workflow-transition:Approve",
    label: "Approve",
    sortRank: 9001,
    menuType: "MENUITEM",
  } as MenuAction;
}

describe("per-item permission evaluation (T028)", () => {
  it("skips folders and ineligible items; only eligible items reach apply", () => {
    const plan = partitionWorkflowSelection([
      item({ id: "1", name: "ok1", workflowId: 5 } as Partial<PSPathItem>),
      item({ id: "2", name: "folder1", type: "folder", path: "/Sites/f1/" }),
      item({ id: "3", name: "nowf", workflowId: -1 } as Partial<PSPathItem>),
      item({ id: "1", name: "ok1-dup", workflowId: 5 } as Partial<PSPathItem>),
    ]);
    expect(plan.eligible.map((i) => i.id)).toEqual(["1"]);
    expect(plan.skippedFolders).toEqual(["folder1"]);
    expect(plan.skippedOther).toEqual(["nowf"]);
  });
});

describe("per-item result reporting (T029)", () => {
  function ctx(behavior: (id: string) => void): ActionDispatchContext {
    return {
      item: item({ id: "1", workflowId: 5 } as Partial<PSPathItem>),
      selectedItems: [
        item({ id: "1", name: "good", workflowId: 5 } as Partial<PSPathItem>),
        item({ id: "2", name: "bad", workflowId: 5 } as Partial<PSPathItem>),
      ],
      confirm: () => true,
      runWorkflow: async (id: string) => {
        behavior(id);
      },
    } as ActionDispatchContext;
  }

  it("successes stand while a sibling failure names the item and reason", async () => {
    const run = vi.fn(async (id: string) => {
      if (id === "2") {
        throw { status: 403, statusText: "Forbidden", body: "denied" };
      }
    });
    const result = await dispatchAction(transitionAction(), {
      ...ctx(run),
      runWorkflow: run,
    });
    expect(result.kind).toBe("workflow");
    expect(result.refresh).toBe(true);
    expect(run).toHaveBeenCalledTimes(2);
    // Failure names the item and the reason; successes are not rolled back.
    expect(result.messageText ?? "").toMatch(/bad/);
    expect(result.messageText ?? "").toMatch(/403/);
    expect(result.messageText ?? "").not.toMatch(/good.*403|403.*good/);
  });

  it("reports nothing-eligible without invoking anyone", async () => {
    const run = vi.fn();
    const folders = [
      item({ id: "f1", name: "folder1", type: "folder", path: "/Sites/f1/" }),
      item({ id: "f2", name: "folder2", type: "folder", path: "/Sites/f2/" }),
    ];
    const result = await dispatchAction(transitionAction(), {
      ...ctx(run),
      item: folders[0],
      selectedItems: folders,
      runWorkflow: run,
    } as ActionDispatchContext);
    expect(run).not.toHaveBeenCalled();
    expect(result.messageText ?? "").toBeTruthy();
  });
});
