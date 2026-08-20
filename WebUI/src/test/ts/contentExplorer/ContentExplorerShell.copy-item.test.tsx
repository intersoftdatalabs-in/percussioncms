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
  id: "a-3656-src",
  path: "/Assets/qa3656_src",
  name: "qa3656_src",
  type: "percSimpleTextAsset",
  category: "ASSET",
  accessLevel: "WRITE" as const,
  folderPath: "/Assets",
  leaf: true,
};

const COPIED_ITEM = {
  id: "a-3656-copy",
  path: "/Assets/qa3656_dst/qa3656_src",
  name: "qa3656_src",
  type: "percSimpleTextAsset",
  category: "ASSET",
  accessLevel: "WRITE" as const,
  folderPath: "/Assets/qa3656_dst",
  leaf: true,
};

const DEST_FOLDER = {
  id: "f-3656-dst",
  path: "/Assets/qa3656_dst",
  name: "qa3656_dst",
  type: "folder",
  accessLevel: "WRITE" as const,
  folderPath: "/Assets",
};

describe("ContentExplorerShell copy item (#3656)", () => {
  it("POSTs copy/item for a selected asset and refreshes the dest list", async () => {
    let copied = false;
    const copyUrls: string[] = [];
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/folders/copy/")) {
        copyUrls.push(url);
        copied = true;
        return new Response(JSON.stringify({ message: "Copied OK" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      const destList = url.includes("qa3656_dst") || url.includes("paginatedFolder");
      const kids = copied && destList ? [COPIED_ITEM] : [SOURCE_ITEM, DEST_FOLDER];
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
          prompt: () => "/Assets/qa3656_dst",
        }}
      />,
    );

    const sourceRow = await screen.findByTestId("detail-row-a-3656-src");
    fireEvent.click(sourceRow);
    const copyBtn = await screen.findByTestId("action-copy");
    expect(copyBtn).toBeEnabled();
    fireEvent.click(copyBtn);

    await waitFor(() => {
      expect(copied).toBe(true);
      expect(copyUrls.some((u) => u.includes("/folders/copy/item"))).toBe(true);
      expect(copyUrls.some((u) => u.includes("/folders/copy/folder"))).toBe(
        false,
      );
      expect(screen.getByTestId("detail-row-a-3656-copy")).toBeInTheDocument();
    });
    await renderA11yGate(container);
  });
});
