/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

vi.mock("../../../main/ts/api/developer/contentTypesApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/contentTypesApi")>();
  return {
    ...actual,
    listContentTypes: vi.fn().mockResolvedValue([]),
    getContentTypeDetail: vi.fn().mockResolvedValue({
      name: "x",
      fields: [],
      designGaps: [],
    }),
    lockContentType: vi.fn(),
    unlockContentType: vi.fn(),
    createContentType: vi.fn(),
    deleteContentType: vi.fn(),
    getContentTypeItemExits: vi.fn().mockResolvedValue({
      inputTranslations: [],
      outputTranslations: [],
      validations: [],
      preExits: [],
      postExits: [],
    }),
    replaceContentTypeItemExits: vi.fn().mockImplementation(async (_id, body) => body),
  };
});

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  listTemplates: vi.fn().mockResolvedValue([]),
  getTemplateDetail: vi.fn().mockResolvedValue({ templateName: "x" }),
  createTemplate: vi.fn(),
  deleteTemplate: vi.fn(),
  updateTemplateDetail: vi.fn(),
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
  allowExternalAvatarFetch: true,
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
    // Intentional: non-admin home still mounts HomeShell; awaiting it drains
    // lazy-load + RecentSection updates so the next test does not see act()
    // races under full-suite load (not only a nav-absence assertion).
    await screen.findByTestId("home-shell", {}, { timeout: SHELL_TIMEOUT });
  });

  it("top nav order is Home then Explorer with single Admin (#2702)", async () => {
    window.history.replaceState({}, "", "/cm/app/home");
    render(
      <App bootstrap={bootstrap} entrySearch="?entry=home" basename="/cm/app" />,
    );
    const nav = screen.getByTestId("perc-spa-topnav");
    const home = screen.getByTestId("nav-home");
    const explorer = screen.getByTestId("nav-explorer");
    expect(screen.queryByTestId("nav-dashboard")).toBeNull();
    expect(screen.queryByTestId("nav-workflow")).toBeNull();
    expect(screen.getByTestId("nav-admin")).toBeTruthy();
    // Document order: Home immediately before Explorer
    expect(
      home.compareDocumentPosition(explorer) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
    // No other top-nav link between them
    let sibling = home.parentElement?.nextElementSibling;
    expect(sibling?.querySelector("[data-testid='nav-explorer']")).toBeTruthy();
    await screen.findByTestId("home-shell", {}, { timeout: SHELL_TIMEOUT });
    expect(nav).toBeTruthy();
  });

  it("folds workflow entry into unified AdminShell (#3088)", async () => {
    // entry=workflow&tab=roles handoff lands on /admin/roles
    window.history.replaceState({}, "", "/cm/app/admin/roles");
    render(
      <App
        bootstrap={bootstrap}
        entrySearch="?entry=workflow&tab=roles"
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
    expect(screen.getByTestId("tab-roles").getAttribute("aria-selected")).toBe(
      "true",
    );
    // No sibling product chrome
    expect(screen.queryByTestId("perc-workflow-admin-shell")).toBeNull();
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
