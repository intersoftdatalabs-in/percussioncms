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
  revision: 2,
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
          <Route
            path="/editor"
            element={<EditorHost loadContentTypes={async () => []} />}
          />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByTestId("editor-overlay")).toBeTruthy();
    expect(screen.getByTestId("editor-create-panel")).toBeTruthy();
    expect(screen.getByTestId("editor-create-empty").textContent).toMatch(
      /Explorer or Home/i,
    );
  });

  it("surfaces linkback warningMessage when contentId is missing", () => {
    render(
      <MemoryRouter
        initialEntries={[
          "/editor?warningMessage=The page you are attempting to reach, does not exist in the CMS.",
        ]}
      >
        <Routes>
          <Route
            path="/editor"
            element={<EditorHost loadContentTypes={async () => []} />}
          />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByTestId("editor-error").textContent).toMatch(/does not exist in the CMS/i);
    expect(screen.getByTestId("editor-create-panel")).toBeTruthy();
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
    expect(saved.revision).toBe(2);
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

  it("maps 403 on check-out and does not treat it as success", async () => {
    const checkout = vi.fn().mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: {},
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
    expect(checkout).toHaveBeenCalledWith("42");
    expect(screen.getByTestId("editor-lock-error").textContent).toMatch(
      /not allowed to check out/i,
    );
    expect(screen.queryByTestId("editor-save")).toBeNull();
    expect(screen.getByTestId("editor-field-displaytitle")).toHaveProperty(
      "readOnly",
      true,
    );
  });

  it("stays view-only when checkout user-info has another holder and empty currentUser", async () => {
    const checkout = vi.fn().mockResolvedValue({
      checkOutUser: "editor",
      currentUser: "",
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
    expect(screen.queryByTestId("editor-save")).toBeNull();
    expect(screen.getByTestId("editor-field-displaytitle")).toHaveProperty(
      "readOnly",
      true,
    );
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
    expect(checkin).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("editor-checkin-confirm"));
    await waitFor(() => {
      expect(checkin).toHaveBeenCalledWith("42", undefined);
    });
    expect(screen.getByTestId("editor-lock-error").textContent).toMatch(
      /not allowed to check in/i,
    );
    expect(screen.queryByTestId("editor-force-checkin")).toBeNull();
  });

  it("check-in comment cancel does not check in; confirm sends the comment", async () => {
    const checkin = vi.fn().mockResolvedValue(undefined);
    const close = vi.spyOn(window, "close").mockImplementation(() => undefined);
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
                checkin={checkin}
                loadFields={vi.fn().mockResolvedValue(fields)}
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
    expect(screen.getByTestId("editor-checkin-comment")).toBeTruthy();
    fireEvent.click(screen.getByTestId("editor-checkin-cancel"));
    expect(screen.queryByTestId("editor-checkin-comment")).toBeNull();
    expect(checkin).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("editor-checkin"));
    fireEvent.change(screen.getByTestId("editor-checkin-comment-input"), {
      target: { value: "  shipped copy  " },
    });
    fireEvent.click(screen.getByTestId("editor-checkin-confirm"));
    await waitFor(() => {
      expect(checkin).toHaveBeenCalledWith("42", "shipped copy");
    });
    close.mockRestore();
  });

  it("force check-in of another user clears the lock only after confirm", async () => {
    const checkout = vi.fn().mockResolvedValue({
      checkOutUser: "editor",
      currentUser: "admin",
    });
    const forceCheckin = vi.fn().mockResolvedValue(undefined);
    const confirmForceCheckin = vi.fn().mockReturnValueOnce(false).mockReturnValueOnce(true);
    const loadFields = vi.fn().mockResolvedValue({
      ...fields,
      checkoutUser: "editor",
    });
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={checkout}
                forceCheckin={forceCheckin}
                confirmForceCheckin={confirmForceCheckin}
                loadFields={loadFields}
                loadType={async () => ({ fields: [] })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-force-checkin")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-checkout-user").textContent).toMatch(/editor/i);
    expect(screen.queryByTestId("editor-checkin")).toBeNull();
    expect(screen.queryByTestId("editor-save")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-force-checkin"));
    expect(forceCheckin).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("editor-force-checkin"));
    await waitFor(() => {
      expect(forceCheckin).toHaveBeenCalledWith("42");
    });
    await waitFor(() => {
      expect(screen.queryByTestId("editor-checkout-user")).toBeNull();
    });
    expect(screen.queryByTestId("editor-force-checkin")).toBeNull();
    expect(screen.getByTestId("editor-checkout")).toBeTruthy();
  });

  it("maps 403, 404, and 409 on force check-in as failures", async () => {
    const checkout = vi.fn().mockResolvedValue({
      checkOutUser: "editor",
      currentUser: "admin",
    });
    const forceCheckin = vi
      .fn()
      .mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: {} })
      .mockRejectedValueOnce({ status: 404, statusText: "Not Found", body: {} })
      .mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: {} });
    const loadFields = vi.fn().mockResolvedValue({
      ...fields,
      checkoutUser: "editor",
    });
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={checkout}
                forceCheckin={forceCheckin}
                confirmForceCheckin={() => true}
                loadFields={loadFields}
                loadType={async () => ({ fields: [] })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-force-checkin")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-force-checkin"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-lock-error").textContent).toMatch(
        /not allowed to force check-in/i,
      );
    });
    expect(screen.getByTestId("editor-checkout-user").textContent).toMatch(/editor/i);
    fireEvent.click(screen.getByTestId("editor-force-checkin"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-lock-error").textContent).toMatch(/not found/i);
    });
    fireEvent.click(screen.getByTestId("editor-force-checkin"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-lock-error").textContent).toMatch(
        /not checked out/i,
      );
    });
    expect(forceCheckin).toHaveBeenCalledTimes(3);
    expect(screen.queryByTestId("editor-save")).toBeNull();
  });

  it("does not offer force check-in in view mode", async () => {
    const checkout = vi.fn();
    const loadFields = vi.fn().mockResolvedValue({
      ...fields,
      checkoutUser: "editor",
    });
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={<EditorHost checkout={checkout} loadFields={loadFields} />}
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-checkout-user").textContent).toMatch(/editor/i);
    });
    expect(checkout).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-force-checkin")).toBeNull();
    expect(screen.queryByTestId("editor-checkin")).toBeNull();
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

  it("lists related slot and inline content", async () => {
    const checkout = vi.fn().mockResolvedValue(undefined);
    const loadFields = vi.fn().mockResolvedValue(fields);
    const loadRelatedCanvas = vi.fn().mockResolvedValue({
      ownerId: 42,
      templateId: null,
      slots: [
        {
          slotId: 1,
          name: "content",
          label: "Content",
          items: [
            {
              relationshipId: 9,
              ownerId: 42,
              dependentId: 55,
              slotId: 1,
              templateId: 2,
              sortRank: 0,
            },
          ],
        },
      ],
    });
    const loadRelatedLocal = vi.fn().mockResolvedValue({
      count: 1,
      links: [{ type: "local", targetId: "88" }],
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
                loadRelatedCanvas={loadRelatedCanvas}
                loadRelatedLocal={loadRelatedLocal}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-list")).toBeTruthy();
    });
    expect(screen.getAllByTestId("editor-related-row")).toHaveLength(2);
    expect(screen.queryByTestId("editor-related-empty")).toBeNull();
    expect(screen.getByTestId("editor-related-insert")).toBeTruthy();
    expect(screen.getAllByTestId("editor-related-remove")).toHaveLength(1);
  });

  it("does not offer related insert in view mode", async () => {
    const loadRelatedCanvas = vi.fn().mockResolvedValue({
      ownerId: 42,
      templateId: 7,
      slots: [
        {
          slotId: 1,
          name: "content",
          label: "Content",
          items: [
            {
              relationshipId: 9,
              ownerId: 42,
              dependentId: 55,
              slotId: 1,
              templateId: 2,
              sortRank: 0,
            },
          ],
        },
      ],
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
                loadType={async () => ({ fields: [] })}
                loadRelatedCanvas={loadRelatedCanvas}
                loadRelatedLocal={async () => ({ count: 0, links: [] })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-panel")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-related-insert")).toBeNull();
    expect(screen.queryByTestId("editor-related-remove")).toBeNull();
  });

  it("shows empty related content when there are no links", async () => {
    const checkout = vi.fn().mockResolvedValue(undefined);
    const loadFields = vi.fn().mockResolvedValue(fields);
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
                loadRelatedCanvas={async () => ({
                  ownerId: 42,
                  templateId: null,
                  slots: [],
                })}
                loadRelatedLocal={async () => ({ count: 0, links: [] })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-related-empty").textContent).toMatch(
      /No related content/i,
    );
  });

  it("shows forbidden when related APIs return 403", async () => {
    const checkout = vi.fn().mockResolvedValue(undefined);
    const loadFields = vi.fn().mockResolvedValue(fields);
    const forbidden = { status: 403, statusText: "Forbidden", body: {} };
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
                loadRelatedCanvas={async () => {
                  throw forbidden;
                }}
                loadRelatedLocal={async () => {
                  throw forbidden;
                }}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-error")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-related-error").textContent).toMatch(
      /not allowed to list related content/i,
    );
    expect(screen.queryByTestId("editor-related-empty")).toBeNull();
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
    await waitFor(() => {
      expect(
        (screen.getByTestId("editor-field-sys_communityid") as HTMLSelectElement).value,
      ).toBe("20");
    });
    expect(saved.fields.find((f) => f.name === "img")).toBeUndefined();
    expect(uploadBinary).toHaveBeenCalledWith("42", "img", file);
  });

  it("maps file-field upload 403 and 413 as errors, not success", async () => {
    const loadFields = vi.fn().mockResolvedValue({
      contentId: "42",
      contentType: "percFile",
      name: "Spec",
      checkoutUser: "admin",
      fields: [{ name: "sys_title", value: "Spec" }],
    });
    const saveFields = vi.fn().mockResolvedValue({
      contentId: "42",
      contentType: "percFile",
      name: "Spec",
      checkoutUser: "admin",
      fields: [{ name: "sys_title", value: "Spec" }],
    });
    const uploadBinary = vi.fn().mockRejectedValue({
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
                checkout={async () => undefined}
                loadFields={loadFields}
                saveFields={saveFields}
                uploadBinary={uploadBinary}
                loadType={async () => ({
                  fields: [
                    { name: "sys_title", label: "Title", control: "sys_EditBox" },
                    {
                      name: "item_file_attachment",
                      label: "File",
                      control: "sys_File",
                    },
                  ],
                })}
                loadBinaryMeta={async () => ({
                  contentId: "42",
                  field: "item_file_attachment",
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
      expect(screen.getByTestId("editor-file-item_file_attachment")).toBeTruthy();
    });
    const file = new File(["x"], "spec.pdf", { type: "application/pdf" });
    fireEvent.change(screen.getByTestId("editor-file-item_file_attachment"), {
      target: { files: [file] },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-save-error").textContent).toMatch(
        /not allowed to upload/i,
      );
    });
    expect(screen.queryByTestId("editor-saved")).toBeNull();

    uploadBinary.mockRejectedValueOnce({
      status: 413,
      statusText: "Payload Too Large",
      body: {},
    });
    fireEvent.change(screen.getByTestId("editor-file-item_file_attachment"), {
      target: { files: [file] },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-save-error").textContent).toMatch(/too large/i);
    });
  });

  it("maps image-field upload 403 as an error, not success", async () => {
    const loadFields = vi.fn().mockResolvedValue({
      contentId: "42",
      contentType: "percImage",
      name: "Hero",
      checkoutUser: "admin",
      fields: [{ name: "sys_title", value: "Hero" }],
    });
    const saveFields = vi.fn().mockResolvedValue({
      contentId: "42",
      contentType: "percImage",
      name: "Hero",
      checkoutUser: "admin",
      fields: [{ name: "sys_title", value: "Hero" }],
    });
    const uploadBinary = vi.fn().mockRejectedValue({
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
                checkout={async () => undefined}
                loadFields={loadFields}
                saveFields={saveFields}
                uploadBinary={uploadBinary}
                loadType={async () => ({
                  fields: [
                    { name: "sys_title", label: "Title", control: "sys_EditBox" },
                    { name: "img", label: "Image", control: "sys_webImageFX" },
                  ],
                })}
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
      expect(screen.getByTestId("editor-file-img")).toBeTruthy();
    });
    const file = new File(["x"], "hero.png", { type: "image/png" });
    fireEvent.change(screen.getByTestId("editor-file-img"), {
      target: { files: [file] },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-save-error").textContent).toMatch(
        /not allowed to upload an image/i,
      );
    });
    expect(screen.queryByTestId("editor-saved")).toBeNull();
  });

  it("blocks a non-image file on an image field before PUT", async () => {
    const uploadBinary = vi.fn();
    const saveFields = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={async () => undefined}
                loadFields={async () => ({
                  contentId: "42",
                  contentType: "percImage",
                  name: "Hero",
                  checkoutUser: "admin",
                  fields: [{ name: "sys_title", value: "Hero" }],
                })}
                saveFields={saveFields}
                uploadBinary={uploadBinary}
                loadType={async () => ({
                  fields: [
                    { name: "sys_title", label: "Title", control: "sys_EditBox" },
                    { name: "img", label: "Image", control: "sys_webImageFX" },
                  ],
                })}
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
      expect(screen.getByTestId("editor-file-img")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-file-img"), {
      target: { files: [new File(["x"], "spec.pdf", { type: "application/pdf" })] },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-save-error").textContent).toMatch(
        /could not be uploaded/i,
      );
    });
    expect(uploadBinary).not.toHaveBeenCalled();
    expect(saveFields).not.toHaveBeenCalled();
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
  });
});

