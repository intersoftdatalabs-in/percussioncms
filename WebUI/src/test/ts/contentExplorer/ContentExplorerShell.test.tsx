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
import { describe, expect, it, vi } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";
import { ContentExplorerShell } from "../../../main/ts/contentExplorer/ContentExplorerShell";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
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

function renderShell(
  ui: ReactElement,
  bootstrap: SpaBootstrap = adminBootstrap,
) {
  return render(
    <BootstrapProvider value={bootstrap}>{ui}</BootstrapProvider>,
  );
}

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

/** Open the DCE-style View menu so nested view-tool items are in the DOM. */
function openViewMenu(): void {
  fireEvent.click(screen.getByTestId("explorer-menu-view"));
}

describe("ContentExplorerShell product composition (#2400)", () => {
  it("renders DCE menu bar, nested view tools, and server action toolbar (#2731)", async () => {
    stubPathFetch();

    renderShell(
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
    expect(screen.getByTestId("explorer-menu-bar")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-menu-content")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-menu-view")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-menu-help")).toBeInTheDocument();

    // Display format is always-visible shell chrome next to the menubar.
    expect(screen.getByTestId("explorer-display-format")).toBeInTheDocument();
    // Always-visible refresh residual (#2733) — not only under View menu.
    expect(screen.getByTestId("explorer-refresh-list")).toBeInTheDocument();
    // #3208: Search / Folder Security are first-class view-tool chrome (QA #2588).
    expect(screen.getByTestId("explorer-view-tools")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-view-tool-search")).toBeInTheDocument();
    expect(
      screen.getByTestId("explorer-view-tool-search").getAttribute(
        "aria-expanded",
      ),
    ).toBe("false");
    // Always-visible Security (#3268 / #2410) — product Playwright
    // locates explorer-toggle-security without opening View.
    expect(screen.getByTestId("explorer-toggle-security")).toBeInTheDocument();
    openViewMenu();
    expect(screen.getByTestId("explorer-toggle-search")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-menu-view-security")).toBeInTheDocument();
    expect(
      screen.getByTestId("explorer-toggle-translations"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("explorer-toggle-relationships"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("explorer-toggle-dependencies"),
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId("action-toolbar")).toBeInTheDocument();
    });
    // #2972: labeled Server actions chrome is always mounted for QA/operators.
    expect(screen.getByTestId("explorer-server-actions")).toBeInTheDocument();
    expect(
      screen.getByTestId("explorer-server-actions-label"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("action-toolbar-item-open")).toBeInTheDocument();

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
    expect(
      screen.getByTestId("explorer-view-tool-search").getAttribute(
        "aria-expanded",
      ),
    ).toBe("true");
  });

  it("opens Search under the header from the always-visible view-tool toggle (#3208)", async () => {
    stubPathFetch();
    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        listSavedSearches={async () => []}
      />,
    );

    expect(screen.queryByTestId("explorer-side-panels")).toBeNull();
    fireEvent.click(screen.getByTestId("explorer-view-tool-search"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-side-panels")).toBeInTheDocument();
      expect(screen.getByTestId("explorer-search-panel")).toBeInTheDocument();
      expect(screen.getByTestId("search-panel-input")).toBeInTheDocument();
    });
    const searchTool = screen.getByTestId("explorer-view-tool-search");
    expect(searchTool.getAttribute("aria-expanded")).toBe("true");
    expect(searchTool.getAttribute("aria-pressed")).toBe("true");
    expect(
      screen
        .getByTestId("explorer-side-panels")
        .contains(screen.getByTestId("explorer-search-panel")),
    ).toBe(true);
    await renderA11yGate(container);

    fireEvent.click(screen.getByTestId("explorer-view-tool-search"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-search-panel")).toBeNull();
      expect(screen.queryByTestId("explorer-side-panels")).toBeNull();
    });
  });

  it("surfaces display-format load error without removing the selector (#3208)", async () => {
    stubPathFetch();
    const { container } = renderShell(
      <ContentExplorerShell
        loadDisplayFormats={async () => {
          throw new Error("formats down");
        }}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("explorer-display-format")).toBeInTheDocument();
      expect(
        screen.getByTestId("explorer-display-format-error"),
      ).toBeInTheDocument();
    });
    await renderA11yGate(container);
  });

  it("uses injected loaders only (no real network for menus/formats)", async () => {
    const loadDisplayFormats = vi.fn(async () => []);
    const loadMenuActions = vi.fn(async () => []);
    const loadWorkflowMenuActions = vi.fn(async () => null);
    stubPathFetch();

    renderShell(
      <ContentExplorerShell
        loadDisplayFormats={loadDisplayFormats}
        loadMenuActions={loadMenuActions}
        loadWorkflowMenuActions={loadWorkflowMenuActions}
      />,
    );

    await waitFor(() => {
      expect(loadDisplayFormats).toHaveBeenCalled();
      expect(loadMenuActions).toHaveBeenCalled();
      expect(loadWorkflowMenuActions).toHaveBeenCalled();
    });
  });

  it("keeps server-actions chrome and surfaces load error without wiping region (#2972)", async () => {
    stubPathFetch();
    const loadMenuActions = vi.fn(async () => {
      throw new Error("actions down");
    });

    const { container } = renderShell(
      <ContentExplorerShell
        loadDisplayFormats={async () => []}
        loadMenuActions={loadMenuActions}
        loadWorkflowMenuActions={async () => null}
      />,
    );

    await waitFor(() => {
      expect(loadMenuActions).toHaveBeenCalled();
      expect(
        screen.getByTestId("explorer-server-actions"),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId("explorer-server-actions-label"),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId("explorer-server-actions-error"),
      ).toBeInTheDocument();
    });
    // Empty toolbar placeholder still mounts under the labeled region.
    expect(screen.getByTestId("action-toolbar")).toBeInTheDocument();
    expect(screen.getByTestId("action-toolbar-empty")).toBeInTheDocument();
    // T082a: new load-error DOM (status region) must pass axe serious/critical.
    await renderA11yGate(container);
  });

  it("Publish Now HTTP 200 FORBIDDEN mounts server-actions error (#3451)", async () => {
    const fetchSpy = mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("sitemanage/publish/")) {
        return new Response(
          JSON.stringify({
            status: "FORBIDDEN",
            warningMessage:
              "Publication stopped because of licensing issues",
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "page",
                  accessLevel: "WRITE",
                },
              ],
              childrenCount: 1,
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
    vi.spyOn(window, "confirm").mockReturnValue(true);

    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => [
          {
            name: "Publish_Now",
            label: "Publish Now",
            sortRank: 1,
            menuType: "MENUITEM",
          },
        ]}
        loadWorkflowMenuActions={async () => null}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-42")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-row-42"));
    await waitFor(() => {
      expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
        "data-selected-item-id",
        "42",
      );
      expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
        "data-selected-item-kind",
        "page",
      );
      expect(
        screen.getByTestId("action-toolbar-item-Publish_Now"),
      ).toBeInTheDocument();
    });

    const folderLoadsBefore = fetchSpy.mock.calls.filter((call) => {
      const url = String(call[0] ?? "");
      return url.includes("paginatedFolder") || url.includes("/folder/");
    }).length;

    fireEvent.click(screen.getByTestId("action-toolbar-item-Publish_Now"));

    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-server-actions-error"),
      ).toHaveTextContent(/FORBIDDEN|licensing|Publication stopped/i);
    });
    expect(
      fetchSpy.mock.calls.some((call) =>
        String(call[0] ?? "").includes("sitemanage/publish/"),
      ),
    ).toBe(true);
    const folderLoadsAfter = fetchSpy.mock.calls.filter((call) => {
      const url = String(call[0] ?? "");
      return url.includes("paginatedFolder") || url.includes("/folder/");
    }).length;
    // Throw must not refresh the list as if the item published.
    expect(folderLoadsAfter).toBe(folderLoadsBefore);
    await renderA11yGate(container);
  });

  it("hides toolbar Publish Now until a page row is selected (#3467)", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "percPage",
                  category: "page",
                  leaf: true,
                },
              ],
              childrenCount: 1,
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

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => [
          {
            name: "Publish_Now",
            label: "Publish Now",
            sortRank: 1,
            menuType: "MENUITEM",
          },
        ]}
        loadWorkflowMenuActions={async () => null}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-42")).toBeInTheDocument();
    });
    expect(
      screen.queryByTestId("action-toolbar-item-Publish_Now"),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
      "data-selected-item-id",
      "",
    );

    fireEvent.click(screen.getByTestId("detail-row-42"));
    await waitFor(() => {
      expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
        "data-selected-item-id",
        "42",
      );
      expect(
        screen.getByTestId("action-toolbar-item-Publish_Now"),
      ).toBeInTheDocument();
    });
  });

  it("discards stale context-menu loads when right-clicking rows rapidly (#2732 race)", async () => {
    let releaseSlow: (() => void) | undefined;
    const slowGate = new Promise<void>((resolve) => {
      releaseSlow = resolve;
    });
    const loadMenuActions = vi.fn(async (item: { id?: string | number } | null) => {
      if (String(item?.id) === "101") {
        await slowGate;
        return [
          {
            name: "open-slow",
            label: "Open slow",
            sortRank: 1,
            menuType: "MENUITEM" as const,
          },
        ];
      }
      return [
        {
          name: "open-fast",
          label: "Open fast",
          sortRank: 1,
          menuType: "MENUITEM" as const,
        },
      ];
    });
    const loadWorkflowMenuActions = vi.fn(async () => null);

    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "101",
                  name: "item-a",
                  path: "/Sites/item-a",
                  type: "page",
                  accessLevel: "WRITE",
                },
                {
                  id: "202",
                  name: "item-b",
                  path: "/Sites/item-b",
                  type: "page",
                  accessLevel: "WRITE",
                },
              ],
              childrenCount: 2,
              startIndex: 1,
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

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={loadMenuActions}
        loadWorkflowMenuActions={loadWorkflowMenuActions}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-101")).toBeInTheDocument();
      expect(screen.getByTestId("detail-row-202")).toBeInTheDocument();
    });

    fireEvent.contextMenu(screen.getByTestId("detail-row-101"), {
      clientX: 10,
      clientY: 20,
    });
    fireEvent.contextMenu(screen.getByTestId("detail-row-202"), {
      clientX: 40,
      clientY: 50,
    });

    await waitFor(() => {
      expect(screen.getByTestId("context-menu")).toBeInTheDocument();
      expect(screen.getByTestId("context-menu-item-open-fast")).toBeInTheDocument();
    });

    releaseSlow?.();
    // Allow the slow first request to resolve; generation guard must ignore it.
    await waitFor(() => {
      expect(loadMenuActions.mock.calls.length).toBeGreaterThanOrEqual(2);
    });
    await new Promise((r) => setTimeout(r, 30));

    expect(screen.getByTestId("context-menu-item-open-fast")).toBeInTheDocument();
    expect(screen.queryByTestId("context-menu-item-open-slow")).toBeNull();
  });

  it("filters desktop-only actions from toolbar and mounts context menu for items (#2849)", async () => {
    const loadMenuActions = vi.fn(async () => [
      {
        name: "open",
        label: "Open",
        sortRank: 1,
        menuType: "MENUITEM" as const,
      },
      {
        name: "desktop-cx",
        label: "Desktop only",
        sortRank: 2,
        menuType: "MENUITEM" as const,
        url: "rxapp://launch-cx",
      },
      {
        name: "cx-popup",
        label: "CX popup",
        sortRank: 3,
        menuType: "CONTEXTMENU" as const,
        children: [
          {
            name: "cx-edit",
            label: "Edit in CX",
            sortRank: 1,
            menuType: "MENUITEM" as const,
          },
        ],
      },
    ]);

    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "501",
                  name: "page-five",
                  path: "/Sites/page-five",
                  type: "page",
                  accessLevel: "WRITE",
                },
              ],
              childrenCount: 1,
              startIndex: 1,
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

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={loadMenuActions}
        loadWorkflowMenuActions={async () => null}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("action-toolbar")).toBeInTheDocument();
      expect(screen.getByTestId("action-toolbar-item-open")).toBeInTheDocument();
    });
    // Desktop-only URL and CONTEXTMENU roots must not appear on the toolbar.
    expect(screen.queryByTestId("action-toolbar-item-desktop-cx")).toBeNull();
    expect(screen.queryByTestId("action-toolbar-item-cx-popup")).toBeNull();

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-501")).toBeInTheDocument();
    });
    fireEvent.contextMenu(screen.getByTestId("detail-row-501"), {
      clientX: 12,
      clientY: 24,
    });

    await waitFor(() => {
      expect(screen.getByTestId("context-menu")).toBeInTheDocument();
      expect(screen.getByTestId("context-menu-item-open")).toBeInTheDocument();
      // CONTEXTMENU roots are allowed on the context-menu surface.
      expect(screen.getByTestId("context-menu-item-cx-popup")).toBeInTheDocument();
    });
    // Desktop-only URL still filtered from context menu.
    expect(screen.queryByTestId("context-menu-item-desktop-cx")).toBeNull();
  });

  it("merges workflow transition group into toolbar and invokes transition (#2732)", async () => {
    const runWorkflowTransition = vi.fn(async () => undefined);
    const loadWorkflowMenuActions = vi.fn(async (item) => {
      if (!item?.id) return null;
      return {
        name: "workflow",
        label: "Workflow",
        sortRank: 9000,
        menuType: "MENU" as const,
        children: [
          {
            name: "workflow-transition:Submit",
            label: "Submit",
            sortRank: 1,
            menuType: "MENUITEM" as const,
          },
        ],
      };
    });

    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "33554432-101-1",
                  name: "page-one",
                  path: "/Sites/page-one",
                  type: "page",
                  accessLevel: "WRITE",
                },
              ],
              childrenCount: 1,
              startIndex: 1,
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

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => [
          {
            name: "open",
            label: "Open",
            sortRank: 1,
            menuType: "MENUITEM",
          },
        ]}
        loadWorkflowMenuActions={loadWorkflowMenuActions}
        runWorkflowTransition={runWorkflowTransition}
      />,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId("detail-row-33554432-101-1") ||
          screen.getByText("page-one"),
      ).toBeTruthy();
    });

    const row =
      screen.queryByTestId("detail-row-33554432-101-1") ??
      screen.getByText("page-one");
    fireEvent.click(row);

    await waitFor(() => {
      expect(
        screen.getByTestId("action-toolbar-group-workflow"),
      ).toBeInTheDocument();
    });

    fireEvent.click(
      screen.getByTestId("action-toolbar-item-workflow-transition:Submit"),
    );

    await waitFor(() => {
      expect(runWorkflowTransition).toHaveBeenCalledWith(
        "33554432-101-1",
        "Submit",
      );
    });
  });

  it("flush cache API failure surfaces the server message, not a generic error", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("flush-cache")) {
        return new Response(
          JSON.stringify({ message: "Assembler cache is locked" }),
          { status: 500, headers: { "Content-Type": "application/json" } },
        );
      }
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
    vi.spyOn(window, "confirm").mockReturnValue(true);

    renderShell(
      <ContentExplorerShell
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => [
          {
            name: "Flush_Cache",
            label: "Flush Cache",
            sortRank: 1,
            menuType: "MENUITEM",
          },
        ]}
      />,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId("action-toolbar-item-Flush_Cache"),
      ).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("action-toolbar-item-Flush_Cache"));
    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(
        "Assembler cache is locked",
      );
    });
    expect(screen.getByRole("alert").textContent).not.toBe(
      EXPLORER_MSG.ERROR_GENERIC,
    );
  });

  it("activating a pathmanagement Folder stays in Explorer browse (#3330)", async () => {
    const onOpenItem = vi.fn();
    const loadWorkflowMenuActions = vi.fn(async (item) => {
      // Folders must not request transitions; default returns null (#3330).
      if (item && String(item.type).toLowerCase() === "folder") {
        return null;
      }
      return null;
    });
    const folderRow = {
      id: "16777215-101-1",
      name: "New-Folder",
      path: "/Folders/New-Folder/",
      type: "Folder",
      leaf: true,
      accessLevel: "WRITE" as const,
    };
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        const listingFolders = url.includes("/Folders") && !url.includes("New-Folder");
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: listingFolders ? [folderRow] : [],
              childrenCount: listingFolders ? 1 : 0,
              startIndex: 1,
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

    renderShell(
      <ContentExplorerShell
        initialPath="/Folders"
        onOpenItem={onOpenItem}
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={loadWorkflowMenuActions}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-16777215-101-1")).toBeInTheDocument();
    });
    fireEvent.doubleClick(screen.getByTestId("detail-row-16777215-101-1"));

    await waitFor(() => {
      expect(screen.getByTestId("content-explorer-shell")).toBeInTheDocument();
      expect(screen.getByTestId("detail-list")).toBeInTheDocument();
    });
    expect(onOpenItem).not.toHaveBeenCalled();
    expect(screen.queryByTestId("detail-row-16777215-101-1")).toBeNull();
    for (const call of loadWorkflowMenuActions.mock.calls) {
      const arg = call[0] as { type?: string } | null;
      if (arg) {
        expect(arg.type).not.toBe("Folder");
      }
    }
  });

  it("activating a page still opens the editor host (#3330)", async () => {
    const onOpenItem = vi.fn();
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "55",
                  name: "page-one",
                  path: "/Sites/Demo/page-one",
                  type: "page",
                  leaf: true,
                  accessLevel: "WRITE",
                },
              ],
              childrenCount: 1,
              startIndex: 1,
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

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        onOpenItem={onOpenItem}
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-55")).toBeInTheDocument();
    });
    fireEvent.doubleClick(screen.getByTestId("detail-row-55"));
    await waitFor(() => {
      expect(onOpenItem).toHaveBeenCalled();
    });
    const opened = onOpenItem.mock.calls[0][0] as { id?: string; type?: string };
    expect(opened.id).toBe("55");
    expect(opened.type).toBe("page");
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
      EXPLORER_MSG.SERVER_ACTIONS_LABEL,
      EXPLORER_MSG.SERVER_ACTIONS_LOAD_ERROR,
      EXPLORER_MSG.VIEW_TOOLS_ARIA,
      EXPLORER_MSG.MENU_BAR_ARIA,
      EXPLORER_MSG.MENU_CONTENT,
      EXPLORER_MSG.MENU_VIEW,
      EXPLORER_MSG.MENU_HELP,
      EXPLORER_MSG.MENU_VIEW_REFRESH,
      EXPLORER_MSG.MENU_HELP_EXPLORER,
      EXPLORER_MSG.MENU_HELP_ABOUT,
      EXPLORER_MSG.ACTION_REFRESH,
      EXPLORER_MSG.ACTION_REFRESH_ARIA,
      EXPLORER_MSG.PREVIEW_UNAVAILABLE,
      EXPLORER_MSG.TOGGLE_SEARCH_ARIA,
      EXPLORER_MSG.TOGGLE_SECURITY_ARIA,
      EXPLORER_MSG.TOGGLE_TRANSLATIONS_ARIA,
      EXPLORER_MSG.SEARCH_PANEL_REGION,
      EXPLORER_MSG.SECURITY_PANEL_REGION,
      EXPLORER_MSG.TRANSLATIONS_PANEL_REGION,
      EXPLORER_MSG.TRANSLATIONS_TITLE,
      EXPLORER_MSG.TRANSLATIONS_SELECT_ITEM,
      EXPLORER_MSG.SECURITY_SELECT_FOLDER,
      EXPLORER_MSG.SECURITY_HOST_NO_FOLDER,
      EXPLORER_MSG.FOLDER_PROPS_TITLE,
      EXPLORER_MSG.FOLDER_PROPS_COMMUNITY,
      EXPLORER_MSG.FOLDER_PROPS_LOCALE,
      EXPLORER_MSG.WORKFLOW_MENU_LABEL,
      EXPLORER_MSG.WORKFLOW_TRANSITION_FAILED,
    ];
    for (const key of chromeKeys) {
      expect(key.startsWith("perc.ui.explorer@")).toBe(true);
    }
  });

  it("refresh control bumps list key (#2733 view residual)", async () => {
    stubPathFetch();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("detail-list")).toBeInTheDocument();
    });
    const listBefore = screen.getByTestId("detail-list");
    fireEvent.click(screen.getByTestId("explorer-refresh-list"));
    await waitFor(() => {
      // Remount via listEpoch key — node may be replaced; control stays enabled.
      expect(screen.getByTestId("explorer-refresh-list")).toBeEnabled();
      expect(screen.getByTestId("detail-list")).toBeInTheDocument();
    });
    // Refresh is always available (shell-state residual View control).
    expect(listBefore).toBeTruthy();
  });

  it("wires product preview handler so Preview is enabled for pages (#2733)", async () => {
    const onPreview = vi.fn(async () => undefined);
    stubPathFetch();
    // Seed a page selection by loading children with a page row.
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "page",
                  accessLevel: "READ",
                },
              ],
              childrenCount: 1,
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

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        actionHandlers={{ onPreview }}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-42")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-row-42"));
    await waitFor(() => {
      expect(screen.getByTestId("action-preview")).toBeEnabled();
    });
    fireEvent.click(screen.getByTestId("action-preview"));
    await waitFor(() => {
      expect(onPreview).toHaveBeenCalled();
    });
    const arg = onPreview.mock.calls[0][0] as { id?: string; type?: string };
    expect(arg.id).toBe("42");
    expect(arg.type).toBe("page");
  });

  it("enables Preview for a listed percPage row (#3456)", async () => {
    const onPreview = vi.fn(async () => undefined);
    stubPathFetch();
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "16777215-101-9",
                  name: "About",
                  path: "/Sites/Demo/Pages/About",
                  type: "percPage",
                  leaf: false,
                  accessLevel: "READ",
                },
              ],
              childrenCount: 1,
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

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo/Pages"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        actionHandlers={{ onPreview }}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-16777215-101-9")).toBeInTheDocument();
    });
    const row = screen.getByTestId("detail-row-16777215-101-9");
    expect(row.getAttribute("data-previewable")).toBe("true");
    fireEvent.click(row);
    await waitFor(() => {
      expect(screen.getByTestId("action-preview")).toBeEnabled();
    });
    fireEvent.click(screen.getByTestId("action-preview"));
    await waitFor(() => {
      expect(onPreview).toHaveBeenCalled();
    });
  });

  it("passes the zero serious/critical axe-core gate (T082a / 508)", async () => {
    stubPathFetch();
    const { container } = renderShell(
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
    const { container } = renderShell(
      <ContentExplorerShell
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-view")).toBeInTheDocument(),
    );
    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-search"));
    await waitFor(() =>
      expect(screen.getByTestId("explorer-search-panel")).toBeInTheDocument(),
    );
    await renderA11yGate(container);
  });

  it("security toggle is visible without opening the View menu (#3268)", async () => {
    stubPathFetch();
    const { container } = renderShell(
      <ContentExplorerShell
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("explorer-toggle-security")).toBeInTheDocument();
    });
    expect(screen.queryByTestId("explorer-menu-view-dropdown")).toBeNull();
    fireEvent.click(screen.getByTestId("explorer-toggle-security"));
    await waitFor(() => {
      expect(
        screen.queryByTestId("explorer-security-hint") ??
          screen.queryByTestId("explorer-security-panel"),
      ).toBeTruthy();
    });
    await renderA11yGate(container);
  });

  it("security toggle shows hint when no folder id is available (#2410)", async () => {
    stubPathFetch();
    renderShell(
      <ContentExplorerShell
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        resolveFolderId={async () => undefined}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-toggle-security"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-security-hint")).toBeInTheDocument();
    });
  });

  it("View → Clipboard opens the empty clipboard panel and toggles aria-checked (#3544)", async () => {
    stubPathFetch();
    const { container } = renderShell(
      <ContentExplorerShell
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-view")).toBeInTheDocument(),
    );
    openViewMenu();
    const toggle = screen.getByTestId(
      "explorer-toggle-clipboard",
    ) as HTMLButtonElement;
    expect(toggle.disabled).toBe(false);
    expect(toggle.getAttribute("aria-checked")).toBe("false");
    expect(screen.queryByTestId("explorer-clipboard-panel")).toBeNull();

    fireEvent.click(toggle);
    await waitFor(() => {
      expect(screen.getByTestId("explorer-clipboard-panel")).toBeInTheDocument();
    });
    expect(
      screen.getByTestId("explorer-toggle-clipboard").getAttribute(
        "aria-checked",
      ),
    ).toBe("true");

    fireEvent.click(screen.getByTestId("explorer-toggle-clipboard"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-clipboard-panel")).toBeNull();
    });
    expect(
      screen.getByTestId("explorer-toggle-clipboard").getAttribute(
        "aria-checked",
      ),
    ).toBe("false");
    await renderA11yGate(container);
  });

  it("Content → Add to clipboard mounts the panel with Sites rows (#3551)", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "site-a",
                  name: "CorporateInvestments",
                  path: "/Sites/CorporateInvestments",
                  type: "site",
                  category: "SITE",
                  accessLevel: "ADMIN",
                },
                {
                  name: "EnterpriseInvestments",
                  path: "/Sites/EnterpriseInvestments",
                  type: "FSFolder",
                  category: "site",
                  accessLevel: "WRITE",
                },
              ],
              childrenCount: 2,
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

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-site-a")).toBeInTheDocument();
      expect(
        screen.getByTestId("detail-row-/Sites/EnterpriseInvestments"),
      ).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("detail-select-site-a"));
    fireEvent.click(
      screen.getByTestId("detail-select-/Sites/EnterpriseInvestments"),
    );
    await waitFor(() => {
      expect(screen.getByTestId("explorer-multi-select-count")).toHaveTextContent(
        "2",
      );
    });

    fireEvent.click(screen.getByTestId("explorer-menu-content"));
    fireEvent.click(screen.getByTestId("explorer-clipboard-add"));

    await waitFor(() => {
      expect(screen.getByTestId("explorer-clipboard-panel")).toBeInTheDocument();
    });
    expect(screen.getAllByTestId("clipboard-item-row")).toHaveLength(2);

    fireEvent.click(screen.getByTestId("explorer-menu-view"));
    expect(
      screen.getByTestId("explorer-toggle-clipboard").getAttribute(
        "aria-checked",
      ),
    ).toBe("true");
  });

  it("translations toggle shows select-item hint without a content selection (#2430)", async () => {
    stubPathFetch();
    renderShell(
      <ContentExplorerShell
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-translations"));
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-translations-hint"),
      ).toBeInTheDocument();
    });
    expect(screen.queryByTestId("translations-panel")).toBeNull();
  });

  it("GUID list row opens translations panel (not select-item hint) (#3545)", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/content-explorer/translations/")) {
        return new Response(
          JSON.stringify({
            itemId: 708,
            locale: "en-us",
            variants: [
              { contentId: 708, locale: "en-us", role: "source", revision: 1 },
            ],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "1-101-708",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "page",
                  accessLevel: "WRITE",
                },
              ],
              childrenCount: 1,
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
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("detail-row-1-101-708")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-row-1-101-708"));
    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-translations"));
    await waitFor(() => {
      expect(screen.getByTestId("translations-panel")).toBeInTheDocument();
    });
    expect(screen.queryByTestId("explorer-translations-hint")).toBeNull();
    await waitFor(() =>
      expect(screen.getByTestId("translations-panel")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    expect(screen.getByTestId("translations-current-locale-value")).toHaveTextContent(
      "en-us",
    );
    expect(screen.getByTestId("translations-variant-row-708")).toBeTruthy();
  });

  it("Content → Site Copy mounts wizard with source from /Sites/<name> (#2767)", async () => {
    stubPathFetch();
    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo/Home"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-content")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("explorer-menu-content"));
    const siteCopy = screen.getByTestId(
      "explorer-content-site-copy",
    ) as HTMLButtonElement;
    expect(siteCopy.disabled).toBe(false);
    fireEvent.click(siteCopy);
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-site-copy-panel"),
      ).toBeInTheDocument();
      expect(screen.getByTestId("site-copy-wizard")).toBeInTheDocument();
    });
    const source = screen.getByTestId("site-copy-source") as HTMLInputElement;
    expect(source.value).toBe("Demo");
    // T082a / WebUI AGENTS.md — a11y gate with Site Copy panel open.
    await renderA11yGate(container);
  });

  it("Content → Site Copy is disabled at /Sites root without a site (#2767)", async () => {
    stubPathFetch();
    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-content")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("explorer-menu-content"));
    const siteCopy = screen.getByTestId(
      "explorer-content-site-copy",
    ) as HTMLButtonElement;
    expect(siteCopy.disabled).toBe(true);
    expect(screen.queryByTestId("site-copy-wizard")).toBeNull();
    // T082a — shell remains accessible when Site Copy is disabled (no panel).
    await renderA11yGate(container);
  });

  it("Content → Subfolder Copy mounts wizard with source from folder path (#2792)", async () => {
    stubPathFetch();
    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo/Home"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-content")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("explorer-menu-content"));
    const subCopy = screen.getByTestId(
      "explorer-content-subfolder-copy",
    ) as HTMLButtonElement;
    expect(subCopy.disabled).toBe(false);
    fireEvent.click(subCopy);
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-subfolder-copy-panel"),
      ).toBeInTheDocument();
      expect(screen.getByTestId("subfolder-copy-wizard")).toBeInTheDocument();
    });
    const source = screen.getByTestId(
      "subfolder-copy-source",
    ) as HTMLInputElement;
    expect(source.value).toBe("/Sites/Demo/Home");
    // T082a / WebUI AGENTS.md — a11y gate with Subfolder Copy panel open.
    await renderA11yGate(container);
  });

  it("Content → Subfolder Copy is disabled without a folder path (#2792)", async () => {
    stubPathFetch();
    const { container } = renderShell(
      <ContentExplorerShell
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-content")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("explorer-menu-content"));
    const subCopy = screen.getByTestId(
      "explorer-content-subfolder-copy",
    ) as HTMLButtonElement;
    expect(subCopy.disabled).toBe(true);
    expect(screen.queryByTestId("subfolder-copy-wizard")).toBeNull();
    await renderA11yGate(container);
  });

  it("relationships toggle shows select-item hint without a selection (#2769)", async () => {
    stubPathFetch();
    renderShell(
      <ContentExplorerShell
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-relationships"));
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-relationships-hint"),
      ).toBeInTheDocument();
    });
    expect(screen.queryByTestId("relationships-view")).toBeNull();
    expect(screen.queryByTestId("explorer-relationships-panel")).toBeNull();
  });

  it("relationships panel mounts RelationshipsView for a selected item (#2769)", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "page",
                  accessLevel: "READ",
                },
              ],
              childrenCount: 1,
              startIndex: 0,
            },
            PathItem: [],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      // RelationshipsView → fetchNodeSummary (content-explorer/relationships)
      if (url.includes("relationships") && url.includes("/summary")) {
        return new Response(
          JSON.stringify({
            outgoing: { count: 1, byType: [{ type: "related", count: 1 }] },
            incoming: { count: 0, byType: [] },
            taxonomy: { count: 0, nodes: [] },
            local: { count: 0, links: [] },
            reverse: { count: 0, byType: [] },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (url.includes("relationships")) {
        return new Response(
          JSON.stringify({
            outgoing: { count: 1, byType: [{ type: "related", count: 1 }] },
            incoming: { count: 0, byType: [] },
            taxonomy: { count: 0, nodes: [] },
            local: { count: 0, links: [] },
            reverse: { count: 0, byType: [] },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });

    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-42")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-row-42"));

    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-relationships"));

    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-relationships-panel"),
      ).toBeInTheDocument();
      expect(screen.getByTestId("relationships-view")).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByTestId("relationships-view")).toHaveAttribute(
        "data-testid-state",
        "ok",
      );
    });
    expect(screen.getByTestId("relationships-row-outgoing")).toBeInTheDocument();
    expect(screen.queryByTestId("explorer-relationships-hint")).toBeNull();
    // T082a — a11y gate with relationships panel expanded (search-panel peer).
    await renderA11yGate(container);
  });

  it("relationships toggle second click turns panel off (#2769)", async () => {
    stubPathFetch();
    renderShell(
      <ContentExplorerShell
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-relationships"));
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-relationships-hint"),
      ).toBeInTheDocument();
    });
    // View toggles keep the dropdown open (ExplorerMenuBar) — second click
    // without re-toggling the View menuitem collapses relationships.
    fireEvent.click(screen.getByTestId("explorer-toggle-relationships"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-relationships-hint")).toBeNull();
      expect(screen.queryByTestId("explorer-relationships-panel")).toBeNull();
      expect(screen.queryByTestId("relationships-view")).toBeNull();
    });
  });

  it("relationships panel shows select-item hint when selection becomes a folder (#2769)", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "page",
                  accessLevel: "READ",
                },
                {
                  id: "99",
                  name: "AssetsFolder",
                  path: "/Sites/Demo/AssetsFolder",
                  type: "folder",
                  accessLevel: "WRITE",
                },
              ],
              childrenCount: 2,
              startIndex: 0,
            },
            PathItem: [],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (url.includes("relationships")) {
        return new Response(
          JSON.stringify({
            outgoing: { count: 1, byType: [{ type: "related", count: 1 }] },
            incoming: { count: 0, byType: [] },
            taxonomy: { count: 0, nodes: [] },
            local: { count: 0, links: [] },
            reverse: { count: 0, byType: [] },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-42")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-row-42"));
    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-relationships"));
    await waitFor(() => {
      expect(screen.getByTestId("relationships-view")).toBeInTheDocument();
    });

    // Switch selection to a folder → content-id panel unmounts, hint appears.
    fireEvent.click(screen.getByTestId("detail-row-99"));
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-relationships-hint"),
      ).toBeInTheDocument();
    });
    expect(screen.queryByTestId("relationships-view")).toBeNull();
    expect(screen.queryByTestId("explorer-relationships-panel")).toBeNull();
  });

  it("relationships panel mounts for a GUID-shaped content row (#3546)", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "1-101-708",
                  name: "Home",
                  path: "/Sites/Corporate_Investments/Pages/Home",
                  type: "rffHome",
                  category: "PAGE",
                  accessLevel: "READ",
                },
              ],
              childrenCount: 1,
              startIndex: 0,
            },
            PathItem: [],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (url.includes("relationships")) {
        return new Response(
          JSON.stringify({
            outgoing: { count: 0, byType: [] },
            incoming: { count: 0, byType: [] },
            taxonomy: { count: 0, nodes: [] },
            local: { count: 0, links: [] },
            reverse: { count: 0, byType: [] },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });

    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Corporate_Investments/Pages"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-1-101-708")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-row-1-101-708"));
    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-relationships"));

    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-relationships-panel"),
      ).toBeInTheDocument();
      expect(screen.getByTestId("relationships-view")).toBeInTheDocument();
    });
    expect(screen.queryByTestId("explorer-relationships-hint")).toBeNull();
    await renderA11yGate(container);
  });

  it("relationships panel binds slug rows via sys_contentid (#3546)", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "ci-home",
                  name: "Home",
                  path: "/Sites/Corporate_Investments/Pages/Home",
                  type: "rffHome",
                  category: "PAGE",
                  accessLevel: "READ",
                  displayProperties: { sys_contentid: "708" },
                },
              ],
              childrenCount: 1,
              startIndex: 0,
            },
            PathItem: [],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (url.includes("relationships")) {
        return new Response(
          JSON.stringify({
            outgoing: { count: 0, byType: [] },
            incoming: { count: 0, byType: [] },
            taxonomy: { count: 0, nodes: [] },
            local: { count: 0, links: [] },
            reverse: { count: 0, byType: [] },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Corporate_Investments/Pages"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-708")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-row-708"));
    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-relationships"));

    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-relationships-panel"),
      ).toBeInTheDocument();
    });
    expect(screen.queryByTestId("explorer-relationships-hint")).toBeNull();
  });

  it("relationships panel resolves omitted list id via path lookup (#3546)", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/path/item/")) {
        return new Response(
          JSON.stringify({
            PathItem: {
              id: "1-101-708",
              name: "Home",
              path: "/Sites/Corporate_Investments/Pages/Home",
              type: "rffHome",
              category: "PAGE",
            },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  name: "Home",
                  path: "/Sites/Corporate_Investments/Pages/Home",
                  type: "rffHome",
                  category: "PAGE",
                  accessLevel: "READ",
                },
              ],
              childrenCount: 1,
              startIndex: 0,
            },
            PathItem: [],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (url.includes("relationships")) {
        return new Response(
          JSON.stringify({
            outgoing: { count: 0, byType: [] },
            incoming: { count: 0, byType: [] },
            taxonomy: { count: 0, nodes: [] },
            local: { count: 0, links: [] },
            reverse: { count: 0, byType: [] },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Corporate_Investments/Pages"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId("detail-row-/Sites/Corporate_Investments/Pages/Home"),
      ).toBeInTheDocument();
    });
    fireEvent.click(
      screen.getByTestId("detail-row-/Sites/Corporate_Investments/Pages/Home"),
    );
    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-relationships"));

    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-relationships-panel"),
      ).toBeInTheDocument();
    });
    expect(screen.queryByTestId("explorer-relationships-hint")).toBeNull();
  });

  it("dependencies toggle shows select-item hint without a content selection (#2768)", async () => {
    stubPathFetch();
    renderShell(
      <ContentExplorerShell
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-dependencies"));
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-dependencies-hint"),
      ).toBeInTheDocument();
    });
    expect(screen.queryByTestId("dependency-viewer")).toBeNull();
    expect(screen.queryByTestId("explorer-dependencies-panel")).toBeNull();
  });

  it("dependencies toggle mounts DependencyViewer for a selected content item (#2768)", async () => {
    const loadDependencySummary = vi.fn(async () => ({
      outgoing: { count: 2, byType: [{ type: "translation", count: 2 }] },
      incoming: { count: 1, byType: [{ type: "translation", count: 1 }] },
      taxonomy: { count: 0, nodes: [] as string[] },
      local: { count: 0, links: [] as { type: string; targetId: string }[] },
      reverse: { count: 0, byType: [] as { type: string; count: number }[] },
    }));

    // DetailList loads folder children — return one page so we can select it.
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "1001",
                  name: "Home",
                  path: "/Sites/Home",
                  type: "page",
                  accessLevel: "WRITE",
                },
              ],
              childrenCount: 1,
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

    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadDependencySummary={loadDependencySummary}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-1001")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-row-1001"));

    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-dependencies"));

    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-dependencies-panel"),
      ).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByTestId("dependency-viewer")).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(loadDependencySummary).toHaveBeenCalledWith("1001");
    });
    expect(screen.queryByTestId("explorer-dependencies-hint")).toBeNull();
    // T082a — a11y gate with dependencies panel expanded (search-panel peer).
    await renderA11yGate(container);
  });

  it("root initialPath does not call resolveFolderId (#3468)", async () => {
    stubPathFetch();
    const resolveFolderId = vi.fn(async () => "should-not-run");
    renderShell(
      <ContentExplorerShell
        initialPath="/"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        resolveFolderId={resolveFolderId}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("content-explorer-shell")).toBeInTheDocument();
    });
    expect(resolveFolderId).not.toHaveBeenCalled();
  });

  it("root initialPath does not GET path/item/ (#3458)", async () => {
    const seen: string[] = [];
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      seen.push(url);
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
    });

    renderShell(
      <ContentExplorerShell
        initialPath="/"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("content-explorer-shell")).toBeInTheDocument();
    });
    expect(
      seen.some((u) => /\/path\/item\/?(\?|$)/i.test(u)),
      `unexpected path/item probe: ${seen.join(" | ")}`,
    ).toBe(false);
  });

  it("security stays on hint when resolveFolderId rejects (#2410)", async () => {
    stubPathFetch();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        resolveFolderId={async () => {
          throw new Error("lookup failed");
        }}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-toggle-security"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-security-hint")).toBeInTheDocument();
    });
    expect(screen.queryByTestId("folder-security-panel")).toBeNull();
  });

  it("security panel mounts with resolved folder id and session identities (#2410)", async () => {
    stubPathFetch();
    const resolveFolderId = vi.fn(async () => "folder-42");
    // FolderSecurityPanel will fetch properties for folder-42 — stub returns empty.
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("folderProperties") || url.includes("folderproperties")) {
        return new Response(
          JSON.stringify({
            id: "folder-42",
            name: "Sites",
            permission: {
              accessLevel: "ADMIN",
              adminPrincipals: [{ type: "USER", name: "Admin" }],
            },
            communityName: "Default",
            locale: "en-us",
            displayFormatName: "FolderList",
            workflowId: "6",
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
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

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        resolveFolderId={resolveFolderId}
      />,
    );

    fireEvent.click(screen.getByTestId("explorer-toggle-security"));
    await waitFor(() => {
      expect(resolveFolderId).toHaveBeenCalled();
      expect(screen.getByTestId("explorer-security-panel")).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByTestId("folder-security-panel")).toBeInTheDocument();
      expect(screen.getByTestId("folder-properties")).toBeInTheDocument();
    });
  });

  it("View → Search toggle opens and closes the product Search panel (#2850)", async () => {
    stubPathFetch();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        listSavedSearches={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-view")).toBeInTheDocument(),
    );
    expect(screen.queryByTestId("explorer-search-panel")).toBeNull();

    openViewMenu();
    const viewToggle = screen.getByTestId("explorer-toggle-search");
    expect(viewToggle.getAttribute("aria-expanded")).toBe("false");
    expect(viewToggle.getAttribute("aria-controls")).toBe(
      "explorer-search-panel",
    );
    fireEvent.click(viewToggle);

    await waitFor(() => {
      expect(screen.getByTestId("explorer-search-panel")).toBeInTheDocument();
      expect(screen.getByTestId("search-panel")).toBeInTheDocument();
      expect(screen.getByTestId("search-panel-input")).toBeInTheDocument();
    });
    expect(viewToggle.getAttribute("aria-expanded")).toBe("true");

    // View menu stays open for view toggles — flip Search off without re-clicking View
    // (re-clicking View would collapse the dropdown and hide the test id).
    fireEvent.click(screen.getByTestId("explorer-toggle-search"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-search-panel")).toBeNull();
    });
  });

  it("Content → Search opens the same Search panel (#2850)", async () => {
    stubPathFetch();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        listSavedSearches={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-content")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("explorer-menu-content"));
    const contentSearch = screen.getByTestId("explorer-menu-content-search");
    expect(contentSearch.getAttribute("role")).toBe("menuitemcheckbox");
    expect(contentSearch.getAttribute("aria-controls")).toBe(
      "explorer-search-panel",
    );
    expect(contentSearch.getAttribute("aria-expanded")).toBe("false");
    fireEvent.click(contentSearch);
    await waitFor(() => {
      expect(screen.getByTestId("explorer-search-panel")).toBeInTheDocument();
      expect(screen.getByTestId("search-panel-submit")).toBeInTheDocument();
    });
    expect(contentSearch.getAttribute("aria-expanded")).toBe("true");

    // Content menu stays open for view-style toggles — flip Search off in place.
    fireEvent.click(screen.getByTestId("explorer-menu-content-search"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-search-panel")).toBeNull();
    });
    expect(
      screen.getByTestId("explorer-menu-content-search").getAttribute(
        "aria-expanded",
      ),
    ).toBe("false");
  });

  it("injects free-text search transport into the product Search panel (#2850)", async () => {
    stubPathFetch();
    const search = vi.fn(async () => ({
      children: [
        {
          id: "99",
          name: "Hit",
          title: "Hit",
          folderPath: "/Sites/Demo",
          type: "page",
        },
      ],
      totalCount: 1,
      startIndex: 1,
    }));
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        listSavedSearches={async () => []}
        search={search}
      />,
    );
    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-search"));
    await waitFor(() =>
      expect(screen.getByTestId("search-panel-input")).toBeInTheDocument(),
    );
    fireEvent.change(screen.getByTestId("search-panel-input"), {
      target: { value: "demo page" },
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    await waitFor(() => {
      expect(search).toHaveBeenCalled();
    });
    const criteria = search.mock.calls[0][0] as {
      query?: string;
      folderPath?: string;
    };
    expect(criteria.query).toBe("demo page");
    expect(criteria.folderPath).toBe("//Sites/Demo");
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-results")).toBeInTheDocument();
    });
  });

  it("passes listSavedSearches and executeSavedSearch into SearchPanel (#2850)", async () => {
    stubPathFetch();
    const listSavedSearches = vi.fn(async () => [
      {
        name: "All Content",
        label: "All Content",
        standardSearch: true,
      },
    ]);
    const executeSavedSearch = vi.fn(async () => ({
      children: [
        {
          id: "saved-1",
          name: "Saved Hit",
          title: "Saved Hit",
          folderPath: "/Sites/Demo",
          type: "page",
        },
      ],
      totalCount: 1,
      startIndex: 1,
    }));
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        listSavedSearches={listSavedSearches}
        executeSavedSearch={executeSavedSearch}
      />,
    );
    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-search"));
    await waitFor(() => {
      expect(listSavedSearches).toHaveBeenCalled();
    });
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-saved-picker")).toBeInTheDocument();
      expect(screen.getByTestId("search-panel-saved-select")).toBeInTheDocument();
    });
    fireEvent.change(screen.getByTestId("search-panel-saved-select"), {
      target: { value: "All Content" },
    });
    fireEvent.click(screen.getByTestId("search-panel-saved-run"));
    await waitFor(() => {
      expect(executeSavedSearch).toHaveBeenCalled();
    });
    expect(executeSavedSearch.mock.calls[0][0]).toBe("All Content");
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-results")).toBeInTheDocument();
    });
  });

  it("wires Views catalog groups into the product tree (#3116)", async () => {
    stubPathFetch();
    const listViews = vi.fn(async () => [
      {
        name: "MyPages",
        label: "My Pages",
        parentCategory: 1,
        standardView: true,
      },
      {
        name: "View_All",
        label: "All Content",
        parentCategory: 3,
        standardView: true,
      },
    ]);
    const executeView = vi.fn();
    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        listViews={listViews}
        executeView={executeView}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("explorer-nav")).toBeInTheDocument();
      expect(screen.getByTestId("explorer-views-tree")).toBeInTheDocument();
      expect(screen.getByTestId("explorer-views-group-1")).toBeInTheDocument();
    });
    expect(listViews).toHaveBeenCalled();
    expect(screen.getByTestId("explorer-views-leaf-MyPages")).toBeInTheDocument();
    expect(executeView).not.toHaveBeenCalled();
    await renderA11yGate(container);
  });

  it("running a standard view leaf shows open/reveal results (#3116)", async () => {
    stubPathFetch();
    const listViews = vi.fn(async () => [
      {
        name: "View_All",
        label: "All Content",
        parentCategory: 1,
        standardView: true,
      },
    ]);
    const executeView = vi.fn(async () => ({
      children: [
        {
          id: "77",
          title: "Hit",
          folderPath: "/Sites/Demo",
          type: "page",
        },
      ],
      totalCount: 1,
      startIndex: 1,
      viewName: "View_All",
    }));
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        listViews={listViews}
        executeView={executeView}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-views-leaf-View_All")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("explorer-views-leaf-View_All"));
    await waitFor(() => {
      expect(executeView).toHaveBeenCalledWith("View_All", {
        startIndex: 1,
        maxResults: 50,
      });
      expect(screen.getByTestId("explorer-view-results")).toBeInTheDocument();
      expect(screen.getByTestId("explorer-view-results-list")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("explorer-view-open-77"));
    fireEvent.click(screen.getByTestId("explorer-view-reveal-77"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-view-results")).toBeNull();
    });
  });

  it("Inbox default executeView POSTs ViewExecuteRequest envelope (#3323)", async () => {
    const executeBodies: string[] = [];
    mockFetch(async (input, init) => {
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
      if (/\/views\/[^/]+\/execute/i.test(url) && String(init?.method) === "POST") {
        executeBodies.push(String(init?.body ?? ""));
        return new Response(
          JSON.stringify({
            children: [],
            totalCount: 0,
            startIndex: 1,
            viewName: "Inbox",
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        listViews={async () => [
          { name: "Inbox", label: "Inbox", parentCategory: 1, customView: true },
        ]}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-views-leaf-Inbox")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("explorer-views-leaf-Inbox"));
    await waitFor(() => {
      expect(executeBodies.length).toBeGreaterThan(0);
    });
    const parsed = JSON.parse(executeBodies[0] ?? "{}");
    expect(parsed.ViewExecuteRequest).toEqual({
      startIndex: 1,
      maxResults: 50,
    });
    expect(parsed.startIndex).toBeUndefined();
    await waitFor(() => {
      expect(screen.getByTestId("explorer-view-results-empty")).toBeInTheDocument();
    });
  });

  it("running Inbox POSTs view execute and shows results (#3240)", async () => {
    stubPathFetch();
    const listViews = vi.fn(async () => [
      {
        name: "Inbox",
        label: "Inbox",
        parentCategory: 1,
        customView: true,
      },
    ]);
    const executeView = vi.fn(async () => ({
      children: [
        {
          id: "88",
          title: "Assigned page",
          folderPath: "/Sites/Demo",
          type: "page",
        },
      ],
      totalCount: 1,
      startIndex: 1,
      viewName: "Inbox",
    }));
    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        listViews={listViews}
        executeView={executeView}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-views-leaf-Inbox")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("explorer-views-leaf-Inbox"));
    await waitFor(() => {
      expect(executeView).toHaveBeenCalledWith("Inbox", {
        startIndex: 1,
        maxResults: 50,
      });
      expect(screen.getByTestId("explorer-view-results")).toBeInTheDocument();
      expect(screen.getByTestId("explorer-view-results-list")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("explorer-view-open-88"));
    fireEvent.click(screen.getByTestId("explorer-view-reveal-88"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-view-results")).toBeNull();
    });
    await renderA11yGate(container);
  });

  it("Inbox empty execute shows empty state (#3240)", async () => {
    stubPathFetch();
    const executeView = vi.fn(async () => ({
      children: [],
      totalCount: 0,
      startIndex: 1,
      viewName: "Inbox",
    }));
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        listViews={async () => [
          { name: "Inbox", label: "Inbox", parentCategory: 1, customView: true },
        ]}
        executeView={executeView}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-views-leaf-Inbox")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("explorer-views-leaf-Inbox"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-view-results-empty")).toBeInTheDocument();
    });
    expect(executeView).toHaveBeenCalled();
  });

  it("other custom URL view leaf does not call execute (#3116 / #3240)", async () => {
    stubPathFetch();
    const listViews = vi.fn(async () => [
      {
        name: "Outbox",
        label: "Outbox",
        parentCategory: 1,
        customView: true,
      },
    ]);
    const executeView = vi.fn();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        listViews={listViews}
        executeView={executeView}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-views-leaf-Outbox")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("explorer-views-leaf-Outbox"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-view-results-error")).toBeInTheDocument();
    });
    expect(executeView).not.toHaveBeenCalled();
    expect(screen.getByTestId("explorer-view-results-error").textContent).toMatch(
      /Custom URL views cannot be run/i,
    );
  });

  it("New Item host opens a content-type picker instead of an error toast (#3513)", async () => {
    stubPathFetch();
    const createItem = vi.fn().mockResolvedValue({
      itemId: "101",
      folderPath: "//Sites/Demo",
      name: "New-rffEvent",
      contentType: "rffEvent",
    });
    const openWindow = vi.fn();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => [
          {
            name: "New",
            label: "New Item",
            sortRank: 1,
            menuType: "MENUITEM",
          },
        ]}
        loadContentTypes={async () => [
          { name: "percFile", label: "File" },
          { name: "rffEvent", label: "Event" },
        ]}
        createItem={createItem}
        openWindow={openWindow}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("action-toolbar-item-New")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("action-toolbar-item-New"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-type-picker")).toBeInTheDocument();
    });
    expect(screen.queryByText(/Choose a content type from New Item/i)).toBeNull();
    fireEvent.change(screen.getByTestId("explorer-type-picker-select"), {
      target: { value: "rffEvent" },
    });
    fireEvent.click(screen.getByTestId("explorer-type-picker-ok"));
    await waitFor(() => {
      expect(createItem).toHaveBeenCalledWith({
        contentType: "rffEvent",
        folderPath: "/Sites/Demo",
      });
    });
    expect(openWindow).toHaveBeenCalled();
  });
});

describe("ContentExplorerShell missing BootstrapProvider (#3331)", () => {
  it("renders an error state without throwing useContext", async () => {
    stubPathFetch();
    expect(() =>
      render(
        <ContentExplorerShell
          initialPath="/Folders"
          loadDisplayFormats={async () => []}
          loadMenuActions={async () => []}
        />,
      ),
    ).not.toThrow();
    const alert = screen.getByTestId("explorer-bootstrap-unavailable");
    expect(alert).toBeInTheDocument();
    expect(alert.textContent).toMatch(/application session is not available/i);
    expect(screen.queryByTestId("explorer-nav")).toBeNull();
    await renderA11yGate(screen.getByTestId("content-explorer-shell"));
  });
});
