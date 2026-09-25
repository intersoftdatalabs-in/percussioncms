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

const PAGE = {
  id: "99",
  path: "/Sites/hello.html",
  name: "hello.html",
  type: "page",
  accessLevel: "WRITE",
};

describe("ContentExplorerShell create page (#4874)", {
  timeout: EXPLORER_SHELL_TEST_TIMEOUT,
}, () => {
  it("shows the new page in the folder list after confirm", async () => {
    let created = false;
    let posts = 0;
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("/contenttypes") && !url.includes("/percPage")) {
        return new Response(
          JSON.stringify([{ name: "percPage", label: "Page" }]),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (url.includes("/item/create") && method === "POST") {
        posts += 1;
        created = true;
        return new Response(
          JSON.stringify({
            ItemCreateResult: {
              itemId: "99",
              name: "hello.html",
              folderPath: "/Sites",
              contentType: "percPage",
            },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      const kids = created ? [PAGE] : [];
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
          onCreatePage: async () => {
            posts += 1;
            created = true;
          },
        }}
      />,
    );

    fireEvent.click(
      await screen.findByTestId("action-create-page", {}, { timeout: 8_000 }),
    );
    const name = await screen.findByTestId("explorer-create-page-name");
    fireEvent.change(name, { target: { value: "hello" } });
    await waitFor(() => {
      expect(screen.getByTestId("explorer-create-page-type")).toHaveValue(
        "percPage",
      );
    });
    fireEvent.click(screen.getByTestId("explorer-create-page-confirm"));

    await waitFor(
      () => {
        expect(screen.getByTestId("detail-row-99")).toBeInTheDocument();
      },
      { timeout: 8_000 },
    );
    expect(posts).toBe(1);
    expect(screen.queryByTestId("explorer-create-page")).not.toBeInTheDocument();
    await renderA11yGate(container);
  });

  it("does not create when cancel is chosen or the name is blank", async () => {
    let posts = 0;
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("/contenttypes")) {
        return new Response(
          JSON.stringify([{ name: "percPage", label: "Page" }]),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      if (url.includes("/item/create") && method === "POST") {
        posts += 1;
      }
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
        listViews={async () => []}
        actionHandlers={{
          onCreatePage: async () => {
            posts += 1;
          },
        }}
      />,
    );

    fireEvent.click(
      await screen.findByTestId("action-create-page", {}, { timeout: 8_000 }),
    );
    fireEvent.click(await screen.findByTestId("explorer-create-page-confirm"));
    expect(await screen.findByTestId("explorer-create-page-error")).toHaveTextContent(
      "Enter a page name",
    );
    expect(posts).toBe(0);

    fireEvent.click(screen.getByTestId("explorer-create-page-cancel"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-create-page")).not.toBeInTheDocument();
    });
    expect(posts).toBe(0);
  });

  it("keeps an HTTP failure visible and does not show a new row", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/contenttypes")) {
        return new Response(
          JSON.stringify([{ name: "percPage", label: "Page" }]),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      return new Response(
        JSON.stringify({
          PagedItemList: { childrenInPage: [], childrenCount: 0, startIndex: 0 },
          PathItem: [],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
        listViews={async () => []}
        actionHandlers={{
          onCreatePage: async () => {
            throw { status: 409, statusText: "Conflict", body: {} };
          },
        }}
      />,
    );

    fireEvent.click(
      await screen.findByTestId("action-create-page", {}, { timeout: 8_000 }),
    );
    fireEvent.change(await screen.findByTestId("explorer-create-page-name"), {
      target: { value: "taken" },
    });
    await waitFor(() => {
      expect(screen.getByTestId("explorer-create-page-type")).toHaveValue(
        "percPage",
      );
    });
    fireEvent.click(screen.getByTestId("explorer-create-page-confirm"));
    expect(await screen.findByTestId("explorer-create-page-error")).toHaveTextContent(
      "Could not create the page",
    );
    expect(screen.queryByTestId("detail-row-99")).not.toBeInTheDocument();
    expect(screen.getByTestId("explorer-create-page")).toBeInTheDocument();
  });
});