describe("EditorHost HTML field save (#4680)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("saves TinyMCE HTML through PUT fields", async () => {
    const saveFields = vi.fn().mockResolvedValue({
      contentId: "42",
      contentType: "percRichText",
      name: "Intro",
      checkoutUser: "admin",
      fields: [
        { name: "sys_title", value: "Intro" },
        { name: "text", value: "<p>Bye</p>" },
      ],
    });
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
                  contentType: "percRichText",
                  name: "Intro",
                  checkoutUser: "admin",
                  fields: [
                    { name: "sys_title", value: "Intro" },
                    { name: "text", value: "<p>Hi</p>" },
                  ],
                })}
                saveFields={saveFields}
                loadType={async () => ({
                  fields: [
                    { name: "sys_title", label: "Title", control: "sys_EditBox" },
                    { name: "text", label: "Body", control: "sys_tinymce" },
                  ],
                })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-text")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-field-text"), {
      target: { value: "<p>Bye</p>" },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "text")?.value).toBe("<p>Bye</p>");
  });

  it("blocks XSS markup before PUT and maps unnamed HTTP 400 onto the html field", async () => {
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
                  contentType: "percRichText",
                  name: "Intro",
                  checkoutUser: "admin",
                  fields: [
                    { name: "sys_title", value: "Intro" },
                    { name: "text", value: "<p>Hi</p>" },
                  ],
                })}
                saveFields={saveFields}
                loadType={async () => ({
                  fields: [
                    { name: "sys_title", label: "Title", control: "sys_EditBox" },
                    { name: "text", label: "Body", control: "sys_tinymce" },
                  ],
                })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-text")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-field-text"), {
      target: { value: '<p><script>alert(1)</script></p>' },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-text")).toBeTruthy();
    });
    expect(saveFields).not.toHaveBeenCalled();
  });

  it("maps unnamed HTTP 400 onto the html field", async () => {
    const saveFields = vi.fn().mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { Error: { message: "HTML rejected" } },
    });
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
                  contentType: "percRichText",
                  name: "Intro",
                  checkoutUser: "admin",
                  fields: [
                    { name: "sys_title", value: "Intro" },
                    { name: "text", value: "<p>Hi</p>" },
                  ],
                })}
                saveFields={saveFields}
                loadType={async () => ({
                  fields: [
                    { name: "sys_title", label: "Title", control: "sys_EditBox" },
                    { name: "text", label: "Body", control: "sys_tinymce" },
                  ],
                })}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-text")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-field-text"), {
      target: { value: "<p>Updated</p>" },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-text")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-save-error").textContent).toMatch(
      /could not be saved/i,
    );
  });

  it("keeps html widgets read-only in view mode", async () => {
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={async () => ({
                  contentId: "42",
                  contentType: "percRichText",
                  name: "Intro",
                  checkoutUser: "",
                  fields: [{ name: "text", value: "<p>Hi</p>" }],
                })}
                loadType={async () => ({
                  fields: [{ name: "text", label: "Body", control: "sys_tinymce" }],
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
    const area = screen.getByTestId("editor-field-text") as HTMLTextAreaElement;
    expect(area.readOnly).toBe(true);
    expect(screen.queryByTestId("editor-save")).toBeNull();
  });

  it("saves sys_EditBox maxtext through PUT and keeps line breaks", async () => {
    const saveFields = vi.fn().mockResolvedValue({
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "description", value: "line one\nline two" },
      ],
    });
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
                  contentType: "percPage",
                  name: "Home",
                  checkoutUser: "admin",
                  fields: [
                    { name: "sys_title", value: "Home" },
                    { name: "description", value: "line one" },
                  ],
                })}
                saveFields={saveFields}
                loadType={async () => ({
                  fields: [
                    { name: "sys_title", label: "Title", control: "sys_EditBox", dataType: "text" },
                    {
                      name: "description",
                      label: "Description",
                      control: "sys_EditBox",
                      dataType: "maxtext",
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
      expect(screen.getByTestId("editor-field-description")).toBeTruthy();
    });
    const area = screen.getByTestId("editor-field-description") as HTMLTextAreaElement;
    expect(area.getAttribute("data-editor-kind")).toBe("longtext");
    fireEvent.change(area, { target: { value: "line one\nline two" } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "description")?.value).toBe(
      "line one\nline two",
    );
  });

  it("blocks a NUL in long text before PUT", async () => {
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
                  contentType: "percPage",
                  name: "Home",
                  checkoutUser: "admin",
                  fields: [{ name: "description", value: "ok" }],
                })}
                saveFields={saveFields}
                loadType={async () => ({
                  fields: [
                    {
                      name: "description",
                      label: "Description",
                      control: "sys_TextArea",
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
      expect(screen.getByTestId("editor-field-description")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-field-description"), {
      target: { value: "bad\u0000value" },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-description")).toBeTruthy();
    });
    expect(saveFields).not.toHaveBeenCalled();
  });

  it("maps unnamed HTTP 400 onto the long-text field", async () => {
    const saveFields = vi.fn().mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { Error: { message: "rejected" } },
    });
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
                  contentType: "percPage",
                  name: "Home",
                  checkoutUser: "admin",
                  fields: [{ name: "description", value: "ok" }],
                })}
                saveFields={saveFields}
                loadType={async () => ({
                  fields: [
                    {
                      name: "description",
                      label: "Description",
                      control: "sys_TextArea",
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
      expect(screen.getByTestId("editor-field-description")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-field-description"), {
      target: { value: "updated\nbody" },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-description")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-save-error").textContent).toMatch(
      /long text could not be saved/i,
    );
  });

  it("keeps long-text read-only in view mode", async () => {
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={async () => ({
                  contentId: "42",
                  contentType: "percPage",
                  name: "Home",
                  checkoutUser: "",
                  fields: [{ name: "description", value: "line one" }],
                })}
                loadType={async () => ({
                  fields: [
                    {
                      name: "description",
                      label: "Description",
                      control: "sys_EditBox",
                      dataType: "maxtext",
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
    const area = screen.getByTestId("editor-field-description") as HTMLTextAreaElement;
    expect(area.readOnly).toBe(true);
    expect(screen.queryByTestId("editor-save")).toBeNull();
  });

  it("saves an in-range sys_Number and blocks non-numeric input", async () => {
    const saveFields = vi.fn().mockResolvedValue({
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "qty", value: "7" },
      ],
    });
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
                  contentType: "percPage",
                  name: "Home",
                  checkoutUser: "admin",
                  fields: [
                    { name: "sys_title", value: "Home" },
                    { name: "qty", value: "1" },
                  ],
                })}
                saveFields={saveFields}
                loadType={async () => ({
                  fields: [
                    { name: "sys_title", label: "Title", control: "sys_EditBox", dataType: "text" },
                    {
                      name: "qty",
                      label: "Quantity",
                      control: "sys_Number",
                      dataType: "integer",
                      controlProperties: [
                        { name: "minimum", value: "0" },
                        { name: "maximum", value: "10" },
                      ],
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
      expect(screen.getByTestId("editor-field-qty")).toBeTruthy();
    });
    const input = screen.getByTestId("editor-field-qty") as HTMLInputElement;
    expect(input.getAttribute("data-editor-kind")).toBe("number");
    fireEvent.change(input, { target: { value: "abc" } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-qty").textContent).toMatch(/valid number/i);
    });
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("editor-field-qty"), { target: { value: "11" } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-qty").textContent).toMatch(/range/i);
    });
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("editor-field-qty"), { target: { value: "7" } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const sent = saveFields.mock.calls[0][1] as {
      fields: { name: string; value: string; dataType?: string; minimum?: string; maximum?: string }[];
    };
    const qty = sent.fields.find((field) => field.name === "qty");
    expect(qty).toMatchObject({
      value: "7",
      dataType: "integer",
      minimum: "0",
      maximum: "10",
    });
  });

  it("maps a numeric fields HTTP 400 onto the number field", async () => {
    const saveFields = vi.fn().mockRejectedValue({
      status: 400,
      body: { message: "Field 'qty' is not a valid number." },
    });
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
                  contentType: "percPage",
                  name: "Home",
                  checkoutUser: "admin",
                  fields: [{ name: "qty", value: "1" }],
                })}
                saveFields={saveFields}
                loadType={async () => ({
                  fields: [
                    {
                      name: "qty",
                      label: "Quantity",
                      control: "sys_Number",
                      dataType: "integer",
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
      expect(screen.getByTestId("editor-field-qty")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-field-qty"), { target: { value: "4" } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-qty")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-save-error").textContent).toMatch(/number could not be saved/i);
  });

  it("keeps a number field read-only in view mode", async () => {
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={async () => ({
                  contentId: "42",
                  contentType: "percPage",
                  name: "Home",
                  checkoutUser: "",
                  fields: [{ name: "qty", value: "3" }],
                })}
                loadType={async () => ({
                  fields: [
                    {
                      name: "qty",
                      label: "Quantity",
                      control: "sys_Number",
                      dataType: "integer",
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
    const input = screen.getByTestId("editor-field-qty") as HTMLInputElement;
    expect(input.readOnly).toBe(true);
    expect(screen.queryByTestId("editor-save")).toBeNull();
  });

  it("saves and clears a page link without calling save for an invalid target", async () => {
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: { fields: { name: string; value: string }[] }) => ({
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      revision: 3,
      fields: body.fields,
    }));
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
                  contentType: "percPage",
                  name: "Home",
                  checkoutUser: "admin",
                  fields: [{ name: "page", value: "" }],
                })}
                saveFields={saveFields}
                loadType={async () => ({
                  fields: [
                    {
                      name: "page",
                      label: "Page link",
                      control: "sys_PageLink",
                      dataType: "text",
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
      expect(screen.getByTestId("editor-field-page")).toBeTruthy();
    });
    const input = screen.getByTestId("editor-field-page") as HTMLInputElement;
    expect(input.getAttribute("data-editor-kind")).toBe("link");
    fireEvent.change(input, { target: { value: "javascript:alert(1)" } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-page").textContent).toMatch(/content id|site path/i);
    });
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("editor-field-page"), { target: { value: "594" } });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const sent = saveFields.mock.calls[0][1] as {
      fields: { name: string; value: string; dataType?: string }[];
    };
    expect(sent.fields.find((field) => field.name === "page")).toMatchObject({
      value: "594",
      dataType: "link",
    });
    fireEvent.click(screen.getByTestId("editor-link-clear-page"));
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(saveFields.mock.calls.length).toBeGreaterThan(1);
    });
    const cleared = saveFields.mock.calls[1][1] as {
      fields: { name: string; value: string }[];
    };
    expect(cleared.fields.find((field) => field.name === "page")?.value).toBe("");
  });

  it("maps link HTTP 404 and 403 onto the link field", async () => {
    const saveFields = vi
      .fn()
      .mockRejectedValueOnce({ status: 404, body: { message: "missing" } })
      .mockRejectedValueOnce({ status: 403, body: { message: "denied" } });
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
                  contentType: "percPage",
                  name: "Home",
                  checkoutUser: "admin",
                  fields: [{ name: "page", value: "594" }],
                })}
                saveFields={saveFields}
                loadType={async () => ({
                  fields: [
                    {
                      name: "page",
                      label: "Page link",
                      control: "sys_ManagedLink",
                      dataType: "text",
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
      expect(screen.getByTestId("editor-field-page")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-error-page").textContent).toMatch(/not found/i);
    });
    expect(screen.getByTestId("editor-save-error").textContent).toMatch(/not found/i);
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-save-error").textContent).toMatch(/not allowed/i);
    });
  });

  it("keeps a link field read-only in view mode", async () => {
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={async () => ({
                  contentId: "42",
                  contentType: "percPage",
                  name: "Home",
                  checkoutUser: "",
                  fields: [{ name: "page", value: "594" }],
                })}
                loadType={async () => ({
                  fields: [
                    {
                      name: "page",
                      label: "Page link",
                      control: "sys_PageLink",
                      dataType: "text",
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
    const input = screen.getByTestId("editor-field-page") as HTMLInputElement;
    expect(input.readOnly).toBe(true);
    expect(screen.queryByTestId("editor-link-clear-page")).toBeNull();
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
                loadWorkflowChoices={
                  extra.loadWorkflowChoices ??
                  (async () => ({
                    currentWorkflowId: "4",
                    choices: [
                      { id: "4", name: "Local" },
                      { id: "7", name: "Review" },
                    ],
                  }))
                }
                changeWorkflow={extra.changeWorkflow}
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

  it("saves a different workflow and shows the new state (#4861)", async () => {
    const changeWorkflow = vi.fn().mockResolvedValue({
      stateName: "Pending",
      workflowId: "7",
      transitionTriggers: ["Approve"],
    });
    renderEdit({ changeWorkflow });
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-picker")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-workflow-picker"), {
      target: { value: "7" },
    });
    fireEvent.click(screen.getByTestId("editor-workflow-save"));
    await waitFor(() => {
      expect(changeWorkflow).toHaveBeenCalledWith("42", "7");
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-changed")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-workflow-state").textContent).toMatch(/Pending/);
    expect(screen.getByTestId("editor-workflow-trigger-Approve")).toBeTruthy();
    expect(screen.queryByTestId("editor-workflow-trigger-Submit")).toBeNull();
  });

  it("does not claim success when the workflow is forbidden (#4861)", async () => {
    const changeWorkflow = vi.fn().mockRejectedValue(new Error("HTTP 403"));
    renderEdit({ changeWorkflow });
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-picker")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-workflow-picker"), {
      target: { value: "7" },
    });
    fireEvent.click(screen.getByTestId("editor-workflow-save"));
    await waitFor(() => {
      expect(changeWorkflow).toHaveBeenCalledWith("42", "7");
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-error").textContent).toMatch(
        /Could not change/i,
      );
    });
    expect(screen.queryByTestId("editor-workflow-changed")).toBeNull();
  });
});

describe("EditorHost stage the open item (#4915)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  function titleType() {
    return { fields: [{ name: "sys_title", label: "Title", readOnly: false }] };
  }

  it("confirms then stages a percPage and reports success", async () => {
    const stageItem = vi.fn().mockResolvedValue(true);
    const confirmStage = vi.fn().mockReturnValue(true);
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
                stageItem={stageItem}
                confirmStage={confirmStage}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-stage-item")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-stage-item"));
    await waitFor(() => {
      expect(confirmStage).toHaveBeenCalled();
    });
    await waitFor(() => {
      expect(stageItem).toHaveBeenCalledWith("42", "page");
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-stage-done")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-stage-error")).toBeNull();
  });

  it("does not stage when the confirm is cancelled", async () => {
    const stageItem = vi.fn().mockResolvedValue(true);
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
                stageItem={stageItem}
                confirmStage={() => false}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-stage-item")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-stage-item"));
    await waitFor(() => {
      expect(stageItem).not.toHaveBeenCalled();
    });
    expect(screen.queryByTestId("editor-stage-done")).toBeNull();
  });

  it("keeps HTTP 403 on the host and does not claim success", async () => {
    const stageItem = vi.fn().mockRejectedValue({ status: 403, statusText: "Forbidden" });
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
                stageItem={stageItem}
                confirmStage={() => true}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-stage-item")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-stage-item"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-stage-error").textContent).toMatch(/403/);
    });
    expect(screen.queryByTestId("editor-stage-done")).toBeNull();
  });

  it("hides Stage for a template and in view mode", async () => {
    const stageItem = vi.fn();
    const templateFields = { ...fields, contentType: "percTemplate" };
    const { unmount } = render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(templateFields)}
                loadType={async () => titleType()}
                stageItem={stageItem}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-stage-item")).toBeNull();
    unmount();
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
                stageItem={stageItem}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-stage-item")).toBeNull();
    expect(stageItem).not.toHaveBeenCalled();
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

  it("opens publish history for the open item without publishing (#4863)", async () => {
    const publishItem = vi.fn();
    vi.spyOn(global, "fetch").mockImplementation(async (input) => {
      const url = String(input);
      if (url.includes("/item/pubhistory/")) {
        return new Response(
          JSON.stringify({
            ItemPublishingHistory: [
              { server: "prod", operation: "publish", status: "SUCCESS" },
            ],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
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
                publishItem={publishItem}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-publishing-history")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-publish-now")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-publishing-history"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-publishing-history-dialog")).toBeTruthy();
    });
    await waitFor(() => {
      expect(screen.getByTestId("item-history-row").textContent).toMatch(/prod/);
    });
    expect(publishItem).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("explorer-publishing-history-close"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-publishing-history-dialog")).toBeNull();
    });
  });

  it("shows an empty history state and an HTTP error without treating them as success (#4863)", async () => {
    vi.spyOn(global, "fetch").mockImplementation(
      async () =>
        new Response(JSON.stringify({ ItemPublishingHistory: [] }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
    );
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
                publishItem={vi.fn()}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-publishing-history")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-publishing-history"));
    await waitFor(() => {
      expect(screen.getByTestId("item-history-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("item-history-error")).toBeNull();
    cleanup();
    vi.spyOn(global, "fetch").mockImplementation(
      async () => new Response("missing", { status: 404, statusText: "Not Found" }),
    );
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
                publishItem={vi.fn()}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-publishing-history")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-publishing-history"));
    await waitFor(() => {
      expect(screen.getByTestId("item-history-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("item-history-empty")).toBeNull();
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

describe("EditorHost takedown (#4862)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  function titleType() {
    return { fields: [{ name: "sys_title", label: "Title", readOnly: false }] };
  }

  it("confirms then takes down a percPage and reports success", async () => {
    const takedownItem = vi.fn().mockResolvedValue(true);
    const confirmTakedown = vi.fn().mockReturnValue(true);
    const loadTakedownLinked = vi.fn().mockResolvedValue([
      { pagePath: "/Sites/Demo/Home" },
    ]);
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
                takedownItem={takedownItem}
                loadTakedownLinked={loadTakedownLinked}
                confirmTakedown={confirmTakedown}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-takedown")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-takedown"));
    await waitFor(() => {
      expect(confirmTakedown).toHaveBeenCalled();
    });
    const body = String(confirmTakedown.mock.calls[0]?.[0] ?? "");
    expect(body).toContain("/Sites/Demo/Home");
    await waitFor(() => {
      expect(takedownItem).toHaveBeenCalledWith(
        "42",
        "page",
        [{ pagePath: "/Sites/Demo/Home" }],
      );
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-takedown-done")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-takedown-error")).toBeNull();
  });

  it("does not call takedown when confirm is cancelled", async () => {
    const takedownItem = vi.fn().mockResolvedValue(true);
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
                takedownItem={takedownItem}
                loadTakedownLinked={async () => []}
                confirmTakedown={() => false}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-takedown")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-takedown"));
    await waitFor(() => {
      expect(takedownItem).not.toHaveBeenCalled();
    });
    expect(screen.queryByTestId("editor-takedown-done")).toBeNull();
  });

  it("surfaces FORBIDDEN as failure, not success", async () => {
    const takedownItem = vi.fn().mockRejectedValue(new Error("FORBIDDEN"));
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
                takedownItem={takedownItem}
                loadTakedownLinked={async () => []}
                confirmTakedown={() => true}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-takedown")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-takedown"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-takedown-error").textContent).toMatch(
        /FORBIDDEN/,
      );
    });
    expect(screen.queryByTestId("editor-takedown-done")).toBeNull();
  });

  it("hides Take down in view mode and for a new unsaved item", async () => {
    const takedownItem = vi.fn();
    const { unmount } = render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                takedownItem={takedownItem}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-takedown")).toBeNull();
    unmount();
    render(
      <MemoryRouter initialEntries={["/editor?mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={vi.fn()}
                loadContentTypes={async () => []}
                takedownItem={takedownItem}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-host")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-takedown")).toBeNull();
    expect(takedownItem).not.toHaveBeenCalled();
  });

  it("hides Take down for a folder content type", async () => {
    render(
      <MemoryRouter initialEntries={["/editor?contentId=8&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue({
                  contentId: "8",
                  contentType: "Folder",
                  name: "News",
                  checkoutUser: "admin",
                  fields: [{ name: "sys_title", value: "News" }],
                })}
                loadType={async () => titleType()}
                takedownItem={vi.fn()}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-takedown")).toBeNull();
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
    expect(screen.queryByTestId("editor-checkin-comment")).toBeNull();
  });
});

describe("EditorHost save field values (#4645)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("maps save HTTP 409 to a stale revision banner", async () => {
    const saveFields = vi.fn().mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "newer revision" },
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
                loadType={async () => ({
                  fields: [
                    { name: "displaytitle", label: "Display title", readOnly: false },
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
    fireEvent.change(screen.getByTestId("editor-field-displaytitle"), {
      target: { value: "Updated" },
    });
    fireEvent.click(screen.getByTestId("editor-save"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-save-error").textContent).toMatch(
        /newer revision/i,
      );
    });
    expect(screen.queryByText(/Saved/)).toBeNull();
    expect(screen.getByTestId("editor-form")).toBeTruthy();
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

  it("reloads the preview frame for a chosen template without saving it", async () => {
    const loadPreviewLocation = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=42&sys_template=8&sys_revision=1",
    });
    const changeTemplate = vi.fn();
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
                  contentType: "percPage",
                  fields: [
                    { name: "sys_title", value: "Home" },
                    { name: "templateid", value: "7" },
                  ],
                })}
                loadType={async () => ({
                  fields: [{ name: "sys_title", label: "Title", readOnly: false }],
                  allowedTemplates: [
                    { name: "Home", label: "Home page", guid: { stringValue: "7" } },
                    { name: "Blog", label: "Blog", guid: { stringValue: "8" } },
                  ],
                })}
                loadPreviewLocation={loadPreviewLocation}
                changeTemplate={changeTemplate}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-preview-template")).toBeTruthy();
    });
    const select = screen.getByTestId("editor-preview-template") as HTMLSelectElement;
    expect(select.value).toBe("");
    expect(screen.queryByTestId("editor-preview-frame")).toBeNull();
    fireEvent.change(select, { target: { value: "8" } });
    await waitFor(() => {
      expect(loadPreviewLocation).toHaveBeenCalledWith(42, 8);
    });
    const frame = screen.getByTestId("editor-preview-frame");
    expect(frame.getAttribute("src")).toContain("sys_template=8");
    expect(frame.getAttribute("data-preview-template")).toBe("8");
    expect(changeTemplate).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("editor-preview-template"), {
      target: { value: "" },
    });
    await waitFor(() => {
      const current = screen.getByTestId("editor-preview-frame");
      expect(current.getAttribute("src")).toContain("/pagemanagement/render/page/42");
      expect(current.getAttribute("data-preview-template")).toBe("current");
    });
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

  const emptyRelated = {
    loadRelatedCanvas: async () => ({
      ownerId: 42,
      templateId: null,
      slots: [],
    }),
    loadRelatedLocal: async () => ({ count: 0, links: [] }),
  };

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
                {...emptyRelated}
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
                {...emptyRelated}
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
                {...emptyRelated}
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
                {...emptyRelated}
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
                {...emptyRelated}
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
    expect(screen.queryByTestId("editor-recycle")).toBeNull();
  });
});

