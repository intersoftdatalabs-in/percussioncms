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
  createSiteSection: vi.fn(),
}));

vi.mock("../../../main/ts/api/home/homeApi", () => ({
  fetchTemplatesForSectionCreate: vi.fn(),
}));

const loadSectionTree = sectionApi.loadSectionTree as ReturnType<typeof vi.fn>;
const createSiteSection = sectionApi.createSiteSection as ReturnType<typeof vi.fn>;

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
    expect(loadSectionTree).not.toHaveBeenCalled();
  });
});
