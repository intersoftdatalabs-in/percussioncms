/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as sitesApi from "../../../main/ts/api/developer/sitesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import * as sourceViewer from "../../../main/ts/developer/templateSourceViewer";
import { VirtualSiteSourcePanel } from "../../../main/ts/developer/VirtualSiteSourcePanel";

vi.mock("../../../main/ts/api/developer/sitesApi", () => ({
  getVirtualSiteProperties: vi.fn(),
  updateVirtualSiteProperties: vi.fn(),
  buildVirtualSite: vi.fn(),
  publishVirtualSite: vi.fn(),
  getVirtualSitePreviewStatus: vi.fn(),
  virtualSitePreviewContentHref: vi.fn(
    (name: string, home: string) => `/services/sites/${encodeURIComponent(name)}/virtual/preview/${home}`,
  ),
  listSites: vi.fn(),
  SITE_DESIGN_GAPS: [],
}));

const getVirtual = sitesApi.getVirtualSiteProperties as ReturnType<typeof vi.fn>;
const updateVirtual = sitesApi.updateVirtualSiteProperties as ReturnType<typeof vi.fn>;
const buildVirtual = sitesApi.buildVirtualSite as ReturnType<typeof vi.fn>;
const publishVirtual = sitesApi.publishVirtualSite as ReturnType<typeof vi.fn>;
const previewStatus = sitesApi.getVirtualSitePreviewStatus as ReturnType<typeof vi.fn>;

