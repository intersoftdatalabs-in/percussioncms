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
  it("POSTs deleteFolder then refreshes list and tree", async () => {
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

    const row = await screen.findByTestId("detail-row-n-3646");
    fireEvent.click(row);

    const deleteBtn = await screen.findByTestId("action-delete");
    expect(deleteBtn).toBeEnabled();
    fireEvent.click(deleteBtn);

    await waitFor(() => {
      expect(deleteUrls.length).toBeGreaterThan(0);
    });
    expect(deleteUrls.every((u) => u.includes("deleteFolder"))).toBe(true);
    await waitFor(() => {
      expect(screen.queryByTestId("detail-row-n-3646")).not.toBeInTheDocument();
    });
    await waitFor(() => {
      expect(
        screen.queryByTestId("tree-node-/Sites/qa3646"),
      ).not.toBeInTheDocument();
    });
    await renderA11yGate(container);
  });
});
