/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
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

vi.mock("../../../main/ts/api/developer/autoTranslationsApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/autoTranslationsApi")
  >();
  return {
    ...actual,
    listAutoTranslations: vi.fn().mockResolvedValue([]),
    saveAutoTranslations: vi.fn().mockResolvedValue([]),
  };
});

vi.mock("../../../main/ts/api/developer/contentTypesApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/contentTypesApi")
  >();
  return {
    ...actual,
    listContentTypes: vi.fn().mockResolvedValue([{ name: "percPage", label: "Page" }]),
  };
});

vi.mock("../../../main/ts/api/developer/workflowsApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/workflowsApi")
  >();
  return {
    ...actual,
    listWorkflows: vi.fn().mockResolvedValue([{ workflowName: "Default Workflow" }]),
  };
});

vi.mock("../../../main/ts/api/developer/assemblyApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/assemblyApi")
  >();
  return {
    ...actual,
    listCommunities: vi.fn().mockResolvedValue([{ name: "Default" }]),
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
    expect(screen.getByTestId("developer-at-open")).toBeTruthy();
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

  it("opens auto-translations editor from the catalog", async () => {
    listMock.mockResolvedValue([
      { languageString: "en-us", label: "English", status: "active" },
    ]);
    render(<LocalesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-open")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-at-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-panel")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-loc-table")).toBeNull();
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

  it("does not apply catalog results after unmount", async () => {
    let resolveList!: (v: Awaited<ReturnType<typeof listLocales>>) => void;
    const pending = new Promise<Awaited<ReturnType<typeof listLocales>>>((resolve) => {
      resolveList = resolve;
    });
    listMock.mockReturnValue(pending);
    const { unmount } = render(<LocalesPanel />);
    expect(screen.getByTestId("developer-loc-loading")).toBeTruthy();
    unmount();
    await act(async () => {
      resolveList([]);
      await pending;
    });
    expect(screen.queryByTestId("developer-loc-empty")).toBeNull();
    expect(screen.queryByTestId("developer-loc-table")).toBeNull();
  });

  it("does not apply catalog errors after unmount", async () => {
    let rejectList!: (e: unknown) => void;
    const pending = new Promise<Awaited<ReturnType<typeof listLocales>>>((_, reject) => {
      rejectList = reject;
    });
    listMock.mockReturnValue(pending);
    const { unmount } = render(<LocalesPanel />);
    expect(screen.getByTestId("developer-loc-loading")).toBeTruthy();
    unmount();
    await act(async () => {
      rejectList(new Error("late"));
      await pending.catch(() => undefined);
    });
    expect(screen.queryByTestId("developer-loc-error")).toBeNull();
  });
});
