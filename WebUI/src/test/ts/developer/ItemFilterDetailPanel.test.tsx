/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as itemFiltersApi from "../../../main/ts/api/developer/itemFiltersApi";
import { ItemFilterDetailPanel } from "../../../main/ts/developer/ItemFilterDetailPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/itemFiltersApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/itemFiltersApi")
  >();
  return {
    ...actual,
    listItemFilters: vi.fn(),
    getItemFilterDetail: vi.fn(),
    createItemFilter: vi.fn(),
    updateItemFilter: vi.fn(),
    deleteItemFilter: vi.fn(),
  };
});

const getItemFilterDetail = itemFiltersApi.getItemFilterDetail as ReturnType<typeof vi.fn>;
const createItemFilter = itemFiltersApi.createItemFilter as ReturnType<typeof vi.fn>;
const updateItemFilter = itemFiltersApi.updateItemFilter as ReturnType<typeof vi.fn>;
const deleteItemFilter = itemFiltersApi.deleteItemFilter as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "publicItems",
  description: "Public content filter",
  filterId: { stringValue: "0-1-50" },
  legacyAuthtype: 1,
  parentFilter: { name: "allItems" },
  rules: [
    {
      name: "sys_filterByPublishable",
      ruleId: { stringValue: "0-1-51" },
      params: [{ name: "state", value: "public" }],
    },
  ],
};

