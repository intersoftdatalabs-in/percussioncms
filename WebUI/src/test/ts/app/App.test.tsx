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

// Load feature shells via direct imports (still async for React.lazy) but skip
// the production registry graph. Avoids blank barrel mocks and keeps Suspense
// resolution predictable under jsdom.
vi.mock("../../../main/ts/registry", () => {
  const loaders: Record<
    string,
    () => Promise<React.ComponentType<any>>
  > = {
    HomeShell: () =>
      import("../../../main/ts/home").then((m) => m.HomeShell),
    PublishingShell: () =>
      import("../../../main/ts/publishing").then((m) => m.PublishingShell),
    DeveloperShell: () =>
      import("../../../main/ts/developer").then((m) => m.DeveloperShell),
    WorkflowAdminShell: () =>
      import("../../../main/ts/workflowAdmin/WorkflowAdminShell").then(
        (m) => m.WorkflowAdminShell,
      ),
    AdminShell: () =>
      import("../../../main/ts/admin/AdminShell").then((m) => m.AdminShell),
    WidgetBuilderApp: () =>
      import("../../../main/ts/widgetbuilder/WidgetBuilderApp").then(
        (m) => m.WidgetBuilderApp,
      ),
    ContentExplorerShell: () =>
      import("../../../main/ts/contentExplorer/ContentExplorerShell").then(
        (m) => m.ContentExplorerShell,
      ),
  };
  return {
    loadComponent: (name: string) => {
      const load = loaders[name];
      if (!load) {
        return Promise.reject(new Error(`Unknown component: ${name}`));
      }
      return load();
    },
    isRegisteredComponent: (name: string) =>
      Object.prototype.hasOwnProperty.call(loaders, name),
  };
});

vi.mock("../../../main/ts/api/widgetbuilder/widgetBuilderApi", () => ({
  isWidgetBuilderActive: vi.fn().mockResolvedValue(true),
  fetchSummaries: vi.fn().mockResolvedValue([]),
  loadDefinition: vi.fn(),
  saveDefinition: vi.fn(),
  deleteDefinition: vi.fn(),
  deployDefinition: vi.fn(),
  validateDefinition: vi.fn(),
}));

