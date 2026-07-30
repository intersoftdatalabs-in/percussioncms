/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DisplayFormatsPanel } from "../../../main/ts/developer/DisplayFormatsPanel";

vi.mock("../../../main/ts/api/developer/displayFormatsApi", () => ({
  listDisplayFormats: vi.fn().mockResolvedValue([
    {
      name: "Default",
      label: "Default View",
      description: "System default",
      validForFolder: true,
      validForViewsAndSearches: true,
      columns: [{ source: "sys_title", displayName: "Title", position: 0 }],
    },
  ]),
  getDisplayFormatDetail: vi.fn().mockResolvedValue({
    name: "Default",
    label: "Default View",
    columns: [{ source: "sys_title", displayName: "Title", position: 0, renderType: "text" }],
  }),
  normalizeColumns: (c: unknown) => (Array.isArray(c) ? c : []),
}));

describe("DisplayFormatsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  it("lists display formats and opens detail", async () => {
    render(<DisplayFormatsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-table")).toBeTruthy();
    });
    expect(screen.getByText("Default")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-df-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-df-columns-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-df-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-table")).toBeTruthy();
    });
  });
});
