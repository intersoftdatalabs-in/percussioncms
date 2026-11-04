/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as localesApi from "../../../main/ts/api/developer/localesApi";
import { LocaleDetailPanel } from "../../../main/ts/developer/LocaleDetailPanel";
import { LOC_DATE_MSG } from "../../../main/ts/developer/localeDatePatternMessages";
import { LOC_DESC_MSG } from "../../../main/ts/developer/localeDescriptionMessages";
import { LOC_LABEL_MSG } from "../../../main/ts/developer/localeLabelMessages";
import { LOC_DATETIME_MSG } from "../../../main/ts/developer/localeDateTimePatternMessages";
import { LOC_DECIMAL_MSG } from "../../../main/ts/developer/localeDecimalSepMessages";
import { LOC_CURRENCY_MSG } from "../../../main/ts/developer/localeCurrencyCodeMessages";
import { LOC_STATUS_MSG } from "../../../main/ts/developer/localeEnabledStatusMessages";
import { LOC_TEXT_DIR_MSG } from "../../../main/ts/developer/localeTextDirMessages";
import { LOC_GROUPING_MSG } from "../../../main/ts/developer/localeGroupingSepMessages";
import { LOC_TIME_MSG } from "../../../main/ts/developer/localeTimePatternMessages";
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
    timePattern: "HH:mm:ss",
    dateTimePattern: "MM/dd/yyyy HH:mm:ss",
    decimalSep: ".",
    groupingSep: ",",
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
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
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
      DEV_MSG.LOC_FORMAT_CLEAR_HINT,
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
    expect(createLocale.mock.calls[0][0].hasFormatProfile).toBeUndefined();
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
      expect.objectContaining({
        label: "US English",
        languageString: "en-us",
        hasFormatProfile: true,
        format: expect.objectContaining({ datePattern: "MM/dd/yyyy", currencyCode: "USD" }),
      }),
    );
  });

  it("saves format profile fields on the locale PUT", async () => {
    getLocaleDetail.mockResolvedValue({
      ...sampleDetail,
      hasFormatProfile: false,
      format: undefined,
    });
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      hasFormatProfile: true,
      format: { languageString: "en-us", datePattern: "yyyy-MM-dd", textDir: "ltr" },
    });
    const onSaved = vi.fn();
    render(
      <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-format-enable")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-format-enable"));
    fireEvent.change(screen.getByTestId("developer-loc-fmt-date"), {
      target: { value: "yyyy-MM-dd" },
    });
    fireEvent.change(screen.getByTestId("developer-loc-fmt-dir"), {
      target: { value: "ltr" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateLocale).toHaveBeenCalledWith(
      "en-us",
      expect.objectContaining({
        hasFormatProfile: true,
        format: expect.objectContaining({ datePattern: "yyyy-MM-dd", textDir: "ltr" }),
      }),
    );
    expect(
      screen.getByTestId("developer-loc-set-text-dir-text").getAttribute("data-loc-text-dir"),
    ).toBe("ltr");
  });

  it("clears format profile when store-format is unchecked", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      hasFormatProfile: false,
      format: undefined,
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-format-enable")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-format-enable"));
    fireEvent.click(screen.getByTestId("developer-loc-save"));
    await waitFor(() => {
      expect(updateLocale).toHaveBeenCalled();
    });
    expect(updateLocale).toHaveBeenCalledWith(
      "en-us",
      expect.objectContaining({ hasFormatProfile: false }),
    );
    const body = updateLocale.mock.calls[0][1] as { format?: unknown };
    expect(body.format).toBeUndefined();
  });

  it("deletes after confirm and omits delete chrome in create mode", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    deleteLocale.mockResolvedValue(undefined);
    const onDeleted = vi.fn();
    render(
      <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onDeleted={onDeleted} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(onDeleted).toHaveBeenCalled();
    });
    expect(deleteLocale).toHaveBeenCalledWith("en-us");
  });

  it("does not show delete on create", () => {
    render(<LocaleDetailPanel idOrLang={null} onBack={() => undefined} />);
    expect(screen.queryByTestId("developer-loc-delete")).toBeNull();
    expect(screen.queryByTestId("developer-loc-set-description")).toBeNull();
    expect(screen.queryByTestId("developer-loc-set-label")).toBeNull();
    expect(screen.queryByTestId("developer-loc-set-date-pattern")).toBeNull();
    expect(screen.queryByTestId("developer-loc-set-time-pattern")).toBeNull();
  });

  it("sets the description only after success and leaves language, label, status, and format", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({ ...sampleDetail, description: "note" });
    const onSaved = vi.fn();
    render(
      <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("developer-loc-set-description-text").getAttribute("data-loc-description"),
      ).toBe("US English");
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-description-edit"));
    fireEvent.click(screen.getByTestId("developer-loc-set-description-save"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-loc-set-description-editor")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-loc-set-description-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-description-input"), {
      target: { value: " note " },
    });
    expect(
      screen.getByTestId("developer-loc-set-description-text").getAttribute("data-loc-description"),
    ).toBe("US English");
    fireEvent.click(screen.getByTestId("developer-loc-set-description-cancel"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(
      screen.getByTestId("developer-loc-set-description-text").getAttribute("data-loc-description"),
    ).toBe("US English");

    fireEvent.click(screen.getByTestId("developer-loc-set-description-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-description-input"), {
      target: { value: " note " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-description-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", { description: "note" });
    const sent = updateLocale.mock.calls.at(-1)?.[1] as Record<string, unknown>;
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("status");
    expect(sent).not.toHaveProperty("format");
    expect(
      screen.getByTestId("developer-loc-set-description-text").getAttribute("data-loc-description"),
    ).toBe("note");
    expect((screen.getByTestId("developer-loc-description") as HTMLInputElement).value).toBe("note");
    expect(screen.getByTestId("developer-loc-set-description-notice").textContent).toBe(
      LOC_DESC_MSG.SAVED,
    );
    expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-status") as HTMLSelectElement).value).toBe("active");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
  });

  it("clears a blank description and keeps the previous description on 400, 403, and 409", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({ ...sampleDetail, description: "" });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-description-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-description-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-description-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-description-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-description-notice").textContent).toBe(
        LOC_DESC_MSG.CLEARED,
      );
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", { description: "" });
    expect(
      screen.getByTestId("developer-loc-set-description-text").getAttribute("data-loc-description"),
    ).toBe("");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );

    for (const status of [400, 403, 409]) {
      updateLocale.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message: `forced ${status}` },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-description-edit"));
      fireEvent.change(screen.getByTestId("developer-loc-set-description-input"), {
        target: { value: "later" },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-description-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-loc-set-description-error").textContent).toContain(
          `forced ${status}`,
        );
      });
      expect(
        screen.getByTestId("developer-loc-set-description-text").getAttribute("data-loc-description"),
      ).toBe("");
      expect((screen.getByTestId("developer-loc-description") as HTMLInputElement).value).toBe("");
      expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
      fireEvent.click(screen.getByTestId("developer-loc-set-description-cancel"));
    }
  });

  it("does not show a description when the update changes the label or format", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      description: "note",
      label: "Renamed",
      format: { ...sampleDetail.format, datePattern: "yyyy-MM-dd" },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-description-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-description-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-description-input"), {
      target: { value: "note" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-description-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-description-error").textContent).toContain(
        LOC_DESC_MSG.ERROR,
      );
    });
    expect(
      screen.getByTestId("developer-loc-set-description-text").getAttribute("data-loc-description"),
    ).toBe("US English");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
  });

  it("sets the label only after success and leaves language, description, status, and format", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({ ...sampleDetail, label: "US English" });
    const onSaved = vi.fn();
    render(
      <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-label-text").getAttribute("data-loc-label")).toBe(
        "English",
      );
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-label-edit"));
    fireEvent.click(screen.getByTestId("developer-loc-set-label-save"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-loc-set-label-editor")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-loc-set-label-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-label-input"), {
      target: { value: " US English " },
    });
    expect(screen.getByTestId("developer-loc-set-label-text").getAttribute("data-loc-label")).toBe(
      "English",
    );
    fireEvent.click(screen.getByTestId("developer-loc-set-label-cancel"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-loc-set-label-text").getAttribute("data-loc-label")).toBe(
      "English",
    );

    fireEvent.click(screen.getByTestId("developer-loc-set-label-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-label-input"), {
      target: { value: " US English " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-label-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", { label: "US English" });
    const sent = updateLocale.mock.calls.at(-1)?.[1] as Record<string, unknown>;
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("status");
    expect(sent).not.toHaveProperty("format");
    expect(screen.getByTestId("developer-loc-set-label-text").getAttribute("data-loc-label")).toBe(
      "US English",
    );
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("US English");
    expect(screen.getByTestId("developer-loc-set-label-notice").textContent).toBe(
      LOC_LABEL_MSG.SAVED,
    );
    expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
    expect((screen.getByTestId("developer-loc-description") as HTMLInputElement).value).toBe(
      "US English",
    );
    expect((screen.getByTestId("developer-loc-status") as HTMLSelectElement).value).toBe("active");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
  });

  it("does not write a blank label and keeps the previous label on 400, 403, and 409", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-label-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-label-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-label-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-label-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-label-error").textContent).toBe(
        LOC_LABEL_MSG.BLANK,
      );
    });
    expect(updateLocale).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-loc-set-label-text").getAttribute("data-loc-label")).toBe(
      "English",
    );

    for (const status of [400, 403, 409]) {
      updateLocale.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message: `forced ${status}` },
      });
      fireEvent.change(screen.getByTestId("developer-loc-set-label-input"), {
        target: { value: "later" },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-label-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-loc-set-label-error").textContent).toContain(
          `forced ${status}`,
        );
      });
      expect(screen.getByTestId("developer-loc-set-label-text").getAttribute("data-loc-label")).toBe(
        "English",
      );
      expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
      expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
    }
  });

  it("does not show a label when the update changes the description or format", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      label: "US English",
      description: "changed",
      format: { ...sampleDetail.format, datePattern: "yyyy-MM-dd" },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-label-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-label-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-label-input"), {
      target: { value: "US English" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-label-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-label-error").textContent).toContain(
        LOC_LABEL_MSG.ERROR,
      );
    });
    expect(screen.getByTestId("developer-loc-set-label-text").getAttribute("data-loc-label")).toBe(
      "English",
    );
    expect((screen.getByTestId("developer-loc-description") as HTMLInputElement).value).toBe(
      "US English",
    );
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
  });

  it("sets the date pattern only after success and leaves language, label, description, status, and other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, datePattern: "yyyy-MM-dd" },
    });
    const onSaved = vi.fn();
    render(
      <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(
        screen
          .getByTestId("developer-loc-set-date-pattern-text")
          .getAttribute("data-loc-date-pattern"),
      ).toBe("MM/dd/yyyy");
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-date-pattern-edit"));
    fireEvent.click(screen.getByTestId("developer-loc-set-date-pattern-save"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-loc-set-date-pattern-editor")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-loc-set-date-pattern-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-date-pattern-input"), {
      target: { value: " yyyy-MM-dd " },
    });
    expect(
      screen
        .getByTestId("developer-loc-set-date-pattern-text")
        .getAttribute("data-loc-date-pattern"),
    ).toBe("MM/dd/yyyy");
    fireEvent.click(screen.getByTestId("developer-loc-set-date-pattern-cancel"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(
      screen
        .getByTestId("developer-loc-set-date-pattern-text")
        .getAttribute("data-loc-date-pattern"),
    ).toBe("MM/dd/yyyy");

    fireEvent.click(screen.getByTestId("developer-loc-set-date-pattern-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-date-pattern-input"), {
      target: { value: " yyyy-MM-dd " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-date-pattern-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", {
      format: { datePattern: "yyyy-MM-dd" },
    });
    const sent = updateLocale.mock.calls.at(-1)?.[1] as Record<string, unknown>;
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("status");
    expect(sent).not.toHaveProperty("hasFormatProfile");
    expect(
      screen
        .getByTestId("developer-loc-set-date-pattern-text")
        .getAttribute("data-loc-date-pattern"),
    ).toBe("yyyy-MM-dd");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "yyyy-MM-dd",
    );
    expect(screen.getByTestId("developer-loc-set-date-pattern-notice").textContent).toBe(
      LOC_DATE_MSG.SAVED,
    );
    expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-description") as HTMLInputElement).value).toBe(
      "US English",
    );
    expect((screen.getByTestId("developer-loc-status") as HTMLSelectElement).value).toBe("active");
    expect((screen.getByTestId("developer-loc-fmt-dir") as HTMLSelectElement).value).toBe("ltr");
    expect((screen.getByTestId("developer-loc-fmt-currency-code") as HTMLInputElement).value).toBe(
      "USD",
    );
  });

  it("clears a blank date pattern and keeps the previous pattern on 400, 403, and 409", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, datePattern: "" },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-date-pattern-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-date-pattern-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-date-pattern-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-date-pattern-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-date-pattern-notice").textContent).toBe(
        LOC_DATE_MSG.CLEARED,
      );
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", { format: { datePattern: "" } });
    expect(
      screen
        .getByTestId("developer-loc-set-date-pattern-text")
        .getAttribute("data-loc-date-pattern"),
    ).toBe("");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe("");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-dir") as HTMLSelectElement).value).toBe("ltr");

    for (const status of [400, 403, 409]) {
      updateLocale.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message: `forced ${status}` },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-date-pattern-edit"));
      fireEvent.change(screen.getByTestId("developer-loc-set-date-pattern-input"), {
        target: { value: "dd/MM/yyyy" },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-date-pattern-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-loc-set-date-pattern-error").textContent).toContain(
          `forced ${status}`,
        );
      });
      expect(
        screen
          .getByTestId("developer-loc-set-date-pattern-text")
          .getAttribute("data-loc-date-pattern"),
      ).toBe("");
      expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe("");
      expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
      fireEvent.click(screen.getByTestId("developer-loc-set-date-pattern-cancel"));
    }
  });

  it("does not show a date pattern when the update changes the label or other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      label: "Renamed",
      format: { ...sampleDetail.format, datePattern: "yyyy-MM-dd", timePattern: "hh:mm a" },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-date-pattern-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-date-pattern-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-date-pattern-input"), {
      target: { value: "yyyy-MM-dd" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-date-pattern-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-date-pattern-error").textContent).toContain(
        LOC_DATE_MSG.ERROR,
      );
    });
    expect(
      screen
        .getByTestId("developer-loc-set-date-pattern-text")
        .getAttribute("data-loc-date-pattern"),
    ).toBe("MM/dd/yyyy");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
  });

  it("sets the time pattern only after success and leaves language, label, description, status, and other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, timePattern: "hh:mm a" },
    });
    const onSaved = vi.fn();
    render(
      <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(
        screen
          .getByTestId("developer-loc-set-time-pattern-text")
          .getAttribute("data-loc-time-pattern"),
      ).toBe("HH:mm:ss");
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-time-pattern-edit"));
    fireEvent.click(screen.getByTestId("developer-loc-set-time-pattern-save"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-loc-set-time-pattern-editor")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-loc-set-time-pattern-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-time-pattern-input"), {
      target: { value: " hh:mm a " },
    });
    expect(
      screen
        .getByTestId("developer-loc-set-time-pattern-text")
        .getAttribute("data-loc-time-pattern"),
    ).toBe("HH:mm:ss");
    fireEvent.click(screen.getByTestId("developer-loc-set-time-pattern-cancel"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(
      screen
        .getByTestId("developer-loc-set-time-pattern-text")
        .getAttribute("data-loc-time-pattern"),
    ).toBe("HH:mm:ss");

    fireEvent.click(screen.getByTestId("developer-loc-set-time-pattern-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-time-pattern-input"), {
      target: { value: " hh:mm a " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-time-pattern-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", {
      format: { timePattern: "hh:mm a" },
    });
    const sent = updateLocale.mock.calls.at(-1)?.[1] as Record<string, unknown>;
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("status");
    expect(sent).not.toHaveProperty("hasFormatProfile");
    expect(
      screen
        .getByTestId("developer-loc-set-time-pattern-text")
        .getAttribute("data-loc-time-pattern"),
    ).toBe("hh:mm a");
    expect((screen.getByTestId("developer-loc-fmt-time") as HTMLInputElement).value).toBe(
      "hh:mm a",
    );
    expect(screen.getByTestId("developer-loc-set-time-pattern-notice").textContent).toBe(
      LOC_TIME_MSG.SAVED,
    );
    expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-description") as HTMLInputElement).value).toBe(
      "US English",
    );
    expect((screen.getByTestId("developer-loc-status") as HTMLSelectElement).value).toBe("active");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
    expect((screen.getByTestId("developer-loc-fmt-dir") as HTMLSelectElement).value).toBe("ltr");
    expect((screen.getByTestId("developer-loc-fmt-currency-code") as HTMLInputElement).value).toBe(
      "USD",
    );
  });

  it("clears a blank time pattern and keeps the previous pattern on 400, 403, and 409", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, timePattern: "" },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-time-pattern-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-time-pattern-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-time-pattern-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-time-pattern-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-time-pattern-notice").textContent).toBe(
        LOC_TIME_MSG.CLEARED,
      );
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", { format: { timePattern: "" } });
    expect(
      screen
        .getByTestId("developer-loc-set-time-pattern-text")
        .getAttribute("data-loc-time-pattern"),
    ).toBe("");
    expect((screen.getByTestId("developer-loc-fmt-time") as HTMLInputElement).value).toBe("");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );

    for (const status of [400, 403, 409]) {
      updateLocale.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message: `forced ${status}` },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-time-pattern-edit"));
      fireEvent.change(screen.getByTestId("developer-loc-set-time-pattern-input"), {
        target: { value: "hh:mm a" },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-time-pattern-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-loc-set-time-pattern-error").textContent).toContain(
          `forced ${status}`,
        );
      });
      expect(
        screen
          .getByTestId("developer-loc-set-time-pattern-text")
          .getAttribute("data-loc-time-pattern"),
      ).toBe("");
      expect((screen.getByTestId("developer-loc-fmt-time") as HTMLInputElement).value).toBe("");
      expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
      fireEvent.click(screen.getByTestId("developer-loc-set-time-pattern-cancel"));
    }
  });

  it("does not show a time pattern when the update changes the label or other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      label: "Renamed",
      format: { ...sampleDetail.format, timePattern: "hh:mm a", datePattern: "yyyy-MM-dd" },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-time-pattern-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-time-pattern-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-time-pattern-input"), {
      target: { value: "hh:mm a" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-time-pattern-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-time-pattern-error").textContent).toContain(
        LOC_TIME_MSG.ERROR,
      );
    });
    expect(
      screen
        .getByTestId("developer-loc-set-time-pattern-text")
        .getAttribute("data-loc-time-pattern"),
    ).toBe("HH:mm:ss");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-time") as HTMLInputElement).value).toBe(
      "HH:mm:ss",
    );
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
  });

  it("sets the date-time pattern only after success and leaves language, label, description, status, and other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, dateTimePattern: "yyyy-MM-dd HH:mm:ss" },
    });
    const onSaved = vi.fn();
    render(
      <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(
        screen
          .getByTestId("developer-loc-set-date-time-pattern-text")
          .getAttribute("data-loc-date-time-pattern"),
      ).toBe("MM/dd/yyyy HH:mm:ss");
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-date-time-pattern-edit"));
    fireEvent.click(screen.getByTestId("developer-loc-set-date-time-pattern-save"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-loc-set-date-time-pattern-editor")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-loc-set-date-time-pattern-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-date-time-pattern-input"), {
      target: { value: " yyyy-MM-dd HH:mm:ss " },
    });
    expect(
      screen
        .getByTestId("developer-loc-set-date-time-pattern-text")
        .getAttribute("data-loc-date-time-pattern"),
    ).toBe("MM/dd/yyyy HH:mm:ss");
    fireEvent.click(screen.getByTestId("developer-loc-set-date-time-pattern-cancel"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(
      screen
        .getByTestId("developer-loc-set-date-time-pattern-text")
        .getAttribute("data-loc-date-time-pattern"),
    ).toBe("MM/dd/yyyy HH:mm:ss");

    fireEvent.click(screen.getByTestId("developer-loc-set-date-time-pattern-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-date-time-pattern-input"), {
      target: { value: " yyyy-MM-dd HH:mm:ss " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-date-time-pattern-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", {
      format: { dateTimePattern: "yyyy-MM-dd HH:mm:ss" },
    });
    const sent = updateLocale.mock.calls.at(-1)?.[1] as Record<string, unknown>;
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("status");
    expect(sent).not.toHaveProperty("hasFormatProfile");
    expect(
      screen
        .getByTestId("developer-loc-set-date-time-pattern-text")
        .getAttribute("data-loc-date-time-pattern"),
    ).toBe("yyyy-MM-dd HH:mm:ss");
    expect((screen.getByTestId("developer-loc-fmt-datetime") as HTMLInputElement).value).toBe(
      "yyyy-MM-dd HH:mm:ss",
    );
    expect(screen.getByTestId("developer-loc-set-date-time-pattern-notice").textContent).toBe(
      LOC_DATETIME_MSG.SAVED,
    );
    expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-description") as HTMLInputElement).value).toBe(
      "US English",
    );
    expect((screen.getByTestId("developer-loc-status") as HTMLSelectElement).value).toBe("active");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
    expect((screen.getByTestId("developer-loc-fmt-time") as HTMLInputElement).value).toBe(
      "HH:mm:ss",
    );
    expect((screen.getByTestId("developer-loc-fmt-dir") as HTMLSelectElement).value).toBe("ltr");
    expect((screen.getByTestId("developer-loc-fmt-currency-code") as HTMLInputElement).value).toBe(
      "USD",
    );
  });

  it("clears a blank date-time pattern and keeps the previous pattern on 400, 403, and 409", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, dateTimePattern: "" },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-date-time-pattern-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-date-time-pattern-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-date-time-pattern-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-date-time-pattern-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-date-time-pattern-notice").textContent).toBe(
        LOC_DATETIME_MSG.CLEARED,
      );
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", { format: { dateTimePattern: "" } });
    expect(
      screen
        .getByTestId("developer-loc-set-date-time-pattern-text")
        .getAttribute("data-loc-date-time-pattern"),
    ).toBe("");
    expect((screen.getByTestId("developer-loc-fmt-datetime") as HTMLInputElement).value).toBe("");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );

    for (const status of [400, 403, 409]) {
      updateLocale.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message: `forced ${status}` },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-date-time-pattern-edit"));
      fireEvent.change(screen.getByTestId("developer-loc-set-date-time-pattern-input"), {
        target: { value: "yyyy-MM-dd HH:mm:ss" },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-date-time-pattern-save"));
      await waitFor(() => {
        expect(
          screen.getByTestId("developer-loc-set-date-time-pattern-error").textContent,
        ).toContain(`forced ${status}`);
      });
      expect(
        screen
          .getByTestId("developer-loc-set-date-time-pattern-text")
          .getAttribute("data-loc-date-time-pattern"),
      ).toBe("");
      expect((screen.getByTestId("developer-loc-fmt-datetime") as HTMLInputElement).value).toBe(
        "",
      );
      expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe(
        "en-us",
      );
      fireEvent.click(screen.getByTestId("developer-loc-set-date-time-pattern-cancel"));
    }
  });

  it("does not show a date-time pattern when the update changes the label or other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      label: "Renamed",
      format: {
        ...sampleDetail.format,
        dateTimePattern: "yyyy-MM-dd HH:mm:ss",
        datePattern: "yyyy-MM-dd",
      },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-date-time-pattern-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-date-time-pattern-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-date-time-pattern-input"), {
      target: { value: "yyyy-MM-dd HH:mm:ss" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-date-time-pattern-save"));
    await waitFor(() => {
      expect(
        screen.getByTestId("developer-loc-set-date-time-pattern-error").textContent,
      ).toContain(LOC_DATETIME_MSG.ERROR);
    });
    expect(
      screen
        .getByTestId("developer-loc-set-date-time-pattern-text")
        .getAttribute("data-loc-date-time-pattern"),
    ).toBe("MM/dd/yyyy HH:mm:ss");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-datetime") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy HH:mm:ss",
    );
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
  });

  it("sets the decimal separator only after success and leaves language, label, description, status, and other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, decimalSep: "," },
    });
    const onSaved = vi.fn();
    render(
      <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("developer-loc-set-decimal-sep-text").getAttribute("data-loc-decimal-sep"),
      ).toBe(".");
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-decimal-sep-edit"));
    fireEvent.click(screen.getByTestId("developer-loc-set-decimal-sep-save"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-loc-set-decimal-sep-editor")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-loc-set-decimal-sep-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-decimal-sep-input"), {
      target: { value: " , " },
    });
    expect(
      screen.getByTestId("developer-loc-set-decimal-sep-text").getAttribute("data-loc-decimal-sep"),
    ).toBe(".");
    fireEvent.click(screen.getByTestId("developer-loc-set-decimal-sep-cancel"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(
      screen.getByTestId("developer-loc-set-decimal-sep-text").getAttribute("data-loc-decimal-sep"),
    ).toBe(".");

    fireEvent.click(screen.getByTestId("developer-loc-set-decimal-sep-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-decimal-sep-input"), {
      target: { value: " , " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-decimal-sep-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", {
      format: { decimalSep: "," },
    });
    const sent = updateLocale.mock.calls.at(-1)?.[1] as Record<string, unknown>;
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("status");
    expect(sent).not.toHaveProperty("hasFormatProfile");
    expect(
      screen.getByTestId("developer-loc-set-decimal-sep-text").getAttribute("data-loc-decimal-sep"),
    ).toBe(",");
    expect((screen.getByTestId("developer-loc-fmt-decimal") as HTMLInputElement).value).toBe(",");
    expect(screen.getByTestId("developer-loc-set-decimal-sep-notice").textContent).toBe(
      LOC_DECIMAL_MSG.SAVED,
    );
    expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-description") as HTMLInputElement).value).toBe(
      "US English",
    );
    expect((screen.getByTestId("developer-loc-status") as HTMLSelectElement).value).toBe("active");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
    expect((screen.getByTestId("developer-loc-fmt-time") as HTMLInputElement).value).toBe(
      "HH:mm:ss",
    );
    expect((screen.getByTestId("developer-loc-fmt-datetime") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy HH:mm:ss",
    );
    expect((screen.getByTestId("developer-loc-fmt-grouping") as HTMLInputElement).value).toBe(",");
    expect((screen.getByTestId("developer-loc-fmt-dir") as HTMLSelectElement).value).toBe("ltr");
    expect((screen.getByTestId("developer-loc-fmt-currency-code") as HTMLInputElement).value).toBe(
      "USD",
    );
  });

  it("clears a blank decimal separator and keeps the previous separator on 400, 403, and 409", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, decimalSep: "" },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-decimal-sep-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-decimal-sep-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-decimal-sep-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-decimal-sep-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-decimal-sep-notice").textContent).toBe(
        LOC_DECIMAL_MSG.CLEARED,
      );
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", { format: { decimalSep: "" } });
    expect(
      screen.getByTestId("developer-loc-set-decimal-sep-text").getAttribute("data-loc-decimal-sep"),
    ).toBe("");
    expect((screen.getByTestId("developer-loc-fmt-decimal") as HTMLInputElement).value).toBe("");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-grouping") as HTMLInputElement).value).toBe(",");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );

    for (const status of [400, 403, 409]) {
      updateLocale.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message: `forced ${status}` },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-decimal-sep-edit"));
      fireEvent.change(screen.getByTestId("developer-loc-set-decimal-sep-input"), {
        target: { value: "," },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-decimal-sep-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-loc-set-decimal-sep-error").textContent).toContain(
          `forced ${status}`,
        );
      });
      expect(
        screen.getByTestId("developer-loc-set-decimal-sep-text").getAttribute("data-loc-decimal-sep"),
      ).toBe("");
      expect((screen.getByTestId("developer-loc-fmt-decimal") as HTMLInputElement).value).toBe("");
      expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
      fireEvent.click(screen.getByTestId("developer-loc-set-decimal-sep-cancel"));
    }
  });

  it("does not show a decimal separator when the update changes the label or other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      label: "Renamed",
      format: {
        ...sampleDetail.format,
        decimalSep: ",",
        groupingSep: " ",
      },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-decimal-sep-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-decimal-sep-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-decimal-sep-input"), {
      target: { value: "," },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-decimal-sep-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-decimal-sep-error").textContent).toContain(
        LOC_DECIMAL_MSG.ERROR,
      );
    });
    expect(
      screen.getByTestId("developer-loc-set-decimal-sep-text").getAttribute("data-loc-decimal-sep"),
    ).toBe(".");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-decimal") as HTMLInputElement).value).toBe(".");
    expect((screen.getByTestId("developer-loc-fmt-grouping") as HTMLInputElement).value).toBe(",");
  });

  it("sets the grouping separator only after success and leaves language, label, description, status, and other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, groupingSep: "." },
    });
    const onSaved = vi.fn();
    render(
      <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("developer-loc-set-grouping-sep-text").getAttribute("data-loc-grouping-sep"),
      ).toBe(",");
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-grouping-sep-edit"));
    fireEvent.click(screen.getByTestId("developer-loc-set-grouping-sep-save"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-loc-set-grouping-sep-editor")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-loc-set-grouping-sep-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-grouping-sep-input"), {
      target: { value: " . " },
    });
    expect(
      screen.getByTestId("developer-loc-set-grouping-sep-text").getAttribute("data-loc-grouping-sep"),
    ).toBe(",");
    fireEvent.click(screen.getByTestId("developer-loc-set-grouping-sep-cancel"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(
      screen.getByTestId("developer-loc-set-grouping-sep-text").getAttribute("data-loc-grouping-sep"),
    ).toBe(",");

    fireEvent.click(screen.getByTestId("developer-loc-set-grouping-sep-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-grouping-sep-input"), {
      target: { value: " . " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-grouping-sep-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", {
      format: { groupingSep: "." },
    });
    const sent = updateLocale.mock.calls.at(-1)?.[1] as Record<string, unknown>;
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("status");
    expect(sent).not.toHaveProperty("hasFormatProfile");
    expect(
      screen.getByTestId("developer-loc-set-grouping-sep-text").getAttribute("data-loc-grouping-sep"),
    ).toBe(".");
    expect((screen.getByTestId("developer-loc-fmt-grouping") as HTMLInputElement).value).toBe(".");
    expect(screen.getByTestId("developer-loc-set-grouping-sep-notice").textContent).toBe(
      LOC_GROUPING_MSG.SAVED,
    );
    expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-description") as HTMLInputElement).value).toBe(
      "US English",
    );
    expect((screen.getByTestId("developer-loc-status") as HTMLSelectElement).value).toBe("active");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
    expect((screen.getByTestId("developer-loc-fmt-time") as HTMLInputElement).value).toBe(
      "HH:mm:ss",
    );
    expect((screen.getByTestId("developer-loc-fmt-datetime") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy HH:mm:ss",
    );
    expect((screen.getByTestId("developer-loc-fmt-decimal") as HTMLInputElement).value).toBe(".");
    expect((screen.getByTestId("developer-loc-fmt-dir") as HTMLSelectElement).value).toBe("ltr");
    expect((screen.getByTestId("developer-loc-fmt-currency-code") as HTMLInputElement).value).toBe(
      "USD",
    );
  });

  it("clears a blank grouping separator and keeps the previous separator on 400, 403, and 409", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, groupingSep: "" },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-grouping-sep-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-grouping-sep-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-grouping-sep-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-grouping-sep-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-grouping-sep-notice").textContent).toBe(
        LOC_GROUPING_MSG.CLEARED,
      );
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", { format: { groupingSep: "" } });
    expect(
      screen.getByTestId("developer-loc-set-grouping-sep-text").getAttribute("data-loc-grouping-sep"),
    ).toBe("");
    expect((screen.getByTestId("developer-loc-fmt-grouping") as HTMLInputElement).value).toBe("");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-decimal") as HTMLInputElement).value).toBe(".");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );

    for (const status of [400, 403, 409]) {
      updateLocale.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message: `forced ${status}` },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-grouping-sep-edit"));
      fireEvent.change(screen.getByTestId("developer-loc-set-grouping-sep-input"), {
        target: { value: "." },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-grouping-sep-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-loc-set-grouping-sep-error").textContent).toContain(
          `forced ${status}`,
        );
      });
      expect(
        screen.getByTestId("developer-loc-set-grouping-sep-text").getAttribute("data-loc-grouping-sep"),
      ).toBe("");
      expect((screen.getByTestId("developer-loc-fmt-grouping") as HTMLInputElement).value).toBe("");
      expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
      fireEvent.click(screen.getByTestId("developer-loc-set-grouping-sep-cancel"));
    }
  });

  it("does not show a grouping separator when the update changes the label or other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      label: "Renamed",
      format: {
        ...sampleDetail.format,
        groupingSep: ".",
        decimalSep: ",",
      },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-grouping-sep-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-grouping-sep-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-grouping-sep-input"), {
      target: { value: "." },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-grouping-sep-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-grouping-sep-error").textContent).toContain(
        LOC_GROUPING_MSG.ERROR,
      );
    });
    expect(
      screen.getByTestId("developer-loc-set-grouping-sep-text").getAttribute("data-loc-grouping-sep"),
    ).toBe(",");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-grouping") as HTMLInputElement).value).toBe(",");
    expect((screen.getByTestId("developer-loc-fmt-decimal") as HTMLInputElement).value).toBe(".");
  });

  it("sets the currency code only after success and leaves language, label, description, status, and other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, currencyCode: "EUR" },
    });
    const onSaved = vi.fn();
    render(
      <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("developer-loc-set-currency-code-text").getAttribute("data-loc-currency-code"),
      ).toBe("USD");
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-currency-code-edit"));
    fireEvent.click(screen.getByTestId("developer-loc-set-currency-code-save"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-loc-set-currency-code-editor")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-loc-set-currency-code-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-currency-code-input"), {
      target: { value: " EUR " },
    });
    expect(
      screen.getByTestId("developer-loc-set-currency-code-text").getAttribute("data-loc-currency-code"),
    ).toBe("USD");
    fireEvent.click(screen.getByTestId("developer-loc-set-currency-code-cancel"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(
      screen.getByTestId("developer-loc-set-currency-code-text").getAttribute("data-loc-currency-code"),
    ).toBe("USD");

    fireEvent.click(screen.getByTestId("developer-loc-set-currency-code-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-currency-code-input"), {
      target: { value: " EUR " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-currency-code-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", {
      format: { currencyCode: "EUR" },
    });
    const sent = updateLocale.mock.calls.at(-1)?.[1] as Record<string, unknown>;
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("status");
    expect(sent).not.toHaveProperty("hasFormatProfile");
    expect(
      screen.getByTestId("developer-loc-set-currency-code-text").getAttribute("data-loc-currency-code"),
    ).toBe("EUR");
    expect((screen.getByTestId("developer-loc-fmt-currency-code") as HTMLInputElement).value).toBe(
      "EUR",
    );
    expect(screen.getByTestId("developer-loc-set-currency-code-notice").textContent).toBe(
      LOC_CURRENCY_MSG.SAVED,
    );
    expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-description") as HTMLInputElement).value).toBe(
      "US English",
    );
    expect((screen.getByTestId("developer-loc-status") as HTMLSelectElement).value).toBe("active");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
    expect((screen.getByTestId("developer-loc-fmt-time") as HTMLInputElement).value).toBe(
      "HH:mm:ss",
    );
    expect((screen.getByTestId("developer-loc-fmt-datetime") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy HH:mm:ss",
    );
    expect((screen.getByTestId("developer-loc-fmt-decimal") as HTMLInputElement).value).toBe(".");
    expect((screen.getByTestId("developer-loc-fmt-grouping") as HTMLInputElement).value).toBe(",");
    expect((screen.getByTestId("developer-loc-fmt-dir") as HTMLSelectElement).value).toBe("ltr");
  });

  it("clears a blank currency code and keeps the previous code on 400, 403, and 409", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, currencyCode: "" },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-currency-code-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-currency-code-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-currency-code-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-currency-code-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-currency-code-notice").textContent).toBe(
        LOC_CURRENCY_MSG.CLEARED,
      );
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", { format: { currencyCode: "" } });
    expect(
      screen.getByTestId("developer-loc-set-currency-code-text").getAttribute("data-loc-currency-code"),
    ).toBe("");
    expect((screen.getByTestId("developer-loc-fmt-currency-code") as HTMLInputElement).value).toBe("");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-grouping") as HTMLInputElement).value).toBe(",");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );

    for (const status of [400, 403, 409]) {
      updateLocale.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message: `forced ${status}` },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-currency-code-edit"));
      fireEvent.change(screen.getByTestId("developer-loc-set-currency-code-input"), {
        target: { value: "EUR" },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-currency-code-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-loc-set-currency-code-error").textContent).toContain(
          `forced ${status}`,
        );
      });
      expect(
        screen.getByTestId("developer-loc-set-currency-code-text").getAttribute("data-loc-currency-code"),
      ).toBe("");
      expect((screen.getByTestId("developer-loc-fmt-currency-code") as HTMLInputElement).value).toBe(
        "",
      );
      expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
      fireEvent.click(screen.getByTestId("developer-loc-set-currency-code-cancel"));
    }
  });

  it("does not show a currency code when the update changes the label or other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      label: "Renamed",
      format: {
        ...sampleDetail.format,
        currencyCode: "EUR",
        groupingSep: " ",
      },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-currency-code-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-currency-code-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-currency-code-input"), {
      target: { value: "EUR" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-currency-code-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-currency-code-error").textContent).toContain(
        LOC_CURRENCY_MSG.ERROR,
      );
    });
    expect(
      screen.getByTestId("developer-loc-set-currency-code-text").getAttribute("data-loc-currency-code"),
    ).toBe("USD");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-currency-code") as HTMLInputElement).value).toBe(
      "USD",
    );
    expect((screen.getByTestId("developer-loc-fmt-grouping") as HTMLInputElement).value).toBe(",");
  });

  it("sets the text direction only after success and leaves language, label, description, status, and other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, textDir: "rtl" },
    });
    const onSaved = vi.fn();
    render(
      <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("developer-loc-set-text-dir-text").getAttribute("data-loc-text-dir"),
      ).toBe("ltr");
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-text-dir-edit"));
    fireEvent.click(screen.getByTestId("developer-loc-set-text-dir-save"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-loc-set-text-dir-editor")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-loc-set-text-dir-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-text-dir-input"), {
      target: { value: "rtl" },
    });
    expect(
      screen.getByTestId("developer-loc-set-text-dir-text").getAttribute("data-loc-text-dir"),
    ).toBe("ltr");
    fireEvent.click(screen.getByTestId("developer-loc-set-text-dir-cancel"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(
      screen.getByTestId("developer-loc-set-text-dir-text").getAttribute("data-loc-text-dir"),
    ).toBe("ltr");

    fireEvent.click(screen.getByTestId("developer-loc-set-text-dir-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-text-dir-input"), {
      target: { value: "rtl" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-text-dir-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", {
      format: { textDir: "rtl" },
    });
    const sent = updateLocale.mock.calls.at(-1)?.[1] as Record<string, unknown>;
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("status");
    expect(sent).not.toHaveProperty("hasFormatProfile");
    expect(
      screen.getByTestId("developer-loc-set-text-dir-text").getAttribute("data-loc-text-dir"),
    ).toBe("rtl");
    expect((screen.getByTestId("developer-loc-fmt-dir") as HTMLSelectElement).value).toBe("rtl");
    expect(screen.getByTestId("developer-loc-set-text-dir-notice").textContent).toBe(
      LOC_TEXT_DIR_MSG.SAVED,
    );
    expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-description") as HTMLInputElement).value).toBe(
      "US English",
    );
    expect((screen.getByTestId("developer-loc-status") as HTMLSelectElement).value).toBe("active");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
    expect((screen.getByTestId("developer-loc-fmt-time") as HTMLInputElement).value).toBe(
      "HH:mm:ss",
    );
    expect((screen.getByTestId("developer-loc-fmt-datetime") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy HH:mm:ss",
    );
    expect((screen.getByTestId("developer-loc-fmt-decimal") as HTMLInputElement).value).toBe(".");
    expect((screen.getByTestId("developer-loc-fmt-grouping") as HTMLInputElement).value).toBe(",");
    expect((screen.getByTestId("developer-loc-fmt-currency-code") as HTMLInputElement).value).toBe(
      "USD",
    );
  });

  it("clears a blank text direction and keeps the previous direction on 400, 403, and 409", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      format: { ...sampleDetail.format, textDir: "" },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-text-dir-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-text-dir-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-text-dir-input"), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-text-dir-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-text-dir-notice").textContent).toBe(
        LOC_TEXT_DIR_MSG.CLEARED,
      );
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", { format: { textDir: "" } });
    expect(
      screen.getByTestId("developer-loc-set-text-dir-text").getAttribute("data-loc-text-dir"),
    ).toBe("");
    expect((screen.getByTestId("developer-loc-fmt-dir") as HTMLSelectElement).value).toBe("");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-decimal") as HTMLInputElement).value).toBe(".");
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );

    for (const status of [400, 403, 409]) {
      updateLocale.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message: `forced ${status}` },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-text-dir-edit"));
      fireEvent.change(screen.getByTestId("developer-loc-set-text-dir-input"), {
        target: { value: "rtl" },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-text-dir-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-loc-set-text-dir-error").textContent).toContain(
          `forced ${status}`,
        );
      });
      expect(
        screen.getByTestId("developer-loc-set-text-dir-text").getAttribute("data-loc-text-dir"),
      ).toBe("");
      expect((screen.getByTestId("developer-loc-fmt-dir") as HTMLSelectElement).value).toBe("");
      expect(screen.queryByTestId("developer-loc-set-text-dir-notice")).toBeNull();
      fireEvent.click(screen.getByTestId("developer-loc-set-text-dir-cancel"));
    }
  });

  it("does not show a text direction when the update changes the label or other format fields", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      label: "Renamed",
      format: {
        ...sampleDetail.format,
        textDir: "rtl",
        decimalSep: ",",
      },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-text-dir-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-text-dir-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-text-dir-input"), {
      target: { value: "rtl" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-text-dir-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-text-dir-error").textContent).toContain(
        LOC_TEXT_DIR_MSG.ERROR,
      );
    });
    expect(
      screen.getByTestId("developer-loc-set-text-dir-text").getAttribute("data-loc-text-dir"),
    ).toBe("ltr");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-dir") as HTMLSelectElement).value).toBe("ltr");
    expect((screen.getByTestId("developer-loc-fmt-decimal") as HTMLInputElement).value).toBe(".");
  });

  it("sets enabled status only after success and leaves language, label, description, and format", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      status: "inactive",
    });
    const onSaved = vi.fn();
    render(
      <LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("developer-loc-set-enabled-status-text").getAttribute("data-loc-enabled-status"),
      ).toBe("active");
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-enabled-status-edit"));
    fireEvent.click(screen.getByTestId("developer-loc-set-enabled-status-save"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-loc-set-enabled-status-editor")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-loc-set-enabled-status-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-enabled-status-input"), {
      target: { value: "inactive" },
    });
    expect(
      screen.getByTestId("developer-loc-set-enabled-status-text").getAttribute("data-loc-enabled-status"),
    ).toBe("active");
    fireEvent.click(screen.getByTestId("developer-loc-set-enabled-status-cancel"));
    expect(updateLocale).not.toHaveBeenCalled();
    expect(
      screen.getByTestId("developer-loc-set-enabled-status-text").getAttribute("data-loc-enabled-status"),
    ).toBe("active");

    fireEvent.click(screen.getByTestId("developer-loc-set-enabled-status-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-enabled-status-input"), {
      target: { value: "inactive" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-enabled-status-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateLocale).toHaveBeenCalledWith("en-us", { status: "inactive" });
    const sent = updateLocale.mock.calls.at(-1)?.[1] as Record<string, unknown>;
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("format");
    expect(sent).not.toHaveProperty("hasFormatProfile");
    expect(
      screen.getByTestId("developer-loc-set-enabled-status-text").getAttribute("data-loc-enabled-status"),
    ).toBe("inactive");
    expect((screen.getByTestId("developer-loc-status") as HTMLSelectElement).value).toBe("inactive");
    expect(screen.getByTestId("developer-loc-set-enabled-status-notice").textContent).toBe(
      LOC_STATUS_MSG.SAVED,
    );
    expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-description") as HTMLInputElement).value).toBe(
      "US English",
    );
    expect((screen.getByTestId("developer-loc-fmt-date") as HTMLInputElement).value).toBe(
      "MM/dd/yyyy",
    );
    expect((screen.getByTestId("developer-loc-fmt-grouping") as HTMLInputElement).value).toBe(",");
    expect((screen.getByTestId("developer-loc-fmt-currency-code") as HTMLInputElement).value).toBe(
      "USD",
    );
  });

  it("keeps the previous enabled status on 400, 403, and 409", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-enabled-status-edit")).toBeTruthy();
    });

    for (const status of [400, 403, 409]) {
      updateLocale.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message: `forced ${status}` },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-enabled-status-edit"));
      fireEvent.change(screen.getByTestId("developer-loc-set-enabled-status-input"), {
        target: { value: "inactive" },
      });
      fireEvent.click(screen.getByTestId("developer-loc-set-enabled-status-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-loc-set-enabled-status-error").textContent).toContain(
          `forced ${status}`,
        );
      });
      expect(
        screen.getByTestId("developer-loc-set-enabled-status-text").getAttribute("data-loc-enabled-status"),
      ).toBe("active");
      expect((screen.getByTestId("developer-loc-status") as HTMLSelectElement).value).toBe("active");
      expect((screen.getByTestId("developer-loc-language") as HTMLInputElement).value).toBe("en-us");
      fireEvent.click(screen.getByTestId("developer-loc-set-enabled-status-cancel"));
    }
  });

  it("does not show a new enabled status when the update changes the label or format", async () => {
    getLocaleDetail.mockResolvedValue(sampleDetail);
    updateLocale.mockResolvedValue({
      ...sampleDetail,
      status: "inactive",
      label: "Renamed",
      format: {
        ...sampleDetail.format,
        groupingSep: " ",
      },
    });
    render(<LocaleDetailPanel idOrLang="en-us" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-enabled-status-edit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-enabled-status-edit"));
    fireEvent.change(screen.getByTestId("developer-loc-set-enabled-status-input"), {
      target: { value: "inactive" },
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-enabled-status-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-enabled-status-error").textContent).toContain(
        LOC_STATUS_MSG.ERROR,
      );
    });
    expect(
      screen.getByTestId("developer-loc-set-enabled-status-text").getAttribute("data-loc-enabled-status"),
    ).toBe("active");
    expect((screen.getByTestId("developer-loc-status") as HTMLSelectElement).value).toBe("active");
    expect((screen.getByTestId("developer-loc-label") as HTMLInputElement).value).toBe("English");
    expect((screen.getByTestId("developer-loc-fmt-grouping") as HTMLInputElement).value).toBe(",");
  });
});
