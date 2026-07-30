/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { WorkflowsPanel } from "../../../main/ts/developer/WorkflowsPanel";

vi.mock("../../../main/ts/api/developer/workflowsApi", () => ({
  listWorkflows: vi.fn().mockResolvedValue([
    {
      workflowName: "Simple Workflow",
      workflowDescription: "Default",
      defaultWorkflow: true,
      workflowSteps: [{ stepName: "Draft" }],
    },
  ]),
  getWorkflowDetail: vi.fn().mockResolvedValue({
    workflowName: "Simple Workflow",
    workflowDescription: "Default",
    defaultWorkflow: true,
    stagingRoleNames: "Admin;Editor",
    workflowSteps: [
      {
        stepName: "Draft",
        permissionNames: ["Read", "Write"],
        stepRoles: [{ roleName: "Author" }],
      },
      {
        stepName: "Approved",
        permissionNames: ["Read"],
        stepRoles: [{ roleName: "Admin" }],
      },
    ],
  }),
}));

describe("WorkflowsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  it("lists workflows and opens detail", async () => {
    render(<WorkflowsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-table").textContent).toContain("Simple Workflow");
    fireEvent.click(screen.getByTestId("developer-wf-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-steps-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-wf-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-table")).toBeTruthy();
    });
  });
});
