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
});
