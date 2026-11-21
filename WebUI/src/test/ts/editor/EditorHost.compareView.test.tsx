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
import { renderA11yGate } from "../contentExplorer/a11y";

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

// Load-hardening for #6837 (sibling of the #6706/#6742 slotCreate timing
// flake): under full-suite CPU contention every async chain in this spec can
// exceed the 1s waitFor default, so every wait below carries an explicit 10s
// budget (inside the 20s vitest testTimeout). No assertion is weakened.
const COMPARE_WAIT_MS = 10_000;

async function openComparePanel() {
  await waitFor(
    () => {
      expect(screen.getByTestId("editor-form")).toBeTruthy();
    },
    { timeout: COMPARE_WAIT_MS },
  );
  fireEvent.click(screen.getByTestId("editor-restore-toggle"));
  await waitFor(
    () => {
      expect(screen.getByTestId("editor-compare-left")).toBeTruthy();
    },
    { timeout: COMPARE_WAIT_MS },
  );
  // Readiness gate for #6837: the compare selects render before the async
  // loadRevisions in handleRestoreOpen settles, so editor-compare-run stays
  // disabled (empty compareLeft/compareRight) until the revisions land. A
  // synchronous run click before that either no-ops or records
  // COMPARE_NEED_TWO while the table waitFor below times out — the same
  // signature as the #6742 slotCreate flake. Never proceed on a disabled run.
  await waitFor(
    () => {
      expect(
        (screen.getByTestId("editor-compare-run") as HTMLButtonElement)
          .disabled,
      ).toBe(false);
    },
    { timeout: COMPARE_WAIT_MS },
  );
}

