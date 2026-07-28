/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { BootstrapProvider } from "@/app/bootstrap/BootstrapContext";
import { DEFAULT_SPA_BOOTSTRAP } from "@/app/bootstrap/types";
import { GadgetsSection } from "@/home/sections/GadgetsSection";

vi.mock("@/dashboard", () => ({
  Dashboard: ({
    embedded,
    userId,
  }: {
    embedded?: boolean;
    userId?: string;
  }) => (
    <div
      data-testid="dashboard-root"
      data-embedded={embedded ? "1" : "0"}
      data-user-id={userId ?? ""}
    />
  ),
}));

describe("GadgetsSection", () => {
  it("embeds Dashboard with embedded flag and bootstrap userId", () => {
    render(
      <BootstrapProvider
        value={{ ...DEFAULT_SPA_BOOTSTRAP, userName: "admin" }}
      >
        <GadgetsSection />
      </BootstrapProvider>,
    );
    expect(screen.getByTestId("home-gadgets-section")).toBeDefined();
    const dash = screen.getByTestId("dashboard-root");
    expect(dash.getAttribute("data-embedded")).toBe("1");
    expect(dash.getAttribute("data-user-id")).toBe("admin");
  });
});
