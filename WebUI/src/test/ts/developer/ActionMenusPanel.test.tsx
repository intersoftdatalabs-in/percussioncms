/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ActionMenusPanel } from "../../../main/ts/developer/ActionMenusPanel";

vi.mock("../../../main/ts/api/developer/actionMenusApi", () => ({
  listActionMenus: vi.fn().mockResolvedValue([
    {
      id: 1,
      name: "Edit",
      label: "Edit Item",
      menuType: "MENUITEM",
      handler: "CLIENT",
      parameters: [{ name: "sys_contentid", value: "0" }],
      properties: [],
    },
  ]),
  getActionMenuDetail: vi.fn().mockResolvedValue({
    id: 1,
    name: "Edit",
    label: "Edit",
    menuType: "MENUITEM",
    parameters: [{ name: "sys_contentid", value: "0" }],
    properties: [{ name: "AcceleratorKey", value: "E" }],
  }),
}));

describe("ActionMenusPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  it("lists action menus and opens detail", async () => {
    render(<ActionMenusPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-table").textContent).toContain("Edit");
    fireEvent.click(screen.getByTestId("developer-am-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-am-params-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-am-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-am-table")).toBeTruthy();
    });
  });
});
