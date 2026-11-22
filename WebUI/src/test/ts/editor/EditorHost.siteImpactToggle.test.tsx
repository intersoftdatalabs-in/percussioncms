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
import { renderA11yGate } from "../contentExplorer/a11y";

const fields: ItemEditorFields = {
  contentId: "42",
  contentType: "percPage",
  name: "Home",
  checkoutUser: "admin",
  fields: [{ name: "sys_title", value: "Home" }],
};

function renderHost({ mode = "edit" }: { mode?: string } = {}) {
  render(
    <MemoryRouter initialEntries={[`/editor?contentId=42&mode=${mode}`]}>
      <Routes>
        <Route
          path="/editor"
          element={
            <EditorHost
              checkout={vi.fn().mockResolvedValue(undefined)}
              loadFields={vi.fn().mockResolvedValue(fields)}
              loadType={async () => ({
                fields: [{ name: "sys_title", label: "Title", readOnly: false }],
              })}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("EditorHost site-impact section toggle (#6897)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("is open by default and exposes its expanded state", async () => {
    renderHost();
    await waitFor(() => {
      expect(screen.getByTestId("editor-site-impact-toggle")).toBeTruthy();
    });
    // Open by default so the panel stays visible exactly as before; the
    // toggle only adds collapsibility plus an exposed expanded state.
    expect(
      screen.getByTestId("editor-site-impact-toggle").getAttribute("aria-expanded"),
    ).toBe("true");
    expect(
      screen.getByTestId("editor-site-impact-toggle").getAttribute("aria-controls"),
    ).toBe("editor-site-impact-panel");
    expect(screen.getByTestId("editor-site-impact-panel")).toBeTruthy();
    await renderA11yGate(screen.getByTestId("editor-site-impact-panel"));
  });

  it("collapses and re-opens the panel through the toggle", async () => {
    renderHost();
    await waitFor(() => {
      expect(screen.getByTestId("editor-site-impact-toggle")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-site-impact-toggle"));
    await waitFor(() => {
      expect(screen.queryByTestId("editor-site-impact-panel")).toBeNull();
    });
    expect(
      screen.getByTestId("editor-site-impact-toggle").getAttribute("aria-expanded"),
    ).toBe("false");
    fireEvent.click(screen.getByTestId("editor-site-impact-toggle"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-site-impact-panel")).toBeTruthy();
    });
    expect(
      screen.getByTestId("editor-site-impact-toggle").getAttribute("aria-expanded"),
    ).toBe("true");
  });

  it("shows the toggle read-only in view mode", async () => {
    renderHost({ mode: "view" });
    await waitFor(() => {
      expect(screen.getByTestId("editor-site-impact-toggle")).toBeTruthy();
    });
    expect(
      screen.getByTestId("editor-site-impact-toggle").getAttribute("aria-expanded"),
    ).toBe("true");
    expect(screen.getByTestId("editor-site-impact-panel")).toBeTruthy();
  });
});
