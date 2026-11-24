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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { RevisionsPanel } from "../../../main/ts/contentExplorer/RevisionsPanel";
import { renderA11yGate } from "./a11y";

/**
 * Single wall-clock budget for every polling gate in this file (#7226).
 * Each round trip here is purely microtask-driven (mocked promises
 * resolve, then setState, then render) with no real timers involved,
 * so the gates are deterministic in outcome; the budget is strictly a
 * backstop for loaded-worker scheduling lag. Every {@code findBy} and
 * {@code waitFor} uses it explicitly: the 1s/5s testing-library
 * defaults are the most-tripped cliffs when hundreds of suite files
 * share the worker host (same rationale as {@code RETRY_ROUND_TRIP_TIMEOUT}
 * in #7122 and the #7147 bin-lifecycle slice).
 */
const REVISIONS_PANEL_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7226, same load-contention rationale as
 * the #7207/#7147 slices). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const REVISIONS_PANEL_TEST_TIMEOUT = 30_000;

const SAMPLE = {
  restorable: true,
  revisions: [
    {
      revId: 1,
      lastModifiedDate: "2026-01-01",
      lastModifier: "Admin",
      status: "Draft",
    },
    {
      revId: 2,
      lastModifiedDate: "2026-01-02",
      lastModifier: "Editor",
      status: "Live",
    },
  ],
  comments: [
    {
      comment: "Looks good",
      commenter: "Admin",
      commentType: "Approve",
      commentDate: "2026-01-02",
    },
  ],
};