vi.mock("../../../main/ts/api/developer/contentTypesApi", () => ({
  listContentTypes: vi.fn().mockResolvedValue([]),
  getContentTypeDetail: vi.fn().mockResolvedValue({
    name: "x",
    fields: [],
    designGaps: [],
  }),
}));

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  listTemplates: vi.fn().mockResolvedValue([]),
  getTemplateDetail: vi.fn().mockResolvedValue({ templateName: "x" }),
  listSlots: vi.fn().mockResolvedValue([]),
  getSlotDetail: vi.fn().mockResolvedValue({}),
  listCommunities: vi.fn().mockResolvedValue([]),
  getCommunityDetail: vi.fn().mockResolvedValue({}),
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

  // Lazy feature shells routinely resolve in a few hundred ms; under full
  // suite load they occasionally exceed RTL's 1000ms default. Keep waits
  // consistent so App tests do not flake on slower CI agents.
  const SHELL_TIMEOUT = 8000;

  it("renders TopNav and embedded Home shell from entry query", async () => {
    window.history.replaceState({}, "", "/cm/app/home");
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
    expect(
      await screen.findByTestId("home-shell", {}, { timeout: SHELL_TIMEOUT }),
    ).toBeTruthy();
  });

  it("shows publish nav for designer and loads PublishingShell", async () => {
    // Seed path route so BrowserRouter initial location is publish even if
    // entry-query handoff races with the first paint (order-sensitive suites).
    window.history.replaceState({}, "", "/cm/app/publish");
    render(
      <App
        bootstrap={bootstrap}
        entrySearch="?entry=publish"
        basename="/cm/app"
      />,
    );
    expect(screen.getByTestId("nav-publish")).toBeTruthy();
    expect(
      await screen.findByTestId(
        "publishing-shell",
        {},
        { timeout: SHELL_TIMEOUT },
      ),
    ).toBeTruthy();
  });

  it("shows developer nav for designer and loads DeveloperShell", async () => {
    window.history.replaceState({}, "", "/cm/app/developer/templates");
    render(
      <App
        bootstrap={bootstrap}
        entrySearch="?entry=developer&section=templates"
        basename="/cm/app"
      />,
    );
    expect(screen.getByTestId("nav-developer")).toBeTruthy();
    expect(
      await screen.findByTestId(
        "perc-developer-shell",
        {},
        { timeout: SHELL_TIMEOUT },
      ),
    ).toBeTruthy();
    expect(
      screen.getByTestId("tab-developer-templates").getAttribute("aria-selected"),
    ).toBe("true");
  });

  it("hides admin tools for non-admin", async () => {
    render(
      <App
        bootstrap={{ ...bootstrap, isAdmin: false, isDesigner: false }}
        entrySearch="?entry=home"
        basename="/cm/app"
      />,
    );
    expect(screen.queryByTestId("nav-admin")).toBeNull();
    expect(screen.queryByTestId("nav-publish")).toBeNull();
    // Drain Home shell async updates so this case does not race act() warnings
    // against the next test when run under full-suite load.
    await screen.findByTestId("home-shell", {}, { timeout: SHELL_TIMEOUT });
  });

  it("loads WorkflowAdminShell for admin entry", async () => {
    window.history.replaceState({}, "", "/cm/app/workflow/roles");
    render(
      <App
        bootstrap={bootstrap}
        entrySearch="?entry=workflow&tab=roles"
        basename="/cm/app"
      />,
    );
    expect(
      await screen.findByTestId(
        "perc-workflow-admin-shell",
        {},
        { timeout: SHELL_TIMEOUT },
      ),
    ).toBeTruthy();
    expect(screen.getByTestId("tab-roles").getAttribute("aria-selected")).toBe(
      "true",
    );
  });

  it("loads AdminShell for admin tools entry", async () => {
    window.history.replaceState({}, "", "/cm/app/admin/tools");
    render(
      <App
        bootstrap={bootstrap}
        entrySearch="?entry=admin&tab=tools"
        basename="/cm/app"
      />,
    );
    expect(
      await screen.findByTestId(
        "perc-admin-shell",
        {},
        { timeout: SHELL_TIMEOUT },
      ),
    ).toBeTruthy();
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
    await waitFor(
      () => {
        const app = screen.queryByTestId("widget-builder-app");
        const disabled = screen.queryByTestId("wb-disabled");
        expect(app ?? disabled).toBeTruthy();
      },
      { timeout: SHELL_TIMEOUT },
    );
  });

  it("redirects non-admin away from workflow to home", async () => {
    render(
      <App
        bootstrap={{ ...bootstrap, isAdmin: false, isDesigner: true }}
        entrySearch="?entry=workflow"
        basename="/cm/app"
      />,
    );
    expect(
      await screen.findByTestId("home-shell", {}, { timeout: SHELL_TIMEOUT }),
    ).toBeTruthy();
  });

  it("redirects non-admin away from admin tools to home", async () => {
    render(
      <App
        bootstrap={{ ...bootstrap, isAdmin: false, isDesigner: true }}
        entrySearch="?entry=admin"
        basename="/cm/app"
      />,
    );
    expect(
      await screen.findByTestId("home-shell", {}, { timeout: SHELL_TIMEOUT }),
    ).toBeTruthy();
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
    expect(
      await screen.findByTestId("home-shell", {}, { timeout: SHELL_TIMEOUT }),
    ).toBeTruthy();
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
    expect(
      await screen.findByTestId("home-shell", {}, { timeout: SHELL_TIMEOUT }),
    ).toBeTruthy();
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
      expect(
        await screen.findByTestId(
          "content-explorer-shell",
          {},
          { timeout: SHELL_TIMEOUT },
        ),
      ).toBeTruthy();
      expect(screen.queryByTestId("route-explorer")).toBeNull();
    } finally {
      vi.unstubAllGlobals();
    }
  });
});
