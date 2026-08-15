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
