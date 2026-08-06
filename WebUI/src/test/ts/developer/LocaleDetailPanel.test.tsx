/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as localesApi from "../../../main/ts/api/developer/localesApi";
import { LocaleDetailPanel } from "../../../main/ts/developer/LocaleDetailPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/localesApi", () => ({
  listLocales: vi.fn(),
  getLocaleDetail: vi.fn(),
}));

const getLocaleDetail = localesApi.getLocaleDetail as ReturnType<typeof vi.fn>;

const sampleDetail = {
  id: 1,
  languageString: "en-us",
  label: "English",
  status: "active",
  baseLocale: false,
  hasFormatProfile: true,
  description: "US English",
  format: {
    languageString: "en-us",
    textDir: "ltr",
    datePattern: "MM/dd/yyyy",
    currencyCode: "USD",
  },
  designGaps: ["write not supported"],
};

describe("LocaleDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getLocaleDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<LocaleDetailPanel idOrLang="en-us" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-loc-detail-title").textContent).toContain("English");
    expect(screen.getByTestId("developer-loc-format-grid")).toBeTruthy();
    expect(screen.getByText("MM/dd/yyyy")).toBeTruthy();
    expect(screen.getByTestId("developer-loc-gaps")).toBeTruthy();
    expect(getLocaleDetail).toHaveBeenCalledWith("en-us");
    fireEvent.click(screen.getByTestId("developer-loc-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty format section when detail has no format", async () => {
    getLocaleDetail.mockResolvedValue({
      ...sampleDetail,
      hasFormatProfile: false,
      format: undefined,
      designGaps: [],
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-format-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-loc-format-empty").textContent).toBe(
      DEV_MSG.LOC_FORMAT_EMPTY,
    );
    expect(screen.queryByTestId("developer-loc-format-grid")).toBeNull();
    expect(screen.queryByTestId("developer-loc-gaps")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getLocaleDetail.mockRejectedValue(new SessionRedirectError());
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-loc-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-loc-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-loc-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getLocaleDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-loc-detail-error").textContent).toBe(
      `${DEV_MSG.LOC_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getLocaleDetail.mockRejectedValue(new Error("network down"));
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-loc-detail-error").textContent).toBe(
      `${DEV_MSG.LOC_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-loc-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getLocaleDetail.mockRejectedValue("boom");
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-loc-detail-error").textContent).toBe(
      DEV_MSG.LOC_DETAIL_ERROR,
    );
  });
});
