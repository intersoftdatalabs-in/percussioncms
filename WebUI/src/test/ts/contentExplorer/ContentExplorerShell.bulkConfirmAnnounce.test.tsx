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

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function listingResponse(
  childrenInPage: Array<Record<string, unknown>>,
): Response {
  return jsonResponse({
    PagedItemList: {
      childrenInPage,
      childrenCount: childrenInPage.length,
      startIndex: 0,
    },
  });
}

function openContentMenu(itemTestId: "explorer-multi-recycle" | "explorer-multi-purge" | "explorer-multi-restore"): void {
  fireEvent.click(screen.getByTestId("explorer-menu-content"));
  // jsdom clicks do not move focus: focus the menu item first so the
  // shell captures the invoking control like a keyboard open would.
  screen.getByTestId(itemTestId).focus();
  fireEvent.click(screen.getByTestId(itemTestId));
}

describe("ContentExplorerShell bulk-confirm announcements (#7037)", () => {
  it("recycle confirm success announces via role=status and returns focus to the Content menu", async () => {
    const deleteUrls: string[] = [];
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const method = String(init?.method ?? "GET").toUpperCase();
      if (method === "DELETE" && url.includes("/folders/item/")) {
        deleteUrls.push(url);
        return jsonResponse({ Status: { statusCode: 200, message: "Ok" } });
      }
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return listingResponse([
          {
            id: "page-a",
            name: "Alpha",
            path: "/Assets/Src/Alpha",
            type: "percPage",
            category: "page",
            leaf: true,
          },
          {
            id: "page-b",
            name: "Beta",
            path: "/Assets/Src/Beta",
            type: "percPage",
            category: "page",
            leaf: true,
          },
        ]);
      }
      return jsonResponse({});
    });

    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Assets/Src"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-select-page-a")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-select-page-a"));
    fireEvent.click(screen.getByTestId("detail-select-page-b"));
    openContentMenu("explorer-multi-recycle");
    await screen.findByTestId("explorer-recycle-confirm");
    // Destructive confirm starts on Cancel so Enter cannot confirm (#7003).
    expect(screen.getByTestId("explorer-recycle-cancel")).toHaveFocus();
    fireEvent.click(screen.getByTestId("explorer-recycle-ok"));

    const result = await screen.findByTestId("explorer-multi-recycle-result");
    expect(result.getAttribute("data-outcome")).toBe("success");
    expect(result.getAttribute("role")).toBe("status");
    expect(result.textContent).toContain("Recycled 2 item(s)");
    expect(deleteUrls.length).toBe(2);
    // The Content menu closed when the command ran, so focus lands back on
    // the Content menu button — the stable return target for menu opens.
    await waitFor(() => {
      expect(screen.getByTestId("explorer-menu-content")).toHaveFocus();
    });
    await renderA11yGate(container);
  });

  it("purge partial failure names the item via role=alert and returns focus", async () => {
    const purgeUrls: string[] = [];
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const method = String(init?.method ?? "GET").toUpperCase();
      if (
        method === "DELETE" &&
        url.includes("/folders/recycle/") &&
        !url.includes("/restore") &&
        !url.includes("/empty")
      ) {
        purgeUrls.push(url);
        if (url.includes("guid-miss")) {
          return jsonResponse({}, 409);
        }
        return jsonResponse({ Status: { statusCode: 200, message: "Ok" } });
      }
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return listingResponse([
          {
            id: "guid-ok",
            name: "OkPage",
            path: "/Recycling/OkPage",
            type: "percPage",
            category: "page",
            leaf: true,
          },
          {
            id: "guid-miss",
            name: "Missing",
            path: "/Recycling/Missing",
            type: "percPage",
            category: "page",
            leaf: true,
          },
        ]);
      }
      return jsonResponse({});
    });

    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Recycling"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-select-guid-ok")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-select-guid-ok"));
    fireEvent.click(screen.getByTestId("detail-select-guid-miss"));
    openContentMenu("explorer-multi-purge");
    await screen.findByTestId("explorer-purge-confirm");
    // Destructive confirm starts on Cancel so Enter cannot confirm (#7003).
    expect(screen.getByTestId("explorer-purge-cancel")).toHaveFocus();
    fireEvent.click(screen.getByTestId("explorer-purge-ok"));

    const result = await screen.findByTestId("explorer-multi-purge-result");
    expect(result.getAttribute("data-outcome")).toBe("partial");
    expect(result.getAttribute("role")).toBe("alert");
    expect(result.textContent).toContain("Missing");
    expect(result.textContent).not.toContain("Purged 2 item(s)");
    expect(purgeUrls.length).toBe(2);
    await waitFor(() => {
      expect(screen.getByTestId("explorer-menu-content")).toHaveFocus();
    });
    await renderA11yGate(container);
  });

  it("restore cancel announces the no-op, sends nothing, and returns focus to the toolbar control", async () => {
    const restoreUrls: string[] = [];
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const method = String(init?.method ?? "GET").toUpperCase();
      if (method === "PUT" && url.includes("/folders/recycle/restore/")) {
        restoreUrls.push(url);
        return jsonResponse({ Status: { statusCode: 200, message: "Ok" } });
      }
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return listingResponse([
          {
            id: "guid-a",
            name: "Alpha",
            path: "/Recycling/Alpha",
            type: "percPage",
            category: "page",
            leaf: true,
          },
          {
            id: "guid-b",
            name: "Beta",
            path: "/Recycling/Beta",
            type: "percPage",
            category: "page",
            leaf: true,
          },
        ]);
      }
      return jsonResponse({});
    });

    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Recycling"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-select-guid-a")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-select-guid-a"));
    fireEvent.click(screen.getByTestId("detail-select-guid-b"));
    // Toolbar path: the ReducedActions control stays mounted, so focus
    // returns to the invoking control itself.
    screen.getByTestId("action-restore").focus();
    fireEvent.click(screen.getByTestId("action-restore"));
    await screen.findByTestId("explorer-restore-confirm");
    // Bulk confirm starts on Cancel so Enter cannot confirm (#7003).
    expect(screen.getByTestId("explorer-restore-cancel")).toHaveFocus();
    fireEvent.click(screen.getByTestId("explorer-restore-cancel"));

    expect(restoreUrls).toEqual([]);
    const result = await screen.findByTestId("explorer-multi-restore-result");
    expect(result.getAttribute("data-outcome")).toBe("cancelled");
    expect(result.getAttribute("role")).toBe("status");
    expect(result.textContent).toContain("Restore cancelled");
    await waitFor(() => {
      expect(screen.getByTestId("action-restore")).toHaveFocus();
    });
    await renderA11yGate(container);
  });

  it("Escape on the recycle confirm cancels with the no-op announcement", async () => {
    const deleteUrls: string[] = [];
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const method = String(init?.method ?? "GET").toUpperCase();
      if (method === "DELETE" && url.includes("/folders/item/")) {
        deleteUrls.push(url);
        return jsonResponse({ Status: { statusCode: 200, message: "Ok" } });
      }
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return listingResponse([
          {
            id: "page-a",
            name: "Alpha",
            path: "/Assets/Src/Alpha",
            type: "percPage",
            category: "page",
            leaf: true,
          },
          {
            id: "page-b",
            name: "Beta",
            path: "/Assets/Src/Beta",
            type: "percPage",
            category: "page",
            leaf: true,
          },
        ]);
      }
      return jsonResponse({});
    });

    renderShell(
      <ContentExplorerShell
        initialPath="/Assets/Src"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-select-page-a")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-select-page-a"));
    fireEvent.click(screen.getByTestId("detail-select-page-b"));
    openContentMenu("explorer-multi-recycle");
    await screen.findByTestId("explorer-recycle-confirm");
    fireEvent.keyDown(window, { key: "Escape" });

    expect(deleteUrls).toEqual([]);
    const result = await screen.findByTestId("explorer-multi-recycle-result");
    expect(result.getAttribute("data-outcome")).toBe("cancelled");
    expect(result.getAttribute("role")).toBe("status");
    expect(result.textContent).toContain("Recycle cancelled");
    await waitFor(() => {
      expect(screen.getByTestId("explorer-menu-content")).toHaveFocus();
    });
  });

  it("cancel keeps a single shared live region across re-renders", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return listingResponse([
          {
            id: "guid-a",
            name: "Alpha",
            path: "/Recycling/Alpha",
            type: "percPage",
            category: "page",
            leaf: true,
          },
          {
            id: "guid-b",
            name: "Beta",
            path: "/Recycling/Beta",
            type: "percPage",
            category: "page",
            leaf: true,
          },
        ]);
      }
      return jsonResponse({});
    });

    const ui = (
      <ContentExplorerShell
        initialPath="/Recycling"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />
    );
    const { rerender } = renderShell(ui);

    await waitFor(() => {
      expect(screen.getByTestId("detail-select-guid-a")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-select-guid-a"));
    fireEvent.click(screen.getByTestId("detail-select-guid-b"));
    openContentMenu("explorer-multi-purge");
    fireEvent.click(await screen.findByTestId("explorer-purge-cancel"));

    const first = await screen.findByTestId("explorer-multi-purge-result");
    expect(first.textContent).toContain("Purge cancelled");
    // An unrelated re-render must not duplicate or restate the region:
    // one node, stable text, polite live region.
    rerender(
      <BootstrapProvider value={adminBootstrap}>{ui}</BootstrapProvider>,
    );
    const regions = screen.getAllByTestId("explorer-multi-purge-result");
    expect(regions.length).toBe(1);
    expect(regions[0].textContent).toBe(first.textContent);
    expect(regions[0].getAttribute("role")).toBe("status");
  });

  it("purge names the catalog unnamed-item fallback for rows without a name", async () => {
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const method = String(init?.method ?? "GET").toUpperCase();
      if (
        method === "DELETE" &&
        url.includes("/folders/recycle/") &&
        !url.includes("/restore") &&
        !url.includes("/empty")
      ) {
        return jsonResponse({}, 403);
      }
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return listingResponse([
          {
            id: "guid-ok",
            name: "OkPage",
            path: "/Recycling/OkPage",
            type: "percPage",
            category: "page",
            leaf: true,
          },
          {
            id: "guid-blank",
            name: "",
            path: "",
            type: "percPage",
            category: "page",
            leaf: true,
          },
        ]);
      }
      return jsonResponse({});
    });

    renderShell(
      <ContentExplorerShell
        initialPath="/Recycling"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("detail-select-guid-ok")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("detail-select-guid-ok"));
    fireEvent.click(screen.getByTestId("detail-select-guid-blank"));
    openContentMenu("explorer-multi-purge");
    fireEvent.click(await screen.findByTestId("explorer-purge-ok"));

    const result = await screen.findByTestId("explorer-multi-purge-result");
    expect(result.getAttribute("role")).toBe("alert");
    // The display-name fallback resolves through EXPLORER_MSG, not a
    // hardcoded literal, so catalog locales apply to the announcement.
    expect(result.textContent).toContain("(unnamed)");
  });
});
