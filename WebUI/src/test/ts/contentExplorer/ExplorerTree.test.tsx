/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import { ExplorerTree } from "../../../main/ts/contentExplorer/ExplorerTree";
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
});
