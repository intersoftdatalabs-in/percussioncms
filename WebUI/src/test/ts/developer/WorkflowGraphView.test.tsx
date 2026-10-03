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
  createWorkflowTransition: vi.fn(),
  createWorkflowAgingTransition: vi.fn(),
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
const createWorkflowTransition = workflowsApi.createWorkflowTransition as ReturnType<typeof vi.fn>;
const createWorkflowAgingTransition =
  workflowsApi.createWorkflowAgingTransition as ReturnType<typeof vi.fn>;
const updateWorkflowTransition = workflowsApi.updateWorkflowTransition as ReturnType<typeof vi.fn>;

describe("WorkflowGraphView step delete", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key.replace(/^perc\.ui\.developer@/, ""),
    };
    getWorkflowGraph.mockReset();
    deleteWorkflowStep.mockReset();
    updateTransitionCommentRequired.mockReset();
    createWorkflowTransition.mockReset();
    createWorkflowAgingTransition.mockReset();
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
    expect(screen.queryByTestId("developer-wf-graph-comment-0")).toBeNull();
  });
});
