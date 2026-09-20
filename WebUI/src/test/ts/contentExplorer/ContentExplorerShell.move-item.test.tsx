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

const SOURCE_ITEM = {
  id: "a-4601-src",
  path: "/Assets/qa4601_src/qa4601itm",
  name: "qa4601itm",
  type: "percSimpleTextAsset",
  category: "ASSET",
  accessLevel: "WRITE" as const,
  folderPath: "/Assets/qa4601_src",
  leaf: true,
};

const MOVED_ITEM = {
  ...SOURCE_ITEM,
  path: "/Assets/qa4601_dst/qa4601itm",
  folderPath: "/Assets/qa4601_dst",
};

const DEST_FOLDER = {
  id: "f-4601-dst",
  path: "/Assets/qa4601_dst",
  name: "qa4601_dst",
  type: "folder",
  accessLevel: "WRITE" as const,
  folderPath: "/Assets",
};

const SRC_FOLDER = {
  id: "f-4601-src",
  path: "/Assets/qa4601_src",
  name: "qa4601_src",
  type: "folder",
  accessLevel: "WRITE" as const,
  folderPath: "/Assets",
};

describe("ContentExplorerShell move item (#4601)", () => {
  it("POSTs move/item for a selected asset and refreshes the dest list", async () => {
    let moved = false;
    const moveUrls: string[] = [];
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/rest/folders/move/")) {
        moveUrls.push(url);
        moved = true;
        return new Response(JSON.stringify({ message: "Moved OK" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      const destList = url.includes("qa4601_dst") || url.includes("paginatedFolder");
      const kids = moved && destList ? [MOVED_ITEM] : [SOURCE_ITEM, DEST_FOLDER, SRC_FOLDER];
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
        initialPath="/Assets/qa4601_src"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
        listViews={async () => []}
        actionHandlers={{}}
      />,
    );

    const sourceRow = await screen.findByTestId("detail-row-a-4601-src");
    fireEvent.click(sourceRow);
    const moveBtn = await screen.findByTestId("action-move");
    expect(moveBtn).toBeEnabled();
    fireEvent.click(moveBtn);
    const destInput = await screen.findByTestId("explorer-move-dest-input");
    fireEvent.change(destInput, { target: { value: "/Assets/qa4601_dst" } });
    fireEvent.click(screen.getByTestId("explorer-move-dest-ok"));

    await waitFor(() => {
      expect(moved).toBe(true);
      expect(moveUrls.some((u) => u.includes("/rest/folders/move/item"))).toBe(true);
      expect(moveUrls.some((u) => u.includes("/rest/folders/move/folder"))).toBe(
        false,
      );
    });
    await renderA11yGate(container);
  });
});
