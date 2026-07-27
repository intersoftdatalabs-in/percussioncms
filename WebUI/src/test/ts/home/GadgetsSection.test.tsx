/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { GadgetsSection } from "@/home/sections/GadgetsSection";

vi.mock("@/dashboard", () => ({
  Dashboard: ({ embedded }: { embedded?: boolean }) => (
    <div data-testid="dashboard-root" data-embedded={embedded ? "1" : "0"} />
  ),
}));

describe("GadgetsSection", () => {
  it("embeds Dashboard with embedded flag", () => {
    render(<GadgetsSection />);
    expect(screen.getByTestId("home-gadgets-section")).toBeDefined();
    const dash = screen.getByTestId("dashboard-root");
    expect(dash.getAttribute("data-embedded")).toBe("1");
  });
});
