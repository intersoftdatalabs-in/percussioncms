/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as workflowsApi from "../../../main/ts/api/developer/workflowsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { WorkflowDetailPanel } from "../../../main/ts/developer/WorkflowDetailPanel";

vi.mock("../../../main/ts/api/developer/workflowsApi", () => ({
  getWorkflowDetail: vi.fn(),
  listWorkflows: vi.fn(),
  WORKFLOW_DESIGN_GAPS: [
    "Full workflow graph design is not exposed in the Developer catalog",
    "Workflow create / update / delete is not supported from this Developer surface",
    "Content type workflow association is edited on the content type detail panel",
  ],
}));

const getWorkflowDetail = workflowsApi.getWorkflowDetail as ReturnType<typeof vi.fn>;

const sampleDetail = {
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
};

describe("WorkflowDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getWorkflowDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getWorkflowDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-detail-title").textContent).toContain(
      "Simple Workflow",
    );
    expect(screen.getByTestId("developer-wf-steps-table")).toBeTruthy();
    expect(screen.getByTestId("developer-wf-gaps").textContent).toContain("gap-a");
    expect(getWorkflowDetail).toHaveBeenCalledWith("Simple Workflow");
    const back = screen.getByTestId("developer-wf-back");
    expect(back.getAttribute("aria-label")).toBe("Back to workflows list");
    fireEvent.click(back);
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty steps section when detail has none", async () => {
    getWorkflowDetail.mockResolvedValue({
      ...sampleDetail,
      workflowSteps: [],
    });
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-steps")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-steps").textContent).toContain(DEV_MSG.WF_NONE);
    expect(screen.queryByTestId("developer-wf-steps-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getWorkflowDetail.mockRejectedValue(new SessionRedirectError());
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-wf-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-wf-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getWorkflowDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-detail-error").textContent).toBe(
      `${DEV_MSG.WF_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getWorkflowDetail.mockRejectedValue(new Error("network down"));
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-detail-error").textContent).toBe(
      `${DEV_MSG.WF_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-wf-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getWorkflowDetail.mockRejectedValue("boom");
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-detail-error").textContent).toBe(
      DEV_MSG.WF_DETAIL_ERROR,
    );
  });
});
