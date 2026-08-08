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
import { mockFetch } from "./setup";

describe("ContentExplorerShell product composition (#2400)", () => {
  it("renders search toggle, display format select, and server action toolbar", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      // Tree / list path calls — return empty paginated / folder payloads.
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

    fireEvent.click(screen.getByTestId("explorer-toggle-search"));
    expect(screen.getByTestId("explorer-search-panel")).toBeInTheDocument();

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
    mockFetch(async () => {
      return new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: [],
            childrenCount: 0,
            startIndex: 0,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });

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
});
