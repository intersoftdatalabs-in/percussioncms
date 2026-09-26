/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { MemoryRouter, Route, Routes } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { EditorHost } from "../../../main/ts/editor/EditorHost";
import type { ItemEditorFields } from "../../../main/ts/editor/itemFieldsApi";

const fields: ItemEditorFields = {
  contentId: "42",
  contentType: "percPage",
  name: "Home",
  checkoutUser: "admin",
  fields: [{ name: "sys_title", value: "Home" }],
};

const STORED = {
  itemId: "42",
  startDate: "09/18/2026 09:00 am",
  endDate: "09/19/2026 10:00 am",
  comments: "first",
};

function renderEdit(
  extra: Partial<React.ComponentProps<typeof EditorHost>> = {},
) {
  return render(
    <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
      <Routes>
        <Route
          path="/editor"
          element={
            <EditorHost
              checkout={vi.fn().mockResolvedValue(undefined)}
              loadFields={vi.fn().mockResolvedValue(fields)}
              loadType={async () => ({
                fields: [{ name: "sys_title", label: "Title", readOnly: false }],
              })}
              {...extra}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("EditorHost schedule publish dates (#4917)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("saves dates and shows them again on reopen", async () => {
    let stored = { ...STORED };
    const loadScheduleDates = vi.fn(async () => ({ ...stored }));
    const saveScheduleDates = vi.fn(async (dates) => {
      stored = { ...dates };
    });
    renderEdit({ loadScheduleDates, saveScheduleDates });
    await waitFor(() => {
      expect(screen.getByTestId("editor-schedule")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-schedule"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-schedule-start")).toHaveValue(
        "2026-09-18T09:00",
      );
    });
    fireEvent.change(screen.getByTestId("explorer-schedule-start"), {
      target: { value: "2026-09-20T09:00" },
    });
    fireEvent.change(screen.getByTestId("explorer-schedule-end"), {
      target: { value: "2026-09-21T10:00" },
    });
    fireEvent.click(screen.getByTestId("explorer-schedule-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-schedule-done")).toBeTruthy();
    });
    expect(saveScheduleDates).toHaveBeenCalledWith(
      expect.objectContaining({
        itemId: "42",
        startDate: "09/20/2026 09:00 am",
        endDate: "09/21/2026 10:00 am",
      }),
    );
    expect(screen.queryByTestId("explorer-schedule-dialog")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-schedule"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-schedule-start")).toHaveValue(
        "2026-09-20T09:00",
      );
    });
    expect(screen.getByTestId("explorer-schedule-end")).toHaveValue(
      "2026-09-21T10:00",
    );
  });

  it("cancel leaves previous dates and does not save", async () => {
    const saveScheduleDates = vi.fn();
    renderEdit({
      loadScheduleDates: vi.fn().mockResolvedValue({ ...STORED }),
      saveScheduleDates,
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-schedule")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-schedule"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-schedule-dialog")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("explorer-schedule-comments"), {
      target: { value: "do not persist" },
    });
    fireEvent.click(screen.getByTestId("explorer-schedule-cancel"));
    expect(saveScheduleDates).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-schedule-done")).toBeNull();
    expect(screen.queryByTestId("explorer-schedule-dialog")).toBeNull();
  });

  it("keeps invalid dates on the form and does not save", async () => {
    const saveScheduleDates = vi.fn();
    renderEdit({
      loadScheduleDates: vi.fn().mockResolvedValue({ ...STORED }),
      saveScheduleDates,
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-schedule")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-schedule"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-schedule-start")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("explorer-schedule-start"), {
      target: { value: "2026-09-22T09:00" },
    });
    fireEvent.change(screen.getByTestId("explorer-schedule-end"), {
      target: { value: "2026-09-18T09:00" },
    });
    fireEvent.click(screen.getByTestId("explorer-schedule-save"));
    expect(saveScheduleDates).not.toHaveBeenCalled();
    expect(screen.getByTestId("explorer-schedule-dialog")).toBeTruthy();
    expect(screen.getByTestId("explorer-schedule-dialog-error")).toBeTruthy();
    expect(screen.queryByTestId("editor-schedule-done")).toBeNull();
  });

  it("keeps the form open on HTTP 400 and 403", async () => {
    const saveScheduleDates = vi
      .fn()
      .mockRejectedValueOnce({ status: 400, statusText: "Bad Request", body: {} })
      .mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: {} });
    renderEdit({
      loadScheduleDates: vi.fn().mockResolvedValue({ ...STORED }),
      saveScheduleDates,
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-schedule")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-schedule"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-schedule-save")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("explorer-schedule-save"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-schedule-dialog-error").textContent).toMatch(
        /not valid/i,
      );
    });
    expect(screen.queryByTestId("editor-schedule-done")).toBeNull();
    fireEvent.click(screen.getByTestId("explorer-schedule-save"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-schedule-dialog-error").textContent).toMatch(
        /not allowed/i,
      );
    });
    expect(screen.getByTestId("explorer-schedule-dialog")).toBeTruthy();
    expect(screen.queryByTestId("editor-schedule-done")).toBeNull();
  });

  it("hides Schedule in view mode", async () => {
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => ({
                  fields: [{ name: "sys_title", label: "Title" }],
                })}
                loadScheduleDates={vi.fn()}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-host")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-schedule")).toBeNull();
  });
});
