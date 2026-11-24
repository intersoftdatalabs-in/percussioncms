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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactElement } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";
import { ContentExplorerShell } from "../../../main/ts/contentExplorer/ContentExplorerShell";
import { jsonResponse, mockFetch } from "./setup";

/**
 * Single wall-clock budget for every polling gate in this file (#7207).
 * Each round trip here is purely microtask-driven (mocked promises
 * resolve, then setState, then render) with no real timers involved,
 * so the gates are deterministic in outcome; the budget is strictly a
 * backstop for loaded-worker scheduling lag. Every {@code findBy} and
 * {@code waitFor} uses it explicitly: the 1s/5s testing-library
 * defaults are the most-tripped cliffs when hundreds of suite files
 * share the worker host (same rationale as {@code RETRY_ROUND_TRIP_TIMEOUT}
 * in #7122 and the #7147 bin-lifecycle slice).
 */
const RESTORE_REVISION_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7207, same load-contention rationale as
 * the #7147 slice). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const RESTORE_REVISION_TEST_TIMEOUT = 30_000;

const adminBootstrap: SpaBootstrap = {
  userName: "Admin",
  locale: "en-us",
  entry: "explorer",
  isAdmin: true,
  isDesigner: false,
  isWidgetBuilderActive: false,
  allowExternalAvatarFetch: true,
};

const PAGE = {
  id: "42",
  name: "Home",
  path: "/Sites/Demo/Home",
  type: "percPage",
  category: "page",
  leaf: true,
};

const FOLDER = {
  id: "fold-1",
  name: "News",
  path: "/Sites/Demo/News/",
  type: "folder",
  category: "folder",
  leaf: false,
};

const REVISIONS = [
  {
    name: "Workflow_Revisions",
    label: "Revisions",
    sortRank: 1,
    menuType: "MENUITEM" as const,
  },
];

function renderShell(ui: ReactElement) {
  return render(
    <BootstrapProvider value={adminBootstrap}>{ui}</BootstrapProvider>,
  );
}

function requestUrl(input: RequestInfo | URL): string {
  if (typeof input === "string") return input;
  if (input instanceof Request) return input.url;
  return String(input);
}

function stubList(children: unknown[]) {
  mockFetch(async (input: RequestInfo | URL) => {
    const url = requestUrl(input);
    if (url.includes("paginatedFolder") || url.includes("/path/folder/")) {
      return jsonResponse({
        PagedItemList: {
          childrenInPage: children,
          childrenCount: children.length,
          startIndex: 0,
        },
        PathItem: [],
      });
    }
    return jsonResponse({});
  });
}

describe("Explorer revisions panel restore one older revision (#5220)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("an empty selection does not claim a restore", { timeout: RESTORE_REVISION_TEST_TIMEOUT }, async () => {
    stubList([PAGE, FOLDER]);
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => REVISIONS}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-42")).toBeInTheDocument(),
      { timeout: RESTORE_REVISION_ROUND_TRIP_TIMEOUT },
    );
    const revisions = await screen.findByTestId(
      "action-toolbar-item-Workflow_Revisions",
      undefined,
      { timeout: RESTORE_REVISION_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(revisions);
    expect(await screen.findByTestId("explorer-revisions-hint", undefined, { timeout: RESTORE_REVISION_ROUND_TRIP_TIMEOUT })).toBeTruthy();
    expect(screen.queryByTestId("revisions-restore-1")).toBeNull();
    expect(screen.queryByTestId("explorer-flush-cache-status")).toBeNull();
    expect(screen.getByTestId("explorer-server-actions-error").textContent).toMatch(
      /Select a content item first/,
    );
  });

  it("a folder does not claim a restore", { timeout: RESTORE_REVISION_TEST_TIMEOUT }, async () => {
    stubList([PAGE, FOLDER]);
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => REVISIONS}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-fold-1")).toBeInTheDocument(),
      { timeout: RESTORE_REVISION_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("detail-row-fold-1"));
    const revisions = await screen.findByTestId(
      "action-toolbar-item-Workflow_Revisions",
      undefined,
      { timeout: RESTORE_REVISION_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(revisions);
    expect(await screen.findByTestId("explorer-revisions-hint", undefined, { timeout: RESTORE_REVISION_ROUND_TRIP_TIMEOUT })).toBeTruthy();
    expect(screen.queryByTestId("revisions-restore-1")).toBeNull();
    expect(screen.queryByTestId("explorer-flush-cache-status")).toBeNull();
    expect(screen.getByTestId("explorer-server-actions-error").textContent).toMatch(
      /Select a content item first/,
    );
  });
});
