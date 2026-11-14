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
import type {
  ItemEditorBinaryMeta,
} from "../../../main/ts/editor/itemBinaryApi";
import type { ItemEditorFields } from "../../../main/ts/editor/itemFieldsApi";

const fields: ItemEditorFields = {
  contentId: "42",
  contentType: "percImage",
  name: "Hero",
  checkoutUser: "admin",
  revision: 4,
  fields: [
    { name: "sys_title", value: "Hero" },
    { name: "img", value: "hero.png" },
    { name: "img_alt", value: "Stored alt" },
  ],
};

const schemaFields = [
  { name: "sys_title", label: "Title" },
  { name: "img", label: "Image", control: "sys_webImageFX", required: true },
];

function binaryMeta(): ItemEditorBinaryMeta {
  return {
    contentId: "42",
    field: "img",
    filename: "hero.png",
    contentType: "image/png",
    present: true,
  };
}

function renderHost({
  loadFields,
  saveFields,
  loadType,
  schema = schemaFields,
  confirmLeaveUnsaved,
}: {
  loadFields: (id: string) => Promise<ItemEditorFields>;
  saveFields?: (id: string, body: ItemEditorFields) => Promise<ItemEditorFields>;
  loadType?: () => Promise<{ fields: typeof schemaFields }>;
  schema?: typeof schemaFields;
  confirmLeaveUnsaved?: (body: string) => boolean;
}) {
  render(
    <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
      <Routes>
        <Route
          path="/editor"
          element={
            <EditorHost
              checkout={vi.fn().mockResolvedValue({
                checkOutUser: "admin",
                currentUser: "admin",
              })}
              loadFields={loadFields}
              saveFields={saveFields ?? vi.fn().mockResolvedValue(fields)}
              loadType={loadType ?? (async () => ({ fields: schema }))}
              loadBinaryMeta={async () => binaryMeta()}
              confirmLeaveUnsaved={confirmLeaveUnsaved}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

async function openForm(): Promise<void> {
  await waitFor(() => {
    expect(screen.getByTestId("editor-form")).toBeTruthy();
  });
  await waitFor(() => {
    expect(screen.getByTestId("editor-image-alt-img")).toBeTruthy();
  });
}

function altInput(): HTMLInputElement {
  return screen.getByTestId("editor-image-alt-img") as HTMLInputElement;
}

describe("EditorHost image alternative text (#6263)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders the stored alt on the image row with a label association", async () => {
    renderHost({ loadFields: async () => fields });
    await openForm();
    expect(altInput().value).toBe("Stored alt");
    expect(altInput().getAttribute("aria-label")).toBe("Alternative text");
    // The row is an implicit <label>: the input sits inside the labelled row.
    expect(
      screen.getByTestId("editor-field-row-img").textContent,
    ).toContain("Image");
    // The sibling text entry hides behind the image row instead of doubling it.
    expect(screen.queryByTestId("editor-field-img_alt")).toBeNull();
    expect(screen.getByTestId("editor-field-img")).toHaveAttribute(
      "data-editor-kind",
      "image",
    );
  });

  it("refuses a blank alt on a required image before the PUT with a named summary entry", async () => {
    const saveFields = vi.fn();
    renderHost({ loadFields: async () => fields, saveFields });
    await openForm();
    fireEvent.change(altInput(), { target: { value: "   " } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-summary")).toBeTruthy();
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("editor-field-error-img").textContent).toMatch(
      /required/i,
    );
    expect(
      screen.getByTestId("editor-field-error-summary-img").textContent,
    ).toContain("Image");
    expect(screen.queryByTestId("editor-saved")).toBeNull();
    // The refused save keeps the typed value; no PUT means storage is intact.
    expect(altInput().value).toBe("   ");
  });

  it("saves a non-blank alt through the fields PUT sibling entry", async () => {
    const reloaded: ItemEditorFields = {
      ...fields,
      revision: 5,
      fields: [
        { name: "sys_title", value: "Hero" },
        { name: "img", value: "hero.png" },
        { name: "img_alt", value: "A hero at dawn" },
      ],
    };
    const saveFields = vi.fn().mockResolvedValue(reloaded);
    renderHost({
      loadFields: vi
        .fn()
        .mockResolvedValueOnce(fields)
        .mockResolvedValue(reloaded),
      saveFields,
    });
    await openForm();
    fireEvent.change(altInput(), { target: { value: "A hero at dawn" } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-saved")).toBeTruthy();
    });
    expect(saveFields).toHaveBeenCalledTimes(1);
    const body = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(
      body.fields.find((field) => field.name === "img_alt")?.value,
    ).toBe("A hero at dawn");
    await waitFor(() => {
      expect(altInput().value).toBe("A hero at dawn");
    });
  });

  it("leaves an optional image with a blank alt alone and still saves", async () => {
    const saveFields = vi.fn().mockResolvedValue(fields);
    renderHost({
      loadFields: async () => fields,
      saveFields,
      schema: [
        { name: "sys_title", label: "Title" },
        { name: "img", label: "Image", control: "sys_webImageFX" },
      ],
    });
    await openForm();
    fireEvent.change(altInput(), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-saved")).toBeTruthy();
    });
    expect(saveFields).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId("editor-field-error-img")).toBeNull();
  });

  it("keeps the existing save behavior when the type defines no alt sibling", async () => {
    const withoutAlt: ItemEditorFields = {
      ...fields,
      fields: [
        { name: "sys_title", value: "Hero" },
        { name: "img", value: "hero.png" },
      ],
    };
    const saveFields = vi.fn().mockResolvedValue(withoutAlt);
    renderHost({ loadFields: async () => withoutAlt, saveFields });
    await openForm();
    // The alt input still renders, but nothing gates the save.
    expect(altInput().value).toBe("");
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-saved")).toBeTruthy();
    });
    expect(saveFields).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId("editor-field-error-img")).toBeNull();
  });

  it("does not write on close after an unsaved alt edit", async () => {
    const saveFields = vi.fn();
    renderHost({
      loadFields: async () => fields,
      saveFields,
      confirmLeaveUnsaved: () => false,
    });
    await openForm();
    fireEvent.change(altInput(), { target: { value: "Unsaved alt" } });
    fireEvent.click(screen.getByTestId("editor-close"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("editor-form")).toBeTruthy();
    expect(altInput().value).toBe("Unsaved alt");
  });

  it("maps an HTTP 400 on the alt save onto the banner without claiming success", async () => {
    const saveFields = vi.fn().mockRejectedValue({ status: 400, message: "rejected" });
    renderHost({ loadFields: async () => fields, saveFields });
    await openForm();
    fireEvent.change(altInput(), { target: { value: "A hero at dawn" } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-save-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-saved")).toBeNull();
    expect(altInput().value).toBe("A hero at dawn");
  });
});
