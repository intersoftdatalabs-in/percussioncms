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

async function openComparePanel() {
  await waitFor(() => {
    expect(screen.getByTestId("editor-form")).toBeTruthy();
  });
  fireEvent.click(screen.getByTestId("editor-restore-toggle"));
  await waitFor(() => {
    expect(screen.getByTestId("editor-compare-left")).toBeTruthy();
  });
}

describe("EditorHost revision compare view polish", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("shows a loading status while the compare is pending", async () => {
    let resolveCompare!: (value: typeof compare) => void;
    const pending = new Promise<typeof compare>((resolve) => {
      resolveCompare = resolve;
    });
    renderHost({ compareRevisions: vi.fn().mockReturnValue(pending) });
    await openComparePanel();
    fireEvent.click(screen.getByTestId("editor-compare-run"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-compare-loading")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-compare-loading")).toHaveProperty(
      "role",
      "status",
    );
    resolveCompare(compare);
    await waitFor(() => {
      expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-compare-loading")).toBeNull();
  });

  it("changed-only filter hides unchanged rows and can show them again", async () => {
    renderHost();
    await openComparePanel();
    fireEvent.click(screen.getByTestId("editor-compare-run"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
    });
    expect(
      screen.getByTestId("editor-compare-row-displaytitle"),
    ).toBeTruthy();
    fireEvent.click(screen.getByTestId("editor-compare-changed-only"));
    expect(screen.queryByTestId("editor-compare-row-displaytitle")).toBeNull();
    expect(screen.getByTestId("editor-compare-row-sys_title")).toBeTruthy();
    fireEvent.click(screen.getByTestId("editor-compare-changed-only"));
    expect(
      screen.getByTestId("editor-compare-row-displaytitle"),
    ).toBeTruthy();
  });

  it("names the source revision in the field-restored notice", async () => {
    renderHost();
    await openComparePanel();
    fireEvent.click(screen.getByTestId("editor-compare-run"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-compare-use-left-sys_title"));
    expect(
      screen.getByTestId("editor-compare-field-restored").textContent,
    ).toMatch(/sys_title.*revision #4/);
    fireEvent.click(screen.getByTestId("editor-compare-use-right-sys_title"));
    expect(
      screen.getByTestId("editor-compare-field-restored").textContent,
    ).toMatch(/sys_title.*revision #5/);
  });

  it("moves focus to the results and then to the restored notice", async () => {
    renderHost();
    await openComparePanel();
    fireEvent.click(screen.getByTestId("editor-compare-run"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
    });
    await waitFor(() => {
      expect(document.activeElement).toBe(
        screen.getByTestId("editor-compare-results"),
      );
    });
    fireEvent.click(screen.getByTestId("editor-compare-use-left-sys_title"));
    await waitFor(() => {
      expect(document.activeElement).toBe(
        screen.getByTestId("editor-compare-field-restored"),
      );
    });
  });

  it("marks added and removed words inside changed cells", async () => {
    renderHost({
      compareRevisions: vi.fn().mockResolvedValue({
        itemId: "42",
        rev1: 4,
        rev2: 5,
        fields: [
          {
            name: "sys_title",
            leftValue: "The old title",
            rightValue: "The new title",
            changed: true,
          },
        ],
      }),
    });
    await openComparePanel();
    fireEvent.click(screen.getByTestId("editor-compare-run"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-compare-word-left-sys_title"),
      ).toBeTruthy();
    });
    const older = screen.getByTestId("editor-compare-word-left-sys_title");
    const newer = screen.getByTestId("editor-compare-word-right-sys_title");
    expect(older.querySelector("del")?.textContent).toBe("old");
    expect(older.querySelector("ins")).toBeNull();
    expect(newer.querySelector("ins")?.textContent).toBe("new");
    expect(newer.querySelector("del")).toBeNull();
    expect(older.textContent).toMatch(/The old title/);
    expect(newer.textContent).toMatch(/The new title/);
  });

  it("word-diff toggle is keyboard-focusable and falls back to plain values", async () => {
    renderHost();
    await openComparePanel();
    fireEvent.click(screen.getByTestId("editor-compare-run"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
    });
    const toggle = screen.getByTestId("editor-compare-word-diff");
    expect(toggle).toHaveProperty("checked", true);
    expect(
      screen.queryByTestId("editor-compare-word-left-sys_title"),
    ).toBeTruthy();
    toggle.focus();
    expect(document.activeElement).toBe(toggle);
    fireEvent.click(document.activeElement as HTMLElement);
    expect(
      screen.queryByTestId("editor-compare-word-left-sys_title"),
    ).toBeNull();
    expect(
      screen.getByTestId("editor-compare-row-sys_title").textContent,
    ).toMatch(/Old/);
    fireEvent.click(screen.getByTestId("editor-compare-word-diff"));
    expect(
      screen.getByTestId("editor-compare-word-left-sys_title"),
    ).toBeTruthy();
  });

  it("announces compare results through a live region", async () => {
    renderHost();
    await openComparePanel();
    fireEvent.click(screen.getByTestId("editor-compare-run"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
    });
    const results = screen.getByTestId("editor-compare-results");
    expect(results).toHaveProperty("role", "region");
    expect(results.getAttribute("aria-live")).toBe("polite");
  });
});
