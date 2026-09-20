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

  it("loads fields view-only when checkout fails and maps the lock error", async () => {
    const checkout = vi.fn().mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "checked out" },
    });
    const loadFields = vi.fn().mockResolvedValue({
      ...fields,
      checkoutUser: "editor",
    });
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
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(loadFields).toHaveBeenCalledWith("42");
    expect(screen.getByTestId("editor-lock-error").textContent).toMatch(
      /checked out to another user/i,
    );
    expect(screen.getByTestId("editor-locked")).toBeTruthy();
    expect(screen.queryByTestId("editor-save")).toBeNull();
    expect(screen.getByTestId("editor-checkout")).toBeTruthy();
    expect(screen.getByTestId("editor-field-displaytitle")).toHaveProperty("readOnly", true);
  });

  it("maps 403 on check-in and does not treat it as success", async () => {
    const checkout = vi.fn().mockResolvedValue({
      checkOutUser: "admin",
      currentUser: "admin",
    });
    const checkin = vi.fn().mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: {},
    });
    const loadFields = vi.fn().mockResolvedValue(fields);
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={checkout}
                checkin={checkin}
                loadFields={loadFields}
                loadType={async () => ({ fields: [] })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-checkin")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-checkin"));
    await waitFor(() => {
      expect(checkin).toHaveBeenCalledWith("42");
    });
    expect(screen.getByTestId("editor-lock-error").textContent).toMatch(
      /not allowed to check in/i,
    );
  });

  it("restore toggle loads revisions, restore revives fields, and 403 is not success", async () => {
    const checkout = vi.fn().mockResolvedValue(undefined);
    const loadFields = vi
      .fn()
      .mockResolvedValueOnce(fields)
      .mockResolvedValueOnce({
        ...fields,
        fields: [
          { name: "sys_title", value: "Home (restored)" },
          { name: "displaytitle", value: "Restored welcome" },
        ],
      });
    const loadRevisions = vi.fn().mockResolvedValue({
      restorable: true,
      revisions: [
        {
          revId: 5,
          status: "Quick Edit",
          lastModifier: "admin",
          lastModifiedDate: "2026-04-12",
        },
      ],
      comments: [],
    });
    const restoreRevision = vi.fn().mockResolvedValue(undefined);
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={checkout}
                loadFields={loadFields}
                loadType={async () => ({ fields: [] })}
                loadRevisions={loadRevisions}
                restoreRevision={restoreRevision}
                confirmRestore={() => true}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-restore-toggle"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-restore-panel")).toBeTruthy();
    });
    expect(loadRevisions).toHaveBeenCalledWith("42");
    expect(screen.getByTestId("editor-restore-select")).toBeTruthy();
    fireEvent.change(screen.getByTestId("editor-restore-select"), {
      target: { value: "5" },
    });
    fireEvent.click(screen.getByTestId("editor-restore-confirm"));
    await waitFor(() => {
      expect(restoreRevision).toHaveBeenCalledWith("42", 5);
    });
    expect(loadFields).toHaveBeenCalledTimes(2);
    await waitFor(() => {
      expect(screen.getByTestId("editor-restore-done")).toBeTruthy();
    });
    const titleField = screen.getByTestId(
      "editor-field-sys_title",
    ) as HTMLInputElement;
    expect(titleField.value).toBe("Home (restored)");
  });

  it("restore shows forbidden copy when REST returns 403 and does not refresh fields", async () => {
    const checkout = vi.fn().mockResolvedValue(undefined);
    const loadFields = vi.fn().mockResolvedValue(fields);
    const loadRevisions = vi.fn().mockResolvedValue({
      restorable: true,
      revisions: [
        {
          revId: 3,
          status: "Live",
          lastModifier: "admin",
          lastModifiedDate: "2026-04-10",
        },
      ],
      comments: [],
    });
    const restoreRevision = vi.fn().mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "not allowed" },
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
                loadType={async () => ({ fields: [] })}
                loadRevisions={loadRevisions}
                restoreRevision={restoreRevision}
                confirmRestore={() => true}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-restore-toggle"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-restore-panel")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-restore-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-restore-error")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-restore-error").textContent).toMatch(
      /not allowed to restore/i,
    );
    expect(loadFields).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId("editor-restore-done")).toBeNull();
  });

  it("restore maps 404 to the not-found copy and shows it in the panel", async () => {
    const checkout = vi.fn().mockResolvedValue(undefined);
    const loadFields = vi.fn().mockResolvedValue(fields);
    const loadRevisions = vi.fn().mockResolvedValue({
      restorable: true,
      revisions: [
        {
          revId: 9,
          status: "Live",
          lastModifier: "admin",
          lastModifiedDate: "2026-04-10",
        },
      ],
      comments: [],
    });
    const restoreRevision = vi.fn().mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "no such revision" },
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
                loadType={async () => ({ fields: [] })}
                loadRevisions={loadRevisions}
                restoreRevision={restoreRevision}
                confirmRestore={() => true}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-restore-toggle"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-restore-panel")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-restore-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-restore-error")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-restore-error").textContent).toMatch(
      /was not found/i,
    );
    expect(loadFields).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId("editor-restore-done")).toBeNull();
  });

  it("does not show the restore toggle in view or promote modes", async () => {
    const loadFields = vi.fn().mockResolvedValue(fields);
    const viewRender = render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
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
    expect(screen.queryByTestId("editor-restore-toggle")).toBeNull();
    viewRender.unmount();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=promote"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                loadFields={loadFields}
                loadType={async () => ({ fields: [] })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-promote-form")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-restore-toggle")).toBeNull();
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

describe("EditorHost date calendar fields (#4569)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders a date widget, persists yyyy-MM-dd, and maps 400 onto the field", async () => {
    const checkout = vi.fn().mockResolvedValue(undefined);
    const loadFields = vi.fn().mockResolvedValue({
      contentId: "42",
      contentType: "percEvent",
      name: "Event",
      checkoutUser: "admin",
      fields: [
        { name: "sys_title", value: "Event" },
        { name: "sys_contentstartdate", value: "2026-01-01" },
      ],
    });
    const saveFields = vi.fn().mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: {
        Error: {
          message: "sys_contentstartdate is not a valid date",
          errorData: "sys_contentstartdate",
        },
      },
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
                loadType={async () => ({
                  fields: [
                    { name: "sys_title", label: "Title", control: "sys_EditBox" },
                    {
                      name: "sys_contentstartdate",
                      label: "Start",
                      control: "sys_CalendarSimple",
                    },
                  ],
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
    const input = screen.getByTestId("editor-field-sys_contentstartdate") as HTMLInputElement;
    expect(input.getAttribute("data-editor-kind")).toBe("date");
    expect(input.type).toBe("date");
    fireEvent.change(input, { target: { value: "2026-09-18" } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "sys_contentstartdate")?.value).toBe(
      "2026-09-18",
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-sys_contentstartdate")).toBeTruthy();
    });
  });

  it("keeps date widgets read-only in view mode", async () => {
    const checkout = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={checkout}
                loadFields={async () => ({
                  contentId: "42",
                  contentType: "percEvent",
                  name: "Event",
                  checkoutUser: "",
                  fields: [{ name: "sys_contentstartdate", value: "2026-09-18" }],
                })}
                loadType={async () => ({
                  fields: [
                    {
                      name: "sys_contentstartdate",
                      label: "Start",
                      control: "sys_CalendarSimple",
                    },
                  ],
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
    const input = screen.getByTestId("editor-field-sys_contentstartdate") as HTMLInputElement;
    expect(input.readOnly || input.disabled).toBe(true);
    expect(checkout).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-save")).toBeNull();
  });
});

describe("EditorHost workflow transitions (#4539)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  function titleType() {
    return { fields: [{ name: "sys_title", label: "Title", readOnly: false }] };
  }

  function renderEdit(extra: Partial<React.ComponentProps<typeof EditorHost>> = {}) {
    const checkout = extra.checkout ?? vi.fn().mockResolvedValue(undefined);
    const loadFields = extra.loadFields ?? vi.fn().mockResolvedValue(fields);
    return render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={checkout}
                loadFields={loadFields}
                loadType={extra.loadType ?? (async () => titleType())}
                loadTransitions={
                  extra.loadTransitions ??
                  (async () => ({
                    stateName: "Draft",
                    transitionTriggers: ["Submit", "Reject"],
                  }))
                }
                runTransition={extra.runTransition}
                commentRequiredTriggers={extra.commentRequiredTriggers}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
  }

  it("lists allowed triggers in edit mode and runs Submit with a comment", async () => {
    const runTransition = vi.fn().mockResolvedValue({});
    const loadTransitions = vi
      .fn()
      .mockResolvedValueOnce({
        stateName: "Draft",
        transitionTriggers: ["Submit", "Reject"],
      })
      .mockResolvedValue({
        stateName: "Review",
        transitionTriggers: ["Approve"],
      });
    renderEdit({ runTransition, loadTransitions });
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-workflow-state").textContent).toMatch(/Draft/);
    expect(screen.getByTestId("editor-workflow-trigger-Submit")).toBeTruthy();
    expect(screen.getByTestId("editor-workflow-trigger-Reject")).toBeTruthy();
    expect(screen.queryByTestId("editor-workflow-trigger-Approve")).toBeNull();
    fireEvent.change(screen.getByTestId("editor-workflow-comment"), {
      target: { value: "ready" },
    });
    fireEvent.click(screen.getByTestId("editor-workflow-trigger-Submit"));
    await waitFor(() => {
      expect(runTransition).toHaveBeenCalledWith("42", "Submit", "ready");
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-done")).toBeTruthy();
    });
  });

  it("blocks comment-required Reject until a comment is present", async () => {
    const runTransition = vi.fn().mockResolvedValue({});
    renderEdit({ runTransition });
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-trigger-Reject")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-workflow-trigger-Reject"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-error").textContent).toMatch(
        /Enter a comment/i,
      );
    });
    expect(runTransition).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("editor-workflow-comment"), {
      target: { value: "needs work" },
    });
    fireEvent.click(screen.getByTestId("editor-workflow-trigger-Reject"));
    await waitFor(() => {
      expect(runTransition).toHaveBeenCalledWith("42", "Reject", "needs work");
    });
  });

  it("does not run a trigger that is not in the allowlist", async () => {
    const runTransition = vi.fn().mockResolvedValue({});
    renderEdit({
      runTransition,
      loadTransitions: async () => ({
        stateName: "Draft",
        transitionTriggers: ["Submit"],
      }),
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-trigger-Submit")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-workflow-trigger-Approve")).toBeNull();
    expect(runTransition).not.toHaveBeenCalled();
  });

  it("hides workflow controls in view mode", async () => {
    const loadTransitions = vi.fn();
    const runTransition = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                loadTransitions={loadTransitions}
                runTransition={runTransition}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-workflow")).toBeNull();
    expect(loadTransitions).not.toHaveBeenCalled();
  });
});

