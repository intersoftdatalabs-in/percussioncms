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
}));

const listStepRoleAssignments = workflowsApi.listStepRoleAssignments as ReturnType<typeof vi.fn>;
const addStepRole = workflowsApi.addStepRole as ReturnType<typeof vi.fn>;

const stored = [
  { stepName: "Draft", roleName: "Author", assignmentType: "ASSIGNEE" },
  { stepName: "Review", roleName: "Author", assignmentType: "ASSIGNEE" },
  { stepName: "Review", roleName: "System", assignmentType: "READER" },
];

function draftHasSystem(): boolean {
  return screen.getAllByTestId(/^developer-wf-role-assign-row-/).some((row) => {
    const cells = row.querySelectorAll("td");
    return cells[0]?.textContent?.trim() === "Draft" && cells[1]?.textContent?.trim() === "System";
  });
}

describe("Workflow step add role", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listStepRoleAssignments.mockReset();
    addStepRole.mockReset();
    listStepRoleAssignments.mockResolvedValue(stored);
  });

  it("cancel does not call the server and the table does not show the role", async () => {
    render(<WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-add-role")).toBeTruthy();
    });
    expect(draftHasSystem()).toBe(false);
    fireEvent.change(screen.getByTestId("developer-wf-role-add-type"), {
      target: { value: "ASSIGNEE" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-role-add-cancel"));
    expect(addStepRole).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-wf-role-add-type")).toHaveProperty("value", "READER");
    expect(draftHasSystem()).toBe(false);
  });

  it("shows the role only after a successful reload", async () => {
    const reloaded = [
      ...stored,
      { stepName: "Draft", roleName: "System", assignmentType: "ASSIGNEE" },
    ];
    addStepRole.mockResolvedValue(reloaded);
    listStepRoleAssignments.mockResolvedValueOnce(stored).mockResolvedValueOnce(reloaded);
    render(<WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-add-confirm")).toBeTruthy();
    });
    expect(draftHasSystem()).toBe(false);
    fireEvent.change(screen.getByTestId("developer-wf-role-add-type"), {
      target: { value: "ASSIGNEE" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-role-add-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-add-notice")).toBeTruthy();
    });
    const draftSystem = screen.getAllByTestId(/^developer-wf-role-assign-row-/).filter((row) => {
      const cells = row.querySelectorAll("td");
      return cells[0]?.textContent?.trim() === "Draft" && cells[1]?.textContent?.trim() === "System";
    });
    expect(draftSystem).toHaveLength(1);
    expect(draftSystem[0].querySelector("[data-assignment-type]")?.getAttribute("data-assignment-type")).toBe(
      "ASSIGNEE",
    );
    expect(addStepRole).toHaveBeenCalledWith("Nightly QA", "Draft", {
      roleName: "System",
      assignmentType: "ASSIGNEE",
    });
  });

  it("HTTP 409 does not claim the role was added", async () => {
    addStepRole.mockRejectedValue({ status: 409, body: "conflict" });
    render(<WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-add-confirm")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-wf-role-add-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-add-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-wf-role-add-notice")).toBeNull();
    const draftSystem = screen.getAllByTestId(/^developer-wf-role-assign-row-/).filter((row) => {
      const cells = row.querySelectorAll("td");
      return cells[0]?.textContent?.trim() === "Draft" && cells[1]?.textContent?.trim() === "System";
    });
    expect(draftSystem).toEqual([]);
    expect(listStepRoleAssignments).toHaveBeenCalledTimes(1);
  });

  it("packaged workflows do not render add confirm", async () => {
    render(
      <WorkflowStepRoleAssignmentSection workflowName="Default Workflow" defaultWorkflow />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-assign")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-wf-role-add")).toBeNull();
    expect(screen.queryByTestId("developer-wf-role-add-confirm")).toBeNull();
    expect(addStepRole).not.toHaveBeenCalled();
  });
});
