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
  setStepRoleInbox: vi.fn(),
  addStepRole: vi.fn(),
  removeStepRole: vi.fn(),
}));

const listStepRoleAssignments = workflowsApi.listStepRoleAssignments as ReturnType<typeof vi.fn>;
const setStepRoleInbox = workflowsApi.setStepRoleInbox as ReturnType<typeof vi.fn>;
const setStepRoleNotify = workflowsApi.setStepRoleNotify as ReturnType<typeof vi.fn>;
const setStepRoleAssignment = workflowsApi.setStepRoleAssignment as ReturnType<typeof vi.fn>;

const stored = [
  { stepName: "Draft", roleName: "Author", assignmentType: "ASSIGNEE", notify: true, inbox: true },
  { stepName: "Draft", roleName: "Editor", assignmentType: "READER", notify: false, inbox: false },
  { stepName: "Draft", roleName: "Admin", assignmentType: "ADMIN", notify: true, inbox: true },
];

describe("Workflow step role inbox", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listStepRoleAssignments.mockReset();
    setStepRoleInbox.mockReset();
    setStepRoleNotify.mockReset();
    setStepRoleAssignment.mockReset();
    listStepRoleAssignments.mockResolvedValue(stored);
  });

  it("cancel does not call the server and keeps the stored flag", async () => {
    render(<WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-inbox-0").getAttribute("data-inbox")).toBe(
        "true",
      );
    });
    fireEvent.change(screen.getByTestId("developer-wf-role-inbox-value"), {
      target: { value: "off" },
    });
    expect(screen.getByTestId("developer-wf-role-inbox-0").getAttribute("data-inbox")).toBe(
      "true",
    );
    fireEvent.click(screen.getByTestId("developer-wf-role-inbox-cancel"));
    expect(setStepRoleInbox).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-wf-role-inbox-0").getAttribute("data-inbox")).toBe(
      "true",
    );
    expect(screen.getByTestId("developer-wf-role-inbox-value")).toHaveProperty("value", "on");
  });

  it("shows the new flag only after a successful reload", async () => {
    const reloaded = [
      { stepName: "Draft", roleName: "Author", assignmentType: "ASSIGNEE", notify: true, inbox: false },
      { stepName: "Draft", roleName: "Editor", assignmentType: "READER", notify: false, inbox: false },
      { stepName: "Draft", roleName: "Admin", assignmentType: "ADMIN", notify: true, inbox: true },
    ];
    let resolvePut: (value: unknown) => void = () => {};
    let resolveList: (value: unknown) => void = () => {};
    setStepRoleInbox.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolvePut = resolve;
        }),
    );
    listStepRoleAssignments.mockResolvedValueOnce(stored).mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveList = resolve;
        }),
    );
    render(<WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-inbox-confirm")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-wf-role-inbox-value"), {
      target: { value: "off" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-role-inbox-confirm"));
    await waitFor(() => {
      expect(setStepRoleInbox).toHaveBeenCalledWith("Nightly QA", "Draft", {
        roleName: "Author",
        inbox: false,
      });
    });
    expect(screen.getByTestId("developer-wf-role-inbox-0").getAttribute("data-inbox")).toBe(
      "true",
    );
    expect(screen.getByTestId("developer-wf-role-notify-0").getAttribute("data-notify")).toBe(
      "true",
    );
    expect(setStepRoleNotify).not.toHaveBeenCalled();
    expect(setStepRoleAssignment).not.toHaveBeenCalled();
    resolvePut(reloaded);
    await waitFor(() => {
      expect(listStepRoleAssignments).toHaveBeenCalledTimes(2);
    });
    expect(screen.getByTestId("developer-wf-role-inbox-0").getAttribute("data-inbox")).toBe(
      "true",
    );
    resolveList(reloaded);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-inbox-0").getAttribute("data-inbox")).toBe(
        "false",
      );
    });
    expect(screen.getByTestId("developer-wf-role-inbox-2").getAttribute("data-inbox")).toBe(
      "true",
    );
    expect(screen.getByTestId("developer-wf-role-assign-type-0").getAttribute(
      "data-assignment-type",
    )).toBe("ASSIGNEE");
    expect(screen.getByTestId("developer-wf-role-notify-0").getAttribute("data-notify")).toBe(
      "true",
    );
  });

  it("HTTP 400, 403, and 409 leave the table unchanged", async () => {
    for (const status of [400, 403, 409]) {
      setStepRoleInbox.mockRejectedValue({ status, body: "rejected" });
      const { unmount } = render(
        <WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("developer-wf-role-inbox-0")).toBeTruthy();
      });
      fireEvent.change(screen.getByTestId("developer-wf-role-inbox-value"), {
        target: { value: "off" },
      });
      fireEvent.click(screen.getByTestId("developer-wf-role-inbox-confirm"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-wf-role-inbox-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-wf-role-inbox-0").getAttribute("data-inbox")).toBe(
        "true",
      );
      unmount();
    }
    expect(listStepRoleAssignments).toHaveBeenCalledTimes(3);
  });

  it("does not offer Admin in the inbox role list", async () => {
    render(<WorkflowStepRoleAssignmentSection workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-inbox-role")).toBeTruthy();
    });
    const select = screen.getByTestId("developer-wf-role-inbox-role") as HTMLSelectElement;
    const names = Array.from(select.options).map((option) => option.value);
    expect(names).toEqual(["Author", "Editor"]);
    expect(names).not.toContain("Admin");
  });

  it("packaged workflows do not render set inbox", async () => {
    render(
      <WorkflowStepRoleAssignmentSection workflowName="Default Workflow" defaultWorkflow />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-role-assign")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-wf-role-inbox-confirm")).toBeNull();
    expect(setStepRoleInbox).not.toHaveBeenCalled();
  });
});
