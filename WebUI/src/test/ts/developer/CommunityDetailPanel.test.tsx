/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import * as searchesApi from "../../../main/ts/api/developer/searchesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { CommunityDetailPanel } from "../../../main/ts/developer/CommunityDetailPanel";

vi.mock("../../../main/ts/api/developer/assemblyApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/assemblyApi")
  >();
  return {
    ...actual,
    getCommunityDetail: vi.fn(),
    listAvailableRoles: vi.fn(),
    getCommunityVisibility: vi.fn(),
    updateCommunityRoles: vi.fn(),
    createCommunity: vi.fn(),
    deleteCommunity: vi.fn(),
    getCommunityNewSearchDefaults: vi.fn(),
    replaceCommunityNewSearchDefaults: vi.fn(),
  };
});

vi.mock("../../../main/ts/api/developer/searchesApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/searchesApi")
  >();
  return {
    ...actual,
    listSearches: vi.fn(),
  };
});

const getCommunityDetail = assemblyApi.getCommunityDetail as ReturnType<typeof vi.fn>;
const listAvailableRoles = assemblyApi.listAvailableRoles as ReturnType<typeof vi.fn>;
const getCommunityVisibility = assemblyApi.getCommunityVisibility as ReturnType<typeof vi.fn>;
const updateCommunityRoles = assemblyApi.updateCommunityRoles as ReturnType<typeof vi.fn>;
const createCommunity = assemblyApi.createCommunity as ReturnType<typeof vi.fn>;
const deleteCommunity = assemblyApi.deleteCommunity as ReturnType<typeof vi.fn>;
const getCommunityNewSearchDefaults = assemblyApi.getCommunityNewSearchDefaults as ReturnType<
  typeof vi.fn
>;
const replaceCommunityNewSearchDefaults =
  assemblyApi.replaceCommunityNewSearchDefaults as ReturnType<typeof vi.fn>;
const listSearches = searchesApi.listSearches as ReturnType<typeof vi.fn>;

const sampleDetail = {
  name: "Default",
  label: "Default Community",
  description: "System default",
  id: 1001,
  guid: { stringValue: "0-10-1001" },
  roleList: [{ roleName: "Admin", roleId: 1, roleGuid: { stringValue: "0-6-1" } }],
};

const sampleRoles = [
  { roleName: "Admin", roleId: 1, roleGuid: { stringValue: "0-6-1" } },
  { roleName: "Editor", roleId: 2, roleGuid: { stringValue: "0-6-2" } },
];

const sampleSearches = [
  { name: "SimpleSearch", id: 42, label: "Simple" },
  { name: "OtherSearch", id: 43, label: "Other" },
];

