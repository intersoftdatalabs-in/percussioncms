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
import { afterEach, describe, expect, it, vi } from "vitest";
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

const PAGE_ID = "16777215-101-551";

function renderShell(ui: ReactElement) {
  return render(<BootstrapProvider value={adminBootstrap}>{ui}</BootstrapProvider>);
}

function stubList(children: unknown[]) {
  mockFetch(async () =>
    new Response(
      JSON.stringify({
        PagedItemList: {
          childrenInPage: children,
          childrenCount: children.length,
          startIndex: 0,
        },
        PathItem: [],
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    ),
  );
}

function openCopy(): void {
  fireEvent.click(screen.getByTestId("explorer-menu-content"));
  fireEvent.click(screen.getByTestId("explorer-copy-item-guid"));
}

describe("Content → Copy item id (#4989)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("copies the selected page content id and shows success", async () => {
    stubList([
      {
        id: PAGE_ID,
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "page",
        leaf: true,
      },
    ]);
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText },
    });
    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId(`detail-row-${PAGE_ID}`)).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId(`detail-row-${PAGE_ID}`));
    await waitFor(() =>
      expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
        "data-selected-item-id",
        PAGE_ID,
      ),
    );
    openCopy();
    await waitFor(() => {
      expect(screen.getByTestId("explorer-copy-item-guid-status")).toHaveAttribute(
        "data-kind",
        "success",
      );
    });
    expect(writeText).toHaveBeenCalledWith(PAGE_ID);
    expect(screen.getByTestId("explorer-copy-item-guid-status")).toHaveAttribute(
      "data-copied-guid",
      PAGE_ID,
    );
    expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
      "data-selected-item-id",
      PAGE_ID,
    );
    await renderA11yGate(container);
  });

  it("does not write the clipboard when no row is selected", async () => {
    stubList([]);
    const writeText = vi.fn();
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText },
    });
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-content")).toBeInTheDocument(),
    );
    openCopy();
    await waitFor(() => {
      expect(screen.getByTestId("explorer-copy-item-guid-status")).toHaveAttribute(
        "data-reason",
        "none",
      );
    });
    expect(writeText).not.toHaveBeenCalled();
    expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
      "data-selected-item-id",
      "",
    );
  });

  it("names a selected folder and does not write the clipboard", async () => {
    stubList([
      {
        id: "16777215-101-703",
        name: "Corporate Investments",
        path: "/Sites/CI/",
        type: "folder",
        category: "folder",
      },
    ]);
    const writeText = vi.fn();
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText },
    });
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-16777215-101-703")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("detail-row-16777215-101-703"));
    await waitFor(() =>
      expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
        "data-selected-item-id",
        "16777215-101-703",
      ),
    );
    openCopy();
    await waitFor(() => {
      const status = screen.getByTestId("explorer-copy-item-guid-status");
      expect(status).toHaveAttribute("data-kind", "error");
      expect(status).toHaveAttribute("data-reason", "folder");
      expect(status).toHaveTextContent("Corporate Investments");
    });
    expect(writeText).not.toHaveBeenCalled();
  });

  it("shows clipboard failure and keeps the selection", async () => {
    stubList([
      {
        id: PAGE_ID,
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "page",
        category: "page",
        leaf: true,
      },
    ]);
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: {
        writeText: vi.fn().mockRejectedValue(new Error("denied")),
      },
    });
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId(`detail-row-${PAGE_ID}`)).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId(`detail-row-${PAGE_ID}`));
    await waitFor(() =>
      expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
        "data-selected-item-id",
        PAGE_ID,
      ),
    );
    openCopy();
    await waitFor(() => {
      const status = screen.getByTestId("explorer-copy-item-guid-status");
      expect(status).toHaveAttribute("data-kind", "error");
      expect(status).toHaveAttribute("data-reason", "clipboard");
      expect(status).toHaveAttribute("data-copied-guid", PAGE_ID);
    });
    expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
      "data-selected-item-id",
      PAGE_ID,
    );
  });
});
