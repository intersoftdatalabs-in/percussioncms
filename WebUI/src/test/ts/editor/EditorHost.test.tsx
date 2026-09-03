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
import { AppRoutes } from "../../../main/ts/app/routes";
import {
  EditorHost,
  fieldValueAsString,
  mergeEditorRows,
} from "../../../main/ts/editor/EditorHost";
import type { ItemEditorFields } from "../../../main/ts/editor/itemFieldsApi";

const fields: ItemEditorFields = {
  contentId: "42",
  contentType: "percPage",
  name: "Home",
  checkoutUser: "admin",
  fields: [
    { name: "sys_title", value: "Home" },
    { name: "displaytitle", value: "Welcome" },
  ],
};

describe("EditorHost", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("asks for an item when contentId is missing", () => {
    render(
      <MemoryRouter initialEntries={["/editor"]}>
        <Routes>
          <Route path="/editor" element={<EditorHost />} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByTestId("editor-overlay")).toBeTruthy();
    expect(screen.getByTestId("editor-error").textContent).toMatch(/Explorer or Home/i);
  });

  it("surfaces linkback warningMessage when contentId is missing", () => {
    render(
      <MemoryRouter
        initialEntries={[
          "/editor?warningMessage=The page you are attempting to reach, does not exist in the CMS.",
        ]}
      >
        <Routes>
          <Route path="/editor" element={<EditorHost />} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByTestId("editor-error").textContent).toMatch(/does not exist in the CMS/i);
  });

  it("loads fields after checkout and saves edits", async () => {
    const checkout = vi.fn().mockResolvedValue(undefined);
    const loadFields = vi.fn().mockResolvedValue(fields);
    const saveFields = vi.fn().mockResolvedValue({
      ...fields,
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "displaytitle", value: "Updated" },
      ],
    });
    const loadType = vi.fn().mockResolvedValue({
      fields: [{ name: "displaytitle", label: "Display title", readOnly: false }],
    });
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={checkout}
                loadFields={loadFields}
                saveFields={saveFields}
                loadType={loadType}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(checkout).toHaveBeenCalledWith("42");
    expect(screen.getByTestId("editor-content-type").textContent).toContain("percPage");
    fireEvent.change(screen.getByTestId("editor-field-displaytitle"), {
      target: { value: "Updated" },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "displaytitle")?.value).toBe("Updated");
  });

  it("does not checkout in view mode", async () => {
    const checkout = vi.fn();
    const loadFields = vi.fn().mockResolvedValue(fields);
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={checkout}
                loadFields={loadFields}
                loadType={async () => ({ fields: [] })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(checkout).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-save")).toBeNull();
  });

  it("AppRoutes mounts editor outside BrandBar chrome", () => {
    render(
      <MemoryRouter initialEntries={["/editor"]}>
        <AppRoutes />
      </MemoryRouter>,
    );
    expect(screen.getByTestId("editor-host")).toBeTruthy();
    expect(screen.queryByTestId("perc-spa-app")).toBeNull();
  });

  it("shows the editor error when checkout fails instead of crashing", async () => {
    const checkout = vi.fn().mockRejectedValue(new Error("CONTENTSTATUS lock"));
    const loadFields = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={<EditorHost checkout={checkout} loadFields={loadFields} />}
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-error")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-error").textContent).toMatch(
      /Could not load this item for editing/i,
    );
    expect(screen.getByTestId("editor-error").textContent).toMatch(/CONTENTSTATUS lock/);
    expect(screen.getByTestId("editor-host")).toBeTruthy();
    expect(screen.queryByTestId("editor-form")).toBeNull();
    expect(loadFields).not.toHaveBeenCalled();
  });
});

describe("fieldValueAsString", () => {
  it("coerces numbers and nulls to strings", () => {
    expect(fieldValueAsString(42)).toBe("42");
    expect(fieldValueAsString(null)).toBe("");
    expect(fieldValueAsString("news")).toBe("news");
  });
});

describe("mergeEditorRows", () => {
  it("uses content-type labels", () => {
    const rows = mergeEditorRows(fields, [
      { name: "displaytitle", label: "Display title", readOnly: false },
    ]);
    expect(rows.find((r) => r.name === "displaytitle")?.label).toBe("Display title");
    expect(rows.find((r) => r.name === "sys_title")?.label).toBe("sys_title");
  });
});

