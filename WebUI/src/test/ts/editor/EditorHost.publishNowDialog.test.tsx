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
import { EDITOR_MSG } from "../../../main/ts/editor/messages";

const fields: ItemEditorFields = {
  contentId: "42",
  contentType: "percPage",
  name: "Home",
  checkoutUser: "admin",
  revision: 2,
  fields: [{ name: "sys_title", value: "Home" }],
};

function titleType() {
  return { fields: [{ name: "sys_title", label: "Title", readOnly: false }] };
}

function renderHost(publishItem: (...args: unknown[]) => Promise<unknown>) {
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
              publishItem={publishItem as never}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

async function openPublishDialog(): Promise<void> {
  await waitFor(() => {
    expect(screen.getByTestId("editor-publish-now")).toBeTruthy();
  });
  fireEvent.click(screen.getByTestId("editor-publish-now"));
  await waitFor(() => {
    expect(screen.getByTestId("editor-publish-now-dialog")).toBeTruthy();
  });
}

describe("EditorHost publish now dialog keyboard/a11y (#6932)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    delete (window as { I18N?: unknown }).I18N;
  });

  it("moves keyboard focus to Cancel when the dialog opens", async () => {
    renderHost(vi.fn().mockResolvedValue(true));
    await openPublishDialog();
    await waitFor(() => {
      expect(document.activeElement).toBe(
        screen.getByTestId("editor-publish-now-cancel"),
      );
    });
  });

  it("Escape cancels without publishing and returns focus to Publish now", async () => {
    const publishItem = vi.fn().mockResolvedValue(true);
    renderHost(publishItem);
    await openPublishDialog();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => {
      expect(screen.queryByTestId("editor-publish-now-dialog")).toBeNull();
    });
    expect(publishItem).not.toHaveBeenCalled();
    expect(document.activeElement).toBe(screen.getByTestId("editor-publish-now"));
  });

  it("Tab on the last control wraps focus to the first control", async () => {
    renderHost(vi.fn().mockResolvedValue(true));
    await openPublishDialog();
    const cancel = screen.getByTestId("editor-publish-now-cancel");
    const confirm = screen.getByTestId("editor-publish-now-confirm");
    await waitFor(() => {
      expect(document.activeElement).toBe(cancel);
    });
    confirm.focus();
    expect(document.activeElement).toBe(confirm);
    fireEvent.keyDown(confirm, { key: "Tab" });
    expect(document.activeElement).toBe(cancel);
  });

  it("Shift+Tab on the first control wraps focus to the last control", async () => {
    renderHost(vi.fn().mockResolvedValue(true));
    await openPublishDialog();
    const cancel = screen.getByTestId("editor-publish-now-cancel");
    const confirm = screen.getByTestId("editor-publish-now-confirm");
    await waitFor(() => {
      expect(document.activeElement).toBe(cancel);
    });
    fireEvent.keyDown(cancel, { key: "Tab", shiftKey: true });
    expect(document.activeElement).toBe(confirm);
  });

  it("a server failure is announced and described on the confirm button", async () => {
    const publishItem = vi.fn().mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: {},
    });
    renderHost(publishItem);
    await openPublishDialog();
    fireEvent.click(screen.getByTestId("editor-publish-now-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-publish-now-error")).toBeTruthy();
    });
    const error = screen.getByTestId("editor-publish-now-error");
    expect(error.getAttribute("role")).toBe("alert");
    expect(error.getAttribute("id")).toBe("editor-publish-now-error");
    expect(
      screen.getByTestId("editor-publish-now-confirm").getAttribute(
        "aria-describedby",
      ),
    ).toBe("editor-publish-now-error");
    // The dialog stays open so keyboard users can read the failure.
    expect(screen.getByTestId("editor-publish-now-dialog")).toBeTruthy();
  });

  it("confirm carries no error description when there is no failure", async () => {
    renderHost(vi.fn().mockResolvedValue(true));
    await openPublishDialog();
    expect(screen.queryByTestId("editor-publish-now-error")).toBeNull();
    expect(
      screen.getByTestId("editor-publish-now-confirm").getAttribute(
        "aria-describedby",
      ),
    ).toBeNull();
  });

  it("routes dialog strings through the TMX catalog, not hardcoded nodes", async () => {
    (window as {
      I18N?: { message: (key: string) => string };
    }).I18N = {
      message: (key: string) =>
        key === EDITOR_MSG.PUBLISH_TITLE
          ? "CATALOG_PUBLISH_NOW_TITLE"
          : key.slice(key.lastIndexOf("@") + 1),
    };
    renderHost(vi.fn().mockResolvedValue(true));
    await openPublishDialog();
    expect(screen.getByTestId("editor-publish-now-dialog")).toHaveTextContent(
      "CATALOG_PUBLISH_NOW_TITLE",
    );
  });
});