describe("EditorHost recycle (#4773)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  function host(extra: Record<string, unknown>) {
    return (
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => ({ fields: [] })}
                {...extra}
              />
            }
          />
        </Routes>
      </MemoryRouter>
    );
  }

  it("recycles the open item and leaves edit mode", async () => {
    const recycleItem = vi.fn().mockResolvedValue(undefined);
    const resolveRecycleTarget = vi.fn().mockResolvedValue({
      path: "//Sites/Demo/Home",
      type: "percPage",
    });
    render(
      host({
        recycleItem,
        resolveRecycleTarget,
        confirmRecycle: () => true,
      }),
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-recycle")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-recycle"));
    await waitFor(() => {
      expect(recycleItem).toHaveBeenCalledWith("//Sites/Demo/Home");
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-recycle-done")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-content-id")).toBeNull();
    expect(screen.queryByTestId("editor-form")).toBeNull();
    expect(screen.queryByTestId("editor-recycle")).toBeNull();
  });

  it("does not recycle when confirm is cancelled", async () => {
    const recycleItem = vi.fn();
    const resolveRecycleTarget = vi.fn();
    render(
      host({
        recycleItem,
        resolveRecycleTarget,
        confirmRecycle: () => false,
      }),
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-recycle")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-recycle"));
    expect(resolveRecycleTarget).not.toHaveBeenCalled();
    expect(recycleItem).not.toHaveBeenCalled();
    expect(screen.getByTestId("editor-content-id").textContent).toMatch(/42/);
    expect(screen.queryByTestId("editor-recycle-done")).toBeNull();
  });

  it("does not recycle a folder", async () => {
    const recycleItem = vi.fn();
    render(
      host({
        recycleItem,
        resolveRecycleTarget: async () => ({
          path: "//Sites/Demo/",
          type: "Folder",
        }),
        confirmRecycle: () => true,
      }),
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-recycle")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-recycle"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-recycle-error").textContent).toMatch(
        /folder/i,
      );
    });
    expect(recycleItem).not.toHaveBeenCalled();
    expect(screen.getByTestId("editor-content-id").textContent).toMatch(/42/);
  });

  it.each([
    [403, /not allowed to recycle/i],
    [404, /not found/i],
    [409, /cannot be recycled/i],
  ])("surfaces HTTP %s as a failure, not success", async (status, pattern) => {
    const recycleItem = vi.fn().mockRejectedValue({
      status,
      statusText: "err",
      body: {},
    });
    render(
      host({
        recycleItem,
        resolveRecycleTarget: async () => ({
          path: "//Sites/Demo/Home",
          type: "percPage",
        }),
        confirmRecycle: () => true,
      }),
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-recycle")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-recycle"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-recycle-error").textContent).toMatch(pattern);
    });
    expect(screen.queryByTestId("editor-recycle-done")).toBeNull();
    expect(screen.getByTestId("editor-content-id").textContent).toMatch(/42/);
  });

  it("hides recycle in view mode", async () => {
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=view"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn()}
                loadFields={vi.fn().mockResolvedValue(fields)}
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
    expect(screen.queryByTestId("editor-recycle")).toBeNull();
  });
});

