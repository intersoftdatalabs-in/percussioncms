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
import { MemoryRouter, Route, Routes, useLocation } from "react-router";
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

function LocationProbe(): React.ReactElement {
  const location = useLocation();
  return <div data-testid="location-probe">{location.pathname + location.search}</div>;
}

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
        <Route path="/explorer" element={<LocationProbe />} />
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

describe("EditorHost leave with unsaved edits", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("cancel on open folder keeps the dirty value and does not save", async () => {
    const saveFields = vi.fn();
    renderEdit({
      saveFields,
      confirmLeaveUnsaved: () => false,
    });
    await dirtyTitle();
    fireEvent.click(screen.getByTestId("editor-open-folder"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(
      (screen.getByTestId("editor-field-sys_title") as HTMLInputElement).value,
    ).toBe("Draft title");
    expect(screen.queryByTestId("location-probe")).toBeNull();
  });

  it("confirm on open folder leaves without a PUT", async () => {
    const saveFields = vi.fn();
    renderEdit({
      saveFields,
      loadItemLocation: vi.fn().mockResolvedValue({ path: "//Folders/Lab/Home" }),
      confirmLeaveUnsaved: () => true,
    });
    await dirtyTitle();
    fireEvent.click(screen.getByTestId("editor-open-folder"));
    await waitFor(() => {
      expect(screen.getByTestId("location-probe").textContent).toBe(
        "/explorer?path=%2FFolders%2FLab",
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
  });

  it("cancel on mode change stays on the dirty form", async () => {
    const saveFields = vi.fn();
    renderEdit({ saveFields, confirmLeaveUnsaved: () => false });
    await dirtyTitle();
    fireEvent.click(screen.getByTestId("editor-mode"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(
      (screen.getByTestId("editor-field-sys_title") as HTMLInputElement).value,
    ).toBe("Draft title");
    expect(screen.getByTestId("editor-mode").textContent).toMatch(/View/i);
  });

  it("confirm on mode change switches to view without a PUT", async () => {
    const saveFields = vi.fn();
    const loadFields = vi.fn().mockResolvedValue(fields);
    renderEdit({
      saveFields,
      loadFields,
      confirmLeaveUnsaved: () => true,
    });
    await dirtyTitle();
    fireEvent.click(screen.getByTestId("editor-mode"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-mode").textContent).toMatch(/Edit/i);
    });
    expect(saveFields).not.toHaveBeenCalled();
  });

  it("cancel on close stays; confirm closes without a PUT", async () => {
    const saveFields = vi.fn();
    const close = vi.spyOn(window, "close").mockImplementation(() => undefined);
    renderEdit({ saveFields, confirmLeaveUnsaved: () => false });
    await dirtyTitle();
    fireEvent.click(screen.getByTestId("editor-close"));
    expect(close).not.toHaveBeenCalled();
    expect(
      (screen.getByTestId("editor-field-sys_title") as HTMLInputElement).value,
    ).toBe("Draft title");
    cleanup();
    close.mockClear();
    renderEdit({ saveFields, confirmLeaveUnsaved: () => true });
    await dirtyTitle();
    fireEvent.click(screen.getByTestId("editor-close"));
    expect(close).toHaveBeenCalled();
    expect(saveFields).not.toHaveBeenCalled();
  });

  it("a successful save clears the leave prompt", async () => {
    const confirmLeaveUnsaved = vi.fn().mockReturnValue(false);
    const saveFields = vi.fn().mockResolvedValue({
      ...fields,
      fields: [{ name: "sys_title", value: "Draft title" }],
    });
    renderEdit({
      saveFields,
      confirmLeaveUnsaved,
      loadItemLocation: vi.fn().mockResolvedValue({ path: "//Folders/Lab/Home" }),
    });
    await dirtyTitle();
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-saved")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-open-folder"));
    await waitFor(() => {
      expect(screen.getByTestId("location-probe").textContent).toBe(
        "/explorer?path=%2FFolders%2FLab",
      );
    });
    expect(confirmLeaveUnsaved).not.toHaveBeenCalled();
  });

  it("a pending file prompts and cancel keeps the pick", async () => {
    const saveFields = vi.fn();
    renderEdit({
      saveFields,
      confirmLeaveUnsaved: () => false,
      loadType: vi.fn().mockResolvedValue({
        fields: [
          { name: "sys_title", label: "Title" },
          { name: "item_file_attachment", label: "File", control: "sys_file" },
        ],
      }),
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-file-item_file_attachment")).toBeTruthy();
    });
    const input = screen.getByTestId("editor-file-item_file_attachment");
    const file = new File(["bytes"], "notes.txt", { type: "text/plain" });
    fireEvent.change(input, { target: { files: [file] } });
    await waitFor(() => {
      expect(screen.getByTestId("editor-file-name-item_file_attachment").textContent).toMatch(
        /notes\.txt/,
      );
    });
    fireEvent.click(screen.getByTestId("editor-mode"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("editor-file-name-item_file_attachment").textContent).toMatch(
      /notes\.txt/,
    );
  });

  it("confirm on mode change drops a pending file so a later save does not upload it", async () => {
    const saveFields = vi.fn().mockImplementation(async () => fields);
    const uploadBinary = vi.fn().mockResolvedValue(undefined);
    const clearBinary = vi.fn().mockResolvedValue(undefined);
    renderEdit({
      saveFields,
      uploadBinary,
      clearBinary,
      confirmLeaveUnsaved: () => true,
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
      expect(screen.getByTestId("editor-file-name-item_file_attachment").textContent).toMatch(
        /notes\.txt/,
      );
    });
    fireEvent.click(screen.getByTestId("editor-mode"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-mode").textContent).toMatch(/Edit/i);
    });
    fireEvent.click(screen.getByTestId("editor-mode"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-save")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-saved")).toBeTruthy();
    });
    expect(uploadBinary).not.toHaveBeenCalled();
    expect(clearBinary).not.toHaveBeenCalled();
  });

  it("confirm on mode change drops a pending clear so a later save does not delete the binary", async () => {
    const saveFields = vi.fn().mockImplementation(async () => fields);
    const uploadBinary = vi.fn().mockResolvedValue(undefined);
    const clearBinary = vi.fn().mockResolvedValue(undefined);
    renderEdit({
      saveFields,
      uploadBinary,
      clearBinary,
      confirmLeaveUnsaved: () => true,
      loadType: vi.fn().mockResolvedValue({
        fields: [
          { name: "sys_title", label: "Title" },
          { name: "item_file_attachment", label: "File", control: "sys_file" },
        ],
      }),
      loadBinaryMeta: vi.fn().mockResolvedValue({
        present: true,
        filename: "stored.bin",
      }),
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-file-clear-item_file_attachment")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-file-clear-item_file_attachment"));
    fireEvent.click(screen.getByTestId("editor-mode"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-mode").textContent).toMatch(/Edit/i);
    });
    fireEvent.click(screen.getByTestId("editor-mode"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-save")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-saved")).toBeTruthy();
    });
    expect(clearBinary).not.toHaveBeenCalled();
    expect(uploadBinary).not.toHaveBeenCalled();
  });

  it("confirm switches to another item without a PUT", async () => {
    const saveFields = vi.fn();
    const loadFields = vi.fn().mockImplementation(async (id: string) => ({
      ...fields,
      contentId: id,
    }));
    renderEdit({
      saveFields,
      loadFields,
      confirmLeaveUnsaved: () => true,
      loadTranslationVariants: vi.fn().mockResolvedValue({
        itemId: 42,
        variants: [{ contentId: 900, locale: "fr-fr", role: "translation" }],
      }),
    });
    await dirtyTitle();
    await waitFor(() => {
      expect(screen.getByTestId("translations-open-variant-900")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("translations-open-variant-900"));
    await waitFor(() => {
      expect(loadFields).toHaveBeenCalledWith("900");
    });
    expect(saveFields).not.toHaveBeenCalled();
  });

  it("cancel on recycle keeps the item and does not delete it", async () => {
    const recycleItem = vi.fn().mockResolvedValue(undefined);
    const resolveRecycleTarget = vi.fn().mockResolvedValue({
      path: "//Sites/Demo/Home",
      type: "percPage",
    });
    renderEdit({
      recycleItem,
      resolveRecycleTarget,
      confirmRecycle: () => true,
      confirmLeaveUnsaved: () => false,
    });
    await dirtyTitle();
    fireEvent.click(screen.getByTestId("editor-recycle"));
    expect(recycleItem).not.toHaveBeenCalled();
    expect(resolveRecycleTarget).not.toHaveBeenCalled();
    expect(
      (screen.getByTestId("editor-field-sys_title") as HTMLInputElement).value,
    ).toBe("Draft title");
    expect(screen.getByTestId("editor-content-id").textContent).toMatch(/42/);
  });

  it("confirm on recycle deletes only after the leave prompt", async () => {
    const recycleItem = vi.fn().mockResolvedValue(undefined);
    const resolveRecycleTarget = vi.fn().mockResolvedValue({
      path: "//Sites/Demo/Home",
      type: "percPage",
    });
    const confirmLeaveUnsaved = vi.fn().mockReturnValue(true);
    renderEdit({
      recycleItem,
      resolveRecycleTarget,
      confirmRecycle: () => true,
      confirmLeaveUnsaved,
    });
    await dirtyTitle();
    fireEvent.click(screen.getByTestId("editor-recycle"));
    await waitFor(() => {
      expect(recycleItem).toHaveBeenCalledWith("//Sites/Demo/Home");
    });
    expect(confirmLeaveUnsaved).toHaveBeenCalledTimes(1);
    await waitFor(() => {
      expect(screen.queryByTestId("editor-content-id")).toBeNull();
    });
  });

  it("cancel on new copy does not copy", async () => {
    const copyItem = vi.fn().mockResolvedValue({ itemId: "99" });
    renderEdit({
      copyItem,
      confirmCopy: () => true,
      confirmLeaveUnsaved: () => false,
    });
    await dirtyTitle();
    fireEvent.click(screen.getByTestId("editor-new-copy"));
    expect(copyItem).not.toHaveBeenCalled();
    expect(
      (screen.getByTestId("editor-field-sys_title") as HTMLInputElement).value,
    ).toBe("Draft title");
  });

  it("cancel on new item does not create", async () => {
    const createItem = vi.fn().mockResolvedValue({ itemId: "77" });
    renderEdit({
      createItem,
      confirmLeaveUnsaved: () => false,
      loadContentTypes: vi.fn().mockResolvedValue([
        { name: "percPage", label: "Page" },
      ]),
    });
    await dirtyTitle();
    fireEvent.click(screen.getByTestId("editor-new-item"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-create-type").querySelector('option[value="percPage"]'),
      ).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-create-type"), {
      target: { value: "percPage" },
    });
    fireEvent.change(screen.getByTestId("editor-create-folder"), {
      target: { value: "/Sites/Demo" },
    });
    fireEvent.click(screen.getByTestId("editor-create-submit"));
    expect(createItem).not.toHaveBeenCalled();
    expect(
      (screen.getByTestId("editor-field-sys_title") as HTMLInputElement).value,
    ).toBe("Draft title");
  });
});
