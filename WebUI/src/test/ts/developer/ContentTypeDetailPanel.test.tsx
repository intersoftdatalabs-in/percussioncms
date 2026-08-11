/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as contentTypesApi from "../../../main/ts/api/developer/contentTypesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ContentTypeDetailPanel } from "../../../main/ts/developer/ContentTypeDetailPanel";

vi.mock("../../../main/ts/api/developer/contentTypesApi", () => ({
  getContentTypeDetail: vi.fn(),
  updateContentTypeDetail: vi.fn(),
}));

// ObjectAclSection loads ACL via separate API; stub so detail success path stays isolated.
vi.mock("../../../main/ts/developer/ObjectAclSection", () => ({
  ObjectAclSection: () => <div data-testid="developer-ct-acl-stub" />,
}));

const getContentTypeDetail = contentTypesApi.getContentTypeDetail as ReturnType<typeof vi.fn>;
const updateContentTypeDetail = contentTypesApi.updateContentTypeDetail as ReturnType<
  typeof vi.fn
>;

const sampleDetail = {
  name: "percPage",
  label: "Page",
  description: "Page type",
  enabled: true,
  hideFromMenu: false,
  appName: "rx_cm",
  guid: { stringValue: "0-2-301" },
  fields: [],
  allowedWorkflows: [],
  allowedTemplates: [],
  designGaps: [],
};

describe("ContentTypeDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getContentTypeDetail.mockReset();
    updateContentTypeDetail.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-title").textContent).toContain("Page");
    expect(screen.getByTestId("developer-ct-fields-table")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-wf-empty")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-tpl-empty")).toBeTruthy();
    const back = screen.getByTestId("developer-ct-back");
    expect(back.getAttribute("aria-label")).toBe("Back to list");
    fireEvent.click(back);
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty workflows and templates when detail has none", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      allowedWorkflows: [],
      allowedTemplates: [],
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-wf-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-tpl-empty")).toBeTruthy();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getContentTypeDetail.mockRejectedValue(new SessionRedirectError());
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-ct-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-ct-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getContentTypeDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-error").textContent).toBe(
      `${DEV_MSG.CT_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getContentTypeDetail.mockRejectedValue(new Error("network down"));
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-error").textContent).toBe(
      `${DEV_MSG.CT_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-ct-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getContentTypeDetail.mockRejectedValue("boom");
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-error").textContent).toBe(
      DEV_MSG.CT_DETAIL_ERROR,
    );
  });
});
