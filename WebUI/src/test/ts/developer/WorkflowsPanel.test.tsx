/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { WorkflowsPanel } from "../../../main/ts/developer/WorkflowsPanel";
import * as workflowsApi from "../../../main/ts/api/developer/workflowsApi";

vi.mock("../../../main/ts/api/developer/workflowsApi", () => ({
  listWorkflows: vi.fn(),
  getWorkflowDetail: vi.fn(),
  WORKFLOW_DESIGN_GAPS: [
    "Full workflow graph design is not exposed in the Developer catalog",
    "Workflow create / update / delete is not supported from this Developer surface",
    "Content type workflow association is edited on the content type detail panel",
  ],
}));

const listWorkflows = workflowsApi.listWorkflows as ReturnType<typeof vi.fn>;
const getWorkflowDetail = workflowsApi.getWorkflowDetail as ReturnType<typeof vi.fn>;

describe("WorkflowsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listWorkflows.mockReset();
    getWorkflowDetail.mockReset();
  });

  it("lists workflows and opens detail", async () => {
    listWorkflows.mockResolvedValue([
      {
        workflowName: "Simple Workflow",
        workflowDescription: "Default",
        defaultWorkflow: true,
        workflowSteps: [{ stepName: "Draft" }],
      },
    ]);
    getWorkflowDetail.mockResolvedValue({
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
      designGaps: ["gap-a"],
    });

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
    expect(screen.getByTestId("developer-wf-gaps").textContent).toContain("gap-a");
    fireEvent.click(screen.getByTestId("developer-wf-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-table")).toBeTruthy();
    });
  });

  it("shows loading state while list is pending", async () => {
    let resolveList!: (v: unknown) => void;
    listWorkflows.mockReturnValue(
      new Promise((resolve) => {
        resolveList = resolve;
      }),
    );
    render(<WorkflowsPanel />);
    expect(screen.getByTestId("developer-wf-loading")).toBeTruthy();
    resolveList([]);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-empty")).toBeTruthy();
    });
  });

  it("shows empty state when API returns no workflows", async () => {
    listWorkflows.mockResolvedValue([]);
    render(<WorkflowsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-empty")).toBeTruthy();
    });
  });

  it("shows error state when list fails (items stay unloaded)", async () => {
    listWorkflows.mockRejectedValue(new Error("network down"));
    render(<WorkflowsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-wf-empty")).toBeNull();
    expect(screen.queryByTestId("developer-wf-table")).toBeNull();
  });
});
