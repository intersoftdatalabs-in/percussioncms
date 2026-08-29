/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import { getLocaleDetail, listLocales } from "../../../main/ts/api/developer/localesApi";
import { LocalesPanel } from "../../../main/ts/developer/LocalesPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/localesApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/localesApi")
  >();
  return {
    ...actual,
    listLocales: vi.fn(),
    getLocaleDetail: vi.fn(),
    createLocale: vi.fn(),
    updateLocale: vi.fn(),
    deleteLocale: vi.fn(),
  };
});

const listMock = vi.mocked(listLocales);
const detailMock = vi.mocked(getLocaleDetail);

describe("LocalesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
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
    expect(screen.getByTestId("developer-loc-new")).toBeTruthy();
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
    expect(screen.getByTestId("developer-loc-save")).toBeTruthy();
    expect(screen.getByTestId("developer-loc-delete")).toBeTruthy();
  });

  it("opens create chrome from New locale", async () => {
    listMock.mockResolvedValue([]);
    render(<LocalesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-empty")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-new"));
    expect(screen.getByTestId("developer-loc-detail")).toBeTruthy();
    expect(screen.getByTestId("developer-loc-save")).toBeDisabled();
    expect(detailMock).not.toHaveBeenCalled();
  });

  it("shows empty state when API returns no locales", async () => {
    listMock.mockResolvedValue([]);
    render(<LocalesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-loc-new")).toBeTruthy();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listMock.mockRejectedValue(new SessionRedirectError());
    render(<LocalesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-loc-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-loc-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listMock.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<LocalesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-loc-error").textContent).toBe(
      `${DEV_MSG.LOC_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    listMock.mockRejectedValue(new Error("network down"));
    render(<LocalesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-loc-error").textContent).toBe(
      `${DEV_MSG.LOC_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-loc-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listMock.mockRejectedValue("boom");
    render(<LocalesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-loc-error").textContent).toBe(DEV_MSG.LOC_ERROR);
  });
});