describe("EditorHost move to folder (#4774)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  const emptyRelated = {
    loadRelatedCanvas: async () => ({
      ownerId: 42,
      templateId: null,
      slots: [],
    }),
    loadRelatedLocal: async () => ({ count: 0, links: [] }),
  };

  function titleType() {
    return {
      fields: [{ name: "sys_title", label: "Title", readOnly: false }],
    };
  }

  function renderHost(
    mode: string,
    extra: Partial<React.ComponentProps<typeof EditorHost>> = {},
  ) {
    return render(
      <MemoryRouter initialEntries={[`/editor?contentId=42&mode=${mode}`]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                loadItemLocation={async () => ({ path: "//Sites/Demo/Home" })}
                {...emptyRelated}
                {...extra}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
  }

  it("moves the open item and stays on the same content id", async () => {
    const moveItem = vi.fn().mockResolvedValue(undefined);
    renderHost("edit", { moveItem });
    await waitFor(() => {
      expect(screen.getByTestId("editor-move")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-move"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-move-dest-input")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("explorer-move-dest-input"), {
      target: { value: "//Sites/Other" },
    });
    fireEvent.click(screen.getByTestId("explorer-move-dest-ok"));
    await waitFor(() => {
      expect(moveItem).toHaveBeenCalledWith("//Sites/Demo/Home", "//Sites/Other");
    });
    expect(screen.getByTestId("editor-content-id").textContent).toMatch(/42/);
    await waitFor(() => {
      expect(screen.getByTestId("editor-move-done")).toBeTruthy();
    });
  });

  it("does not move when the picker is cancelled", async () => {
    const moveItem = vi.fn();
    renderHost("edit", { moveItem });
    await waitFor(() => {
      expect(screen.getByTestId("editor-move")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-move"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-move-dest-cancel")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("explorer-move-dest-cancel"));
    expect(moveItem).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-move-done")).toBeNull();
  });

  it("does not post when the destination is the current folder", async () => {
    const moveItem = vi.fn();
    renderHost("edit", { moveItem });
    await waitFor(() => {
      expect(screen.getByTestId("editor-move")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-move"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-move-dest-ok")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("explorer-move-dest-ok"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-move-error").textContent).toMatch(
        /already there/i,
      );
    });
    expect(moveItem).not.toHaveBeenCalled();
  });

  it("hides move in view mode", async () => {
    renderHost("view");
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-move")).toBeNull();
  });

  it("surfaces HTTP 403, 404, and 409", async () => {
    for (const [status, pattern] of [
      [403, /not allowed to move/i],
      [404, /not found/i],
      [409, /already has an item|not a folder/i],
    ] as const) {
      cleanup();
      const moveItem = vi.fn().mockRejectedValue({
        status,
        statusText: String(status),
        body: {},
      });
      renderHost("edit", { moveItem });
      await waitFor(() => {
        expect(screen.getByTestId("editor-move")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("editor-move"));
      await waitFor(() => {
        expect(screen.getByTestId("explorer-move-dest-input")).toBeTruthy();
      });
      fireEvent.change(screen.getByTestId("explorer-move-dest-input"), {
        target: { value: "//Sites/Other" },
      });
      fireEvent.click(screen.getByTestId("explorer-move-dest-ok"));
      await waitFor(() => {
        expect(screen.getByTestId("editor-move-error").textContent).toMatch(pattern);
      });
      expect(screen.getByTestId("editor-content-id").textContent).toMatch(/42/);
      expect(screen.queryByTestId("editor-move-done")).toBeNull();
    }
  });
});

describe("EditorHost copy to folder (#4793)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  const emptyRelated = {
    loadRelatedCanvas: async () => ({
      ownerId: 42,
      templateId: null,
      slots: [],
    }),
    loadRelatedLocal: async () => ({ count: 0, links: [] }),
  };

  function titleType() {
    return {
      fields: [{ name: "sys_title", label: "Title", readOnly: false }],
    };
  }

  function renderHost(
    mode: string,
    extra: Partial<React.ComponentProps<typeof EditorHost>> = {},
  ) {
    return render(
      <MemoryRouter initialEntries={[`/editor?contentId=42&mode=${mode}`]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => titleType()}
                loadItemLocation={async () => ({ path: "//Sites/Demo/Home" })}
                {...emptyRelated}
                {...extra}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
  }

  it("copies into the chosen folder and stays on the original item", async () => {
    const copyItemToFolder = vi.fn().mockResolvedValue(undefined);
    renderHost("edit", { copyItemToFolder });
    await waitFor(() => {
      expect(screen.getByTestId("editor-copy-to-folder")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-copy-to-folder"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-copy-dest-input")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("explorer-copy-dest-input"), {
      target: { value: "//Sites/Other" },
    });
    fireEvent.click(screen.getByTestId("explorer-copy-dest-ok"));
    await waitFor(() => {
      expect(copyItemToFolder).toHaveBeenCalledWith("//Sites/Demo/Home", "//Sites/Other");
    });
    expect(screen.getByTestId("editor-content-id").textContent).toMatch(/42/);
    await waitFor(() => {
      expect(screen.getByTestId("editor-copy-to-folder-done").textContent).toMatch(
        /Sites\/Other/,
      );
    });
  });

  it("does not copy when the picker is cancelled", async () => {
    const copyItemToFolder = vi.fn();
    renderHost("edit", { copyItemToFolder });
    await waitFor(() => {
      expect(screen.getByTestId("editor-copy-to-folder")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-copy-to-folder"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-copy-dest-cancel")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("explorer-copy-dest-cancel"));
    expect(copyItemToFolder).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-copy-to-folder-done")).toBeNull();
  });

  it("hides copy to folder in view mode", async () => {
    renderHost("view");
    await waitFor(() => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-copy-to-folder")).toBeNull();
  });

  it("surfaces HTTP 400, 403, and 404 without success", async () => {
    for (const [status, pattern] of [
      [400, /not valid/i],
      [403, /not allowed to copy/i],
      [404, /not found/i],
    ] as const) {
      cleanup();
      const copyItemToFolder = vi.fn().mockRejectedValue({
        status,
        statusText: String(status),
        body: {},
      });
      renderHost("edit", { copyItemToFolder });
      await waitFor(() => {
        expect(screen.getByTestId("editor-copy-to-folder")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("editor-copy-to-folder"));
      await waitFor(() => {
        expect(screen.getByTestId("explorer-copy-dest-input")).toBeTruthy();
      });
      fireEvent.change(screen.getByTestId("explorer-copy-dest-input"), {
        target: { value: "//Sites/Other" },
      });
      fireEvent.click(screen.getByTestId("explorer-copy-dest-ok"));
      await waitFor(() => {
        expect(screen.getByTestId("editor-copy-error").textContent).toMatch(pattern);
      });
      expect(screen.getByTestId("editor-content-id").textContent).toMatch(/42/);
      expect(screen.queryByTestId("editor-copy-to-folder-done")).toBeNull();
    }
  });
});

describe("EditorHost rename open item (#4791)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  function renderHost(
    mode: string,
    extra: Partial<React.ComponentProps<typeof EditorHost>> = {},
  ) {
    return render(
      <MemoryRouter initialEntries={[`/editor?contentId=42&mode=${mode}`]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => ({ fields: [] })}
                loadItemLocation={async () => ({ path: "//Assets/Home" })}
                {...extra}
              />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
  }

  it("hides rename in view mode", async () => {
    renderHost("view");
    await waitFor(() => {
      expect(screen.getByTestId("editor-host")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-rename")).toBeNull();
    expect(screen.queryByTestId("editor-rename-name")).toBeNull();
  });

  it("renames via folders/rename/item and shows the reloaded name", async () => {
    const renameItem = vi.fn().mockResolvedValue(undefined);
    const loadFields = vi
      .fn()
      .mockResolvedValueOnce(fields)
      .mockResolvedValueOnce({
        ...fields,
        name: "Home2",
        fields: [
          { name: "sys_title", value: "Home2" },
          { name: "displaytitle", value: "Welcome" },
        ],
      });
    renderHost("edit", { renameItem, loadFields });
    await waitFor(() => {
      expect(screen.getByTestId("editor-rename-name")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-rename-name"), {
      target: { value: "Home2" },
    });
    fireEvent.click(screen.getByTestId("editor-rename"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-renamed")).toBeTruthy();
    });
    expect(renameItem).toHaveBeenCalledWith("//Assets/Home", "Home2");
    expect(screen.getByTestId("editor-field-sys_title")).toHaveProperty(
      "value",
      "Home2",
    );
    expect(screen.queryByTestId("editor-rename-error")).toBeNull();
  });

  it("does not claim success for a blank name or HTTP 400/403/404", async () => {
    const renameItem = vi.fn();
    renderHost("edit", { renameItem });
    await waitFor(() => {
      expect(screen.getByTestId("editor-rename")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-rename-name"), {
      target: { value: "  " },
    });
    fireEvent.click(screen.getByTestId("editor-rename"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-rename-error").textContent).toMatch(
        /slash/i,
      );
    });
    expect(renameItem).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-renamed")).toBeNull();

    for (const status of [400, 403, 404]) {
      cleanup();
      const failing = vi.fn().mockRejectedValue({
        status,
        statusText: String(status),
        body: {},
      });
      renderHost("edit", { renameItem: failing });
      await waitFor(() => {
        expect(screen.getByTestId("editor-rename-name")).toBeTruthy();
      });
      fireEvent.change(screen.getByTestId("editor-rename-name"), {
        target: { value: "Other" },
      });
      fireEvent.click(screen.getByTestId("editor-rename"));
      await waitFor(() => {
        expect(screen.getByTestId("editor-rename-error")).toBeTruthy();
      });
      expect(screen.getByTestId("editor-field-sys_title")).toHaveProperty(
        "value",
        "Home",
      );
      expect(screen.queryByTestId("editor-renamed")).toBeNull();
    }
  });

  it("compares two revisions and shows field diffs without calling restore", async () => {
    const checkout = vi.fn().mockResolvedValue(undefined);
    const loadFields = vi.fn().mockResolvedValue(fields);
    const loadRevisions = vi.fn().mockResolvedValue({
      restorable: true,
      revisions: [
        {
          revId: 4,
          status: "Live",
          lastModifier: "admin",
          lastModifiedDate: "2026-04-10",
        },
        {
          revId: 5,
          status: "Quick Edit",
          lastModifier: "admin",
          lastModifiedDate: "2026-04-12",
        },
      ],
      comments: [],
    });
    const compareRevisions = vi.fn().mockResolvedValue({
      itemId: "42",
      rev1: 4,
      rev2: 5,
      fields: [
        {
          name: "sys_title",
          leftValue: "Old",
          rightValue: "Home",
          changed: true,
        },
      ],
    });
    const restoreRevision = vi.fn();
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
                compareRevisions={compareRevisions}
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
      expect(screen.getByTestId("editor-compare-left")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-compare-run"));
    await waitFor(() => {
      expect(compareRevisions).toHaveBeenCalledWith("42", 4, 5);
    });
    expect(screen.getByTestId("editor-compare-row-sys_title").textContent).toMatch(
      /Old/,
    );
    expect(screen.getByTestId("editor-compare-row-sys_title").textContent).toMatch(
      /Changed/,
    );
    expect(restoreRevision).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-compare-empty")).toBeNull();
  });

  it("does not show a diff when the history has fewer than two revisions", async () => {
    const loadRevisions = vi.fn().mockResolvedValue({
      restorable: false,
      revisions: [],
      comments: [],
    });
    const compareRevisions = vi.fn();
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => ({ fields: [] })}
                loadRevisions={loadRevisions}
                compareRevisions={compareRevisions}
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
      expect(screen.getByTestId("editor-compare-need-two")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-compare-table")).toBeNull();
    expect(compareRevisions).not.toHaveBeenCalled();
  });

  it("shows an empty compare and a load failure instead of a fake diff", async () => {
    const loadRevisions = vi.fn().mockResolvedValue({
      restorable: true,
      revisions: [
        { revId: 1, status: "A", lastModifier: "a", lastModifiedDate: "d" },
        { revId: 2, status: "B", lastModifier: "b", lastModifiedDate: "e" },
      ],
      comments: [],
    });
    const compareRevisions = vi
      .fn()
      .mockResolvedValueOnce({ itemId: "42", rev1: 1, rev2: 2, fields: [] })
      .mockRejectedValueOnce({ status: 404, statusText: "Not Found", body: {} });
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => ({ fields: [] })}
                loadRevisions={loadRevisions}
                compareRevisions={compareRevisions}
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
      expect(screen.getByTestId("editor-compare-run")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-compare-right"), {
      target: { value: "1" },
    });
    const run = screen.getByTestId("editor-compare-run") as HTMLButtonElement;
    expect(run.disabled).toBe(true);
    expect(screen.queryByTestId("editor-compare-table")).toBeNull();
    fireEvent.change(screen.getByTestId("editor-compare-right"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByTestId("editor-compare-run"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-compare-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-compare-table")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-compare-run"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-compare-error").textContent).toMatch(
        /not found for compare/i,
      );
    });
    expect(screen.queryByTestId("editor-compare-table")).toBeNull();
  });

  it("shows compare unavailable when revision history fails to load", async () => {
    render(
      <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
        <Routes>
          <Route
            path="/editor"
            element={
              <EditorHost
                checkout={vi.fn().mockResolvedValue(undefined)}
                loadFields={vi.fn().mockResolvedValue(fields)}
                loadType={async () => ({ fields: [] })}
                loadRevisions={vi.fn().mockRejectedValue({
                  status: 403,
                  statusText: "Forbidden",
                  body: {},
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
    fireEvent.click(screen.getByTestId("editor-restore-toggle"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-compare-unavailable")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-restore-load-error")).toBeTruthy();
    expect(screen.queryByTestId("editor-compare-table")).toBeNull();
  });
});
