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

const SOURCE = {
  id: "f-3647-src",
  path: "/Assets/qa3647_src",
  name: "qa3647_src",
  type: "folder",
  accessLevel: "WRITE" as const,
  folderPath: "/Assets",
};

const COPIED = {
  id: "f-3647-copy",
  path: "/Assets/qa3647_src-2",
  name: "qa3647_src-2",
  type: "folder",
  accessLevel: "WRITE" as const,
  folderPath: "/Assets",
};

describe("ContentExplorerShell copy folder (#3647)", () => {
  it("refreshes destination list and tree after Copy succeeds", async () => {
    let copied = false;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/folders/copy/folder")) {
        copied = true;
        return new Response(JSON.stringify({ message: "Copied OK" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      const parentKids = copied ? [SOURCE, COPIED] : [SOURCE];
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        const kids = parentKids;
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
          prompt: () => "/Assets",
        }}
      />,
    );

    const sourceRow = await screen.findByTestId("detail-row-f-3647-src");
    fireEvent.click(sourceRow);
    const copyBtn = await screen.findByTestId("action-copy");
    expect(copyBtn).toBeEnabled();
    fireEvent.click(copyBtn);

    await waitFor(() => {
      expect(copied).toBe(true);
      expect(screen.getByTestId("detail-row-f-3647-copy")).toBeInTheDocument();
    });
    expect(screen.getByTestId("tree-node-/Assets/qa3647_src-2")).toBeInTheDocument();
    await renderA11yGate(container);
  });
});
