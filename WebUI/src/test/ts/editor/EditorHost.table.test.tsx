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
  revision: 3,
  fields: [
    { name: "sys_title", value: "Home" },
    { name: "hours", value: "" },
  ],
};

describe("EditorHost table field", () => {
  afterEach(() => {
    cleanup();
  });

  it("saves an edited table without dropping the title", async () => {
    const saveFields = vi.fn().mockResolvedValue(fields);
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                saveFields={saveFields}
                loadType={vi.fn().mockResolvedValue({
                  fields: [
                    { name: "sys_title", label: "Title", control: "sys_EditBox" },
                    { name: "hours", label: "Hours", control: "sys_Table" },
                  ],
                })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-hours")).toHaveAttribute(
        "data-editor-kind",
        "table",
      );
    });
    fireEvent.click(screen.getByTestId("editor-table-add-hours"));
    fireEvent.change(screen.getByTestId("editor-table-cell-hours-0-0"), {
      target: { value: "Mon" },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((row) => row.name === "sys_title")?.value).toBe("Home");
    expect(saved.fields.find((row) => row.name === "hours")?.value).toBe(
      JSON.stringify({ columns: ["value"], rows: [["Mon"]] }),
    );
  });

  it("keeps an empty table and the title on save, and locks the grid in view mode", async () => {
    const saveFields = vi.fn().mockResolvedValue(fields);
    const { unmount } = render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                saveFields={saveFields}
                loadType={vi.fn().mockResolvedValue({
                  fields: [
                    { name: "sys_title", label: "Title", control: "sys_EditBox" },
                    { name: "hours", label: "Hours", control: "sys_Table" },
                  ],
                })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-save")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.map((row) => row.name).sort()).toEqual(["hours", "sys_title"]);
    expect(saved.fields.find((row) => row.name === "hours")?.value).toBe("");
    expect(saved.fields.find((row) => row.name === "sys_title")?.value).toBe("Home");
    unmount();

    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={vi.fn().mockResolvedValue({
                  ...fields,
                  fields: [
                    { name: "sys_title", value: "Home" },
                    {
                      name: "hours",
                      value: '{"columns":["day"],"rows":[["Mon"]]}',
                    },
                  ],
                })}
                saveFields={vi.fn()}
                loadType={vi.fn().mockResolvedValue({
                  fields: [
                    { name: "sys_title", label: "Title", control: "sys_EditBox" },
                    { name: "hours", label: "Hours", control: "sys_Table" },
                  ],
                })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-table-cell-hours-0-0")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-field-sys_title")).toHaveProperty("readOnly", true);
    expect(screen.queryByTestId("editor-table-add-hours")).toBeNull();
    expect(screen.queryByTestId("editor-save")).toBeNull();
  });
});
