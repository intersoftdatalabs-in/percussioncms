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
import { describe, expect, it } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";
import { ContentExplorerShell } from "../../../main/ts/contentExplorer/ContentExplorerShell";
import { renderA11yGate } from "./a11y";
import { EXPLORER_SHELL_TEST_TIMEOUT, mockFetch } from "./setup";

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

const CREATED = {
  id: "n-3640",
  path: "/Sites/qa3640",
  name: "qa3640",
  type: "folder",
  accessLevel: "WRITE",
};

describe("ContentExplorerShell create folder (#3640)", {
  timeout: EXPLORER_SHELL_TEST_TIMEOUT,
}, () => {
  it("refreshes the detail list after Create Folder succeeds", async () => {
    let created = false;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/folders/create")) {
        created = true;
        return new Response(JSON.stringify({ Folder: CREATED }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      const kids = created ? [CREATED] : [];
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
        listViews={async () => []}
        actionHandlers={{
          prompt: () => "qa3640",
        }}
      />,
    );

    const createBtn = await screen.findByTestId(
      "action-create-folder",
      {},
      { timeout: 8_000 },
    );
    expect(createBtn).toBeEnabled();
    fireEvent.click(createBtn);

    await waitFor(
      () => {
        expect(screen.getByTestId("detail-row-n-3640")).toBeInTheDocument();
      },
      { timeout: 8_000 },
    );
    expect(screen.getByTestId("tree-node-/Sites/qa3640")).toBeInTheDocument();
    const nav = screen.getByTestId("explorer-nav");
    expect(Number(nav.getAttribute("data-folder-tree-epoch"))).toBeGreaterThan(
      0,
    );
    await renderA11yGate(container);
  });
});
