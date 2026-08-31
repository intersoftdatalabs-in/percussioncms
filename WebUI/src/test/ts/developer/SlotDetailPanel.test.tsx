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

vi.mock("../../../main/ts/api/developer/assemblyApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/assemblyApi")
  >();
  return {
    ...actual,
    getSlotDetail: vi.fn(),
    updateSlotDetail: vi.fn(),
    createSlot: vi.fn(),
    deleteSlot: vi.fn(),
    lockSlot: vi.fn(),
    unlockSlot: vi.fn(),
  };
});

const getSlotDetail = assemblyApi.getSlotDetail as ReturnType<typeof vi.fn>;
const updateSlotDetail = assemblyApi.updateSlotDetail as ReturnType<typeof vi.fn>;
const createSlot = assemblyApi.createSlot as ReturnType<typeof vi.fn>;
const deleteSlot = assemblyApi.deleteSlot as ReturnType<typeof vi.fn>;
const lockSlot = assemblyApi.lockSlot as ReturnType<typeof vi.fn>;
const unlockSlot = assemblyApi.unlockSlot as ReturnType<typeof vi.fn>;

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
    createSlot.mockReset();
    deleteSlot.mockReset();
    lockSlot.mockReset();
    unlockSlot.mockReset();
    lockSlot.mockResolvedValue({ locker: "Admin" });
    unlockSlot.mockResolvedValue(undefined);
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

  it("does not throw when associations is a Jackson empty bean (#3554)", async () => {
    getSlotDetail.mockResolvedValue({
      ...sampleDetail,
      associations: { empty: false },
      designGaps: [
        { code: "SLOT_CREATE_DELETE", message: "Create / delete not supported" },
      ],
    });
    render(<SlotDetailPanel idOrName="sys_AutoIndex" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail")).toBeTruthy();
    expect(screen.getByTestId("developer-slot-assoc-empty")).toBeTruthy();
    expect(screen.getByTestId("developer-slot-gaps").textContent).toContain(
      "Create / delete not supported",
    );
    expect(
      screen.getByTestId("developer-slot-gaps").querySelector(
        '[data-gap-code="SLOT_CREATE_DELETE"]',
      ),
    ).toBeTruthy();
    expect(screen.queryByTestId("developer-slot-detail-error")).toBeNull();
  });

  it("renders JAXB finderArguments entries as readable strings (#3554)", async () => {
    getSlotDetail.mockResolvedValue({
      ...sampleDetail,
      associations: {
        contentTypeGuid: { stringValue: "0-2-316" },
        templateGuid: { stringValue: "0-4-512" },
      },
      finderArguments: {
        entry: [
          { key: "template", value: "rffSnDateAndTitleLink" },
          { key: "type", value: "sql" },
          { key: "query", value: "SELECT 1" },
        ],
      },
      designGaps: [
        { code: "SLOT_CREATE_DELETE", message: "Create / delete not supported via this REST API" },
      ],
    });
    render(<SlotDetailPanel idOrName="rffAutoPressReleases2007" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-title")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("developer-slot-arg-key-0") as HTMLInputElement).value,
    ).toBe("template");
    expect(
      (screen.getByTestId("developer-slot-arg-value-0") as HTMLInputElement).value,
    ).toBe("rffSnDateAndTitleLink");
    expect(
      (screen.getByTestId("developer-slot-arg-key-1") as HTMLInputElement).value,
    ).toBe("type");
    expect(
      (screen.getByTestId("developer-slot-arg-value-1") as HTMLInputElement).value,
    ).toBe("sql");
    expect(screen.getByTestId("developer-slot-assoc-row-0").textContent).toContain("0-2-316");
    expect(screen.getByTestId("developer-slot-gaps").textContent).toContain(
      "Create / delete not supported via this REST API",
    );
    expect(screen.queryByTestId("developer-slot-detail-error")).toBeNull();
  });

  it("unwraps a single association object and JAXB DesignGap envelope (#3554)", async () => {
    getSlotDetail.mockResolvedValue({
      ...sampleDetail,
      associations: {
        contentTypeGuid: { stringValue: "0-2-301" },
        templateGuid: { stringValue: "0-10-1" },
      },
      designGaps: {
        DesignGap: { code: "SLOT_ASSOC_GUIDS_ONLY", message: "Guids only on associations" },
      },
    });
    render(<SlotDetailPanel idOrName="rffCalendar" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-assoc-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-assoc-row-0").textContent).toContain("0-2-301");
    expect(screen.getByTestId("developer-slot-gaps").textContent).toContain(
      "Guids only on associations",
    );
    expect(
      screen.getByTestId("developer-slot-gaps").querySelector(
        '[data-gap-code="SLOT_ASSOC_GUIDS_ONLY"]',
      ),
    ).toBeTruthy();
  });

  it("disables save for invalid name (spaces / wildcard)", () => {
    render(<SlotDetailPanel idOrName={null} onBack={() => undefined} />);
    const save = screen.getByTestId("developer-slot-save") as HTMLButtonElement;
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "my slot" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "foo*" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "qaSlot" },
    });
    expect(save.disabled).toBe(false);
    expect(createSlot).not.toHaveBeenCalled();
  });

  it("surfaces 400 invalid slotType from create", async () => {
    createSlot.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "slotType must be REGULAR or INLINE" },
    });
    render(<SlotDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "qaSlot" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
      DEV_MSG.SLOT_TYPE_INVALID,
    );
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
      "slotType",
    );
  });

  it("surfaces 400 invalid name from create", async () => {
    createSlot.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "name cannot contain whitespace" },
    });
    render(<SlotDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "qaSlot" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
      DEV_MSG.SLOT_NAME_INVALID,
    );
  });

  it("does not treat a name 400 as slotType when the message only mentions slotType", async () => {
    createSlot.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "name cannot contain the token slotType" },
    });
    render(<SlotDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "qaSlot" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
      DEV_MSG.SLOT_NAME_INVALID,
    );
    expect(screen.getByTestId("developer-slot-detail-error").textContent).not.toContain(
      DEV_MSG.SLOT_TYPE_INVALID,
    );
  });

  it("trims label and description on update", async () => {
    getSlotDetail.mockResolvedValue(sampleDetail);
    updateSlotDetail.mockResolvedValue({
      ...sampleDetail,
      label: "QA Slot",
      description: "Trimmed",
    });
    render(<SlotDetailPanel idOrName="rffList" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-label")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-slot-label"), {
      target: { value: "  QA Slot  " },
    });
    const description = screen.getByTestId("developer-slot-description") as HTMLTextAreaElement
      | HTMLInputElement;
    fireEvent.change(description, { target: { value: "  Trimmed  " } });
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(updateSlotDetail).toHaveBeenCalled();
    });
    expect(updateSlotDetail).toHaveBeenCalledWith(
      "rffList",
      expect.objectContaining({
        label: "QA Slot",
        description: "Trimmed",
      }),
    );
    const body = updateSlotDetail.mock.calls.at(-1)?.[1] as Record<string, unknown>;
    expect(body).not.toHaveProperty("finderName");
    expect(body).not.toHaveProperty("relationshipName");
    expect(body).not.toHaveProperty("finderArguments");
  });

  it("surfaces 409 duplicate name on create", async () => {
    createSlot.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Slot already exists: rffList" },
    });
    const onSaved = vi.fn();
    render(
      <SlotDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />,
    );
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "rffList" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(createSlot).toHaveBeenCalled();
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
      DEV_MSG.SLOT_DUPLICATE,
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 403 non-Admin on create", async () => {
    createSlot.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<SlotDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "qaSlot" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
      DEV_MSG.SLOT_FORBIDDEN,
    );
  });

  it("creates a slot when name is valid", async () => {
    createSlot.mockResolvedValue({
      name: "qaSlot",
      label: "QA Slot",
      slotType: "REGULAR",
      systemSlot: false,
      associations: [],
      designGaps: [],
    });
    const onSaved = vi.fn();
    render(
      <SlotDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />,
    );
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "qaSlot" },
    });
    fireEvent.change(screen.getByTestId("developer-slot-label"), {
      target: { value: "QA Slot" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(createSlot).toHaveBeenCalledWith(
      expect.objectContaining({
        name: "qaSlot",
        label: "QA Slot",
        slotType: "REGULAR",
      }),
    );
    expect(screen.getByTestId("developer-slot-detail-notice").textContent).toBe(
      DEV_MSG.SLOT_SAVED,
    );
    expect(screen.getByTestId("developer-slot-delete")).toBeTruthy();
  });

  it("does not POST create twice when save is clicked twice", async () => {
    let resolveCreate: (value: typeof sampleDetail) => void = () => undefined;
    createSlot.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    render(<SlotDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "qaSlot" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    expect(createSlot).toHaveBeenCalledTimes(1);
    resolveCreate({ ...sampleDetail, name: "qaSlot" });
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-notice")).toBeTruthy();
    });
  });

  it("does not show delete on create", () => {
    render(<SlotDetailPanel idOrName={null} onBack={() => undefined} />);
    expect(screen.queryByTestId("developer-slot-delete")).toBeNull();
  });

  it("deletes after confirm", async () => {
    getSlotDetail.mockResolvedValue(sampleDetail);
    deleteSlot.mockResolvedValue(undefined);
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    try {
      const onDeleted = vi.fn();
      render(
        <SlotDetailPanel
          idOrName="rffList"
          onBack={() => undefined}
          onDeleted={onDeleted}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("developer-slot-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-slot-delete"));
      await waitFor(() => {
        expect(onDeleted).toHaveBeenCalled();
      });
      expect(deleteSlot).toHaveBeenCalledWith("rffList");
    } finally {
      confirmSpy.mockRestore();
    }
  });

  it("surfaces 409 system-slot delete", async () => {
    getSlotDetail.mockResolvedValue({
      ...sampleDetail,
      name: "sys_inline_link",
      systemSlot: true,
    });
    deleteSlot.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "System slots cannot be deleted" },
    });
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    try {
      const onDeleted = vi.fn();
      render(
        <SlotDetailPanel
          idOrName="sys_inline_link"
          onBack={() => undefined}
          onDeleted={onDeleted}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("developer-slot-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-slot-delete"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
      });
      expect(deleteSlot).toHaveBeenCalledWith("sys_inline_link");
      expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
        DEV_MSG.SLOT_DELETE_SYSTEM,
      );
      expect(onDeleted).not.toHaveBeenCalled();
    } finally {
      confirmSpy.mockRestore();
    }
  });

  it("does not treat a generic 409 containing system as a system-slot delete", async () => {
    getSlotDetail.mockResolvedValue(sampleDetail);
    deleteSlot.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "ecosystem constraint" },
    });
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    try {
      const onDeleted = vi.fn();
      render(
        <SlotDetailPanel
          idOrName="rffList"
          onBack={() => undefined}
          onDeleted={onDeleted}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("developer-slot-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-slot-delete"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
        DEV_MSG.SLOT_DELETE_ERROR,
      );
      expect(screen.getByTestId("developer-slot-detail-error").textContent).not.toContain(
        DEV_MSG.SLOT_DELETE_SYSTEM,
      );
      expect(onDeleted).not.toHaveBeenCalled();
    } finally {
      confirmSpy.mockRestore();
    }
  });

  it("surfaces 403 non-Admin on delete", async () => {
    getSlotDetail.mockResolvedValue(sampleDetail);
    deleteSlot.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    try {
      render(<SlotDetailPanel idOrName="rffList" onBack={() => undefined} />);
      await waitFor(() => {
        expect(screen.getByTestId("developer-slot-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-slot-delete"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
        DEV_MSG.SLOT_FORBIDDEN,
      );
    } finally {
      confirmSpy.mockRestore();
    }
  });

  async function renderLoadedSlot() {
    getSlotDetail.mockResolvedValue(sampleDetail);
    render(<SlotDetailPanel idOrName="rffList" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-finder")).toBeTruthy();
    });
  }

  async function lockSlotPanel() {
    fireEvent.click(screen.getByTestId("developer-slot-lock"));
    await waitFor(() => {
      expect(lockSlot).toHaveBeenCalledWith("rffList");
    });
    await waitFor(() => {
      expect(
        (screen.getByTestId("developer-slot-finder") as HTMLInputElement).disabled,
      ).toBe(false);
    });
  }

  it("keeps finder fields read-only until lock", async () => {
    await renderLoadedSlot();
    expect(
      (screen.getByTestId("developer-slot-finder") as HTMLInputElement).disabled,
    ).toBe(true);
    expect(
      (screen.getByTestId("developer-slot-relationship") as HTMLInputElement).disabled,
    ).toBe(true);
    expect(screen.getByTestId("developer-slot-lock-status").textContent).toBe(
      DEV_MSG.SLOT_UNLOCKED,
    );
  });

  it("omits unchanged finder fields on a properties-only save", async () => {
    await renderLoadedSlot();
    updateSlotDetail.mockResolvedValue({
      ...sampleDetail,
      label: "List edited",
    });
    fireEvent.change(screen.getByTestId("developer-slot-label"), {
      target: { value: "List edited" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(updateSlotDetail).toHaveBeenCalled();
    });
    expect(lockSlot).not.toHaveBeenCalled();
    const body = updateSlotDetail.mock.calls.at(-1)?.[1] as Record<string, unknown>;
    expect(body.label).toBe("List edited");
    expect(body).not.toHaveProperty("finderName");
    expect(body).not.toHaveProperty("relationshipName");
    expect(body).not.toHaveProperty("finderArguments");
  });

  it("sends empty relationshipName to clear after lock", async () => {
    await renderLoadedSlot();
    await lockSlotPanel();
    updateSlotDetail.mockResolvedValue({
      ...sampleDetail,
      relationshipName: "",
    });
    fireEvent.change(screen.getByTestId("developer-slot-relationship"), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(updateSlotDetail).toHaveBeenCalled();
    });
    expect(updateSlotDetail).toHaveBeenCalledWith(
      "rffList",
      expect.objectContaining({ relationshipName: "" }),
    );
    const body = updateSlotDetail.mock.calls.at(-1)?.[1] as Record<string, unknown>;
    expect(body).not.toHaveProperty("finderName");
  });

  it("surfaces unlocked PUT 409", async () => {
    await renderLoadedSlot();
    await lockSlotPanel();
    updateSlotDetail.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Slot is not locked by the current user" },
    });
    fireEvent.change(screen.getByTestId("developer-slot-finder"), {
      target: { value: "Java/global/percussion/slotcontentfinder/sys_RelationshipContentFinder" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
      DEV_MSG.SLOT_LOCK_REQUIRED,
    );
  });

  it("surfaces invalid finder 400 from loadFinder wording", async () => {
    await renderLoadedSlot();
    await lockSlotPanel();
    updateSlotDetail.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "extension name not valid for full name nope" },
    });
    fireEvent.change(screen.getByTestId("developer-slot-finder"), {
      target: { value: "nope" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
      DEV_MSG.SLOT_FINDER_INVALID,
    );
  });

  it("surfaces invalid relationship 400", async () => {
    await renderLoadedSlot();
    await lockSlotPanel();
    updateSlotDetail.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "Unknown relationship type: Missing Rel" },
    });
    fireEvent.change(screen.getByTestId("developer-slot-relationship"), {
      target: { value: "Missing Rel" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
      DEV_MSG.SLOT_RELATIONSHIP_INVALID,
    );
  });
});
