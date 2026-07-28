/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../../../main/ts/app/App";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";

vi.mock("../../../main/ts/api/home/homeApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/home/homeApi")>();
  return {
    ...actual,
    fetchRecentItems: vi.fn().mockResolvedValue([]),
    fetchMyContent: vi.fn().mockResolvedValue([]),
    fetchSites: vi.fn().mockResolvedValue([]),
    fetchFolderChildren: vi.fn().mockResolvedValue([]),
    searchContent: vi.fn().mockResolvedValue([]),
    createPage: vi.fn().mockResolvedValue({}),
    addToMyPages: vi.fn().mockResolvedValue(undefined),
    removeFromMyPages: vi.fn().mockResolvedValue(undefined),
    isMyPage: vi.fn().mockResolvedValue(false),
  };
});

vi.mock("../../../main/ts/api/publishing", () => ({
  // Publishing sections load their own APIs; keep module resolution soft
}));

vi.mock("../../../main/ts/api/widgetbuilder/widgetBuilderApi", () => ({
  isWidgetBuilderActive: vi.fn().mockResolvedValue(true),
  fetchSummaries: vi.fn().mockResolvedValue([]),
  loadDefinition: vi.fn(),
  saveDefinition: vi.fn(),
  deleteDefinition: vi.fn(),
  deployDefinition: vi.fn(),
  validateDefinition: vi.fn(),
}));

const bootstrap: SpaBootstrap = {
  userName: "demo",
  locale: "en-us",
  entry: "home",
  isAdmin: true,
  isDesigner: true,
  isWidgetBuilderActive: true,
};

describe("App shell", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    // BrowserRouter basename="/cm/app" requires pathname under that prefix
    window.history.replaceState({}, "", "/cm/app/spa.jsp");
  });

  afterEach(() => {
    cleanup();
    window.history.replaceState({}, "", "/");
  });

  it("renders TopNav and embedded Home shell from entry query", async () => {
    render(
      <App
        bootstrap={bootstrap}
        entrySearch="?entry=home"
        basename="/cm/app"
      />,
    );
    expect(screen.getByTestId("perc-spa-topnav")).toBeTruthy();
    expect(screen.getByTestId("perc-spa-user-name").textContent).toContain(
      "demo",
    );
    await waitFor(() => {
      expect(screen.getByTestId("home-shell")).toBeTruthy();
    });
  });

  it("shows publish nav for designer and loads PublishingShell", async () => {
    render(
      <App
        bootstrap={bootstrap}
        entrySearch="?entry=publish"
        basename="/cm/app"
      />,
    );
    expect(screen.getByTestId("nav-publish")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByTestId("publishing-shell")).toBeTruthy();
    });
  });

  it("hides admin tools for non-admin", () => {
    render(
      <App
        bootstrap={{ ...bootstrap, isAdmin: false, isDesigner: false }}
        entrySearch="?entry=home"
        basename="/cm/app"
      />,
    );
    expect(screen.queryByTestId("nav-admin")).toBeNull();
    expect(screen.queryByTestId("nav-publish")).toBeNull();
  });

  it("loads WorkflowAdminShell for admin entry", async () => {
    render(
      <App
        bootstrap={bootstrap}
        entrySearch="?entry=workflow&tab=roles"
        basename="/cm/app"
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("perc-workflow-admin-shell")).toBeTruthy();
    });
    expect(screen.getByTestId("tab-roles").getAttribute("aria-selected")).toBe(
      "true",
    );
  });

  it("loads AdminShell for admin tools entry", async () => {
    render(
      <App
        bootstrap={bootstrap}
        entrySearch="?entry=admin&tab=tools"
        basename="/cm/app"
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("perc-admin-shell")).toBeTruthy();
    });
    expect(screen.getByTestId("tab-tools").getAttribute("aria-selected")).toBe(
      "true",
    );
  });

  it("loads WidgetBuilder for eligible users", async () => {
    render(
      <App
        bootstrap={bootstrap}
        entrySearch="?entry=widget-builder"
        basename="/cm/app"
      />,
    );
    await waitFor(() => {
      const app = screen.queryByTestId("widget-builder-app");
      const disabled = screen.queryByTestId("wb-disabled");
      expect(app ?? disabled).toBeTruthy();
    });
  });

  it("redirects non-admin away from workflow to home", async () => {
    render(
      <App
        bootstrap={{ ...bootstrap, isAdmin: false, isDesigner: true }}
        entrySearch="?entry=workflow"
      
        basename="/cm/app"
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("home-shell")).toBeTruthy();
    });
  });

  it("redirects non-admin away from admin tools to home", async () => {
    render(
      <App
        bootstrap={{ ...bootstrap, isAdmin: false, isDesigner: true }}
        entrySearch="?entry=admin"
      
        basename="/cm/app"
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("home-shell")).toBeTruthy();
    });
    expect(screen.queryByTestId("perc-admin-shell")).toBeNull();
  });

  it("redirects ineligible users away from widget-builder to home", async () => {
    render(
      <App
        bootstrap={{
          ...bootstrap,
          isAdmin: false,
          isDesigner: false,
          isWidgetBuilderActive: true,
        }}
        entrySearch="?entry=widget-builder"
      
        basename="/cm/app"
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("home-shell")).toBeTruthy();
    });
    expect(screen.queryByTestId("widget-builder-app")).toBeNull();
  });

  it("redirects when widget builder inactive even for admin", async () => {
    render(
      <App
        bootstrap={{ ...bootstrap, isWidgetBuilderActive: false }}
        entrySearch="?entry=widget-builder"
      
        basename="/cm/app"
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("home-shell")).toBeTruthy();
    });
    expect(screen.queryByTestId("widget-builder-app")).toBeNull();
  });

  it("loads ContentExplorerShell for explorer entry", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response("[]", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    try {
      render(
        <App
          bootstrap={bootstrap}
          entrySearch="?entry=explorer&path=/Sites"
        
        basename="/cm/app"
      />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("content-explorer-shell")).toBeTruthy();
      });
      expect(screen.queryByTestId("route-explorer")).toBeNull();
    } finally {
      vi.unstubAllGlobals();
    }
  });
});
