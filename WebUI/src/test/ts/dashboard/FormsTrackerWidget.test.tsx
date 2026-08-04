/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { FormsTrackerWidget } from "@/dashboard/FormsTrackerWidget";
import * as gadgetApi from "@/api/dashboard/gadgetApi";
import { MSG } from "@/i18n/message";

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
    delete (window as { I18N?: unknown }).I18N;
  });

  afterEach(() => {
    delete (window as { I18N?: unknown }).I18N;
  });

  it("localizes default title via I18N when key resolves (GH-1837)", async () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) =>
        k === MSG.GADGET_FORM_TRACKER ? "फ़ॉर्म ट्रैकर" : k,
    };
    vi.mocked(gadgetApi.fetchFormsForDefaultSite).mockResolvedValue({
      site: "Demo",
      forms: [],
    });
    render(<FormsTrackerWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("forms-tracker-empty")).toBeDefined();
    });
    const titleEl = screen
      .getByTestId("forms-tracker-widget")
      .querySelector("div");
    expect(titleEl?.textContent).toBe("फ़ॉर्म ट्रैकर");
    expect(titleEl?.textContent).not.toBe("Form Tracker");
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
