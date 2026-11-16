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
import { ExplorerBreadcrumb } from "../../../main/ts/contentExplorer/ExplorerBreadcrumb";
import {
  breadcrumbPathAt,
  buildExplorerDeepLink,
  collapseBreadcrumbSegments,
  splitBreadcrumbSegments,
} from "../../../main/ts/contentExplorer/breadcrumbPath";
import { renderA11yGate } from "./a11y";
import { mockFetch } from "./setup";

const PAGE_URL = "https://cms.example/Rhythmyx/cm/app/spa.jsp?entry=explorer";

describe("splitBreadcrumbSegments", () => {
  it("returns no segments for root, blank, and nullish paths", () => {
    expect(splitBreadcrumbSegments("/")).toEqual([]);
    expect(splitBreadcrumbSegments("  ")).toEqual([]);
    expect(splitBreadcrumbSegments(null)).toEqual([]);
    expect(splitBreadcrumbSegments(undefined)).toEqual([]);
  });

  it("splits CMS paths and normalizes backslash separators", () => {
    expect(splitBreadcrumbSegments("/Sites/Demo/About")).toEqual([
      "Sites",
      "Demo",
      "About",
    ]);
    expect(splitBreadcrumbSegments("Sites\\Demo")).toEqual(["Sites", "Demo"]);
  });
});

describe("breadcrumbPathAt", () => {
  it("builds cumulative CMS paths per segment", () => {
    const segments = ["Sites", "Demo", "About"];
    expect(breadcrumbPathAt(segments, 0)).toBe("/Sites");
    expect(breadcrumbPathAt(segments, 1)).toBe("/Sites/Demo");
    expect(breadcrumbPathAt(segments, 2)).toBe("/Sites/Demo/About");
  });
});

describe("collapseBreadcrumbSegments", () => {
  it("keeps short trails fully visible", () => {
    const crumbs = collapseBreadcrumbSegments(["Sites", "Demo", "About"]);
    expect(crumbs.filter((crumb) => crumb.kind === "overflow")).toHaveLength(0);
    expect(crumbs).toHaveLength(3);
  });

  it("collapses the middle behind one overflow, keeping first and tail", () => {
    const segments = ["Sites", "a", "b", "c", "d", "About"];
    const crumbs = collapseBreadcrumbSegments(segments, 4);
    expect(crumbs[0]).toMatchObject({ kind: "segment", name: "Sites" });
    expect(crumbs[1]).toEqual({ kind: "overflow", names: ["a", "b"] });
    expect(crumbs.slice(2).map((crumb) => (crumb.kind === "segment" ? crumb.name : ""))).toEqual([
      "c",
      "d",
      "About",
    ]);
    const tail = crumbs[crumbs.length - 1];
    expect(tail).toMatchObject({ kind: "segment", path: "/Sites/a/b/c/d/About" });
  });
});

describe("buildExplorerDeepLink", () => {
  it("sets ?path= on the Explorer entry and strips the hash", () => {
    expect(
      buildExplorerDeepLink(`${PAGE_URL}#/stale`, "/Sites/Demo"),
    ).toBe(`${PAGE_URL}&path=${encodeURIComponent("/Sites/Demo")}`);
  });

  it("returns null for empty folder paths and unparseable bases", () => {
    expect(buildExplorerDeepLink(PAGE_URL, "/")).toBeNull();
    expect(buildExplorerDeepLink(PAGE_URL, "   ")).toBeNull();
    expect(buildExplorerDeepLink("not a url", "/Sites/Demo")).toBeNull();
    expect(buildExplorerDeepLink(null, "/Sites/Demo")).toBeNull();
  });
});