describe("ItemFilterDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getItemFilterDetail.mockReset();
    createItemFilter.mockReset();
    updateItemFilter.mockReset();
    deleteItemFilter.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getItemFilterDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<ItemFilterDetailPanel idOrName="publicItems" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-title").textContent).toContain("publicItems");
    expect(screen.getByTestId("developer-if-rules-table")).toBeTruthy();
    expect(screen.getByTestId("developer-if-gaps")).toBeTruthy();
    expect(getItemFilterDetail).toHaveBeenCalledWith("publicItems");
    fireEvent.click(screen.getByTestId("developer-if-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty rules section when detail has none", async () => {
    getItemFilterDetail.mockResolvedValue({ ...sampleDetail, rules: [] });
    render(<ItemFilterDetailPanel idOrName="publicItems" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-rules-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-if-rules-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getItemFilterDetail.mockRejectedValue(new SessionRedirectError());
    render(<ItemFilterDetailPanel idOrName="publicItems" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-if-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-if-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getItemFilterDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ItemFilterDetailPanel idOrName="publicItems" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-error").textContent).toBe(
      `${DEV_MSG.IF_DETAIL_ERROR} (500)`,
    );
  });

  it("shows 404 missing filter via panelErrMsg", async () => {
    getItemFilterDetail.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Item filter not found" },
    });
    render(<ItemFilterDetailPanel idOrName="missing" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-error").textContent).toContain(
      DEV_MSG.IF_DETAIL_ERROR,
    );
    expect(screen.getByTestId("developer-if-detail-error").textContent).toContain(
      "Item filter not found",
    );
    expect(screen.queryByTestId("developer-if-save")).toBeNull();
  });

  it("shows 404 status when missing filter has no body message", async () => {
    getItemFilterDetail.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    render(<ItemFilterDetailPanel idOrName="missing" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-error").textContent).toBe(
      `${DEV_MSG.IF_DETAIL_ERROR} (404)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getItemFilterDetail.mockRejectedValue(new Error("network down"));
    render(<ItemFilterDetailPanel idOrName="publicItems" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-error").textContent).toBe(
      `${DEV_MSG.IF_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-if-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getItemFilterDetail.mockRejectedValue("boom");
    render(<ItemFilterDetailPanel idOrName="publicItems" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-error").textContent).toBe(
      DEV_MSG.IF_DETAIL_ERROR,
    );
  });

  it("disables save until the name is valid on create", () => {
    render(<ItemFilterDetailPanel idOrName={null} onBack={() => undefined} />);
    const save = screen.getByTestId("developer-if-save") as HTMLButtonElement;
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-if-name"), {
      target: { value: "has space" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-if-name"), {
      target: { value: "previewPublic" },
    });
    expect(save.disabled).toBe(false);
  });

  it("keeps name read-only on edit", async () => {
    getItemFilterDetail.mockResolvedValue(sampleDetail);
    render(<ItemFilterDetailPanel idOrName="publicItems" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-name")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-if-name") as HTMLInputElement).disabled).toBe(true);
  });

  it("surfaces 400 invalid name on create", async () => {
    createItemFilter.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "name cannot contain whitespace" },
    });
    const onSaved = vi.fn();
    render(
      <ItemFilterDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />,
    );
    fireEvent.change(screen.getByTestId("developer-if-name"), {
      target: { value: "previewPublic" },
    });
    fireEvent.click(screen.getByTestId("developer-if-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-error").textContent).toContain(
      DEV_MSG.IF_INVALID_NAME,
    );
    expect(screen.getByTestId("developer-if-detail-error").textContent).toContain(
      "name cannot contain whitespace",
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 409 duplicate name on create", async () => {
    createItemFilter.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Item filter already exists: preview" },
    });
    const onSaved = vi.fn();
    render(
      <ItemFilterDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />,
    );
    fireEvent.change(screen.getByTestId("developer-if-name"), {
      target: { value: "preview" },
    });
    fireEvent.click(screen.getByTestId("developer-if-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-error")).toBeTruthy();
    });
    expect(createItemFilter).toHaveBeenCalled();
    expect(screen.getByTestId("developer-if-detail-error").textContent).toContain(
      DEV_MSG.IF_DUPLICATE,
    );
    expect(screen.getByTestId("developer-if-detail-error").textContent).toContain(
      "already exists",
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 403 non-Admin on create", async () => {
    createItemFilter.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<ItemFilterDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-if-name"), {
      target: { value: "previewPublic" },
    });
    fireEvent.click(screen.getByTestId("developer-if-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-if-detail-error").textContent).toContain(
      DEV_MSG.IF_FORBIDDEN,
    );
    expect(screen.getByTestId("developer-if-detail-error").textContent).toContain(
      "Admin role required",
    );
  });

  it("does not POST create twice when save is clicked twice", async () => {
    let resolveCreate: (value: typeof sampleDetail) => void = () => undefined;
    createItemFilter.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    render(<ItemFilterDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-if-name"), {
      target: { value: "previewPublic" },
    });
    fireEvent.click(screen.getByTestId("developer-if-save"));
    fireEvent.click(screen.getByTestId("developer-if-save"));
    expect(createItemFilter).toHaveBeenCalledTimes(1);
    resolveCreate({
      name: "previewPublic",
      description: "",
      rules: [],
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-editor-notice")).toBeTruthy();
    });
  });

  it("creates a filter when the name is valid", async () => {
    createItemFilter.mockResolvedValue({
      name: "previewPublic",
      description: "Preview public",
      rules: [],
    });
    const onSaved = vi.fn();
    render(
      <ItemFilterDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />,
    );
    fireEvent.change(screen.getByTestId("developer-if-name"), {
      target: { value: "previewPublic" },
    });
    fireEvent.change(screen.getByTestId("developer-if-description"), {
      target: { value: "Preview public" },
    });
    fireEvent.click(screen.getByTestId("developer-if-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(createItemFilter).toHaveBeenCalledWith(
      expect.objectContaining({
        name: "previewPublic",
        description: "Preview public",
      }),
    );
    expect(screen.getByTestId("developer-if-editor-notice").textContent).toBe(DEV_MSG.IF_SAVED);
  });

  it("saves description changes on an existing filter and round-trips rules", async () => {
    getItemFilterDetail.mockResolvedValue(sampleDetail);
    updateItemFilter.mockResolvedValue({
      ...sampleDetail,
      description: "Updated public",
    });
    const onSaved = vi.fn();
    render(
      <ItemFilterDetailPanel
        idOrName="publicItems"
        onBack={() => undefined}
        onSaved={onSaved}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-description")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-if-description"), {
      target: { value: "Updated public" },
    });
    fireEvent.click(screen.getByTestId("developer-if-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(updateItemFilter).toHaveBeenCalledWith(
      "publicItems",
      expect.objectContaining({
        name: "publicItems",
        description: "Updated public",
        rules: sampleDetail.rules,
        parentFilter: { name: "allItems" },
        legacyAuthtype: 1,
      }),
    );
  });

  it("deletes after confirm and omits delete chrome in create mode", async () => {
    getItemFilterDetail.mockResolvedValue(sampleDetail);
    deleteItemFilter.mockResolvedValue(undefined);
    const onDeleted = vi.fn();
    render(
      <ItemFilterDetailPanel
        idOrName="publicItems"
        onBack={() => undefined}
        onDeleted={onDeleted}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-if-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(onDeleted).toHaveBeenCalled();
    });
    expect(deleteItemFilter).toHaveBeenCalledWith("publicItems");
  });

  it("does not show delete on create", () => {
    render(<ItemFilterDetailPanel idOrName={null} onBack={() => undefined} />);
    expect(screen.queryByTestId("developer-if-delete")).toBeNull();
  });
});