describe("RevisionsPanel", () => {
  it("renders revision rows and restore for a prior revision", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    const restore = vi.fn().mockResolvedValue(undefined);
    render(
      <RevisionsPanel
        itemId="42"
        itemLabel="Home"
        loadSummary={async () => SAMPLE}
        restoreRevision={restore}
        confirm={() => true}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("revisions-row-1")).toBeTruthy();
    expect(screen.getByTestId("revisions-row-2")).toBeTruthy();
    expect(screen.getByTestId("revisions-restore-1")).toBeTruthy();
    expect(screen.queryByTestId("revisions-restore-2")).toBeNull();
    fireEvent.click(screen.getByTestId("revisions-restore-1"));
    await waitFor(() => expect(restore).toHaveBeenCalledWith("42", 1), { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT });
  });

  it("opens on the audit tab when requested", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    render(
      <RevisionsPanel
        itemId="42"
        initialTab="audit"
        loadSummary={async () => SAMPLE}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
        "data-testid-tab",
        "audit",
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("audit-row-0")).toHaveTextContent("Looks good");
  });

  it("does not restore when confirm is cancelled", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    const restore = vi.fn();
    render(
      <RevisionsPanel
        itemId="42"
        loadSummary={async () => SAMPLE}
        restoreRevision={restore}
        confirm={() => false}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-restore-1")).toBeTruthy(),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("revisions-restore-1"));
    expect(restore).not.toHaveBeenCalled();
  });

  it("shows the older revision as current only after restore succeeds (#5220)", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    let resolveRestore: (value: void) => void = () => {};
    const restore = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          resolveRestore = resolve;
        }),
    );
    let resolveReload: (value: typeof SAMPLE) => void = () => {};
    let loads = 0;
    const loadSummary = vi.fn(() => {
      loads += 1;
      if (loads === 1) {
        return Promise.resolve({ ...SAMPLE, currentRevision: 2 });
      }
      return new Promise<typeof SAMPLE>((resolve) => {
        resolveReload = resolve;
      });
    });
    const { container } = render(
      <RevisionsPanel
        itemId="42"
        loadSummary={loadSummary}
        restoreRevision={restore}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-current")).toHaveAttribute(
        "data-current-rev",
        "2",
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("revisions-restore-1"));
    expect(restore).not.toHaveBeenCalled();
    await renderA11yGate(container);
    fireEvent.click(screen.getByTestId("revisions-restore-cancel"));
    expect(restore).not.toHaveBeenCalled();
    expect(screen.getByTestId("revisions-current")).toHaveAttribute(
      "data-current-rev",
      "2",
    );

    fireEvent.click(screen.getByTestId("revisions-restore-1"));
    fireEvent.click(screen.getByTestId("revisions-restore-ok"));
    await waitFor(() => expect(restore).toHaveBeenCalledWith("42", 1), { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT });
    expect(screen.getByTestId("revisions-current")).toHaveAttribute(
      "data-current-rev",
      "2",
    );
    resolveRestore();
    await waitFor(() => expect(loads).toBe(2), { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT });
    expect(screen.getByTestId("revisions-current")).toHaveAttribute(
      "data-current-rev",
      "2",
    );
    resolveReload({
      ...SAMPLE,
      currentRevision: 1,
    });
    await waitFor(() =>
      expect(screen.getByTestId("revisions-current")).toHaveAttribute(
        "data-current-rev",
        "1",
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("revisions-restore-1")).toBeNull();
    expect(screen.getByTestId("revisions-restore-2")).toBeTruthy();
  });

  it("HTTP 403 and 409 leave the current revision in place (#5220)", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    const restore403 = vi.fn().mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: {},
    });
    const loadSummary = vi.fn(async () => ({ ...SAMPLE, currentRevision: 2 }));
    const { rerender } = render(
      <RevisionsPanel
        itemId="42"
        loadSummary={loadSummary}
        restoreRevision={restore403}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-restore-1")).toBeTruthy(),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("revisions-restore-1"));
    fireEvent.click(screen.getByTestId("revisions-restore-ok"));
    await waitFor(() =>
      expect(screen.getByTestId("revisions-restore-error").textContent).toMatch(
        /403/,
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(loadSummary).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("revisions-current")).toHaveAttribute(
      "data-current-rev",
      "2",
    );
    expect(screen.getByTestId("revisions-restore-1")).toBeTruthy();

    const restore409 = vi.fn().mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: {},
    });
    rerender(
      <RevisionsPanel
        itemId="42"
        loadSummary={loadSummary}
        restoreRevision={restore409}
      />,
    );
    fireEvent.click(screen.getByTestId("revisions-restore-1"));
    fireEvent.click(screen.getByTestId("revisions-restore-ok"));
    await waitFor(() =>
      expect(screen.getByTestId("revisions-restore-error").textContent).toMatch(
        /409/,
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(loadSummary).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("revisions-current")).toHaveAttribute(
      "data-current-rev",
      "2",
    );
  });

  it("shows an error when the loader fails", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    render(
      <RevisionsPanel
        itemId="42"
        loadSummary={async () => {
          throw new Error("nope");
        }}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
        "data-testid-state",
        "error",
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
  });

  it("compares two selected revisions and shows field diffs", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    const compare = vi.fn().mockResolvedValue({
      itemId: "42",
      rev1: 1,
      rev2: 2,
      fields: [
        {
          name: "displaytitle",
          leftValue: "old",
          rightValue: "new",
          changed: true,
        },
      ],
    });
    render(
      <RevisionsPanel
        itemId="42"
        loadSummary={async () => SAMPLE}
        compareRevisions={compare}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-compare-run")).toBeTruthy(),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("revisions-compare-run"));
    await waitFor(() =>
      expect(compare).toHaveBeenCalledWith("42", 1, 2),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("revisions-compare-row-displaytitle")).toHaveAttribute(
      "data-testid-changed",
      "true",
    );
  });

  it("shows 404 and 403 compare errors without treating them as empty", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    const compare404 = vi.fn().mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: {},
    });
    const { rerender } = render(
      <RevisionsPanel
        itemId="42"
        loadSummary={async () => SAMPLE}
        compareRevisions={compare404}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-compare-run")).toBeTruthy(),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("revisions-compare-run"));
    await waitFor(() =>
      expect(screen.getByTestId("revisions-compare-error")).toBeTruthy(),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("revisions-compare-error").textContent).toMatch(
      /404/,
    );
    expect(screen.queryByTestId("revisions-compare-table")).toBeNull();

    const compare403 = vi.fn().mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: {},
    });
    rerender(
      <RevisionsPanel
        itemId="42"
        loadSummary={async () => SAMPLE}
        compareRevisions={compare403}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-compare-run")).toBeTruthy(),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("revisions-compare-run"));
    await waitFor(() =>
      expect(screen.getByTestId("revisions-compare-error").textContent).toMatch(
        /403/,
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
  });

  const OTHER_ITEM = {
    restorable: true,
    currentRevision: 12,
    revisions: [
      {
        revId: 10,
        lastModifiedDate: "2026-02-01",
        lastModifier: "Admin",
        status: "Draft",
      },
      {
        revId: 12,
        lastModifiedDate: "2026-02-02",
        lastModifier: "Editor",
        status: "Live",
      },
    ],
    comments: [] as typeof SAMPLE.comments,
  };

  async function restoreThenReload(expectedCurrent: string) {
    fireEvent.click(screen.getByTestId("revisions-restore-1"));
    fireEvent.click(screen.getByTestId("revisions-restore-ok"));
    await waitFor(() =>
      expect(screen.getByTestId("revisions-current")).toHaveAttribute(
        "data-current-rev",
        expectedCurrent,
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
  }

  it("resets compare, pending, and errors when the item changes after restore", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    let loads42 = 0;
    const loadSummary = vi.fn(async (id: string) => {
      if (id === "99") {
        return OTHER_ITEM;
      }
      loads42 += 1;
      return {
        ...SAMPLE,
        currentRevision: loads42 === 1 ? 2 : 1,
      };
    });
    const restore = vi.fn().mockResolvedValue(undefined);
    const compare = vi.fn(
      async (id: string, rev1: number, rev2: number) => {
        if (id === "42") {
          throw { status: 404, statusText: "Not Found", body: {} };
        }
        return { itemId: id, rev1, rev2, fields: [] };
      },
    );
    const { container, rerender } = render(
      <RevisionsPanel
        itemId="42"
        loadSummary={loadSummary}
        restoreRevision={restore}
        compareRevisions={compare}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-compare-left")).toHaveValue("1"),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("revisions-compare-right")).toHaveValue("2");
    await restoreThenReload("1");
    fireEvent.click(screen.getByTestId("revisions-compare-run"));
    await waitFor(() =>
      expect(screen.getByTestId("revisions-compare-error")).toBeTruthy(),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("revisions-restore-2"));
    expect(screen.getByTestId("revisions-restore-confirm")).toBeTruthy();

    rerender(
      <RevisionsPanel
        itemId="99"
        loadSummary={loadSummary}
        restoreRevision={restore}
        compareRevisions={compare}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-current")).toHaveAttribute(
        "data-current-rev",
        "12",
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("revisions-restore-confirm")).toBeNull();
    expect(screen.queryByTestId("revisions-compare-error")).toBeNull();
    expect(screen.queryByTestId("revisions-restore-error")).toBeNull();
    expect(screen.getByTestId("revisions-compare-left")).toHaveValue("10");
    expect(screen.getByTestId("revisions-compare-right")).toHaveValue("12");
    fireEvent.click(screen.getByTestId("revisions-compare-run"));
    await waitFor(() => expect(compare).toHaveBeenCalledWith("99", 10, 12), { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT });
    expect(screen.queryByTestId("revisions-compare-error")).toBeNull();
    expect(screen.getByTestId("revisions-compare-empty")).toBeTruthy();
    await renderA11yGate(container);
  });

  it("loads the restored item fresh when it is selected again", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    let loads42 = 0;
    const loadSummary = vi.fn(async (id: string) => {
      if (id === "99") {
        return OTHER_ITEM;
      }
      loads42 += 1;
      return {
        ...SAMPLE,
        currentRevision: loads42 === 1 ? 2 : 1,
      };
    });
    const restore = vi.fn().mockResolvedValue(undefined);
    const compare = vi.fn(
      async (id: string, rev1: number, rev2: number) => ({
        itemId: id,
        rev1,
        rev2,
        fields: [] as [],
      }),
    );
    const { container, rerender } = render(
      <RevisionsPanel
        itemId="42"
        loadSummary={loadSummary}
        restoreRevision={restore}
        compareRevisions={compare}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-compare-left")).toHaveValue("1"),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    await restoreThenReload("1");
    rerender(
      <RevisionsPanel
        itemId="99"
        loadSummary={loadSummary}
        restoreRevision={restore}
        compareRevisions={compare}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-compare-left")).toHaveValue("10"),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("revisions-compare-right")).toHaveValue("12");
    rerender(
      <RevisionsPanel
        itemId="42"
        loadSummary={loadSummary}
        restoreRevision={restore}
        compareRevisions={compare}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-current")).toHaveAttribute(
        "data-current-rev",
        "1",
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
      "data-testid-state",
      "ok",
    );
    expect(screen.getByTestId("revisions-compare-left")).toHaveValue("1");
    expect(screen.getByTestId("revisions-compare-right")).toHaveValue("2");
    fireEvent.click(screen.getByTestId("revisions-compare-run"));
    await waitFor(() => expect(compare).toHaveBeenCalledWith("42", 1, 2), { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT });
    expect(compare).not.toHaveBeenCalledWith("42", 10, 12);
    await renderA11yGate(container);
  });

  it("shows a load error instead of Loading when the restored item fails on return", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    let loads42 = 0;
    const loadSummary = vi.fn(async (id: string) => {
      if (id === "99") {
        return OTHER_ITEM;
      }
      loads42 += 1;
      if (loads42 > 2) {
        throw new Error("return failed");
      }
      return { ...SAMPLE, currentRevision: loads42 === 1 ? 2 : 1 };
    });
    const restore = vi.fn().mockResolvedValue(undefined);
    const { rerender } = render(
      <RevisionsPanel
        itemId="42"
        loadSummary={loadSummary}
        restoreRevision={restore}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-restore-1")).toBeTruthy(),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    await restoreThenReload("1");
    rerender(
      <RevisionsPanel
        itemId="99"
        loadSummary={loadSummary}
        restoreRevision={restore}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-compare-left")).toHaveValue("10"),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    rerender(
      <RevisionsPanel
        itemId="42"
        loadSummary={loadSummary}
        restoreRevision={restore}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
        "data-testid-state",
        "error",
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByRole("alert").textContent).toMatch(/return failed/);
    expect(screen.queryByTestId("revisions-compare-left")).toBeNull();
  });

  it("shows a load error instead of staying on Loading when the next item fails after restore", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    let loads42 = 0;
    const loadSummary = vi.fn(async (id: string) => {
      if (id === "99") {
        throw new Error("nope");
      }
      loads42 += 1;
      return { ...SAMPLE, currentRevision: loads42 === 1 ? 2 : 1 };
    });
    const restore = vi.fn().mockResolvedValue(undefined);
    const { rerender } = render(
      <RevisionsPanel
        itemId="42"
        loadSummary={loadSummary}
        restoreRevision={restore}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-restore-1")).toBeTruthy(),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    await restoreThenReload("1");
    rerender(
      <RevisionsPanel
        itemId="99"
        loadSummary={loadSummary}
        restoreRevision={restore}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
        "data-testid-state",
        "error",
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByRole("alert").textContent).toMatch(/nope/);
    expect(screen.queryByTestId("revisions-compare-left")).toBeNull();
  });

  it("keeps the previous revisions when a same-item reload fails after restore", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    let loads = 0;
    const loadSummary = vi.fn(async () => {
      loads += 1;
      if (loads > 1) {
        throw new Error("reload failed");
      }
      return { ...SAMPLE, currentRevision: 2 };
    });
    const restore = vi.fn().mockResolvedValue(undefined);
    render(
      <RevisionsPanel
        itemId="42"
        loadSummary={loadSummary}
        restoreRevision={restore}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-restore-1")).toBeTruthy(),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("revisions-restore-1"));
    fireEvent.click(screen.getByTestId("revisions-restore-ok"));
    await waitFor(() =>
      expect(screen.getByTestId("revisions-restore-error").textContent).toMatch(
        /reload failed/,
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
      "data-testid-state",
      "ok",
    );
    expect(screen.getByTestId("revisions-current")).toHaveAttribute(
      "data-current-rev",
      "2",
    );
    expect(screen.getByTestId("revisions-compare-left")).toHaveValue("1");
    expect(screen.getByTestId("revisions-compare-right")).toHaveValue("2");
  });

  it("does not reload the new item when the previous restore finishes late", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    let resolveRestore: (value: void) => void = () => {};
    const restore = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          resolveRestore = resolve;
        }),
    );
    const onRestored = vi.fn();
    const loadSummary = vi.fn(async (id: string) => {
      if (id === "99") {
        return OTHER_ITEM;
      }
      return { ...SAMPLE, currentRevision: 2 };
    });
    const { rerender } = render(
      <RevisionsPanel
        itemId="42"
        loadSummary={loadSummary}
        restoreRevision={restore}
        onRestored={onRestored}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-restore-1")).toBeTruthy(),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("revisions-restore-1"));
    fireEvent.click(screen.getByTestId("revisions-restore-ok"));
    await waitFor(() => expect(restore).toHaveBeenCalledWith("42", 1), { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT });
    rerender(
      <RevisionsPanel
        itemId="99"
        loadSummary={loadSummary}
        restoreRevision={restore}
        onRestored={onRestored}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-current")).toHaveAttribute(
        "data-current-rev",
        "12",
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    const loads99 = loadSummary.mock.calls.filter((call) => call[0] === "99")
      .length;
    resolveRestore();
    await waitFor(() => expect(onRestored).toHaveBeenCalledWith(1), { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT });
    await waitFor(() =>
      expect(screen.getByTestId("revisions-compare-left")).toHaveValue("10"),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(
      loadSummary.mock.calls.filter((call) => call[0] === "99").length,
    ).toBe(loads99);
    expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
      "data-testid-state",
      "ok",
    );
    expect(screen.getByTestId("revisions-compare-right")).toHaveValue("12");
  });

  it("passes the a11y gate on the loaded revisions table", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    const { container } = render(
      <RevisionsPanel itemId="42" loadSummary={async () => SAMPLE} />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    await renderA11yGate(container);
  });

  it("shows date, user, type, and comment only after history loads (#5245)", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    let release: (value: typeof SAMPLE) => void = () => {};
    const loadSummary = vi.fn(
      () =>
        new Promise<typeof SAMPLE>((resolve) => {
          release = resolve;
        }),
    );
    const { container } = render(
      <RevisionsPanel
        itemId="42"
        initialTab="audit"
        loadSummary={loadSummary}
      />,
    );
    expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
      "data-testid-state",
      "loading",
    );
    expect(screen.queryByTestId("audit-row-0")).toBeNull();
    expect(screen.queryByTestId("revisions-audit-empty")).toBeNull();
    expect(screen.queryByTestId("revisions-audit-table")).toBeNull();
    expect(screen.queryByTestId("revisions-restore-1")).toBeNull();
    expect(screen.queryByTestId("revisions-compare-run")).toBeNull();

    release(SAMPLE);
    await waitFor(() =>
      expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
        "data-testid-tab",
        "audit",
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("audit-date-0")).toHaveTextContent("2026-01-02");
    expect(screen.getByTestId("audit-user-0")).toHaveTextContent("Admin");
    expect(screen.getByTestId("audit-type-0")).toHaveTextContent("Approve");
    expect(screen.getByTestId("audit-comment-0")).toHaveTextContent("Looks good");
    expect(screen.queryByTestId("revisions-restore-1")).toBeNull();
    expect(screen.queryByTestId("revisions-compare-run")).toBeNull();
    await renderA11yGate(container);
  });

  it("an empty history shows the empty message, not a fake row (#5245)", { timeout: REVISIONS_PANEL_TEST_TIMEOUT }, async () => {
    render(
      <RevisionsPanel
        itemId="42"
        initialTab="audit"
        loadSummary={async () => ({
          ...SAMPLE,
          comments: [
            {
              comment: " ",
              commenter: "",
              commentType: "",
              commentDate: " ",
            },
          ],
        })}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-audit-empty").textContent).toMatch(
        /No workflow comments/,
      ),
      { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("audit-row-0")).toBeNull();
    expect(screen.queryByTestId("revisions-audit-table")).toBeNull();
  });

  it.each([403, 404])(
    "HTTP %s stays in the audit panel and is not an empty trail (#5245)",
    { timeout: REVISIONS_PANEL_TEST_TIMEOUT },
    async (status) => {
      render(
        <RevisionsPanel
          itemId="42"
          initialTab="audit"
          loadSummary={async () => {
            throw {
              status,
              statusText: status === 403 ? "Forbidden" : "Not Found",
              body: {
                message: "No workflow comments are recorded for this item",
              },
            };
          }}
        />,
      );
      await waitFor(() =>
        expect(screen.getByTestId("revisions-load-error").textContent).toMatch(
          new RegExp(String(status)),
        ),
        { timeout: REVISIONS_PANEL_ROUND_TRIP_TIMEOUT },
      );
      expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
        "data-testid-state",
        "error",
      );
      expect(screen.queryByTestId("revisions-audit-empty")).toBeNull();
      expect(screen.queryByTestId("audit-row-0")).toBeNull();
      expect(screen.queryByTestId("revisions-audit-table")).toBeNull();
    },
  );
});
