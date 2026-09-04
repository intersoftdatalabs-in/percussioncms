/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as workflowsApi from "../../../main/ts/api/developer/workflowsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { WorkflowsPanel } from "../../../main/ts/developer/WorkflowsPanel";

vi.mock("../../../main/ts/api/developer/workflowsApi", () => ({
  listWorkflows: vi.fn(),
  getWorkflowDetail: vi.fn(),
  getWorkflowAllowedContentTypes: vi.fn().mockResolvedValue([]),
  setWorkflowAllowedContentTypes: vi.fn(),
  WORKFLOW_DESIGN_GAPS: [
    "Full workflow graph design is not exposed in the Developer catalog",
    "Workflow create / update / delete is not supported from this Developer surface",
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

  it("shows session-redirect message via panelErrMsg", async () => {
    listWorkflows.mockRejectedValue(new SessionRedirectError());
    render(<WorkflowsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-wf-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listWorkflows.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<WorkflowsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-error").textContent).toBe(
      `${DEV_MSG.WF_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    listWorkflows.mockRejectedValue(new Error("network down"));
    render(<WorkflowsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-error").textContent).toBe(
      `${DEV_MSG.WF_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-wf-empty")).toBeNull();
    expect(screen.queryByTestId("developer-wf-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listWorkflows.mockRejectedValue("boom");
    render(<WorkflowsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-error").textContent).toBe(DEV_MSG.WF_ERROR);
  });
});
