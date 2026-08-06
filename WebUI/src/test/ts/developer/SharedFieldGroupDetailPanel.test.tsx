/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as sharedFieldsApi from "../../../main/ts/api/developer/sharedFieldsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SharedFieldGroupDetailPanel } from "../../../main/ts/developer/SharedFieldGroupDetailPanel";

vi.mock("../../../main/ts/api/developer/sharedFieldsApi", () => ({
  listSharedFieldGroups: vi.fn(),
  getSharedFieldGroupDetail: vi.fn(),
}));

const getSharedFieldGroupDetail = sharedFieldsApi.getSharedFieldGroupDetail as ReturnType<
  typeof vi.fn
>;

const sampleDetail = {
  name: "shared",
  filename: "shared.xml",
  fields: [
    {
      name: "rx_title",
      dataType: "text",
      required: true,
      searchable: true,
      readOnly: false,
      occurrence: "required",
    },
  ],
  designGaps: ["write not supported"],
};

describe("SharedFieldGroupDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getSharedFieldGroupDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getSharedFieldGroupDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<SharedFieldGroupDetailPanel name="shared" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-detail-title").textContent).toBe("shared");
    expect(screen.getByTestId("developer-sf-fields-table")).toBeTruthy();
    expect(screen.getByText("rx_title")).toBeTruthy();
    expect(screen.getByTestId("developer-sf-gaps")).toBeTruthy();
    expect(getSharedFieldGroupDetail).toHaveBeenCalledWith("shared");
    fireEvent.click(screen.getByTestId("developer-sf-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty fields section when detail has none", async () => {
    getSharedFieldGroupDetail.mockResolvedValue({
      ...sampleDetail,
      fields: [],
    });
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-fields-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-fields-empty").textContent).toBe(DEV_MSG.SF_NONE);
    expect(screen.queryByTestId("developer-sf-fields-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getSharedFieldGroupDetail.mockRejectedValue(new SessionRedirectError());
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-sf-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-sf-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getSharedFieldGroupDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toBe(
      `${DEV_MSG.SF_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getSharedFieldGroupDetail.mockRejectedValue(new Error("network down"));
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toBe(
      `${DEV_MSG.SF_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-sf-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getSharedFieldGroupDetail.mockRejectedValue("boom");
    render(<SharedFieldGroupDetailPanel name="shared" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-sf-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-sf-detail-error").textContent).toBe(
      DEV_MSG.SF_DETAIL_ERROR,
    );
  });
});
