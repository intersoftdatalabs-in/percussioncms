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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as workflowsApi from "../../../main/ts/api/developer/workflowsApi";
import { WorkflowStepRoleAssignmentSection } from "../../../main/ts/developer/WorkflowStepRoleAssignmentSection";

vi.mock("../../../main/ts/api/developer/workflowsApi", () => ({
  listStepRoleAssignments: vi.fn(),
  setStepRoleAssignment: vi.fn(),
  addStepRole: vi.fn(),
  removeStepRole: vi.fn(),
}));

const listStepRoleAssignments = workflowsApi.listStepRoleAssignments as ReturnType<typeof vi.fn>;
const removeStepRole = workflowsApi.removeStepRole as ReturnType<typeof vi.fn>;

const stored = [
  { stepName: "Draft", roleName: "Author", assignmentType: "ASSIGNEE" },
  { stepName: "Draft", roleName: "Admin", assignmentType: "ADMIN" },
  { stepName: "Review", roleName: "Author", assignmentType: "ASSIGNEE" },
];

function rowMatching(step: string, role: string): HTMLElement[] {
  return screen.getAllByTestId(/^developer-wf-role-assign-row-/).filter((row) => {
    const cells = row.querySelectorAll("td");
    return cells[0]?.textContent?.trim() === step && cells[1]?.textContent?.trim() === role;
  });
}

describe("Workflow step remove role", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listStepRoleAssignments.mockReset();
    removeStepRole.mockReset();
    listStepRoleAssignments.mockResolvedValue(stored);
  });

  it("cancel does not call the server and the role stays listed", async () => {
    render(<WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-remove-request")).toBeTruthy();
    });
    expect(rowMatching("Draft", "Author")).toHaveLength(1);
    fireEvent.click(screen.getByTestId("developer-wf-role-remove-request"));
    expect(screen.getByTestId("developer-catalog-confirm-dialog")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-cancel"));
    expect(removeStepRole).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-catalog-confirm-dialog")).toBeNull();
    expect(rowMatching("Draft", "Author")).toHaveLength(1);
  });

  it("drops the role only after a successful reload and keeps it on the other step", async () => {
    const reloaded = stored.filter(
      (row) => !(row.stepName === "Draft" && row.roleName === "Author"),
    );
    removeStepRole.mockResolvedValue(reloaded);
    listStepRoleAssignments.mockResolvedValueOnce(stored).mockResolvedValueOnce(reloaded);
    render(<WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-remove-request")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-wf-role-remove-request"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-remove-notice")).toBeTruthy();
    });
    expect(rowMatching("Draft", "Author")).toEqual([]);
    expect(rowMatching("Review", "Author")).toHaveLength(1);
    expect(rowMatching("Draft", "Admin")).toHaveLength(1);
    expect(removeStepRole).toHaveBeenCalledWith("Nightly QA", "Draft", "Author");
    expect(removeStepRole).toHaveBeenCalledTimes(1);
  });

  it("HTTP 409 does not claim the role was removed", async () => {
    removeStepRole.mockRejectedValue({ status: 409, body: "conflict" });
    render(<WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-remove-request")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-wf-role-remove-request"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-remove-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-wf-role-remove-notice")).toBeNull();
    expect(rowMatching("Draft", "Author")).toHaveLength(1);
    expect(listStepRoleAssignments).toHaveBeenCalledTimes(1);
  });

  it("a reload that still lists the role does not drop the row", async () => {
    removeStepRole.mockResolvedValue(stored);
    listStepRoleAssignments.mockResolvedValue(stored);
    render(<WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-remove-request")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-wf-role-remove-request"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-remove-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-wf-role-remove-notice")).toBeNull();
    expect(rowMatching("Draft", "Author")).toHaveLength(1);
  });

  it("packaged workflows do not render remove", async () => {
    render(
      <WorkflowStepRoleAssignmentSection workflowName="Default Workflow" defaultWorkflow />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-assign")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-wf-role-remove")).toBeNull();
    expect(screen.queryByTestId("developer-wf-role-remove-request")).toBeNull();
    expect(removeStepRole).not.toHaveBeenCalled();
  });
});
