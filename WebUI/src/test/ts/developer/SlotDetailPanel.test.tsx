/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SLOT_DESC_MSG } from "../../../main/ts/developer/slotDescriptionMessages";
import { SLOT_LABEL_MSG } from "../../../main/ts/developer/slotLabelMessages";
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
    const onDeleted = vi.fn();
    render(
      <SlotDetailPanel idOrName="rffList" onBack={() => undefined} onDeleted={onDeleted} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-slot-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(onDeleted).toHaveBeenCalled();
    });
    expect(deleteSlot).toHaveBeenCalledWith("rffList");
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
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(deleteSlot).toHaveBeenCalledWith("sys_inline_link");
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
      DEV_MSG.SLOT_DELETE_SYSTEM,
    );
    expect(onDeleted).not.toHaveBeenCalled();
  });

  it("does not treat a generic 409 containing system as a system-slot delete", async () => {
    getSlotDetail.mockResolvedValue(sampleDetail);
    deleteSlot.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "ecosystem constraint" },
    });
    const onDeleted = vi.fn();
    render(
      <SlotDetailPanel idOrName="rffList" onBack={() => undefined} onDeleted={onDeleted} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-slot-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
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
  });

  it("surfaces 403 non-Admin on delete", async () => {
    getSlotDetail.mockResolvedValue(sampleDetail);
    deleteSlot.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<SlotDetailPanel idOrName="rffList" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-slot-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
      DEV_MSG.SLOT_FORBIDDEN,
    );
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
    expect(body).not.toHaveProperty("associations");
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

  it("renders association names instead of GUID-only rows (#4462)", async () => {
    getSlotDetail.mockResolvedValue({
      ...sampleDetail,
      associations: [
        {
          contentTypeName: "percPage",
          contentTypeLabel: "Page",
          contentTypeGuid: { stringValue: "0-2-301" },
          templateName: "perc.page",
          templateLabel: "Page",
          templateGuid: { stringValue: "0-10-1" },
        },
      ],
    });
    render(<SlotDetailPanel idOrName="rffList" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-assoc-ct-name-0")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-assoc-ct-name-0").textContent).toBe("percPage");
    expect(screen.getByTestId("developer-slot-assoc-tpl-name-0").textContent).toBe("perc.page");
    expect(
      (screen.getByTestId("developer-slot-assoc-ct") as HTMLInputElement).disabled,
    ).toBe(true);
    expect(
      (screen.getByTestId("developer-slot-assoc-remove-0") as HTMLButtonElement).disabled,
    ).toBe(true);
  });

  it("lock then add association by name PUTs names (#4462)", async () => {
    await renderLoadedSlot();
    await lockSlotPanel();
    updateSlotDetail.mockResolvedValue({
      ...sampleDetail,
      associations: [
        {
          contentTypeName: "percPage",
          templateName: "perc.page",
          contentTypeGuid: { stringValue: "0-2-301" },
          templateGuid: { stringValue: "0-10-1" },
        },
      ],
    });
    fireEvent.change(screen.getByTestId("developer-slot-assoc-ct"), {
      target: { value: "percPage" },
    });
    fireEvent.change(screen.getByTestId("developer-slot-assoc-tpl"), {
      target: { value: "perc.page" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-assoc-add"));
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(updateSlotDetail).toHaveBeenCalled();
    });
    const body = updateSlotDetail.mock.calls.at(-1)?.[1] as {
      associations?: { contentTypeName?: string; templateName?: string }[];
    };
    expect(body.associations).toEqual([
      { contentTypeName: "percPage", templateName: "perc.page" },
    ]);
    expect(screen.getByTestId("developer-slot-assoc-ct-name-0").textContent).toBe("percPage");
  });

  it("surfaces unknown association pair 400 (#4462)", async () => {
    await renderLoadedSlot();
    await lockSlotPanel();
    updateSlotDetail.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "association[0].contentType not found: nope" },
    });
    fireEvent.change(screen.getByTestId("developer-slot-assoc-ct"), {
      target: { value: "nope" },
    });
    fireEvent.change(screen.getByTestId("developer-slot-assoc-tpl"), {
      target: { value: "perc.page" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-assoc-add"));
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
      DEV_MSG.SLOT_ASSOC_UNKNOWN,
    );
  });

  it("renames a user slot only after a successful save (#5043)", async () => {
    await renderLoadedSlot();
    expect(
      (screen.getByTestId("developer-slot-name") as HTMLInputElement).disabled,
    ).toBe(false);
    expect(screen.getByTestId("developer-slot-committed-name").textContent).toContain(
      "rffList",
    );
    updateSlotDetail.mockResolvedValue({ ...sampleDetail, name: "qaRenamed" });
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "qaRenamed" },
    });
    expect(screen.getByTestId("developer-slot-committed-name").textContent).toContain(
      "rffList",
    );
    fireEvent.click(screen.getByTestId("developer-slot-save"));
    await waitFor(() => {
      expect(updateSlotDetail).toHaveBeenCalledWith(
        "rffList",
        expect.objectContaining({ name: "qaRenamed" }),
      );
    });
    expect(screen.getByTestId("developer-slot-committed-name").textContent).toContain(
      "qaRenamed",
    );
    expect((screen.getByTestId("developer-slot-name") as HTMLInputElement).value).toBe(
      "qaRenamed",
    );
    expect(screen.getByTestId("developer-slot-detail-notice").textContent).toBe(
      DEV_MSG.SLOT_SAVED,
    );
  });

  it("cancel reverts a draft name and does not PUT (#5043)", async () => {
    const onBack = vi.fn();
    const onSaved = vi.fn();
    getSlotDetail.mockResolvedValue(sampleDetail);
    render(
      <SlotDetailPanel idOrName="rffList" onBack={onBack} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-name")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "qaRenamed" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-cancel"));
    expect(updateSlotDetail).not.toHaveBeenCalled();
    expect(onSaved).not.toHaveBeenCalled();
    expect(onBack).toHaveBeenCalled();
  });

  it("does not PUT a blank or wildcard rename (#5043)", async () => {
    await renderLoadedSlot();
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "   " },
    });
    expect(
      (screen.getByTestId("developer-slot-save") as HTMLButtonElement).disabled,
    ).toBe(true);
    fireEvent.change(screen.getByTestId("developer-slot-name"), {
      target: { value: "qa*slot" },
    });
    expect(
      (screen.getByTestId("developer-slot-save") as HTMLButtonElement).disabled,
    ).toBe(true);
    expect(updateSlotDetail).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-slot-committed-name").textContent).toContain(
      "rffList",
    );
  });

  it("does not claim success on rename 409, 403, or 400 (#5043)", async () => {
    await renderLoadedSlot();
    const onSaved = vi.fn();
    const cases = [
      { status: 409, message: "Slot already exists: taken", text: DEV_MSG.SLOT_DUPLICATE },
      { status: 403, message: "Admin role required", text: DEV_MSG.SLOT_FORBIDDEN },
      { status: 400, message: "name cannot contain whitespace", text: DEV_MSG.SLOT_NAME_INVALID },
      {
        status: 409,
        message: "System slots cannot be renamed: sys_inline",
        text: DEV_MSG.SLOT_RENAME_SYSTEM,
      },
    ];
    for (const c of cases) {
      updateSlotDetail.mockRejectedValue({
        status: c.status,
        statusText: "err",
        body: { message: c.message },
      });
      fireEvent.change(screen.getByTestId("developer-slot-name"), {
        target: { value: "qaRenamed" },
      });
      fireEvent.click(screen.getByTestId("developer-slot-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-slot-detail-error").textContent).toContain(
          c.text,
        );
      });
      expect(screen.queryByTestId("developer-slot-detail-notice")).toBeNull();
      expect(screen.getByTestId("developer-slot-committed-name").textContent).toContain(
        "rffList",
      );
    }
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("keeps system slot names read-only (#5043)", async () => {
    getSlotDetail.mockResolvedValue({
      ...sampleDetail,
      name: "sys_inline_link",
      systemSlot: true,
    });
    render(<SlotDetailPanel idOrName="sys_inline_link" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-name")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("developer-slot-name") as HTMLInputElement).disabled,
    ).toBe(true);
    expect(screen.getByText(DEV_MSG.SLOT_NAME_READONLY)).toBeTruthy();
  });

  it("does not offer set-description chrome before a slot exists", () => {
    render(<SlotDetailPanel idOrName={null} onBack={() => undefined} />);
    expect(screen.queryByTestId("developer-slot-set-description")).toBeNull();
    expect(screen.queryByTestId("developer-slot-set-label")).toBeNull();
  });

  it("sets the description only after success and leaves name, label, type, and finder", async () => {
    const userDetail = {
      ...sampleDetail,
      name: "qa5408slot",
      label: "QA slot",
      description: "Folder list",
      slotType: "INLINE",
      finderName: "sys_RelationshipContentFinder",
      relationshipName: "ActiveAssembly",
      finderArguments: { type: "qa5408" },
    };
    getSlotDetail.mockResolvedValue(userDetail);
    updateSlotDetail.mockResolvedValue({ ...userDetail, description: "note" });
    const onSaved = vi.fn();
    render(
      <SlotDetailPanel idOrName="qa5408slot" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("developer-slot-set-description-text").getAttribute("data-slot-description"),
      ).toBe("Folder list");
    });
    fireEvent.click(screen.getByTestId("developer-slot-set-description-edit"));
    fireEvent.click(screen.getByTestId("developer-slot-set-description-save"));
    expect(updateSlotDetail).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-slot-set-description-editor")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-slot-set-description-edit"));
    fireEvent.change(screen.getByTestId("developer-slot-set-description-input"), {
      target: { value: " note " },
    });
    expect(
      screen.getByTestId("developer-slot-set-description-text").getAttribute("data-slot-description"),
    ).toBe("Folder list");
    fireEvent.click(screen.getByTestId("developer-slot-set-description-cancel"));
    expect(updateSlotDetail).not.toHaveBeenCalled();
    expect(
      screen.getByTestId("developer-slot-set-description-text").getAttribute("data-slot-description"),
    ).toBe("Folder list");

    fireEvent.click(screen.getByTestId("developer-slot-set-description-edit"));
    fireEvent.change(screen.getByTestId("developer-slot-set-description-input"), {
      target: { value: " note " },
    });
    fireEvent.click(screen.getByTestId("developer-slot-set-description-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateSlotDetail).toHaveBeenCalledWith("qa5408slot", { description: "note" });
    expect(
      screen.getByTestId("developer-slot-set-description-text").getAttribute("data-slot-description"),
    ).toBe("note");
    expect(screen.getByTestId("developer-slot-set-description-notice").textContent).toBe(
      SLOT_DESC_MSG.SAVED,
    );
    expect((screen.getByTestId("developer-slot-name") as HTMLInputElement).value).toBe("qa5408slot");
    expect((screen.getByTestId("developer-slot-label") as HTMLInputElement).value).toBe("QA slot");
    expect(screen.getByTestId("developer-slot-type-value").textContent).toBe("INLINE");
    expect((screen.getByTestId("developer-slot-finder") as HTMLInputElement).value).toBe(
      "sys_RelationshipContentFinder",
    );
    expect((screen.getByTestId("developer-slot-relationship") as HTMLInputElement).value).toBe(
      "ActiveAssembly",
    );
  });

  it("clears a blank description and keeps the previous description on 400, 403, and 409", async () => {
    const userDetail = {
      ...sampleDetail,
      name: "qa5408slot",
      label: "QA slot",
      description: "Folder list",
      slotType: "INLINE",
      finderName: "sys_RelationshipContentFinder",
      relationshipName: "ActiveAssembly",
      finderArguments: { type: "qa5408" },
    };
    getSlotDetail.mockResolvedValue(userDetail);
    updateSlotDetail.mockResolvedValue({ ...userDetail, description: "" });
    render(<SlotDetailPanel idOrName="qa5408slot" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-set-description-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-slot-set-description-edit"));
    fireEvent.change(screen.getByTestId("developer-slot-set-description-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-slot-set-description-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-set-description-notice").textContent).toBe(
        SLOT_DESC_MSG.CLEARED,
      );
    });
    expect(updateSlotDetail).toHaveBeenCalledWith("qa5408slot", { description: "" });
    expect(
      screen.getByTestId("developer-slot-set-description-text").getAttribute("data-slot-description"),
    ).toBe("");

    for (const status of [400, 403, 409]) {
      updateSlotDetail.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message: `forced ${status}` },
      });
      fireEvent.click(screen.getByTestId("developer-slot-set-description-edit"));
      fireEvent.change(screen.getByTestId("developer-slot-set-description-input"), {
        target: { value: "later" },
      });
      fireEvent.click(screen.getByTestId("developer-slot-set-description-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-slot-set-description-error").textContent).toContain(
          `forced ${status}`,
        );
      });
      expect(
        screen.getByTestId("developer-slot-set-description-text").getAttribute("data-slot-description"),
      ).toBe("");
      expect((screen.getByTestId("developer-slot-description") as HTMLInputElement).value).toBe("");
      expect((screen.getByTestId("developer-slot-name") as HTMLInputElement).value).toBe("qa5408slot");
      expect(screen.queryByTestId("developer-slot-set-description-notice")).toBeNull();
      fireEvent.click(screen.getByTestId("developer-slot-set-description-cancel"));
    }
  });

  it("does not show a description when the update changes the finder", async () => {
    const userDetail = {
      ...sampleDetail,
      name: "qa5408slot",
      label: "QA slot",
      description: "Folder list",
      slotType: "INLINE",
      finderName: "sys_RelationshipContentFinder",
      relationshipName: "ActiveAssembly",
      finderArguments: { type: "qa5408" },
    };
    getSlotDetail.mockResolvedValue(userDetail);
    updateSlotDetail.mockResolvedValue({
      ...userDetail,
      description: "note",
      finderName: "otherFinder",
    });
    render(<SlotDetailPanel idOrName="qa5408slot" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-set-description-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-slot-set-description-edit"));
    fireEvent.change(screen.getByTestId("developer-slot-set-description-input"), {
      target: { value: "note" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-set-description-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-set-description-error").textContent).toContain(
        SLOT_DESC_MSG.ERROR,
      );
    });
    expect(
      screen.getByTestId("developer-slot-set-description-text").getAttribute("data-slot-description"),
    ).toBe("Folder list");
    expect((screen.getByTestId("developer-slot-finder") as HTMLInputElement).value).toBe(
      "sys_RelationshipContentFinder",
    );
    expect(screen.queryByTestId("developer-slot-set-description-notice")).toBeNull();
  });

  it("sets the label only after success and leaves name, description, type, and finder", async () => {
    const userDetail = {
      ...sampleDetail,
      name: "qa5431slot",
      label: "QA slot",
      description: "Folder list",
      slotType: "INLINE",
      finderName: "sys_RelationshipContentFinder",
      relationshipName: "ActiveAssembly",
      finderArguments: { type: "qa5431" },
    };
    getSlotDetail.mockResolvedValue(userDetail);
    updateSlotDetail.mockResolvedValue({ ...userDetail, label: "note" });
    const onSaved = vi.fn();
    render(
      <SlotDetailPanel idOrName="qa5431slot" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("developer-slot-set-label-text").getAttribute("data-slot-label"),
      ).toBe("QA slot");
    });
    fireEvent.click(screen.getByTestId("developer-slot-set-label-edit"));
    fireEvent.click(screen.getByTestId("developer-slot-set-label-save"));
    expect(updateSlotDetail).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-slot-set-label-editor")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-slot-set-label-edit"));
    fireEvent.change(screen.getByTestId("developer-slot-set-label-input"), {
      target: { value: " note " },
    });
    expect(screen.getByTestId("developer-slot-set-label-text").getAttribute("data-slot-label")).toBe(
      "QA slot",
    );
    fireEvent.click(screen.getByTestId("developer-slot-set-label-cancel"));
    expect(updateSlotDetail).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-slot-set-label-text").getAttribute("data-slot-label")).toBe(
      "QA slot",
    );

    fireEvent.click(screen.getByTestId("developer-slot-set-label-edit"));
    fireEvent.change(screen.getByTestId("developer-slot-set-label-input"), {
      target: { value: " note " },
    });
    fireEvent.click(screen.getByTestId("developer-slot-set-label-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateSlotDetail).toHaveBeenCalledWith("qa5431slot", { label: "note" });
    expect(screen.getByTestId("developer-slot-set-label-text").getAttribute("data-slot-label")).toBe(
      "note",
    );
    expect(screen.getByTestId("developer-slot-set-label-notice").textContent).toBe(
      SLOT_LABEL_MSG.SAVED,
    );
    expect((screen.getByTestId("developer-slot-name") as HTMLInputElement).value).toBe("qa5431slot");
    expect((screen.getByTestId("developer-slot-label") as HTMLInputElement).value).toBe("note");
    expect((screen.getByTestId("developer-slot-description") as HTMLInputElement).value).toBe(
      "Folder list",
    );
    expect(screen.getByTestId("developer-slot-type-value").textContent).toBe("INLINE");
    expect((screen.getByTestId("developer-slot-finder") as HTMLInputElement).value).toBe(
      "sys_RelationshipContentFinder",
    );
    expect((screen.getByTestId("developer-slot-relationship") as HTMLInputElement).value).toBe(
      "ActiveAssembly",
    );
  });

  it("keeps the name when a blank label echoes it, and 400, 403, and 409 are not success", async () => {
    const userDetail = {
      ...sampleDetail,
      name: "qa5431slot",
      label: "QA slot",
      description: "Folder list",
      slotType: "INLINE",
      finderName: "sys_RelationshipContentFinder",
      relationshipName: "ActiveAssembly",
      finderArguments: { type: "qa5431" },
    };
    getSlotDetail.mockResolvedValue(userDetail);
    updateSlotDetail.mockResolvedValue({ ...userDetail, label: "qa5431slot" });
    render(<SlotDetailPanel idOrName="qa5431slot" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-set-label-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-slot-set-label-edit"));
    fireEvent.change(screen.getByTestId("developer-slot-set-label-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-slot-set-label-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-set-label-notice").textContent).toBe(
        SLOT_LABEL_MSG.CLEARED,
      );
    });
    expect(updateSlotDetail).toHaveBeenCalledWith("qa5431slot", { label: "" });
    expect(screen.getByTestId("developer-slot-set-label-text").getAttribute("data-slot-label")).toBe(
      "qa5431slot",
    );
    expect((screen.getByTestId("developer-slot-name") as HTMLInputElement).value).toBe("qa5431slot");
    expect((screen.getByTestId("developer-slot-label") as HTMLInputElement).value).toBe("qa5431slot");
    expect((screen.getByTestId("developer-slot-description") as HTMLInputElement).value).toBe(
      "Folder list",
    );
    expect((screen.getByTestId("developer-slot-finder") as HTMLInputElement).value).toBe(
      "sys_RelationshipContentFinder",
    );

    for (const status of [400, 403, 409]) {
      updateSlotDetail.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message: `forced ${status}` },
      });
      fireEvent.click(screen.getByTestId("developer-slot-set-label-edit"));
      fireEvent.change(screen.getByTestId("developer-slot-set-label-input"), {
        target: { value: "later" },
      });
      fireEvent.click(screen.getByTestId("developer-slot-set-label-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-slot-set-label-error").textContent).toContain(
          `forced ${status}`,
        );
      });
      expect(screen.getByTestId("developer-slot-set-label-text").getAttribute("data-slot-label")).toBe(
        "qa5431slot",
      );
      expect((screen.getByTestId("developer-slot-name") as HTMLInputElement).value).toBe(
        "qa5431slot",
      );
      expect(screen.queryByTestId("developer-slot-set-label-notice")).toBeNull();
      fireEvent.click(screen.getByTestId("developer-slot-set-label-cancel"));
    }
  });

  it("does not show a label when the update changes the name or description", async () => {
    const userDetail = {
      ...sampleDetail,
      name: "qa5431slot",
      label: "QA slot",
      description: "Folder list",
      slotType: "INLINE",
      finderName: "sys_RelationshipContentFinder",
      relationshipName: "ActiveAssembly",
      finderArguments: { type: "qa5431" },
    };
    getSlotDetail.mockResolvedValue(userDetail);
    updateSlotDetail.mockResolvedValue({
      ...userDetail,
      label: "",
      name: "other",
      description: "changed",
    });
    render(<SlotDetailPanel idOrName="qa5431slot" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-set-label-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-slot-set-label-edit"));
    fireEvent.change(screen.getByTestId("developer-slot-set-label-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-slot-set-label-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-set-label-error").textContent).toContain(
        SLOT_LABEL_MSG.ERROR,
      );
    });
    expect(screen.getByTestId("developer-slot-set-label-text").getAttribute("data-slot-label")).toBe(
      "QA slot",
    );
    expect((screen.getByTestId("developer-slot-name") as HTMLInputElement).value).toBe("qa5431slot");
    expect((screen.getByTestId("developer-slot-description") as HTMLInputElement).value).toBe(
      "Folder list",
    );
    expect(screen.queryByTestId("developer-slot-set-label-notice")).toBeNull();
  });
});
