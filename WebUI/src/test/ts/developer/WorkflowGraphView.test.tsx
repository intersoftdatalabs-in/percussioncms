/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { WorkflowGraphView } from "../../../main/ts/developer/WorkflowGraphView";

vi.mock("../../../main/ts/api/developer/workflowsApi", () => ({
  getWorkflowGraph: vi.fn(),
  deleteWorkflowTransition: vi.fn(),
  deleteWorkflowStep: vi.fn(),
  updateTransitionCommentRequired: vi.fn(),
  updateTransitionApprovalsRequired: vi.fn(),
  markTransitionAsDefault: vi.fn(),
  restrictTransitionToOneRole: vi.fn(),
  addTransitionAllowedRole: vi.fn(),
  clearTransitionAllowedRoles: vi.fn(),
  isNonNegativeApprovalCount: (raw: string | number | null | undefined) => {
    if (typeof raw === "number") {
      return Number.isSafeInteger(raw) && raw >= 0;
    }
    if (typeof raw !== "string") {
      return false;
    }
    return /^(0|[1-9]\d*)$/.test(raw.trim());
  },
  createWorkflowTransition: vi.fn(),
  createWorkflowAgingTransition: vi.fn(),
  updateWorkflowAgingInterval: vi.fn(),
  deleteWorkflowAgingTransition: vi.fn(),
  isPositiveMinuteInterval: (raw: string | number | null | undefined) => {
    if (typeof raw === "number") {
      return Number.isSafeInteger(raw) && raw > 0;
    }
    if (typeof raw !== "string") {
      return false;
    }
    return /^[1-9]\d*$/.test(raw.trim());
  },
  updateWorkflowTransition: vi.fn(),
  isValidWorkflowName: (name: string | null | undefined) =>
    !!name && name.trim().length > 0 && name.trim().length <= 50 && /^[\s\w-]+$/.test(name.trim()),
}));

import * as workflowsApi from "../../../main/ts/api/developer/workflowsApi";

const getWorkflowGraph = workflowsApi.getWorkflowGraph as ReturnType<typeof vi.fn>;
const deleteWorkflowStep = workflowsApi.deleteWorkflowStep as ReturnType<typeof vi.fn>;
const updateTransitionCommentRequired =
  workflowsApi.updateTransitionCommentRequired as ReturnType<typeof vi.fn>;
const updateTransitionApprovalsRequired =
  workflowsApi.updateTransitionApprovalsRequired as ReturnType<typeof vi.fn>;
const markTransitionAsDefault = workflowsApi.markTransitionAsDefault as ReturnType<typeof vi.fn>;
const restrictTransitionToOneRole =
  workflowsApi.restrictTransitionToOneRole as ReturnType<typeof vi.fn>;
const addTransitionAllowedRole =
  workflowsApi.addTransitionAllowedRole as ReturnType<typeof vi.fn>;
const clearTransitionAllowedRoles =
  workflowsApi.clearTransitionAllowedRoles as ReturnType<typeof vi.fn>;
const createWorkflowTransition = workflowsApi.createWorkflowTransition as ReturnType<typeof vi.fn>;
const createWorkflowAgingTransition =
  workflowsApi.createWorkflowAgingTransition as ReturnType<typeof vi.fn>;
const updateWorkflowAgingInterval =
  workflowsApi.updateWorkflowAgingInterval as ReturnType<typeof vi.fn>;
const deleteWorkflowAgingTransition =
  workflowsApi.deleteWorkflowAgingTransition as ReturnType<typeof vi.fn>;
const deleteWorkflowTransition =
  workflowsApi.deleteWorkflowTransition as ReturnType<typeof vi.fn>;
const updateWorkflowTransition = workflowsApi.updateWorkflowTransition as ReturnType<typeof vi.fn>;