describe("EditorHost publish now (#4540)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  function titleType() {
    return { fields: [{ name: "sys_title", label: "Title", readOnly: false }] };
  }

  it("confirms then demand-publishes a percPage in edit mode", async () => {
    const publishItem = vi.fn().mockResolvedValue(true);
    const confirmPublish = vi.fn().mockReturnValue(true);
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                publishItem={publishItem}
                confirmPublish={confirmPublish}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-publish-now")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-publish-now"));
    await waitFor(() => {
      expect(confirmPublish).toHaveBeenCalled();
    });
    await waitFor(() => {
      expect(publishItem).toHaveBeenCalledWith("42", "page");
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-publish-done")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-publish-error")).toBeNull();
  });

  it("does not publish when the confirm is cancelled", async () => {
    const publishItem = vi.fn().mockResolvedValue(true);
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                publishItem={publishItem}
                confirmPublish={() => false}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-publish-now")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-publish-now"));
    await waitFor(() => {
      expect(publishItem).not.toHaveBeenCalled();
    });
    expect(screen.queryByTestId("editor-publish-done")).toBeNull();
  });

  it("surfaces FORBIDDEN as failure, not success", async () => {
    const publishItem = vi.fn().mockRejectedValue(new Error("FORBIDDEN"));
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                publishItem={publishItem}
                confirmPublish={() => true}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-publish-now")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-publish-now"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-publish-error").textContent).toMatch(
        /FORBIDDEN/,
      );
    });
    expect(screen.queryByTestId("editor-publish-done")).toBeNull();
  });

  it("hides Publish now in view mode", async () => {
    const publishItem = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                publishItem={publishItem}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-publish-now")).toBeNull();
    expect(publishItem).not.toHaveBeenCalled();
  });

  it("demand-publishes percRichText as an asset", async () => {
    const publishItem = vi.fn().mockResolvedValue(true);
    render(
      <MemoryRouter initialEntries={["/editor?contentId=99&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue({
                  contentId: "99",
                  contentType: "percRichText",
                  name: "Intro",
                  checkoutUser: "admin",
                  fields: [{ name: "sys_title", value: "Intro" }],
                })}
                loadType={async () => titleType()}
                publishItem={publishItem}
                confirmPublish={() => true}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-publish-now")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-publish-now"));
    await waitFor(() => {
      expect(publishItem).toHaveBeenCalledWith("99", "asset");
    });
  });
});

