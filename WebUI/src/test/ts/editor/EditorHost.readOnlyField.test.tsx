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

const STORED_TITLE = "Welcome";

describe("EditorHost leave a schema read-only field unchanged (#5345)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  function readOnlyHost(opts: {
    saveFields?: (id: string, body: ItemEditorFields) => Promise<ItemEditorFields>;
    title?: string;
    readOnlyValue?: string;
    confirmLeaveUnsaved?: (message: string) => boolean;
  }) {
    const title = opts.title ?? "Home";
    const readOnlyValue = opts.readOnlyValue ?? STORED_TITLE;
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
            { name: "sys_title", value: title },
            { name: "displaytitle", value: readOnlyValue },
          ],
        })}
        saveFields={opts.saveFields ?? vi.fn()}
        loadType={async () => ({
          fields: [
            { name: "sys_title", label: "Title", control: "sys_EditBox" },
            {
              name: "displaytitle",
              label: "Display title",
              control: "sys_EditBox",
              readOnly: true,
            },
          ],
        })}
        confirmLeaveUnsaved={opts.confirmLeaveUnsaved}
      />
    );
  }

  async function openEditor(
    element: React.ReactElement,
    title = "Home",
    readOnlyValue = STORED_TITLE,
  ): Promise<{ title: HTMLInputElement; locked: HTMLInputElement }> {
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route path="/editor" element={element} />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect((screen.getByTestId("editor-field-sys_title") as HTMLInputElement).value).toBe(
        title,
      );
    });
    const locked = screen.getByTestId("editor-field-displaytitle") as HTMLInputElement;
    expect(locked.value).toBe(readOnlyValue);
    return {
      title: screen.getByTestId("editor-field-sys_title") as HTMLInputElement,
      locked,
    };
  }

  it("does not edit a schema read-only field and omits it from the fields PUT", async () => {
    let title = "Home";
    const readOnlyValue = STORED_TITLE;
    const saveFields = vi.fn(async (_id: string, payload: ItemEditorFields) => {
      title = payload.fields.find((field) => field.name === "sys_title")?.value ?? title;
      expect(payload.fields.find((field) => field.name === "displaytitle")).toBeUndefined();
      return {
        contentId: "42",
        contentType: "percPage",
        name: "Home",
        checkoutUser: "admin",
        revision: 4,
        fields: [
          { name: "sys_title", value: title },
          { name: "displaytitle", value: readOnlyValue },
        ],
      };
    });
    const fields = await openEditor(readOnlyHost({ saveFields }));
    expect(fields.locked.readOnly).toBe(true);
    expect(screen.queryByTestId("editor-text-clear-displaytitle")).toBeNull();
    fireEvent.change(fields.locked, { target: { value: "Hacked" } });
    expect(fields.locked.value).toBe(STORED_TITLE);
    fireEvent.change(fields.title, { target: { value: "Home page" } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-saved")).toBeTruthy();
    });
    const sent = saveFields.mock.calls[0][1] as ItemEditorFields;
    expect(sent.fields.map((field) => field.name)).toEqual(["sys_title"]);
    expect(sent.fields[0]?.value).toBe("Home page");
    expect(fields.locked.value).toBe(STORED_TITLE);
    expect(fields.title.value).toBe("Home page");
    cleanup();
    const reloaded = await openEditor(readOnlyHost({ saveFields, title }), "Home page");
    expect(reloaded.locked.value).toBe(STORED_TITLE);
    expect(reloaded.locked.readOnly).toBe(true);
  });

  it("does not write when Close cancels an edit of another field", async () => {
    const saveFields = vi.fn();
    const fields = await openEditor(
      readOnlyHost({ saveFields, confirmLeaveUnsaved: () => false }),
    );
    fireEvent.change(fields.title, { target: { value: "Home page" } });
    fireEvent.change(fields.locked, { target: { value: "Hacked" } });
    fireEvent.click(screen.getByTestId("editor-close"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-saved")).toBeNull();
    expect(fields.locked.value).toBe(STORED_TITLE);
    expect(fields.title.value).toBe("Home page");
    cleanup();
    const reloaded = await openEditor(readOnlyHost({ saveFields }));
    expect(reloaded.title.value).toBe("Home");
    expect(reloaded.locked.value).toBe(STORED_TITLE);
    expect(saveFields).not.toHaveBeenCalled();
  });

  it.each([
    [400, "bad request"],
    [403, "forbidden"],
    [409, "newer revision"],
  ])("does not claim success on HTTP %s and keeps the read-only value", async (status, detail) => {
    const saveFields = vi.fn().mockRejectedValue({
      status,
      body: { message: `Field 'sys_title' ${detail}.` },
    });
    const fields = await openEditor(readOnlyHost({ saveFields }));
    fireEvent.change(fields.locked, { target: { value: "Hacked" } });
    fireEvent.change(fields.title, { target: { value: "Home page" } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-save-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-saved")).toBeNull();
    expect(saveFields).toHaveBeenCalledTimes(1);
    const sent = saveFields.mock.calls[0][1] as ItemEditorFields;
    expect(sent.fields.find((field) => field.name === "displaytitle")).toBeUndefined();
    expect(fields.locked.value).toBe(STORED_TITLE);
    cleanup();
    const reloaded = await openEditor(readOnlyHost({ saveFields }));
    expect(reloaded.title.value).toBe("Home");
    expect(reloaded.locked.value).toBe(STORED_TITLE);
  });
});
