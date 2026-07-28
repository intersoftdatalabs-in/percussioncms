/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { CommentsWidget } from "@/dashboard/CommentsWidget";
import * as api from "@/api/dashboard/deliveryGadgetsApi";

vi.mock("@/api/dashboard/deliveryGadgetsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/deliveryGadgetsApi")>();
  return { ...actual, fetchDefaultPagesWithComments: vi.fn() };
});

describe("CommentsWidget", () => {
  beforeEach(() => {
    vi.mocked(api.fetchDefaultPagesWithComments).mockReset();
  });

  it("lists pages with comments", async () => {
    vi.mocked(api.fetchDefaultPagesWithComments).mockResolvedValue({
      site: "Demo",
      pages: [
        {
          id: "1",
          pageLinkTitle: "Home",
          commentCount: 3,
          newCount: 1,
          approvedCount: 2,
        },
      ],
    });
    render(<CommentsWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("comments-widget-list")).toBeDefined();
    });
    expect(screen.getByText("Home")).toBeDefined();
  });
});
