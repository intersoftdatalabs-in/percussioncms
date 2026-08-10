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
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";
import {
  WORKFLOW_MENU_NAME,
  WORKFLOW_TRANSITION_PREFIX,
  buildWorkflowTransitionMenu,
  isWorkflowTransitionActionName,
  mergeWorkflowMenuActions,
  parseWorkflowTransitionTrigger,
} from "../../../main/ts/contentExplorer/workflowMenuActions";

describe("workflowMenuActions mapping (#2732)", () => {
  it("returns null when triggers are empty or blank", () => {
    expect(buildWorkflowTransitionMenu([])).toBeNull();
    expect(buildWorkflowTransitionMenu(null)).toBeNull();
    expect(buildWorkflowTransitionMenu(["", "  "])).toBeNull();
  });

  it("builds a Workflow MENU group with tagged children", () => {
    const menu = buildWorkflowTransitionMenu(["Submit", "Approve"], {
      groupLabel: "Workflow",
      stateName: "Draft",
    });
    expect(menu).not.toBeNull();
    expect(menu!.name).toBe(WORKFLOW_MENU_NAME);
    expect(menu!.menuType).toBe("MENU");
    expect(menu!.label).toBe("Workflow");
    expect(menu!.description).toContain("Draft");
    expect(menu!.children?.map((c) => c.name)).toEqual([
      `${WORKFLOW_TRANSITION_PREFIX}Submit`,
      `${WORKFLOW_TRANSITION_PREFIX}Approve`,
    ]);
    expect(menu!.children?.map((c) => c.label)).toEqual([
      "Submit",
      "Approve",
    ]);
  });

  it("de-duplicates triggers preserving first-seen order", () => {
    const menu = buildWorkflowTransitionMenu([
      "Submit",
      "Approve",
      "Submit",
      "  Approve  ",
    ]);
    expect(menu!.children?.map((c) => c.label)).toEqual(["Submit", "Approve"]);
  });

  it("merges workflow group after base actions and replaces prior workflow group", () => {
    const base: MenuAction[] = [
      { name: "open", label: "Open", sortRank: 10, menuType: "MENUITEM" },
      {
        name: WORKFLOW_MENU_NAME,
        label: "Stale",
        sortRank: 1,
        menuType: "MENU",
        children: [
          {
            name: `${WORKFLOW_TRANSITION_PREFIX}Old`,
            label: "Old",
            sortRank: 1,
            menuType: "MENUITEM",
          },
        ],
      },
    ];
    const wf = buildWorkflowTransitionMenu(["Submit"], {
      groupLabel: "Workflow",
      sortRank: 9000,
    });
    const merged = mergeWorkflowMenuActions(base, wf);
    expect(merged.map((a) => a.name)).toEqual(["open", WORKFLOW_MENU_NAME]);
    expect(merged[1]?.children?.[0]?.label).toBe("Submit");
  });

  it("omits workflow group when null", () => {
    const base: MenuAction[] = [
      { name: "open", label: "Open", sortRank: 1, menuType: "MENUITEM" },
    ];
    expect(mergeWorkflowMenuActions(base, null).map((a) => a.name)).toEqual([
      "open",
    ]);
  });

  it("parses and detects workflow transition action names", () => {
    expect(isWorkflowTransitionActionName(`${WORKFLOW_TRANSITION_PREFIX}Submit`)).toBe(
      true,
    );
    expect(isWorkflowTransitionActionName("open")).toBe(false);
    expect(parseWorkflowTransitionTrigger(`${WORKFLOW_TRANSITION_PREFIX}Approve`)).toBe(
      "Approve",
    );
    expect(parseWorkflowTransitionTrigger("open")).toBeNull();
    expect(parseWorkflowTransitionTrigger(WORKFLOW_TRANSITION_PREFIX)).toBeNull();
  });
});
