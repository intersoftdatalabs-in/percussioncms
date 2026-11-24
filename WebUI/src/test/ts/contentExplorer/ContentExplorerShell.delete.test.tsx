/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
import { describe, expect, it } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";
import { ContentExplorerShell } from "../../../main/ts/contentExplorer/ContentExplorerShell";
import { renderA11yGate } from "./a11y";
import { mockFetch } from "./setup";

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
const DELETE_FOLDER_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7207, same load-contention rationale as
 * the #7147 slice). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const DELETE_FOLDER_TEST_TIMEOUT = 30_000;

const adminBootstrap: SpaBootstrap = {
  userName: "Admin",
  locale: "en-us",
  entry: "explorer",
  isAdmin: true,
  isDesigner: false,
  isWidgetBuilderActive: false,
  allowExternalAvatarFetch: true,
};

function renderShell(ui: ReactElement) {
  return render(
    <BootstrapProvider value={adminBootstrap}>{ui}</BootstrapProvider>,
  );
}

const DISPOSABLE = {
  id: "n-3646",
  path: "/Sites/qa3646",
  name: "qa3646",
  type: "folder",
  accessLevel: "WRITE",
};

describe("ContentExplorerShell delete folder (#3646)", () => {
  it("POSTs deleteFolder then refreshes list and tree", { timeout: DELETE_FOLDER_TEST_TIMEOUT }, async () => {
    let deleted = false;
    const deleteUrls: string[] = [];
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/pathmanagement/path/deleteFolder")) {
        deleted = true;
        deleteUrls.push(url);
        return new Response("0", {
          status: 200,
          headers: { "Content-Type": "text/plain" },
        });
      }
      if (url.includes("/pathmanagement/path/delete/") && !url.includes("deleteFolder")) {
        return new Response("missing", { status: 404 });
      }
      const kids = deleted ? [] : [DISPOSABLE];
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: kids,
              childrenCount: kids.length,
              startIndex: 0,
            },
            PathItem: kids,
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
        loadWorkflowMenuActions={async () => null}
        actionHandlers={{
          confirm: () => true,
        }}
      />,
    );

    const row = await screen.findByTestId("detail-row-n-3646", undefined, { timeout: DELETE_FOLDER_ROUND_TRIP_TIMEOUT });
    fireEvent.click(row);

    const deleteBtn = await screen.findByTestId("action-delete", undefined, { timeout: DELETE_FOLDER_ROUND_TRIP_TIMEOUT });
    expect(deleteBtn).toBeEnabled();
    fireEvent.click(deleteBtn);

    await waitFor(() => {
      expect(deleteUrls.length).toBeGreaterThan(0);
    }, { timeout: DELETE_FOLDER_ROUND_TRIP_TIMEOUT });
    expect(deleteUrls.every((u) => u.includes("deleteFolder"))).toBe(true);
    await waitFor(() => {
      expect(screen.queryByTestId("detail-row-n-3646")).not.toBeInTheDocument();
    }, { timeout: DELETE_FOLDER_ROUND_TRIP_TIMEOUT });
    await waitFor(() => {
      expect(
        screen.queryByTestId("tree-node-/Sites/qa3646"),
      ).not.toBeInTheDocument();
    }, { timeout: DELETE_FOLDER_ROUND_TRIP_TIMEOUT });
    await renderA11yGate(container);
  });

  it("drops the deleted name from the tree when product initialPath is / (#3653)", { timeout: DELETE_FOLDER_TEST_TIMEOUT }, async () => {
    const SITES = {
      id: "sites-root",
      path: "/Sites",
      name: "Sites",
      type: "folder",
      folderPath: "//Sites",
      hasFolderChildren: true,
    };
    let deleted = false;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/pathmanagement/path/deleteFolder")) {
        deleted = true;
        return new Response("0", {
          status: 200,
          headers: { "Content-Type": "text/plain" },
        });
      }
      if (url.includes("/pathmanagement/path/delete/") && !url.includes("deleteFolder")) {
        return new Response("missing", { status: 404 });
      }
      const pathOnly = url.split("?")[0];
      const isRootList =
        /\/path\/folder\/?$/.test(pathOnly) ||
        /\/paginatedFolder\/?$/.test(pathOnly);
      const isSitesList =
        /\/folder\/Sites\/?$/.test(pathOnly) ||
        /\/paginatedFolder\/Sites/.test(pathOnly);
      const kids = isRootList
        ? [SITES]
        : isSitesList
          ? deleted
            ? []
            : [DISPOSABLE]
          : [];
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: kids,
              childrenCount: kids.length,
              startIndex: 0,
            },
            PathItem: kids,
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
        initialPath="/"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
        actionHandlers={{
          confirm: () => true,
        }}
      />,
    );

    const sitesNode = await screen.findByTestId("tree-node-/Sites", undefined, { timeout: DELETE_FOLDER_ROUND_TRIP_TIMEOUT });
    const toggle = sitesNode.querySelector('[aria-hidden="true"]');
    expect(toggle).toBeTruthy();
    fireEvent.click(toggle!);
    await waitFor(() => {
      expect(screen.getByTestId("tree-node-/Sites/qa3646")).toBeInTheDocument();
    }, { timeout: DELETE_FOLDER_ROUND_TRIP_TIMEOUT });

    const row = await screen.findByTestId("detail-row-n-3646", undefined, { timeout: DELETE_FOLDER_ROUND_TRIP_TIMEOUT });
    fireEvent.click(row);

    const deleteBtn = await screen.findByTestId("action-delete", undefined, { timeout: DELETE_FOLDER_ROUND_TRIP_TIMEOUT });
    expect(deleteBtn).toBeEnabled();
    fireEvent.click(deleteBtn);

    await waitFor(() => {
      expect(screen.queryByTestId("detail-row-n-3646")).not.toBeInTheDocument();
    }, { timeout: DELETE_FOLDER_ROUND_TRIP_TIMEOUT });
    await waitFor(() => {
      expect(
        screen.queryByTestId("tree-node-/Sites/qa3646"),
      ).not.toBeInTheDocument();
    }, { timeout: DELETE_FOLDER_ROUND_TRIP_TIMEOUT });
    expect(screen.queryByText("qa3646")).not.toBeInTheDocument();
    await renderA11yGate(container);
  });
});
