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

const STORED_GRID = JSON.stringify({ columns: ["day"], rows: [["Mon"]] });

describe("EditorHost refuse saving an empty required table (#5281)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  function tableHost(opts: {
    saveFields?: (id: string, body: ItemEditorFields) => Promise<ItemEditorFields>;
    hours?: string;
    required?: boolean;
    confirmLeaveUnsaved?: (body: string) => boolean;
  }) {
    const hours = opts.hours ?? STORED_GRID;
    const required = opts.required !== false;
    return (
      <EditorHost
        checkout={vi.fn().mockResolvedValue(undefined)}
        loadFields={async () => ({
          contentId: "42",
          contentType: "percPage",
          name: "Home",
          checkoutUser: "admin",
          revision: 3,
          fields: [
            { name: "sys_title", value: "Home" },
            { name: "hours", value: hours },
          ],
        })}
        saveFields={opts.saveFields ?? vi.fn()}
        loadType={async () => ({
          fields: [
            { name: "sys_title", label: "Title", control: "sys_EditBox" },
            {
              name: "hours",
              label: "Hours",
              control: "sys_Table",
              required,
            },
          ],
        })}
        confirmLeaveUnsaved={opts.confirmLeaveUnsaved}
      />
    );
  }

  function renderTableHost(element: React.ReactElement): void {
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route path="/editor" element={element} />
        </Routes>
      </MemoryRouter>,
    );
  }

  async function openStoredGrid(element: React.ReactElement): Promise<void> {
    renderTableHost(element);
    await waitFor(() => {
      expect(
        (screen.getByTestId("editor-table-cell-hours-0-0") as HTMLInputElement).value,
      ).toBe("Mon");
    });
  }

  it("does not save an emptied required table and reloads the previous grid", async () => {
    const saveFields = vi.fn();
    await openStoredGrid(tableHost({ saveFields }));
    expect(screen.getByTestId("editor-field-hours").getAttribute("data-editor-kind")).toBe(
      "table",
    );
    expect(screen.getByTestId("editor-field-row-hours").getAttribute("data-required")).toBe(
      "true",
    );
    fireEvent.click(screen.getByTestId("editor-table-remove-hours-0"));
    expect(screen.queryByTestId("editor-table-cell-hours-0-0")).toBeNull();
    expect(screen.getByRole("columnheader", { name: "day" })).toBeTruthy();
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-hours").textContent).toBe(
        "This field is required.",
      );
    });
    expect(screen.getByTestId("editor-save-error").textContent).toMatch(
      /required fields before saving/i,
    );
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-saved")).toBeNull();
    expect(screen.getByTestId("editor-field-hours").getAttribute("data-editor-kind")).toBe(
      "table",
    );
    expect(screen.getByRole("columnheader", { name: "day" })).toBeTruthy();
    expect(screen.getByTestId("editor-field-row-hours").getAttribute("data-required")).toBe(
      "true",
    );
    cleanup();
    await openStoredGrid(tableHost({ saveFields }));
    expect(
      (screen.getByTestId("editor-table-cell-hours-0-0") as HTMLInputElement).value,
    ).toBe("Mon");
    expect(saveFields).not.toHaveBeenCalled();
  });

  it("does not save a required table that was never filled", async () => {
    const saveFields = vi.fn();
    renderTableHost(tableHost({ saveFields, hours: "" }));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-hours").getAttribute("data-editor-kind")).toBe(
        "table",
      );
    });
    expect(screen.getByTestId("editor-field-row-hours").getAttribute("data-required")).toBe(
      "true",
    );
    expect(screen.queryByTestId("editor-table-cell-hours-0-0")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-hours").textContent).toBe(
        "This field is required.",
      );
    });
    expect(screen.getByTestId("editor-save-error").textContent).toMatch(
      /required fields before saving/i,
    );
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-saved")).toBeNull();
    expect(screen.getByTestId("editor-table-add-hours")).toBeTruthy();
    expect(screen.getByTestId("editor-field-row-hours").getAttribute("data-required")).toBe(
      "true",
    );
  });

  it("does not save whitespace-only cells on a required table", async () => {
    const saveFields = vi.fn();
    await openStoredGrid(tableHost({ saveFields }));
    fireEvent.change(screen.getByTestId("editor-table-cell-hours-0-0"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-hours").textContent).toBe(
        "This field is required.",
      );
    });
    expect(document.activeElement).toBe(screen.getByTestId("editor-table-cell-hours-0-0"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-saved")).toBeNull();
    expect(
      (screen.getByTestId("editor-table-cell-hours-0-0") as HTMLInputElement).value,
    ).toBe("   ");
    cleanup();
    await openStoredGrid(tableHost({ saveFields }));
    expect(
      (screen.getByTestId("editor-table-cell-hours-0-0") as HTMLInputElement).value,
    ).toBe("Mon");
  });

  it("does not write when Close cancels an emptied required table", async () => {
    const saveFields = vi.fn();
    await openStoredGrid(
      tableHost({ saveFields, confirmLeaveUnsaved: () => false }),
    );
    fireEvent.click(screen.getByTestId("editor-table-remove-hours-0"));
    fireEvent.click(screen.getByTestId("editor-close"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-saved")).toBeNull();
    expect(screen.queryByTestId("editor-table-cell-hours-0-0")).toBeNull();
    expect(screen.getByTestId("editor-field-row-hours").getAttribute("data-required")).toBe(
      "true",
    );
    cleanup();
    await openStoredGrid(tableHost({ saveFields }));
    expect(
      (screen.getByTestId("editor-table-cell-hours-0-0") as HTMLInputElement).value,
    ).toBe("Mon");
    expect(saveFields).not.toHaveBeenCalled();
  });

  it("still saves a required table that has a column and cell text", async () => {
    let hours = STORED_GRID;
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => {
      hours = body.fields.find((row) => row.name === "hours")?.value ?? hours;
      return {
        contentId: "42",
        contentType: "percPage",
        name: "Home",
        checkoutUser: "admin",
        revision: 4,
        fields: body.fields,
      };
    });
    await openStoredGrid(tableHost({ saveFields, hours }));
    fireEvent.change(screen.getByTestId("editor-table-cell-hours-0-0"), {
      target: { value: "Tue" },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-saved")).toBeTruthy();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((row) => row.name === "sys_title")?.value).toBe("Home");
    expect(saved.fields.find((row) => row.name === "hours")?.value).toBe(
      JSON.stringify({ columns: ["day"], rows: [["Tue"]] }),
    );
    expect(screen.queryByTestId("editor-field-error-hours")).toBeNull();
    cleanup();
    renderTableHost(tableHost({ saveFields, hours }));
    await waitFor(() => {
      expect(
        (screen.getByTestId("editor-table-cell-hours-0-0") as HTMLInputElement).value,
      ).toBe("Tue");
    });
    expect(screen.getByRole("columnheader", { name: "day" })).toBeTruthy();
  });

  it("still saves cell text added to an empty required table", async () => {
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => ({
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      revision: 4,
      fields: body.fields,
    }));
    renderTableHost(tableHost({ saveFields, hours: "" }));
    await waitFor(() => {
      expect(screen.getByTestId("editor-table-add-hours")).toBeTruthy();
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
    expect(saved.fields.find((row) => row.name === "hours")?.value).toBe(
      JSON.stringify({ columns: ["value"], rows: [["Mon"]] }),
    );
    expect(screen.queryByTestId("editor-field-error-hours")).toBeNull();
    await waitFor(() => {
      expect(screen.getByTestId("editor-saved")).toBeTruthy();
    });
  });

  it("still saves an optional empty table", async () => {
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => ({
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      revision: 4,
      fields: body.fields,
    }));
    renderTableHost(tableHost({ saveFields, hours: "", required: false }));
    await waitFor(() => {
      expect(screen.getByTestId("editor-save")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-field-row-hours").getAttribute("data-required")).toBe(
      "false",
    );
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((row) => row.name === "hours")?.value).toBe("");
    expect(saved.fields.find((row) => row.name === "sys_title")?.value).toBe("Home");
    expect(screen.queryByTestId("editor-field-error-hours")).toBeNull();
    await waitFor(() => {
      expect(screen.getByTestId("editor-saved")).toBeTruthy();
    });
  });
});

describe("EditorHost refuse a table-cell NUL (#5417)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  function tableNulHost(opts: {
    saveFields?: (id: string, body: ItemEditorFields) => Promise<ItemEditorFields>;
    hours?: string;
    required?: boolean;
    confirmLeaveUnsaved?: (body: string) => boolean;
  }) {
    const hours = opts.hours ?? STORED_GRID;
    const required = opts.required === true;
    return (
      <EditorHost
        checkout={vi.fn().mockResolvedValue(undefined)}
        loadFields={async () => ({
          contentId: "42",
          contentType: "percPage",
          name: "Home",
          checkoutUser: "admin",
          revision: 3,
          fields: [
            { name: "sys_title", value: "Home" },
            { name: "hours", value: hours },
          ],
        })}
        saveFields={opts.saveFields ?? vi.fn()}
        loadType={async () => ({
          fields: [
            { name: "sys_title", label: "Title", control: "sys_EditBox" },
            {
              name: "hours",
              label: "Hours",
              control: "sys_Table",
              required,
            },
          ],
        })}
        confirmLeaveUnsaved={opts.confirmLeaveUnsaved}
      />
    );
  }

  function renderTableNulHost(element: React.ReactElement): void {
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route path="/editor" element={element} />
        </Routes>
      </MemoryRouter>,
    );
  }

  async function openStoredGrid(element: React.ReactElement): Promise<void> {
    renderTableNulHost(element);
    await waitFor(() => {
      expect(
        (screen.getByTestId("editor-table-cell-hours-0-0") as HTMLInputElement).value,
      ).toBe("Mon");
    });
  }

  it("refuses a table-cell NUL before PUT and keeps the previous cell after reload", async () => {
    const saveFields = vi.fn();
    await openStoredGrid(tableNulHost({ saveFields, required: true }));
    expect(screen.getByTestId("editor-field-hours").getAttribute("data-editor-kind")).toBe(
      "table",
    );
    fireEvent.change(screen.getByTestId("editor-table-cell-hours-0-0"), {
      target: { value: "Mon\u0000" },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-hours").textContent).toMatch(
        /table cell contains a character that cannot be saved/i,
      );
    });
    expect(screen.getByTestId("editor-field-error-hours").textContent).not.toBe(
      "This field is required.",
    );
    expect(screen.getByTestId("editor-save-error").textContent).toMatch(
      /table fields before saving/i,
    );
    expect(
      screen.getByTestId("editor-table-cell-hours-0-0").getAttribute("aria-invalid"),
    ).toBe("true");
    expect(document.activeElement).toBe(screen.getByTestId("editor-table-cell-hours-0-0"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-saved")).toBeNull();
    cleanup();
    await openStoredGrid(tableNulHost({ saveFields: vi.fn(), required: true }));
    expect(
      (screen.getByTestId("editor-table-cell-hours-0-0") as HTMLInputElement).value,
    ).toBe("Mon");
  });

  it("does not write when Close cancels an unsaved table-cell NUL", async () => {
    const saveFields = vi.fn();
    await openStoredGrid(
      tableNulHost({ saveFields, confirmLeaveUnsaved: () => false }),
    );
    fireEvent.change(screen.getByTestId("editor-table-cell-hours-0-0"), {
      target: { value: "Mon\u0000" },
    });
    fireEvent.click(screen.getByTestId("editor-close"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-saved")).toBeNull();
    cleanup();
    await openStoredGrid(tableNulHost({ saveFields: vi.fn() }));
    expect(
      (screen.getByTestId("editor-table-cell-hours-0-0") as HTMLInputElement).value,
    ).toBe("Mon");
  });

  it("still saves a normal cell and still refuses an empty required table", async () => {
    let hours = STORED_GRID;
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => {
      hours = body.fields.find((row) => row.name === "hours")?.value ?? hours;
      return {
        contentId: "42",
        contentType: "percPage",
        name: "Home",
        checkoutUser: "admin",
        revision: 4,
        fields: body.fields,
      };
    });
    await openStoredGrid(tableNulHost({ saveFields, hours, required: true }));
    fireEvent.change(screen.getByTestId("editor-table-cell-hours-0-0"), {
      target: { value: "Tue" },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-saved")).toBeTruthy();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    const sent = saved.fields.find((row) => row.name === "hours")?.value ?? "";
    expect(sent).toBe(JSON.stringify({ columns: ["day"], rows: [["Tue"]] }));
    expect(sent.includes("\u0000")).toBe(false);
    expect(screen.queryByTestId("editor-field-error-hours")).toBeNull();
    cleanup();
    renderTableNulHost(tableNulHost({ saveFields, hours, required: true }));
    await waitFor(() => {
      expect(
        (screen.getByTestId("editor-table-cell-hours-0-0") as HTMLInputElement).value,
      ).toBe("Tue");
    });
    fireEvent.click(screen.getByTestId("editor-table-remove-hours-0"));
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-hours").textContent).toBe(
        "This field is required.",
      );
    });
    expect(screen.getByTestId("editor-save-error").textContent).toMatch(
      /required fields before saving/i,
    );
    expect(saveFields).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId("editor-saved")).toBeNull();
  });

  it("does not treat HTTP 400 on a table save as success", async () => {
    const saveFields = vi.fn().mockRejectedValue({
      status: 400,
      body: { message: "rejected" },
    });
    await openStoredGrid(tableNulHost({ saveFields }));
    fireEvent.change(screen.getByTestId("editor-table-cell-hours-0-0"), {
      target: { value: "Tue" },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-save-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-saved")).toBeNull();
    expect(saveFields).toHaveBeenCalledTimes(1);
    const sent = (saveFields.mock.calls[0]?.[1] as ItemEditorFields).fields.find(
      (row) => row.name === "hours",
    )?.value;
    expect(sent).toBe(JSON.stringify({ columns: ["day"], rows: [["Tue"]] }));
    cleanup();
    await openStoredGrid(tableNulHost({ saveFields: vi.fn() }));
    expect(
      (screen.getByTestId("editor-table-cell-hours-0-0") as HTMLInputElement).value,
    ).toBe("Mon");
  });
});
