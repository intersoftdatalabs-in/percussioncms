/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SearchesPanel } from "../../../main/ts/developer/SearchesPanel";

vi.mock("../../../main/ts/api/developer/searchesApi", () => ({
  listSearches: vi.fn().mockResolvedValue([
    {
      name: "All Content",
      label: "All Content",
      standardSearch: true,
      fields: [{ fieldName: "sys_title", operator: "=", fieldValue: "*" }],
    },
  ]),
  getSearchDetail: vi.fn().mockResolvedValue({
    name: "All Content",
    label: "All Content",
    fields: [{ fieldName: "sys_title", operator: "=", fieldValue: "*" }],
    designGaps: ["gap"],
  }),
}));

describe("SearchesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  it("lists searches and opens detail", async () => {
    render(<SearchesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-table").textContent).toContain("All Content");
    fireEvent.click(screen.getByTestId("developer-sr-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-fields-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-sr-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-table")).toBeTruthy();
    });
  });
});
