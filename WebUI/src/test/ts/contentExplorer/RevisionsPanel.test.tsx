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
  it("renders revision rows and restore for a prior revision", async () => {
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
    );
    expect(screen.getByTestId("revisions-row-1")).toBeTruthy();
    expect(screen.getByTestId("revisions-row-2")).toBeTruthy();
    expect(screen.getByTestId("revisions-restore-1")).toBeTruthy();
    expect(screen.queryByTestId("revisions-restore-2")).toBeNull();
    fireEvent.click(screen.getByTestId("revisions-restore-1"));
    await waitFor(() => expect(restore).toHaveBeenCalledWith("42", 1));
  });

  it("opens on the audit tab when requested", async () => {
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
    );
    expect(screen.getByTestId("audit-row-0")).toHaveTextContent("Looks good");
  });

  it("does not restore when confirm is cancelled", async () => {
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
    );
    fireEvent.click(screen.getByTestId("revisions-restore-1"));
    expect(restore).not.toHaveBeenCalled();
  });

  it("shows the older revision as current only after restore succeeds (#5220)", async () => {
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
    await waitFor(() => expect(restore).toHaveBeenCalledWith("42", 1));
    expect(screen.getByTestId("revisions-current")).toHaveAttribute(
      "data-current-rev",
      "2",
    );
    resolveRestore();
    await waitFor(() => expect(loads).toBe(2));
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
    );
    expect(screen.queryByTestId("revisions-restore-1")).toBeNull();
    expect(screen.getByTestId("revisions-restore-2")).toBeTruthy();
  });

  it("HTTP 403 and 409 leave the current revision in place (#5220)", async () => {
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
    );
    fireEvent.click(screen.getByTestId("revisions-restore-1"));
    fireEvent.click(screen.getByTestId("revisions-restore-ok"));
    await waitFor(() =>
      expect(screen.getByTestId("revisions-restore-error").textContent).toMatch(
        /403/,
      ),
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
    );
    expect(loadSummary).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("revisions-current")).toHaveAttribute(
      "data-current-rev",
      "2",
    );
  });

  it("shows an error when the loader fails", async () => {
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
    );
  });

  it("compares two selected revisions and shows field diffs", async () => {
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
    );
    fireEvent.click(screen.getByTestId("revisions-compare-run"));
    await waitFor(() =>
      expect(compare).toHaveBeenCalledWith("42", 1, 2),
    );
    expect(screen.getByTestId("revisions-compare-row-displaytitle")).toHaveAttribute(
      "data-testid-changed",
      "true",
    );
  });

  it("shows 404 and 403 compare errors without treating them as empty", async () => {
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
    );
    fireEvent.click(screen.getByTestId("revisions-compare-run"));
    await waitFor(() =>
      expect(screen.getByTestId("revisions-compare-error")).toBeTruthy(),
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
    );
    fireEvent.click(screen.getByTestId("revisions-compare-run"));
    await waitFor(() =>
      expect(screen.getByTestId("revisions-compare-error").textContent).toMatch(
        /403/,
      ),
    );
  });

  it("passes the a11y gate on the loaded revisions table", async () => {
    const { container } = render(
      <RevisionsPanel itemId="42" loadSummary={async () => SAMPLE} />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    await renderA11yGate(container);
  });
});
