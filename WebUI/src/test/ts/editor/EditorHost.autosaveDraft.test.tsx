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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { EditorHost } from "../../../main/ts/editor/EditorHost";
import {
  editorAutosaveKey,
  writeAutosavedEditorDraft,
} from "../../../main/ts/editor/editorAutosave";
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
              loadTemplates={vi.fn().mockResolvedValue([])}
              {...extra}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

async function loadedTitle(): Promise<void> {
  await waitFor(() => {
    expect(screen.getByTestId("editor-field-sys_title")).toBeTruthy();
  });
}

describe("EditorHost autosave draft recovery (#6076)", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    cleanup();
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it("offers the stale draft after reload; restore fills the draft and never PUTs", async () => {
    writeAutosavedEditorDraft(localStorage, "42", { sys_title: "Stale draft" }, 1);
    const saveFields = vi.fn();
    renderEdit({ saveFields });
    await loadedTitle();
    await waitFor(() => {
      expect(screen.getByTestId("editor-autosave-notice")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("editor-field-sys_title") as HTMLInputElement).value,
    ).toBe("Home");
    fireEvent.click(screen.getByTestId("editor-autosave-restore"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("editor-field-sys_title") as HTMLInputElement).value,
      ).toBe("Stale draft");
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-autosave-notice")).toBeNull();
  });

  it("discard drops the stored draft and keeps the loaded values", async () => {
    writeAutosavedEditorDraft(localStorage, "42", { sys_title: "Stale draft" }, 1);
    const saveFields = vi.fn();
    renderEdit({ saveFields });
    await loadedTitle();
    await waitFor(() => {
      expect(screen.getByTestId("editor-autosave-notice")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-autosave-discard"));
    await waitFor(() => {
      expect(screen.queryByTestId("editor-autosave-notice")).toBeNull();
    });
    expect(
      (screen.getByTestId("editor-field-sys_title") as HTMLInputElement).value,
    ).toBe("Home");
    expect(localStorage.getItem(editorAutosaveKey("42"))).toBeNull();
    expect(saveFields).not.toHaveBeenCalled();
  });

  it("stays silent when the stored draft matches the loaded values", async () => {
    writeAutosavedEditorDraft(localStorage, "42", { sys_title: "Home" }, 1);
    renderEdit({ saveFields: vi.fn() });
    await loadedTitle();
    await waitFor(() => {
      expect(screen.getByTestId("editor-save")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-autosave-notice")).toBeNull();
  });

  it("stays silent in view mode even with a stored draft", async () => {
    writeAutosavedEditorDraft(localStorage, "42", { sys_title: "Stale draft" }, 1);
    renderEdit({ saveFields: vi.fn() }, "/editor?contentId=42&mode=view");
    await loadedTitle();
    await waitFor(() => {
      expect(screen.getByTestId("editor-host")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-autosave-notice")).toBeNull();
  });

  it("mirrors dirty edits to storage and clears the entry on save", async () => {
    const saveFields = vi.fn().mockResolvedValue({
      ...fields,
      fields: [{ name: "sys_title", value: "New title" }],
    });
    renderEdit({ saveFields });
    await loadedTitle();
    expect(localStorage.getItem(editorAutosaveKey("42"))).toBeNull();
    fireEvent.change(screen.getByTestId("editor-field-sys_title"), {
      target: { value: "New title" },
    });
    await waitFor(() => {
      expect(localStorage.getItem(editorAutosaveKey("42"))).not.toBeNull();
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-saved")).toBeTruthy();
    });
    expect(saveFields).toHaveBeenCalledTimes(1);
    expect(localStorage.getItem(editorAutosaveKey("42"))).toBeNull();
    expect(screen.queryByTestId("editor-autosave-notice")).toBeNull();
  });
});
