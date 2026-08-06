/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { CommunityDetailPanel } from "../../../main/ts/developer/CommunityDetailPanel";

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  getCommunityDetail: vi.fn(),
  listAvailableRoles: vi.fn(),
  getCommunityVisibility: vi.fn(),
  updateCommunityRoles: vi.fn(),
}));

const getCommunityDetail = assemblyApi.getCommunityDetail as ReturnType<typeof vi.fn>;
const listAvailableRoles = assemblyApi.listAvailableRoles as ReturnType<typeof vi.fn>;
const getCommunityVisibility = assemblyApi.getCommunityVisibility as ReturnType<typeof vi.fn>;
const updateCommunityRoles = assemblyApi.updateCommunityRoles as ReturnType<typeof vi.fn>;

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

describe("CommunityDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getCommunityDetail.mockReset();
    listAvailableRoles.mockReset();
    getCommunityVisibility.mockReset();
    updateCommunityRoles.mockReset();
    listAvailableRoles.mockResolvedValue(sampleRoles);
    getCommunityVisibility.mockResolvedValue([]);
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
});
