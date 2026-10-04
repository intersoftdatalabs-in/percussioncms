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
import {
  applyRemovedRoleAfterReload,
  isRemoveRoleReady,
  removableRolesOnStep,
  removableStepNames,
} from "../../../main/ts/developer/workflowStepRoleRemove";

const rows = [
  { stepName: "Draft", roleName: "Author", assignmentType: "ASSIGNEE" },
  { stepName: "Draft", roleName: "Admin", assignmentType: "ADMIN" },
  { stepName: "Review", roleName: "Author", assignmentType: "ASSIGNEE" },
  { stepName: "Review", roleName: "Editor", assignmentType: "READER" },
];

describe("workflow step role remove", () => {
  it("offers only Reader or Assignee roles on the chosen step", () => {
    expect(removableStepNames(rows)).toEqual(["Draft", "Review"]);
    expect(removableRolesOnStep(rows, "Draft")).toEqual(["Author"]);
    expect(removableRolesOnStep(rows, "Review")).toEqual(["Author", "Editor"]);
    expect(isRemoveRoleReady(rows, "Draft", "Author")).toBe(true);
    expect(isRemoveRoleReady(rows, "Draft", "Admin")).toBe(false);
    expect(isRemoveRoleReady(rows, "Draft", "Editor")).toBe(false);
    expect(isRemoveRoleReady(rows, "Draft", " ")).toBe(false);
  });

  it("keeps the previous rows until a reload drops the role", () => {
    const rejected = applyRemovedRoleAfterReload(rows, rows, "Draft", "Author");
    expect(rejected.accepted).toBe(false);
    expect(rejected.rows).toBe(rows);

    const reloaded = rows.filter(
      (row) => !(row.stepName === "Draft" && row.roleName === "Author"),
    );
    const accepted = applyRemovedRoleAfterReload(rows, reloaded, "draft", "author");
    expect(accepted.accepted).toBe(true);
    expect(accepted.rows).toBe(reloaded);
    expect(
      accepted.rows.some((row) => row.stepName === "Review" && row.roleName === "Author"),
    ).toBe(true);
    expect(applyRemovedRoleAfterReload(rows, null, "Draft", "Author").accepted).toBe(false);
  });
});
