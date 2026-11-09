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
  comments: "",
};

function renderHost({
  mode = "edit",
  extra = {},
}: {
  mode?: string;
  extra?: Partial<React.ComponentProps<typeof EditorHost>>;
}) {
  render(
    <MemoryRouter initialEntries={[`/editor?contentId=42&mode=${mode}`]}>
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

describe("EditorHost scheduled transitions panel (#5830)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("opens the panel from the toggle and lists embargo and expiry", async () => {
    const loadScheduleDates = vi.fn(async () => ({ ...STORED }));
    renderHost({ extra: { loadScheduleDates } });
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-scheduled-transitions-toggle"),
      ).toBeTruthy();
    });
    // Collapsed by default: no dates fetch fires and no panel renders.
    expect(loadScheduleDates).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-scheduled-transitions")).toBeNull();
    fireEvent.click(
      screen.getByTestId("editor-scheduled-transitions-toggle"),
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-scheduled-transitions-list"),
      ).toBeTruthy();
    });
    expect(loadScheduleDates).toHaveBeenCalledWith("42");
    expect(
      screen.getByTestId("editor-scheduled-transition-embargo-date")
        .textContent,
    ).toBe("09/18/2026 09:00 am");
    expect(
      screen.getByTestId("editor-scheduled-transition-expiry-date")
        .textContent,
    ).toBe("09/19/2026 10:00 am");
    // Toggling again hides the panel without refetching.
    fireEvent.click(
      screen.getByTestId("editor-scheduled-transitions-toggle"),
    );
    await waitFor(() => {
      expect(screen.queryByTestId("editor-scheduled-transitions")).toBeNull();
    });
    expect(loadScheduleDates).toHaveBeenCalledTimes(1);
  });

  it("shows an empty state when no dates are scheduled", async () => {
    renderHost({
      extra: {
        loadScheduleDates: vi
          .fn()
          .mockResolvedValue({ itemId: "42", startDate: "", endDate: "" }),
      },
    });
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-scheduled-transitions-toggle"),
      ).toBeTruthy();
    });
    fireEvent.click(
      screen.getByTestId("editor-scheduled-transitions-toggle"),
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-scheduled-transitions-empty"),
      ).toBeTruthy();
    });
    expect(
      screen.queryByTestId("editor-scheduled-transitions-list"),
    ).toBeNull();
  });

  it("a dates load failure shows an error, not an empty panel", async () => {
    renderHost({
      extra: {
        loadScheduleDates: vi
          .fn()
          .mockRejectedValue({ status: 403, message: "forbidden" }),
      },
    });
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-scheduled-transitions-toggle"),
      ).toBeTruthy();
    });
    fireEvent.click(
      screen.getByTestId("editor-scheduled-transitions-toggle"),
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-scheduled-transitions-load-error"),
      ).toBeTruthy();
    });
    expect(
      screen.getByTestId("editor-scheduled-transitions-load-error")
        .textContent,
    ).toMatch(/could not load the scheduled transitions/i);
    expect(
      screen.queryByTestId("editor-scheduled-transitions-empty"),
    ).toBeNull();
  });

  it("Set opens the schedule dialog and a save refreshes the panel", async () => {
    let stored = { ...STORED };
    const loadScheduleDates = vi.fn(async () => ({ ...stored }));
    const saveScheduleDates = vi.fn(async (dates: typeof STORED) => {
      stored = { ...dates };
    });
    renderHost({ extra: { loadScheduleDates, saveScheduleDates } });
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-scheduled-transitions-toggle"),
      ).toBeTruthy();
    });
    fireEvent.click(
      screen.getByTestId("editor-scheduled-transitions-toggle"),
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-scheduled-transitions-set"),
      ).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-scheduled-transitions-set"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-schedule-dialog")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("explorer-schedule-start"), {
      target: { value: "2026-09-20T09:00" },
    });
    fireEvent.change(screen.getByTestId("explorer-schedule-end"), {
      target: { value: "2026-09-21T10:00" },
    });
    fireEvent.click(screen.getByTestId("explorer-schedule-save"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-scheduled-transition-embargo-date")
          .textContent,
      ).toBe("09/20/2026 09:00 am");
    });
    expect(saveScheduleDates).toHaveBeenCalledWith(
      expect.objectContaining({ itemId: "42" }),
    );
  });

  it("shows the toggle in view mode without Set or Clear actions", async () => {
    renderHost({
      mode: "view",
      extra: { loadScheduleDates: vi.fn(async () => ({ ...STORED })) },
    });
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-scheduled-transitions-toggle"),
      ).toBeTruthy();
    });
    fireEvent.click(
      screen.getByTestId("editor-scheduled-transitions-toggle"),
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-scheduled-transitions-list"),
      ).toBeTruthy();
    });
    expect(
      screen.queryByTestId("editor-scheduled-transitions-actions"),
    ).toBeNull();
  });

  it("hides the toggle in promote mode", async () => {
    renderHost({
      mode: "promote",
      extra: { loadScheduleDates: vi.fn(async () => ({ ...STORED })) },
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-promote-form")).toBeTruthy();
    });
    expect(
      screen.queryByTestId("editor-scheduled-transitions-toggle"),
    ).toBeNull();
  });
});
