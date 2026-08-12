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
      /Architecture/i,
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
    } as never);
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
});