describe("ExplorerBreadcrumb wayfinding (#6468)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders ancestor segments and navigates to the clicked ancestor", async () => {
    const onNavigate = vi.fn();
    const { container } = render(
      <ExplorerBreadcrumb
        folderPath="/Sites/Demo/About"
        onNavigate={onNavigate}
        pageUrl={PAGE_URL}
        writeClipboard={async () => {}}
      />,
    );
    expect(screen.getByTestId("explorer-breadcrumb")).toHaveAttribute(
      "data-full-path",
      "/Sites/Demo/About",
    );
    fireEvent.click(screen.getByTestId("explorer-breadcrumb-home"));
    expect(onNavigate).toHaveBeenCalledWith("/");
    const segments = screen.getAllByTestId("explorer-breadcrumb-segment");
    expect(segments.map((segment) => segment.getAttribute("data-path"))).toEqual([
      "/Sites",
      "/Sites/Demo",
    ]);
    fireEvent.click(segments[0]);
    expect(onNavigate).toHaveBeenCalledWith("/Sites");
    expect(screen.getByTestId("explorer-breadcrumb-current")).toHaveTextContent("About");
    await renderA11yGate(container);
  });

  it("collapses long trails behind an expandable overflow", async () => {
    const onNavigate = vi.fn();
    const { container } = render(
      <ExplorerBreadcrumb
        folderPath="/Sites/a/b/c/d/About"
        onNavigate={onNavigate}
        pageUrl={PAGE_URL}
        writeClipboard={async () => {}}
      />,
    );
    const overflow = screen.getByTestId("explorer-breadcrumb-overflow");
    expect(overflow).toHaveAttribute("aria-expanded", "false");
    expect(overflow.getAttribute("aria-label")).toContain("a");
    expect(overflow.getAttribute("aria-label")).toContain("b");
    expect(
      screen.queryByTestId("explorer-breadcrumb-overflow-list"),
    ).not.toBeInTheDocument();
    fireEvent.click(overflow);
    expect(overflow).toHaveAttribute("aria-expanded", "true");
    const hidden = screen.getAllByTestId("explorer-breadcrumb-hidden-segment");
    expect(hidden.map((node) => node.textContent)).toEqual(["a", "b"]);
    await renderA11yGate(container);
  });

  it("collapses the overflow again when the folder changes", async () => {
    const { rerender } = render(
      <ExplorerBreadcrumb
        folderPath="/Sites/a/b/c/d/About"
        onNavigate={() => {}}
        pageUrl={PAGE_URL}
        writeClipboard={async () => {}}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-breadcrumb-overflow"));
    expect(
      screen.queryByTestId("explorer-breadcrumb-overflow-list"),
    ).toBeInTheDocument();
    rerender(
      <ExplorerBreadcrumb
        folderPath="/Sites/a/b/c/d/Contact"
        onNavigate={() => {}}
        pageUrl={PAGE_URL}
        writeClipboard={async () => {}}
      />,
    );
    await waitFor(() => {
      expect(
        screen.queryByTestId("explorer-breadcrumb-overflow-list"),
      ).not.toBeInTheDocument();
    });
  });

  it("copies the deep link and reports success", async () => {    const writeClipboard = vi.fn().mockResolvedValue(undefined);
    render(
      <ExplorerBreadcrumb
        folderPath="/Sites/Demo"
        onNavigate={() => {}}
        pageUrl={PAGE_URL}
        writeClipboard={writeClipboard}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-breadcrumb-copy-link"));
    await waitFor(() => {
      expect(writeClipboard).toHaveBeenCalledWith(
        `${PAGE_URL}&path=${encodeURIComponent("/Sites/Demo")}`,
      );
    });
    await waitFor(() => {
      expect(screen.getByTestId("explorer-breadcrumb-copy-status")).toHaveAttribute(
        "data-kind",
        "success",
      );
    });
  });

  it("reports failure when the clipboard refuses the link", async () => {
    render(
      <ExplorerBreadcrumb
        folderPath="/Sites/Demo"
        onNavigate={() => {}}
        pageUrl={PAGE_URL}
        writeClipboard={async () => {
          throw new Error("denied");
        }}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-breadcrumb-copy-link"));
    await waitFor(() => {
      const status = screen.getByTestId("explorer-breadcrumb-copy-status");
      expect(status).toHaveAttribute("data-kind", "error");
      expect(status).toHaveAttribute("data-reason", "failed");
    });
  });

  it("reports empty when the open folder has no linkable path", async () => {
    const writeClipboard = vi.fn().mockResolvedValue(undefined);
    render(
      <ExplorerBreadcrumb
        folderPath="/"
        onNavigate={() => {}}
        pageUrl={PAGE_URL}
        writeClipboard={writeClipboard}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-breadcrumb-copy-link"));
    await waitFor(() => {
      const status = screen.getByTestId("explorer-breadcrumb-copy-status");
      expect(status).toHaveAttribute("data-kind", "error");
      expect(status).toHaveAttribute("data-reason", "empty");
    });
    expect(writeClipboard).not.toHaveBeenCalled();
  });
});

describe("ExplorerBreadcrumb in ContentExplorerShell (#6468)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

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

  it("mounts the wayfinding bar and steps back to an ancestor", async () => {
    mockFetch(
      async () =>
        new Response(
          JSON.stringify({
            PagedItemList: { childrenInPage: [], childrenCount: 0, startIndex: 0 },
            PathItem: [],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
    );
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites/Demo/About"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-breadcrumb")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("explorer-breadcrumb")).toHaveAttribute(
      "data-full-path",
      "/Sites/Demo/About",
    );
    fireEvent.click(screen.getAllByTestId("explorer-breadcrumb-segment")[0]);
    await waitFor(() => {
      expect(screen.getByTestId("content-explorer-shell")).toHaveAttribute(
        "data-selected-folder-path",
        "/Sites",
      );
    });
    expect(screen.getByTestId("explorer-breadcrumb")).toHaveAttribute(
      "data-full-path",
      "/Sites",
    );
  });
});
