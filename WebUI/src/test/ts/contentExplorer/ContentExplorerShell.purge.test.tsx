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
  path: "/Recycling/Assets/qa4763",
  name: "qa4763",
  type: "percSimpleTextAsset",
  accessLevel: "WRITE",
  leaf: true,
};

describe("ContentExplorerShell recycle purge one (#4763)", () => {
  it("DELETEs one recycled guid after confirm then drops the row", async () => {
    let purged = false;
    const purgeUrls: string[] = [];
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const method = (init as RequestInit | undefined)?.method ?? "GET";
      if (method === "DELETE" && url.includes("/folders/recycle/1-101-9")) {
        purged = true;
        purgeUrls.push(url);
        return new Response(
          JSON.stringify({ Status: { statusCode: 200, message: "Ok" } }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      const kids = purged ? [] : [RECYCLED];
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

    const realConfirm = window.confirm;
    window.confirm = () => true;
    try {
      renderShell(
        <ContentExplorerShell
          initialPath="/Recycling"
          loadDisplayFormats={async () => []}
          loadMenuActions={async () => []}
          loadWorkflowMenuActions={async () => null}
        />,
      );

      const row = await screen.findByTestId("detail-row-1-101-9");
      fireEvent.click(row);
      const purgeBtn = await screen.findByTestId("action-purge");
      fireEvent.click(purgeBtn);

      await waitFor(() => {
        expect(purgeUrls.length).toBeGreaterThan(0);
      });
      await waitFor(() => {
        expect(screen.queryByTestId("detail-row-1-101-9")).not.toBeInTheDocument();
      });
    } finally {
      window.confirm = realConfirm;
    }
  });
});
