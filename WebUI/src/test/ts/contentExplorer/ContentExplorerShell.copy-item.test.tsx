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
const COPY_ITEM_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7207, same load-contention rationale as
 * the #7147 slice). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const COPY_ITEM_TEST_TIMEOUT = 30_000;

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
  it("POSTs copy/item for a selected asset and refreshes the dest list", { timeout: COPY_ITEM_TEST_TIMEOUT }, async () => {
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
        actionHandlers={{}}
      />,
    );

    const sourceRow = await screen.findByTestId("detail-row-a-3656-src", undefined, { timeout: COPY_ITEM_ROUND_TRIP_TIMEOUT });
    fireEvent.click(sourceRow);
    const copyBtn = await screen.findByTestId("action-copy", undefined, { timeout: COPY_ITEM_ROUND_TRIP_TIMEOUT });
    expect(copyBtn).toBeEnabled();
    fireEvent.click(copyBtn);
    const destInput = await screen.findByTestId("explorer-copy-dest-input", undefined, { timeout: COPY_ITEM_ROUND_TRIP_TIMEOUT });
    fireEvent.change(destInput, { target: { value: "/Assets/qa3656_dst" } });
    fireEvent.click(screen.getByTestId("explorer-copy-dest-ok"));

    // Timing-hardening for #6749, unified under the #7207 file budget: the
    // default 1s waitFor loses to full-suite CPU contention before the
    // post-copy refresh renders (same roulette class as #6742/#6706).
    await waitFor(() => {
      expect(copied).toBe(true);
      expect(copyUrls.some((u) => u.includes("/folders/copy/item"))).toBe(true);
      expect(copyUrls.some((u) => u.includes("/folders/copy/folder"))).toBe(
        false,
      );
      expect(screen.getByTestId("detail-row-a-3656-copy")).toBeInTheDocument();
    }, { timeout: COPY_ITEM_ROUND_TRIP_TIMEOUT });
    await renderA11yGate(container);
  });

  it("does not POST copy/item when the destination contains a NUL (#5551)", { timeout: COPY_ITEM_TEST_TIMEOUT }, async () => {
    const copyUrls: string[] = [];
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/folders/copy/")) {
        copyUrls.push(url);
        return new Response(JSON.stringify({ message: "Copied OK" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      const kids = [SOURCE_ITEM, DEST_FOLDER];
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
        actionHandlers={{}}
      />,
    );

    const sourceRow = await screen.findByTestId("detail-row-a-3656-src", undefined, { timeout: COPY_ITEM_ROUND_TRIP_TIMEOUT });
    fireEvent.click(sourceRow);
    fireEvent.click(await screen.findByTestId("action-copy", undefined, { timeout: COPY_ITEM_ROUND_TRIP_TIMEOUT }));
    fireEvent.change(await screen.findByTestId("explorer-copy-dest-input", undefined, { timeout: COPY_ITEM_ROUND_TRIP_TIMEOUT }), {
      target: { value: "/Assets/qa3656_dst\u0000" },
    });
    fireEvent.click(screen.getByTestId("explorer-copy-dest-ok"));

    expect(copyUrls).toEqual([]);
    expect(screen.getByTestId("explorer-copy-dest-picker")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-copy-dest-error")).toHaveTextContent(
      /cannot be stored/i,
    );
    expect(screen.queryByTestId("detail-row-a-3656-copy")).toBeNull();
    await renderA11yGate(container);
  });

  it("does not POST copy/item when the destination is blank (#5553)", { timeout: COPY_ITEM_TEST_TIMEOUT }, async () => {
    const copyUrls: string[] = [];
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("/folders/copy/")) {
        copyUrls.push(url);
        return new Response(JSON.stringify({ message: "Copied OK" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      const kids = [SOURCE_ITEM, DEST_FOLDER];
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
        actionHandlers={{}}
      />,
    );

    const sourceRow = await screen.findByTestId("detail-row-a-3656-src", undefined, { timeout: COPY_ITEM_ROUND_TRIP_TIMEOUT });
    fireEvent.click(sourceRow);
    fireEvent.click(await screen.findByTestId("action-copy", undefined, { timeout: COPY_ITEM_ROUND_TRIP_TIMEOUT }));
    fireEvent.change(await screen.findByTestId("explorer-copy-dest-input", undefined, { timeout: COPY_ITEM_ROUND_TRIP_TIMEOUT }), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("explorer-copy-dest-ok"));

    expect(copyUrls).toEqual([]);
    expect(screen.getByTestId("explorer-copy-dest-picker")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-copy-dest-error")).toHaveTextContent(
      /enter a destination folder path/i,
    );
    expect(screen.queryByTestId("detail-row-a-3656-copy")).toBeNull();
    await renderA11yGate(container);
  });
});