describe("EditorHost required field save errors (#4541)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  const requiredType = {
    fields: [
      { name: "sys_title", label: "Title", readOnly: false, required: true },
      { name: "displaytitle", label: "Display title", readOnly: false, required: true },
    ],
  };

  it("shows inline required errors on save and does not PUT", async () => {
    const saveFields = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue({
                  ...fields,
                  fields: [
                    { name: "sys_title", value: "Home" },
                    { name: "displaytitle", value: "" },
                  ],
                })}
                saveFields={saveFields}
                loadType={async () => requiredType}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-displaytitle")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-save-error").textContent).toMatch(
      /required fields before saving/i,
    );
    expect(screen.getByTestId("editor-form")).toBeTruthy();
    expect(saveFields).not.toHaveBeenCalled();
  });

  it("maps a save 400 onto the named field and keeps the form", async () => {
    const saveFields = vi.fn().mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: {
        Error: {
          message: "displaytitle is invalid",
          errorData: "displaytitle",
        },
      },
    });
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
                loadType={async () => requiredType}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-displaytitle").textContent).toMatch(
        /displaytitle/i,
      );
    });
    expect(screen.getByTestId("editor-save-error")).toBeTruthy();
    expect(screen.getByTestId("editor-form")).toBeTruthy();
    expect(screen.queryByTestId("editor-error")).toBeNull();
  });

  it("blocks check-in when a required field is empty", async () => {
    const checkin = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue({
                  ...fields,
                  fields: [
                    { name: "sys_title", value: "Home" },
                    { name: "displaytitle", value: "  " },
                  ],
                })}
                checkin={checkin}
                loadType={async () => requiredType}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-checkin")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-checkin"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-displaytitle")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-save-error").textContent).toMatch(
      /required fields before checking in/i,
    );
    expect(checkin).not.toHaveBeenCalled();
  });
});

