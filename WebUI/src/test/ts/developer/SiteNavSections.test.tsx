/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as sectionApi from "../../../main/ts/api/architecture/sectionApi";
import * as homeApi from "../../../main/ts/api/home/homeApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SiteNavSections } from "../../../main/ts/developer/SiteNavSections";

vi.mock("../../../main/ts/api/architecture/sectionApi", () => ({
  loadSectionTree: vi.fn(),
  loadSection: vi.fn(),
  loadSectionProperties: vi.fn(),
  createSiteSection: vi.fn(),
  updateSiteSection: vi.fn(),
  deleteSiteSection: vi.fn(),
  moveSiteSection: vi.fn(),
  createExternalLinkSection: vi.fn(),
}));

vi.mock("../../../main/ts/api/home/homeApi", () => ({
  fetchTemplatesForSectionCreate: vi.fn(),
}));

const loadSectionTree = sectionApi.loadSectionTree as ReturnType<typeof vi.fn>;
const createSiteSection = sectionApi.createSiteSection as ReturnType<typeof vi.fn>;
const loadSectionProperties = sectionApi.loadSectionProperties as ReturnType<typeof vi.fn>;
const updateSiteSection = sectionApi.updateSiteSection as ReturnType<typeof vi.fn>;
const deleteSiteSection = sectionApi.deleteSiteSection as ReturnType<typeof vi.fn>;
const moveSiteSection = sectionApi.moveSiteSection as ReturnType<typeof vi.fn>;
const createExternalLinkSection = sectionApi.createExternalLinkSection as ReturnType<typeof vi.fn>;

const tree = {
  id: "root",
  title: "Corporate",
  folderPath: "//Sites/Corporate",
  sectionType: "section",
  requiresLogin: false,
  children: [],
};

