/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../../../main/ts/app/App";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";

vi.mock("../../../main/ts/api/home/homeApi", () => ({
  fetchRecentItems: vi.fn().mockResolvedValue([]),
  fetchMyContent: vi.fn().mockResolvedValue([]),
  fetchSites: vi.fn().mockResolvedValue([]),
  fetchFolderChildren: vi.fn().mockResolvedValue([]),
  searchContent: vi.fn().mockResolvedValue([]),
  createPage: vi.fn().mockResolvedValue({}),
}));

vi.mock("../../../main/ts/api/publishing", () => ({
  // Publishing sections load their own APIs; keep module resolution soft
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
  });

  afterEach(() => {
    cleanup();
  });

  it("renders TopNav and embedded Home shell from entry query", async () => {
    render(<App bootstrap={bootstrap} entrySearch="?entry=home" />);
    expect(screen.getByTestId("perc-spa-topnav")).toBeTruthy();
    expect(screen.getByTestId("perc-spa-user-name").textContent).toContain(
      "demo",
    );
    await waitFor(() => {
      expect(screen.getByTestId("home-shell")).toBeTruthy();
    });
  });

  it("shows publish nav for designer and loads PublishingShell", async () => {
    render(<App bootstrap={bootstrap} entrySearch="?entry=publish" />);
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
      />,
    );
    expect(screen.queryByTestId("nav-admin")).toBeNull();
    expect(screen.queryByTestId("nav-publish")).toBeNull();
  });
});
