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

describe("EditorHost create item", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("creates in the selected folder and opens the new item", async () => {
    const createItem = vi.fn().mockResolvedValue({
      itemId: "77",
      folderPath: "/Assets",
      name: "Shot",
      contentType: "percImageAsset",
    });
    const loadFields = vi.fn().mockResolvedValue({
      contentId: "77",
      contentType: "percImageAsset",
      name: "Shot",
      checkoutUser: "admin",
      revision: 1,
      fields: [{ name: "sys_title", value: "Shot" }],
    });
    render(
      <MemoryRouter initialEntries={["/editor"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                loadContentTypes={async () => [
                  { name: "percImageAsset", label: "Image" },
                ]}
                createItem={createItem}
                loadFields={loadFields}
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadType={async () => ({
                  fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
                })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-create-type").querySelector(
          'option[value="percImageAsset"]',
        ),
      ).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-create-type"), {
      target: { value: "percImageAsset" },
    });
    fireEvent.change(screen.getByTestId("editor-create-folder"), {
      target: { value: "/Assets" },
    });
    fireEvent.click(screen.getByTestId("editor-create-submit"));
    await waitFor(() => {
      expect(createItem).toHaveBeenCalledWith({
        contentType: "percImageAsset",
        folderPath: "/Assets",
      });
    });
    await waitFor(() => {
      expect(loadFields).toHaveBeenCalledWith("77");
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-content-id").textContent).toMatch(/77/);
    });
  });

  it("does not create when type or folder is missing", async () => {
    const createItem = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                loadContentTypes={async () => [
                  { name: "percImageAsset", label: "Image" },
                ]}
                createItem={createItem}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    fireEvent.click(screen.getByTestId("editor-create-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-create-error").textContent).toMatch(
        /type and folder/i,
      );
    });
    expect(createItem).not.toHaveBeenCalled();
  });

  it("surfaces 403 as failure, not success", async () => {
    const createItem = vi.fn().mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { Error: { message: "FORBIDDEN" } },
    });
    render(
      <MemoryRouter initialEntries={["/editor"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                loadContentTypes={async () => [
                  { name: "percImageAsset", label: "Image" },
                ]}
                createItem={createItem}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-create-type").querySelector(
          'option[value="percImageAsset"]',
        ),
      ).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-create-type"), {
      target: { value: "percImageAsset" },
    });
    fireEvent.change(screen.getByTestId("editor-create-folder"), {
      target: { value: "/Assets" },
    });
    fireEvent.click(screen.getByTestId("editor-create-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-create-error").textContent).toMatch(
        /not allowed/i,
      );
    });
    expect(screen.queryByTestId("editor-content-id")).toBeNull();
  });
});
