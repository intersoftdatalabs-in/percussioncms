/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { SitewideFrameworkWidget } from "@/dashboard/SitewideFrameworkWidget";
import * as api from "@/api/dashboard/shellGadgetsApi";

vi.mock("@/api/dashboard/shellGadgetsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/shellGadgetsApi")>();
  return { ...actual, fetchThemeSummaries: vi.fn() };
});

describe("SitewideFrameworkWidget", () => {
  beforeEach(() => {
    vi.mocked(api.fetchThemeSummaries).mockReset();
  });

  it("lists themes", async () => {
    vi.mocked(api.fetchThemeSummaries).mockResolvedValue([
      { name: "perc-default", cssFilePath: "perc-default/theme.css" },
    ]);
    render(<SitewideFrameworkWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("sitewide-framework-list")).toBeDefined();
    });
    expect(screen.getByText("perc-default")).toBeDefined();
  });
});
