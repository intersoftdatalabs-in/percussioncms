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
  revision: 2,
  fields: [
    { name: "sys_title", value: "Home" },
    { name: "displaytitle", value: "Welcome" },
  ],
};

const savedFields: ItemEditorFields = {
  ...fields,
  revision: 5,
  fields: [
    { name: "sys_title", value: "Home" },
    { name: "displaytitle", value: "Updated" },
  ],
};

const reloadedFields: ItemEditorFields = {
  ...savedFields,
  revision: 7,
};

function renderHost({
  loadFields,
  saveFields,
  loadType,
}: {
  loadFields: (id: string) => Promise<ItemEditorFields>;
  saveFields?: (id: string, body: ItemEditorFields) => Promise<ItemEditorFields>;
  loadType?: () => Promise<{ fields: Array<{ name: string; label?: string; required?: boolean }> }>;
}) {
  render(
    <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
      <Routes>
        <Route
          path="/editor"
          element={
            <EditorHost
              checkout={vi.fn().mockResolvedValue({
                checkOutUser: "admin",
                currentUser: "admin",
              })}
              loadFields={loadFields}
              saveFields={saveFields ?? vi.fn().mockResolvedValue(savedFields)}
              loadType={loadType ?? (async () => ({ fields: [] }))}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

async function openForm(): Promise<void> {
  await waitFor(() => {
    expect(screen.getByTestId("editor-form")).toBeTruthy();
  });
}

async function editDisplayTitle(value: string): Promise<void> {
  fireEvent.change(screen.getByTestId("editor-field-displaytitle"), {
    target: { value },
  });
  fireEvent.click(screen.getByTestId("editor-save"));
}

describe("EditorHost save-success reload failure (#5768)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("keeps save truthful with a retry notice when the post-save reload fails, then clears on retry", async () => {
    const loadFields = vi
      .fn()
      .mockResolvedValueOnce(fields)
      .mockRejectedValueOnce({ status: 500, message: "boom" })
      .mockResolvedValue(reloadedFields);
    const saveFields = vi.fn().mockResolvedValue(savedFields);
    renderHost({ loadFields, saveFields });
    await openForm();
    await editDisplayTitle("Updated");
    await waitFor(() => {
      expect(screen.getByTestId("editor-save-reload-error")).toBeTruthy();
    });
    // The PUT succeeded: the saved indicator stays truthful and no PUT
    // failure is claimed on the save error region.
    expect(screen.getByTestId("editor-saved")).toBeTruthy();
    expect(screen.queryByTestId("editor-save-error")).toBeNull();
    expect(screen.getByTestId("editor-save-reload-error").textContent).toMatch(
      /could not be reloaded/i,
    );
    expect(screen.getByTestId("editor-retry-save-reload")).toBeTruthy();
    expect(saveFields).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByTestId("editor-retry-save-reload"));
    await waitFor(() => {
      expect(screen.queryByTestId("editor-save-reload-error")).toBeNull();
    });
    // Retry only reloads: the PUT is not re-sent and the refreshed payload
    // (revision 7) replaces the stale post-save state.
    expect(saveFields).toHaveBeenCalledTimes(1);
    expect(loadFields).toHaveBeenLastCalledWith("42");
    await waitFor(() => {
      expect(screen.getByTestId("editor-revision").textContent).toMatch(/Revision 7/);
    });
    expect(screen.getByTestId("editor-saved")).toBeTruthy();
  });

  it("a failed retry keeps the notice and the saved indicator", async () => {
    const loadFields = vi
      .fn()
      .mockResolvedValueOnce(fields)
      .mockRejectedValue({ status: 500, message: "boom" });
    const saveFields = vi.fn().mockResolvedValue(savedFields);
    renderHost({ loadFields, saveFields });
    await openForm();
    await editDisplayTitle("Updated");
    await waitFor(() => {
      expect(screen.getByTestId("editor-save-reload-error")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-retry-save-reload"));
    await waitFor(() => {
      expect(loadFields.mock.calls.length).toBeGreaterThanOrEqual(3);
    });
    expect(screen.getByTestId("editor-save-reload-error")).toBeTruthy();
    expect(screen.getByTestId("editor-saved")).toBeTruthy();
    expect(screen.queryByTestId("editor-form")).toBeTruthy();
    expect(saveFields).toHaveBeenCalledTimes(1);
  });

  it("a 400 PUT failure does not claim success and shows no reload notice", async () => {
    const loadFields = vi.fn().mockResolvedValue(fields);
    const saveFields = vi.fn().mockRejectedValue({ status: 400, message: "rejected" });
    renderHost({ loadFields, saveFields });
    await openForm();
    await editDisplayTitle("Updated");
    await waitFor(() => {
      expect(screen.getByTestId("editor-save-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-saved")).toBeNull();
    expect(screen.queryByTestId("editor-save-reload-error")).toBeNull();
    // No post-save reload is attempted when the PUT itself fails.
    expect(loadFields).toHaveBeenCalledTimes(1);
  });

  it("a blank required field still blocks the PUT without a reload notice", async () => {
    const loadFields = vi.fn().mockResolvedValue(fields);
    const saveFields = vi.fn();
    renderHost({
      loadFields,
      saveFields,
      loadType: async () => ({
        fields: [
          { name: "sys_title", label: "Title", required: true },
          { name: "displaytitle", label: "Display title" },
        ],
      }),
    });
    await openForm();
    fireEvent.change(screen.getByTestId("editor-field-sys_title"), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-summary")).toBeTruthy();
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(loadFields).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId("editor-save-reload-error")).toBeNull();
    expect(screen.queryByTestId("editor-saved")).toBeNull();
  });
});
