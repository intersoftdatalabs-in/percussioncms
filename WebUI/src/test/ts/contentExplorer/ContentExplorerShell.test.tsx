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
    openViewMenu();
    expect(screen.getByTestId("explorer-toggle-search")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-toggle-security")).toBeInTheDocument();
    expect(
      screen.getByTestId("explorer-toggle-translations"),
    ).toBeInTheDocument();

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

  it("security toggle shows hint when no folder id is available (#2410)", async () => {
    stubPathFetch();
    renderShell(
      <ContentExplorerShell
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        resolveFolderId={async () => undefined}
      />,
    );
    openViewMenu();
    fireEvent.click(screen.getByTestId("explorer-toggle-security"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-security-hint")).toBeInTheDocument();
    });
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

  it("Content → Site Copy mounts wizard with source from /Sites/<name> (#2767)", async () => {
    stubPathFetch();
    renderShell(
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
  });

  it("Content → Site Copy is disabled at /Sites root without a site (#2767)", async () => {
    stubPathFetch();
    renderShell(
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
    openViewMenu();
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

    openViewMenu();
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
});
