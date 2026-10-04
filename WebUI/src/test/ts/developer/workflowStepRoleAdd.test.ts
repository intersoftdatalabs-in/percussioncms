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
  applyAddedRoleAfterReload,
  firstStepWithRoleToAdd,
  isAddRoleReady,
  rolesNotOnStep,
} from "../../../main/ts/developer/workflowStepRoleAdd";

const rows = [
  { stepName: "Draft", roleName: "Author", assignmentType: "ASSIGNEE" },
  { stepName: "Review", roleName: "Author", assignmentType: "ASSIGNEE" },
  { stepName: "Review", roleName: "System", assignmentType: "READER" },
];

describe("workflow step role add", () => {
  it("offers a role that is on another step but not this one", () => {
    expect(rolesNotOnStep(rows, "Draft")).toEqual(["System"]);
    expect(rolesNotOnStep(rows, "Review")).toEqual([]);
    expect(firstStepWithRoleToAdd(rows)).toBe("Draft");
    expect(isAddRoleReady(rows, "Draft", "System", "reader")).toBe(true);
    expect(isAddRoleReady(rows, "Draft", "Author", "ASSIGNEE")).toBe(false);
    expect(isAddRoleReady(rows, "Draft", "System", "ADMIN")).toBe(false);
    expect(isAddRoleReady(rows, "Draft", " ", "READER")).toBe(false);
  });

  it("keeps the previous rows until a reload contains the new role", () => {
    const rejected = applyAddedRoleAfterReload(rows, rows, "Draft", "System", "READER");
    expect(rejected.accepted).toBe(false);
    expect(rejected.rows).toBe(rows);
    expect(rejected.rows.some((row) => row.roleName === "System" && row.stepName === "Draft")).toBe(
      false,
    );

    const reloaded = [
      ...rows,
      { stepName: "Draft", roleName: "System", assignmentType: "ASSIGNEE" },
    ];
    const accepted = applyAddedRoleAfterReload(rows, reloaded, "Draft", "System", "assignee");
    expect(accepted.accepted).toBe(true);
    expect(accepted.rows).toBe(reloaded);
    const wrongType = applyAddedRoleAfterReload(rows, reloaded, "Draft", "System", "READER");
    expect(wrongType.accepted).toBe(false);
    expect(wrongType.rows).toBe(rows);
  });
});