describe("WorkflowGraphView step delete", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key.replace(/^perc\.ui\.developer@/, ""),
    };
    getWorkflowGraph.mockReset();
    deleteWorkflowStep.mockReset();
    updateTransitionCommentRequired.mockReset();
    updateTransitionApprovalsRequired.mockReset();
    markTransitionAsDefault.mockReset();
    restrictTransitionToOneRole.mockReset();
    addTransitionAllowedRole.mockReset();
    clearTransitionAllowedRoles.mockReset();
    createWorkflowTransition.mockReset();
    createWorkflowAgingTransition.mockReset();
    updateWorkflowAgingInterval.mockReset();
    deleteWorkflowAgingTransition.mockReset();
    deleteWorkflowTransition.mockReset();
    updateWorkflowTransition.mockReset();
  });

  it("adds a transition between existing steps and does not call update", async () => {
    const initial = {
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [{ from: "Draft", to: "Review", label: "Submit", commentRequired: false }],
    };
    const updated = {
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [
        { from: "Draft", to: "Review", label: "Submit", commentRequired: false },
        { from: "Draft", to: "Review", label: "Send", commentRequired: false },
      ],
    };
    let current = initial;
    getWorkflowGraph.mockImplementation(async () => current);
    createWorkflowTransition.mockImplementation(async () => {
      current = updated;
      return updated;
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    const label = await screen.findByTestId("developer-wf-transition-label");
    fireEvent.change(label, { target: { value: "Send" } });
    fireEvent.click(screen.getByTestId("developer-wf-transition-add"));
    await waitFor(() => {
      expect(createWorkflowTransition).toHaveBeenCalledWith("Nightly QA", {
        from: "Draft",
        to: "Review",
        label: "Send",
      });
    });
    expect(updateWorkflowTransition).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-notice").textContent).toContain("Transition saved");
    });
  });

  it("updates the selected transition label and destination", async () => {
    const initial = {
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }, { name: "Pending" }],
      edges: [{ from: "Draft", to: "Review", label: "Submit" }],
    };
    const updated = {
      packaged: false,
      nodes: initial.nodes,
      edges: [{ from: "Draft", to: "Pending", label: "Send" }],
    };
    let current = initial;
    getWorkflowGraph.mockImplementation(async () => current);
    updateWorkflowTransition.mockImplementation(async () => {
      current = updated;
      return updated;
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    fireEvent.click(await screen.findByTestId("developer-wf-graph-edit-0"));
    fireEvent.change(screen.getByTestId("developer-wf-transition-label"), {
      target: { value: "Send" },
    });
    fireEvent.change(screen.getByTestId("developer-wf-transition-to"), {
      target: { value: "Pending" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-transition-save"));
    await waitFor(() => {
      expect(updateWorkflowTransition).toHaveBeenCalledWith(
        "Nightly QA",
        "Draft",
        "Submit",
        "Review",
        { to: "Pending", label: "Send" },
      );
    });
    expect(createWorkflowTransition).not.toHaveBeenCalled();
  });

  it("cancel edit does not post", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [{ from: "Draft", to: "Review", label: "Submit" }],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    fireEvent.click(await screen.findByTestId("developer-wf-graph-edit-0"));
    fireEvent.click(screen.getByTestId("developer-wf-transition-cancel"));
    expect(screen.queryByTestId("developer-wf-transition-save")).toBeNull();
    expect(updateWorkflowTransition).not.toHaveBeenCalled();
    expect(createWorkflowTransition).not.toHaveBeenCalled();
  });

  it("hides the transition form on packaged workflows", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: true,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [{ from: "Draft", to: "Review", label: "Submit" }],
    });
    render(<WorkflowGraphView workflowName="Default Workflow" />);
    await screen.findByTestId("developer-wf-graph-edge-0");
    expect(screen.queryByTestId("developer-wf-transition-form")).toBeNull();
    expect(screen.queryByTestId("developer-wf-graph-edit-0")).toBeNull();
  });

  it("maps 403 on create to the packaged message", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [],
    });
    createWorkflowTransition.mockRejectedValue({ status: 403, message: "forbidden" });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    fireEvent.change(await screen.findByTestId("developer-wf-transition-label"), {
      target: { value: "Send" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-transition-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-error").textContent).toMatch(/Packaged or default/i);
    });
  });

  it("saves comment required on a custom transition", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [{ from: "Draft", to: "Review", label: "Submit", commentRequired: false }],
    });
    updateTransitionCommentRequired.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [{ from: "Draft", to: "Review", label: "Submit", commentRequired: true }],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    const box = await screen.findByTestId("developer-wf-graph-comment-0");
    expect((box as HTMLInputElement).checked).toBe(false);
    fireEvent.click(box);
    await waitFor(() => {
      expect(updateTransitionCommentRequired).toHaveBeenCalledWith(
        "Nightly QA",
        "Draft",
        "Submit",
        true,
        "Review",
      );
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-notice").textContent).toContain(
        "Comment requirement saved",
      );
    });
    expect((screen.getByTestId("developer-wf-graph-comment-0") as HTMLInputElement).checked).toBe(
      true,
    );
  });

  it("hides step delete on packaged workflows", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: true,
      nodes: [{ name: "Draft" }],
      edges: [],
    });
    render(<WorkflowGraphView workflowName="Default Workflow" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-node-0")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-wf-graph-delete-step-0")).toBeNull();
  });

  it("confirms and removes an unreferenced step", async () => {
    getWorkflowGraph
      .mockResolvedValueOnce({
        packaged: false,
        nodes: [{ name: "Draft" }, { name: "Orphan" }],
        edges: [],
      })
      .mockResolvedValueOnce({
        packaged: false,
        nodes: [{ name: "Draft" }],
        edges: [],
      });
    deleteWorkflowStep.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }],
      edges: [],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-delete-step-1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-wf-graph-delete-step-1"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(deleteWorkflowStep).toHaveBeenCalledWith("Nightly QA", "Orphan");
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-notice").textContent).toContain(
        "Workflow step deleted",
      );
    });
  });

  it("maps 409 to the still-referenced message", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }],
      edges: [{ from: "Draft", to: "Draft", label: "Submit" }],
    });
    deleteWorkflowStep.mockRejectedValue({ status: 409, message: "conflict" });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-delete-step-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-wf-graph-delete-step-0"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-error").textContent).toMatch(/transition/i);
    });
  });

  it("lists an absolute aging transition only after the server accepts it", async () => {
    const initial = {
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [{ from: "Draft", to: "Review", label: "Submit", commentRequired: false }],
    };
    const updated = {
      packaged: false,
      nodes: initial.nodes,
      edges: [
        { from: "Draft", to: "Review", label: "Submit", commentRequired: false },
        { from: "Draft", to: "Review", label: "Aging 15", aging: true, intervalMinutes: 15 },
      ],
    };
    let current = initial;
    let release: (graph: typeof updated) => void = () => {};
    getWorkflowGraph.mockImplementation(async () => current);
    createWorkflowAgingTransition.mockImplementation(
      () =>
        new Promise((resolve) => {
          release = (graph) => {
            current = graph;
            resolve(graph);
          };
        }),
    );
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    fireEvent.change(await screen.findByTestId("developer-wf-aging-to"), {
      target: { value: "Review" },
    });
    fireEvent.change(screen.getByTestId("developer-wf-aging-minutes"), {
      target: { value: "15" },
    });
    expect(screen.queryByTestId("developer-wf-aging-edge-0")).toBeNull();
    fireEvent.click(screen.getByTestId("developer-wf-aging-add"));
    await waitFor(() => {
      expect(createWorkflowAgingTransition).toHaveBeenCalledWith("Nightly QA", {
        from: "Draft",
        to: "Review",
        intervalMinutes: 15,
      });
    });
    expect(screen.queryByTestId("developer-wf-aging-edge-0")).toBeNull();
    expect(screen.queryByTestId("developer-wf-graph-notice")).toBeNull();
    release(updated);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-aging-edge-0").textContent).toContain("Aging 15");
    });
    expect(screen.getByTestId("developer-wf-aging-edge-0").textContent).toContain("15 minutes");
    expect(screen.getByTestId("developer-wf-graph-notice").textContent).toContain(
      "Aging transition saved",
    );
    expect(screen.queryByTestId("developer-wf-graph-comment-1")).toBeNull();
  });

  it("cancel, a blank destination, and a non-positive interval do not call the server", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    const minutes = await screen.findByTestId("developer-wf-aging-minutes");
    fireEvent.change(minutes, { target: { value: "15" } });
    fireEvent.click(screen.getByTestId("developer-wf-aging-cancel"));
    expect(createWorkflowAgingTransition).not.toHaveBeenCalled();
    expect((screen.getByTestId("developer-wf-aging-minutes") as HTMLInputElement).value).toBe("");

    fireEvent.change(screen.getByTestId("developer-wf-aging-minutes"), { target: { value: "15" } });
    fireEvent.click(screen.getByTestId("developer-wf-aging-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-error").textContent).toMatch(/destination/i);
    });
    expect(createWorkflowAgingTransition).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-wf-graph-notice")).toBeNull();

    fireEvent.change(screen.getByTestId("developer-wf-aging-to"), { target: { value: "Review" } });
    fireEvent.change(screen.getByTestId("developer-wf-aging-minutes"), { target: { value: "0" } });
    fireEvent.click(screen.getByTestId("developer-wf-aging-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-error").textContent).toMatch(/positive/i);
    });
    expect(createWorkflowAgingTransition).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-wf-aging-edge-0")).toBeNull();
  });

  it("does not claim success on 400, 403, or 409", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    const to = await screen.findByTestId("developer-wf-aging-to");
    fireEvent.change(to, { target: { value: "Review" } });
    fireEvent.change(screen.getByTestId("developer-wf-aging-minutes"), { target: { value: "15" } });

    for (const status of [400, 403, 409]) {
      createWorkflowAgingTransition.mockRejectedValueOnce({ status, message: "no" });
      fireEvent.click(screen.getByTestId("developer-wf-aging-add"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-wf-graph-error")).toBeTruthy();
      });
      expect(screen.queryByTestId("developer-wf-graph-notice")).toBeNull();
      expect(screen.queryByTestId("developer-wf-aging-edge-0")).toBeNull();
    }
  });

  it("hides the aging form on a packaged workflow", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: true,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [
        { from: "Draft", to: "Review", label: "Aging 15", aging: true, intervalMinutes: 15 },
      ],
    });
    render(<WorkflowGraphView workflowName="Default Workflow" />);
    await screen.findByTestId("developer-wf-aging-edge-0");
    expect(screen.queryByTestId("developer-wf-aging-form")).toBeNull();
    expect(screen.queryByTestId("developer-wf-aging-change-0")).toBeNull();
    expect(screen.queryByTestId("developer-wf-aging-delete-0")).toBeNull();
    expect(screen.queryByTestId("developer-wf-graph-comment-0")).toBeNull();
  });

  it("shows the new aging minutes only after the server accepts the interval change", async () => {
    const initial = {
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [
        { from: "Draft", to: "Review", label: "Aging 15", aging: true, intervalMinutes: 15 },
      ],
    };
    const updated = {
      packaged: false,
      nodes: initial.nodes,
      edges: [
        { from: "Draft", to: "Review", label: "Aging 30", aging: true, intervalMinutes: 30 },
      ],
    };
    let current = initial;
    let release: (graph: typeof updated) => void = () => {};
    getWorkflowGraph.mockImplementation(async () => current);
    updateWorkflowAgingInterval.mockImplementation(
      () =>
        new Promise((resolve) => {
          release = (graph) => {
            current = graph;
            resolve(graph);
          };
        }),
    );
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    const edge = await screen.findByTestId("developer-wf-aging-edge-0");
    expect(edge.textContent).toContain("15 minutes");
    fireEvent.click(screen.getByTestId("developer-wf-aging-change-0"));
    fireEvent.change(screen.getByTestId("developer-wf-aging-new-minutes"), {
      target: { value: "30" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-aging-interval-save"));
    await waitFor(() => {
      expect(updateWorkflowAgingInterval).toHaveBeenCalledWith("Nightly QA", {
        from: "Draft",
        to: "Review",
        intervalMinutes: 15,
        newIntervalMinutes: 30,
      });
    });
    expect(screen.getByTestId("developer-wf-aging-edge-0").textContent).toContain("15 minutes");
    expect(screen.queryByTestId("developer-wf-graph-notice")).toBeNull();
    release(updated);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-aging-edge-0").textContent).toContain("30 minutes");
    });
    expect(screen.getByTestId("developer-wf-aging-edge-0").textContent).toContain("Aging 30");
    expect(screen.getByTestId("developer-wf-aging-edge-0").getAttribute("data-interval")).toBe("30");
    expect(screen.getByTestId("developer-wf-graph-notice").textContent).toContain(
      "Aging interval saved",
    );
    expect(screen.queryByTestId("developer-wf-aging-interval-form")).toBeNull();
  });

  it("cancel and a non-positive or unchanged interval do not call the server", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [
        { from: "Draft", to: "Review", label: "Aging 15", aging: true, intervalMinutes: 15 },
      ],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    await screen.findByTestId("developer-wf-aging-edge-0");
    fireEvent.click(screen.getByTestId("developer-wf-aging-change-0"));
    fireEvent.change(screen.getByTestId("developer-wf-aging-new-minutes"), {
      target: { value: "30" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-aging-interval-cancel"));
    expect(updateWorkflowAgingInterval).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-wf-aging-interval-form")).toBeNull();
    expect(screen.getByTestId("developer-wf-aging-edge-0").getAttribute("data-interval")).toBe("15");

    fireEvent.click(screen.getByTestId("developer-wf-aging-change-0"));
    fireEvent.change(screen.getByTestId("developer-wf-aging-new-minutes"), {
      target: { value: "0" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-aging-interval-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-error").textContent).toMatch(/positive/i);
    });
    expect(updateWorkflowAgingInterval).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-wf-graph-notice")).toBeNull();
    expect(screen.getByTestId("developer-wf-aging-edge-0").textContent).toContain("15 minutes");

    fireEvent.change(screen.getByTestId("developer-wf-aging-new-minutes"), {
      target: { value: "15" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-aging-interval-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-error").textContent).toMatch(/different/i);
    });
    expect(updateWorkflowAgingInterval).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-wf-aging-edge-0").getAttribute("data-interval")).toBe("15");
  });

  it("does not claim the interval changed on 400, 403, or 409", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [
        { from: "Draft", to: "Review", label: "Aging 15", aging: true, intervalMinutes: 15 },
      ],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    await screen.findByTestId("developer-wf-aging-change-0");
    for (const status of [400, 403, 409]) {
      fireEvent.click(screen.getByTestId("developer-wf-aging-change-0"));
      fireEvent.change(screen.getByTestId("developer-wf-aging-new-minutes"), {
        target: { value: "30" },
      });
      updateWorkflowAgingInterval.mockRejectedValueOnce({ status, message: "no" });
      fireEvent.click(screen.getByTestId("developer-wf-aging-interval-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-wf-graph-error")).toBeTruthy();
      });
      expect(screen.queryByTestId("developer-wf-graph-notice")).toBeNull();
      expect(screen.getByTestId("developer-wf-aging-edge-0").textContent).toContain("15 minutes");
      expect(screen.getByTestId("developer-wf-aging-edge-0").textContent).not.toContain("30 minutes");
    }
  });

  it("removes an aging transition only after confirm and leaves the regular transition", async () => {
    const initial = {
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [
        { from: "Draft", to: "Review", label: "Submit", commentRequired: false },
        { from: "Draft", to: "Review", label: "Aging 15", aging: true, intervalMinutes: 15 },
      ],
    };
    const updated = {
      packaged: false,
      nodes: initial.nodes,
      edges: [{ from: "Draft", to: "Review", label: "Submit", commentRequired: false }],
    };
    let current = initial;
    let release: (graph: typeof updated) => void = () => {};
    getWorkflowGraph.mockImplementation(async () => current);
    deleteWorkflowAgingTransition.mockImplementation(
      () =>
        new Promise((resolve) => {
          release = (graph) => {
            current = graph;
            resolve(graph);
          };
        }),
    );
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    await screen.findByTestId("developer-wf-aging-edge-0");
    expect(screen.getByTestId("developer-wf-graph-edge-0").textContent).toContain("Submit");
    fireEvent.click(screen.getByTestId("developer-wf-aging-delete-0"));
    expect(screen.getByTestId("developer-catalog-confirm-body").textContent).toContain("15 minutes");
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-cancel"));
    expect(deleteWorkflowAgingTransition).not.toHaveBeenCalled();
    expect(deleteWorkflowTransition).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-wf-aging-edge-0").textContent).toContain("15 minutes");

    fireEvent.click(screen.getByTestId("developer-wf-aging-delete-0"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(deleteWorkflowAgingTransition).toHaveBeenCalledWith("Nightly QA", "Draft", "Review", 15);
    });
    expect(deleteWorkflowTransition).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-wf-aging-edge-0").textContent).toContain("15 minutes");
    expect(screen.queryByTestId("developer-wf-graph-notice")).toBeNull();
    release(updated);
    await waitFor(() => {
      expect(screen.queryByTestId("developer-wf-aging-edge-0")).toBeNull();
    });
    expect(screen.getByTestId("developer-wf-graph-edge-0").textContent).toContain("Submit");
    expect(screen.getByTestId("developer-wf-graph-notice").textContent).toContain(
      "Aging transition deleted",
    );
  });

  it("does not claim the aging transition was deleted on 400, 403, or 409", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [
        { from: "Draft", to: "Review", label: "Submit" },
        { from: "Draft", to: "Review", label: "Aging 15", aging: true, intervalMinutes: 15 },
      ],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    await screen.findByTestId("developer-wf-aging-delete-0");
    for (const status of [400, 403, 409]) {
      deleteWorkflowAgingTransition.mockRejectedValueOnce({ status, message: "no" });
      fireEvent.click(screen.getByTestId("developer-wf-aging-delete-0"));
      fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-wf-graph-error")).toBeTruthy();
      });
      expect(screen.queryByTestId("developer-wf-graph-notice")).toBeNull();
      expect(screen.getByTestId("developer-wf-aging-edge-0").textContent).toContain("15 minutes");
      expect(screen.getByTestId("developer-wf-graph-edge-0").textContent).toContain("Submit");
    }
    expect(deleteWorkflowTransition).not.toHaveBeenCalled();
  });

  it("shows the new approval count only after save and cancel does not write", async () => {
    const initial = {
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [
        {
          from: "Draft",
          to: "Review",
          label: "Submit",
          commentRequired: true,
          approvalsRequired: 1,
        },
        {
          from: "Draft",
          to: "Live",
          label: "Publish",
          commentRequired: false,
          approvalsRequired: 4,
        },
      ],
    };
    const updated = {
      packaged: false,
      nodes: initial.nodes,
      edges: [
        {
          from: "Draft",
          to: "Review",
          label: "Submit",
          commentRequired: true,
          approvalsRequired: 2,
        },
        initial.edges[1],
      ],
    };
    let release: (graph: typeof updated) => void = () => {};
    getWorkflowGraph.mockResolvedValue(initial);
    updateTransitionApprovalsRequired.mockImplementation(
      () =>
        new Promise((resolve) => {
          release = resolve;
        }),
    );
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    const shown = await screen.findByTestId("developer-wf-graph-approvals-0");
    expect(shown.getAttribute("data-approvals")).toBe("1");
    expect(screen.getByTestId("developer-wf-graph-approvals-1").getAttribute("data-approvals")).toBe(
      "4",
    );
    fireEvent.click(screen.getByTestId("developer-wf-approvals-edit-0"));
    fireEvent.change(screen.getByTestId("developer-wf-approvals-value"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-approvals-cancel"));
    expect(updateTransitionApprovalsRequired).not.toHaveBeenCalled();
    expect(updateTransitionCommentRequired).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-wf-graph-approvals-0").getAttribute("data-approvals")).toBe(
      "1",
    );

    fireEvent.click(screen.getByTestId("developer-wf-approvals-edit-0"));
    fireEvent.change(screen.getByTestId("developer-wf-approvals-value"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-approvals-save"));
    await waitFor(() => {
      expect(updateTransitionApprovalsRequired).toHaveBeenCalledWith(
        "Nightly QA",
        "Draft",
        "Submit",
        2,
        "Review",
      );
    });
    expect(screen.getByTestId("developer-wf-graph-approvals-0").getAttribute("data-approvals")).toBe(
      "1",
    );
    expect((screen.getByTestId("developer-wf-graph-comment-0") as HTMLInputElement).checked).toBe(
      true,
    );
    release(updated);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-approvals-0").getAttribute("data-approvals")).toBe(
        "2",
      );
    });
    expect(screen.getByTestId("developer-wf-graph-notice").textContent).toContain(
      "Approvals required saved",
    );
    expect(screen.getByTestId("developer-wf-graph-approvals-1").getAttribute("data-approvals")).toBe(
      "4",
    );
    expect((screen.getByTestId("developer-wf-graph-comment-0") as HTMLInputElement).checked).toBe(
      true,
    );
    expect(screen.getByTestId("developer-wf-graph-edge-0").textContent).toContain("Submit");
    expect(screen.getByTestId("developer-wf-graph-edge-0").textContent).toContain("Review");
  });

  it("keeps the previous approval count on HTTP 400, 403, and 409", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [
        {
          from: "Draft",
          to: "Review",
          label: "Submit",
          commentRequired: false,
          approvalsRequired: 1,
        },
      ],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    await screen.findByTestId("developer-wf-graph-approvals-0");
    for (const status of [400, 403, 409]) {
      updateTransitionApprovalsRequired.mockRejectedValueOnce({ status, message: "no" });
      fireEvent.click(screen.getByTestId("developer-wf-approvals-edit-0"));
      fireEvent.change(screen.getByTestId("developer-wf-approvals-value"), {
        target: { value: "3" },
      });
      fireEvent.click(screen.getByTestId("developer-wf-approvals-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-wf-graph-error")).toBeTruthy();
      });
      expect(screen.queryByTestId("developer-wf-graph-notice")).toBeNull();
      expect(screen.getByTestId("developer-wf-graph-approvals-0").getAttribute("data-approvals")).toBe(
        "1",
      );
      expect((screen.getByTestId("developer-wf-graph-comment-0") as HTMLInputElement).checked).toBe(
        false,
      );
      fireEvent.click(screen.getByTestId("developer-wf-approvals-cancel"));
    }
  });

  it("does not write a blank or negative approval count", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [
        { from: "Draft", to: "Review", label: "Submit", approvalsRequired: 1, commentRequired: false },
      ],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    await screen.findByTestId("developer-wf-approvals-edit-0");
    fireEvent.click(screen.getByTestId("developer-wf-approvals-edit-0"));
    fireEvent.change(screen.getByTestId("developer-wf-approvals-value"), {
      target: { value: "-1" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-approvals-save"));
    expect(updateTransitionApprovalsRequired).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-wf-graph-approvals-0").getAttribute("data-approvals")).toBe(
      "1",
    );
  });

  it("shows the new default only after save and cancel does not write", async () => {
    const initial = {
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }, { name: "Live" }],
      edges: [
        {
          from: "Draft",
          to: "Review",
          label: "Submit",
          commentRequired: true,
          approvalsRequired: 1,
          defaultTransition: true,
        },
        {
          from: "Draft",
          to: "Live",
          label: "Send",
          commentRequired: false,
          approvalsRequired: 4,
          defaultTransition: false,
        },
        {
          from: "Review",
          to: "Live",
          label: "Approve",
          commentRequired: false,
          approvalsRequired: 2,
          defaultTransition: true,
        },
      ],
    };
    const updated = {
      packaged: false,
      nodes: initial.nodes,
      edges: [
        { ...initial.edges[0], defaultTransition: false },
        { ...initial.edges[1], defaultTransition: true },
        initial.edges[2],
      ],
    };
    let release: (graph: typeof updated) => void = () => {};
    getWorkflowGraph.mockResolvedValue(initial);
    markTransitionAsDefault.mockImplementation(
      () =>
        new Promise((resolve) => {
          release = resolve;
        }),
    );
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    const previous = await screen.findByTestId("developer-wf-graph-default-0");
    expect(previous.getAttribute("data-default")).toBe("true");
    expect(screen.getByTestId("developer-wf-graph-default-1").getAttribute("data-default")).toBe(
      "false",
    );
    expect(screen.getByTestId("developer-wf-graph-default-2").getAttribute("data-default")).toBe(
      "true",
    );
    expect(screen.queryByTestId("developer-wf-default-edit-0")).toBeNull();
    fireEvent.click(screen.getByTestId("developer-wf-default-edit-1"));
    fireEvent.click(screen.getByTestId("developer-wf-default-cancel"));
    expect(markTransitionAsDefault).not.toHaveBeenCalled();
    expect(updateTransitionApprovalsRequired).not.toHaveBeenCalled();
    expect(updateTransitionCommentRequired).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-wf-graph-default-0").getAttribute("data-default")).toBe(
      "true",
    );
    expect(screen.getByTestId("developer-wf-graph-default-1").getAttribute("data-default")).toBe(
      "false",
    );

    fireEvent.click(screen.getByTestId("developer-wf-default-edit-1"));
    fireEvent.click(screen.getByTestId("developer-wf-default-save"));
    await waitFor(() => {
      expect(markTransitionAsDefault).toHaveBeenCalledWith("Nightly QA", "Draft", "Send", "Live");
    });
    expect(screen.getByTestId("developer-wf-graph-default-0").getAttribute("data-default")).toBe(
      "true",
    );
    expect(screen.getByTestId("developer-wf-graph-default-1").getAttribute("data-default")).toBe(
      "false",
    );
    expect((screen.getByTestId("developer-wf-graph-comment-0") as HTMLInputElement).checked).toBe(
      true,
    );
    release(updated);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-default-1").getAttribute("data-default")).toBe(
        "true",
      );
    });
    expect(screen.getByTestId("developer-wf-graph-notice").textContent).toContain(
      "Default transition saved",
    );
    expect(screen.getByTestId("developer-wf-graph-default-0").getAttribute("data-default")).toBe(
      "false",
    );
    expect(screen.getByTestId("developer-wf-graph-default-2").getAttribute("data-default")).toBe(
      "true",
    );
    expect(screen.getByTestId("developer-wf-graph-approvals-1").getAttribute("data-approvals")).toBe(
      "4",
    );
    expect(screen.getByTestId("developer-wf-graph-edge-1").textContent).toContain("Send");
  });

  it("keeps the previous default on HTTP 400, 403, and 409", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      nodes: [{ name: "Draft" }, { name: "Review" }],
      edges: [
        {
          from: "Draft",
          to: "Review",
          label: "Submit",
          commentRequired: false,
          approvalsRequired: 1,
          defaultTransition: true,
        },
        {
          from: "Draft",
          to: "Live",
          label: "Send",
          commentRequired: true,
          approvalsRequired: 4,
          defaultTransition: false,
        },
      ],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    await screen.findByTestId("developer-wf-graph-default-0");
    for (const status of [400, 403, 409]) {
      markTransitionAsDefault.mockRejectedValueOnce({ status, message: "no" });
      fireEvent.click(screen.getByTestId("developer-wf-default-edit-1"));
      fireEvent.click(screen.getByTestId("developer-wf-default-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-wf-graph-error")).toBeTruthy();
      });
      expect(screen.queryByTestId("developer-wf-graph-notice")).toBeNull();
      expect(screen.getByTestId("developer-wf-graph-default-0").getAttribute("data-default")).toBe(
        "true",
      );
      expect(screen.getByTestId("developer-wf-graph-default-1").getAttribute("data-default")).toBe(
        "false",
      );
      expect(screen.getByTestId("developer-wf-graph-approvals-0").getAttribute("data-approvals")).toBe(
        "1",
      );
      fireEvent.click(screen.getByTestId("developer-wf-default-cancel"));
    }
  });

  it("shows one allowed role only after save and cancel does not write", async () => {
    const initial = {
      packaged: false,
      roles: ["Editor", "Author"],
      nodes: [{ name: "Draft" }, { name: "Review" }, { name: "Live" }],
      edges: [
        {
          from: "Draft",
          to: "Review",
          label: "Submit",
          commentRequired: true,
          approvalsRequired: 1,
          defaultTransition: true,
          allowAllRoles: true,
        },
        {
          from: "Draft",
          to: "Live",
          label: "Send",
          commentRequired: false,
          approvalsRequired: 4,
          defaultTransition: false,
          allowAllRoles: true,
        },
        {
          from: "Live",
          to: "Archive",
          label: "Expire",
          aging: true,
          intervalMinutes: 30,
        },
      ],
    };
    const updated = {
      ...initial,
      edges: [
        initial.edges[0],
        {
          ...initial.edges[1],
          allowAllRoles: false,
          allowedRoles: "Editor" as unknown as string[],
        },
        initial.edges[2],
      ],
    };
    let release: (graph: typeof updated) => void = () => {};
    getWorkflowGraph.mockResolvedValue(initial);
    restrictTransitionToOneRole.mockImplementation(
      () =>
        new Promise((resolve) => {
          release = resolve;
        }),
    );
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    const openBadge = await screen.findByTestId("developer-wf-graph-roles-1");
    expect(openBadge.getAttribute("data-allow-all")).toBe("true");
    expect(openBadge.textContent).toContain("All roles");
    expect(screen.getByTestId("developer-wf-graph-roles-0").getAttribute("data-allow-all")).toBe(
      "true",
    );
    expect(screen.queryByTestId("developer-wf-graph-roles-2")).toBeNull();
    expect(screen.queryByTestId("developer-wf-roles-edit-2")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-wf-roles-edit-1"));
    fireEvent.click(screen.getByTestId("developer-wf-roles-cancel"));
    expect(restrictTransitionToOneRole).not.toHaveBeenCalled();
    expect(markTransitionAsDefault).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-wf-graph-roles-1").getAttribute("data-allow-all")).toBe(
      "true",
    );
    expect(screen.getByTestId("developer-wf-graph-roles-1").textContent).toContain("All roles");

    fireEvent.click(screen.getByTestId("developer-wf-roles-edit-1"));
    fireEvent.change(screen.getByTestId("developer-wf-roles-value"), {
      target: { value: "Editor" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-roles-save"));
    await waitFor(() => {
      expect(restrictTransitionToOneRole).toHaveBeenCalledWith(
        "Nightly QA",
        "Draft",
        "Send",
        "Editor",
        "Live",
      );
    });
    expect(screen.getByTestId("developer-wf-graph-roles-1").getAttribute("data-allow-all")).toBe(
      "true",
    );
    expect(screen.getByTestId("developer-wf-graph-roles-1").textContent).toContain("All roles");
    release(updated);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-roles-1").getAttribute("data-allow-all")).toBe(
        "false",
      );
    });
    expect(screen.getByTestId("developer-wf-graph-roles-1").getAttribute("data-allowed-roles")).toBe(
      "Editor",
    );
    expect(screen.getByTestId("developer-wf-graph-roles-1").textContent).toContain("Editor");
    expect(screen.getByTestId("developer-wf-graph-roles-1").textContent).not.toContain("All roles");
    expect(screen.queryByTestId("developer-wf-roles-edit-1")).toBeNull();
    expect(screen.getByTestId("developer-wf-graph-roles-0").textContent).toContain("All roles");
    expect(screen.getByTestId("developer-wf-graph-notice").textContent).toContain(
      "Transition role saved",
    );
    expect(screen.getByTestId("developer-wf-graph-default-1").getAttribute("data-default")).toBe(
      "false",
    );
    expect(screen.getByTestId("developer-wf-graph-approvals-1").getAttribute("data-approvals")).toBe(
      "4",
    );
    expect(screen.queryByTestId("developer-wf-graph-roles-2")).toBeNull();
  });

  it("keeps allow-all on HTTP 400, 403, and 409", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      roles: ["Editor"],
      nodes: [{ name: "Draft" }, { name: "Live" }],
      edges: [
        {
          from: "Draft",
          to: "Live",
          label: "Send",
          commentRequired: false,
          approvalsRequired: 4,
          defaultTransition: false,
          allowAllRoles: true,
        },
      ],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    await screen.findByTestId("developer-wf-graph-roles-0");
    for (const status of [400, 403, 409]) {
      restrictTransitionToOneRole.mockRejectedValueOnce({ status, message: "no" });
      fireEvent.click(screen.getByTestId("developer-wf-roles-edit-0"));
      fireEvent.click(screen.getByTestId("developer-wf-roles-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-wf-graph-error")).toBeTruthy();
      });
      expect(screen.queryByTestId("developer-wf-graph-notice")).toBeNull();
      expect(screen.getByTestId("developer-wf-graph-roles-0").getAttribute("data-allow-all")).toBe(
        "true",
      );
      expect(screen.getByTestId("developer-wf-graph-roles-0").textContent).toContain("All roles");
      fireEvent.click(screen.getByTestId("developer-wf-roles-cancel"));
    }
  });

  it("hides the role editor on packaged workflows and already-restricted edges", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: true,
      roles: ["Editor"],
      nodes: [{ name: "Draft" }, { name: "Live" }],
      edges: [
        {
          from: "Draft",
          to: "Live",
          label: "Send",
          allowAllRoles: true,
          defaultTransition: true,
        },
      ],
    });
    const { unmount } = render(<WorkflowGraphView workflowName="Default Workflow" />);
    await screen.findByTestId("developer-wf-graph-roles-0");
    expect(screen.queryByTestId("developer-wf-roles-edit-0")).toBeNull();
    unmount();

    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      roles: ["Editor", "Author"],
      nodes: [{ name: "Draft" }, { name: "Live" }],
      edges: [
        {
          from: "Draft",
          to: "Live",
          label: "Send",
          allowAllRoles: false,
          allowedRoles: ["Editor"],
          defaultTransition: false,
        },
      ],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    const badge = await screen.findByTestId("developer-wf-graph-roles-0");
    expect(badge.getAttribute("data-allow-all")).toBe("false");
    expect(badge.textContent).toContain("Editor");
    expect(screen.queryByTestId("developer-wf-roles-edit-0")).toBeNull();
    expect(screen.getByTestId("developer-wf-roles-add-0")).toBeTruthy();
  });

  it("lists both roles only after save and cancel does not add a role", async () => {
    const initial = {
      packaged: false,
      roles: ["Editor", "Author"],
      nodes: [{ name: "Draft" }, { name: "Live" }],
      edges: [
        {
          from: "Draft",
          to: "Review",
          label: "Submit",
          commentRequired: true,
          approvalsRequired: 1,
          defaultTransition: true,
          allowAllRoles: true,
        },
        {
          from: "Draft",
          to: "Live",
          label: "Send",
          commentRequired: false,
          approvalsRequired: 4,
          defaultTransition: false,
          allowAllRoles: false,
          allowedRoles: ["Editor"],
        },
      ],
    };
    const updated = {
      ...initial,
      edges: [
        initial.edges[0],
        {
          ...initial.edges[1],
          allowAllRoles: false,
          allowedRoles: ["Editor", "Author"],
        },
      ],
    };
    let release: (graph: typeof updated) => void = () => {};
    getWorkflowGraph.mockResolvedValue(initial);
    addTransitionAllowedRole.mockImplementation(
      () =>
        new Promise((resolve) => {
          release = resolve;
        }),
    );
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    const badge = await screen.findByTestId("developer-wf-graph-roles-1");
    expect(badge.getAttribute("data-allow-all")).toBe("false");
    expect(badge.getAttribute("data-allowed-roles")).toBe("Editor");
    expect(screen.queryByTestId("developer-wf-roles-edit-1")).toBeNull();
    expect(screen.queryByTestId("developer-wf-roles-add-0")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-wf-roles-add-1"));
    fireEvent.click(screen.getByTestId("developer-wf-roles-add-cancel"));
    expect(addTransitionAllowedRole).not.toHaveBeenCalled();
    expect(restrictTransitionToOneRole).not.toHaveBeenCalled();
    expect(badge.getAttribute("data-allowed-roles")).toBe("Editor");
    expect(badge.textContent).not.toContain("Author");

    fireEvent.click(screen.getByTestId("developer-wf-roles-add-1"));
    fireEvent.change(screen.getByTestId("developer-wf-roles-add-value"), {
      target: { value: "Author" },
    });
    fireEvent.click(screen.getByTestId("developer-wf-roles-add-save"));
    await waitFor(() => {
      expect(addTransitionAllowedRole).toHaveBeenCalledWith(
        "Nightly QA",
        "Draft",
        "Send",
        "Author",
        "Live",
      );
    });
    expect(badge.getAttribute("data-allow-all")).toBe("false");
    expect(badge.getAttribute("data-allowed-roles")).toBe("Editor");
    expect(badge.textContent).not.toContain("Author");
    release(updated);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-roles-1").getAttribute("data-allowed-roles")).toBe(
        "Editor,Author",
      );
    });
    const saved = screen.getByTestId("developer-wf-graph-roles-1");
    expect(saved.getAttribute("data-allow-all")).toBe("false");
    expect(saved.textContent).toContain("Editor");
    expect(saved.textContent).toContain("Author");
    expect(saved.textContent).not.toContain("All roles");
    expect(screen.getByTestId("developer-wf-graph-roles-0").textContent).toContain("All roles");
    expect(screen.getByTestId("developer-wf-graph-notice").textContent).toContain(
      "Transition role added",
    );
    expect(screen.getByTestId("developer-wf-graph-approvals-1").getAttribute("data-approvals")).toBe(
      "4",
    );
    expect(screen.queryByTestId("developer-wf-roles-add-1")).toBeNull();
  });

  it("keeps the previous role list on HTTP 400, 403, and 409", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      roles: ["Editor", "Author"],
      nodes: [{ name: "Draft" }, { name: "Live" }],
      edges: [
        {
          from: "Draft",
          to: "Live",
          label: "Send",
          commentRequired: false,
          approvalsRequired: 4,
          defaultTransition: false,
          allowAllRoles: false,
          allowedRoles: ["Editor"],
        },
      ],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    await screen.findByTestId("developer-wf-graph-roles-0");
    for (const status of [400, 403, 409]) {
      addTransitionAllowedRole.mockRejectedValueOnce({ status, message: "no" });
      fireEvent.click(screen.getByTestId("developer-wf-roles-add-0"));
      fireEvent.click(screen.getByTestId("developer-wf-roles-add-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-wf-graph-error")).toBeTruthy();
      });
      expect(screen.queryByTestId("developer-wf-graph-notice")).toBeNull();
      const badge = screen.getByTestId("developer-wf-graph-roles-0");
      expect(badge.getAttribute("data-allow-all")).toBe("false");
      expect(badge.getAttribute("data-allowed-roles")).toBe("Editor");
      expect(badge.textContent).toContain("Editor");
      expect(badge.textContent).not.toContain("Author");
      expect(badge.textContent).not.toContain("All roles");
      fireEvent.click(screen.getByTestId("developer-wf-roles-add-cancel"));
    }
  });

  it("shows allow-all only after the role list is cleared and cancel does not write", async () => {
    const initial = {
      packaged: false,
      roles: ["Editor", "Author"],
      nodes: [{ name: "Draft" }, { name: "Live" }],
      edges: [
        {
          from: "Draft",
          to: "Review",
          label: "Submit",
          commentRequired: true,
          approvalsRequired: 1,
          defaultTransition: true,
          allowAllRoles: false,
          allowedRoles: ["Editor"],
        },
        {
          from: "Draft",
          to: "Live",
          label: "Send",
          commentRequired: false,
          approvalsRequired: 4,
          defaultTransition: false,
          allowAllRoles: false,
          allowedRoles: ["Editor", "Author"],
        },
      ],
    };
    const updated = {
      ...initial,
      edges: [
        initial.edges[0],
        {
          ...initial.edges[1],
          allowAllRoles: true,
          allowedRoles: undefined,
        },
      ],
    };
    let release: (graph: typeof updated) => void = () => {};
    getWorkflowGraph.mockResolvedValue(initial);
    clearTransitionAllowedRoles.mockImplementation(
      () =>
        new Promise((resolve) => {
          release = resolve;
        }),
    );
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    const badge = await screen.findByTestId("developer-wf-graph-roles-1");
    expect(badge.getAttribute("data-allow-all")).toBe("false");
    expect(badge.getAttribute("data-allowed-roles")).toBe("Editor,Author");
    expect(screen.queryByTestId("developer-wf-roles-clear-0")).toBeTruthy();
    expect(screen.getByTestId("developer-wf-roles-clear-1")).toBeTruthy();

    fireEvent.click(screen.getByTestId("developer-wf-roles-clear-1"));
    fireEvent.click(screen.getByTestId("developer-wf-roles-clear-cancel"));
    expect(clearTransitionAllowedRoles).not.toHaveBeenCalled();
    expect(addTransitionAllowedRole).not.toHaveBeenCalled();
    expect(badge.getAttribute("data-allow-all")).toBe("false");
    expect(badge.getAttribute("data-allowed-roles")).toBe("Editor,Author");
    expect(badge.textContent).not.toContain("All roles");

    fireEvent.click(screen.getByTestId("developer-wf-roles-clear-1"));
    fireEvent.click(screen.getByTestId("developer-wf-roles-clear-save"));
    await waitFor(() => {
      expect(clearTransitionAllowedRoles).toHaveBeenCalledWith(
        "Nightly QA",
        "Draft",
        "Send",
        "Live",
      );
    });
    expect(badge.getAttribute("data-allow-all")).toBe("false");
    expect(badge.textContent).not.toContain("All roles");
    release(updated);
    await waitFor(() => {
      expect(screen.getByTestId("developer-wf-graph-roles-1").getAttribute("data-allow-all")).toBe(
        "true",
      );
    });
    const saved = screen.getByTestId("developer-wf-graph-roles-1");
    expect(saved.getAttribute("data-allowed-roles")).toBe("");
    expect(saved.textContent).toContain("All roles");
    expect(saved.textContent).not.toContain("Editor");
    expect(saved.textContent).not.toContain("Author");
    expect(screen.getByTestId("developer-wf-graph-roles-0").getAttribute("data-allow-all")).toBe(
      "false",
    );
    expect(screen.getByTestId("developer-wf-graph-roles-0").textContent).toContain("Editor");
    expect(screen.getByTestId("developer-wf-graph-notice").textContent).toContain(
      "Every role may fire this transition",
    );
    expect(screen.getByTestId("developer-wf-graph-approvals-1").getAttribute("data-approvals")).toBe(
      "4",
    );
    expect(screen.queryByTestId("developer-wf-roles-clear-1")).toBeNull();
    expect(screen.queryByTestId("developer-wf-roles-add-1")).toBeNull();
    expect(screen.getByTestId("developer-wf-roles-clear-0")).toBeTruthy();
  });

  it("keeps the previous role list when clearing fails with HTTP 400, 403, and 409", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      roles: ["Editor", "Author"],
      nodes: [{ name: "Draft" }, { name: "Live" }],
      edges: [
        {
          from: "Draft",
          to: "Live",
          label: "Send",
          commentRequired: false,
          approvalsRequired: 4,
          defaultTransition: false,
          allowAllRoles: false,
          allowedRoles: ["Editor", "Author"],
        },
      ],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    await screen.findByTestId("developer-wf-graph-roles-0");
    for (const status of [400, 403, 409]) {
      clearTransitionAllowedRoles.mockRejectedValueOnce({ status, message: "no" });
      fireEvent.click(screen.getByTestId("developer-wf-roles-clear-0"));
      fireEvent.click(screen.getByTestId("developer-wf-roles-clear-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-wf-graph-error")).toBeTruthy();
      });
      expect(screen.queryByTestId("developer-wf-graph-notice")).toBeNull();
      const badge = screen.getByTestId("developer-wf-graph-roles-0");
      expect(badge.getAttribute("data-allow-all")).toBe("false");
      expect(badge.getAttribute("data-allowed-roles")).toBe("Editor,Author");
      expect(badge.textContent).toContain("Editor");
      expect(badge.textContent).toContain("Author");
      expect(badge.textContent).not.toContain("All roles");
      fireEvent.click(screen.getByTestId("developer-wf-roles-clear-cancel"));
    }
  });

  it("hides allow every role on packaged workflows and on allow-all edges", async () => {
    getWorkflowGraph.mockResolvedValue({
      packaged: true,
      roles: ["Editor"],
      nodes: [{ name: "Draft" }],
      edges: [
        {
          from: "Draft",
          to: "Live",
          label: "Send",
          allowAllRoles: false,
          allowedRoles: ["Editor"],
        },
      ],
    });
    const { unmount } = render(<WorkflowGraphView workflowName="Default Workflow" />);
    await screen.findByTestId("developer-wf-graph-roles-0");
    expect(screen.queryByTestId("developer-wf-roles-clear-0")).toBeNull();
    unmount();

    getWorkflowGraph.mockResolvedValue({
      packaged: false,
      roles: ["Editor", "Author"],
      nodes: [{ name: "Draft" }, { name: "Live" }],
      edges: [
        {
          from: "Draft",
          to: "Live",
          label: "Send",
          allowAllRoles: true,
        },
      ],
    });
    render(<WorkflowGraphView workflowName="Nightly QA" />);
    const badge = await screen.findByTestId("developer-wf-graph-roles-0");
    expect(badge.getAttribute("data-allow-all")).toBe("true");
    expect(screen.queryByTestId("developer-wf-roles-clear-0")).toBeNull();
    expect(screen.getByTestId("developer-wf-roles-edit-0")).toBeTruthy();
  });
});
