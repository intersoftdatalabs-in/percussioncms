/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AboutDialog } from "../../../../main/ts/app/layout/AboutDialog";
import { fetchAbout } from "../../../../main/ts/api/about/aboutApi";

vi.mock("../../../../main/ts/api/about/aboutApi", () => ({
  fetchAbout: vi.fn(),
}));

const fetchAboutMock = vi.mocked(fetchAbout);

describe("AboutDialog", () => {
  beforeEach(() => {
    fetchAboutMock.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("shows a loading state before the fetch resolves", () => {
    fetchAboutMock.mockReturnValue(new Promise(() => {}));
    render(<AboutDialog onClose={() => {}} />);
    expect(screen.getByTestId("perc-about-dialog-loading")).toBeTruthy();
  });

  it("renders version, copyright, and third-party disclaimer once loaded", async () => {
    fetchAboutMock.mockResolvedValue({
      productName: "Percussion CMS",
      versionString: "Version 8.2.0 Build 20260731 (1)",
      copyright: "Percussion CMS Copyright (C) Percussion Software, Inc.  1999-2026",
      thirdPartyCopyright: "This product includes software developed by The Apache Software Foundation...",
    });

    render(<AboutDialog onClose={() => {}} />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-about-dialog-content")).toBeTruthy();
    });

    expect(screen.getByTestId("perc-about-dialog-version").textContent).toContain(
      "Version 8.2.0",
    );
    expect(screen.getByTestId("perc-about-dialog-copyright").textContent).toContain(
      "Percussion Software, Inc.",
    );
    expect(screen.getByTestId("perc-about-dialog-third-party").textContent).toContain(
      "Apache Software Foundation",
    );
  });

  it("shows an error message when the fetch fails", async () => {
    fetchAboutMock.mockRejectedValue({ status: 500, statusText: "Error" });

    render(<AboutDialog onClose={() => {}} />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-about-dialog-error")).toBeTruthy();
    });
  });

  it("calls onClose when the Close button is clicked", async () => {
    fetchAboutMock.mockResolvedValue({
      productName: "Percussion CMS",
      versionString: "Version 8.2.0",
      copyright: "Copyright",
      thirdPartyCopyright: "Third party",
    });
    const onClose = vi.fn();

    render(<AboutDialog onClose={onClose} />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-about-dialog-content")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("perc-about-dialog-close"));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("calls onClose when the Escape key is pressed", async () => {
    fetchAboutMock.mockResolvedValue({
      productName: "Percussion CMS",
      versionString: "Version 8.2.0",
      copyright: "Copyright",
      thirdPartyCopyright: "Third party",
    });
    const onClose = vi.fn();

    render(<AboutDialog onClose={onClose} />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-about-dialog-content")).toBeTruthy();
    });

    fireEvent.keyDown(screen.getByTestId("perc-about-dialog-overlay"), {
      key: "Escape",
    });
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
