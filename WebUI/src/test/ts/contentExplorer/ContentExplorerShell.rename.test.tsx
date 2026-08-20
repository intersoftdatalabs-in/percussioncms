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

const OLD_FOLDER = {
  id: "f-3645",
  path: "/Sites/OldFolder",
  name: "OldFolder",
  type: "folder",
  accessLevel: "WRITE" as const,
};

const RENAMED_FOLDER = {
  ...OLD_FOLDER,
  path: "/Sites/qa3645",
  name: "qa3645",
};

describe("ContentExplorerShell rename folder (#3645)", () => {
  it("refreshes the detail list and tree after Rename succeeds", async () => {
    let renamed = false;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/pathmanagement/path/renameFolder")) {
        renamed = true;
        return new Response(JSON.stringify({ PathItem: RENAMED_FOLDER }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      const kids = renamed ? [RENAMED_FOLDER] : [OLD_FOLDER];
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
          prompt: () => "qa3645",
        }}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-row-f-3645")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-row-f-3645"));

    const renameBtn = await screen.findByTestId("action-rename");
    expect(renameBtn).toBeEnabled();
    fireEvent.click(renameBtn);

    await waitFor(() => {
      expect(screen.getByTestId("detail-cell-name-f-3645")).toHaveTextContent(
        "qa3645",
      );
    });
    expect(renamed).toBe(true);
    await waitFor(() => {
      expect(screen.getByTestId("tree-node-/Sites/qa3645")).toBeInTheDocument();
    });
    await renderA11yGate(container);
  });
});
