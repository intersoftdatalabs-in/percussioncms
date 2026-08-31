/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as searchesApi from "../../../main/ts/api/developer/searchesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SearchDetailPanel } from "../../../main/ts/developer/SearchDetailPanel";

vi.mock("../../../main/ts/api/developer/searchesApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/searchesApi")
  >();
  return {
    ...actual,
    listSearches: vi.fn(),
    getSearchDetail: vi.fn(),
    createSearch: vi.fn(),
    saveSearch: vi.fn(),
    deleteSearch: vi.fn(),
  };
});

vi.mock("../../../main/ts/developer/ObjectAclSection", () => ({
  ObjectAclSection: (props: {
    objectGuid?: string | null;
    objectKind?: string | null;
    testIdPrefix?: string;
  }) => (
    <div
      data-testid={`${props.testIdPrefix ?? "developer-acl"}-stub`}
      data-object-guid={props.objectGuid ?? ""}
      data-object-kind={props.objectKind ?? ""}
    />
  ),
}));

const getSearchDetail = searchesApi.getSearchDetail as ReturnType<typeof vi.fn>;
const createSearch = searchesApi.createSearch as ReturnType<typeof vi.fn>;
const saveSearch = searchesApi.saveSearch as ReturnType<typeof vi.fn>;
const deleteSearch = searchesApi.deleteSearch as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "All Content",
  label: "All Content",
  description: "System search",
  type: "StandardSearch",
  displayFormatId: "Default",
  maximumResultSize: 100,
  caseSensitive: false,
  guid: { stringValue: "0-26-7" },
  fields: [{ fieldName: "sys_title", operator: "=", fieldValue: "*", fieldType: "text" }],
  designGaps: ["gap-a"],
};

