/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as relationshipTypesApi from "../../../main/ts/api/developer/relationshipTypesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { RelationshipTypesPanel } from "../../../main/ts/developer/RelationshipTypesPanel";

vi.mock("../../../main/ts/api/developer/relationshipTypesApi", () => ({
  listRelationshipTypes: vi.fn(),
  getRelationshipTypeDetail: vi.fn(),
}));

const listRelationshipTypes = relationshipTypesApi.listRelationshipTypes as ReturnType<
  typeof vi.fn
>;
const getRelationshipTypeDetail = relationshipTypesApi.getRelationshipTypeDetail as ReturnType<
  typeof vi.fn
>;

const sampleType = {
  name: "ActiveAssembly",
  label: "Active Assembly",
  category: "rs_activeassembly",
  categoryLabel: "Active Assembly",
  type: "system",
  systemType: true,
  allowCloning: true,
};

const sampleDetail = {
  name: "ActiveAssembly",
  label: "Active Assembly",
  categoryLabel: "Active Assembly",
  type: "system",
  effects: [
    {
      name: "sys_aaEffect",
      activationEndPoint: "owner",
      extensionRef: "Java/global/percussion/sys_aaEffect",
    },
  ],
  systemProperties: [{ name: "rs_allowcloning", value: "yes" }],
  userProperties: [],
  designGaps: ["Relationship type create / update / delete not supported via this API"],
};

describe("RelationshipTypesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listRelationshipTypes.mockReset();
    getRelationshipTypeDetail.mockReset();
  });

  it("lists relationship types and opens detail", async () => {
    listRelationshipTypes.mockResolvedValue([sampleType]);
    getRelationshipTypeDetail.mockResolvedValue(sampleDetail);
    render(<RelationshipTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-table").textContent).toContain("ActiveAssembly");
    fireEvent.click(screen.getByTestId("developer-rt-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-effects-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-rt-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-table")).toBeTruthy();
    });
  });

  it("shows empty state when API returns no relationship types", async () => {
    listRelationshipTypes.mockResolvedValue([]);
    render(<RelationshipTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listRelationshipTypes.mockRejectedValue(new SessionRedirectError());
    render(<RelationshipTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-rt-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listRelationshipTypes.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<RelationshipTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-error").textContent).toBe(`${DEV_MSG.RT_ERROR} (500)`);
  });

  it("shows Error.message via panelErrMsg", async () => {
    listRelationshipTypes.mockRejectedValue(new Error("network down"));
    render(<RelationshipTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-error").textContent).toBe(
      `${DEV_MSG.RT_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-rt-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listRelationshipTypes.mockRejectedValue("boom");
    render(<RelationshipTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-error").textContent).toBe(DEV_MSG.RT_ERROR);
  });
});