describe("EditorHost preview assembled item (#4568)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  function titleType() {
    return { fields: [{ name: "sys_title", label: "Title", readOnly: false }] };
  }

  it("previews in view mode without a dirty confirm", async () => {
    const previewItem = vi.fn().mockResolvedValue(undefined);
    const confirmUnsavedPreview = vi.fn().mockReturnValue(false);
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                previewItem={previewItem}
                confirmUnsavedPreview={confirmUnsavedPreview}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-preview")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-preview"));
    await waitFor(() => {
      expect(previewItem).toHaveBeenCalledWith("42", "page");
    });
    expect(confirmUnsavedPreview).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId("editor-preview-done")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-preview-error")).toBeNull();
  });

  it("confirms unsaved edits then previews the last saved revision", async () => {
    const previewItem = vi.fn().mockResolvedValue(undefined);
    const confirmUnsavedPreview = vi.fn().mockReturnValue(true);
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                previewItem={previewItem}
                confirmUnsavedPreview={confirmUnsavedPreview}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-sys_title")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-field-sys_title"), {
      target: { value: "Draft title" },
    });
    fireEvent.click(screen.getByTestId("editor-preview"));
    await waitFor(() => {
      expect(confirmUnsavedPreview).toHaveBeenCalled();
    });
    await waitFor(() => {
      expect(previewItem).toHaveBeenCalledWith("42", "page");
    });
  });

  it("does not preview when the unsaved confirm is cancelled", async () => {
    const previewItem = vi.fn().mockResolvedValue(undefined);
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                previewItem={previewItem}
                confirmUnsavedPreview={() => false}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-sys_title")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-field-sys_title"), {
      target: { value: "Draft title" },
    });
    fireEvent.click(screen.getByTestId("editor-preview"));
    await waitFor(() => {
      expect(previewItem).not.toHaveBeenCalled();
    });
    expect(screen.queryByTestId("editor-preview-done")).toBeNull();
  });

  it("surfaces FORBIDDEN as failure, not success", async () => {
    const previewItem = vi.fn().mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { Error: { message: "FORBIDDEN" } },
    });
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                previewItem={previewItem}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-preview")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-preview"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-preview-error").textContent).toMatch(
        /FORBIDDEN/,
      );
    });
    expect(screen.queryByTestId("editor-preview-done")).toBeNull();
  });

  it("hides Preview in promote mode", async () => {
    const previewItem = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=promote"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost checkout={vi.fn()} loadFields={vi.fn()} previewItem={previewItem} />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-promote-form")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-preview")).toBeNull();
    expect(previewItem).not.toHaveBeenCalled();
  });

  it("previews percRichText as an asset", async () => {
    const previewItem = vi.fn().mockResolvedValue(undefined);
    render(
      <MemoryRouter initialEntries={["/editor?contentId=99&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={vi.fn().mockResolvedValue({
                  contentId: "99",
                  contentType: "percRichText",
                  name: "Intro",
                  checkoutUser: "admin",
                  fields: [{ name: "sys_title", value: "Intro" }],
                })}
                loadType={async () => titleType()}
                previewItem={previewItem}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-preview")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-preview"));
    await waitFor(() => {
      expect(previewItem).toHaveBeenCalledWith("99", "asset");
    });
  });
});

