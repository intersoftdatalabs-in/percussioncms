/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { GlobalVariablesWidget } from "@/dashboard/GlobalVariablesWidget";
import * as gadgetApi from "@/api/dashboard/gadgetApi";
import { MSG } from "@/i18n/message";

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
    delete (window as { I18N?: unknown }).I18N;
  });

  afterEach(() => {
    delete (window as { I18N?: unknown }).I18N;
  });

  it("localizes default title via I18N when key resolves (GH-1839)", async () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) =>
        k === MSG.GADGET_GLOBAL_VARIABLES ? "वैश्विक चर" : k,
    };
    vi.mocked(gadgetApi.fetchGlobalVariables).mockResolvedValue([]);
    render(<GlobalVariablesWidget />);
    await waitFor(() => {
      expect(screen.getByTestId("global-variables-empty")).toBeDefined();
    });
    const titleEl = screen
      .getByTestId("global-variables-widget")
      .querySelector("div");
    expect(titleEl?.textContent).toBe("वैश्विक चर");
    expect(titleEl?.textContent).not.toBe("Global Variables");
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