describe("SearchDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getSearchDetail.mockReset();
    createSearch.mockReset();
    saveSearch.mockReset();
    deleteSearch.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getSearchDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<SearchDetailPanel idOrName="All Content" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-title").textContent).toContain("All Content");
    expect(screen.getByTestId("developer-sr-fields-table")).toBeTruthy();
    expect(screen.getByTestId("developer-sr-gaps").textContent).toContain("gap-a");
    expect(getSearchDetail).toHaveBeenCalledWith("All Content");
    const acl = screen.getByTestId("developer-sr-acl-stub");
    expect(acl.getAttribute("data-object-kind")).toBe("search");
    expect(acl.getAttribute("data-object-guid")).toBe("0-26-7");
    fireEvent.click(screen.getByTestId("developer-sr-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty fields section when detail has none", async () => {
    getSearchDetail.mockResolvedValue({ ...sampleDetail, fields: [] });
    render(<SearchDetailPanel idOrName="All Content" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-fields-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-sr-fields-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getSearchDetail.mockRejectedValue(new SessionRedirectError());
    render(<SearchDetailPanel idOrName="All Content" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-sr-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-sr-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getSearchDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<SearchDetailPanel idOrName="All Content" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toBe(
      `${DEV_MSG.SR_DETAIL_ERROR} (500)`,
    );
  });

  it("shows 404 missing search via panelErrMsg", async () => {
    getSearchDetail.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Search not found" },
    });
    render(<SearchDetailPanel idOrName="missing" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toContain(
      DEV_MSG.SR_DETAIL_ERROR,
    );
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toContain(
      "Search not found",
    );
    expect(screen.queryByTestId("developer-sr-save")).toBeNull();
  });

  it("shows Error.message via panelErrMsg", async () => {
    getSearchDetail.mockRejectedValue(new Error("network down"));
    render(<SearchDetailPanel idOrName="All Content" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toBe(
      `${DEV_MSG.SR_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-sr-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getSearchDetail.mockRejectedValue("boom");
    render(<SearchDetailPanel idOrName="All Content" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toBe(
      DEV_MSG.SR_DETAIL_ERROR,
    );
  });

  it("disables save until the name is valid on create", () => {
    render(<SearchDetailPanel idOrName={null} onBack={() => undefined} />);
    const save = screen.getByTestId("developer-sr-save") as HTMLButtonElement;
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-sr-name"), {
      target: { value: "has space" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-sr-name"), {
      target: { value: "MySearch" },
    });
    expect(save.disabled).toBe(false);
  });

  it("keeps name read-only on edit", async () => {
    getSearchDetail.mockResolvedValue(sampleDetail);
    render(<SearchDetailPanel idOrName="All Content" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-name")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-sr-name") as HTMLInputElement).disabled).toBe(true);
  });

  it("surfaces 400 invalid name on create", async () => {
    createSearch.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "name cannot contain whitespace" },
    });
    const onSaved = vi.fn();
    render(<SearchDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-sr-name"), {
      target: { value: "MySearch" },
    });
    fireEvent.click(screen.getByTestId("developer-sr-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toContain(
      DEV_MSG.SR_INVALID_NAME,
    );
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toContain(
      "name cannot contain whitespace",
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 409 duplicate name on create", async () => {
    createSearch.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Search already exists: MySearch" },
    });
    const onSaved = vi.fn();
    render(<SearchDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-sr-name"), {
      target: { value: "MySearch" },
    });
    fireEvent.click(screen.getByTestId("developer-sr-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(createSearch).toHaveBeenCalled();
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toContain(
      DEV_MSG.SR_DUPLICATE,
    );
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toContain(
      "already exists",
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 403 non-Admin on create", async () => {
    createSearch.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<SearchDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-sr-name"), {
      target: { value: "MySearch" },
    });
    fireEvent.click(screen.getByTestId("developer-sr-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toContain(
      DEV_MSG.SR_FORBIDDEN,
    );
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toContain(
      "Admin role required",
    );
  });

  it("does not POST create twice when save is clicked twice", async () => {
    let resolveCreate: (value: typeof sampleDetail) => void = () => undefined;
    createSearch.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    render(<SearchDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-sr-name"), {
      target: { value: "MySearch" },
    });
    fireEvent.click(screen.getByTestId("developer-sr-save"));
    fireEvent.click(screen.getByTestId("developer-sr-save"));
    expect(createSearch).toHaveBeenCalledTimes(1);
    resolveCreate({
      name: "MySearch",
      label: "My Search",
      description: "",
      type: "StandardSearch",
      fields: [],
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-editor-notice")).toBeTruthy();
    });
  });

  it("creates a standard search when the name is valid", async () => {
    createSearch.mockResolvedValue({
      name: "MySearch",
      label: "My Search",
      description: "Created via SPA",
      type: "StandardSearch",
      displayFormatId: "1",
      fields: [],
    });
    const onSaved = vi.fn();
    render(<SearchDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-sr-name"), {
      target: { value: "MySearch" },
    });
    fireEvent.change(screen.getByTestId("developer-sr-label"), {
      target: { value: "My Search" },
    });
    fireEvent.change(screen.getByTestId("developer-sr-description"), {
      target: { value: "Created via SPA" },
    });
    fireEvent.change(screen.getByTestId("developer-sr-display-format"), {
      target: { value: "1" },
    });
    fireEvent.click(screen.getByTestId("developer-sr-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(createSearch).toHaveBeenCalledWith(
      expect.objectContaining({
        name: "MySearch",
        label: "My Search",
        description: "Created via SPA",
        type: "StandardSearch",
        displayFormatId: "1",
      }),
    );
    expect(screen.getByTestId("developer-sr-editor-notice").textContent).toBe(DEV_MSG.SR_SAVED);
  });

  it("saves label changes on an existing search", async () => {
    getSearchDetail.mockResolvedValue(sampleDetail);
    saveSearch.mockResolvedValue({
      ...sampleDetail,
      label: "Updated label",
    });
    const onSaved = vi.fn();
    render(
      <SearchDetailPanel idOrName="All Content" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-label")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-sr-label"), {
      target: { value: "Updated label" },
    });
    fireEvent.click(screen.getByTestId("developer-sr-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(saveSearch).toHaveBeenCalledWith(
      "All Content",
      expect.objectContaining({
        name: "All Content",
        label: "Updated label",
        description: "System search",
        type: "StandardSearch",
        displayFormatId: "Default",
      }),
    );
  });

  it("deletes after confirm and omits delete chrome in create mode", async () => {
    getSearchDetail.mockResolvedValue(sampleDetail);
    deleteSearch.mockResolvedValue(undefined);
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    try {
      const onDeleted = vi.fn();
      render(
        <SearchDetailPanel
          idOrName="All Content"
          onBack={() => undefined}
          onDeleted={onDeleted}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("developer-sr-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-sr-delete"));
      await waitFor(() => {
        expect(onDeleted).toHaveBeenCalled();
      });
      expect(deleteSearch).toHaveBeenCalledWith("All Content");
    } finally {
      confirmSpy.mockRestore();
    }
  });

  it("surfaces 404 missing search on delete", async () => {
    getSearchDetail.mockResolvedValue(sampleDetail);
    deleteSearch.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Search not found" },
    });
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    try {
      render(<SearchDetailPanel idOrName="All Content" onBack={() => undefined} />);
      await waitFor(() => {
        expect(screen.getByTestId("developer-sr-delete")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-sr-delete"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-sr-detail-error").textContent).toContain(
        DEV_MSG.SR_NOT_FOUND,
      );
    } finally {
      confirmSpy.mockRestore();
    }
  });

  it("does not show delete on create", () => {
    render(<SearchDetailPanel idOrName={null} onBack={() => undefined} />);
    expect(screen.queryByTestId("developer-sr-delete")).toBeNull();
  });
});
