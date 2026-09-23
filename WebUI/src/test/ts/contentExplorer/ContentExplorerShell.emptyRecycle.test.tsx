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
import { describe, expect, it, vi } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";
import { ContentExplorerShell } from "../../../main/ts/contentExplorer/ContentExplorerShell";
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

const RECYCLED = {
  id: "1-101-9",
  path: "/Recycling/Assets/qa4762",
  name: "qa4762",
  type: "percSimpleTextAsset",
  accessLevel: "WRITE",
  leaf: true,
};

describe("ContentExplorerShell empty recycle bin (#4762)", () => {
  it("confirms, POSTs empty, then reloads an empty list", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    let emptied = false;
    const emptyUrls: string[] = [];
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/folders/recycle/empty")) {
        emptied = true;
        emptyUrls.push(url);
        return new Response(
          JSON.stringify({ Status: { statusCode: 200, message: "Ok" } }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      const kids = emptied ? [] : [RECYCLED];
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
        initialPath="/Recycling"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
      />,
    );

    expect(await screen.findByTestId("detail-row-1-101-9")).toBeInTheDocument();
    const emptyBtn = await screen.findByTestId("action-empty-recycle");
    expect(emptyBtn).toBeEnabled();
    fireEvent.click(emptyBtn);

    await waitFor(() => {
      expect(emptyUrls.length).toBeGreaterThan(0);
    });
    await waitFor(() => {
      expect(screen.queryByTestId("detail-row-1-101-9")).not.toBeInTheDocument();
    });
  });

  it("keeps the list when empty returns 409", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/folders/recycle/empty")) {
        return new Response("conflict", { status: 409, statusText: "Conflict" });
      }
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [RECYCLED],
              childrenCount: 1,
              startIndex: 0,
            },
            PathItem: [RECYCLED],
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
        initialPath="/Recycling"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
      />,
    );

    expect(await screen.findByTestId("detail-row-1-101-9")).toBeInTheDocument();
    fireEvent.click(await screen.findByTestId("action-empty-recycle"));
    await waitFor(() => {
      expect(
        screen.getAllByText(/could not empty the recycle bin/i).length,
      ).toBeGreaterThan(0);
    });
    expect(screen.getByTestId("detail-row-1-101-9")).toBeInTheDocument();
  });
});
