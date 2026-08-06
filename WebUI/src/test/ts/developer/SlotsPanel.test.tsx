/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import { SlotsPanel } from "../../../main/ts/developer/SlotsPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  listSlots: vi.fn(),
}));

const listSlots = assemblyApi.listSlots as ReturnType<typeof vi.fn>;

describe("SlotsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listSlots.mockReset();
  });

  it("lists slots on success", async () => {
    listSlots.mockResolvedValue([
      {
        name: "rffList",
        label: "List",
        description: "List slot",
        guid: { stringValue: "0-1-20", longValue: 20 },
      },
    ]);
    render(<SlotsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-table").textContent).toContain("List");
    expect(screen.getByTestId("developer-slot-table").textContent).toContain("rffList");
  });

  it("shows empty state when API returns no slots", async () => {
    listSlots.mockResolvedValue([]);
    render(<SlotsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listSlots.mockRejectedValue(new SessionRedirectError());
    render(<SlotsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-slot-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listSlots.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<SlotsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-error").textContent).toBe(
      `${DEV_MSG.SLOT_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    listSlots.mockRejectedValue(new Error("network down"));
    render(<SlotsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-error").textContent).toBe(
      `${DEV_MSG.SLOT_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-slot-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listSlots.mockRejectedValue("boom");
    render(<SlotsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-error").textContent).toBe(DEV_MSG.SLOT_ERROR);
  });
});
