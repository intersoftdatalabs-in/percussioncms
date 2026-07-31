/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppLayout } from "../../../../main/ts/app/layout/AppLayout";
import { fetchAbout } from "../../../../main/ts/api/about/aboutApi";

vi.mock("../../../../main/ts/api/about/aboutApi", () => ({
  fetchAbout: vi.fn(),
}));

const fetchAboutMock = vi.mocked(fetchAbout);

function renderAppLayout() {
  return render(
    <MemoryRouter initialEntries={["/"]}>
      <Routes>
        <Route path="/" element={<AppLayout />}>
          <Route index element={<div data-testid="perc-outlet-child" />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe("AppLayout About dialog", () => {
  beforeEach(() => {
    fetchAboutMock.mockReset();
    fetchAboutMock.mockResolvedValue({
      productName: "Percussion CMS",
      versionString: "Version 8.2.0 Build 20260731 (1)",
      copyright: "Percussion CMS Copyright (C) Percussion Software, Inc.  1999-2026",
      thirdPartyCopyright: "This product includes software developed by...",
    });
  });

  afterEach(() => {
    cleanup();
  });

  it("does not render the About dialog until the footer link is clicked", () => {
    renderAppLayout();
    expect(screen.queryByTestId("perc-about-dialog")).toBeNull();
    expect(screen.getByTestId("perc-brand-footer-about-link")).toBeTruthy();
  });

  it("opens the About dialog when the footer link is clicked and closes on Close", async () => {
    renderAppLayout();

    fireEvent.click(screen.getByTestId("perc-brand-footer-about-link"));

    await waitFor(() => {
      expect(screen.getByTestId("perc-about-dialog")).toBeTruthy();
    });
    expect(fetchAboutMock).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByTestId("perc-about-dialog-close"));
    expect(screen.queryByTestId("perc-about-dialog")).toBeNull();
  });
});
