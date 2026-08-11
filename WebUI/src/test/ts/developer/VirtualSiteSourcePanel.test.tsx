/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as sitesApi from "../../../main/ts/api/developer/sitesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { VirtualSiteSourcePanel } from "../../../main/ts/developer/VirtualSiteSourcePanel";

vi.mock("../../../main/ts/api/developer/sitesApi", () => ({
  getVirtualSiteProperties: vi.fn(),
  updateVirtualSiteProperties: vi.fn(),
  listSites: vi.fn(),
  SITE_DESIGN_GAPS: [],
}));

const getVirtual = sitesApi.getVirtualSiteProperties as ReturnType<typeof vi.fn>;
const updateVirtual = sitesApi.updateVirtualSiteProperties as ReturnType<typeof vi.fn>;

describe("VirtualSiteSourcePanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getVirtual.mockReset();
    updateVirtual.mockReset();
  });

  it("shows loading then success form for traditional (repository) site", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: null,
      rootPath: null,
      virtual: false,
    });
    render(<VirtualSiteSourcePanel siteName="Corporate" />);
    expect(screen.getByTestId("developer-site-virtual-loading")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-form")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-status").textContent).toContain(
      DEV_MSG.SITE_VIRT_STATUS_REPO,
    );
    expect(screen.getByTestId("developer-site-virtual-source-kind")).toBeTruthy();
    // Root path hidden until virtual selected
    expect(screen.queryByTestId("developer-site-virtual-root-path")).toBeNull();
    expect(getVirtual).toHaveBeenCalledWith("Corporate");
  });

  it("loads git-filesystem values and shows root/config/siteKey fields", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "C:/workspaces/product-docs",
      configFile: "_config.yaml",
      siteKey: "product-docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("developer-site-virtual-root-path") as HTMLInputElement).value,
    ).toBe("C:/workspaces/product-docs");
    expect(
      (screen.getByTestId("developer-site-virtual-config-file") as HTMLInputElement).value,
    ).toBe("_config.yaml");
    expect((screen.getByTestId("developer-site-virtual-site-key") as HTMLInputElement).value).toBe(
      "product-docs",
    );
    expect(screen.getByTestId("developer-site-virtual-status").textContent).toContain(
      DEV_MSG.SITE_VIRT_STATUS_VIRTUAL,
    );
  });

  it("shows error state and retry on load failure", async () => {
    getVirtual.mockRejectedValueOnce({ status: 500, statusText: "ERR", body: "boom" });
    getVirtual.mockResolvedValueOnce({ sourceKind: null, virtual: false });
    render(<VirtualSiteSourcePanel siteName="X" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-error").textContent).toContain(
      DEV_MSG.SITE_VIRT_ERROR,
    );
    fireEvent.click(screen.getByTestId("developer-site-virtual-retry"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-form")).toBeTruthy();
    });
    expect(getVirtual).toHaveBeenCalledTimes(2);
  });

  it("saves git-filesystem configuration after client validation", async () => {
    getVirtual.mockResolvedValue({ sourceKind: null, virtual: false });
    updateVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      configFile: null,
      siteKey: null,
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Corporate" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-form")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("developer-site-virtual-source-kind"), {
      target: { value: "git-filesystem" },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });

    // Client validation: root required
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-error").textContent).toContain(
        DEV_MSG.SITE_VIRT_ERR_ROOT_REQUIRED,
      );
    });
    expect(updateVirtual).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("developer-site-virtual-root-path"), {
      target: { value: "C:/docs" },
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-saved")).toBeTruthy();
    });
    expect(updateVirtual).toHaveBeenCalledWith("Corporate", {
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      configFile: null,
      siteKey: null,
    });
    expect(
      (screen.getByTestId("developer-site-virtual-root-path") as HTMLInputElement).value,
    ).toBe("C:/docs");
  });

  it("saves repository mode to clear virtual configuration", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      virtual: true,
    });
    updateVirtual.mockResolvedValue({
      sourceKind: null,
      rootPath: null,
      virtual: false,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-site-virtual-source-kind"), {
      target: { value: "repository" },
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-saved")).toBeTruthy();
    });
    expect(updateVirtual).toHaveBeenCalledWith("Help", {
      sourceKind: "repository",
      rootPath: null,
      configFile: null,
      siteKey: null,
    });
  });

  it("surfaces save API errors", async () => {
    getVirtual.mockResolvedValue({ sourceKind: null, virtual: false });
    updateVirtual.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "rootPath is required" },
    });
    render(<VirtualSiteSourcePanel siteName="Corporate" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-form")).toBeTruthy();
    });
    // Stay repository — save still allowed (clears virtual)
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-error").textContent).toContain(
        DEV_MSG.SITE_VIRT_SAVE_ERROR,
      );
    });
    expect(screen.getByTestId("developer-site-virtual-error").textContent).toContain(
      "rootPath is required",
    );
  });
});
