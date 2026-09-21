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
  updateWorkflow: vi.fn(),
  deleteWorkflow: vi.fn(),
  wrapWorkflowContentTypesForWire: vi.fn((body) => ({ WorkflowContentTypes: body })),
  WORKFLOW_CONTENT_TYPES_ROOT: "WorkflowContentTypes",
  WORKFLOW_DESIGN_GAPS: [
    "Full workflow graph design is not exposed in the Developer catalog",
  ],
}));

const getWorkflowDetail = workflowsApi.getWorkflowDetail as ReturnType<typeof vi.fn>;
const getWorkflowAllowedContentTypes =
  workflowsApi.getWorkflowAllowedContentTypes as ReturnType<typeof vi.fn>;
const setWorkflowAllowedContentTypes =
  workflowsApi.setWorkflowAllowedContentTypes as ReturnType<typeof vi.fn>;
const updateWorkflowMock = workflowsApi.updateWorkflow as ReturnType<typeof vi.fn>;
const deleteWorkflowMock = workflowsApi.deleteWorkflow as ReturnType<typeof vi.fn>;

const sampleDetail = {
  workflowName: "Simple Workflow",
  workflowDescription: "Default",
  defaultWorkflow: true,
  stagingRoleNames: "Admin;Editor",
  workflowSteps: [
    {
      stepName: "Draft",
      permissionNames: ["Read", "Write"],
      stepRoles: [
        {
          roleName: "Author",
          roleTransitions: [{ transitionPermission: "Submit" }],
        },
      ],
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
    updateWorkflowMock.mockReset();
    deleteWorkflowMock.mockReset();
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
    expect(screen.getByTestId("developer-wf-step-name-0").textContent).toContain("Draft");
    expect(screen.getByTestId("developer-wf-step-transitions-0").textContent).toContain(
      "Submit",
    );
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

  it("maps GET 404 to WF_NOT_FOUND without a blank success body", async () => {
    getWorkflowDetail.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    render(<WorkflowDetailPanel name="Missing" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-detail-error").textContent).toContain(
      DEV_MSG.WF_NOT_FOUND,
    );
    expect(screen.queryByTestId("developer-wf-detail-title")).toBeNull();
    expect(screen.queryByTestId("developer-wf-steps-table")).toBeNull();
  });

  it("maps GET 403 to WF_FORBIDDEN without a blank success body", async () => {
    getWorkflowDetail.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: null,
    });
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-detail-error").textContent).toContain(
      DEV_MSG.WF_FORBIDDEN,
    );
    expect(screen.queryByTestId("developer-wf-detail-title")).toBeNull();
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

  it("rejects invalid content-type names client-side before add", async () => {
    getWorkflowDetail.mockResolvedValue(sampleDetail);
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-ct-row-0")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-wf-ct-add-name"), {
      target: { value: "bad name" },
    });
    expect(screen.getByTestId("developer-wf-ct-add")).toBeDisabled();
    expect(screen.getByTestId("developer-wf-ct-name-invalid").textContent).toBe(
      DEV_MSG.WF_CT_NAME_INVALID,
    );
  });

  it("dedupes by guid stringValue when adding a Percussion GUID", async () => {
    getWorkflowDetail.mockResolvedValue(sampleDetail);
    getWorkflowAllowedContentTypes.mockResolvedValue([
      { name: "percPage", guid: { stringValue: "2-1-100" } },
    ]);
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-ct-row-0")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-wf-ct-add-name"), {
      target: { value: "2-1-100" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-ct-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-ct-error").textContent).toBe(DEV_MSG.WF_CT_DUP);
    });
    expect(screen.queryByTestId("developer-wf-ct-row-1")).toBeNull();
  });

  it("exposes status live region and stable remove aria-label", async () => {
    getWorkflowDetail.mockResolvedValue(sampleDetail);
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-ct-row-0")).toBeTruthy();
    });
    expect(screen.getByLabelText(`${DEV_MSG.CT_ASSOC_REMOVE} percPage`)).toBeTruthy();
    expect(screen.getByLabelText(DEV_MSG.WF_CT_ADD_LABEL)).toBeTruthy();

    fireEvent.change(screen.getByTestId("developer-wf-ct-add-name"), {
      target: { value: "percImage" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-ct-add"));
    fireEvent.click(screen.getByTestId("developer-wf-ct-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-ct-notice").getAttribute("role")).toBe("status");
    });
    expect(screen.getByTestId("developer-wf-ct-notice").getAttribute("aria-live")).toBe(
      "polite",
    );
  });

  it("keeps the description save disabled until the draft differs", async () => {
    getWorkflowDetail.mockResolvedValue(sampleDetail);
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-description-save")).toBeTruthy();
    });
    expect(
      screen.getByTestId("developer-wf-description-input").getAttribute("value") ??
        (screen.getByTestId("developer-wf-description-input") as HTMLInputElement).value,
    ).toBe("Default");
    expect(screen.getByTestId("developer-wf-description-save")).toBeDisabled();

    fireEvent.change(screen.getByTestId("developer-wf-description-input"), {
      target: { value: "Edited" },
    });
    expect(screen.getByTestId("developer-wf-description-save")).not.toBeDisabled();
  });

  it("saves description via PUT and resets the dirty flag on success", async () => {
    getWorkflowDetail.mockResolvedValue(sampleDetail);
    updateWorkflowMock.mockResolvedValue({
      workflowName: "Simple Workflow",
      workflowDescription: "Edited",
      defaultWorkflow: true,
    });
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-description-save")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("developer-wf-description-input"), {
      target: { value: "Edited" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-description-save"));

    await waitFor(() => {
      expect(updateWorkflowMock).toHaveBeenCalledWith("Simple Workflow", {
        name: "Simple Workflow",
        description: "Edited",
      });
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-description-notice")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-description-notice").textContent).toBe(
      DEV_MSG.WF_DETAIL_SAVED,
    );
    expect(screen.getByTestId("developer-wf-description-save")).toBeDisabled();
  });

  it("surfaces update 409 errors as name mismatch", async () => {
    getWorkflowDetail.mockResolvedValue(sampleDetail);
    updateWorkflowMock.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: null,
    });
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-description-input")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-wf-description-input"), {
      target: { value: "Edited" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-description-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-description-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-description-error").textContent).toContain(
      DEV_MSG.WF_DETAIL_NAME_MISMATCH,
    );
  });

  it("opens the delete confirm dialog, cancels, and does not delete", async () => {
    getWorkflowDetail.mockResolvedValue(sampleDetail);
    const onDeleted = vi.fn();
    render(
      <WorkflowDetailPanel
        name="Simple Workflow"
        onBack={() => undefined}
        onDeleted={onDeleted}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-delete")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("developer-wf-delete"));
    expect(screen.getByTestId("developer-catalog-confirm-dialog")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-cancel"));
    expect(screen.queryByTestId("developer-catalog-confirm-dialog")).toBeNull();
    expect(deleteWorkflowMock).not.toHaveBeenCalled();
    expect(onDeleted).not.toHaveBeenCalled();
  });

  it("confirms the delete dialog and calls onDeleted on success", async () => {
    getWorkflowDetail.mockResolvedValue(sampleDetail);
    deleteWorkflowMock.mockResolvedValue(undefined);
    const onDeleted = vi.fn();
    render(
      <WorkflowDetailPanel
        name="Simple Workflow"
        onBack={() => undefined}
        onDeleted={onDeleted}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-delete")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("developer-wf-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));

    await waitFor(() => {
      expect(deleteWorkflowMock).toHaveBeenCalledWith("Simple Workflow");
    });
    await waitFor(() => {
      expect(onDeleted).toHaveBeenCalledTimes(1);
    });
  });

  it("surfaces a 409 delete error message about items-in-use", async () => {
    getWorkflowDetail.mockResolvedValue(sampleDetail);
    deleteWorkflowMock.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      message: "Workflow still has associated items",
      body: null,
    });
    render(<WorkflowDetailPanel name="Simple Workflow" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-delete")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("developer-wf-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));

    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-delete-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-wf-delete-error").textContent).toContain(
      DEV_MSG.WF_DETAIL_DELETE_HAS_ITEMS,
    );
  });
});
