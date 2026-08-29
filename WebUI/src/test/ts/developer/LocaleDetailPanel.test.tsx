/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as localesApi from "../../../main/ts/api/developer/localesApi";
import { LocaleDetailPanel } from "../../../main/ts/developer/LocaleDetailPanel";
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

const getLocaleDetail = localesApi.getLocaleDetail as ReturnType<typeof vi.fn>;
const createLocale = localesApi.createLocale as ReturnType<typeof vi.fn>;
const updateLocale = localesApi.updateLocale as ReturnType<typeof vi.fn>;
const deleteLocale = localesApi.deleteLocale as ReturnType<typeof vi.fn>;

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
    createLocale.mockReset();
    updateLocale.mockReset();
    deleteLocale.mockReset();
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

  it("shows 404 missing locale via panelErrMsg", async () => {
    getLocaleDetail.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Locale not found" },
    });
    render(<LocaleDetailPanel idOrLang="xx-missing" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-loc-detail-error").textContent).toContain(
      DEV_MSG.LOC_DETAIL_ERROR,
    );
    expect(screen.getByTestId("developer-loc-detail-error").textContent).toContain(
      "Locale not found",
    );
    expect(screen.queryByTestId("developer-loc-save")).toBeNull();
  });

  it("shows 404 status when missing locale has no body message", async () => {
    getLocaleDetail.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    render(<LocaleDetailPanel idOrLang="xx-missing" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-loc-detail-error").textContent).toBe(
      `${DEV_MSG.LOC_DETAIL_ERROR} (404)`,
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

  it("disables save until language and label are valid on create", () => {
    render(<LocaleDetailPanel idOrLang={null} onBack={() => undefined} />);
    const save = screen.getByTestId("developer-loc-save") as HTMLButtonElement;
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-loc-language"), {
      target: { value: "fr-ca" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-loc-label"), {
      target: { value: "French Canada" },
    });
    expect(save.disabled).toBe(false);
  });

  it("keeps language read-only on edit", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-language")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-loc-save") as HTMLButtonElement).disabled).toBe(
      false,
    );
  });

  it("surfaces 409 duplicate language on create", async () => {
    createLocale.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Locale already exists: en-us" },
    });
    const onSaved = vi.fn();
    render(<LocaleDetailPanel idOrLang={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-loc-language"), {
      target: { value: "en-us" },
    });
    fireEvent.change(screen.getByTestId("developer-loc-label"), {
      target: { value: "English" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-detail-error")).toBeTruthy();
    });
    expect(createLocale).toHaveBeenCalled();
    expect(screen.getByTestId("developer-loc-detail-error").textContent).toContain(
      DEV_MSG.LOC_DUPLICATE,
    );
    expect(screen.getByTestId("developer-loc-detail-error").textContent).toContain(
      "Locale already exists",
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("does not POST create twice when save is clicked twice", async () => {
    let resolveCreate: (value: typeof sampleDetail) => void = () => undefined;
    createLocale.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    render(<LocaleDetailPanel idOrLang={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-loc-language"), {
      target: { value: "fr-ca" },
    });
    fireEvent.change(screen.getByTestId("developer-loc-label"), {
      target: { value: "French Canada" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-save"));
    fireEvent.click(screen.getByTestId("developer-loc-save"));
    expect(createLocale).toHaveBeenCalledTimes(1);
    resolveCreate({
      id: 99,
      languageString: "fr-ca",
      label: "French Canada",
      status: "active",
      baseLocale: false,
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-editor-notice")).toBeTruthy();
    });
  });

  it("creates a locale when language and label are valid", async () => {
    createLocale.mockResolvedValue({
      id: 99,
      languageString: "fr-ca",
      label: "French Canada",
      status: "active",
      baseLocale: false,
    });
    const onSaved = vi.fn();
    render(<LocaleDetailPanel idOrLang={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-loc-language"), {
      target: { value: "FR_CA" },
    });
    fireEvent.change(screen.getByTestId("developer-loc-label"), {
      target: { value: "French Canada" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(createLocale).toHaveBeenCalledWith(
      expect.objectContaining({
        languageString: "fr-ca",
        label: "French Canada",
      }),
    );
    expect(screen.getByTestId("developer-loc-editor-notice").textContent).toBe(
      DEV_MSG.LOC_SAVED,
    );
  });

  it("saves label changes on an existing locale", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      label: "US English",
    });
    const onSaved = vi.fn();
    render(
      <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-label")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-loc-label"), {
      target: { value: "US English" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateLocale).toHaveBeenCalledWith(
      "en-us",
      expect.objectContaining({ label: "US English", languageString: "en-us" }),
    );
  });

  it("deletes after confirm and omits delete chrome in create mode", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    deleteLocale.mockResolvedValue(undefined);
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    try {
      const onDeleted = vi.fn();
      render(
        <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onDeleted={onDeleted} />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("developer-loc-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-loc-delete"));
      await waitFor(() => {
        expect(onDeleted).toHaveBeenCalled();
      });
      expect(deleteLocale).toHaveBeenCalledWith("en-us");
    } finally {
      confirmSpy.mockRestore();
    }
  });

  it("does not show delete on create", () => {
    render(<LocaleDetailPanel idOrLang={null} onBack={() => undefined} />);
    expect(screen.queryByTestId("developer-loc-delete")).toBeNull();
  });
});
