/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { ReportsWidget } from "@/dashboard/ReportsWidget";
import * as api from "@/api/dashboard/shellGadgetsApi";

vi.mock("@/api/dashboard/shellGadgetsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/shellGadgetsApi")>();
  return { ...actual, fetchReportsHubSnapshot: vi.fn() };
});

describe("ReportsWidget", () => {
  beforeEach(() => {
    vi.mocked(api.fetchReportsHubSnapshot).mockReset();
  });

  it("shows hub metrics", async () => {
    vi.mocked(api.fetchReportsHubSnapshot).mockResolvedValue({
      path: "/Sites/Demo",
      seoIssuePages: 3,
      formsCount: 2,
      formsNewSubmissions: 5,
      pagesWithComments: 1,
      activityRows: 4,
      activityNewItems: 7,
    });
    render(<ReportsWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("reports-hub")).toBeDefined();
    });
    expect(screen.getByText("SEO issue pages")).toBeDefined();
    expect(screen.getByText("3")).toBeDefined();
  });
});