describe("EditorHost rich controls", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders TinyMCE / file / keyword / community widgets and uploads binary on save", async () => {
    const checkout = vi.fn().mockResolvedValue(undefined);
    const loadFields = vi.fn().mockResolvedValue({
      contentId: "42",
      contentType: "percRichText",
      name: "Intro",
      checkoutUser: "admin",
      fields: [
        { name: "sys_title", value: "Intro" },
        { name: "text", value: "<p>Hi</p>" },
        { name: "keywords", value: "news" },
        { name: "sys_communityid", value: "10" },
      ],
    });
    const saveFields = vi.fn().mockResolvedValue({
      contentId: "42",
      contentType: "percRichText",
      name: "Intro",
      checkoutUser: "admin",
      fields: [
        { name: "sys_title", value: "Intro" },
        { name: "text", value: "<p>Hi</p>" },
        { name: "keywords", value: "events" },
        { name: "sys_communityid", value: "20" },
      ],
    });
    const uploadBinary = vi.fn().mockResolvedValue({});
    const loadType = vi.fn().mockResolvedValue({
      fields: [
        { name: "sys_title", label: "Title", control: "sys_EditBox" },
        { name: "text", label: "Body", control: "sys_tinymce" },
        { name: "img", label: "Image", control: "sys_webImageFX" },
        { name: "keywords", label: "Keywords", control: "sys_DropDownSingle" },
        { name: "sys_communityid", label: "Community", control: "sys_DropDownSingle" },
      ],
    });
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={checkout}
                loadFields={loadFields}
                saveFields={saveFields}
                loadType={loadType}
                uploadBinary={uploadBinary}
                loadKeywords={async () => [
                  {
                    value: "keywords",
                    choices: [
                      { value: "news", label: "News" },
                      { value: "events", label: "Events" },
                    ],
                  },
                ]}
                loadCommunities={async () => [
                  { id: 10, name: "Default" },
                  { id: 20, name: "Enterprise" },
                ]}
                loadBinaryMeta={async () => ({
                  contentId: "42",
                  field: "img",
                  filename: "",
                  contentType: "",
                  present: false,
                })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-field-text").getAttribute("data-editor-kind")).toBe(
      "html",
    );
    expect(screen.getByTestId("editor-field-img").getAttribute("data-editor-kind")).toBe(
      "image",
    );
    expect(screen.getByTestId("editor-field-keywords").getAttribute("data-editor-kind")).toBe(
      "keyword",
    );
    expect(
      screen.getByTestId("editor-field-sys_communityid").getAttribute("data-editor-kind"),
    ).toBe("community");
    await waitFor(() => {
      expect(screen.getByText("Events")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-field-keywords"), {
      target: { value: "events" },
    });
    fireEvent.change(screen.getByTestId("editor-field-sys_communityid"), {
      target: { value: "20" },
    });
    const file = new File(["x"], "hero.png", { type: "image/png" });
    fireEvent.change(screen.getByTestId("editor-file-img"), {
      target: { files: [file] },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "keywords")?.value).toBe("events");
    expect(saved.fields.find((f) => f.name === "sys_communityid")?.value).toBe("20");
    expect(saved.fields.find((f) => f.name === "img")).toBeUndefined();
    expect(uploadBinary).toHaveBeenCalledWith("42", "img", file);
  });

  it("opens the promote form without checkout", async () => {
    const checkout = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=promote"]}>
        <Routes>
          <Route
            path="/editor"
            element={<EditorHost checkout={checkout} loadFields={vi.fn()} />}
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-promote-form")).toBeTruthy();
    });
    expect(checkout).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-save")).toBeNull();
  });

  it("renders keyword select when field and catalog values are numbers", async () => {
    const checkout = vi.fn().mockResolvedValue(undefined);
    const loadFields = vi.fn().mockResolvedValue({
      contentId: "42",
      contentType: "percRichText",
      name: "Intro",
      checkoutUser: "admin",
      fields: [
        { name: "sys_title", value: "Intro" },
        { name: "keywords", value: 7 as unknown as string },
      ],
    });
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={checkout}
                loadFields={loadFields}
                loadType={async () => ({
                  fields: [
                    {
                      name: "keywords",
                      label: "Keywords",
                      control: "sys_DropDownSingle",
                    },
                  ],
                })}
                loadKeywords={async () =>
                  [
                    {
                      value: 7,
                      label: "keywords",
                      choices: [
                        { value: 7, label: "Seven" },
                        { value: 8, label: "Eight" },
                      ],
                    },
                  ] as never
                }
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    await waitFor(() => {
      expect(screen.getByText("Seven")).toBeTruthy();
    });
    const select = screen.getByTestId("editor-field-keywords") as HTMLSelectElement;
    expect(select.getAttribute("data-editor-kind")).toBe("keyword");
    expect(select.value).toBe("7");
    expect(screen.queryByTestId("editor-error")).toBeNull();
  });
});
