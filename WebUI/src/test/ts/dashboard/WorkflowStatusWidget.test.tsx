/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { WorkflowStatusWidget } from "@/dashboard/WorkflowStatusWidget";
import * as gadgetApi from "@/api/dashboard/gadgetApi";

vi.mock("@/api/dashboard/gadgetApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/gadgetApi")>();
  return {
    ...actual,
    fetchPagesByStatusSummary: vi.fn(),
  };
});

describe("WorkflowStatusWidget", () => {
  beforeEach(() => {
    vi.mocked(gadgetApi.fetchPagesByStatusSummary).mockReset();
  });

  it("shows empty state when no pages", async () => {
    vi.mocked(gadgetApi.fetchPagesByStatusSummary).mockResolvedValue({
      path: "/Sites/Demo",
      workflow: "Default Workflow",
      buckets: [],
      totalItems: 0,
    });
    render(<WorkflowStatusWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("workflow-status-empty")).toBeDefined();
    });
  });

  it("renders status buckets", async () => {
    vi.mocked(gadgetApi.fetchPagesByStatusSummary).mockResolvedValue({
      path: "/Sites/Demo",
      workflow: "Default Workflow",
      totalItems: 3,
      buckets: [
        { state: "Draft", count: 2, sampleNames: ["Home", "About"] },
        { state: "Live", count: 1, sampleNames: ["Index"] },
      ],
    });
    render(<WorkflowStatusWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("workflow-status-list")).toBeDefined();
    });
    expect(screen.getByText("Draft")).toBeDefined();
    expect(screen.getByText("Live")).toBeDefined();
    expect(screen.getByText("2")).toBeDefined();
    expect(screen.getByText("1")).toBeDefined();
    expect(screen.getByText(/Home, About/)).toBeDefined();
  });

  it("shows error on API failure", async () => {
    vi.mocked(gadgetApi.fetchPagesByStatusSummary).mockRejectedValue(
      new Error("wf fail"),
    );
    render(<WorkflowStatusWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("workflow-status-error")).toBeDefined();
    });
    expect(screen.getByText(/wf fail/)).toBeDefined();
  });
});