// Click-after-ready for #6837: re-assert the run button is enabled at click
// time (a re-render between openComparePanel and the click can re-disable
// it while compareBusy flips), then click. Callers must await this instead
// of fireEvent.click on editor-compare-run directly.
async function clickCompareRun() {
  await waitFor(
    () => {
      expect(
        (screen.getByTestId("editor-compare-run") as HTMLButtonElement)
          .disabled,
      ).toBe(false);
    },
    { timeout: COMPARE_WAIT_MS },
  );
  fireEvent.click(screen.getByTestId("editor-compare-run"));
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
    await clickCompareRun();
    await waitFor(
      () => {
        expect(screen.getByTestId("editor-compare-loading")).toBeTruthy();
      },
      { timeout: COMPARE_WAIT_MS },
    );
    expect(screen.getByTestId("editor-compare-loading")).toHaveProperty(
      "role",
      "status",
    );
    resolveCompare(compare);
    await waitFor(
      () => {
        expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
      },
      { timeout: COMPARE_WAIT_MS },
    );
    expect(screen.queryByTestId("editor-compare-loading")).toBeNull();
  });

  it("changed-only filter hides unchanged rows and can show them again", async () => {
    renderHost();
    await openComparePanel();
    await clickCompareRun();
    await waitFor(
      () => {
        expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
      },
      { timeout: COMPARE_WAIT_MS },
    );
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
    await clickCompareRun();
    await waitFor(
      () => {
        expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
      },
      { timeout: COMPARE_WAIT_MS },
    );
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
    await clickCompareRun();
    await waitFor(
      () => {
        expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
      },
      { timeout: COMPARE_WAIT_MS },
    );
    await waitFor(
      () => {
        expect(document.activeElement).toBe(
          screen.getByTestId("editor-compare-results"),
        );
      },
      { timeout: COMPARE_WAIT_MS },
    );
    fireEvent.click(screen.getByTestId("editor-compare-use-left-sys_title"));
    await waitFor(
      () => {
        expect(document.activeElement).toBe(
          screen.getByTestId("editor-compare-field-restored"),
        );
      },
      { timeout: COMPARE_WAIT_MS },
    );
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
    await clickCompareRun();
    await waitFor(
      () => {
        expect(
          screen.getByTestId("editor-compare-word-left-sys_title"),
        ).toBeTruthy();
      },
      { timeout: COMPARE_WAIT_MS },
    );
    const older = screen.getByTestId("editor-compare-word-left-sys_title");
    const newer = screen.getByTestId("editor-compare-word-right-sys_title");
    expect(older.querySelector("del")?.textContent).toBe("old");
    expect(older.querySelector("ins")).toBeNull();
    expect(newer.querySelector("ins")?.textContent).toBe("new");
    expect(newer.querySelector("del")).toBeNull();
    expect(older.textContent).toMatch(/The old title/);
    expect(newer.textContent).toMatch(/The new title/);
  });

  it("refines single-word substitutions to the changed characters", async () => {
    renderHost({
      compareRevisions: vi.fn().mockResolvedValue({
        itemId: "42",
        rev1: 4,
        rev2: 5,
        fields: [
          {
            name: "sys_title",
            leftValue: "grey",
            rightValue: "gray",
            changed: true,
          },
        ],
      }),
    });
    await openComparePanel();
    await clickCompareRun();
    await waitFor(
      () => {
        expect(
          screen.getByTestId("editor-compare-word-left-sys_title"),
        ).toBeTruthy();
      },
      { timeout: COMPARE_WAIT_MS },
    );
    const older = screen.getByTestId("editor-compare-word-left-sys_title");
    const newer = screen.getByTestId("editor-compare-word-right-sys_title");
    expect(older.querySelector("del")?.textContent).toBe("e");
    expect(older.querySelector("ins")).toBeNull();
    expect(newer.querySelector("ins")?.textContent).toBe("a");
    expect(newer.querySelector("del")).toBeNull();
    expect(older.textContent).toBe("grey");
    expect(newer.textContent).toBe("gray");
  });

  it("word-diff toggle is keyboard-focusable and falls back to plain values", async () => {
    renderHost();
    await openComparePanel();
    await clickCompareRun();
    await waitFor(
      () => {
        expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
      },
      { timeout: COMPARE_WAIT_MS },
    );
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
    await clickCompareRun();
    await waitFor(
      () => {
        expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
      },
      { timeout: COMPARE_WAIT_MS },
    );
    const results = screen.getByTestId("editor-compare-results");
    expect(results).toHaveProperty("role", "region");
    expect(results.getAttribute("aria-live")).toBe("polite");
  });

  it("exposes the compare controls as a labelled toolbar", async () => {
    renderHost();
    await openComparePanel();
    const toolbar = screen.getByTestId("editor-compare-toolbar");
    expect(toolbar).toHaveProperty("role", "toolbar");
    expect(toolbar.getAttribute("aria-label")).toMatch(/controls/i);
  });

  it("steps focus through changed rows with a live Change X of Y status", async () => {
    renderHost({
      compareRevisions: vi.fn().mockResolvedValue({
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
          {
            name: "body",
            leftValue: "Old body",
            rightValue: "New body",
            changed: true,
          },
        ],
      }),
    });
    await openComparePanel();
    await clickCompareRun();
    await waitFor(
      () => {
        expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
      },
      { timeout: COMPARE_WAIT_MS },
    );
    const status = screen.getByTestId("editor-compare-nav-status");
    expect(status).toHaveProperty("role", "status");
    expect(status.textContent).toBe("Change 1 of 2: sys_title");
    const prev = screen.getByTestId("editor-compare-prev-change");
    const next = screen.getByTestId("editor-compare-next-change");
    expect(prev).toHaveProperty("disabled", true);
    expect(next).toHaveProperty("disabled", false);
    fireEvent.click(next);
    await waitFor(
      () => {
        expect(status.textContent).toBe("Change 2 of 2: body");
      },
      { timeout: COMPARE_WAIT_MS },
    );
    expect(document.activeElement).toBe(
      screen.getByTestId("editor-compare-row-body"),
    );
    expect(next).toHaveProperty("disabled", true);
    expect(prev).toHaveProperty("disabled", false);
    fireEvent.click(prev);
    await waitFor(
      () => {
        expect(status.textContent).toBe("Change 1 of 2: sys_title");
      },
      { timeout: COMPARE_WAIT_MS },
    );
    expect(document.activeElement).toBe(
      screen.getByTestId("editor-compare-row-sys_title"),
    );
  });

  it("passes the axe gate on the compare surface", async () => {
    renderHost();
    await openComparePanel();
    await clickCompareRun();
    await waitFor(
      () => {
        expect(screen.getByTestId("editor-compare-table")).toBeTruthy();
      },
      { timeout: COMPARE_WAIT_MS },
    );
    await renderA11yGate(screen.getByTestId("editor-compare"));
  });
});