describe("CommunityDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getCommunityDetail.mockReset();
    listAvailableRoles.mockReset();
    getCommunityVisibility.mockReset();
    updateCommunityRoles.mockReset();
    createCommunity.mockReset();
    deleteCommunity.mockReset();
    getCommunityNewSearchDefaults.mockReset();
    replaceCommunityNewSearchDefaults.mockReset();
    listSearches.mockReset();
    listAvailableRoles.mockResolvedValue(sampleRoles);
    getCommunityVisibility.mockResolvedValue([]);
    getCommunityNewSearchDefaults.mockResolvedValue({ searches: [] });
    replaceCommunityNewSearchDefaults.mockResolvedValue({ searches: [] });
    listSearches.mockResolvedValue(sampleSearches);
  });

  it("loads detail on success and supports back", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<CommunityDetailPanel idOrName="Default" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-detail-title").textContent).toContain(
      "Default Community",
    );
    expect(screen.getByTestId("developer-comm-roles-table")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-visibility-empty")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-comm-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty visibility when API returns no objects", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    getCommunityVisibility.mockResolvedValue([]);
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-visibility-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-comm-visibility-table")).toBeNull();
    expect(screen.getByTestId("developer-comm-visibility-filters")).toBeTruthy();
  });

  it("re-fetches visibility with type filter and shows type-empty state", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    getCommunityVisibility
      .mockResolvedValueOnce([
        { name: "percPage", label: "Page", type: "NODEDEF" },
        { name: "rffSnTitle", label: "Title", type: "TEMPLATE" },
      ])
      .mockResolvedValueOnce([]);
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-visibility-table")).toBeTruthy();
    });
    expect(getCommunityVisibility).toHaveBeenCalledWith(sampleDetail.guid);

    fireEvent.change(screen.getByTestId("developer-comm-visibility-type-filter"), {
      target: { value: "WORKFLOW" },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-visibility-empty-type")).toBeTruthy();
    });
    expect(getCommunityVisibility).toHaveBeenLastCalledWith(sampleDetail.guid, "WORKFLOW");
    expect(screen.queryByTestId("developer-comm-visibility-table")).toBeNull();
  });

  it("filters visibility rows client-side by name and shows name-empty state", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    getCommunityVisibility.mockResolvedValue([
      { name: "percPage", label: "Page", type: "NODEDEF" },
      { name: "rffSnTitle", label: "Title Snippet", type: "TEMPLATE" },
    ]);
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-visibility-summary").textContent).toMatch(
        /2 visible objects/i,
      );
    });

    fireEvent.change(screen.getByTestId("developer-comm-visibility-name-filter"), {
      target: { value: "title" },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-visibility-summary").textContent).toMatch(
        /Showing 1 of 2/i,
      );
    });
    expect(screen.getByTestId("developer-comm-visibility-table").textContent).toMatch(
      /rffSnTitle/,
    );
    expect(screen.getByTestId("developer-comm-visibility-table").textContent).not.toMatch(
      /percPage/,
    );

    fireEvent.change(screen.getByTestId("developer-comm-visibility-name-filter"), {
      target: { value: "zzz-no-match" },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-visibility-empty-name")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-comm-visibility-table")).toBeNull();
    // Name filter is client-side only — no extra fetch
    expect(getCommunityVisibility).toHaveBeenCalledTimes(1);
  });

  it("shows dirty cue and save feedback with role count", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    updateCommunityRoles.mockResolvedValue({
      ...sampleDetail,
      roleList: [
        { roleName: "Admin", roleId: 1, roleGuid: { stringValue: "0-6-1" } },
        { roleName: "Editor", roleId: 2, roleGuid: { stringValue: "0-6-2" } },
      ],
    });
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-roles-table")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-comm-roles-dirty")).toBeNull();

    // Keys prefer roleGuid.stringValue when present (see roleKey in panel).
    fireEvent.click(screen.getByTestId("developer-comm-role-check-0-6-2"));
    expect(screen.getByTestId("developer-comm-roles-dirty")).toBeTruthy();

    fireEvent.click(screen.getByTestId("developer-comm-roles-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-notice").textContent).toMatch(
        /2 roles/i,
      );
    });
    expect(updateCommunityRoles).toHaveBeenCalledWith("Default", [
      { roleName: "Admin", roleId: 1, roleGuid: { stringValue: "0-6-1" } },
      { roleName: "Editor", roleId: 2, roleGuid: { stringValue: "0-6-2" } },
    ]);
    expect(screen.queryByTestId("developer-comm-roles-dirty")).toBeNull();
  });

  it("treats Jackson one-item roleList object as a single membership (SE-02)", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    updateCommunityRoles.mockResolvedValue({
      ...sampleDetail,
      // Live CXF often emits a bare CommunityRole object when size === 1.
      roleList: {
        roleName: "Editor",
        roleId: 2,
        roleGuid: { stringValue: "0-6-2" },
      } as never,
    });
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-roles-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-comm-role-check-0-6-2"));
    fireEvent.click(screen.getByTestId("developer-comm-roles-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-notice").textContent).toMatch(
        /1 roles/i,
      );
    });
    expect(screen.getByTestId("developer-comm-role-check-0-6-2")).toBeChecked();
  });

  it("unassigns a role and can clear all memberships on save (SE-02)", async () => {
    getCommunityDetail.mockResolvedValue({
      ...sampleDetail,
      roleList: [
        { roleName: "Admin", roleId: 1, roleGuid: { stringValue: "0-6-1" } },
        { roleName: "Editor", roleId: 2, roleGuid: { stringValue: "0-6-2" } },
      ],
    });
    updateCommunityRoles
      .mockResolvedValueOnce({
        ...sampleDetail,
        roleList: [{ roleName: "Editor", roleId: 2, roleGuid: { stringValue: "0-6-2" } }],
      })
      .mockResolvedValueOnce({
        ...sampleDetail,
        roleList: [],
      });

    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-roles-table")).toBeTruthy();
    });

    // Unassign Admin (was checked from detail membership).
    fireEvent.click(screen.getByTestId("developer-comm-role-check-0-6-1"));
    expect(screen.getByTestId("developer-comm-roles-dirty")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-comm-roles-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-notice").textContent).toMatch(
        /1 roles/i,
      );
    });
    expect(updateCommunityRoles).toHaveBeenLastCalledWith("Default", [
      { roleName: "Editor", roleId: 2, roleGuid: { stringValue: "0-6-2" } },
    ]);

    // Clear remaining membership.
    fireEvent.click(screen.getByTestId("developer-comm-role-check-0-6-2"));
    fireEvent.click(screen.getByTestId("developer-comm-roles-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-notice").textContent).toMatch(
        /0 roles/i,
      );
    });
    expect(updateCommunityRoles).toHaveBeenLastCalledWith("Default", []);
    expect(screen.queryByTestId("developer-comm-roles-dirty")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getCommunityDetail.mockRejectedValue(new SessionRedirectError());
    listAvailableRoles.mockRejectedValue(new SessionRedirectError());
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-comm-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-comm-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getCommunityDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    listAvailableRoles.mockResolvedValue(sampleRoles);
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-detail-error").textContent).toBe(
      `${DEV_MSG.COMM_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getCommunityDetail.mockRejectedValue(new Error("network down"));
    listAvailableRoles.mockResolvedValue(sampleRoles);
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-detail-error").textContent).toBe(
      `${DEV_MSG.COMM_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-comm-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getCommunityDetail.mockRejectedValue("boom");
    listAvailableRoles.mockResolvedValue(sampleRoles);
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-detail-error").textContent).toBe(
      DEV_MSG.COMM_DETAIL_ERROR,
    );
  });

  it("shows 404 missing community via panelErrMsg", async () => {
    getCommunityDetail.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Community not found" },
    });
    render(<CommunityDetailPanel idOrName="missing" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-detail-error").textContent).toContain(
      DEV_MSG.COMM_DETAIL_ERROR,
    );
    expect(screen.getByTestId("developer-comm-detail-error").textContent).toContain(
      "Community not found",
    );
    expect(screen.queryByTestId("developer-comm-roles-save")).toBeNull();
  });

  it("disables create until the name is non-blank", () => {
    render(<CommunityDetailPanel idOrName={null} onBack={() => undefined} />);
    const create = screen.getByTestId("developer-comm-create") as HTMLButtonElement;
    expect(create.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-comm-name"), {
      target: { value: "   " },
    });
    expect(create.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-comm-name"), {
      target: { value: "QA Community" },
    });
    expect(create.disabled).toBe(false);
  });

  it("surfaces 400 blank name on create", async () => {
    createCommunity.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "name cannot be null or empty" },
    });
    const onSaved = vi.fn();
    render(
      <CommunityDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />,
    );
    fireEvent.change(screen.getByTestId("developer-comm-name"), {
      target: { value: "x" },
    });
    fireEvent.click(screen.getByTestId("developer-comm-create"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-error")).toBeTruthy();
    });
    expect(createCommunity).toHaveBeenCalled();
    expect(screen.getByTestId("developer-comm-detail-error").textContent).toContain(
      DEV_MSG.COMM_NAME_INVALID,
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 409 duplicate name on create", async () => {
    createCommunity.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Community already exists: Default" },
    });
    const onSaved = vi.fn();
    render(
      <CommunityDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />,
    );
    fireEvent.change(screen.getByTestId("developer-comm-name"), {
      target: { value: "Default" },
    });
    fireEvent.click(screen.getByTestId("developer-comm-create"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-detail-error").textContent).toContain(
      DEV_MSG.COMM_DUPLICATE,
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("surfaces 403 non-Admin on create", async () => {
    createCommunity.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<CommunityDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-comm-name"), {
      target: { value: "QA Community" },
    });
    fireEvent.click(screen.getByTestId("developer-comm-create"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-detail-error").textContent).toContain(
      DEV_MSG.COMM_FORBIDDEN,
    );
  });

  it("creates a community and keeps role-association chrome", async () => {
    createCommunity.mockResolvedValue({
      name: "QA Community",
      id: 42,
      guid: { stringValue: "0-13-42" },
    });
    getCommunityDetail.mockResolvedValue({
      name: "QA Community",
      id: 42,
      guid: { stringValue: "0-13-42" },
      roleList: [],
    });
    const onSaved = vi.fn();
    render(
      <CommunityDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />,
    );
    fireEvent.change(screen.getByTestId("developer-comm-name"), {
      target: { value: "QA Community" },
    });
    fireEvent.click(screen.getByTestId("developer-comm-create"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-notice").textContent).toContain(
        DEV_MSG.COMM_CREATED,
      );
    });
    expect(createCommunity).toHaveBeenCalledWith("QA Community");
    expect(onSaved).toHaveBeenCalled();
    expect(screen.getByTestId("developer-comm-roles-save")).toBeTruthy();
    expect(screen.getByTestId("developer-comm-delete")).toBeTruthy();
  });

  it("surfaces 409 in-use delete without ignoredependencies and does not steal", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    deleteCommunity.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Community has dependencies" },
    });
    const onDeleted = vi.fn();
    render(
      <CommunityDetailPanel
        idOrName="Default"
        onBack={() => undefined}
        onDeleted={onDeleted}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-comm-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-error")).toBeTruthy();
    });
    expect(deleteCommunity).toHaveBeenCalledWith(sampleDetail.guid, false);
    expect(screen.getByTestId("developer-comm-detail-error").textContent).toContain(
      DEV_MSG.COMM_IN_USE,
    );
    expect(onDeleted).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-comm-detail-title")).toBeTruthy();
  });

  it("surfaces 403 non-Admin on delete", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    deleteCommunity.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-comm-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-detail-error").textContent).toContain(
      DEV_MSG.COMM_FORBIDDEN,
    );
  });

  it("delete success returns to catalog via onDeleted", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    deleteCommunity.mockResolvedValue(undefined);
    const onDeleted = vi.fn();
    render(
      <CommunityDetailPanel
        idOrName="Default"
        onBack={() => undefined}
        onDeleted={onDeleted}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-comm-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(onDeleted).toHaveBeenCalled();
    });
    expect(deleteCommunity).toHaveBeenCalledWith(sampleDetail.guid, false);
  });

  it("loads new-search defaults and catalog picker", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    getCommunityNewSearchDefaults.mockResolvedValue({
      searches: [{ name: "SimpleSearch", id: 42 }],
    });
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-nsd-table")).toBeTruthy();
    });
    expect(getCommunityNewSearchDefaults).toHaveBeenCalledWith("Default");
    expect(listSearches).toHaveBeenCalled();
    const simple = screen.getByTestId(
      "developer-comm-nsd-check-name:simplesearch",
    ) as HTMLInputElement;
    const other = screen.getByTestId(
      "developer-comm-nsd-check-name:othersearch",
    ) as HTMLInputElement;
    expect(simple.checked).toBe(true);
    expect(other.checked).toBe(false);
  });

  it("saves replaced defaults and shows search count", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    getCommunityNewSearchDefaults.mockResolvedValue({ searches: [] });
    replaceCommunityNewSearchDefaults.mockResolvedValue({
      searches: [{ name: "OtherSearch", id: 43 }],
    });
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-nsd-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-comm-nsd-check-name:othersearch"));
    expect(screen.getByTestId("developer-comm-nsd-dirty")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-comm-nsd-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-notice").textContent).toMatch(
        /1 searches/i,
      );
    });
    expect(replaceCommunityNewSearchDefaults).toHaveBeenCalledWith("Default", [
      { name: "OtherSearch", id: 43 },
    ]);
    expect(screen.queryByTestId("developer-comm-nsd-dirty")).toBeNull();
  });

  it("saves empty set to clear defaults", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    getCommunityNewSearchDefaults.mockResolvedValue({
      searches: [{ name: "SimpleSearch", id: 42 }],
    });
    replaceCommunityNewSearchDefaults.mockResolvedValue({ searches: [] });
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-nsd-check-name:simplesearch")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-comm-nsd-check-name:simplesearch"));
    fireEvent.click(screen.getByTestId("developer-comm-nsd-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-notice").textContent).toBe(
        DEV_MSG.COMM_NSD_CLEARED,
      );
    });
    expect(replaceCommunityNewSearchDefaults).toHaveBeenCalledWith("Default", []);
  });

  it("surfaces 400 unknown search on save", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    replaceCommunityNewSearchDefaults.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "Unknown search: Nope" },
    });
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-nsd-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-comm-nsd-check-name:simplesearch"));
    fireEvent.click(screen.getByTestId("developer-comm-nsd-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-detail-error").textContent).toContain(
      DEV_MSG.COMM_NSD_UNKNOWN_SEARCH,
    );
  });

  it("surfaces 403 non-Admin on load", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    getCommunityNewSearchDefaults.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-nsd-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-nsd-error").textContent).toContain(
      DEV_MSG.COMM_FORBIDDEN,
    );
  });

  it("surfaces 403 non-Admin on save", async () => {
    getCommunityDetail.mockResolvedValue(sampleDetail);
    replaceCommunityNewSearchDefaults.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<CommunityDetailPanel idOrName="Default" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-nsd-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-comm-nsd-check-name:simplesearch"));
    fireEvent.click(screen.getByTestId("developer-comm-nsd-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-detail-error").textContent).toContain(
      DEV_MSG.COMM_FORBIDDEN,
    );
  });
});
