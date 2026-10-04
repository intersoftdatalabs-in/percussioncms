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

function fieldsFor(contentType: string): ItemEditorFields {
  return {
    contentId: "42",
    contentType,
    name: "Home",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Home" }],
  };
}

function renderEdit(
  contentType: string,
  extra: Partial<React.ComponentProps<typeof EditorHost>> = {},
  mode = "edit",
) {
  return render(
    <MemoryRouter initialEntries={[`/editor?contentId=42&mode=${mode}`]}>
      <Routes>
        <Route
          path="/editor"
          element={
            <EditorHost
              checkout={vi.fn().mockResolvedValue(undefined)}
              loadFields={vi.fn().mockResolvedValue(fieldsFor(contentType))}
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

async function approveOpenItem(): Promise<void> {
  fireEvent.click(screen.getByTestId("editor-incremental-approve"));
  fireEvent.click(screen.getByTestId("editor-incremental-approve-confirm"));
  await waitFor(() => {
    expect(screen.getByTestId("editor-incremental-approved")).toBeTruthy();
  });
}

describe("EditorHost incremental remove (#5162)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("clears approved only after the server accepts remove", async () => {
    let resolveRemove: (value: boolean) => void = () => undefined;
    const removeIncremental = vi.fn(
      () =>
        new Promise<boolean>((resolve) => {
          resolveRemove = resolve;
        }),
    );
    const approveIncremental = vi.fn(async () => true);
    renderEdit("percPage", { approveIncremental, removeIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-remove")).toBeTruthy();
    });
    await approveOpenItem();
    fireEvent.click(screen.getByTestId("editor-incremental-remove"));
    expect(removeIncremental).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("editor-incremental-remove-confirm"));
    await waitFor(() => {
      expect(removeIncremental).toHaveBeenCalledWith("42", "page");
    });
    expect(screen.getByTestId("editor-incremental-approved")).toBeTruthy();
    expect(screen.queryByTestId("editor-incremental-removed")).toBeNull();
    expect(screen.getByTestId("editor-incremental-remove-dialog")).toBeTruthy();
    resolveRemove(true);
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-removed").textContent).toMatch(
        /removed from the incremental queue/i,
      );
    });
    expect(screen.queryByTestId("editor-incremental-approved")).toBeNull();
    expect(screen.queryByTestId("editor-incremental-unapproved")).toBeNull();
    expect(screen.queryByTestId("editor-incremental-remove-dialog")).toBeNull();
  });

  it("removes an open asset the same way", async () => {
    const removeIncremental = vi.fn(async () => true);
    renderEdit("percImage", { removeIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-remove")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-incremental-remove"));
    fireEvent.click(screen.getByTestId("editor-incremental-remove-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-removed")).toBeTruthy();
    });
    expect(removeIncremental).toHaveBeenCalledWith("42", "asset");
    expect(screen.queryByTestId("editor-incremental-approved")).toBeNull();
  });

  it("cancel does not call the server or clear approved", async () => {
    const removeIncremental = vi.fn(async () => true);
    const approveIncremental = vi.fn(async () => true);
    renderEdit("percPage", { approveIncremental, removeIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-remove")).toBeTruthy();
    });
    await approveOpenItem();
    fireEvent.click(screen.getByTestId("editor-incremental-remove"));
    fireEvent.click(screen.getByTestId("editor-incremental-remove-cancel"));
    expect(removeIncremental).not.toHaveBeenCalled();
    expect(screen.getByTestId("editor-incremental-approved")).toBeTruthy();
    expect(screen.queryByTestId("editor-incremental-removed")).toBeNull();
    expect(screen.queryByTestId("editor-incremental-remove-dialog")).toBeNull();
  });

  it("does not remove a template or folder", async () => {
    const removeIncremental = vi.fn(async () => true);
    renderEdit("percTemplate", { removeIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-remove")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-incremental-remove"));
    fireEvent.click(screen.getByTestId("editor-incremental-remove-confirm"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-incremental-remove-error").textContent,
      ).toMatch(/only a page or asset/i);
    });
    expect(removeIncremental).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-incremental-removed")).toBeNull();

    cleanup();
    const folderRemove = vi.fn(async () => true);
    renderEdit("folder", { removeIncremental: folderRemove });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-remove")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-incremental-remove"));
    fireEvent.click(screen.getByTestId("editor-incremental-remove-confirm"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-incremental-remove-error").textContent,
      ).toMatch(/only a page or asset/i);
    });
    expect(folderRemove).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-incremental-removed")).toBeNull();
  });

  it("does not claim removed for HTTP 400, 403, or 409", async () => {
    const removeIncremental = vi
      .fn()
      .mockRejectedValueOnce({ status: 400, statusText: "Bad Request", body: {} })
      .mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: {} })
      .mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: {} });
    const approveIncremental = vi.fn(async () => true);
    renderEdit("percPage", { approveIncremental, removeIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-remove")).toBeTruthy();
    });
    await approveOpenItem();
    fireEvent.click(screen.getByTestId("editor-incremental-remove"));
    fireEvent.click(screen.getByTestId("editor-incremental-remove-confirm"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-incremental-remove-error").textContent,
      ).toMatch(/could not be removed/i);
    });
    expect(screen.getByTestId("editor-incremental-approved")).toBeTruthy();
    expect(screen.queryByTestId("editor-incremental-removed")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-incremental-remove-confirm"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-incremental-remove-error").textContent,
      ).toMatch(/not allowed/i);
    });
    fireEvent.click(screen.getByTestId("editor-incremental-remove-confirm"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-incremental-remove-error").textContent,
      ).toMatch(/blocked/i);
    });
    expect(screen.getByTestId("editor-incremental-remove-dialog")).toBeTruthy();
    expect(screen.getByTestId("editor-incremental-approved")).toBeTruthy();
    expect(screen.queryByTestId("editor-incremental-removed")).toBeNull();
    expect(removeIncremental).toHaveBeenCalledTimes(3);
  });

  it("hides remove in view mode", async () => {
    const removeIncremental = vi.fn(async () => true);
    renderEdit("percPage", { removeIncremental }, "view");
    await waitFor(() => {
      expect(screen.getByTestId("editor-host")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-incremental-remove")).toBeNull();
    expect(removeIncremental).not.toHaveBeenCalled();
  });
});
