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
}));

import * as workflowsApi from "../../../main/ts/api/developer/workflowsApi";

const getWorkflowGraph = workflowsApi.getWorkflowGraph as ReturnType<typeof vi.fn>;
const deleteWorkflowStep = workflowsApi.deleteWorkflowStep as ReturnType<typeof vi.fn>;

describe("WorkflowGraphView step delete", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key.replace(/^perc\.ui\.developer@/, ""),
    };
    getWorkflowGraph.mockReset();
    deleteWorkflowStep.mockReset();
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
