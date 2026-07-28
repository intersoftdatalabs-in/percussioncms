/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { FormsTrackerWidget } from "@/dashboard/FormsTrackerWidget";
import * as gadgetApi from "@/api/dashboard/gadgetApi";

vi.mock("@/api/dashboard/gadgetApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/gadgetApi")>();
  return {
    ...actual,
    fetchFormsForDefaultSite: vi.fn(),
    fetchFormsForSite: vi.fn(),
  };
});

describe("FormsTrackerWidget", () => {
  beforeEach(() => {
    vi.mocked(gadgetApi.fetchFormsForDefaultSite).mockReset();
    vi.mocked(gadgetApi.fetchFormsForSite).mockReset();
  });

  it("lists forms for default site", async () => {
    vi.mocked(gadgetApi.fetchFormsForDefaultSite).mockResolvedValue({
      site: "Demo",
      forms: [
        {
          name: "contact",
          title: "Contact Us",
          state: "Live",
          totalSubmissions: 5,
          newSubmissions: 2,
        },
      ],
    });
    render(<FormsTrackerWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("forms-tracker-list")).toBeDefined();
    });
    expect(screen.getByText("Contact Us")).toBeDefined();
    expect(screen.getByText(/5/)).toBeDefined();
  });

  it("shows empty when no forms", async () => {
    vi.mocked(gadgetApi.fetchFormsForDefaultSite).mockResolvedValue({
      site: "Demo",
      forms: [],
    });
    render(<FormsTrackerWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("forms-tracker-empty")).toBeDefined();
    });
  });
});
