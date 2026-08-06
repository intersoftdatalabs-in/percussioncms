/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { WorkflowStatusWidget } from "@/dashboard/WorkflowStatusWidget";
import * as gadgetApi from "@/api/dashboard/gadgetApi";
import { MSG } from "@/i18n/message";

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
    delete (window as { I18N?: unknown }).I18N;
  });

  afterEach(() => {
    delete (window as { I18N?: unknown }).I18N;
  });

  it("localizes default title via I18N when key resolves (GH-1831)", async () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) =>
        k === MSG.GADGET_PAGES_BY_STATUS ? "स्थिति के अनुसार पेज" : k,
    };
    vi.mocked(gadgetApi.fetchPagesByStatusSummary).mockResolvedValue({
      path: "/Sites/Demo",
      workflow: "Default Workflow",
      buckets: [],
      totalItems: 0,
    });
    render(<WorkflowStatusWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("workflow-status-widget")).toBeDefined();
    });
    const titleEl = screen
      .getByTestId("workflow-status-widget")
      .querySelector("div");
    expect(titleEl?.textContent).toBe("स्थिति के अनुसार पेज");
    expect(titleEl?.textContent).not.toBe("Pages By Status");
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
