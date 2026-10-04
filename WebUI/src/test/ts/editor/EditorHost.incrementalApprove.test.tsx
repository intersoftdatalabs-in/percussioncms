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

describe("EditorHost incremental approve (#5124)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("shows approved only after the server accepts the open page", async () => {
    let resolveApprove: (value: boolean) => void = () => undefined;
    const approveIncremental = vi.fn(
      () =>
        new Promise<boolean>((resolve) => {
          resolveApprove = resolve;
        }),
    );
    renderEdit("percPage", { approveIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-approve")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-incremental-approved")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-incremental-approve"));
    expect(approveIncremental).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("editor-incremental-approve-confirm"));
    await waitFor(() => {
      expect(approveIncremental).toHaveBeenCalledWith("42", "page");
    });
    expect(screen.queryByTestId("editor-incremental-approved")).toBeNull();
    expect(screen.getByTestId("editor-incremental-approve-dialog")).toBeTruthy();
    resolveApprove(true);
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-approved").textContent).toMatch(
        /approved onto the incremental queue/i,
      );
    });
    expect(screen.queryByTestId("editor-incremental-approve-dialog")).toBeNull();
  });

  it("approves an open asset the same way", async () => {
    const approveIncremental = vi.fn(async () => true);
    renderEdit("percImage", { approveIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-approve")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-incremental-approve"));
    fireEvent.click(screen.getByTestId("editor-incremental-approve-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-approved")).toBeTruthy();
    });
    expect(approveIncremental).toHaveBeenCalledWith("42", "asset");
  });

  it("cancel does not call the server", async () => {
    const approveIncremental = vi.fn(async () => true);
    renderEdit("percPage", { approveIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-approve")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-incremental-approve"));
    fireEvent.click(screen.getByTestId("editor-incremental-approve-cancel"));
    expect(approveIncremental).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-incremental-approved")).toBeNull();
    expect(screen.queryByTestId("editor-incremental-approve-dialog")).toBeNull();
  });

  it("does not claim approval for an item that cannot be queued", async () => {
    const approveIncremental = vi.fn(async () => true);
    renderEdit("percTemplate", { approveIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-approve")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-incremental-approve"));
    fireEvent.click(screen.getByTestId("editor-incremental-approve-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-approve-error").textContent).toMatch(
        /only a page or asset/i,
      );
    });
    expect(approveIncremental).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-incremental-approved")).toBeNull();
    expect(screen.getByTestId("editor-incremental-approve-dialog")).toBeTruthy();
  });

  it("does not claim approval for HTTP 400, 403, or 409", async () => {
    const approveIncremental = vi
      .fn()
      .mockRejectedValueOnce({ status: 400, statusText: "Bad Request", body: {} })
      .mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: {} })
      .mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: {} });
    renderEdit("percPage", { approveIncremental });
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-approve")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-incremental-approve"));
    fireEvent.click(screen.getByTestId("editor-incremental-approve-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-approve-error").textContent).toMatch(
        /could not be queued/i,
      );
    });
    expect(screen.queryByTestId("editor-incremental-approved")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-incremental-approve-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-approve-error").textContent).toMatch(
        /not allowed/i,
      );
    });
    fireEvent.click(screen.getByTestId("editor-incremental-approve-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-incremental-approve-error").textContent).toMatch(
        /blocked/i,
      );
    });
    expect(screen.getByTestId("editor-incremental-approve-dialog")).toBeTruthy();
    expect(screen.queryByTestId("editor-incremental-approved")).toBeNull();
    expect(approveIncremental).toHaveBeenCalledTimes(3);
  });

  it("hides approve in view mode", async () => {
    const approveIncremental = vi.fn(async () => true);
    renderEdit("percPage", { approveIncremental }, "view");
    await waitFor(() => {
      expect(screen.getByTestId("editor-host")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-incremental-approve")).toBeNull();
    expect(approveIncremental).not.toHaveBeenCalled();
  });
});
