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
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";
import { ContentExplorerShell } from "../../../main/ts/contentExplorer/ContentExplorerShell";
import { recentStorageKey } from "../../../main/ts/contentExplorer/recentItems";
import { renderA11yGate } from "./a11y";
import { mockFetch } from "./setup";

const adminBootstrap: SpaBootstrap = {
  userName: "Admin",
  locale: "en-us",
  entry: "explorer",
  isAdmin: true,
  isDesigner: false,
  isWidgetBuilderActive: false,
  allowExternalAvatarFetch: true,
};

const PAGE_ID = "16777215-101-551";
const FOLDER_ID = "16777215-101-703";

function renderShell(ui: ReactElement) {
  return render(<BootstrapProvider value={adminBootstrap}>{ui}</BootstrapProvider>);
}

function stubList(children: unknown[]) {
  mockFetch(async () =>
    new Response(
      JSON.stringify({
        PagedItemList: {
          childrenInPage: children,
          childrenCount: children.length,
          startIndex: 0,
        },
        PathItem: [],
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    ),
  );
}

function openRecent(): void {
  fireEvent.click(screen.getByTestId("explorer-view-tool-recent"));
}

describe("Explorer recent-items quick access (#6256)", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  afterEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it("records an opened page in recents and re-opens it from the panel", async () => {
    stubList([
      {
        id: PAGE_ID,
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        leaf: true,
      },
    ]);
    const onOpenItem = vi.fn();
    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        onOpenItem={onOpenItem}
        checkRecentItem={async () => true}
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId(`detail-row-${PAGE_ID}`)).toBeInTheDocument(),
    );
    fireEvent.doubleClick(screen.getByTestId(`detail-row-${PAGE_ID}`));
    await waitFor(() => expect(onOpenItem).toHaveBeenCalled());
    // Persisted per-operator (Admin bootstrap).
    const stored = JSON.parse(
      window.localStorage.getItem(recentStorageKey("Admin")) ?? "[]",
    );
    expect(stored.map((e: { path: string }) => e.path)).toEqual([
      "/Sites/Demo/Home",
    ]);
    openRecent();
    await waitFor(() =>
      expect(screen.getByTestId("recent-panel")).toBeInTheDocument(),
    );
    const row = screen.getByTestId("recent-item-row");
    expect(row).toHaveAttribute("data-recent-kind", "item");
    expect(row).toHaveAttribute("data-recent-key", `id:${PAGE_ID}`);
    // Activating the row re-opens the item (probe passes, editor opens).
    onOpenItem.mockClear();
    fireEvent.click(screen.getByTestId("recent-item-open"));
    await waitFor(() => expect(onOpenItem).toHaveBeenCalled());
    const reopened = onOpenItem.mock.calls[0][0] as { id?: string };
    expect(reopened.id).toBe(PAGE_ID);
    await renderA11yGate(container);
  });

  it("records an opened folder with browse affordance", async () => {
    stubList([
      {
        id: FOLDER_ID,
        name: "Demo",
        path: "/Sites/Demo/",
        type: "folder",
        leaf: false,
      },
    ]);
    const onOpenItem = vi.fn();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        onOpenItem={onOpenItem}
        checkRecentItem={async () => true}
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId(`detail-row-${FOLDER_ID}`)).toBeInTheDocument(),
    );
    // Folders browse — the editor never opens for them.
    fireEvent.doubleClick(screen.getByTestId(`detail-row-${FOLDER_ID}`));
    await waitFor(() =>
      expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
        "data-selected-folder-path",
        "/Sites/Demo/",
      ),
    );
    expect(onOpenItem).not.toHaveBeenCalled();
    openRecent();
    await waitFor(() =>
      expect(screen.getByTestId("recent-panel")).toBeInTheDocument(),
    );
    const row = screen.getByTestId("recent-item-row");
    expect(row).toHaveAttribute("data-recent-kind", "folder");
    // Activating the folder row browses back to it.
    fireEvent.click(screen.getByTestId("recent-item-open"));
    await waitFor(() =>
      expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
        "data-selected-folder-path",
        "/Sites/Demo/",
      ),
    );
    expect(onOpenItem).not.toHaveBeenCalled();
  });

  it("drops a deleted entry with a status notice, not an error", async () => {
    stubList([]);
    window.localStorage.setItem(
      recentStorageKey("Admin"),
      JSON.stringify([
        {
          id: "999",
          name: "Gone",
          path: "/Sites/Demo/Gone",
          kind: "item",
          openedAt: new Date("2026-01-01T00:00:00.000Z").toISOString(),
        },
      ]),
    );
    // Another operator's recents must not leak into this shell.
    window.localStorage.setItem(
      recentStorageKey("Designer"),
      JSON.stringify([
        {
          id: "1",
          name: "Theirs",
          path: "/Sites/Other",
          kind: "item",
          openedAt: new Date("2026-01-01T00:00:00.000Z").toISOString(),
        },
      ]),
    );
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        checkRecentItem={async () => false}
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-content")).toBeInTheDocument(),
    );
    openRecent();
    await waitFor(() =>
      expect(screen.getByTestId("recent-panel")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("recent-item-row")).toHaveAttribute(
      "data-recent-key",
      "id:999",
    );
    fireEvent.click(screen.getByTestId("recent-item-open"));
    await waitFor(() =>
      expect(screen.queryByTestId("recent-item-row")).toBeNull(),
    );
    // Dropped with a polite status — never an alert/error.
    const notice = screen.getByTestId("recent-status");
    expect(notice).toHaveAttribute("role", "status");
    expect(notice).toHaveTextContent("no longer available");
    expect(screen.queryByRole("alert")).toBeNull();
    expect(screen.getByTestId("recent-empty")).toBeInTheDocument();
    const stored = JSON.parse(
      window.localStorage.getItem(recentStorageKey("Admin")) ?? "[]",
    );
    expect(stored).toEqual([]);
  });
});
