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

describe("EditorHost incremental unapprove (#5161)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("clears approved only after the server accepts unapprove", async () => {
    let resolveUnapprove: (value: boolean) => void = () => undefined;
    const unapproveIncremental = vi.fn(
      () =>
        new Promise<boolean>((resolve) => {
          resolveUnapprove = resolve;
        }),
    );
    const approveIncremental = vi.fn(async () => true);
    renderEdit("percPage", { approveIncremental, unapproveIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-unapprove")).toBeTruthy();
    });
    await approveOpenItem();
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove"));
    expect(unapproveIncremental).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove-confirm"));
    await waitFor(() => {
      expect(unapproveIncremental).toHaveBeenCalledWith("42", "page");
    });
    expect(screen.getByTestId("editor-incremental-approved")).toBeTruthy();
    expect(screen.queryByTestId("editor-incremental-unapproved")).toBeNull();
    expect(screen.getByTestId("editor-incremental-unapprove-dialog")).toBeTruthy();
    resolveUnapprove(true);
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-unapproved").textContent).toMatch(
        /unapproved from the incremental queue/i,
      );
    });
    expect(screen.queryByTestId("editor-incremental-approved")).toBeNull();
    expect(screen.queryByTestId("editor-incremental-unapprove-dialog")).toBeNull();
  });

  it("unapproves an open asset the same way", async () => {
    const unapproveIncremental = vi.fn(async () => true);
    renderEdit("percImage", { unapproveIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-unapprove")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove"));
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-unapproved")).toBeTruthy();
    });
    expect(unapproveIncremental).toHaveBeenCalledWith("42", "asset");
    expect(screen.queryByTestId("editor-incremental-approved")).toBeNull();
  });

  it("cancel does not call the server or clear approved", async () => {
    const unapproveIncremental = vi.fn(async () => true);
    const approveIncremental = vi.fn(async () => true);
    renderEdit("percPage", { approveIncremental, unapproveIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-unapprove")).toBeTruthy();
    });
    await approveOpenItem();
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove"));
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove-cancel"));
    expect(unapproveIncremental).not.toHaveBeenCalled();
    expect(screen.getByTestId("editor-incremental-approved")).toBeTruthy();
    expect(screen.queryByTestId("editor-incremental-unapproved")).toBeNull();
    expect(screen.queryByTestId("editor-incremental-unapprove-dialog")).toBeNull();
  });

  it("does not unapprove a template or folder", async () => {
    const unapproveIncremental = vi.fn(async () => true);
    renderEdit("percTemplate", { unapproveIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-unapprove")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove"));
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove-confirm"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-incremental-unapprove-error").textContent,
      ).toMatch(/only a page or asset/i);
    });
    expect(unapproveIncremental).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-incremental-unapproved")).toBeNull();

    cleanup();
    const folderUnapprove = vi.fn(async () => true);
    renderEdit("folder", { unapproveIncremental: folderUnapprove });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-unapprove")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove"));
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove-confirm"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-incremental-unapprove-error").textContent,
      ).toMatch(/only a page or asset/i);
    });
    expect(folderUnapprove).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-incremental-unapproved")).toBeNull();
  });

  it("does not clear approved for HTTP 400, 403, or 409", async () => {
    const unapproveIncremental = vi
      .fn()
      .mockRejectedValueOnce({ status: 400, statusText: "Bad Request", body: {} })
      .mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: {} })
      .mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: {} });
    const approveIncremental = vi.fn(async () => true);
    renderEdit("percPage", { approveIncremental, unapproveIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-unapprove")).toBeTruthy();
    });
    await approveOpenItem();
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove"));
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove-confirm"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-incremental-unapprove-error").textContent,
      ).toMatch(/could not be unapproved/i);
    });
    expect(screen.getByTestId("editor-incremental-approved")).toBeTruthy();
    expect(screen.queryByTestId("editor-incremental-unapproved")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove-confirm"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-incremental-unapprove-error").textContent,
      ).toMatch(/not allowed/i);
    });
    fireEvent.click(screen.getByTestId("editor-incremental-unapprove-confirm"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-incremental-unapprove-error").textContent,
      ).toMatch(/blocked/i);
    });
    expect(screen.getByTestId("editor-incremental-unapprove-dialog")).toBeTruthy();
    expect(screen.getByTestId("editor-incremental-approved")).toBeTruthy();
    expect(screen.queryByTestId("editor-incremental-unapproved")).toBeNull();
    expect(unapproveIncremental).toHaveBeenCalledTimes(3);
  });

  it("hides unapprove in view mode", async () => {
    const unapproveIncremental = vi.fn(async () => true);
    renderEdit("percPage", { unapproveIncremental }, "view");
    await waitFor(() => {
      expect(screen.getByTestId("editor-host")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-incremental-unapprove")).toBeNull();
    expect(unapproveIncremental).not.toHaveBeenCalled();
  });
});
