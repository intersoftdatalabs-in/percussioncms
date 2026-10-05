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

  it("an empty selection does not claim a restore", async () => {
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
    );
    const revisions = await screen.findByTestId(
      "action-toolbar-item-Workflow_Revisions",
    );
    fireEvent.click(revisions);
    expect(await screen.findByTestId("explorer-revisions-hint")).toBeTruthy();
    expect(screen.queryByTestId("revisions-restore-1")).toBeNull();
    expect(screen.queryByTestId("explorer-flush-cache-status")).toBeNull();
    expect(screen.getByTestId("explorer-server-actions-error").textContent).toMatch(
      /Select a content item first/,
    );
  });

  it("a folder does not claim a restore", async () => {
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
    );
    fireEvent.click(screen.getByTestId("detail-row-fold-1"));
    const revisions = await screen.findByTestId(
      "action-toolbar-item-Workflow_Revisions",
    );
    fireEvent.click(revisions);
    expect(await screen.findByTestId("explorer-revisions-hint")).toBeTruthy();
    expect(screen.queryByTestId("revisions-restore-1")).toBeNull();
    expect(screen.queryByTestId("explorer-flush-cache-status")).toBeNull();
    expect(screen.getByTestId("explorer-server-actions-error").textContent).toMatch(
      /Select a content item first/,
    );
  });
});
