/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { listCommunities } from "../../../main/ts/api/developer/assemblyApi";
import {
  listAutoTranslations,
  saveAutoTranslations,
} from "../../../main/ts/api/developer/autoTranslationsApi";
import { listContentTypes } from "../../../main/ts/api/developer/contentTypesApi";
import { listLocales } from "../../../main/ts/api/developer/localesApi";
import { listWorkflows } from "../../../main/ts/api/developer/workflowsApi";
import { AutoTranslationsPanel } from "../../../main/ts/developer/AutoTranslationsPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/autoTranslationsApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/autoTranslationsApi")
  >();
  return {
    ...actual,
    listAutoTranslations: vi.fn(),
    saveAutoTranslations: vi.fn(),
  };
});

vi.mock("../../../main/ts/api/developer/localesApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/localesApi")
  >();
  return {
    ...actual,
    listLocales: vi.fn(),
  };
});

vi.mock("../../../main/ts/api/developer/contentTypesApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/contentTypesApi")
  >();
  return {
    ...actual,
    listContentTypes: vi.fn(),
  };
});

vi.mock("../../../main/ts/api/developer/workflowsApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/workflowsApi")
  >();
  return {
    ...actual,
    listWorkflows: vi.fn(),
  };
});

vi.mock("../../../main/ts/api/developer/assemblyApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/assemblyApi")
  >();
  return {
    ...actual,
    listCommunities: vi.fn(),
  };
});

const listAt = vi.mocked(listAutoTranslations);
const saveAt = vi.mocked(saveAutoTranslations);
const localesMock = vi.mocked(listLocales);
const typesMock = vi.mocked(listContentTypes);
const workflowsMock = vi.mocked(listWorkflows);
const communitiesMock = vi.mocked(listCommunities);

const readyRow = {
  locale: "en-us",
  contentTypeName: "percPage",
  workflowName: "Default Workflow",
  communityName: "Default",
};

function mockCatalogs(): void {
  localesMock.mockResolvedValue([{ languageString: "en-us", label: "English" }]);
  typesMock.mockResolvedValue([{ name: "percPage", label: "Page" }]);
  workflowsMock.mockResolvedValue([{ workflowName: "Default Workflow" }]);
  communitiesMock.mockResolvedValue([{ name: "Default", label: "Default" }]);
}

