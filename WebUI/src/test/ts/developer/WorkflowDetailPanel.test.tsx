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
  getWorkflowAllowedContentTypes: vi.fn(),
  setWorkflowAllowedContentTypes: vi.fn(),
  wrapWorkflowContentTypesForWire: vi.fn((body) => ({ WorkflowContentTypes: body })),
  WORKFLOW_CONTENT_TYPES_ROOT: "WorkflowContentTypes",
  WORKFLOW_DESIGN_GAPS: [
    "Full workflow graph design is not exposed in the Developer catalog",
    "Workflow create / update / delete is not supported from this Developer surface",
  ],
}));

const getWorkflowDetail = workflowsApi.getWorkflowDetail as ReturnType<typeof vi.fn>;
const getWorkflowAllowedContentTypes =
  workflowsApi.getWorkflowAllowedContentTypes as ReturnType<typeof vi.fn>;
const setWorkflowAllowedContentTypes =
  workflowsApi.setWorkflowAllowedContentTypes as ReturnType<typeof vi.fn>;

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
    getWorkflowAllowedContentTypes.mockReset();
    setWorkflowAllowedContentTypes.mockReset();
    getWorkflowAllowedContentTypes.mockResolvedValue([{ name: "percPage", label: "Page" }]);
    setWorkflowAllowedContentTypes.mockImplementation(async (_id, body) => body.allowedContentTypes);
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
    expect(back.getAttribute("aria-label")).toBe("Back to list");
    fireEvent.click(back);
    expect(onBack).toHaveBeenCalled();
  });

  it("loads and edits allowed content types then saves (SY-06)", async () => {
    getWorkflowDetail.mockResolvedValue(sampleDetail);
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-ct-row-0")).toBeTruthy();
    });
    expect(getWorkflowAllowedContentTypes).toHaveBeenCalledWith("Simple Workflow");
    expect(screen.getByTestId("developer-wf-ct-row-0").textContent).toContain("percPage");
    expect(screen.getByTestId("developer-wf-ct-save")).toBeDisabled();

    fireEvent.change(screen.getByTestId("developer-wf-ct-add-name"), {
      target: { value: "percImage" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-ct-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-ct-row-1")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-ct-save")).not.toBeDisabled();

    fireEvent.click(screen.getByTestId("developer-wf-ct-save"));
    await waitFor(() => {
      expect(setWorkflowAllowedContentTypes).toHaveBeenCalled();
    });
    expect(setWorkflowAllowedContentTypes).toHaveBeenCalledWith("Simple Workflow", {
      allowedContentTypes: [{ name: "percPage" }, { name: "percImage" }],
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-ct-notice").textContent).toBe(
        DEV_MSG.WF_CT_SAVE_SUCCESS,
      );
    });
    expect(screen.getByTestId("developer-wf-ct-save")).toBeDisabled();
  });

  it("removes a content type and can clear the set on save", async () => {
    getWorkflowDetail.mockResolvedValue(sampleDetail);
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-ct-row-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-wf-ct-remove-0"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-ct-empty")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-wf-ct-save"));
    await waitFor(() => {
      expect(setWorkflowAllowedContentTypes).toHaveBeenCalledWith("Simple Workflow", {
        allowedContentTypes: [],
      });
    });
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

  it("surfaces allowed-content-type load errors without blocking detail", async () => {
    getWorkflowDetail.mockResolvedValue(sampleDetail);
    getWorkflowAllowedContentTypes.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: null,
    });
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-detail-title")).toBeTruthy();
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-ct-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-ct-error").textContent).toContain(
      DEV_MSG.WF_CT_LOAD_ERROR,
    );
  });
});
