/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import { App } from "../../../main/ts/app/App";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";

const bootstrap: SpaBootstrap = {
  userName: "demo",
  locale: "en-us",
  entry: "home",
  isAdmin: true,
  isDesigner: true,
  isWidgetBuilderActive: true,
};

describe("App shell", () => {
  afterEach(() => {
    cleanup();
  });

  it("renders TopNav and home placeholder from entry query", async () => {
    render(<App bootstrap={bootstrap} entrySearch="?entry=home" />);
    expect(screen.getByTestId("perc-spa-topnav")).toBeTruthy();
    expect(screen.getByTestId("perc-spa-user-name").textContent).toContain(
      "demo",
    );
    await waitFor(() => {
      expect(screen.getByTestId("route-home-title").textContent).toMatch(/Home/i);
    });
  });

  it("shows publish nav for designer", () => {
    render(<App bootstrap={bootstrap} entrySearch="?entry=publish" />);
    expect(screen.getByTestId("nav-publish")).toBeTruthy();
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
