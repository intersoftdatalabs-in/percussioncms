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
 * distributed under the License is distributed on an open BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
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

/**
 * Single wall-clock budget for every polling gate in this file (#7207).
 * Each round trip here is purely microtask-driven (mocked promises
 * resolve, then setState, then render) with no real timers involved,
 * so the gates are deterministic in outcome; the budget is strictly a
 * backstop for loaded-worker scheduling lag. Every {@code findBy} and
 * {@code waitFor} uses it explicitly: the 1s/5s testing-library
 * defaults are the most-tripped cliffs when hundreds of suite files
 * share the worker host (same rationale as {@code RETRY_ROUND_TRIP_TIMEOUT}
 * in #7122 and the #7147 bin-lifecycle slice).
 */
const RECYCLE_CONTEXT_MENU_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7207, same load-contention rationale as
 * the #7147 slice). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const RECYCLE_CONTEXT_MENU_TEST_TIMEOUT = 30_000;

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
  path: "/Recycling/Assets/qa6254",
  name: "qa6254",
  type: "percSimpleTextAsset",
  accessLevel: "WRITE",
  leaf: true,
};

const LIVE = {
  id: "2-101-9",
  path: "/Sites/Demo/home",
  name: "home",
  type: "percPage",
  accessLevel: "WRITE",
  leaf: true,
};

function mockRecycleBin(onRestore: (url: string) => void) {
  mockFetch(async (input) => {
    const url = typeof input === "string" ? input : (input as Request).url;
    if (url.includes("/folders/recycle/restore/")) {
      onRestore(url);
      return new Response(
        JSON.stringify({ Status: { statusCode: 200, message: "Ok" } }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
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
}

describe("ContentExplorerShell recycled-row context menu (#6254)", () => {
  it("offers Restore + Purge on a recycled row and restores without the toolbar", { timeout: RECYCLE_CONTEXT_MENU_TEST_TIMEOUT }, async () => {
    const restoreUrls: string[] = [];
    mockRecycleBin((url) => {
      restoreUrls.push(url);
    });

    renderShell(
      <ContentExplorerShell
        initialPath="/Recycling"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
      />,
    );

    const row = await screen.findByTestId("detail-row-1-101-9", undefined, { timeout: RECYCLE_CONTEXT_MENU_ROUND_TRIP_TIMEOUT });
    fireEvent.contextMenu(row);

    // The server catalog is empty here, so both leaves are injected (#6254).
    const restoreItem = await screen.findByTestId("context-menu-item-Restore", undefined, { timeout: RECYCLE_CONTEXT_MENU_ROUND_TRIP_TIMEOUT });
    expect(
      screen.queryByTestId("context-menu-item-Purge"),
    ).not.toBeNull();
    fireEvent.click(restoreItem);

    await waitFor(() => {
      expect(restoreUrls.length).toBeGreaterThan(0);
    }, { timeout: RECYCLE_CONTEXT_MENU_ROUND_TRIP_TIMEOUT });
    expect(
      restoreUrls.every((u) => u.includes("/recycle/restore/1-101-9")),
    ).toBe(true);
  });

  it("purges a recycled row from the context menu after confirm", { timeout: RECYCLE_CONTEXT_MENU_TEST_TIMEOUT }, async () => {
    const purgeUrls: string[] = [];
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const method = (init as RequestInit | undefined)?.method ?? "GET";
      if (method === "DELETE" && url.includes("/folders/recycle/1-101-9")) {
        purgeUrls.push(url);
        return new Response(
          JSON.stringify({ Status: { statusCode: 200, message: "Ok" } }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
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

      const row = await screen.findByTestId("detail-row-1-101-9", undefined, { timeout: RECYCLE_CONTEXT_MENU_ROUND_TRIP_TIMEOUT });
      fireEvent.contextMenu(row);

      const purgeItem = await screen.findByTestId("context-menu-item-Purge", undefined, { timeout: RECYCLE_CONTEXT_MENU_ROUND_TRIP_TIMEOUT });
      fireEvent.click(purgeItem);

      await waitFor(() => {
        expect(purgeUrls.length).toBeGreaterThan(0);
      }, { timeout: RECYCLE_CONTEXT_MENU_ROUND_TRIP_TIMEOUT });
    } finally {
      window.confirm = realConfirm;
    }
  });

  it("offers no Restore on a live row context menu", { timeout: RECYCLE_CONTEXT_MENU_TEST_TIMEOUT }, async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [LIVE],
              childrenCount: 1,
              startIndex: 0,
            },
            PathItem: [LIVE],
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
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
        loadWorkflowMenuActions={async () => null}
      />,
    );

    const row = await screen.findByTestId("detail-row-2-101-9", undefined, { timeout: RECYCLE_CONTEXT_MENU_ROUND_TRIP_TIMEOUT });
    fireEvent.contextMenu(row);

    await screen.findByTestId("context-menu", undefined, { timeout: RECYCLE_CONTEXT_MENU_ROUND_TRIP_TIMEOUT });
    expect(screen.queryByTestId("context-menu-item-Restore")).toBeNull();
  });
});
