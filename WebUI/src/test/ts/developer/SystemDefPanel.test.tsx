/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { getSystemDef } from "../../../main/ts/api/developer/systemDefApi";
import { SystemDefPanel } from "../../../main/ts/developer/SystemDefPanel";

vi.mock("../../../main/ts/api/developer/systemDefApi", () => ({
  getSystemDef: vi.fn(),
}));

const getMock = vi.mocked(getSystemDef);

describe("SystemDefPanel", () => {
  beforeEach(() => {
    getMock.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("renders system field catalog when load succeeds", async () => {
    getMock.mockResolvedValue({
      fieldCount: 1,
      cacheTimeoutMinutes: 15,
      fields: [
        {
          name: "sys_title",
          dataType: "text",
          required: true,
          searchable: true,
          readOnly: false,
          occurrence: "required",
        },
      ],
      designGaps: ["write not supported"],
    });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-fields-table")).toBeTruthy();
    });
    expect(screen.getByText("sys_title")).toBeTruthy();
    expect(screen.getByTestId("developer-sys-gaps")).toBeTruthy();
  });

  it("shows empty state when no fields", async () => {
    getMock.mockResolvedValue({ fieldCount: 0, fields: [], designGaps: [] });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-empty")).toBeTruthy();
    });
  });

  it("shows error UI when fetch fails", async () => {
    getMock.mockRejectedValue({ status: 500, statusText: "Error" });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-error")).toBeTruthy();
    });
  });
});
