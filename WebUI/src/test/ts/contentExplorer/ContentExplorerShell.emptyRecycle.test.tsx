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
  path: "/Recycling/Assets/qa4762",
  name: "qa4762",
  type: "percSimpleTextAsset",
  accessLevel: "WRITE",
  leaf: true,
};

/**
 * Single wall-clock budget for every polling gate in this file (#7147).
 * Each round trip here is purely microtask-driven (mockFetch resolves →
 * setState → render → passive-effect commit) with no real timers involved,
 * so the gates are deterministic in outcome; the budget is strictly a
 * backstop for loaded-worker scheduling lag. Every {@code waitFor} /
 * {@code findBy} uses it explicitly: the 1s testing-library
 * {@code findBy} default is the most-tripped cliff when hundreds of suite
 * files share the worker host (same rationale as
 * {@code RETRY_ROUND_TRIP_TIMEOUT} in #7122).
 */
const EMPTY_RECYCLE_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7147, same load-contention rationale as
 * {@code CONTEXT_MENU_RETRY_TEST_TIMEOUT} in #7122). The sequential
 * backstop gates above can jointly exceed the 20s global default when the
 * worker host is saturated. The primary gates stay progress-based
 * (single bounded backstop per distinct state); this ceiling is strictly
 * a last-resort backstop.
 */
const EMPTY_RECYCLE_TEST_TIMEOUT = 30_000;

describe("ContentExplorerShell empty recycle bin (#6470)", () => {
  it("confirms in the dialog, POSTs empty, then reloads an empty list", { timeout: EMPTY_RECYCLE_TEST_TIMEOUT }, async () => {
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

    expect(await screen.findByTestId("detail-row-1-101-9", undefined, {
      timeout: EMPTY_RECYCLE_ROUND_TRIP_TIMEOUT,
    })).toBeInTheDocument();
    const emptyBtn = await screen.findByTestId("action-empty-recycle", undefined, {
      timeout: EMPTY_RECYCLE_ROUND_TRIP_TIMEOUT,
    });
    expect(emptyBtn).toBeEnabled();
    fireEvent.click(emptyBtn);

    // The confirm dialog lists the bin contents (#6470).
    const list = await screen.findByTestId("explorer-empty-recycle-list", undefined, {
      timeout: EMPTY_RECYCLE_ROUND_TRIP_TIMEOUT,
    });
    expect(list).toHaveTextContent(/qa4762/);
    fireEvent.click(screen.getByTestId("explorer-empty-recycle-ok"));
    const report = await screen.findByTestId("explorer-empty-recycle-report", undefined, {
      timeout: EMPTY_RECYCLE_ROUND_TRIP_TIMEOUT,
    });
    expect(report).toHaveTextContent(/Removed 1 item\(s\)/);

    await waitFor(() => {
      expect(emptyUrls.length).toBeGreaterThan(0);
    }, { timeout: EMPTY_RECYCLE_ROUND_TRIP_TIMEOUT });
    fireEvent.click(screen.getByTestId("explorer-empty-recycle-close"));
    await waitFor(() => {
      expect(screen.queryByTestId("detail-row-1-101-9")).not.toBeInTheDocument();
    }, { timeout: EMPTY_RECYCLE_ROUND_TRIP_TIMEOUT });
  });

  it("keeps the list when empty returns 409", { timeout: EMPTY_RECYCLE_TEST_TIMEOUT }, async () => {
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

    expect(await screen.findByTestId("detail-row-1-101-9", undefined, {
      timeout: EMPTY_RECYCLE_ROUND_TRIP_TIMEOUT,
    })).toBeInTheDocument();
    fireEvent.click(await screen.findByTestId("action-empty-recycle", undefined, {
      timeout: EMPTY_RECYCLE_ROUND_TRIP_TIMEOUT,
    }));
    await screen.findByTestId("explorer-empty-recycle-list", undefined, {
      timeout: EMPTY_RECYCLE_ROUND_TRIP_TIMEOUT,
    });
    fireEvent.click(screen.getByTestId("explorer-empty-recycle-ok"));
    const report = await screen.findByTestId("explorer-empty-recycle-report", undefined, {
      timeout: EMPTY_RECYCLE_ROUND_TRIP_TIMEOUT,
    });
    expect(report.textContent?.toLowerCase()).toContain("could not empty");
    fireEvent.click(screen.getByTestId("explorer-empty-recycle-close"));
    expect(screen.getByTestId("detail-row-1-101-9")).toBeInTheDocument();
  });
});
