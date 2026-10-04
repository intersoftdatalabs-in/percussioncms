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
  applyAssignmentAfterReload,
  canOfferStepRoleAssignment,
  isAssignmentChangeReady,
} from "../../../main/ts/developer/workflowStepRoleAssignment";

const stored = [
  { stepName: "Draft", roleName: "Author", assignmentType: "ASSIGNEE" },
  { stepName: "Draft", roleName: "Editor", assignmentType: "READER" },
];

describe("workflow step role assignment", () => {
  it("does not offer packaged or default workflows", () => {
    expect(canOfferStepRoleAssignment({ name: "Nightly QA", defaultWorkflow: false })).toBe(true);
    expect(canOfferStepRoleAssignment({ name: "Default Workflow" })).toBe(false);
    expect(canOfferStepRoleAssignment({ name: "Simple Workflow" })).toBe(false);
    expect(canOfferStepRoleAssignment({ name: "Local Content" })).toBe(false);
    expect(canOfferStepRoleAssignment({ name: "Nightly QA", defaultWorkflow: true })).toBe(false);
  });

  it("confirm is ready only when Reader or Assignee actually changes", () => {
    expect(isAssignmentChangeReady("ASSIGNEE", "READER")).toBe(true);
    expect(isAssignmentChangeReady("Assignee", "reader")).toBe(true);
    expect(isAssignmentChangeReady("ASSIGNEE", "ASSIGNEE")).toBe(false);
    expect(isAssignmentChangeReady("ADMIN", "READER")).toBe(false);
    expect(isAssignmentChangeReady("READER", "ADMIN")).toBe(false);
  });

  it("shows the new type only when the reload contains it", () => {
    const rejected = applyAssignmentAfterReload(stored, null, "Draft", "Author", "READER");
    expect(rejected.accepted).toBe(false);
    expect(rejected.rows[0].assignmentType).toBe("ASSIGNEE");

    const stale = applyAssignmentAfterReload(stored, stored, "Draft", "Author", "READER");
    expect(stale.accepted).toBe(false);
    expect(stale.rows).toBe(stored);

    const reloaded = [
      { stepName: "Draft", roleName: "Author", assignmentType: "READER" },
      { stepName: "Draft", roleName: "Editor", assignmentType: "READER" },
    ];
    const accepted = applyAssignmentAfterReload(stored, reloaded, "Draft", "Author", "READER");
    expect(accepted.accepted).toBe(true);
    expect(accepted.rows).toBe(reloaded);
    expect(accepted.rows[1].assignmentType).toBe("READER");
  });
});
