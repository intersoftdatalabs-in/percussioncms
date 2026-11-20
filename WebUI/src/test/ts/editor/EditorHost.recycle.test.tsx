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
  revision: 1,
  fields: [{ name: "sys_title", value: "Home" }],
};

function renderEdit(extra: Partial<React.ComponentProps<typeof EditorHost>> = {}) {
  return render(
    <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
      <Routes>
        <Route
          path="/editor"
          element={
            <EditorHost
              checkout={vi.fn().mockResolvedValue(undefined)}
              loadFields={vi.fn().mockResolvedValue(fields)}
              loadType={vi.fn().mockResolvedValue({ fields: [] })}
              loadTransitions={vi.fn().mockResolvedValue({ transitionTriggers: [] })}
              resolveRecycleTarget={vi
                .fn()
                .mockResolvedValue({ path: "//Sites/Demo/Home", type: "percPage" })}
              {...extra}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("EditorHost recycle missing/conflict (#6773)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("stays on the item with a named 404 error and moves focus to it", async () => {
    const recycleItem = vi.fn().mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: {},
    });
    renderEdit({ confirmRecycle: vi.fn().mockReturnValue(true), recycleItem });
    await waitFor(() => {
      expect(screen.getByTestId("editor-recycle")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-recycle"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-recycle-error").textContent).toMatch(
        /not found/i,
      );
    });
    expect(screen.getByTestId("editor-recycle-error").textContent).toContain("(Home)");
    expect(screen.queryByTestId("editor-recycle-done")).toBeNull();
    expect(screen.getByTestId("editor-content-id").textContent).toContain("42");
    expect(screen.getByTestId("editor-form")).toBeTruthy();
    expect(screen.getByTestId("editor-recycle")).toBeTruthy();
    expect(document.activeElement).toBe(screen.getByTestId("editor-recycle-error"));
    expect(recycleItem).toHaveBeenCalledTimes(1);
  });

  it("stays on the item with a named 409 error and no success notice", async () => {
    const recycleItem = vi.fn().mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: {},
    });
    renderEdit({ confirmRecycle: vi.fn().mockReturnValue(true), recycleItem });
    await waitFor(() => {
      expect(screen.getByTestId("editor-recycle")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-recycle"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-recycle-error").textContent).toMatch(
        /in use or checked out/i,
      );
    });
    expect(screen.getByTestId("editor-recycle-error").textContent).toContain("(Home)");
    expect(screen.queryByTestId("editor-recycle-done")).toBeNull();
    expect(screen.getByTestId("editor-content-id").textContent).toContain("42");
    expect(screen.getByTestId("editor-form")).toBeTruthy();
    expect(document.activeElement).toBe(screen.getByTestId("editor-recycle-error"));
    expect(recycleItem).toHaveBeenCalledTimes(1);
  });

  it("cancel recycles nothing and shows no error", async () => {
    const recycleItem = vi.fn().mockResolvedValue(undefined);
    renderEdit({ confirmRecycle: vi.fn().mockReturnValue(false), recycleItem });
    await waitFor(() => {
      expect(screen.getByTestId("editor-recycle")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-recycle"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(recycleItem).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-recycle-error")).toBeNull();
    expect(screen.queryByTestId("editor-recycle-done")).toBeNull();
    expect(screen.getByTestId("editor-content-id").textContent).toContain("42");
  });
});
