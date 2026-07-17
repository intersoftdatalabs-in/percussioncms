/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { WidgetBuilderApp } from "@/widgetbuilder/WidgetBuilderApp";

vi.mock("@/api/widgetbuilder/widgetBuilderApi", () => ({
  isWidgetBuilderActive: vi.fn(),
  fetchSummaries: vi.fn().mockResolvedValue([]),
  loadDefinition: vi.fn(),
  saveDefinition: vi.fn(),
  validateDefinition: vi.fn(),
  deployDefinition: vi.fn(),
  deleteDefinition: vi.fn(),
}));

import { isWidgetBuilderActive } from "@/api/widgetbuilder/widgetBuilderApi";

describe("WidgetBuilder enablement", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k) => k,
    };
    vi.mocked(isWidgetBuilderActive).mockReset();
  });

  it("shows disabled message when inactive", async () => {
    vi.mocked(isWidgetBuilderActive).mockResolvedValue(false);
    render(<WidgetBuilderApp />);
    await waitFor(() => {
      expect(screen.getByTestId("wb-disabled")).toBeDefined();
    });
    expect(
      screen.getByText("perc.ui.widgetbuilder.modern@Disabled"),
    ).toBeDefined();
  });

  it("shows list when active", async () => {
    vi.mocked(isWidgetBuilderActive).mockResolvedValue(true);
    render(<WidgetBuilderApp />);
    await waitFor(() => {
      expect(screen.getByTestId("wb-definition-list")).toBeDefined();
    });
  });
});
