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

describe("EditorHost clear publish schedule (#5123)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("clears both dates and shows them empty only after a successful refresh", async () => {
    let stored = { ...STORED };
    const loadScheduleDates = vi.fn(async () => ({ ...stored }));
    const saveScheduleDates = vi.fn(async (dates) => {
      stored = { ...dates };
    });
    renderEdit({ loadScheduleDates, saveScheduleDates });
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-schedule")).toBeTruthy();
    fireEvent.click(screen.getByTestId("editor-clear-schedule"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule-current").textContent).toMatch(
        /09\/18\/2026 09:00 am/,
      );
    });
    expect(screen.getByTestId("editor-clear-schedule-current").textContent).toMatch(
      /09\/19\/2026 10:00 am/,
    );
    fireEvent.click(screen.getByTestId("editor-clear-schedule-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule-done")).toBeTruthy();
    });
    expect(saveScheduleDates).toHaveBeenCalledWith({
      itemId: "42",
      startDate: "",
      endDate: "",
      comments: "",
    });
    expect(screen.queryByTestId("editor-clear-schedule-dialog")).toBeNull();
    expect(screen.queryByTestId("editor-schedule-done")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-schedule"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-schedule-start")).toHaveValue("");
    });
    expect(screen.getByTestId("explorer-schedule-end")).toHaveValue("");
    fireEvent.click(screen.getByTestId("explorer-schedule-cancel"));
    fireEvent.click(screen.getByTestId("editor-clear-schedule"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule-current").textContent).toMatch(
        /no scheduled dates/i,
      );
    });
  });

  it("cancel does not call the server", async () => {
    const saveScheduleDates = vi.fn();
    const loadScheduleDates = vi.fn(async () => ({ ...STORED }));
    renderEdit({ loadScheduleDates, saveScheduleDates });
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-clear-schedule"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule-dialog")).toBeTruthy();
    });
    expect(loadScheduleDates).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByTestId("editor-clear-schedule-cancel"));
    expect(saveScheduleDates).not.toHaveBeenCalled();
    expect(loadScheduleDates).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId("editor-clear-schedule-done")).toBeNull();
    expect(screen.queryByTestId("editor-clear-schedule-dialog")).toBeNull();
  });

  it("does not claim the schedule was cleared for HTTP 400, 403, or 409", async () => {
    const saveScheduleDates = vi
      .fn()
      .mockRejectedValueOnce({ status: 400, statusText: "Bad Request", body: {} })
      .mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: {} })
      .mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: {} });
    renderEdit({
      loadScheduleDates: vi.fn().mockResolvedValue({ ...STORED }),
      saveScheduleDates,
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-clear-schedule"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule-confirm")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-clear-schedule-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule-error").textContent).toMatch(
        /could not be cleared/i,
      );
    });
    expect(screen.getByTestId("editor-clear-schedule-current").textContent).toMatch(
      /09\/18\/2026 09:00 am/,
    );
    expect(screen.queryByTestId("editor-clear-schedule-done")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-clear-schedule-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule-error").textContent).toMatch(
        /not allowed to clear/i,
      );
    });
    fireEvent.click(screen.getByTestId("editor-clear-schedule-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule-error").textContent).toMatch(
        /was not cleared/i,
      );
    });
    expect(screen.getByTestId("editor-clear-schedule-dialog")).toBeTruthy();
    expect(screen.getByTestId("editor-clear-schedule-current").textContent).toMatch(
      /09\/19\/2026 10:00 am/,
    );
    expect(screen.queryByTestId("editor-clear-schedule-done")).toBeNull();
    expect(saveScheduleDates).toHaveBeenCalledTimes(3);
  });

  it("does not claim success when the refresh still has dates", async () => {
    const saveScheduleDates = vi.fn(async () => undefined);
    renderEdit({
      loadScheduleDates: vi.fn().mockResolvedValue({ ...STORED }),
      saveScheduleDates,
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-clear-schedule"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule-confirm")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-clear-schedule-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-clear-schedule-error").textContent).toMatch(
        /still has dates/i,
      );
    });
    expect(screen.getByTestId("editor-clear-schedule-dialog")).toBeTruthy();
    expect(screen.getByTestId("editor-clear-schedule-current").textContent).toMatch(
      /09\/18\/2026 09:00 am/,
    );
    expect(screen.queryByTestId("editor-clear-schedule-done")).toBeNull();
    expect(saveScheduleDates).toHaveBeenCalledTimes(1);
  });

  it("hides Clear schedule in view mode", async () => {
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
    expect(screen.queryByTestId("editor-clear-schedule")).toBeNull();
    expect(screen.queryByTestId("editor-schedule")).toBeNull();
  });
});
