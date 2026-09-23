/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as workflowsApi from "../../../main/ts/api/developer/workflowsApi";
import { DEFAULT_WORKFLOW_TEMPLATE_STEPS } from "../../../main/ts/api/developer/workflowsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { WorkflowCreatePanel } from "../../../main/ts/developer/WorkflowCreatePanel";

vi.mock("../../../main/ts/api/developer/workflowsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/workflowsApi")>();
  return {
    ...actual,
    createWorkflow: vi.fn(),
  };
});

const createWorkflow = workflowsApi.createWorkflow as ReturnType<typeof vi.fn>;

describe("WorkflowCreatePanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    createWorkflow.mockReset();
  });

  it("lists the base-workflow steps and cancel does not create", () => {
    const onBack = vi.fn();
    render(<WorkflowCreatePanel onBack={onBack} />);
    const steps = screen.getAllByTestId("developer-wf-create-step").map((el) => el.textContent);
    expect(steps).toEqual([...DEFAULT_WORKFLOW_TEMPLATE_STEPS]);
    fireEvent.change(screen.getByTestId("developer-wf-create-name"), {
      target: { value: "Nightly QA" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-create-cancel"));
    expect(onBack).toHaveBeenCalledTimes(1);
    expect(createWorkflow).not.toHaveBeenCalled();
  });

  it("maps 400 and 403 without treating them as created", async () => {
    const onCreated = vi.fn();
    render(<WorkflowCreatePanel onBack={() => undefined} onCreated={onCreated} />);
    fireEvent.change(screen.getByTestId("developer-wf-create-name"), {
      target: { value: "Nightly QA" },
    });
    createWorkflow.mockRejectedValueOnce({ status: 400, statusText: "Bad Request", body: null });
    fireEvent.click(screen.getByTestId("developer-wf-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-create-error").textContent).toContain(
        DEV_MSG.WF_INVALID_NAME,
      );
    });
    createWorkflow.mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: null });
    fireEvent.click(screen.getByTestId("developer-wf-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-create-error").textContent).toContain(
        DEV_MSG.WF_FORBIDDEN,
      );
    });
    expect(onCreated).not.toHaveBeenCalled();
  });

  it("disables save until the name is valid", () => {
    render(<WorkflowCreatePanel onBack={() => undefined} />);
    const save = screen.getByTestId("developer-wf-create-save") as HTMLButtonElement;
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-wf-create-name"), {
      target: { value: "bad/name!" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-wf-create-name"), {
      target: { value: "Nightly QA" },
    });
    expect(save.disabled).toBe(false);
  });

  it("does not POST an invalid name", () => {
    render(<WorkflowCreatePanel onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-wf-create-name"), {
      target: { value: "bad/name!" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-create-save"));
    expect(createWorkflow).not.toHaveBeenCalled();
  });

  it("creates a workflow when the name is valid", async () => {
    createWorkflow.mockResolvedValue({
      workflowName: "Nightly QA",
      workflowDescription: "QA",
      defaultWorkflow: false,
    });
    const onCreated = vi.fn();
    render(<WorkflowCreatePanel onBack={() => undefined} onCreated={onCreated} />);
    fireEvent.change(screen.getByTestId("developer-wf-create-name"), {
      target: { value: "Nightly QA" },
    });
    fireEvent.change(screen.getByTestId("developer-wf-create-description"), {
      target: { value: "QA" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-create-save"));
    await waitFor(() => {
      expect(onCreated).toHaveBeenCalled();
    });
    expect(createWorkflow).toHaveBeenCalledWith(
      expect.objectContaining({ name: "Nightly QA", description: "QA" }),
    );
    expect(screen.getByTestId("developer-wf-create-notice").textContent).toBe(
      DEV_MSG.WF_CREATED,
    );
  });

  it("does not POST create twice when save is clicked twice", async () => {
    let resolveCreate!: (v: unknown) => void;
    createWorkflow.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    render(<WorkflowCreatePanel onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-wf-create-name"), {
      target: { value: "Nightly QA" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-create-save"));
    fireEvent.click(screen.getByTestId("developer-wf-create-save"));
    resolveCreate({ workflowName: "Nightly QA" });
    await waitFor(() => {
      expect(
        screen.getByTestId("developer-wf-create-notice").textContent,
      ).toBe(DEV_MSG.WF_CREATED);
    });
    expect(createWorkflow).toHaveBeenCalledTimes(1);
  });

  it("maps duplicate names to the duplicate message", async () => {
    createWorkflow.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Workflow already exists: Simple Workflow" },
    });
    const onCreated = vi.fn();
    render(<WorkflowCreatePanel onBack={() => undefined} onCreated={onCreated} />);
    fireEvent.change(screen.getByTestId("developer-wf-create-name"), {
      target: { value: "Simple Workflow" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-create-error")).toBeTruthy();
    });
    expect(createWorkflow).toHaveBeenCalled();
    expect(screen.getByTestId("developer-wf-create-error").textContent).toContain(
      DEV_MSG.WF_DUPLICATE,
    );
    expect(onCreated).not.toHaveBeenCalled();
  });
});
