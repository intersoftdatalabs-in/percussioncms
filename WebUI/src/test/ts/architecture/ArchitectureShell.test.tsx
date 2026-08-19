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

import {
  cleanup,
  render,
  screen,
  waitFor,
  fireEvent,
  within,
} from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ArchitectureShell } from "../../../main/ts/architecture/ArchitectureShell";
import * as homeApi from "../../../main/ts/api/home/homeApi";
import * as sectionApi from "../../../main/ts/api/architecture/sectionApi";

const treeFixture = {
  id: "root",
  title: "Home",
  folderPath: "//Sites/Demo",
  sectionType: "section" as const,
  requiresLogin: false,
  children: [
    {
      id: "c1",
      title: "About",
      folderPath: "//Sites/Demo/About",
      sectionType: "section" as const,
      requiresLogin: false,
      children: [],
    },
    {
      id: "c2",
      title: "News",
      folderPath: "//Sites/Demo/News",
      sectionType: "section" as const,
      requiresLogin: false,
      children: [],
    },
  ],
};

describe("ArchitectureShell (#3095/#3096)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
    vi.restoreAllMocks();
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("shows empty state when no site is selected and no sites load", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([]);
    render(<ArchitectureShell embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("architecture-sites-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("architecture-empty-state")).toBeTruthy();
    expect(screen.getByTestId("architecture-shell-title").textContent).toMatch(
      /Navigation/i,
    );
  });

  it("loads tree when initial site is provided", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([
      { name: "Corporate Investments" },
      { name: "Demo" },
    ]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue({
      ...treeFixture,
      folderPath: "//Sites/Corporate Investments",
      title: "Home",
    });

    render(
      <ArchitectureShell embedded initialSite="Corporate Investments" />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("architecture-nav-tree")).toBeTruthy();
    });
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-root")).toBeTruthy();
    });
    expect(screen.getByTestId("nav-tree-item-root").textContent).toMatch(
      /Home/i,
    );
    expect(screen.getByTestId("architecture-site-hint").textContent).toContain(
      "Corporate Investments",
    );
    expect(screen.getByTestId("architecture-structure-note")).toBeTruthy();
    expect(screen.getByTestId("architecture-structure-actions")).toBeTruthy();
    expect(sectionApi.loadSectionTree).toHaveBeenCalledWith(
      "Corporate Investments",
    );
  });

  it("shows operator empty state when tree is missing (#3218)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "BareSite" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(null);

    render(<ArchitectureShell embedded initialSite="BareSite" />);

    await waitFor(() => {
      expect(screen.getByTestId("architecture-nav-tree-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("architecture-nav-tree-empty-title").textContent).toMatch(
      /no navigation tree/i,
    );
    expect(screen.queryByTestId("architecture-nav-tree-error")).toBeNull();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("surfaces tree load errors", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Site not found" },
    });

    render(<ArchitectureShell embedded initialSite="Demo" />);

    await waitFor(() => {
      expect(screen.getByTestId("architecture-nav-tree-error")).toBeTruthy();
    });
    expect(
      screen.getByTestId("architecture-nav-tree-error").textContent,
    ).toMatch(/Site not found|Could not load/i);
  });

  it("refresh reloads the tree", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    const loadSpy = vi
      .spyOn(sectionApi, "loadSectionTree")
      .mockResolvedValue(treeFixture);

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(loadSpy).toHaveBeenCalledTimes(1);
    });
    fireEvent.click(screen.getByTestId("architecture-refresh"));
    await waitFor(() => {
      expect(loadSpy).toHaveBeenCalledTimes(2);
    });
  });

  it("keeps create disabled when the site has no NavTree (#3350)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "BareSite" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(null);

    render(<ArchitectureShell embedded initialSite="BareSite" />);
    await waitFor(() => {
      expect(screen.getByTestId("architecture-nav-tree-empty")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("architecture-action-create") as HTMLButtonElement)
        .disabled,
    ).toBe(true);
    expect(
      (
        screen.getByTestId(
          "architecture-action-create-external-link",
        ) as HTMLButtonElement
      ).disabled,
    ).toBe(true);
    expect(
      (
        screen.getByTestId(
          "architecture-action-create-section-link",
        ) as HTMLButtonElement
      ).disabled,
    ).toBe(true);
  });

  it("enables create when a regular section is selected (#3350)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    vi.spyOn(homeApi, "fetchTemplatesForSite").mockResolvedValue([
      { id: "tpl-1", name: "Base" },
    ]);

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    const createBtn = screen.getByTestId(
      "architecture-action-create",
    ) as HTMLButtonElement;
    expect(createBtn.disabled).toBe(false);
    fireEvent.click(createBtn);
    await waitFor(() => {
      expect(screen.getByTestId("architecture-create-dialog")).toBeTruthy();
    });
    expect(
      screen.getByTestId("architecture-create-dialog").querySelector(
        '[role="dialog"]',
      ),
    ).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => {
      expect(screen.queryByTestId("architecture-create-dialog")).toBeNull();
    });
    expect(document.activeElement).toBe(createBtn);
  });

  it("enables create under root and opens create dialog", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    vi.spyOn(homeApi, "fetchTemplatesForSite").mockResolvedValue([
      { id: "tpl-1", name: "Base" },
    ]);

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(screen.getByTestId("architecture-action-create")).toBeTruthy();
    });
    // Create enabled with root as parent even without selection
    expect(
      (screen.getByTestId("architecture-action-create") as HTMLButtonElement)
        .disabled,
    ).toBe(false);
    fireEvent.click(screen.getByTestId("architecture-action-create"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-create-dialog")).toBeTruthy();
    });
  });

  it("delete confirms and calls deleteSiteSection", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    const loadSpy = vi
      .spyOn(sectionApi, "loadSectionTree")
      .mockResolvedValue(treeFixture);
    const delSpy = vi
      .spyOn(sectionApi, "deleteSiteSection")
      .mockResolvedValue({});
    const confirmFn = vi.fn(() => true);

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        confirmFn={confirmFn}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("architecture-action-delete") as HTMLButtonElement)
          .disabled,
      ).toBe(false);
    });
    fireEvent.click(screen.getByTestId("architecture-action-delete"));
    expect(confirmFn).toHaveBeenCalled();
    await waitFor(() => {
      expect(delSpy).toHaveBeenCalledWith("c1");
    });
    await waitFor(() => {
      expect(loadSpy.mock.calls.length).toBeGreaterThanOrEqual(2);
    });
  });

  it("surfaces mutation errors without silent failure", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    vi.spyOn(sectionApi, "deleteSiteSection").mockRejectedValue({
      status: 500,
      statusText: "Error",
      body: { message: "Cannot delete section" },
    });

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        confirmFn={() => true}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    fireEvent.click(screen.getByTestId("architecture-action-delete"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-mutation-error")).toBeTruthy();
    });
    expect(
      screen.getByTestId("architecture-mutation-error").textContent,
    ).toMatch(/Cannot delete section|Could not update/i);
  });

  it("move section picker cancel does not POST (#3349)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    const moveSpy = vi
      .spyOn(sectionApi, "moveSiteSection")
      .mockResolvedValue({});

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("architecture-action-move") as HTMLButtonElement)
          .disabled,
      ).toBe(false);
    });
    fireEvent.click(screen.getByTestId("architecture-action-move"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-move-dialog")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("architecture-move-cancel"));
    await waitFor(() => {
      expect(screen.queryByTestId("architecture-move-dialog")).toBeNull();
    });
    expect(moveSpy).not.toHaveBeenCalled();
  });

  it("move section picker posts reparent move (#3349)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    const moveSpy = vi
      .spyOn(sectionApi, "moveSiteSection")
      .mockResolvedValue({});

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    fireEvent.click(screen.getByTestId("architecture-action-move"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-move-browse")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("architecture-move-browse"));
    const picker = await waitFor(() =>
      screen.getByTestId("architecture-tree-picker-dialog"),
    );
    await waitFor(() => {
      expect(within(picker).getByTestId("nav-tree-item-c2")).toBeTruthy();
    });
    fireEvent.click(within(picker).getByTestId("nav-tree-item-c2"));
    fireEvent.click(screen.getByTestId("architecture-tree-picker-confirm"));
    fireEvent.click(screen.getByTestId("architecture-move-submit"));
    await waitFor(() => {
      expect(moveSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          sourceId: "c1",
          targetId: "c2",
          targetIndex: -1,
        }),
      );
    });
  });

  it("move up calls moveSiteSection with reordered index", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    const moveSpy = vi
      .spyOn(sectionApi, "moveSiteSection")
      .mockResolvedValue({});

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c2")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c2"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("architecture-action-move-up") as HTMLButtonElement)
          .disabled,
      ).toBe(false);
    });
    fireEvent.click(screen.getByTestId("architecture-action-move-up"));
    await waitFor(() => {
      expect(moveSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          sourceId: "c2",
          targetId: "root",
          targetIndex: 0,
        }),
      );
    });
  });

  it("move down calls moveSiteSection with reordered index", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    const moveSpy = vi
      .spyOn(sectionApi, "moveSiteSection")
      .mockResolvedValue({});

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    await waitFor(() => {
      expect(
        (
          screen.getByTestId(
            "architecture-action-move-down",
          ) as HTMLButtonElement
        ).disabled,
      ).toBe(false);
    });
    fireEvent.click(screen.getByTestId("architecture-action-move-down"));
    await waitFor(() => {
      expect(moveSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          sourceId: "c1",
          targetId: "root",
          targetIndex: 1,
        }),
      );
    });
  });

  it("opens external link create dialog from action bar (#3097)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        useLandingContentBrowser={false}
      />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("architecture-action-create-external-link"),
      ).toBeTruthy();
    });
    fireEvent.click(
      screen.getByTestId("architecture-action-create-external-link"),
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("architecture-external-link-dialog"),
      ).toBeTruthy();
    });
  });

  it("opens section link dialog and landing dialog (#3097)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        useLandingContentBrowser={false}
      />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("architecture-action-create-section-link"),
      ).toBeTruthy();
    });
    fireEvent.click(
      screen.getByTestId("architecture-action-create-section-link"),
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("architecture-section-link-dialog"),
      ).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("architecture-section-link-cancel"));

    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("architecture-action-landing") as HTMLButtonElement)
          .disabled,
      ).toBe(false);
    });
    fireEvent.click(screen.getByTestId("architecture-action-landing"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-landing-dialog")).toBeTruthy();
    });
  });

  it("create external link calls createExternalLinkSection (#3097)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    const createExt = vi
      .spyOn(sectionApi, "createExternalLinkSection")
      .mockResolvedValue({});

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        useLandingContentBrowser={false}
      />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("architecture-action-create-external-link"),
      ).toBeTruthy();
    });
    fireEvent.click(
      screen.getByTestId("architecture-action-create-external-link"),
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("architecture-external-link-text"),
      ).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("architecture-external-link-text"), {
      target: { value: "Docs" },
    });
    fireEvent.change(screen.getByTestId("architecture-external-link-url"), {
      target: { value: "https://docs.example" },
    });
    fireEvent.click(screen.getByTestId("architecture-external-link-submit"));
    await waitFor(() => {
      expect(createExt).toHaveBeenCalledWith(
        expect.objectContaining({
          linkTitle: "Docs",
          externalUrl: "https://docs.example",
          sectionType: "externallink",
        }),
      );
    });
  });

  it("edit external link loads section and submits updateExternalLink (#3097)", async () => {
    const extTree = {
      ...treeFixture,
      children: [
        {
          id: "ext-1",
          title: "Partner",
          folderPath: "//Sites/Demo/Partner",
          sectionType: "externallink" as const,
          requiresLogin: false,
          children: [],
        },
      ],
    };
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(extTree);
    vi.spyOn(sectionApi, "loadSection").mockResolvedValue({
      id: "ext-1",
      title: "Partner",
      folderPath: "//Sites/Demo/Partner",
      externalLinkUrl: "https://old.partner",
      target: "_self",
      sectionType: "externallink",
    });
    const updateSpy = vi
      .spyOn(sectionApi, "updateExternalLink")
      .mockResolvedValue({});

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        useLandingContentBrowser={false}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-ext-1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-ext-1"));
    await waitFor(() => {
      expect(
        (
          screen.getByTestId(
            "architecture-action-edit-link",
          ) as HTMLButtonElement
        ).disabled,
      ).toBe(false);
    });
    fireEvent.click(screen.getByTestId("architecture-action-edit-link"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("architecture-external-link-url") as HTMLInputElement)
          .value,
      ).toBe("https://old.partner");
    });
    fireEvent.change(screen.getByTestId("architecture-external-link-url"), {
      target: { value: "https://new.partner" },
    });
    fireEvent.click(screen.getByTestId("architecture-external-link-submit"));
    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith(
        "ext-1",
        expect.objectContaining({
          externalUrl: "https://new.partner",
          sectionType: "externallink",
        }),
      );
    });
  });

  it("create under a selected non-root section posts that parent folderPath", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    vi.spyOn(homeApi, "fetchTemplatesForSite").mockResolvedValue([
      { id: "tpl-1", name: "Base" },
    ]);
    const createSpy = vi
      .spyOn(sectionApi, "createSiteSection")
      .mockResolvedValue({});

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    fireEvent.click(screen.getByTestId("architecture-action-create"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-create-dialog")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("architecture-create-title-input"), {
      target: { value: "Child Page" },
    });
    fireEvent.change(screen.getByTestId("architecture-create-url-input"), {
      target: { value: "child-page" },
    });
    // Template may auto-select; ensure one is chosen if a select is present.
    const tpl = screen.queryByTestId("architecture-create-template-select");
    if (tpl) {
      fireEvent.change(tpl, { target: { value: "tpl-1" } });
    }
    fireEvent.click(screen.getByTestId("architecture-create-submit"));
    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          folderPath: "//Sites/Demo/About",
          templateId: "tpl-1",
          pageTitle: "Child Page",
        }),
      );
    });
  });

  it("delete section link calls deleteSectionLink with parent id", async () => {
    const linkTree = {
      ...treeFixture,
      children: [
        {
          id: "link-1",
          title: "Linked",
          folderPath: "//Sites/Demo/Linked",
          sectionType: "sectionlink" as const,
          requiresLogin: false,
          children: [],
        },
      ],
    };
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(linkTree);
    const delLinkSpy = vi
      .spyOn(sectionApi, "deleteSectionLink")
      .mockResolvedValue({});
    const delSecSpy = vi
      .spyOn(sectionApi, "deleteSiteSection")
      .mockResolvedValue({});

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        confirmFn={() => true}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-link-1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-link-1"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("architecture-action-delete") as HTMLButtonElement)
          .disabled,
      ).toBe(false);
    });
    fireEvent.click(screen.getByTestId("architecture-action-delete"));
    await waitFor(() => {
      expect(delLinkSpy).toHaveBeenCalledWith("link-1", "root");
    });
    expect(delSecSpy).not.toHaveBeenCalled();
  });

  it("shows New Site for entitled users and opens the create wizard (#3219)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([]);
    const siteCreate = await import(
      "../../../main/ts/api/contentExplorer/siteCreateApi"
    );
    vi.spyOn(siteCreate, "listBaseTemplates").mockResolvedValue([
      { name: "perc.base.plain", label: "Plain" },
    ]);

    render(<ArchitectureShell embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("architecture-action-new-site")).toBeTruthy();
    });
    expect(screen.queryByTestId("architecture-new-site-panel")).toBeNull();
    fireEvent.click(screen.getByTestId("architecture-action-new-site"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-new-site-panel")).toBeTruthy();
    });
    expect(screen.getByTestId("architecture-new-site-title").textContent).toMatch(
      /New Site/i,
    );
    expect(screen.getByTestId("site-create-step-type")).toBeTruthy();
    expect(
      (screen.getByTestId("site-create-type-traditional") as HTMLInputElement)
        .checked,
    ).toBe(true);
    expect(screen.getByTestId("architecture-new-site-panel").getAttribute("role")).toBe(
      "dialog",
    );
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => {
      expect(screen.queryByTestId("architecture-new-site-panel")).toBeNull();
    });
    fireEvent.click(screen.getByTestId("architecture-action-new-site"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-new-site-panel")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("architecture-new-site-close"));
    await waitFor(() => {
      expect(screen.queryByTestId("architecture-new-site-panel")).toBeNull();
    });
  });

  it("keeps a create-success mutation error when site reload fails (#3219)", async () => {
    const fetchSites = vi.spyOn(homeApi, "fetchSites").mockResolvedValue([]);
    const siteCreate = await import(
      "../../../main/ts/api/contentExplorer/siteCreateApi"
    );
    vi.spyOn(siteCreate, "listBaseTemplates").mockResolvedValue([
      { name: "perc.base.plain", label: "Plain" },
    ]);
    vi.spyOn(siteCreate, "createTraditionalSite").mockResolvedValue({
      name: "Acme",
    });

    render(<ArchitectureShell embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("architecture-action-new-site")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("architecture-action-new-site"));
    await waitFor(() => {
      expect(screen.getByTestId("site-create-step-type")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("site-create-next"));
    await waitFor(() => {
      expect(screen.getByTestId("site-create-name")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Acme" },
    });
    fireEvent.click(screen.getByTestId("site-create-next"));
    await waitFor(() => {
      expect(screen.getByTestId("site-create-step-confirm")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("site-create-next"));
    fetchSites.mockRejectedValue(new Error("catalog down"));
    fireEvent.click(screen.getByTestId("site-create-run"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-mutation-error").textContent).toMatch(
        /Acme.*could not be refreshed/i,
      );
    });
    expect(screen.queryByTestId("architecture-new-site-panel")).toBeNull();
    expect(screen.getByTestId("architecture-sites-error").textContent).toMatch(
      /catalog down/i,
    );
  });

  it("hides New Site when allowNewSite is false", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([]);
    render(<ArchitectureShell embedded allowNewSite={false} />);
    await waitFor(() => {
      expect(screen.getByTestId("architecture-sites-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("architecture-action-new-site")).toBeNull();
    expect(screen.queryByTestId("architecture-action-copy-site")).toBeNull();
    expect(screen.queryByTestId("architecture-action-delete-site")).toBeNull();
  });

  it("opens Copy Site wizard after copysiteinfo is idle (#3303)", async () => {
    const siteAdmin = await import(
      "../../../main/ts/api/architecture/siteAdminApi"
    );
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    vi.spyOn(siteAdmin, "loadSiteCopyInfo").mockResolvedValue({ entries: {} });

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(
        (screen.getByTestId("architecture-action-copy-site") as HTMLButtonElement)
          .disabled,
      ).toBe(false);
    });
    fireEvent.click(screen.getByTestId("architecture-action-copy-site"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-copy-site-panel")).toBeTruthy();
    });
    expect(screen.getByTestId("site-copy-wizard")).toBeTruthy();
    expect(
      (screen.getByTestId("site-copy-source") as HTMLInputElement).value,
    ).toBe("Demo");
    fireEvent.click(screen.getByTestId("site-copy-next"));
    expect(
      (screen.getByTestId("site-copy-target") as HTMLInputElement).value,
    ).toBe("Demo-copy");
    fireEvent.click(screen.getByTestId("architecture-copy-site-close"));
    await waitFor(() => {
      expect(screen.queryByTestId("architecture-copy-site-panel")).toBeNull();
    });
  });

  it("blocks Copy Site when a copy is already in progress (#3303)", async () => {
    const siteAdmin = await import(
      "../../../main/ts/api/architecture/siteAdminApi"
    );
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    vi.spyOn(siteAdmin, "loadSiteCopyInfo").mockResolvedValue({
      psmap: { entries: { src: "Other" } },
    });

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(screen.getByTestId("architecture-action-copy-site")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("architecture-action-copy-site"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-mutation-error").textContent).toMatch(
        /copy is already in progress/i,
      );
    });
    expect(screen.queryByTestId("architecture-copy-site-panel")).toBeNull();
  });

  it("confirms Delete Site, calls delete, and refreshes the picker (#3303)", async () => {
    const siteAdmin = await import(
      "../../../main/ts/api/architecture/siteAdminApi"
    );
    const fetchSites = vi
      .spyOn(homeApi, "fetchSites")
      .mockResolvedValue([{ name: "Demo" }, { name: "Keep" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    vi.spyOn(siteAdmin, "loadSiteCopyInfo").mockResolvedValue({ entries: {} });
    vi.spyOn(siteAdmin, "isSiteBeingImported").mockResolvedValue(false);
    const delSpy = vi
      .spyOn(siteAdmin, "deleteManagedSite")
      .mockResolvedValue(undefined);

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        confirmFn={() => true}
      />,
    );
    await waitFor(() => {
      expect(
        (screen.getByTestId(
          "architecture-action-delete-site",
        ) as HTMLButtonElement).disabled,
      ).toBe(false);
    });
    fetchSites.mockResolvedValue([{ name: "Keep" }]);
    fireEvent.click(screen.getByTestId("architecture-action-delete-site"));
    await waitFor(() => {
      expect(delSpy).toHaveBeenCalledWith("Demo");
    });
    await waitFor(() => {
      const picker = screen.getByTestId(
        "architecture-site-select",
      ) as HTMLSelectElement;
      expect(picker.value).toBe("Keep");
      expect(picker.textContent).not.toMatch(/Demo/);
    });
  });

  it("does not delete when the operator cancels confirmation (#3303)", async () => {
    const siteAdmin = await import(
      "../../../main/ts/api/architecture/siteAdminApi"
    );
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    const delSpy = vi.spyOn(siteAdmin, "deleteManagedSite");

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        confirmFn={() => false}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("architecture-action-delete-site")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("architecture-action-delete-site"));
    expect(delSpy).not.toHaveBeenCalled();
  });

  it("convert to folder confirms and calls convertSectionToFolder (#3302)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    const loadSpy = vi
      .spyOn(sectionApi, "loadSectionTree")
      .mockResolvedValue(treeFixture);
    const convertSpy = vi
      .spyOn(sectionApi, "convertSectionToFolder")
      .mockResolvedValue({});
    const confirmFn = vi.fn(() => true);

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        confirmFn={confirmFn}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    await waitFor(() => {
      expect(
        (
          screen.getByTestId(
            "architecture-action-convert-to-folder",
          ) as HTMLButtonElement
        ).disabled,
      ).toBe(false);
    });
    fireEvent.click(screen.getByTestId("architecture-action-convert-to-folder"));
    expect(confirmFn).toHaveBeenCalled();
    await waitFor(() => {
      expect(convertSpy).toHaveBeenCalledWith("c1");
    });
    await waitFor(() => {
      expect(loadSpy.mock.calls.length).toBeGreaterThanOrEqual(2);
    });
  });

  it("create section from folder posts and refreshes tree (#3302)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    const loadSpy = vi
      .spyOn(sectionApi, "loadSectionTree")
      .mockResolvedValue(treeFixture);
    const createSpy = vi
      .spyOn(sectionApi, "createSectionFromFolder")
      .mockResolvedValue({});

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        useLandingContentBrowser={false}
      />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("architecture-action-create-from-folder"),
      ).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("architecture-action-create-from-folder"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-from-folder-dialog")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("architecture-from-folder-path"), {
      target: { value: "//Sites/Demo/Folder" },
    });
    fireEvent.change(screen.getByTestId("architecture-from-folder-page"), {
      target: { value: "index.html" },
    });
    fireEvent.click(screen.getByTestId("architecture-from-folder-submit"));
    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          sourceFolderPath: "//Sites/Demo/Folder",
          pageName: "index.html",
          parentFolderPath: "//Sites/Demo",
        }),
      );
    });
    await waitFor(() => {
      expect(loadSpy.mock.calls.length).toBeGreaterThanOrEqual(2);
    });
  });

  it("replace landing page posts, keeps selection, and shows assigned name (#3304)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    const treeSpy = vi
      .spyOn(sectionApi, "loadSectionTree")
      .mockResolvedValue(treeFixture);
    const replaceSpy = vi.spyOn(sectionApi, "replaceLandingPage").mockResolvedValue({
      sectionId: "c1",
      newLandingPageId: "page-guid-1",
      newLandingPageName: "Picked Page",
      oldLandingPageName: "index",
    });

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        useLandingContentBrowser={false}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("architecture-action-landing") as HTMLButtonElement)
          .disabled,
      ).toBe(false);
    });
    fireEvent.click(screen.getByTestId("architecture-action-landing"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-landing-page-id")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("architecture-landing-page-id"), {
      target: { value: "page-guid-1" },
    });
    fireEvent.click(screen.getByTestId("architecture-landing-submit"));
    await waitFor(() => {
      expect(replaceSpy).toHaveBeenCalledWith({
        sectionId: "c1",
        newLandingPageId: "page-guid-1",
      });
    });
    await waitFor(() => {
      expect(screen.queryByTestId("architecture-landing-dialog")).toBeNull();
    });
    await waitFor(() => {
      expect(treeSpy.mock.calls.length).toBeGreaterThan(1);
    });
    await waitFor(() => {
      expect(screen.getByTestId("architecture-landing-current").textContent).toMatch(
        /Picked Page/,
      );
    });
    expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
  });

  it("clears landing status when a later replace fails (#3304)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    const replaceSpy = vi
      .spyOn(sectionApi, "replaceLandingPage")
      .mockResolvedValueOnce({
        sectionId: "c1",
        newLandingPageId: "page-guid-1",
        newLandingPageName: "Picked Page",
        oldLandingPageName: "index",
      })
      .mockRejectedValueOnce(new Error("replace failed"));

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        useLandingContentBrowser={false}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    fireEvent.click(screen.getByTestId("architecture-action-landing"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-landing-page-id")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("architecture-landing-page-id"), {
      target: { value: "page-guid-1" },
    });
    fireEvent.click(screen.getByTestId("architecture-landing-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-landing-current").textContent).toMatch(
        /Picked Page/,
      );
    });
    fireEvent.click(screen.getByTestId("architecture-action-landing"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-landing-page-id")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("architecture-landing-page-id"), {
      target: { value: "page-guid-2" },
    });
    fireEvent.click(screen.getByTestId("architecture-landing-submit"));
    await waitFor(() => {
      expect(replaceSpy).toHaveBeenCalledTimes(2);
    });
    await waitFor(() => {
      expect(screen.getByTestId("architecture-mutation-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("architecture-landing-current")).toBeNull();
  });

  it("landing cancel does not call replaceLandingPage (#3304)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    const replaceSpy = vi.spyOn(sectionApi, "replaceLandingPage");

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        useLandingContentBrowser={false}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    fireEvent.click(screen.getByTestId("architecture-action-landing"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-landing-cancel")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("architecture-landing-cancel"));
    await waitFor(() => {
      expect(screen.queryByTestId("architecture-landing-dialog")).toBeNull();
    });
    expect(replaceSpy).not.toHaveBeenCalled();
  });

  it("properties is disabled until a regular section is selected (#3353)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    expect(
      (
        screen.getByTestId(
          "architecture-action-properties",
        ) as HTMLButtonElement
      ).disabled,
    ).toBe(true);
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    await waitFor(() => {
      expect(
        (
          screen.getByTestId(
            "architecture-action-properties",
          ) as HTMLButtonElement
        ).disabled,
      ).toBe(false);
    });
  });

  it("loads properties, saves update, and cancel does not POST (#3353)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    const loadProps = vi.spyOn(sectionApi, "loadSectionProperties").mockResolvedValue({
      id: "c1",
      title: "About",
      folderName: "About",
      target: "_self",
      cssClassNames: "",
      requiresLogin: false,
      allowAccessTo: "",
      secureSite: false,
      siteRootSection: false,
      folderPermission: { accessLevel: "WRITE" },
    });
    const updateSpy = vi
      .spyOn(sectionApi, "updateSiteSection")
      .mockResolvedValue({});

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    fireEvent.click(screen.getByTestId("architecture-action-properties"));
    await waitFor(() => {
      expect(loadProps).toHaveBeenCalledWith("c1");
      expect(screen.getByTestId("architecture-properties-title")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("architecture-properties-cancel"));
    await waitFor(() => {
      expect(screen.queryByTestId("architecture-properties-dialog")).toBeNull();
    });
    expect(updateSpy).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("architecture-action-properties"));
    await waitFor(() => {
      const input = screen.getByTestId(
        "architecture-properties-title",
      ) as HTMLInputElement;
      expect(input.value).toBe("About");
      expect(input.disabled).toBe(false);
    });
    fireEvent.change(screen.getByTestId("architecture-properties-title"), {
      target: { value: "About Us" },
    });
    fireEvent.change(screen.getByTestId("architecture-properties-target"), {
      target: { value: "_blank" },
    });
    fireEvent.click(screen.getByTestId("architecture-properties-submit"));
    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          id: "c1",
          title: "About Us",
          folderName: "About",
          target: "_blank",
          folderPermission: { accessLevel: "WRITE" },
        }),
      );
    });
  });

  it("folder ACL is disabled until a regular section is selected (#3588)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    expect(
      (
        screen.getByTestId(
          "architecture-action-folder-acl",
        ) as HTMLButtonElement
      ).disabled,
    ).toBe(true);
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    await waitFor(() => {
      expect(
        (
          screen.getByTestId(
            "architecture-action-folder-acl",
          ) as HTMLButtonElement
        ).disabled,
      ).toBe(false);
    });
  });

  it("opens folder ACL, add/remove write principal, cancel does not save (#3588)", async () => {
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeFixture);
    const save = vi.fn().mockResolvedValue(undefined);
    const load = vi.fn().mockResolvedValue({
      id: "folder-c1",
      name: "About",
      permission: {
        accessLevel: "ADMIN",
        adminPrincipals: [{ type: "USER", name: "Admin" }],
        writePrincipals: [],
        readPrincipals: [],
        viewPrincipals: [],
      },
    });
    const resolveFolderId = vi.fn().mockResolvedValue("folder-c1");

    render(
      <ArchitectureShell
        embedded
        initialSite="Demo"
        resolveFolderId={resolveFolderId}
        loadFolderProperties={load}
        saveFolderProperties={save}
        currentUserIdentities={["Admin"]}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c1"));
    fireEvent.click(screen.getByTestId("architecture-action-folder-acl"));
    await waitFor(() => {
      expect(resolveFolderId).toHaveBeenCalledWith("//Sites/Demo/About");
      expect(screen.getByTestId("folder-security-panel")).toBeTruthy();
    });
    fireEvent.click(
      screen.getByTestId("folder-security-list-writePrincipals-add"),
    );
    fireEvent.change(
      screen.getByTestId("folder-security-list-writePrincipals-input"),
      { target: { value: "night3588" } },
    );
    fireEvent.click(
      screen.getByTestId("folder-security-list-writePrincipals-add-confirm"),
    );
    expect(
      screen.getByTestId(
        "folder-security-list-writePrincipals-remove-night3588",
      ),
    ).toBeTruthy();
    fireEvent.click(
      screen.getByTestId(
        "folder-security-list-writePrincipals-remove-night3588",
      ),
    );
    fireEvent.click(screen.getByTestId("architecture-folder-acl-cancel"));
    await waitFor(() => {
      expect(screen.queryByTestId("architecture-folder-acl-dialog")).toBeNull();
    });
    expect(save).not.toHaveBeenCalled();
  });

  it("treats blog navons as read-only in Navigation (#3351)", async () => {
    const treeWithBlog = {
      ...treeFixture,
      children: [
        ...treeFixture.children,
        {
          id: "blog-1",
          title: "News blog",
          folderPath: "//Sites/Demo/NewsBlog",
          sectionType: "blog" as const,
          requiresLogin: false,
          children: [],
        },
      ],
    };
    vi.spyOn(homeApi, "fetchSites").mockResolvedValue([{ name: "Demo" }]);
    vi.spyOn(sectionApi, "loadSectionTree").mockResolvedValue(treeWithBlog);

    render(<ArchitectureShell embedded initialSite="Demo" />);
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-blog-1")).toBeTruthy();
    });
    expect(screen.getByTestId("nav-tree-badge-blog-1").textContent).toMatch(
      /blog/i,
    );
    expect(screen.getByTestId("architecture-blog-note").textContent).toMatch(
      /blog/i,
    );
    expect(screen.queryByTestId("architecture-action-create-blog")).toBeNull();

    fireEvent.click(screen.getByTestId("nav-tree-item-blog-1"));
    expect(
      (screen.getByTestId("architecture-action-create") as HTMLButtonElement)
        .disabled,
    ).toBe(false);
    expect(
      (screen.getByTestId("architecture-action-landing") as HTMLButtonElement)
        .disabled,
    ).toBe(true);
    expect(
      (
        screen.getByTestId(
          "architecture-action-properties",
        ) as HTMLButtonElement
      ).disabled,
    ).toBe(true);
    expect(
      (
        screen.getByTestId(
          "architecture-action-folder-acl",
        ) as HTMLButtonElement
      ).disabled,
    ).toBe(true);
    expect(
      (screen.getByTestId("architecture-action-rename") as HTMLButtonElement)
        .disabled,
    ).toBe(true);
    expect(
      (
        screen.getByTestId(
          "architecture-action-convert-to-folder",
        ) as HTMLButtonElement
      ).disabled,
    ).toBe(true);
    expect(
      (screen.getByTestId("architecture-action-delete") as HTMLButtonElement)
        .disabled,
    ).toBe(false);
  });
});
