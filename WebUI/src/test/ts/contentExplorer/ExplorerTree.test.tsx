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
import { describe, expect, it } from "vitest";
import {
  ExplorerTree,
  normalizeExplorerTreePathKey,
  parentExplorerTreePathKey,
} from "../../../main/ts/contentExplorer/ExplorerTree";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { mockFetch } from "./setup";
import { renderA11yGate } from "./a11y";

/** Child of /Sites — path is the data-testid suffix (`tree-node-${path}`). */
const ROOT_FOLDER: PSPathItem = {
  id: "root-1",
  path: "/Sites/Foo",
  name: "Foo",
  type: "folder",
  hasFolderChildren: true,
};

const CHILD_FOLDER: PSPathItem = {
  id: "child-1",
  path: "/Sites/Foo/Bar",
  name: "Bar",
  type: "folder",
  hasFolderChildren: false,
};

/** Wire shape from PSPathItemList (pathApi.findChildren unwraps PathItem). */
function pathItemListResponse(items: PSPathItem[]): Response {
  return new Response(JSON.stringify({ PathItem: items }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}

describe("ExplorerTree", () => {
  it("renders an initial empty state when the root fetch returns no children", async () => {
    mockFetch(async () => pathItemListResponse([]));
    render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("explorer-tree").textContent,
      ).toMatch(/No folders available/),
    );
  });

  it("loads children on mount and renders treeitems for each", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.endsWith("/pathmanagement/path/folder/Sites")) {
        return pathItemListResponse([ROOT_FOLDER]);
      }
      return pathItemListResponse([]);
    });
    render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument(),
    );
  });

  it("renders classic Folders root from path/folder/ children (#3044)", async () => {
    const FOLDERS_ROOT: PSPathItem = {
      id: "folders-root",
      path: "/Folders/",
      name: "Folders",
      type: "folder",
      leaf: false,
      hasFolderChildren: true,
    };
    const SITES_ROOT: PSPathItem = {
      id: "sites-root",
      path: "/Sites/",
      name: "Sites",
      type: "folder",
      leaf: false,
      hasFolderChildren: true,
    };
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      // Root children: encodePath("/") → folder/ (empty suffix).
      if (
        url.endsWith("/pathmanagement/path/folder/") ||
        /\/pathmanagement\/path\/folder\/?$/.test(url)
      ) {
        return pathItemListResponse([SITES_ROOT, FOLDERS_ROOT]);
      }
      return pathItemListResponse([]);
    });
    render(
      <ExplorerTree
        initialPath="/"
        selectedPath={null}
        onSelectFolder={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Folders/")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("tree-node-/Sites/")).toBeInTheDocument();
    expect(screen.getByText("Folders")).toBeInTheDocument();
  });

  it("renders site-type children with id paths under /Sites (#3001)", async () => {
    // Wire shape from PSSitePathItemService: type=site, path=/Sites/{id}/
    const SITE_CHILD: PSPathItem = {
      id: "16777215-101-703",
      path: "/Sites/16777215-101-703/",
      name: "Corporate_Investments",
      type: "site",
      leaf: false,
      hasFolderChildren: true,
    };
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.endsWith("/pathmanagement/path/folder/Sites")) {
        return pathItemListResponse([SITE_CHILD]);
      }
      return pathItemListResponse([]);
    });
    render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("tree-node-/Sites/16777215-101-703/"),
      ).toBeInTheDocument(),
    );
    expect(screen.getByText("Corporate_Investments")).toBeInTheDocument();
  });

  it("loads children on first expand (lazy)", async () => {
    let rootCalls = 0;
    let childCalls = 0;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.endsWith("/pathmanagement/path/folder/Sites")) {
        rootCalls++;
        return pathItemListResponse([ROOT_FOLDER]);
      }
      if (url.endsWith("/pathmanagement/path/folder/Sites/Foo")) {
        childCalls++;
        return pathItemListResponse([CHILD_FOLDER]);
      }
      return pathItemListResponse([]);
    });
    render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument(),
    );
    expect(rootCalls).toBe(1);
    expect(childCalls).toBe(0);
    // Expand is on the toggle control (not the row select handler).
    const node = screen.getByTestId("tree-node-/Sites/Foo");
    const toggle = node.querySelector('[aria-hidden="true"]');
    expect(toggle).toBeTruthy();
    fireEvent.click(toggle!);
    await waitFor(() =>
      expect(
        screen.getByTestId("tree-node-/Sites/Foo/Bar"),
      ).toBeInTheDocument(),
    );
    expect(childCalls).toBe(1);
  });

  it("lists site children using folderPath not sitename (#3326)", async () => {
    const SITE_CHILD: PSPathItem = {
      id: "Corporate_Investments",
      path: "/Sites/Corporate_Investments/",
      folderPath: "//Sites/CorporateInvestments",
      name: "Corporate_Investments",
      type: "site",
      leaf: false,
      hasFolderChildren: true,
    };
    const PAGES: PSPathItem = {
      id: "pages-1",
      path: "/Sites/CorporateInvestments/Pages",
      name: "Pages",
      type: "folder",
      hasFolderChildren: false,
    };
    const requested: string[] = [];
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      requested.push(url);
      if (url.endsWith("/pathmanagement/path/folder/Sites")) {
        return pathItemListResponse([SITE_CHILD]);
      }
      if (url.endsWith("/pathmanagement/path/folder/Sites/CorporateInvestments")) {
        return pathItemListResponse([PAGES]);
      }
      return pathItemListResponse([]);
    });
    render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("tree-node-/Sites/Corporate_Investments/"),
      ).toBeInTheDocument(),
    );
    const node = screen.getByTestId("tree-node-/Sites/Corporate_Investments/");
    const toggle = node.querySelector('[aria-hidden="true"]');
    fireEvent.click(toggle!);
    await waitFor(() =>
      expect(
        screen.getByTestId("tree-node-/Sites/CorporateInvestments/Pages"),
      ).toBeInTheDocument(),
    );
    expect(
      requested.some((u) =>
        u.endsWith("/pathmanagement/path/folder/Sites/CorporateInvestments"),
      ),
    ).toBe(true);
    expect(
      requested.some((u) =>
        u.endsWith(
          "/pathmanagement/path/folder/Sites/Corporate_Investments",
        ),
      ),
    ).toBe(false);
  });

  it("fires onSelectFolder when a row is activated", async () => {
    mockFetch(async () => pathItemListResponse([ROOT_FOLDER]));
    let selected: string | null = null;
    render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={(path) => {
          selected = path;
        }}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole("treeitem"));
    expect(selected).toBe("/Sites/Foo");
  });

  it("surfaces fetch errors as an alert", async () => {
    mockFetch(async () => new Response("server down", { status: 500 }));
    render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={() => undefined}
      />,
    );
    // formatApiError (api/client) surfaces Error.message / body text when present.
    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent(/server down/),
    );
  });

  it("passes the zero serious/critical axe-core gate (loaded state with treeitem children)", async () => {
    mockFetch(async () => pathItemListResponse([ROOT_FOLDER]));
    const { container } = render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath="/Sites/Foo"
        onSelectFolder={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeTruthy(),
    );
    await renderA11yGate(container);
  });

  it("normalizeExplorerTreePathKey treats trailing slashes as the same folder", () => {
    expect(normalizeExplorerTreePathKey("/Assets")).toBe("/Assets");
    expect(normalizeExplorerTreePathKey("/Assets/")).toBe("/Assets");
    expect(normalizeExplorerTreePathKey("//Assets")).toBe("/Assets");
    expect(normalizeExplorerTreePathKey("//Assets/")).toBe("/Assets");
    expect(normalizeExplorerTreePathKey("/")).toBe("/");
    expect(normalizeExplorerTreePathKey("")).toBe("/");
    expect(parentExplorerTreePathKey("/Assets/qa3645r_1")).toBe("/Assets");
    expect(parentExplorerTreePathKey("/Assets")).toBe("/");
    expect(parentExplorerTreePathKey("/")).toBe("/");
  });

  it("reloads selected folder children when childrenEpoch changes (#3640 #3645)", async () => {
    const NEW_FOLDER: PSPathItem = {
      id: "new-1",
      path: "/Sites/New",
      name: "New",
      type: "folder",
      hasFolderChildren: false,
    };
    const RENAMED_FOLDER: PSPathItem = {
      id: "ren-1",
      path: "/Sites/Renamed",
      name: "Renamed",
      type: "folder",
      hasFolderChildren: false,
    };
    let sitesCalls = 0;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.endsWith("/pathmanagement/path/folder/Sites")) {
        sitesCalls += 1;
        if (sitesCalls > 1) {
          return pathItemListResponse([
            ROOT_FOLDER,
            NEW_FOLDER,
            RENAMED_FOLDER,
          ]);
        }
        return pathItemListResponse([ROOT_FOLDER]);
      }
      return pathItemListResponse([]);
    });
    const { rerender, container } = render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath="/Sites"
        onSelectFolder={() => undefined}
        childrenEpoch={0}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument(),
    );
    expect(screen.queryByTestId("tree-node-/Sites/New")).toBeNull();
    expect(screen.queryByTestId("tree-node-/Sites/Renamed")).toBeNull();
    rerender(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath="/Sites/"
        onSelectFolder={() => undefined}
        childrenEpoch={1}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/New")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("tree-node-/Sites/Renamed")).toBeInTheDocument();
    expect(sitesCalls).toBeGreaterThan(1);
    const callsAfterEpoch = sitesCalls;
    rerender(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath="/Sites/Foo"
        onSelectFolder={() => undefined}
        childrenEpoch={1}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/New")).toBeInTheDocument(),
    );
    expect(sitesCalls).toBe(callsAfterEpoch);
    await renderA11yGate(container);
  });

  it("does not force-reload expanded sibling folders on childrenEpoch (#3645)", async () => {
    const OTHER: PSPathItem = {
      id: "other-1",
      path: "/Sites/Other",
      name: "Other",
      type: "folder",
      hasFolderChildren: true,
    };
    let sitesCalls = 0;
    let fooCalls = 0;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.endsWith("/pathmanagement/path/folder/Sites")) {
        sitesCalls += 1;
        return pathItemListResponse([ROOT_FOLDER, OTHER]);
      }
      if (url.endsWith("/pathmanagement/path/folder/Sites/Foo")) {
        fooCalls += 1;
        return pathItemListResponse([CHILD_FOLDER]);
      }
      return pathItemListResponse([]);
    });
    const { rerender, container } = render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath="/Sites"
        onSelectFolder={() => undefined}
        childrenEpoch={0}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument(),
    );
    const node = screen.getByTestId("tree-node-/Sites/Foo");
    const toggle = node.querySelector('[aria-hidden="true"]');
    expect(toggle).toBeTruthy();
    fireEvent.click(toggle!);
    await waitFor(() =>
      expect(
        screen.getByTestId("tree-node-/Sites/Foo/Bar"),
      ).toBeInTheDocument(),
    );
    expect(fooCalls).toBe(1);
    const sitesAfterExpand = sitesCalls;
    rerender(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath="/Sites"
        onSelectFolder={() => undefined}
        childrenEpoch={1}
      />,
    );
    await waitFor(() => expect(sitesCalls).toBeGreaterThan(sitesAfterExpand));
    expect(fooCalls).toBe(1);
    await renderA11yGate(container);
  });

  it("epoch bump reloads parent children when selectedPath is the list path not the tree key (#3652)", async () => {
    const ASSETS: PSPathItem = {
      id: "assets-1",
      path: "/Assets",
      folderPath: "//Folders/$System$/Assets",
      name: "Assets",
      type: "folder",
      hasFolderChildren: true,
    };
    const OLD_FOLDER: PSPathItem = {
      id: "ren-1",
      path: "/Assets/Old",
      name: "Old",
      type: "folder",
      hasFolderChildren: false,
    };
    const RENAMED_FOLDER: PSPathItem = {
      id: "ren-1",
      path: "/Assets/Renamed",
      name: "Renamed",
      type: "folder",
      hasFolderChildren: false,
    };
    let assetsCalls = 0;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.endsWith("/pathmanagement/path/folder/") || url.endsWith("/pathmanagement/path/folder")) {
        return pathItemListResponse([ASSETS]);
      }
      if (
        url.endsWith("/pathmanagement/path/folder/Assets") ||
        url.includes("/pathmanagement/path/folder/Folders/%24System%24/Assets")
      ) {
        assetsCalls += 1;
        if (assetsCalls > 1) {
          return pathItemListResponse([RENAMED_FOLDER]);
        }
        return pathItemListResponse([OLD_FOLDER]);
      }
      return pathItemListResponse([]);
    });
    const { rerender, container } = render(
      <ExplorerTree
        initialPath="/"
        selectedPath="/Folders/$System$/Assets"
        onSelectFolder={() => undefined}
        childrenEpoch={0}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Assets")).toBeInTheDocument(),
    );
    const toggle = screen
      .getByTestId("tree-node-/Assets")
      .querySelector('[aria-hidden="true"]');
    expect(toggle).toBeTruthy();
    fireEvent.click(toggle!);
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Assets/Old")).toBeInTheDocument(),
    );
    rerender(
      <ExplorerTree
        initialPath="/"
        selectedPath="/Folders/$System$/Assets"
        onSelectFolder={() => undefined}
        childrenEpoch={1}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Assets/Renamed")).toBeInTheDocument(),
    );
    expect(screen.getByText("Renamed")).toBeInTheDocument();
    expect(screen.queryByTestId("tree-node-/Assets/Old")).toBeNull();
    expect(assetsCalls).toBeGreaterThan(1);
    await renderA11yGate(container);
  });

  it("epoch bump reloads parent children when selectedPath is the renamed child's old path (#3652)", async () => {
    const OLD_FOLDER: PSPathItem = {
      id: "ren-2",
      path: "/Sites/OldChild",
      name: "OldChild",
      type: "folder",
      hasFolderChildren: false,
    };
    const RENAMED_FOLDER: PSPathItem = {
      id: "ren-2",
      path: "/Sites/NewChild",
      name: "NewChild",
      type: "folder",
      hasFolderChildren: false,
    };
    let sitesCalls = 0;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.endsWith("/pathmanagement/path/folder/Sites")) {
        sitesCalls += 1;
        if (sitesCalls > 1) {
          return pathItemListResponse([ROOT_FOLDER, RENAMED_FOLDER]);
        }
        return pathItemListResponse([ROOT_FOLDER, OLD_FOLDER]);
      }
      return pathItemListResponse([]);
    });
    const { rerender, container } = render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath="/Sites/OldChild"
        onSelectFolder={() => undefined}
        childrenEpoch={0}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/OldChild")).toBeInTheDocument(),
    );
    rerender(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath="/Sites/OldChild"
        onSelectFolder={() => undefined}
        childrenEpoch={1}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/NewChild")).toBeInTheDocument(),
    );
    expect(screen.getByText("NewChild")).toBeInTheDocument();
    expect(screen.queryByTestId("tree-node-/Sites/OldChild")).toBeNull();
    await renderA11yGate(container);
  });

  it("epoch bump drops a deleted child from parent children (#3653)", async () => {
    const DOOMED: PSPathItem = {
      id: "del-1",
      path: "/Sites/Doomed",
      name: "Doomed",
      type: "folder",
      hasFolderChildren: false,
    };
    let sitesCalls = 0;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.endsWith("/pathmanagement/path/folder/Sites")) {
        sitesCalls += 1;
        if (sitesCalls > 1) {
          return pathItemListResponse([ROOT_FOLDER]);
        }
        return pathItemListResponse([ROOT_FOLDER, DOOMED]);
      }
      return pathItemListResponse([]);
    });
    const { rerender, container } = render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath="/Sites/Doomed"
        onSelectFolder={() => undefined}
        childrenEpoch={0}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/Doomed")).toBeInTheDocument(),
    );
    rerender(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath="/Sites/Doomed"
        onSelectFolder={() => undefined}
        childrenEpoch={1}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("tree-node-/Sites/Doomed")).toBeNull(),
    );
    expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument();
    await renderA11yGate(container);
  });
});
