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

/**
 * Invalid editor controls name their inline error text through
 * {@code aria-describedby} (#6933). Screen readers announce the message as
 * the field's description; valid rows carry no dangling reference.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { describe, expect, it, vi } from "vitest";
import { EditorHost } from "../../../main/ts/editor/EditorHost";
import { editorFieldErrorId } from "../../../main/ts/editor/editorFieldErrors";

describe("editorFieldErrorId", () => {
  it("matches the rendered inline error testid suffix", () => {
    expect(editorFieldErrorId("displaytitle")).toBe(
      "editor-field-error-displaytitle",
    );
  });
});

function describedByHost() {
  return (
    <EditorHost
      checkout={vi.fn().mockResolvedValue(undefined)}
      loadFields={async () => ({
        contentId: "42",
        contentType: "percEvent",
        name: "Event",
        checkoutUser: "admin",
        fields: [
          { name: "sys_title", value: "Event" },
          { name: "displaytitle", value: "" },
          { name: "event_at", value: "" },
        ],
      })}
      saveFields={vi.fn()}
      loadType={async () => ({
        fields: [
          { name: "sys_title", label: "Title", control: "sys_EditBox" },
          {
            name: "displaytitle",
            label: "Display title",
            control: "sys_EditBox",
            required: true,
          },
          {
            name: "event_at",
            label: "Event at",
            control: "sys_CalendarSimple",
            dataType: "datetime",
            required: true,
          },
        ],
      })}
    />
  );
}

describe("EditorHost field error describedby", () => {
  it("points each invalid control at its inline error text after a blocked save", async () => {
    const saveFields = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={async () => ({
                  contentId: "42",
                  contentType: "percEvent",
                  name: "Event",
                  checkoutUser: "admin",
                  fields: [
                    { name: "sys_title", value: "Event" },
                    { name: "displaytitle", value: "" },
                    { name: "event_at", value: "" },
                  ],
                })}
                saveFields={saveFields}
                loadType={async () => ({
                  fields: [
                    { name: "sys_title", label: "Title", control: "sys_EditBox" },
                    {
                      name: "displaytitle",
                      label: "Display title",
                      control: "sys_EditBox",
                      required: true,
                    },
                    {
                      name: "event_at",
                      label: "Event at",
                      control: "sys_CalendarSimple",
                      dataType: "datetime",
                      required: true,
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
      expect(screen.getByTestId("editor-field-displaytitle")).toBeTruthy();
    });
    // Valid rows carry no dangling description reference.
    expect(
      screen.getByTestId("editor-field-displaytitle").getAttribute("aria-describedby"),
    ).toBeNull();
    fireEvent.click(screen.getByTestId("editor-save"));
    const textError = await screen.findByTestId(
      "editor-field-error-displaytitle",
    );
    const dateError = await screen.findByTestId("editor-field-error-event_at");
    expect(saveFields).not.toHaveBeenCalled();
    // The error text owns the stable id the controls point at.
    expect(textError.getAttribute("id")).toBe(
      "editor-field-error-displaytitle",
    );
    expect(dateError.getAttribute("id")).toBe("editor-field-error-event_at");
    const textControl = screen.getByTestId("editor-field-displaytitle");
    const dateControl = screen.getByTestId("editor-field-event_at");
    expect(textControl.getAttribute("aria-describedby")).toBe(
      "editor-field-error-displaytitle",
    );
    expect(dateControl.getAttribute("aria-describedby")).toBe(
      "editor-field-error-event_at",
    );
    // Each reference resolves to the visible message for that row.
    expect(
      document.getElementById(
        textControl.getAttribute("aria-describedby") ?? "",
      )?.textContent,
    ).toBe(textError.textContent);
    expect(
      document.getElementById(
        dateControl.getAttribute("aria-describedby") ?? "",
      )?.textContent,
    ).toBe(dateError.textContent);
  });

  it("drops the description reference once the row validates", async () => {
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route path="/editor" element={describedByHost()} />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-displaytitle")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await screen.findByTestId("editor-field-error-displaytitle");
    expect(
      screen.getByTestId("editor-field-displaytitle").getAttribute("aria-describedby"),
    ).toBe("editor-field-error-displaytitle");
    // Typing clears that row's error, which removes the association.
    fireEvent.change(screen.getByTestId("editor-field-displaytitle"), {
      target: { value: "Welcome" },
    });
    await waitFor(() => {
      expect(
        screen.queryByTestId("editor-field-error-displaytitle"),
      ).toBeNull();
    });
    expect(
      screen.getByTestId("editor-field-displaytitle").getAttribute("aria-describedby"),
    ).toBeNull();
  });
});
