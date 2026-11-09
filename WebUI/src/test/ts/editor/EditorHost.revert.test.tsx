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

import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
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
  revision: 1,
  fields: [{ name: "sys_title", value: "Home" }],
};

function renderEdit(
  extra: Partial<React.ComponentProps<typeof EditorHost>> = {},
  entry = "/editor?contentId=42&mode=edit",
) {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route
          path="/editor"
          element={
            <EditorHost
              checkout={vi.fn().mockResolvedValue(undefined)}
              loadFields={vi.fn().mockResolvedValue(fields)}
              loadType={vi.fn().mockResolvedValue({
                fields: [{ name: "sys_title", label: "Title", readOnly: false }],
              })}
              loadTransitions={vi.fn().mockResolvedValue({ transitionTriggers: [] })}
              {...extra}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

async function dirtyTitle(): Promise<void> {
  await waitFor(() => {
    expect(screen.getByTestId("editor-field-sys_title")).toBeTruthy();
  });
  fireEvent.change(screen.getByTestId("editor-field-sys_title"), {
    target: { value: "Draft title" },
  });
}

describe("EditorHost bulk revert unsaved edits (#5889)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("is hidden while the form is clean", async () => {
    renderEdit({ saveFields: vi.fn() });
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-sys_title")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-revert")).toBeNull();
    expect(
      screen
        .getByTestId("editor-field-row-sys_title")
        .getAttribute("data-editor-dirty"),
    ).toBeNull();
  });

  it("restores the loaded value, clears dirty state, and never PUTs", async () => {
    const saveFields = vi.fn();
    renderEdit({ saveFields });
    await dirtyTitle();
    await waitFor(() => {
      expect(screen.getByTestId("editor-revert")).toBeTruthy();
    });
    expect(
      screen
        .getByTestId("editor-field-row-sys_title")
        .getAttribute("data-editor-dirty"),
    ).toBe("true");
    fireEvent.click(screen.getByTestId("editor-revert"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("editor-field-sys_title") as HTMLInputElement).value,
      ).toBe("Home");
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-revert")).toBeNull();
    expect(
      screen
        .getByTestId("editor-field-row-sys_title")
        .getAttribute("data-editor-dirty"),
    ).toBeNull();
    expect(screen.queryByTestId("editor-saved")).toBeNull();
  });

  it("is hidden in view mode even with unsaved draft state", async () => {
    renderEdit(
      { saveFields: vi.fn() },
      "/editor?contentId=42&mode=view",
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-host")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-revert")).toBeNull();
  });

  it("revert drops a pending file so a later save does not upload it", async () => {
    const saveFields = vi.fn().mockImplementation(async () => fields);
    const uploadBinary = vi.fn().mockResolvedValue(undefined);
    renderEdit({
      saveFields,
      uploadBinary,
      loadType: vi.fn().mockResolvedValue({
        fields: [
          { name: "sys_title", label: "Title" },
          { name: "item_file_attachment", label: "File", control: "sys_file" },
        ],
      }),
      loadBinaryMeta: vi.fn().mockResolvedValue({ present: false, filename: "" }),
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-file-item_file_attachment")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-file-item_file_attachment"), {
      target: {
        files: [new File(["bytes"], "notes.txt", { type: "text/plain" })],
      },
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-revert")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-revert"));
    await waitFor(() => {
      expect(screen.queryByTestId("editor-revert")).toBeNull();
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-saved")).toBeTruthy();
    });
    expect(uploadBinary).not.toHaveBeenCalled();
  });

  it("revert clears the stale field error from a failed required save", async () => {
    const saveFields = vi.fn();
    renderEdit({
      saveFields,
      loadType: vi.fn().mockResolvedValue({
        fields: [
          {
            name: "sys_title",
            label: "Title",
            control: "sys_EditBox",
            dataType: "text",
            required: true,
          },
        ],
      }),
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-sys_title")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-field-sys_title"), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(
        within(screen.getByTestId("editor-field-row-sys_title")).getByTestId(
          "editor-field-error-sys_title",
        ).textContent,
      ).toMatch(/required/i);
    });
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("editor-revert"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("editor-field-sys_title") as HTMLInputElement).value,
      ).toBe("Home");
    });
    expect(
      within(screen.getByTestId("editor-field-row-sys_title")).queryByTestId(
        "editor-field-error-sys_title",
      ),
    ).toBeNull();
    expect(saveFields).not.toHaveBeenCalled();
  });
});
