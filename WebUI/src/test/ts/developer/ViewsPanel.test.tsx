/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ViewsPanel } from "../../../main/ts/developer/ViewsPanel";

vi.mock("../../../main/ts/api/developer/viewsApi", () => ({
  listViews: vi.fn().mockResolvedValue([
    {
      name: "My View",
      label: "My View",
      standardView: true,
      fields: [{ fieldName: "sys_title", operator: "=", fieldValue: "*" }],
    },
  ]),
  getViewDetail: vi.fn().mockResolvedValue({
    name: "My View",
    label: "My View",
    fields: [{ fieldName: "sys_title", operator: "=", fieldValue: "*" }],
    designGaps: ["gap"],
  }),
}));

describe("ViewsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  it("lists views and opens detail", async () => {
    render(<ViewsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-table").textContent).toContain("My View");
    fireEvent.click(screen.getByTestId("developer-vw-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-vw-fields-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-vw-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-vw-table")).toBeTruthy();
    });
  });
});
