/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SlotDetailPanel } from "../../../main/ts/developer/SlotDetailPanel";

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  getSlotDetail: vi.fn(),
  updateSlotDetail: vi.fn(),
}));

const getSlotDetail = assemblyApi.getSlotDetail as ReturnType<typeof vi.fn>;
const updateSlotDetail = assemblyApi.updateSlotDetail as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "rffList",
  label: "List",
  description: "List slot",
  slotType: "regular",
  systemSlot: false,
  finderName: "sys_SlotContentFinder",
  relationshipName: "Active Assembly",
  guid: { stringValue: "0-1-20" },
  associations: [],
  finderArguments: {},
  designGaps: [],
};

describe("SlotDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getSlotDetail.mockReset();
    updateSlotDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getSlotDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<SlotDetailPanel idOrName="rffList" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-title").textContent).toContain("List");
    expect(screen.getByTestId("developer-slot-assoc-empty")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-slot-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty associations section when detail has none", async () => {
    getSlotDetail.mockResolvedValue({ ...sampleDetail, associations: [] });
    render(<SlotDetailPanel idOrName="rffList" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-assoc-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-slot-assoc-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getSlotDetail.mockRejectedValue(new SessionRedirectError());
    render(<SlotDetailPanel idOrName="rffList" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-slot-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-slot-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getSlotDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<SlotDetailPanel idOrName="rffList" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toBe(
      `${DEV_MSG.SLOT_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getSlotDetail.mockRejectedValue(new Error("network down"));
    render(<SlotDetailPanel idOrName="rffList" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toBe(
      `${DEV_MSG.SLOT_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-slot-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getSlotDetail.mockRejectedValue("boom");
    render(<SlotDetailPanel idOrName="rffList" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toBe(
      DEV_MSG.SLOT_DETAIL_ERROR,
    );
  });
});
