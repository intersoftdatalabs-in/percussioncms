/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import { SlotsPanel } from "../../../main/ts/developer/SlotsPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/assemblyApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/assemblyApi")
  >();
  return {
    ...actual,
    listSlots: vi.fn(),
    getSlotDetail: vi.fn(),
    updateSlotDetail: vi.fn(),
    createSlot: vi.fn(),
    deleteSlot: vi.fn(),
  };
});

const listSlots = assemblyApi.listSlots as ReturnType<typeof vi.fn>;
const getSlotDetail = assemblyApi.getSlotDetail as ReturnType<typeof vi.fn>;

describe("SlotsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listSlots.mockReset();
    getSlotDetail.mockReset();
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
    expect(screen.getByTestId("developer-slot-new")).toBeTruthy();
  });

  it("shows empty state when API returns no slots", async () => {
    listSlots.mockResolvedValue([]);
    render(<SlotsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-new")).toBeTruthy();
  });

  it("opens create chrome from New slot", async () => {
    listSlots.mockResolvedValue([]);
    render(<SlotsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-empty")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-slot-new"));
    expect(screen.getByTestId("developer-slot-detail")).toBeTruthy();
    expect(screen.getByTestId("developer-slot-save")).toBeDisabled();
    expect(getSlotDetail).not.toHaveBeenCalled();
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

  it("filters catalog rows by query with a named empty state and no refetch", async () => {
    listSlots.mockResolvedValue([
      {
        name: "rffList",
        label: "List",
        description: "List slot",
        guid: { stringValue: "0-1-20", longValue: 20 },
      },
      {
        name: "rffImage",
        label: "Image",
        description: "Image slot",
        guid: { stringValue: "0-1-21", longValue: 21 },
      },
    ]);
    render(<SlotsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-search")).toBeTruthy();
    fireEvent.change(screen.getByTestId("developer-slot-search"), {
      target: { value: "image" },
    });
    await waitFor(() => {
      expect(screen.queryByText("rffList")).toBeNull();
    });
    expect(screen.getByTestId("developer-slot-table").textContent).toContain("rffImage");
    fireEvent.change(screen.getByTestId("developer-slot-search"), {
      target: { value: "zzz-no-such-slot" },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-search-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-search-empty").textContent).toBe(
      DEV_MSG.SLOT_SEARCH_EMPTY,
    );
    expect(screen.queryByTestId("developer-slot-table")).toBeNull();
    fireEvent.change(screen.getByTestId("developer-slot-search"), {
      target: { value: "" },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-table").textContent).toContain("rffList");
    expect(listSlots).toHaveBeenCalledTimes(1);
  });
});
