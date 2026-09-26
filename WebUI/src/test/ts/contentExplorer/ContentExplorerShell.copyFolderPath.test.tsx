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

function renderShell(ui: ReactElement) {
  return render(<BootstrapProvider value={adminBootstrap}>{ui}</BootstrapProvider>);
}

function stubPathFetch() {
  mockFetch(async () =>
    new Response(
      JSON.stringify({
        PagedItemList: { childrenInPage: [], childrenCount: 0, startIndex: 0 },
        PathItem: [],
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    ),
  );
}

function openCopy(): void {
  fireEvent.click(screen.getByTestId("explorer-menu-content"));
  fireEvent.click(screen.getByTestId("explorer-copy-folder-path"));
}

describe("Content → Copy folder path (#4911)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("confirms, writes the folder path, and does not change the selection", async () => {
    stubPathFetch();
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-content")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
      "data-selected-folder-path",
      "/Sites/Demo",
    );
    openCopy();
    await waitFor(() => {
      expect(screen.getByTestId("explorer-copy-folder-path-status")).toHaveAttribute(
        "data-kind",
        "success",
      );
    });
    expect(writeText).toHaveBeenCalledWith("/Sites/Demo");
    expect(screen.getByTestId("explorer-copy-folder-path-status")).toHaveAttribute(
      "data-copied-path",
      "/Sites/Demo",
    );
    expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
      "data-selected-folder-path",
      "/Sites/Demo",
    );
    await renderA11yGate(container);
  });

  it("shows an error for an empty path and does not write the clipboard", async () => {
    stubPathFetch();
    const writeText = vi.fn();
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText },
    });
    const confirm = vi.spyOn(window, "confirm");
    renderShell(
      <ContentExplorerShell
        initialPath="/"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-content")).toBeInTheDocument(),
    );
    openCopy();
    await waitFor(() => {
      expect(screen.getByTestId("explorer-copy-folder-path-status")).toHaveAttribute(
        "data-kind",
        "error",
      );
    });
    expect(confirm).not.toHaveBeenCalled();
    expect(writeText).not.toHaveBeenCalled();
    expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
      "data-selected-folder-path",
      "/",
    );
  });

  it("cancel leaves the selection and writes nothing", async () => {
    stubPathFetch();
    const writeText = vi.fn();
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText },
    });
    vi.spyOn(window, "confirm").mockReturnValue(false);
    renderShell(
      <ContentExplorerShell
        initialPath="/Assets"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-content")).toBeInTheDocument(),
    );
    openCopy();
    await waitFor(() => {
      expect(window.confirm).toHaveBeenCalled();
    });
    expect(writeText).not.toHaveBeenCalled();
    expect(screen.queryByTestId("explorer-copy-folder-path-status")).toBeNull();
    expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
      "data-selected-folder-path",
      "/Assets",
    );
  });

  it("clipboard failure is shown and the selection stays", async () => {
    stubPathFetch();
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: {
        writeText: vi.fn().mockRejectedValue(new Error("denied")),
      },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderShell(
      <ContentExplorerShell
        initialPath="/Assets"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-menu-content")).toBeInTheDocument(),
    );
    openCopy();
    await waitFor(() => {
      const status = screen.getByTestId("explorer-copy-folder-path-status");
      expect(status).toHaveAttribute("data-kind", "error");
      expect(status).toHaveAttribute("data-copied-path", "/Assets");
    });
    expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
      "data-selected-folder-path",
      "/Assets",
    );
  });
});
