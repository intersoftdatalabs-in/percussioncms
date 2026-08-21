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
import { describe, expect, it } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";
import { ContentExplorerShell } from "../../../main/ts/contentExplorer/ContentExplorerShell";
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

function renderShell(ui: ReactElement) {
  return render(
    <BootstrapProvider value={adminBootstrap}>{ui}</BootstrapProvider>,
  );
}

const SOURCE = {
  id: "f-3655-src",
  path: "/Assets/qa3655_src",
  name: "qa3655_src",
  type: "folder",
  accessLevel: "WRITE" as const,
  folderPath: "/Assets",
};

const DEST = {
  id: "f-3655-dst",
  path: "/Assets/qa3655_dst",
  name: "qa3655_dst",
  type: "folder",
  accessLevel: "WRITE" as const,
  folderPath: "/Assets",
};

const MOVED = {
  ...SOURCE,
  path: "/Assets/qa3655_dst/qa3655_src",
  folderPath: "/Assets/qa3655_dst",
};

describe("ContentExplorerShell move folder (#3655)", () => {
  it("POSTs moveItem then opens dest and refreshes list and tree", async () => {
    let moved = false;
    const moveUrls: string[] = [];
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/pathmanagement/path/moveItem")) {
        moved = true;
        moveUrls.push(url);
        return new Response(JSON.stringify({ NoContent: { operation: "moveItem" } }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      const pathOnly = url.split("?")[0];
      const isDestList = /qa3655_dst/.test(pathOnly);
      const kids = moved
        ? isDestList
          ? [MOVED]
          : [DEST]
        : [SOURCE, DEST];
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
        initialPath="/Assets"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
        actionHandlers={{
          prompt: () => "/Assets/qa3655_dst",
        }}
      />,
    );

    const sourceRow = await screen.findByTestId("detail-row-f-3655-src");
    fireEvent.click(sourceRow);
    const moveBtn = await screen.findByTestId("action-move");
    expect(moveBtn).toBeEnabled();
    fireEvent.click(moveBtn);

    await waitFor(() => {
      expect(moved).toBe(true);
      expect(moveUrls.some((u) => u.includes("/pathmanagement/path/moveItem"))).toBe(
        true,
      );
      expect(screen.getByTestId("detail-row-f-3655-src")).toBeInTheDocument();
      expect(screen.getByText("qa3655_src")).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByTestId("tree-node-/Assets/qa3655_dst")).toBeInTheDocument();
    });
    expect(
      screen.queryByTestId("tree-node-/Assets/qa3655_src"),
    ).not.toBeInTheDocument();
    const nav = screen.getByTestId("explorer-nav");
    expect(Number(nav.getAttribute("data-folder-tree-epoch"))).toBeGreaterThan(
      0,
    );
    expect(Number(nav.getAttribute("data-list-epoch"))).toBeGreaterThan(0);
    await renderA11yGate(container);
  });
});
