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
import { ContentExplorerShell } from "../../../main/ts/contentExplorer/ContentExplorerShell";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import { renderA11yGate } from "./a11y";
import { mockFetch } from "./setup";

function stubPathFetch() {
  mockFetch(async (input) => {
    const url = typeof input === "string" ? input : (input as Request).url;
    if (url.includes("paginatedFolder") || url.includes("/folder/")) {
      return new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: [],
            childrenCount: 0,
            startIndex: 0,
          },
          PathItem: [],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }
    return new Response("{}", {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  });
}

describe("ContentExplorerShell product composition (#2400)", () => {
  it("renders search toggle, display format select, and server action toolbar", async () => {
    stubPathFetch();

    render(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => [
          {
            name: "FolderList",
            displayId: 3,
            displayName: "Folder list",
            columns: [{ source: "sys_title" }, { source: "sys_contenttypename" }],
          },
        ]}
        loadMenuActions={async () => [
          {
            name: "open",
            label: "Open",
            sortRank: 1,
            menuType: "MENUITEM",
          },
        ]}
      />,
    );

    expect(screen.getByTestId("content-explorer-shell")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-toggle-search")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-toggle-security")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-display-format")).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId("action-toolbar")).toBeInTheDocument();
    });

    const searchToggle = screen.getByTestId("explorer-toggle-search");
    expect(searchToggle.getAttribute("aria-expanded")).toBe("false");
    fireEvent.click(searchToggle);
    expect(searchToggle.getAttribute("aria-expanded")).toBe("true");
    expect(screen.getByTestId("explorer-search-panel")).toBeInTheDocument();
    expect(
      screen.getByTestId("explorer-search-panel").getAttribute("aria-label"),
    ).toContain("Search panel");

    const select = screen.getByTestId(
      "explorer-display-format",
    ) as HTMLSelectElement;
    await waitFor(() => {
      expect(
        Array.from(select.options).some((o) => o.text.includes("Folder list")),
      ).toBe(true);
    });
    fireEvent.change(select, { target: { value: "3" } });
    expect(select.value).toBe("3");
  });

  it("uses injected loaders only (no real network for menus/formats)", async () => {
    const loadDisplayFormats = vi.fn(async () => []);
    const loadMenuActions = vi.fn(async () => []);
    stubPathFetch();

    render(
      <ContentExplorerShell
        loadDisplayFormats={loadDisplayFormats}
        loadMenuActions={loadMenuActions}
      />,
    );

    await waitFor(() => {
      expect(loadDisplayFormats).toHaveBeenCalled();
      expect(loadMenuActions).toHaveBeenCalled();
    });
  });

  it("chrome labels are EXPLORER_MSG / message() keys (i18n FR-026)", () => {
    // Product chrome must use perc.ui.explorer@ keys — not bare English.
    const chromeKeys = [
      EXPLORER_MSG.TITLE,
      EXPLORER_MSG.SEARCH_TITLE,
      EXPLORER_MSG.SECURITY_TITLE,
      EXPLORER_MSG.DISPLAY_FORMAT_LABEL,
      EXPLORER_MSG.DISPLAY_FORMAT_DEFAULT,
      EXPLORER_MSG.SERVER_ACTIONS_ARIA,
      EXPLORER_MSG.VIEW_TOOLS_ARIA,
      EXPLORER_MSG.TOGGLE_SEARCH_ARIA,
      EXPLORER_MSG.TOGGLE_SECURITY_ARIA,
      EXPLORER_MSG.SEARCH_PANEL_REGION,
      EXPLORER_MSG.SECURITY_PANEL_REGION,
      EXPLORER_MSG.SECURITY_SELECT_FOLDER,
    ];
    for (const key of chromeKeys) {
      expect(key.startsWith("perc.ui.explorer@")).toBe(true);
    }
  });

  it("passes the zero serious/critical axe-core gate (T082a / 508)", async () => {
    stubPathFetch();
    const { container } = render(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("content-explorer-shell")).toBeInTheDocument();
    });
    await renderA11yGate(container);
  });

  it("passes axe with search panel expanded", async () => {
    stubPathFetch();
    const { container } = render(
      <ContentExplorerShell
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-toggle-search")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("explorer-toggle-search"));
    await waitFor(() =>
      expect(screen.getByTestId("explorer-search-panel")).toBeInTheDocument(),
    );
    await renderA11yGate(container);
  });
});
