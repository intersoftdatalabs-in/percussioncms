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
    expect(createSearch.mock.calls[0][0]).not.toHaveProperty("customSearch");
    expect(createSearch.mock.calls[0][0]).not.toHaveProperty("url");
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
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(onDeleted).toHaveBeenCalled();
    });
    expect(deleteSearch).toHaveBeenCalledWith("All Content");
  });

  it("surfaces 404 missing search on delete", async () => {
    getSearchDetail.mockResolvedValue(sampleDetail);
    deleteSearch.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Search not found" },
    });
    render(<SearchDetailPanel idOrName="All Content" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-sr-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toContain(
      DEV_MSG.SR_NOT_FOUND,
    );
  });

  it("does not show delete on create", () => {
    render(<SearchDetailPanel idOrName={null} onBack={() => undefined} />);
    expect(screen.queryByTestId("developer-sr-delete")).toBeNull();
  });

  it("cancel on in-app confirm does not delete", async () => {
    getSearchDetail.mockResolvedValue(sampleDetail);
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
    expect(screen.getByTestId("developer-catalog-confirm-dialog").getAttribute("role")).toBe(
      "dialog",
    );
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-cancel"));
    expect(screen.queryByTestId("developer-catalog-confirm-dialog")).toBeNull();
    expect(deleteSearch).not.toHaveBeenCalled();
    expect(onDeleted).not.toHaveBeenCalled();
  });

  it("saves PUT body fields on a user search", async () => {
    const userDetail = {
      ...sampleDetail,
      name: "qa4110srch",
      label: "User search",
      fields: [{ fieldName: "sys_title", operator: "like", fieldValue: "", fieldType: "Text" }],
    };
    getSearchDetail.mockResolvedValue(userDetail);
    saveSearch.mockResolvedValue({
      ...userDetail,
      fields: [
        userDetail.fields[0],
        { fieldName: "sys_contentid", operator: "like", fieldValue: "", fieldType: "Text" },
      ],
    });
    render(<SearchDetailPanel idOrName="qa4110srch" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-field-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-sr-field-source"), {
      target: { value: "sys_contentid" },
    });
    fireEvent.click(screen.getByTestId("developer-sr-field-add"));
    fireEvent.click(screen.getByTestId("developer-sr-fields-save"));
    await waitFor(() => {
      expect(saveSearch).toHaveBeenCalled();
    });
    const [, body] = saveSearch.mock.calls[0];
    expect(body.fields.map((f: { fieldName?: string }) => f.fieldName)).toEqual([
      "sys_title",
      "sys_contentid",
    ]);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-editor-notice").textContent).toBe(
        DEV_MSG.SR_FIELDS_SAVED,
      );
    });
  });

  it("surfaces 400 invalid source from PUT", async () => {
    const userDetail = { ...sampleDetail, name: "qa4110srch" };
    getSearchDetail.mockResolvedValue(userDetail);
    saveSearch.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: "unknown field: has space",
    });
    render(<SearchDetailPanel idOrName="qa4110srch" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-field-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-sr-field-source"), {
      target: { value: "sys_contentid" },
    });
    fireEvent.click(screen.getByTestId("developer-sr-field-add"));
    fireEvent.click(screen.getByTestId("developer-sr-fields-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toContain(
      DEV_MSG.SR_FIELDS_INVALID_SOURCE,
    );
  });

  it("surfaces 403 non-Admin from PUT", async () => {
    const userDetail = { ...sampleDetail, name: "qa4110srch" };
    getSearchDetail.mockResolvedValue(userDetail);
    saveSearch.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: "Admin role required",
    });
    render(<SearchDetailPanel idOrName="qa4110srch" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-field-editor")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-sr-field-source"), {
      target: { value: "sys_workflow" },
    });
    fireEvent.click(screen.getByTestId("developer-sr-field-add"));
    fireEvent.click(screen.getByTestId("developer-sr-fields-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toContain(
      DEV_MSG.SR_FIELDS_FORBIDDEN,
    );
  });

  it("does not mutate a packaged/system search", async () => {
    getSearchDetail.mockResolvedValue({
      ...sampleDetail,
      name: "Default_Search",
      label: "Default CX New Search",
    });
    render(<SearchDetailPanel idOrName="Default_Search" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-fields-readonly")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-sr-field-editor")).toBeNull();
    expect(screen.queryByTestId("developer-sr-fields-save")).toBeNull();
    expect((screen.getByTestId("developer-sr-label") as HTMLInputElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-sr-type") as HTMLSelectElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-sr-save") as HTMLButtonElement).disabled).toBe(true);
    expect(saveSearch).not.toHaveBeenCalled();
  });

  it("requires a URL before saving a CustomSearch", () => {
    render(<SearchDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-sr-name"), {
      target: { value: "MyCustom" },
    });
    fireEvent.change(screen.getByTestId("developer-sr-type"), {
      target: { value: "CustomSearch" },
    });
    expect(screen.getByTestId("developer-sr-url")).toBeTruthy();
    expect((screen.getByTestId("developer-sr-save") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-sr-url"), {
      target: { value: "   " },
    });
    expect((screen.getByTestId("developer-sr-save") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-sr-url"), {
      target: { value: "app/has space.xml" },
    });
    expect((screen.getByTestId("developer-sr-save") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-sr-url"), {
      target: { value: "/Rhythmyx/sys_cxSupport/custom.xml" },
    });
    expect((screen.getByTestId("developer-sr-save") as HTMLButtonElement).disabled).toBe(false);
  });

  it("creates a custom URL search with url and customSearch", async () => {
    createSearch.mockResolvedValue({
      name: "MyCustom",
      label: "My Custom",
      description: "Created via SPA",
      type: "CustomSearch",
      customSearch: true,
      url: "/Rhythmyx/sys_cxSupport/custom.xml",
      fields: [],
    });
    const onSaved = vi.fn();
    render(<SearchDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-sr-name"), {
      target: { value: "MyCustom" },
    });
    fireEvent.change(screen.getByTestId("developer-sr-label"), {
      target: { value: "My Custom" },
    });
    fireEvent.change(screen.getByTestId("developer-sr-type"), {
      target: { value: "CustomSearch" },
    });
    fireEvent.change(screen.getByTestId("developer-sr-url"), {
      target: { value: "/Rhythmyx/sys_cxSupport/custom.xml" },
    });
    fireEvent.click(screen.getByTestId("developer-sr-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(createSearch).toHaveBeenCalledWith(
      expect.objectContaining({
        name: "MyCustom",
        label: "My Custom",
        type: "CustomSearch",
        customSearch: true,
        url: "/Rhythmyx/sys_cxSupport/custom.xml",
      }),
    );
    expect(onSaved.mock.calls[0][0]).toEqual(
      expect.objectContaining({
        url: "/Rhythmyx/sys_cxSupport/custom.xml",
        customSearch: true,
      }),
    );
    expect(screen.queryByTestId("developer-sr-field-editor")).toBeNull();
    expect(screen.getByTestId("developer-sr-fields-custom-url")).toBeTruthy();
  });

  it("surfaces 400 missing URL on custom create", async () => {
    createSearch.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "url is required for CustomSearch" },
    });
    const onSaved = vi.fn();
    render(<SearchDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-sr-name"), {
      target: { value: "MyCustom" },
    });
    fireEvent.change(screen.getByTestId("developer-sr-type"), {
      target: { value: "CustomSearch" },
    });
    fireEvent.change(screen.getByTestId("developer-sr-url"), {
      target: { value: "/Rhythmyx/app/custom.xml" },
    });
    fireEvent.click(screen.getByTestId("developer-sr-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sr-detail-error").textContent).toContain(
      DEV_MSG.SR_INVALID_URL,
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("saves URL changes on an existing custom search", async () => {
    const customDetail = {
      name: "qa4222custom",
      label: "Custom URL search",
      description: "URL search",
      type: "CustomSearch",
      customSearch: true,
      url: "/Rhythmyx/old.xml",
      displayFormatId: "Default",
      guid: { stringValue: "0-26-99" },
      fields: [],
    };
    getSearchDetail.mockResolvedValue(customDetail);
    saveSearch.mockResolvedValue({
      ...customDetail,
      url: "/Rhythmyx/new.xml",
    });
    const onSaved = vi.fn();
    render(
      <SearchDetailPanel idOrName="qa4222custom" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-sr-url")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-sr-url") as HTMLInputElement).value).toBe(
      "/Rhythmyx/old.xml",
    );
    expect(screen.queryByTestId("developer-sr-field-editor")).toBeNull();
    fireEvent.change(screen.getByTestId("developer-sr-url"), {
      target: { value: "/Rhythmyx/new.xml" },
    });
    fireEvent.click(screen.getByTestId("developer-sr-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(saveSearch).toHaveBeenCalledWith(
      "qa4222custom",
      expect.objectContaining({
        name: "qa4222custom",
        type: "CustomSearch",
        customSearch: true,
        url: "/Rhythmyx/new.xml",
      }),
    );
  });
});
