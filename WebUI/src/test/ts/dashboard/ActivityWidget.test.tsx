/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { ActivityWidget } from "@/dashboard/ActivityWidget";
import * as gadgetApi from "@/api/dashboard/gadgetApi";

vi.mock("@/api/dashboard/gadgetApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/gadgetApi")>();
  return {
    ...actual,
    fetchContentActivity: vi.fn(),
    resolveDefaultActivityPath: vi.fn().mockResolvedValue("/Sites/Demo"),
  };
});

describe("ActivityWidget", () => {
  beforeEach(() => {
    vi.mocked(gadgetApi.fetchContentActivity).mockReset();
    vi.mocked(gadgetApi.resolveDefaultActivityPath)
      .mockReset()
      .mockResolvedValue("/Sites/Demo");
  });

  it("shows empty state when no activity rows", async () => {
    vi.mocked(gadgetApi.fetchContentActivity).mockResolvedValue([]);
    render(<ActivityWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("activity-widget-empty")).toBeDefined();
    });
  });

  it("renders content activity metrics", async () => {
    vi.mocked(gadgetApi.fetchContentActivity).mockResolvedValue([
      {
        name: "Demo",
        siteName: "Demo",
        publishedItems: 5,
        pendingItems: 2,
        newItems: 1,
        updatedItems: 3,
        archivedItems: 0,
      },
    ]);
    render(<ActivityWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("activity-widget-list")).toBeDefined();
    });
    expect(screen.getByText("Demo")).toBeDefined();
    expect(screen.getByText(/Published:\s*5/)).toBeDefined();
    expect(screen.getByText(/Pending:\s*2/)).toBeDefined();
    expect(gadgetApi.fetchContentActivity).toHaveBeenCalledWith(
      "/Sites/Demo",
      "days",
      30,
    );
  });

  it("shows error on API failure", async () => {
    vi.mocked(gadgetApi.fetchContentActivity).mockRejectedValue(
      new Error("boom"),
    );
    render(<ActivityWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("activity-widget-error")).toBeDefined();
    });
    expect(screen.getByText(/boom/)).toBeDefined();
  });
});