describe("VirtualSiteSourcePanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getVirtual.mockReset();
    updateVirtual.mockReset();
    buildVirtual.mockReset();
    publishVirtual.mockReset();
    previewStatus.mockReset();
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
    const kindSelect = screen.getByTestId(
      "developer-site-virtual-source-kind",
    ) as HTMLSelectElement;
    const kindValues = Array.from(kindSelect.options).map((o) => o.value);
    expect(kindValues).toEqual([
      "repository",
      "git-filesystem",
      "csv-filesystem",
      "sql-database",
      "http-json",
      "object-storage",
    ]);
    expect(kindSelect.value).toBe("repository");
    // Root path / remote hidden until virtual selected
    expect(screen.queryByTestId("developer-site-virtual-root-path")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-remote-url")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-branch")).toBeNull();
    // Repository sites must not show Build or Publish chrome
    expect(screen.queryByTestId("developer-site-virtual-build")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-build-section")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-preview")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-publish")).toBeNull();
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
    expect(
      (screen.getByTestId("developer-site-virtual-remote-url") as HTMLInputElement).value,
    ).toBe("");
    expect((screen.getByTestId("developer-site-virtual-branch") as HTMLInputElement).value).toBe("");
    expect(screen.getByTestId("developer-site-virtual-remote-hint")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-status").textContent).toContain(
      DEV_MSG.SITE_VIRT_STATUS_VIRTUAL,
    );
  });

  it("loads and re-saves a stored remote URL so Save does not drop it", async () => {
    const stored = {
      sourceKind: "git-filesystem",
      rootPath: "product-docs",
      remoteUrl: "https://git.example.com/org/docs.git",
      branch: "main",
      configFile: "_config.yaml",
      siteKey: "docs",
      virtual: true,
    };
    getVirtual.mockResolvedValue(stored);
    updateVirtual.mockResolvedValue(stored);
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-remote-url")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("developer-site-virtual-remote-url") as HTMLInputElement).value,
    ).toBe("https://git.example.com/org/docs.git");
    expect((screen.getByTestId("developer-site-virtual-branch") as HTMLInputElement).value).toBe(
      "main",
    );
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-saved")).toBeTruthy();
    });
    expect(updateVirtual).toHaveBeenCalledWith("Help", {
      sourceKind: "git-filesystem",
      rootPath: "product-docs",
      remoteUrl: "https://git.example.com/org/docs.git",
      branch: "main",
      configFile: "_config.yaml",
      siteKey: "docs",
    });
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
    getVirtual
      .mockResolvedValueOnce({ sourceKind: null, virtual: false })
      .mockResolvedValueOnce({
        sourceKind: "git-filesystem",
        rootPath: "C:/docs",
        configFile: null,
        siteKey: null,
        virtual: true,
      });
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
      remoteUrl: "",
      branch: "",
      configFile: null,
      siteKey: null,
    });
    expect(getVirtual).toHaveBeenCalledTimes(2);
    expect(
      (screen.getByTestId("developer-site-virtual-root-path") as HTMLInputElement).value,
    ).toBe("C:/docs");
    expect(
      (screen.getByTestId("developer-site-virtual-remote-url") as HTMLInputElement).value,
    ).toBe("");
    expect(screen.getByTestId("developer-site-virtual-build-section")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-status").textContent).toContain(
      DEV_MSG.SITE_VIRT_STATUS_VIRTUAL,
    );
  });

  it("saves optional remote URL and branch with local root path", async () => {
    getVirtual
      .mockResolvedValueOnce({ sourceKind: null, virtual: false })
      .mockResolvedValueOnce({
        sourceKind: "git-filesystem",
        rootPath: "C:/docs",
        remoteUrl: "https://git.example.com/org/docs.git",
        branch: "main",
        configFile: null,
        siteKey: null,
        virtual: true,
      });
    updateVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      remoteUrl: "https://git.example.com/org/docs.git",
      branch: "main",
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
      expect(screen.getByTestId("developer-site-virtual-remote-url")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-site-virtual-root-path"), {
      target: { value: "C:/docs" },
    });
    fireEvent.change(screen.getByTestId("developer-site-virtual-remote-url"), {
      target: { value: "https://git.example.com/org/docs.git" },
    });
    fireEvent.change(screen.getByTestId("developer-site-virtual-branch"), {
      target: { value: "main" },
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-saved")).toBeTruthy();
    });
    expect(updateVirtual).toHaveBeenCalledWith("Corporate", {
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      remoteUrl: "https://git.example.com/org/docs.git",
      branch: "main",
      configFile: null,
      siteKey: null,
    });
    expect(
      (screen.getByTestId("developer-site-virtual-remote-url") as HTMLInputElement).value,
    ).toBe("https://git.example.com/org/docs.git");
  });

  it("loads csv-filesystem values with root path and Build/Publish chrome (no Git remotes)", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "csv-filesystem",
      rootPath: "C:/csv-docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("developer-site-virtual-source-kind") as HTMLSelectElement).value,
    ).toBe("csv-filesystem");
    expect(
      (screen.getByTestId("developer-site-virtual-root-path") as HTMLInputElement).value,
    ).toBe("C:/csv-docs");
    expect(screen.getByTestId("developer-site-virtual-csv-hint")).toBeTruthy();
    expect(screen.queryByTestId("developer-site-virtual-remote-url")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-branch")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-config-file")).toBeNull();
    expect(screen.getByTestId("developer-site-virtual-build-section")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-status").textContent).toContain(
      DEV_MSG.SITE_VIRT_STATUS_VIRTUAL,
    );
  });

  it("saves csv-filesystem configuration without Git remote fields", async () => {
    getVirtual
      .mockResolvedValueOnce({ sourceKind: null, virtual: false })
      .mockResolvedValueOnce({
        sourceKind: "csv-filesystem",
        rootPath: "C:/csv-docs",
        virtual: true,
      });
    updateVirtual.mockResolvedValue({
      sourceKind: "csv-filesystem",
      rootPath: "C:/csv-docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Corporate" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-form")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-site-virtual-source-kind"), {
      target: { value: "csv-filesystem" },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-error").textContent).toContain(
        DEV_MSG.SITE_VIRT_ERR_ROOT_REQUIRED,
      );
    });
    expect(updateVirtual).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("developer-site-virtual-root-path"), {
      target: { value: "C:/csv-docs" },
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-saved")).toBeTruthy();
    });
    expect(updateVirtual).toHaveBeenCalledWith("Corporate", {
      sourceKind: "csv-filesystem",
      rootPath: "C:/csv-docs",
      remoteUrl: "",
      branch: "",
    });
    expect(getVirtual).toHaveBeenCalledTimes(2);
    expect(
      (screen.getByTestId("developer-site-virtual-root-path") as HTMLInputElement).value,
    ).toBe("C:/csv-docs");
    expect(screen.getByTestId("developer-site-virtual-build-section")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-status").textContent).toContain(
      DEV_MSG.SITE_VIRT_STATUS_VIRTUAL,
    );
  });

  it("switching csv-filesystem back to repository hides virtual fields", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "csv-filesystem",
      rootPath: "C:/csv-docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-site-virtual-source-kind"), {
      target: { value: "repository" },
    });
    await waitFor(() => {
      expect(screen.queryByTestId("developer-site-virtual-root-path")).toBeNull();
    });
    expect(screen.queryByTestId("developer-site-virtual-csv-hint")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-build-section")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-build")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-publish")).toBeNull();
  });

  it("loads sql-database values with root path and Build/Publish chrome (no Git remotes)", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "sql-database",
      rootPath: "C:/sql-docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("developer-site-virtual-source-kind") as HTMLSelectElement).value,
    ).toBe("sql-database");
    expect(
      (screen.getByTestId("developer-site-virtual-root-path") as HTMLInputElement).value,
    ).toBe("C:/sql-docs");
    expect(screen.getByTestId("developer-site-virtual-sql-hint")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-sql-hint").textContent).toBe(
      DEV_MSG.SITE_VIRT_SQL_HINT,
    );
    expect(screen.queryByTestId("developer-site-virtual-remote-url")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-branch")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-config-file")).toBeNull();
    expect(screen.getByTestId("developer-site-virtual-build-section")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-status").textContent).toContain(
      DEV_MSG.SITE_VIRT_STATUS_VIRTUAL,
    );
  });

  it("saves sql-database configuration without Git remote fields or password", async () => {
    getVirtual
      .mockResolvedValueOnce({ sourceKind: null, virtual: false })
      .mockResolvedValueOnce({
        sourceKind: "sql-database",
        rootPath: "C:/sql-docs",
        virtual: true,
      });
    updateVirtual.mockResolvedValue({
      sourceKind: "sql-database",
      rootPath: "C:/sql-docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Corporate" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-form")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-site-virtual-source-kind"), {
      target: { value: "sql-database" },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-error").textContent).toContain(
        DEV_MSG.SITE_VIRT_ERR_ROOT_REQUIRED,
      );
    });
    expect(updateVirtual).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("developer-site-virtual-root-path"), {
      target: { value: "C:/sql-docs" },
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-saved")).toBeTruthy();
    });
    expect(updateVirtual).toHaveBeenCalledWith("Corporate", {
      sourceKind: "sql-database",
      rootPath: "C:/sql-docs",
      remoteUrl: "",
      branch: "",
    });
    const savedBody = updateVirtual.mock.calls[0][1] as Record<string, unknown>;
    expect(savedBody).not.toHaveProperty("password");
    expect(getVirtual).toHaveBeenCalledTimes(2);
    expect(
      (screen.getByTestId("developer-site-virtual-root-path") as HTMLInputElement).value,
    ).toBe("C:/sql-docs");
    expect(screen.getByTestId("developer-site-virtual-build-section")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-status").textContent).toContain(
      DEV_MSG.SITE_VIRT_STATUS_VIRTUAL,
    );
  });

  it("switching sql-database back to repository hides virtual Preview/Build", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "sql-database",
      rootPath: "C:/sql-docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-site-virtual-source-kind"), {
      target: { value: "repository" },
    });
    await waitFor(() => {
      expect(screen.queryByTestId("developer-site-virtual-root-path")).toBeNull();
    });
    expect(screen.queryByTestId("developer-site-virtual-sql-hint")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-build-section")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-build")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-preview")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-publish")).toBeNull();
  });

  it("loads http-json values with root path and Build/Preview/Publish chrome", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "http-json",
      rootPath: "C:/http-json-docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("developer-site-virtual-source-kind") as HTMLSelectElement).value,
    ).toBe("http-json");
    expect(
      (screen.getByTestId("developer-site-virtual-root-path") as HTMLInputElement).value,
    ).toBe("C:/http-json-docs");
    expect(screen.getByTestId("developer-site-virtual-http-json-hint")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-http-json-hint").textContent).toBe(
      DEV_MSG.SITE_VIRT_HTTP_JSON_HINT,
    );
    expect(screen.getByTestId("developer-site-virtual-http-json-hint").textContent).toContain(
      "Build Virtual Site",
    );
    expect(screen.getByTestId("developer-site-virtual-http-json-hint").textContent).toContain(
      "Preview assembled site",
    );
    expect(screen.getByTestId("developer-site-virtual-http-json-hint").textContent).toContain(
      "Publish Virtual Site",
    );
    expect(screen.queryByTestId("developer-site-virtual-remote-url")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-branch")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-config-file")).toBeNull();
    expect(screen.getByTestId("developer-site-virtual-build-section")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-status").textContent).toContain(
      DEV_MSG.SITE_VIRT_STATUS_VIRTUAL,
    );
  });

  it("saves http-json configuration without Git remote fields or secrets", async () => {
    getVirtual
      .mockResolvedValueOnce({ sourceKind: null, virtual: false })
      .mockResolvedValueOnce({
        sourceKind: "http-json",
        rootPath: "C:/http-json-docs",
        virtual: true,
      });
    updateVirtual.mockResolvedValue({
      sourceKind: "http-json",
      rootPath: "C:/http-json-docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Corporate" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-form")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-site-virtual-source-kind"), {
      target: { value: "http-json" },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-error").textContent).toContain(
        DEV_MSG.SITE_VIRT_ERR_ROOT_REQUIRED,
      );
    });
    expect(updateVirtual).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("developer-site-virtual-root-path"), {
      target: { value: "C:/http-json-docs" },
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-saved")).toBeTruthy();
    });
    expect(updateVirtual).toHaveBeenCalledWith("Corporate", {
      sourceKind: "http-json",
      rootPath: "C:/http-json-docs",
      remoteUrl: "",
      branch: "",
    });
    const savedBody = updateVirtual.mock.calls[0][1] as Record<string, unknown>;
    expect(savedBody).not.toHaveProperty("password");
    expect(JSON.stringify(savedBody)).not.toMatch(/authorization|api[_-]?key/i);
    expect(getVirtual).toHaveBeenCalledTimes(2);
    expect(
      (screen.getByTestId("developer-site-virtual-root-path") as HTMLInputElement).value,
    ).toBe("C:/http-json-docs");
    expect(screen.getByTestId("developer-site-virtual-build-section")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-status").textContent).toContain(
      DEV_MSG.SITE_VIRT_STATUS_VIRTUAL,
    );
  });

  it("switching http-json back to repository hides virtual fields", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "http-json",
      rootPath: "C:/http-json-docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-site-virtual-source-kind"), {
      target: { value: "repository" },
    });
    await waitFor(() => {
      expect(screen.queryByTestId("developer-site-virtual-root-path")).toBeNull();
    });
    expect(screen.queryByTestId("developer-site-virtual-http-json-hint")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-build-section")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-build")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-preview")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-publish")).toBeNull();
  });

  it("loads object-storage values with root path and Build/Preview/Publish chrome", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "object-storage",
      rootPath: "C:/object-docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("developer-site-virtual-source-kind") as HTMLSelectElement).value,
    ).toBe("object-storage");
    expect(
      (screen.getByTestId("developer-site-virtual-root-path") as HTMLInputElement).value,
    ).toBe("C:/object-docs");
    expect(screen.getByTestId("developer-site-virtual-object-storage-hint")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-object-storage-hint").textContent).toBe(
      DEV_MSG.SITE_VIRT_OBJECT_STORAGE_HINT,
    );
    expect(screen.getByTestId("developer-site-virtual-object-storage-hint").textContent).toContain(
      "Build Virtual Site",
    );
    expect(screen.getByTestId("developer-site-virtual-object-storage-hint").textContent).toContain(
      "Preview assembled site",
    );
    expect(screen.getByTestId("developer-site-virtual-object-storage-hint").textContent).toContain(
      "Publish Virtual Site",
    );
    expect(screen.getByTestId("developer-site-virtual-object-storage-hint").textContent).not.toContain(
      "later phase",
    );
    expect(screen.queryByTestId("developer-site-virtual-remote-url")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-branch")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-config-file")).toBeNull();
    expect(screen.getByTestId("developer-site-virtual-build-section")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-status").textContent).toContain(
      DEV_MSG.SITE_VIRT_STATUS_VIRTUAL,
    );
  });

  it("saves object-storage configuration without Git remote fields or secrets", async () => {
    getVirtual
      .mockResolvedValueOnce({ sourceKind: null, virtual: false })
      .mockResolvedValueOnce({
        sourceKind: "object-storage",
        rootPath: "C:/object-docs",
        virtual: true,
      });
    updateVirtual.mockResolvedValue({
      sourceKind: "object-storage",
      rootPath: "C:/object-docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Corporate" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-form")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-site-virtual-source-kind"), {
      target: { value: "object-storage" },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-error").textContent).toContain(
        DEV_MSG.SITE_VIRT_ERR_ROOT_REQUIRED,
      );
    });
    expect(updateVirtual).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("developer-site-virtual-root-path"), {
      target: { value: "C:/object-docs" },
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-saved")).toBeTruthy();
    });
    expect(updateVirtual).toHaveBeenCalledWith("Corporate", {
      sourceKind: "object-storage",
      rootPath: "C:/object-docs",
      remoteUrl: "",
      branch: "",
    });
    const savedBody = updateVirtual.mock.calls[0][1] as Record<string, unknown>;
    expect(savedBody).not.toHaveProperty("password");
    expect(JSON.stringify(savedBody)).not.toMatch(
      /authorization|api[_-]?key|access[_-]?key|secret|iam|s3:\/\//i,
    );
    expect(getVirtual).toHaveBeenCalledTimes(2);
    expect(
      (screen.getByTestId("developer-site-virtual-root-path") as HTMLInputElement).value,
    ).toBe("C:/object-docs");
    expect(screen.getByTestId("developer-site-virtual-build-section")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-status").textContent).toContain(
      DEV_MSG.SITE_VIRT_STATUS_VIRTUAL,
    );
  });

  it("switching object-storage back to repository hides virtual fields", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "object-storage",
      rootPath: "C:/object-docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-root-path")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-site-virtual-source-kind"), {
      target: { value: "repository" },
    });
    await waitFor(() => {
      expect(screen.queryByTestId("developer-site-virtual-root-path")).toBeNull();
    });
    expect(screen.queryByTestId("developer-site-virtual-object-storage-hint")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-build-section")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-build")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-preview")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-publish")).toBeNull();
  });

  it("saves repository mode to clear virtual configuration", async () => {
    getVirtual
      .mockResolvedValueOnce({
        sourceKind: "git-filesystem",
        rootPath: "C:/docs",
        virtual: true,
      })
      .mockResolvedValueOnce({
        sourceKind: null,
        rootPath: null,
        virtual: false,
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
    expect(getVirtual).toHaveBeenCalledTimes(2);
    expect(screen.queryByTestId("developer-site-virtual-build-section")).toBeNull();
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

  it("shows Build chrome for virtual sites and success result", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      configFile: "_config.yaml",
      siteKey: "product-docs",
      virtual: true,
    });
    buildVirtual.mockResolvedValue({
      siteName: "Help",
      siteKey: "product-docs",
      outputPath: "C:/tmp/virtual-sites/product-docs",
      pagesWritten: 12,
      linkProblemCount: 0,
      hasLinkProblems: false,
      linkProblems: [],
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-build-section")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-site-virtual-build"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build-result")).toBeTruthy();
    });
    expect(buildVirtual).toHaveBeenCalledWith("Help");
    expect(screen.getByTestId("developer-site-virtual-build-success").textContent).toContain(
      DEV_MSG.SITE_VIRT_BUILD_SUCCESS,
    );
    expect(screen.getByTestId("developer-site-virtual-build-pages").textContent).toBe("12");
    expect(screen.getByTestId("developer-site-virtual-build-output").textContent).toContain(
      "product-docs",
    );
    expect(screen.queryByTestId("developer-site-virtual-build-link-problems")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-build-link-list")).toBeNull();
  });

  it("shows Build chrome for csv-filesystem and success result", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "csv-filesystem",
      rootPath: "C:/csv-docs",
      virtual: true,
    });
    buildVirtual.mockResolvedValue({
      siteName: "Help",
      siteKey: "csv-docs",
      outputPath: "C:/tmp/virtual-sites/csv-docs",
      pagesWritten: 2,
      linkProblemCount: 0,
      hasLinkProblems: false,
      linkProblems: [],
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-site-virtual-build"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build-result")).toBeTruthy();
    });
    expect(buildVirtual).toHaveBeenCalledWith("Help");
    expect(screen.getByTestId("developer-site-virtual-build-success").textContent).toContain(
      DEV_MSG.SITE_VIRT_BUILD_SUCCESS,
    );
    expect(screen.getByTestId("developer-site-virtual-build-pages").textContent).toBe("2");
    expect(screen.getByTestId("developer-site-virtual-build-output").textContent).toContain(
      "csv-docs",
    );
  });

  it("shows Publish chrome for csv-filesystem and success dest path", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "csv-filesystem",
      rootPath: "C:/csv-docs",
      virtual: true,
    });
    publishVirtual.mockResolvedValue({
      siteName: "Help",
      publishPath: "C:/inetpub/wwwroot/csv-help",
      filesCopied: 4,
      pagesWritten: 2,
      hasLinkProblems: false,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-publish"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish-result")).toBeTruthy();
    });
    expect(publishVirtual).toHaveBeenCalledWith("Help");
    expect(screen.getByTestId("developer-site-virtual-publish-success").textContent).toContain(
      DEV_MSG.SITE_VIRT_PUBLISH_SUCCESS,
    );
    expect(screen.getByTestId("developer-site-virtual-publish-files").textContent).toBe("4");
    expect(screen.getByTestId("developer-site-virtual-publish-dest").textContent).toContain(
      "csv-help",
    );
  });

  it("shows Build chrome for sql-database and success result", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "sql-database",
      rootPath: "C:/sql-docs",
      virtual: true,
    });
    buildVirtual.mockResolvedValue({
      siteName: "Help",
      siteKey: "sql-docs",
      outputPath: "C:/tmp/virtual-sites/sql-docs",
      pagesWritten: 2,
      linkProblemCount: 0,
      hasLinkProblems: false,
      linkProblems: [],
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-site-virtual-build"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build-result")).toBeTruthy();
    });
    expect(buildVirtual).toHaveBeenCalledWith("Help");
    expect(screen.getByTestId("developer-site-virtual-build-success").textContent).toContain(
      DEV_MSG.SITE_VIRT_BUILD_SUCCESS,
    );
    expect(screen.getByTestId("developer-site-virtual-build-pages").textContent).toBe("2");
    expect(screen.getByTestId("developer-site-virtual-build-output").textContent).toContain(
      "sql-docs",
    );
  });

  it("shows Build, Preview, and Publish chrome for http-json and success result", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "http-json",
      rootPath: "C:/http-json-docs",
      virtual: true,
    });
    buildVirtual.mockResolvedValue({
      siteName: "Help",
      siteKey: "http-json-docs",
      outputPath: "C:/tmp/virtual-sites/http-json-docs",
      pagesWritten: 1,
      linkProblemCount: 0,
      hasLinkProblems: false,
      linkProblems: [],
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-site-virtual-build"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build-result")).toBeTruthy();
    });
    expect(buildVirtual).toHaveBeenCalledWith("Help");
    expect(screen.getByTestId("developer-site-virtual-build-success").textContent).toContain(
      DEV_MSG.SITE_VIRT_BUILD_SUCCESS,
    );
    expect(screen.getByTestId("developer-site-virtual-build-pages").textContent).toBe("1");
    expect(screen.getByTestId("developer-site-virtual-build-output").textContent).toContain(
      "http-json-docs",
    );
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
  });

  it("shows Build chrome for object-storage and success result with Preview/Publish", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "object-storage",
      rootPath: "C:/object-docs",
      virtual: true,
    });
    buildVirtual.mockResolvedValue({
      siteName: "Help",
      siteKey: "object-docs",
      outputPath: "C:/tmp/virtual-sites/object-docs",
      pagesWritten: 1,
      linkProblemCount: 0,
      hasLinkProblems: false,
      linkProblems: [],
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-object-storage-hint").textContent).toContain(
      "Build Virtual Site",
    );
    expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-site-virtual-build"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build-result")).toBeTruthy();
    });
    expect(buildVirtual).toHaveBeenCalledWith("Help");
    expect(screen.getByTestId("developer-site-virtual-build-success").textContent).toContain(
      DEV_MSG.SITE_VIRT_BUILD_SUCCESS,
    );
    expect(screen.getByTestId("developer-site-virtual-build-pages").textContent).toBe("1");
    expect(screen.getByTestId("developer-site-virtual-build-output").textContent).toContain(
      "object-docs",
    );
    expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
  });

  it("shows Publish chrome for http-json and success dest path", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "http-json",
      rootPath: "C:/http-json-docs",
      virtual: true,
    });
    publishVirtual.mockResolvedValue({
      siteName: "Help",
      publishPath: "C:/inetpub/wwwroot/http-json-help",
      filesCopied: 3,
      pagesWritten: 1,
      hasLinkProblems: false,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-site-virtual-publish"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish-result")).toBeTruthy();
    });
    expect(publishVirtual).toHaveBeenCalledWith("Help");
    expect(screen.getByTestId("developer-site-virtual-publish-success").textContent).toContain(
      DEV_MSG.SITE_VIRT_PUBLISH_SUCCESS,
    );
    expect(screen.getByTestId("developer-site-virtual-publish-files").textContent).toBe("3");
    expect(screen.getByTestId("developer-site-virtual-publish-dest").textContent).toContain(
      "http-json-help",
    );
  });

  it("keeps Publish chrome after http-json Build success and copies dest path (#3820)", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "http-json",
      rootPath: "C:/http-json-docs",
      virtual: true,
    });
    buildVirtual.mockResolvedValue({
      siteName: "Help",
      siteKey: "http-json-docs",
      outputPath: "C:/tmp/virtual-sites/http-json-docs",
      pagesWritten: 1,
      linkProblemCount: 0,
      hasLinkProblems: false,
      linkProblems: [],
    });
    publishVirtual.mockResolvedValue({
      siteName: "Help",
      publishPath: "/opt/Percussion/fastforward/http-json-help",
      filesCopied: 3,
      pagesWritten: 1,
      hasLinkProblems: false,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-site-virtual-build"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build-result")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-site-virtual-publish"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish-result")).toBeTruthy();
    });
    expect(buildVirtual).toHaveBeenCalledWith("Help");
    expect(publishVirtual).toHaveBeenCalledWith("Help");
    expect(screen.getByTestId("developer-site-virtual-publish-files").textContent).toBe("3");
    expect(screen.getByTestId("developer-site-virtual-publish-dest").textContent).toContain(
      "http-json-help",
    );
  });

  it("shows Publish chrome for sql-database and success dest path", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "sql-database",
      rootPath: "C:/sql-docs",
      virtual: true,
    });
    publishVirtual.mockResolvedValue({
      siteName: "Help",
      publishPath: "C:/inetpub/wwwroot/sql-help",
      filesCopied: 4,
      pagesWritten: 2,
      hasLinkProblems: false,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-publish"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish-result")).toBeTruthy();
    });
    expect(publishVirtual).toHaveBeenCalledWith("Help");
    expect(screen.getByTestId("developer-site-virtual-publish-success").textContent).toContain(
      DEV_MSG.SITE_VIRT_PUBLISH_SUCCESS,
    );
    expect(screen.getByTestId("developer-site-virtual-publish-files").textContent).toBe("4");
    expect(screen.getByTestId("developer-site-virtual-publish-dest").textContent).toContain(
      "sql-help",
    );
  });

  it("keeps Publish chrome after sql-database Build success and copies dest path (#3778)", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "sql-database",
      rootPath: "C:/sql-docs",
      virtual: true,
    });
    buildVirtual.mockResolvedValue({
      siteName: "Help",
      siteKey: "sql-docs",
      outputPath: "C:/tmp/virtual-sites/sql-docs",
      pagesWritten: 2,
      linkProblemCount: 0,
      hasLinkProblems: false,
      linkProblems: [],
    });
    publishVirtual.mockResolvedValue({
      siteName: "Help",
      publishPath: "/opt/Percussion/fastforward/sql-help",
      filesCopied: 4,
      pagesWritten: 2,
      hasLinkProblems: false,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-site-virtual-build"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build-result")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-site-virtual-publish"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish-result")).toBeTruthy();
    });
    expect(buildVirtual).toHaveBeenCalledWith("Help");
    expect(publishVirtual).toHaveBeenCalledWith("Help");
    expect(screen.getByTestId("developer-site-virtual-publish-files").textContent).toBe("4");
    expect(screen.getByTestId("developer-site-virtual-publish-dest").textContent).toContain(
      "sql-help",
    );
  });

  it("lists link problem details on HTTP 200 with hasLinkProblems", async () => {
    const copySpy = vi.spyOn(sourceViewer, "copyTextToClipboard").mockResolvedValue(true);
    getVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      virtual: true,
    });
    buildVirtual.mockResolvedValue({
      siteName: "Help",
      siteKey: "product-docs",
      outputPath: "C:/tmp/virtual-sites/product-docs",
      pagesWritten: 4,
      linkProblemCount: 2,
      hasLinkProblems: true,
      linkProblems: [
        "broken id:missing-page from 8.2/index.md",
        "  unresolved relative ./gone.md  ",
      ],
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-build"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build-result")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-build-success")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-build-link-problems").textContent).toContain(
      DEV_MSG.SITE_VIRT_BUILD_LINK_PROBLEMS,
    );
    expect(screen.getByTestId("developer-site-virtual-build-link-problems").textContent).toContain(
      "2",
    );
    expect(screen.getByTestId("developer-site-virtual-build-link-report-hint").textContent).toContain(
      DEV_MSG.SITE_VIRT_BUILD_LINK_REPORT_HINT,
    );
    const lines = screen.getAllByTestId("developer-site-virtual-build-link-line");
    expect(lines).toHaveLength(2);
    expect(lines[0].textContent).toBe("broken id:missing-page from 8.2/index.md");
    expect(lines[1].textContent).toBe("unresolved relative ./gone.md");
    fireEvent.click(screen.getByTestId("developer-site-virtual-build-link-copy"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build-link-copied").textContent).toContain(
        DEV_MSG.SITE_VIRT_BUILD_LINK_COPIED,
      );
    });
    expect(copySpy).toHaveBeenCalledWith(
      "broken id:missing-page from 8.2/index.md\nunresolved relative ./gone.md",
    );
    copySpy.mockRestore();
  });

  it("surfaces build API errors", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      virtual: true,
    });
    buildVirtual.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "Site is not configured as a Virtual Site" },
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-build"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-build-error").textContent).toContain(
      DEV_MSG.SITE_VIRT_BUILD_ERROR,
    );
    expect(screen.getByTestId("developer-site-virtual-build-error").textContent).toContain(
      "not configured",
    );
    expect(screen.queryByTestId("developer-site-virtual-build-result")).toBeNull();
  });

  it("hides Build chrome when switching back to repository", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-site-virtual-source-kind"), {
      target: { value: "repository" },
    });
    await waitFor(() => {
      expect(screen.queryByTestId("developer-site-virtual-build")).toBeNull();
    });
    expect(screen.queryByTestId("developer-site-virtual-build-section")).toBeNull();
    expect(screen.queryByTestId("developer-site-virtual-publish")).toBeNull();
  });

  it("client-validates before build when root is empty", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-build"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-build-error").textContent).toContain(
        DEV_MSG.SITE_VIRT_ERR_ROOT_REQUIRED,
      );
    });
    expect(buildVirtual).not.toHaveBeenCalled();
  });

  it("shows Preview chrome for virtual sites and opens home when available", async () => {
    const open = vi.fn();
    window.open = open;
    getVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      virtual: true,
    });
    previewStatus.mockResolvedValue({
      available: true,
      homePath: "8.2/index.html",
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-preview"));
    await waitFor(() => {
      expect(open).toHaveBeenCalled();
    });
    expect(previewStatus).toHaveBeenCalledWith("Help");
    expect(String(open.mock.calls[0][0])).toContain("8.2/index.html");
    expect(open.mock.calls[0][1]).toBe("_blank");
  });

  it("shows Preview chrome for csv-filesystem and opens last-build home", async () => {
    const open = vi.fn();
    window.open = open;
    getVirtual.mockResolvedValue({
      sourceKind: "csv-filesystem",
      rootPath: "C:/csv-docs",
      virtual: true,
    });
    previewStatus.mockResolvedValue({
      available: true,
      homePath: "8.2/index.html",
    });
    render(<VirtualSiteSourcePanel siteName="CsvHelp" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-csv-hint").textContent).toContain(
      DEV_MSG.SITE_VIRT_CSV_HINT,
    );
    expect(screen.getByTestId("developer-site-virtual-preview-hint").textContent).toContain(
      DEV_MSG.SITE_VIRT_PREVIEW_HINT,
    );
    fireEvent.click(screen.getByTestId("developer-site-virtual-preview"));
    await waitFor(() => {
      expect(open).toHaveBeenCalled();
    });
    expect(previewStatus).toHaveBeenCalledWith("CsvHelp");
    expect(String(open.mock.calls[0][0])).toContain("8.2/index.html");
    expect(open.mock.calls[0][1]).toBe("_blank");
  });

  it("shows Preview chrome for sql-database and opens last-build home", async () => {
    const open = vi.fn();
    window.open = open;
    getVirtual.mockResolvedValue({
      sourceKind: "sql-database",
      rootPath: "C:/sql-docs",
      virtual: true,
    });
    previewStatus.mockResolvedValue({
      available: true,
      homePath: "8.2/index.html",
    });
    render(<VirtualSiteSourcePanel siteName="SqlHelp" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-sql-hint").textContent).toContain(
      DEV_MSG.SITE_VIRT_SQL_HINT,
    );
    expect(screen.getByTestId("developer-site-virtual-preview-hint").textContent).toContain(
      DEV_MSG.SITE_VIRT_PREVIEW_HINT,
    );
    expect(screen.getByTestId("developer-site-virtual-preview-hint").textContent).toContain(
      "SQL database",
    );
    fireEvent.click(screen.getByTestId("developer-site-virtual-preview"));
    await waitFor(() => {
      expect(open).toHaveBeenCalled();
    });
    expect(previewStatus).toHaveBeenCalledWith("SqlHelp");
    expect(String(open.mock.calls[0][0])).toContain("8.2/index.html");
    expect(open.mock.calls[0][1]).toBe("_blank");
  });

  it("shows Preview chrome for http-json and opens last-build home", async () => {
    const open = vi.fn();
    window.open = open;
    getVirtual.mockResolvedValue({
      sourceKind: "http-json",
      rootPath: "C:/http-json-docs",
      virtual: true,
    });
    previewStatus.mockResolvedValue({
      available: true,
      homePath: "8.2/index.html",
    });
    render(<VirtualSiteSourcePanel siteName="HttpJsonHelp" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-http-json-hint").textContent).toContain(
      DEV_MSG.SITE_VIRT_HTTP_JSON_HINT,
    );
    expect(screen.getByTestId("developer-site-virtual-preview-hint").textContent).toContain(
      DEV_MSG.SITE_VIRT_PREVIEW_HINT,
    );
    expect(screen.getByTestId("developer-site-virtual-preview-hint").textContent).toContain(
      "HTTP JSON",
    );
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-site-virtual-preview"));
    await waitFor(() => {
      expect(open).toHaveBeenCalled();
    });
    expect(previewStatus).toHaveBeenCalledWith("HttpJsonHelp");
    expect(String(open.mock.calls[0][0])).toContain("8.2/index.html");
    expect(open.mock.calls[0][1]).toBe("_blank");
  });

  it("shows Preview chrome for object-storage and opens last-build home", async () => {
    const open = vi.fn();
    window.open = open;
    getVirtual.mockResolvedValue({
      sourceKind: "object-storage",
      rootPath: "C:/object-docs",
      virtual: true,
    });
    previewStatus.mockResolvedValue({
      available: true,
      homePath: "8.2/index.html",
    });
    render(<VirtualSiteSourcePanel siteName="ObjectHelp" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-object-storage-hint").textContent).toContain(
      DEV_MSG.SITE_VIRT_OBJECT_STORAGE_HINT,
    );
    expect(screen.getByTestId("developer-site-virtual-preview-hint").textContent).toContain(
      DEV_MSG.SITE_VIRT_PREVIEW_HINT,
    );
    expect(screen.getByTestId("developer-site-virtual-preview-hint").textContent).toContain(
      "Object storage",
    );
    expect(screen.getByTestId("developer-site-virtual-build")).toBeTruthy();
    expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-site-virtual-preview"));
    await waitFor(() => {
      expect(open).toHaveBeenCalled();
    });
    expect(previewStatus).toHaveBeenCalledWith("ObjectHelp");
    expect(String(open.mock.calls[0][0])).toContain("8.2/index.html");
    expect(open.mock.calls[0][1]).toBe("_blank");
  });

  it("shows empty preview state when no assembled site exists", async () => {
    window.open = vi.fn();
    getVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      virtual: true,
    });
    previewStatus.mockResolvedValue({
      available: false,
      message: "No assembled Virtual Site to preview. Run Build Virtual Site first.",
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-preview")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-preview"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-preview-error").textContent).toContain(
        "No assembled",
      );
    });
    expect(window.open).not.toHaveBeenCalled();
  });

  it("shows Publish chrome for virtual sites and success dest path", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      virtual: true,
    });
    publishVirtual.mockResolvedValue({
      siteName: "Help",
      publishPath: "C:/inetpub/wwwroot/help",
      filesCopied: 18,
      pagesWritten: 12,
      hasLinkProblems: false,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-publish"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish-result")).toBeTruthy();
    });
    expect(publishVirtual).toHaveBeenCalledWith("Help");
    expect(screen.getByTestId("developer-site-virtual-publish-success").textContent).toContain(
      DEV_MSG.SITE_VIRT_PUBLISH_SUCCESS,
    );
    expect(screen.getByTestId("developer-site-virtual-publish-files").textContent).toBe("18");
    expect(screen.getByTestId("developer-site-virtual-publish-dest").textContent).toContain(
      "inetpub",
    );
  });

  it("shows Publish chrome for object-storage and success dest path", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "object-storage",
      rootPath: "C:/object-docs",
      virtual: true,
    });
    publishVirtual.mockResolvedValue({
      siteName: "ObjectHelp",
      publishPath: "C:/inetpub/wwwroot/object-help",
      filesCopied: 4,
      pagesWritten: 1,
      hasLinkProblems: false,
    });
    render(<VirtualSiteSourcePanel siteName="ObjectHelp" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-object-storage-hint").textContent).toContain(
      "Publish Virtual Site",
    );
    expect(screen.getByTestId("developer-site-virtual-publish-hint").textContent).toContain(
      "Object storage",
    );
    fireEvent.click(screen.getByTestId("developer-site-virtual-publish"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish-result")).toBeTruthy();
    });
    expect(publishVirtual).toHaveBeenCalledWith("ObjectHelp");
    expect(screen.getByTestId("developer-site-virtual-publish-success").textContent).toContain(
      DEV_MSG.SITE_VIRT_PUBLISH_SUCCESS,
    );
    expect(screen.getByTestId("developer-site-virtual-publish-files").textContent).toBe("4");
    expect(screen.getByTestId("developer-site-virtual-publish-dest").textContent).toContain(
      "object-help",
    );
  });

  it("surfaces publish API errors", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      virtual: true,
    });
    publishVirtual.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "Site is not configured as a Virtual Site" },
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-publish"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-site-virtual-publish-error").textContent).toContain(
      DEV_MSG.SITE_VIRT_PUBLISH_ERROR,
    );
    expect(screen.getByTestId("developer-site-virtual-publish-error").textContent).toContain(
      "not configured",
    );
    expect(screen.queryByTestId("developer-site-virtual-publish-result")).toBeNull();
  });

  it("client-validates before publish when root is empty", async () => {
    getVirtual.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "",
      virtual: true,
    });
    render(<VirtualSiteSourcePanel siteName="Help" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-site-virtual-publish"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-virtual-publish-error").textContent).toContain(
        DEV_MSG.SITE_VIRT_ERR_ROOT_REQUIRED,
      );
    });
    expect(publishVirtual).not.toHaveBeenCalled();
  });
});
