/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as systemDefApi from "../../../main/ts/api/developer/systemDefApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SystemDefPanel } from "../../../main/ts/developer/SystemDefPanel";

vi.mock("../../../main/ts/api/developer/systemDefApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/systemDefApi")
  >();
  return {
    ...actual,
    getSystemDef: vi.fn(),
    updateSystemDef: vi.fn(),
    addSystemDefField: vi.fn(),
    deleteSystemDefField: vi.fn(),
  };
});

const getMock = vi.mocked(systemDefApi.getSystemDef);
const updateMock = vi.mocked(systemDefApi.updateSystemDef);
const addMock = vi.mocked(systemDefApi.addSystemDefField);
const deleteMock = vi.mocked(systemDefApi.deleteSystemDefField);

const sampleDetail = {
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
};

describe("SystemDefPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getMock.mockReset();
    updateMock.mockReset();
    addMock.mockReset();
    deleteMock.mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders system field catalog when load succeeds", async () => {
    getMock.mockResolvedValue(sampleDetail);
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-fields-table")).toBeTruthy();
    });
    expect(screen.getByText("sys_title")).toBeTruthy();
    expect(screen.getByTestId("developer-sys-gaps")).toBeTruthy();
    expect(screen.getByTestId("developer-sys-add")).toBeTruthy();
    expect((screen.getByTestId("developer-sys-save") as HTMLButtonElement).disabled).toBe(true);
  });

  it("shows empty state when no fields", async () => {
    getMock.mockResolvedValue({ fieldCount: 0, fields: [], designGaps: [] });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getMock.mockRejectedValue(new SessionRedirectError());
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sys-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-sys-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getMock.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sys-error").textContent).toBe(
      `${DEV_MSG.SYS_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getMock.mockRejectedValue(new Error("network down"));
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sys-error").textContent).toBe(
      `${DEV_MSG.SYS_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-sys-fields-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getMock.mockRejectedValue("boom");
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sys-error").textContent).toBe(DEV_MSG.SYS_ERROR);
  });

  it("disables add until the field name is valid", async () => {
    getMock.mockResolvedValue(sampleDetail);
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-add-btn")).toBeTruthy();
    });
    const add = screen.getByTestId("developer-sys-add-btn") as HTMLButtonElement;
    expect(add.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-sys-new-name"), {
      target: { value: "1bad" },
    });
    expect(add.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-sys-new-name"), {
      target: { value: "qa_note" },
    });
    expect(add.disabled).toBe(false);
  });

  it("surfaces invalid name 400 on add", async () => {
    getMock.mockResolvedValue(sampleDetail);
    addMock.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "name must start with a letter" },
    });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-add-btn")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-sys-new-name"), {
      target: { value: "qa_note" },
    });
    fireEvent.click(screen.getByTestId("developer-sys-add-btn"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-write-error")).toBeTruthy();
    });
    expect(addMock).toHaveBeenCalled();
    expect(screen.getByTestId("developer-sys-write-error").textContent).toContain(
      DEV_MSG.SYS_INVALID_NAME,
    );
    expect(screen.getByTestId("developer-sys-write-error").textContent).toContain(
      "name must start with a letter",
    );
  });

  it("surfaces duplicate 409 on add", async () => {
    getMock.mockResolvedValue(sampleDetail);
    addMock.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "A field with that name already exists" },
    });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-add-btn")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-sys-new-name"), {
      target: { value: "sys_title" },
    });
    fireEvent.click(screen.getByTestId("developer-sys-add-btn"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-write-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sys-write-error").textContent).toContain(
      DEV_MSG.SYS_DUPLICATE,
    );
    expect(screen.getByTestId("developer-sys-write-error").textContent).toContain(
      "already exists",
    );
  });

  it("surfaces lock 409 on save", async () => {
    getMock.mockResolvedValue(sampleDetail);
    updateMock.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Could not save system definition; locked by Admin" },
    });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-searchable")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-sys-searchable"));
    const save = screen.getByTestId("developer-sys-save") as HTMLButtonElement;
    expect(save.disabled).toBe(false);
    fireEvent.click(save);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-write-error")).toBeTruthy();
    });
    expect(updateMock).toHaveBeenCalled();
    expect(screen.getByTestId("developer-sys-write-error").textContent).toContain(
      DEV_MSG.SYS_LOCK,
    );
  });

  it("saves dirty searchable patch then shows notice", async () => {
    getMock.mockResolvedValue(sampleDetail);
    updateMock.mockResolvedValue({
      ...sampleDetail,
      fields: [{ ...sampleDetail.fields[0], searchable: false }],
      designGaps: [],
    });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-searchable")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-sys-searchable"));
    fireEvent.click(screen.getByTestId("developer-sys-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-notice")).toBeTruthy();
    });
    expect(updateMock).toHaveBeenCalledWith({
      fields: [{ name: "sys_title", searchable: false, occurrence: "required" }],
    });
    expect(screen.getByTestId("developer-sys-notice").textContent).toBe(DEV_MSG.SYS_SAVED);
  });

  it("adds a field and lists it in the catalog", async () => {
    getMock.mockResolvedValue(sampleDetail);
    addMock.mockResolvedValue({
      fieldCount: 2,
      fields: [
        ...sampleDetail.fields,
        { name: "qa_note", dataType: "text", searchable: true, required: false },
      ],
      designGaps: [],
    });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-new-name")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-sys-new-name"), {
      target: { value: "qa_note" },
    });
    fireEvent.click(screen.getByTestId("developer-sys-add-btn"));
    await waitFor(() => {
      expect(screen.getByText("qa_note")).toBeTruthy();
    });
    expect(addMock).toHaveBeenCalledWith({
      name: "qa_note",
      dataType: "text",
      searchable: true,
      required: false,
    });
    expect(screen.getByTestId("developer-sys-notice").textContent).toBe(DEV_MSG.SYS_ADDED);
  });

  it("deletes a field after confirm", async () => {
    getMock.mockResolvedValue(sampleDetail);
    deleteMock.mockResolvedValue(undefined);
    getMock.mockResolvedValueOnce(sampleDetail).mockResolvedValueOnce({
      fieldCount: 0,
      fields: [],
      designGaps: [],
    });
    render(<SystemDefPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-sys-delete"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sys-empty")).toBeTruthy();
    });
    expect(deleteMock).toHaveBeenCalledWith("sys_title");
    expect(window.confirm).toHaveBeenCalled();
  });
});
