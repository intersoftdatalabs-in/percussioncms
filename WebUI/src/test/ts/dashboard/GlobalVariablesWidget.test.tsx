/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { GlobalVariablesWidget } from "@/dashboard/GlobalVariablesWidget";
import * as gadgetApi from "@/api/dashboard/gadgetApi";

vi.mock("@/api/dashboard/gadgetApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/gadgetApi")>();
  return {
    ...actual,
    fetchGlobalVariables: vi.fn(),
  };
});

describe("GlobalVariablesWidget", () => {
  beforeEach(() => {
    vi.mocked(gadgetApi.fetchGlobalVariables).mockReset();
  });

  it("lists variables", async () => {
    vi.mocked(gadgetApi.fetchGlobalVariables).mockResolvedValue([
      { name: "company", value: "Percussion" },
    ]);
    render(<GlobalVariablesWidget />);
    await waitFor(() => {
      expect(screen.getByTestId("global-variables-list")).toBeDefined();
    });
    expect(screen.getByText("company")).toBeDefined();
    expect(screen.getByText("Percussion")).toBeDefined();
  });

  it("shows empty", async () => {
    vi.mocked(gadgetApi.fetchGlobalVariables).mockResolvedValue([]);
    render(<GlobalVariablesWidget />);
    await waitFor(() => {
      expect(screen.getByTestId("global-variables-empty")).toBeDefined();
    });
  });
});
