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
  id: "88",
  path: "/Assets/logo",
  name: "logo",
  type: "percFileAsset",
  category: "asset",
  accessLevel: "WRITE",
};

const TYPES = {
  WidgetContentType: [
    {
      widgetId: "percFile",
      widgetLabel: "File",
      contentTypeName: "percFileAsset",
    },
  ],
};

describe("ContentExplorerShell create asset (#4970)", {
  timeout: EXPLORER_SHELL_TEST_TIMEOUT,
}, () => {
  it("shows the new asset in the folder list after confirm", async () => {
    let created = false;
    let posts = 0;
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("assetTypes")) {
        return new Response(JSON.stringify(TYPES), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      if (url.includes("/item/create") && method === "POST") {
        posts += 1;
        created = true;
        return new Response(
          JSON.stringify({
            ItemCreateResult: {
              itemId: "88",
              name: "logo",
              folderPath: "/Assets",
              contentType: "percFileAsset",
            },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      const kids = created ? [ASSET] : [];
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
          onCreateAsset: async () => {
            posts += 1;
            created = true;
          },
        }}
      />,
    );

    fireEvent.click(
      await screen.findByTestId("action-create-asset", {}, { timeout: 8_000 }),
    );
    const name = await screen.findByTestId("explorer-create-asset-name");
    fireEvent.change(name, { target: { value: "logo" } });
    await waitFor(() => {
      expect(screen.getByTestId("explorer-create-asset-type")).toHaveValue(
        "percFileAsset",
      );
    });
    fireEvent.click(screen.getByTestId("explorer-create-asset-confirm"));

    await waitFor(
      () => {
        expect(screen.getByTestId("detail-row-88")).toBeInTheDocument();
      },
      { timeout: 8_000 },
    );
    expect(posts).toBe(1);
    expect(screen.queryByTestId("explorer-create-asset")).not.toBeInTheDocument();
    await renderA11yGate(container);
  });

  it("does not create when cancel is chosen or the name is blank", async () => {
    let posts = 0;
    mockFetch(async () =>
      new Response(JSON.stringify(TYPES), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

    renderShell(
      <ContentExplorerShell
        initialPath="/Assets"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
        listViews={async () => []}
        actionHandlers={{
          onCreateAsset: async () => {
            posts += 1;
          },
        }}
      />,
    );

    fireEvent.click(
      await screen.findByTestId("action-create-asset", {}, { timeout: 8_000 }),
    );
    fireEvent.click(await screen.findByTestId("explorer-create-asset-confirm"));
    expect(await screen.findByTestId("explorer-create-asset-error")).toHaveTextContent(
      "Enter an asset name",
    );
    expect(posts).toBe(0);

    fireEvent.click(screen.getByTestId("explorer-create-asset-cancel"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-create-asset")).not.toBeInTheDocument();
    });
    expect(posts).toBe(0);
  });

  it("keeps an HTTP failure visible and does not show a new row", async () => {
    mockFetch(async () =>
      new Response(JSON.stringify(TYPES), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

    renderShell(
      <ContentExplorerShell
        initialPath="/Assets"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
        listViews={async () => []}
        actionHandlers={{
          onCreateAsset: async () => {
            throw { status: 409, statusText: "Conflict", body: {} };
          },
        }}
      />,
    );

    fireEvent.click(
      await screen.findByTestId("action-create-asset", {}, { timeout: 8_000 }),
    );
    fireEvent.change(await screen.findByTestId("explorer-create-asset-name"), {
      target: { value: "taken" },
    });
    await waitFor(() => {
      expect(screen.getByTestId("explorer-create-asset-type")).toHaveValue(
        "percFileAsset",
      );
    });
    fireEvent.click(screen.getByTestId("explorer-create-asset-confirm"));
    expect(await screen.findByTestId("explorer-create-asset-error")).toHaveTextContent(
      "Could not create the asset",
    );
    expect(screen.queryByTestId("detail-row-88")).not.toBeInTheDocument();
    expect(screen.getByTestId("explorer-create-asset")).toBeInTheDocument();
  });
});