describe("AutoTranslationsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listAt.mockReset();
    saveAt.mockReset();
    localesMock.mockReset();
    typesMock.mockReset();
    workflowsMock.mockReset();
    communitiesMock.mockReset();
    mockCatalogs();
  });

  afterEach(() => {
    cleanup();
  });

  it("loads current rows", async () => {
    listAt.mockResolvedValue([readyRow]);
    render(<AutoTranslationsPanel onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-table")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-at-locale-0") as HTMLInputElement).value).toBe(
      "en-us",
    );
    expect((screen.getByTestId("developer-at-type-0") as HTMLInputElement).value).toBe(
      "percPage",
    );
  });

  it("adds and removes rows; empty list can save to clear", async () => {
    listAt.mockResolvedValue([readyRow]);
    saveAt.mockResolvedValue([]);
    render(<AutoTranslationsPanel onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-row-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-at-remove-0"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-empty")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-at-save"));
    await waitFor(() => {
      expect(saveAt).toHaveBeenCalledWith([]);
    });
    expect(screen.getByTestId("developer-at-notice").textContent).toBe(DEV_MSG.AT_SAVED);
  });

  it("shows duplicate-key alert and disables save", async () => {
    listAt.mockResolvedValue([]);
    render(<AutoTranslationsPanel onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-add")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-at-add"));
    fireEvent.click(screen.getByTestId("developer-at-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-duplicate")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-at-duplicate").textContent).toBe(DEV_MSG.AT_DUPLICATE);
    expect((screen.getByTestId("developer-at-save") as HTMLButtonElement).disabled).toBe(true);
    expect(saveAt).not.toHaveBeenCalled();
  });

  it.each([
    ["locales", () => localesMock.mockRejectedValue(new Error("locales catalog down"))],
    ["content types", () => typesMock.mockRejectedValue(new Error("types catalog down"))],
    ["workflows", () => workflowsMock.mockRejectedValue(new Error("workflows catalog down"))],
    ["communities", () => communitiesMock.mockRejectedValue(new Error("communities catalog down"))],
  ] as const)("surfaces catalogWarning when %s catalog API rejects", async (_name, rejectCatalog) => {
    listAt.mockResolvedValue([]);
    rejectCatalog();
    render(<AutoTranslationsPanel onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-catalog-warning")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-at-catalog-warning").textContent).toBe(
      DEV_MSG.AT_CATALOG_ERROR,
    );
    expect(screen.getByTestId("developer-at-empty")).toBeTruthy();
  });

  it("saves a valid locale×content-type row", async () => {
    listAt.mockResolvedValue([]);
    saveAt.mockResolvedValue([readyRow]);
    render(<AutoTranslationsPanel onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-empty")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-at-add"));
    expect((screen.getByTestId("developer-at-save") as HTMLButtonElement).disabled).toBe(false);
    fireEvent.click(screen.getByTestId("developer-at-save"));
    await waitFor(() => {
      expect(saveAt).toHaveBeenCalled();
    });
    const body = saveAt.mock.calls[0][0];
    expect(body[0]?.locale).toBe("en-us");
    expect(body[0]?.contentTypeName).toBe("percPage");
    expect(screen.getByTestId("developer-at-notice").textContent).toBe(DEV_MSG.AT_SAVED);
  });

  it("surfaces unknown locale 400", async () => {
    listAt.mockResolvedValue([]);
    saveAt.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "unknown locale: xx-xx" },
    });
    render(<AutoTranslationsPanel onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-add")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-at-add"));
    fireEvent.change(screen.getByTestId("developer-at-locale-0"), {
      target: { value: "xx-xx" },
    });
    fireEvent.click(screen.getByTestId("developer-at-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-at-error").textContent).toContain(DEV_MSG.AT_UNKNOWN);
    expect(screen.getByTestId("developer-at-error").textContent).toContain("unknown locale");
  });

  it("surfaces unknown content type 400", async () => {
    listAt.mockResolvedValue([]);
    saveAt.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "unknown content type: missing" },
    });
    render(<AutoTranslationsPanel onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-add")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-at-add"));
    fireEvent.change(screen.getByTestId("developer-at-type-0"), {
      target: { value: "missing" },
    });
    fireEvent.click(screen.getByTestId("developer-at-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-at-error").textContent).toContain(DEV_MSG.AT_UNKNOWN);
    expect(screen.getByTestId("developer-at-error").textContent).toContain(
      "unknown content type",
    );
  });

  it("surfaces lock 409", async () => {
    listAt.mockResolvedValue([readyRow]);
    saveAt.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Could not save auto-translations; locked by other" },
    });
    render(<AutoTranslationsPanel onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-save")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-at-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-at-error").textContent).toContain(DEV_MSG.AT_LOCK);
    expect(screen.getByTestId("developer-at-error").textContent).toContain("locked by other");
  });

  it("does not PUT twice when save is clicked twice", async () => {
    listAt.mockResolvedValue([readyRow]);
    let resolveSave: (v: typeof readyRow[]) => void = () => undefined;
    saveAt.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveSave = resolve;
        }),
    );
    render(<AutoTranslationsPanel onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-save")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-at-save"));
    fireEvent.click(screen.getByTestId("developer-at-save"));
    expect(saveAt).toHaveBeenCalledTimes(1);
    await act(async () => {
      resolveSave([readyRow]);
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-notice")).toBeTruthy();
    });
  });

  it("calls onBack", async () => {
    listAt.mockResolvedValue([]);
    const onBack = vi.fn();
    render(<AutoTranslationsPanel onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-at-back")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-at-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("does not apply late load after unmount", async () => {
    let resolveList!: (v: typeof readyRow[]) => void;
    listAt.mockReturnValue(
      new Promise((resolve) => {
        resolveList = resolve;
      }),
    );
    const { unmount } = render(<AutoTranslationsPanel onBack={() => undefined} />);
    expect(screen.getByTestId("developer-at-loading")).toBeTruthy();
    unmount();
    await act(async () => {
      resolveList([]);
    });
    expect(screen.queryByTestId("developer-at-empty")).toBeNull();
  });
});
