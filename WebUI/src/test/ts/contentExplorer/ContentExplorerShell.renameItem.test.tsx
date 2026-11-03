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

const ASSET = {
  id: "a-5480",
  path: "/Assets/Home",
  name: "Home",
  type: "percSimpleTextAsset",
  category: "ASSET",
  accessLevel: "WRITE" as const,
  leaf: true,
};

const RENAMED = {
  ...ASSET,
  path: "/Assets/Home2",
  name: "Home2",
};

describe("ContentExplorerShell rename item (#5480)", {
  timeout: EXPLORER_SHELL_TEST_TIMEOUT,
}, () => {
  it("does not rename or refresh when the item name contains a NUL (#5480)", async () => {
    let renamed = false;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/folders/rename/item")) {
        renamed = true;
        return new Response(JSON.stringify({ message: "Renamed OK" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      const kids = [ASSET];
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
        listViews={async () => []}
        actionHandlers={{
          prompt: () => "Home\u00002",
        }}
      />,
    );

    await waitFor(
      () => {
        expect(screen.getByTestId("detail-row-a-5480")).toBeInTheDocument();
      },
      { timeout: 8_000 },
    );
    fireEvent.click(screen.getByTestId("detail-row-a-5480"));
    fireEvent.click(
      await screen.findByTestId("action-rename", {}, { timeout: 8_000 }),
    );

    await waitFor(() => {
      expect(screen.getByTestId("explorer-server-actions-error")).toHaveTextContent(
        /cannot be stored/i,
      );
    });
    expect(screen.queryByTestId("explorer-folder-rename")).not.toBeInTheDocument();
    expect(screen.getByTestId("detail-cell-name-a-5480")).toHaveTextContent(
      "Home",
    );
    expect(renamed).toBe(false);
    await renderA11yGate(container);
  });

  it("renames a selected item and refreshes the list after success (#5480)", async () => {
    let renamed = false;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/folders/rename/item")) {
        renamed = true;
        return new Response(JSON.stringify({ message: "Renamed OK" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      const kids = renamed ? [RENAMED] : [ASSET];
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

    renderShell(
      <ContentExplorerShell
        initialPath="/Assets"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
        listViews={async () => []}
        actionHandlers={{
          prompt: () => "Home2",
        }}
      />,
    );

    await waitFor(
      () => {
        expect(screen.getByTestId("detail-row-a-5480")).toBeInTheDocument();
      },
      { timeout: 8_000 },
    );
    fireEvent.click(screen.getByTestId("detail-row-a-5480"));
    fireEvent.click(
      await screen.findByTestId("action-rename", {}, { timeout: 8_000 }),
    );

    await waitFor(
      () => {
        expect(screen.getByTestId("detail-cell-name-a-5480")).toHaveTextContent(
          "Home2",
        );
      },
      { timeout: 8_000 },
    );
    expect(renamed).toBe(true);
  });
});
