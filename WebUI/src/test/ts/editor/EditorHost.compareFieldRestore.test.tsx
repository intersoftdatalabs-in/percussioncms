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
  revision: 2,
  fields: [{ name: "sys_title", value: "Home" }],
};

const revisions = {
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
};

const compare = {
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
    {
      name: "displaytitle",
      leftValue: "Same",
      rightValue: "Same",
      changed: false,
    },
  ],
};

function renderHost(overrides: Record<string, unknown> = {}) {
  const checkout = vi.fn().mockResolvedValue(undefined);
  const loadFields = vi.fn().mockResolvedValue(fields);
  const loadRevisions = vi.fn().mockResolvedValue(revisions);
  const compareRevisions = vi.fn().mockResolvedValue(compare);
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
              {...overrides}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
  return { checkout, loadFields, loadRevisions, compareRevisions, restoreRevision };
}

async function openCompare() {
  await waitFor(() => {
    expect(screen.getByTestId("editor-form")).toBeTruthy();
  });
  fireEvent.click(screen.getByTestId("editor-restore-toggle"));
  await waitFor(() => {
    expect(screen.getByTestId("editor-compare-left")).toBeTruthy();
  });
  fireEvent.click(screen.getByTestId("editor-compare-run"));
  await waitFor(() => {
    expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
  });
}

describe("EditorHost compare field restore", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("copies the older compare value into the draft without calling restore", async () => {
    const { compareRevisions, restoreRevision } = renderHost();
    await openCompare();
    expect(compareRevisions).toHaveBeenCalledWith("42", 4, 5);
    fireEvent.click(screen.getByTestId("editor-compare-use-left-sys_title"));
    expect(screen.getByTestId("editor-field-sys_title")).toHaveProperty(
      "value",
      "Old",
    );
    expect(
      screen.getByTestId("editor-compare-field-restored").textContent,
    ).toMatch(/sys_title/);
    expect(restoreRevision).not.toHaveBeenCalled();
    expect(compareRevisions).toHaveBeenCalledTimes(1);
  });

  it("copies the newer value and offers no buttons on unchanged rows", async () => {
    renderHost();
    await openCompare();
    expect(
      screen.queryByTestId("editor-compare-use-left-displaytitle"),
    ).toBeNull();
    expect(
      screen.queryByTestId("editor-compare-use-right-displaytitle"),
    ).toBeNull();
    fireEvent.click(screen.getByTestId("editor-compare-use-right-sys_title"));
    expect(screen.getByTestId("editor-field-sys_title")).toHaveProperty(
      "value",
      "Home",
    );
    expect(
      screen.getByTestId("editor-compare-field-restored").textContent,
    ).toMatch(/sys_title/);
  });

  it("disables field restore while another user holds the checkout", async () => {
    renderHost({
      checkout: vi
        .fn()
        .mockResolvedValue({ checkOutUser: "other", currentUser: "admin" }),
    });
    await openCompare();
    expect(
      screen.getByTestId("editor-compare-use-left-sys_title"),
    ).toHaveProperty("disabled", true);
    expect(
      screen.getByTestId("editor-compare-use-right-sys_title"),
    ).toHaveProperty("disabled", true);
    expect(screen.queryByTestId("editor-compare-field-restored")).toBeNull();
  });
});
