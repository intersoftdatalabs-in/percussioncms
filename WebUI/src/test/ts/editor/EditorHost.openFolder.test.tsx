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

function renderHost(extra: Partial<React.ComponentProps<typeof EditorHost>> = {}) {
  return render(
    <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
      <Routes>
        <Route
          path="/editor"
          element={
            <EditorHost
              checkout={vi.fn().mockResolvedValue(undefined)}
              loadFields={vi.fn().mockResolvedValue(fields)}
              loadType={vi.fn().mockResolvedValue({ fields: [] })}
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

describe("EditorHost open folder", () => {
  afterEach(() => {
    cleanup();
  });

  it("opens the explorer route for the item folder", async () => {
    renderHost({
      loadItemLocation: vi.fn().mockResolvedValue({ path: "//Folders/Lab/Home" }),
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-open-folder")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-open-folder"));
    await waitFor(() => {
      expect(screen.getByTestId("location-probe").textContent).toBe(
        "/explorer?path=%2FFolders%2FLab",
      );
    });
  });

  it("stays on the editor when the item has no folder", async () => {
    renderHost({
      loadItemLocation: vi.fn().mockResolvedValue({ path: "" }),
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-open-folder")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-open-folder"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-open-folder-error").textContent).toMatch(
        /not in a folder/i,
      );
    });
    expect(screen.getByTestId("editor-host")).toBeTruthy();
    expect(screen.queryByTestId("location-probe")).toBeNull();
  });

  it("stays on the editor when the lookup is forbidden", async () => {
    renderHost({
      loadItemLocation: vi.fn().mockRejectedValue({
        status: 403,
        statusText: "Forbidden",
        body: {},
      }),
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-open-folder")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-open-folder"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-open-folder-error").textContent).toMatch(
        /not allowed/i,
      );
    });
    expect(screen.queryByTestId("location-probe")).toBeNull();
  });

  it("stays on the editor when the lookup is not found", async () => {
    renderHost({
      loadItemLocation: vi.fn().mockRejectedValue({
        status: 404,
        statusText: "Not Found",
        body: {},
      }),
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-open-folder")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-open-folder"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-open-folder-error").textContent).toMatch(
        /not found/i,
      );
    });
    expect(screen.queryByTestId("location-probe")).toBeNull();
  });
});
