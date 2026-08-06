/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as relationshipTypesApi from "../../../main/ts/api/developer/relationshipTypesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { RelationshipTypeDetailPanel } from "../../../main/ts/developer/RelationshipTypeDetailPanel";

vi.mock("../../../main/ts/api/developer/relationshipTypesApi", () => ({
  listRelationshipTypes: vi.fn(),
  getRelationshipTypeDetail: vi.fn(),
}));

const getRelationshipTypeDetail = relationshipTypesApi.getRelationshipTypeDetail as ReturnType<
  typeof vi.fn
>;

const sampleDetail = {
  name: "ActiveAssembly",
  label: "Active Assembly",
  category: "rs_activeassembly",
  categoryLabel: "Active Assembly",
  type: "system",
  description: "AA relationship",
  effects: [
    {
      name: "sys_PublishRequired",
      activationEndPoint: "owner",
      extensionRef: "Java/global/percussion/relationship/sys_PublishRequired",
    },
  ],
  systemProperties: [{ name: "rs_useownerrevision", value: "yes" }],
  userProperties: [{ name: "slotid", value: "0" }],
  designGaps: ["gap-a"],
};

describe("RelationshipTypeDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getRelationshipTypeDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getRelationshipTypeDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<RelationshipTypeDetailPanel idOrName="ActiveAssembly" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-detail-title").textContent).toContain(
      "Active Assembly",
    );
    expect(screen.getByTestId("developer-rt-effects-table")).toBeTruthy();
    expect(screen.getByTestId("developer-rt-sysprops-table")).toBeTruthy();
    expect(screen.getByTestId("developer-rt-userprops-table")).toBeTruthy();
    expect(screen.getByTestId("developer-rt-gaps").textContent).toContain("gap-a");
    expect(getRelationshipTypeDetail).toHaveBeenCalledWith("ActiveAssembly");
    fireEvent.click(screen.getByTestId("developer-rt-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty effects/props sections when detail has none", async () => {
    getRelationshipTypeDetail.mockResolvedValue({
      ...sampleDetail,
      effects: [],
      systemProperties: [],
      userProperties: [],
    });
    render(<RelationshipTypeDetailPanel idOrName="ActiveAssembly" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-effects")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-effects").textContent).toContain(DEV_MSG.RT_NONE);
    expect(screen.getByTestId("developer-rt-sysprops").textContent).toContain(DEV_MSG.RT_NONE);
    expect(screen.getByTestId("developer-rt-userprops").textContent).toContain(DEV_MSG.RT_NONE);
    expect(screen.queryByTestId("developer-rt-effects-table")).toBeNull();
    expect(screen.queryByTestId("developer-rt-sysprops-table")).toBeNull();
    expect(screen.queryByTestId("developer-rt-userprops-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getRelationshipTypeDetail.mockRejectedValue(new SessionRedirectError());
    render(<RelationshipTypeDetailPanel idOrName="ActiveAssembly" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-rt-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-rt-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getRelationshipTypeDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<RelationshipTypeDetailPanel idOrName="ActiveAssembly" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-detail-error").textContent).toBe(
      `${DEV_MSG.RT_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getRelationshipTypeDetail.mockRejectedValue(new Error("network down"));
    render(<RelationshipTypeDetailPanel idOrName="ActiveAssembly" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-detail-error").textContent).toBe(
      `${DEV_MSG.RT_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-rt-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getRelationshipTypeDetail.mockRejectedValue("boom");
    render(<RelationshipTypeDetailPanel idOrName="ActiveAssembly" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-detail-error").textContent).toBe(
      DEV_MSG.RT_DETAIL_ERROR,
    );
  });
});