describe("EditorHost new copy / promotable version (#4570)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  function titleType() {
    return {
      fields: [{ name: "sys_title", label: "Title", readOnly: false }],
    };
  }

  it("lands on the new item id after new copy", async () => {
    const copyItem = vi.fn().mockResolvedValue({
      itemId: "99",
      folderPath: "//Sites/Demo",
      promotable: false,
    });
    const copyPromotable = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                copyItem={copyItem}
                copyPromotable={copyPromotable}
                confirmCopy={() => true}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-new-copy")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-new-copy"));
    await waitFor(() => {
      expect(copyItem).toHaveBeenCalledWith("42");
    });
    expect(copyPromotable).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId("editor-content-id").textContent).toMatch(/99/);
    });
  });

  it("lands on the promotable version id", async () => {
    const copyPromotable = vi.fn().mockResolvedValue({
      itemId: "1-101-77",
      folderPath: "//Sites/Demo",
      promotable: true,
    });
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                copyPromotable={copyPromotable}
                confirmCopy={() => true}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-promotable-version")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-promotable-version"));
    await waitFor(() => {
      expect(copyPromotable).toHaveBeenCalledWith("42");
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-content-id").textContent).toMatch(/77/);
    });
  });

  it("does not copy when confirm is cancelled", async () => {
    const copyItem = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                copyItem={copyItem}
                confirmCopy={() => false}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-new-copy")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-new-copy"));
    await waitFor(() => {
      expect(copyItem).not.toHaveBeenCalled();
    });
  });

  it("surfaces HTTP 403 as forbidden, not success", async () => {
    const copyItem = vi.fn().mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: {},
    });
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                copyItem={copyItem}
                confirmCopy={() => true}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-new-copy")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-new-copy"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-copy-error").textContent).toMatch(
        /not allowed to copy/i,
      );
    });
    expect(screen.getByTestId("editor-content-id").textContent).toMatch(/42/);
  });

  it("surfaces HTTP 404 as not found, not success", async () => {
    const copyItem = vi.fn().mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: {},
    });
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                copyItem={copyItem}
                confirmCopy={() => true}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-new-copy")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-new-copy"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-copy-error").textContent).toMatch(/not found/i);
    });
    expect(screen.getByTestId("editor-content-id").textContent).toMatch(/42/);
  });

  it("hides copy actions in view mode", async () => {
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-new-copy")).toBeNull();
    expect(screen.queryByTestId("editor-promotable-version")).toBeNull();
  });
});
