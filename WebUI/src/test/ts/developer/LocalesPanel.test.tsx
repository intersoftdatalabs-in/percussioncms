/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { getLocaleDetail, listLocales } from "../../../main/ts/api/developer/localesApi";
import { LocalesPanel } from "../../../main/ts/developer/LocalesPanel";

vi.mock("../../../main/ts/api/developer/localesApi", () => ({
  listLocales: vi.fn(),
  getLocaleDetail: vi.fn(),
}));

const listMock = vi.mocked(listLocales);
const detailMock = vi.mocked(getLocaleDetail);

describe("LocalesPanel", () => {
  beforeEach(() => {
    listMock.mockReset();
    detailMock.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("renders catalog with base and format flags", async () => {
    listMock.mockResolvedValue([
      {
        id: 1,
        languageString: "en-us",
        label: "English",
        status: "active",
        baseLocale: false,
        hasFormatProfile: true,
      },
      {
        id: 2,
        languageString: "ar",
        label: "Arabic",
        status: "active",
        baseLocale: true,
        hasFormatProfile: false,
      },
    ]);
    render(<LocalesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-table")).toBeTruthy();
    });
    expect(screen.getByText("en-us")).toBeTruthy();
    expect(screen.getByText("ar")).toBeTruthy();
  });

  it("opens detail with format profile", async () => {
    listMock.mockResolvedValue([
      {
        languageString: "en-us",
        label: "English",
        status: "active",
        baseLocale: false,
        hasFormatProfile: true,
      },
    ]);
    detailMock.mockResolvedValue({
      languageString: "en-us",
      label: "English",
      status: "active",
      baseLocale: false,
      hasFormatProfile: true,
      format: {
        languageString: "en-us",
        textDir: "ltr",
        datePattern: "MM/dd/yyyy",
        currencyCode: "USD",
      },
      designGaps: ["write not supported"],
    });

    render(<LocalesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-detail")).toBeTruthy();
    });
    expect(detailMock).toHaveBeenCalledWith("en-us");
    expect(screen.getByTestId("developer-loc-format-grid")).toBeTruthy();
    expect(screen.getByText("MM/dd/yyyy")).toBeTruthy();
    expect(screen.getByTestId("developer-loc-gaps")).toBeTruthy();
  });

  it("shows error UI when list fails", async () => {
    listMock.mockRejectedValue({ status: 500, statusText: "Error" });
    render(<LocalesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-error")).toBeTruthy();
    });
  });
});
