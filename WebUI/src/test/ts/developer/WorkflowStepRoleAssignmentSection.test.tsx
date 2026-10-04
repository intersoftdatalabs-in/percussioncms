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
  setStepRoleNotify: vi.fn(),
  addStepRole: vi.fn(),
  removeStepRole: vi.fn(),
}));

const listStepRoleAssignments = workflowsApi.listStepRoleAssignments as ReturnType<typeof vi.fn>;
const setStepRoleAssignment = workflowsApi.setStepRoleAssignment as ReturnType<typeof vi.fn>;

const stored = [
  { stepName: "Draft", roleName: "Author", assignmentType: "ASSIGNEE" },
  { stepName: "Draft", roleName: "Editor", assignmentType: "READER" },
];

describe("WorkflowStepRoleAssignmentSection", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listStepRoleAssignments.mockReset();
    setStepRoleAssignment.mockReset();
    listStepRoleAssignments.mockResolvedValue(stored);
  });

  it("cancel does not call the server and keeps the stored type", async () => {
    render(<WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-assign-type-0").textContent).toContain(
        "Assignee",
      );
    });
    fireEvent.change(screen.getByTestId("developer-wf-role-assign-type"), {
      target: { value: "READER" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-role-assign-cancel"));
    expect(setStepRoleAssignment).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-wf-role-assign-type-0").getAttribute(
      "data-assignment-type",
    )).toBe("ASSIGNEE");
    expect(screen.getByTestId("developer-wf-role-assign-type")).toHaveProperty("value", "ASSIGNEE");
  });

  it("shows the new type only after a successful reload", async () => {
    const reloaded = [
      { stepName: "Draft", roleName: "Author", assignmentType: "READER" },
      { stepName: "Draft", roleName: "Editor", assignmentType: "READER" },
    ];
    setStepRoleAssignment.mockResolvedValue(reloaded);
    listStepRoleAssignments.mockResolvedValueOnce(stored).mockResolvedValueOnce(reloaded);
    render(<WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-assign-confirm")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-wf-role-assign-type"), {
      target: { value: "READER" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-role-assign-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-assign-type-0").getAttribute(
        "data-assignment-type",
      )).toBe("READER");
    });
    expect(screen.getByTestId("developer-wf-role-assign-type-1").getAttribute(
      "data-assignment-type",
    )).toBe("READER");
    expect(setStepRoleAssignment).toHaveBeenCalledWith("Nightly QA", "Draft", {
      roleName: "Author",
      assignmentType: "READER",
    });
  });

  it("HTTP 409 does not claim the new type", async () => {
    setStepRoleAssignment.mockRejectedValue({ status: 409, body: "conflict" });
    render(<WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-assign-type-0")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-wf-role-assign-type"), {
      target: { value: "READER" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-role-assign-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-assign-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-role-assign-type-0").getAttribute(
      "data-assignment-type",
    )).toBe("ASSIGNEE");
    expect(listStepRoleAssignments).toHaveBeenCalledTimes(1);
  });

  it("packaged workflows do not render confirm", async () => {
    render(
      <WorkflowStepRoleAssignmentSection workflowName="Default Workflow" defaultWorkflow />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-assign")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-wf-role-assign-confirm")).toBeNull();
    expect(setStepRoleAssignment).not.toHaveBeenCalled();
  });
});