describe("SiteNavSections", () => {
  beforeEach(() => {
    loadSectionTree.mockReset();
    createSiteSection.mockReset();
    loadSectionProperties.mockReset();
    updateSiteSection.mockReset();
    deleteSiteSection.mockReset();
    moveSiteSection.mockReset();
    createExternalLinkSection.mockReset();
    (homeApi.fetchTemplatesForSectionCreate as ReturnType<typeof vi.fn>).mockReset();
    loadSectionTree.mockResolvedValue(tree);
    (homeApi.fetchTemplatesForSectionCreate as ReturnType<typeof vi.fn>).mockResolvedValue([
      { id: "tpl-1", name: "Home" },
    ]);
  });

  it("lists the reloaded section and does not post on cancel", async () => {
    render(<SiteNavSections site={{ name: "Corporate", managedNavigation: true }} />);
    await waitFor(() => {
      expect(screen.getAllByTestId("developer-site-nav-item")[0].textContent).toBe("Corporate");
    });
    fireEvent.change(screen.getByTestId("developer-site-nav-name"), {
      target: { value: "About Us" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-cancel"));
    expect(createSiteSection).not.toHaveBeenCalled();
    expect((screen.getByTestId("developer-site-nav-name") as HTMLInputElement).value).toBe("");
  });

  it("keeps an invalid name on the panel without posting", async () => {
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-add");
    fireEvent.change(screen.getByTestId("developer-site-nav-name"), {
      target: { value: "!!!" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-add"));
    expect(createSiteSection).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-site-nav-error").textContent).toBe(
      DEV_MSG.SITE_NAV_INVALID,
    );
  });

  it("stays on the panel for 400 and 403", async () => {
    createSiteSection.mockRejectedValueOnce({ status: 400, statusText: "Bad Request", body: "bad" });
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-add");
    fireEvent.change(screen.getByTestId("developer-site-nav-name"), {
      target: { value: "About" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-error").textContent).toContain("bad");
    });
    expect(screen.getByTestId("developer-site-nav")).toBeTruthy();

    createSiteSection.mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: "" });
    fireEvent.click(screen.getByTestId("developer-site-nav-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-error").textContent).toContain(
        DEV_MSG.SITE_NAV_FORBIDDEN,
      );
    });
  });

  it("posts add and shows the new title after reload", async () => {
    createSiteSection.mockResolvedValue({ id: "new" });
    loadSectionTree
      .mockResolvedValueOnce(tree)
      .mockResolvedValueOnce({
        ...tree,
        children: [
          {
            id: "new",
            title: "About",
            folderPath: "//Sites/Corporate/about",
            sectionType: "section",
            requiresLogin: false,
            children: [],
          },
        ],
      });
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-add");
    fireEvent.change(screen.getByTestId("developer-site-nav-name"), {
      target: { value: "About" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-add"));
    await waitFor(() => {
      expect(createSiteSection).toHaveBeenCalledTimes(1);
    });
    await waitFor(() => {
      const items = screen.getAllByTestId("developer-site-nav-item").map((n) => n.textContent);
      expect(items).toContain("About");
    });
    expect(screen.getByTestId("developer-site-nav-notice").textContent).toBe(DEV_MSG.SITE_NAV_ADDED);
  });

  it("does not offer add on a read-only system site", () => {
    render(<SiteNavSections site={{ name: "System", managedNavigation: false }} />);
    expect(screen.getByTestId("developer-site-nav-readonly")).toBeTruthy();
    expect(screen.queryByTestId("developer-site-nav-add")).toBeNull();
    expect(screen.queryByTestId("developer-site-nav-rename-save")).toBeNull();
    expect(screen.queryByTestId("developer-site-nav-delete-confirm")).toBeNull();
    expect(screen.queryByTestId("developer-site-nav-ext-add")).toBeNull();
    expect(loadSectionTree).not.toHaveBeenCalled();
  });

  it("rename cancel restores the old name and does not post", async () => {
    render(<SiteNavSections site={{ name: "Corporate", managedNavigation: true }} />);
    await screen.findByTestId("developer-site-nav-rename-save");
    expect((screen.getByTestId("developer-site-nav-rename-name") as HTMLInputElement).value).toBe(
      "Corporate",
    );
    fireEvent.change(screen.getByTestId("developer-site-nav-rename-name"), {
      target: { value: "Holdings" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-rename-cancel"));
    expect(updateSiteSection).not.toHaveBeenCalled();
    expect(loadSectionProperties).not.toHaveBeenCalled();
    expect((screen.getByTestId("developer-site-nav-rename-name") as HTMLInputElement).value).toBe(
      "Corporate",
    );
  });

  it("keeps an invalid or duplicate rename on the panel", async () => {
    loadSectionTree.mockResolvedValue({
      ...tree,
      children: [
        {
          id: "news",
          title: "News",
          folderPath: "//Sites/Corporate/News",
          sectionType: "section",
          requiresLogin: false,
          children: [],
        },
      ],
    });
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-rename-save");
    fireEvent.change(screen.getByTestId("developer-site-nav-rename-target"), {
      target: { value: "news" },
    });
    fireEvent.change(screen.getByTestId("developer-site-nav-rename-name"), {
      target: { value: "!!!" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-rename-save"));
    expect(updateSiteSection).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-site-nav-rename-error").textContent).toBe(
      DEV_MSG.SITE_NAV_INVALID,
    );

    fireEvent.change(screen.getByTestId("developer-site-nav-rename-name"), {
      target: { value: "Corporate" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-rename-save"));
    expect(updateSiteSection).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-site-nav-rename-error").textContent).toBe(
      DEV_MSG.SITE_NAV_RENAME_DUPLICATE,
    );
  });

  it("stays on the panel when rename returns 400 or 403", async () => {
    loadSectionProperties.mockResolvedValue({
      id: "root",
      title: "Corporate",
      folderName: "Corporate",
      siteRootSection: true,
    });
    updateSiteSection.mockRejectedValueOnce({ status: 400, statusText: "Bad Request", body: "bad" });
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-rename-save");
    fireEvent.change(screen.getByTestId("developer-site-nav-rename-name"), {
      target: { value: "Holdings" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-rename-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-rename-error").textContent).toContain("bad");
    });
    expect(screen.getByTestId("developer-site-nav")).toBeTruthy();

    updateSiteSection.mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: "" });
    fireEvent.click(screen.getByTestId("developer-site-nav-rename-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-rename-error").textContent).toContain(
        DEV_MSG.SITE_NAV_RENAME_FORBIDDEN,
      );
    });
  });

  it("posts rename and shows the new title after reload", async () => {
    loadSectionProperties.mockResolvedValue({
      id: "root",
      title: "Corporate",
      folderName: "Corporate",
      siteRootSection: true,
    });
    updateSiteSection.mockResolvedValue({});
    loadSectionTree.mockResolvedValueOnce(tree).mockResolvedValueOnce({
      ...tree,
      title: "Holdings",
    });
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-rename-save");
    fireEvent.change(screen.getByTestId("developer-site-nav-rename-name"), {
      target: { value: "Holdings" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-rename-save"));
    await waitFor(() => {
      expect(updateSiteSection).toHaveBeenCalledTimes(1);
    });
    expect(updateSiteSection.mock.calls[0][0].title).toBe("Holdings");
    expect(updateSiteSection.mock.calls[0][0].folderName).toBe("Corporate");
    await waitFor(() => {
      const items = screen.getAllByTestId("developer-site-nav-item").map((n) => n.textContent);
      expect(items).toContain("Holdings");
    });
    expect(screen.getByTestId("developer-site-nav-rename-notice").textContent).toBe(
      DEV_MSG.SITE_NAV_RENAMED,
    );
  });

  it("delete cancel does not call delete", async () => {
    loadSectionTree.mockResolvedValue({
      ...tree,
      children: [
        {
          id: "news",
          title: "News",
          folderPath: "//Sites/Corporate/News",
          sectionType: "section",
          requiresLogin: false,
          children: [],
        },
      ],
    });
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-delete-confirm");
    fireEvent.click(screen.getByTestId("developer-site-nav-delete-cancel"));
    expect(deleteSiteSection).not.toHaveBeenCalled();
    expect(screen.getAllByTestId("developer-site-nav-item").map((n) => n.textContent)).toContain(
      "News",
    );
  });

  it("stays on the panel when delete returns 403 or 409", async () => {
    loadSectionTree.mockResolvedValue({
      ...tree,
      children: [
        {
          id: "news",
          title: "News",
          folderPath: "//Sites/Corporate/News",
          sectionType: "section",
          requiresLogin: false,
          children: [],
        },
      ],
    });
    deleteSiteSection.mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: "" });
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-delete-confirm");
    fireEvent.click(screen.getByTestId("developer-site-nav-delete-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-delete-error").textContent).toContain(
        DEV_MSG.SITE_NAV_DELETE_FORBIDDEN,
      );
    });
    expect(screen.getAllByTestId("developer-site-nav-item").map((n) => n.textContent)).toContain(
      "News",
    );

    deleteSiteSection.mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: "" });
    fireEvent.click(screen.getByTestId("developer-site-nav-delete-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-delete-error").textContent).toContain(
        DEV_MSG.SITE_NAV_DELETE_CONFLICT,
      );
    });
    expect(loadSectionTree).toHaveBeenCalledTimes(1);
    expect(screen.getAllByTestId("developer-site-nav-item").map((n) => n.textContent)).toContain(
      "News",
    );
  });

  it("confirm deletes only the selected section and reloads without it", async () => {
    const withNews = {
      ...tree,
      children: [
        {
          id: "news",
          title: "News",
          folderPath: "//Sites/Corporate/News",
          sectionType: "section",
          requiresLogin: false,
          children: [],
        },
        {
          id: "about",
          title: "About",
          folderPath: "//Sites/Corporate/About",
          sectionType: "section",
          requiresLogin: false,
          children: [],
        },
      ],
    };
    loadSectionTree.mockResolvedValueOnce(withNews).mockResolvedValueOnce({
      ...tree,
      children: [withNews.children[1]],
    });
    deleteSiteSection.mockResolvedValue({});
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-delete-confirm");
    fireEvent.change(screen.getByTestId("developer-site-nav-delete-target"), {
      target: { value: "news" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-delete-confirm"));
    await waitFor(() => {
      expect(deleteSiteSection).toHaveBeenCalledTimes(1);
    });
    expect(deleteSiteSection).toHaveBeenCalledWith("news");
    await waitFor(() => {
      const items = screen.getAllByTestId("developer-site-nav-item").map((n) => n.textContent);
      expect(items).not.toContain("News");
      expect(items).toContain("About");
    });
    expect(screen.getByTestId("developer-site-nav-delete-notice").textContent).toBe(
      DEV_MSG.SITE_NAV_DELETED,
    );
  });

  function twoChildTree() {
    return {
      ...tree,
      children: [
        {
          id: "news",
          title: "News",
          folderPath: "//Sites/Corporate/News",
          sectionType: "section",
          requiresLogin: false,
          children: [],
        },
        {
          id: "about",
          title: "About",
          folderPath: "//Sites/Corporate/About",
          sectionType: "section",
          requiresLogin: false,
          children: [],
        },
      ],
    };
  }

  it("does not offer the site root and cancel does not move", async () => {
    loadSectionTree.mockResolvedValue(twoChildTree());
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-move-down");
    const options = screen.getByTestId("developer-site-nav-reorder-target").querySelectorAll("option");
    expect(Array.from(options).map((o) => o.getAttribute("value"))).toEqual(["news", "about"]);
    fireEvent.click(screen.getByTestId("developer-site-nav-reorder-cancel"));
    expect(moveSiteSection).not.toHaveBeenCalled();
    expect(screen.getAllByTestId("developer-site-nav-item").map((n) => n.textContent)).toEqual([
      "Corporate",
      "News",
      "About",
    ]);
  });

  it("move down persists sibling order and refreshes the list", async () => {
    const start = twoChildTree();
    const swapped = {
      ...start,
      children: [start.children[1], start.children[0]],
    };
    loadSectionTree.mockResolvedValueOnce(start).mockResolvedValueOnce(swapped);
    moveSiteSection.mockResolvedValue({});
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-move-down");
    fireEvent.click(screen.getByTestId("developer-site-nav-move-down"));
    await waitFor(() => {
      expect(moveSiteSection).toHaveBeenCalledTimes(1);
    });
    expect(moveSiteSection).toHaveBeenCalledWith({
      sourceId: "news",
      targetId: "root",
      sourceParentId: "root",
      targetIndex: 1,
    });
    await waitFor(() => {
      expect(screen.getAllByTestId("developer-site-nav-item").map((n) => n.textContent)).toEqual([
        "Corporate",
        "About",
        "News",
      ]);
    });
    expect(screen.getByTestId("developer-site-nav-reorder-notice").textContent).toBe(
      DEV_MSG.SITE_NAV_MOVED,
    );
  });

  it("stays on the panel when reorder returns 403 or 409", async () => {
    loadSectionTree.mockResolvedValue(twoChildTree());
    moveSiteSection.mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: "" });
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-move-down");
    fireEvent.click(screen.getByTestId("developer-site-nav-move-down"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-reorder-error").textContent).toContain(
        DEV_MSG.SITE_NAV_REORDER_FORBIDDEN,
      );
    });
    expect(screen.getAllByTestId("developer-site-nav-item").map((n) => n.textContent)).toEqual([
      "Corporate",
      "News",
      "About",
    ]);

    moveSiteSection.mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: "" });
    fireEvent.click(screen.getByTestId("developer-site-nav-move-down"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-reorder-error").textContent).toContain(
        DEV_MSG.SITE_NAV_REORDER_CONFLICT,
      );
    });
    expect(loadSectionTree).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId("developer-site-nav-reorder-notice")).toBeNull();
  });

  it("cancel and the current parent do not reparent", async () => {
    loadSectionTree.mockResolvedValue(twoChildTree());
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-reparent-confirm");
    const sections = screen
      .getByTestId("developer-site-nav-reparent-target")
      .querySelectorAll("option");
    expect(Array.from(sections).map((o) => o.getAttribute("value"))).toEqual(["news", "about"]);
    expect(
      (screen.getByTestId("developer-site-nav-reparent-parent") as HTMLSelectElement).value,
    ).toBe("root");
    fireEvent.click(screen.getByTestId("developer-site-nav-reparent-cancel"));
    fireEvent.click(screen.getByTestId("developer-site-nav-reparent-confirm"));
    expect(moveSiteSection).not.toHaveBeenCalled();
  });

  it("confirm moves a section under a different parent and refreshes the list", async () => {
    const start = twoChildTree();
    const moved = {
      ...start,
      children: [
        {
          ...start.children[1],
          children: [start.children[0]],
        },
      ],
    };
    loadSectionTree.mockResolvedValueOnce(start).mockResolvedValueOnce(moved);
    moveSiteSection.mockResolvedValue({});
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-reparent-confirm");
    fireEvent.change(screen.getByTestId("developer-site-nav-reparent-parent"), {
      target: { value: "about" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-reparent-confirm"));
    await waitFor(() => {
      expect(moveSiteSection).toHaveBeenCalledWith({
        sourceId: "news",
        targetId: "about",
        sourceParentId: "root",
        targetIndex: 0,
      });
    });
    await waitFor(() => {
      const news = screen
        .getAllByTestId("developer-site-nav-item")
        .find((node) => node.textContent === "News");
      expect(news?.getAttribute("data-parent-id")).toBe("about");
    });
    expect(screen.getByTestId("developer-site-nav-reparent-notice").textContent).toBe(
      DEV_MSG.SITE_NAV_REPARENTED,
    );
  });

  it("stays on the panel when reparent returns 403, 409, or 404", async () => {
    loadSectionTree.mockResolvedValue(twoChildTree());
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-reparent-confirm");
    fireEvent.change(screen.getByTestId("developer-site-nav-reparent-parent"), {
      target: { value: "about" },
    });
    moveSiteSection.mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: "" });
    fireEvent.click(screen.getByTestId("developer-site-nav-reparent-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-reparent-error").textContent).toContain(
        DEV_MSG.SITE_NAV_REPARENT_FORBIDDEN,
      );
    });
    moveSiteSection.mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: "" });
    fireEvent.click(screen.getByTestId("developer-site-nav-reparent-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-reparent-error").textContent).toContain(
        DEV_MSG.SITE_NAV_REPARENT_CONFLICT,
      );
    });
    moveSiteSection.mockRejectedValueOnce({ status: 404, statusText: "Not Found", body: "" });
    fireEvent.click(screen.getByTestId("developer-site-nav-reparent-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-reparent-error").textContent).toContain(
        DEV_MSG.SITE_NAV_REPARENT_MISSING,
      );
    });
    expect(screen.queryByTestId("developer-site-nav-reparent-notice")).toBeNull();
    expect(loadSectionTree).toHaveBeenCalledTimes(1);
  });

  it("does not post an external link on cancel or empty title or URL", async () => {
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-ext-add");
    fireEvent.change(screen.getByTestId("developer-site-nav-ext-title"), {
      target: { value: "Partner" },
    });
    fireEvent.change(screen.getByTestId("developer-site-nav-ext-url"), {
      target: { value: "https://partner.example" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-ext-cancel"));
    expect(createExternalLinkSection).not.toHaveBeenCalled();
    expect((screen.getByTestId("developer-site-nav-ext-title") as HTMLInputElement).value).toBe("");
    expect((screen.getByTestId("developer-site-nav-ext-url") as HTMLInputElement).value).toBe("");

    fireEvent.click(screen.getByTestId("developer-site-nav-ext-add"));
    expect(createExternalLinkSection).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-site-nav-ext-error").textContent).toBe(
      DEV_MSG.SITE_NAV_EXT_INVALID,
    );
    expect(screen.queryByTestId("developer-site-nav-ext-notice")).toBeNull();

    fireEvent.change(screen.getByTestId("developer-site-nav-ext-title"), {
      target: { value: "Partner" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-ext-add"));
    expect(createExternalLinkSection).not.toHaveBeenCalled();
  });

  it("keeps 400, 403, and 409 on the external link form", async () => {
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-ext-add");
    fireEvent.change(screen.getByTestId("developer-site-nav-ext-title"), {
      target: { value: "Partner" },
    });
    fireEvent.change(screen.getByTestId("developer-site-nav-ext-url"), {
      target: { value: "https://partner.example" },
    });
    fireEvent.change(screen.getByTestId("developer-site-nav-ext-target"), {
      target: { value: "_blank" },
    });
    createExternalLinkSection.mockRejectedValueOnce({
      status: 400,
      statusText: "Bad Request",
      body: "bad",
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-ext-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-ext-error").textContent).toContain("bad");
    });
    expect(screen.queryByTestId("developer-site-nav-ext-notice")).toBeNull();
    expect((screen.getByTestId("developer-site-nav-ext-title") as HTMLInputElement).value).toBe(
      "Partner",
    );

    createExternalLinkSection.mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: "" });
    fireEvent.click(screen.getByTestId("developer-site-nav-ext-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-ext-error").textContent).toContain(
        DEV_MSG.SITE_NAV_EXT_FORBIDDEN,
      );
    });

    createExternalLinkSection.mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: "" });
    fireEvent.click(screen.getByTestId("developer-site-nav-ext-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-nav-ext-error").textContent).toContain(
        DEV_MSG.SITE_NAV_EXT_CONFLICT,
      );
    });
    expect(screen.queryByTestId("developer-site-nav-ext-notice")).toBeNull();
  });

  it("posts CreateExternalLinkSection and lists the new title", async () => {
    createExternalLinkSection.mockResolvedValue({ id: "ext-1" });
    loadSectionTree.mockResolvedValueOnce(tree).mockResolvedValueOnce({
      ...tree,
      children: [
        {
          id: "ext-1",
          title: "Partner",
          folderPath: null,
          sectionType: "externallink",
          requiresLogin: false,
          children: [],
        },
      ],
    });
    render(<SiteNavSections site={{ name: "Corporate" }} />);
    await screen.findByTestId("developer-site-nav-ext-add");
    fireEvent.change(screen.getByTestId("developer-site-nav-ext-title"), {
      target: { value: "Partner" },
    });
    fireEvent.change(screen.getByTestId("developer-site-nav-ext-url"), {
      target: { value: "https://partner.example" },
    });
    fireEvent.change(screen.getByTestId("developer-site-nav-ext-target"), {
      target: { value: "_blank" },
    });
    fireEvent.click(screen.getByTestId("developer-site-nav-ext-add"));
    await waitFor(() => {
      expect(createExternalLinkSection).toHaveBeenCalledWith(
        expect.objectContaining({
          linkTitle: "Partner",
          externalUrl: "https://partner.example",
          folderPath: "//Sites/Corporate",
          sectionType: "externallink",
          target: "_blank",
        }),
      );
    });
    await waitFor(() => {
      const items = screen.getAllByTestId("developer-site-nav-item").map((n) => n.textContent);
      expect(items).toContain("Partner");
    });
    expect(screen.getByTestId("developer-site-nav-ext-notice").textContent).toBe(
      DEV_MSG.SITE_NAV_EXT_ADDED,
    );
  });
});
