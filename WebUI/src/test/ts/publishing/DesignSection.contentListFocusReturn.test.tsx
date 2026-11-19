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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DesignSection } from "@/publishing/sections/DesignSection";

const listContentLists = vi.fn();
const updateContentList = vi.fn();
const copyContentList = vi.fn();
const deleteContentList = vi.fn();

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([{ name: "SiteA", siteId: "1" }]),
}));

vi.mock("@/api/developer/itemFiltersApi", () => ({
  listItemFilters: () => Promise.resolve([]),
}));

vi.mock("@/api/publishing/designApi", () => ({
  listEditionsBySite: vi.fn().mockResolvedValue([]),
  createEdition: vi.fn(),
  updateEdition: vi.fn(),
  deleteEdition: vi.fn(),
  copyEdition: vi.fn(),
  listEditionContentLists: vi.fn().mockResolvedValue([]),
  listContentLists: (...args: unknown[]) => listContentLists(...args),
  createContentList: vi.fn(),
  updateContentList: (...args: unknown[]) => updateContentList(...args),
  deleteContentList: (...args: unknown[]) => deleteContentList(...args),
  copyContentList: (...args: unknown[]) => copyContentList(...args),
  listContexts: vi.fn().mockResolvedValue([]),
  associateContentList: vi.fn(),
  disassociateContentList: vi.fn(),
  listDeliveryTypes: vi.fn().mockResolvedValue([]),
  listSchemesForContext: vi.fn().mockResolvedValue([]),
  listDesignSites: vi
    .fn()
    .mockResolvedValue([{ siteId: "1", name: "S1", folderRoot: "//Sites/S1" }]),
  listSiteProperties: vi.fn().mockResolvedValue([]),
  deleteSiteProperty: vi.fn(),
  putSiteProperty: vi.fn(),
}));

const modern = {
  contentListId: "5",
  name: "NightCl",
  description: "old notes",
  listType: "modern",
  generator: "sys_Search",
  itemFilterId: "public",
  itemFilterName: "public",
};

const legacy = {
  contentListId: "8",
  name: "LegacyCl",
  description: "legacy notes",
  listType: "legacy",
  url: "/Rhythmyx/legacyList",
};

describe("DesignSection content list focus return (#6748)", () => {
  beforeEach(() => {
    listContentLists.mockReset();
    updateContentList.mockReset();
    copyContentList.mockReset();
    deleteContentList.mockReset();
    listContentLists.mockResolvedValue([modern]);
  });

  async function openContentLists(): Promise<void> {
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Content lists/i }));
    await screen.findByTestId("design-content-list-5");
  }

  async function openDescribe(): Promise<void> {
    await openContentLists();
    fireEvent.click(screen.getByTestId("design-content-list-describe-5"));
    expect(await screen.findByTestId("contentlist-description-form")).toBeTruthy();
  }

  async function openCopy(): Promise<void> {
    await openContentLists();
    fireEvent.click(screen.getByTestId("design-content-list-copy-5"));
    expect(await screen.findByTestId("contentlist-copy")).toBeTruthy();
  }

  async function openGenerator(): Promise<void> {
    await openContentLists();
    fireEvent.click(screen.getByTestId("design-content-list-generator-5"));
    expect(await screen.findByTestId("contentlist-generator-form")).toBeTruthy();
  }

  async function openUrl(): Promise<void> {
    listContentLists.mockResolvedValue([legacy]);
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Content lists/i }));
    await screen.findByTestId("design-content-list-8");
    fireEvent.click(screen.getByTestId("design-content-list-url-8"));
    expect(await screen.findByTestId("contentlist-url-form")).toBeTruthy();
  }

  async function openEditor(): Promise<void> {
    await openContentLists();
    fireEvent.click(screen.getByTestId("design-content-list-5"));
    expect(await screen.findByTestId("contentlist-editor")).toBeTruthy();
  }

  it("returns focus to the Description row button after Cancel", async () => {
    await openDescribe();
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "not-saved" },
    });
    fireEvent.click(screen.getByTestId("contentlist-description-cancel"));
    expect(updateContentList).not.toHaveBeenCalled();
    expect(await screen.findByTestId("design-content-list-5")).toBeTruthy();
    expect(screen.getByTestId("design-content-list-describe-5")).toHaveFocus();
  });

  it("returns focus to the Description row button after Escape", async () => {
    await openDescribe();
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "not-saved" },
    });
    fireEvent.keyDown(screen.getByLabelText("Description"), { key: "Escape" });
    expect(updateContentList).not.toHaveBeenCalled();
    expect(await screen.findByTestId("design-content-list-5")).toBeTruthy();
    expect(screen.getByTestId("design-content-list-describe-5")).toHaveFocus();
  });

  it("returns focus to the Description row button after save", async () => {
    updateContentList.mockResolvedValue({ ...modern, description: "night note" });
    listContentLists
      .mockResolvedValueOnce([modern])
      .mockResolvedValue([{ ...modern, description: "night note" }]);
    await openDescribe();
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "night note" },
    });
    fireEvent.click(screen.getByTestId("contentlist-description-save"));
    await waitFor(() =>
      expect(updateContentList).toHaveBeenCalledWith("5", {
        description: "night note",
      }),
    );
    expect(await screen.findByTestId("design-content-list-5")).toBeTruthy();
    expect(screen.getByTestId("design-content-list-describe-5")).toHaveFocus();
  });

  it("returns focus to the Copy row button after Cancel", async () => {
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "Nope" },
    });
    fireEvent.click(screen.getByTestId("contentlist-copy-cancel"));
    expect(copyContentList).not.toHaveBeenCalled();
    expect(await screen.findByTestId("design-content-list-5")).toBeTruthy();
    expect(screen.getByTestId("design-content-list-copy-5")).toHaveFocus();
  });

  it("returns focus to the Copy row button after a successful copy", async () => {
    copyContentList.mockResolvedValue({ ...modern, contentListId: "9", name: "NightCl copy" });
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "NightCl copy" },
    });
    fireEvent.click(screen.getByTestId("contentlist-copy-submit"));
    await waitFor(() => expect(copyContentList).toHaveBeenCalled());
    expect(await screen.findByTestId("design-content-list-5")).toBeTruthy();
    expect(screen.getByTestId("design-content-list-copy-5")).toHaveFocus();
  });

  it("returns focus to the Generator row button after Cancel and save", async () => {
    await openGenerator();
    fireEvent.click(screen.getByTestId("contentlist-generator-cancel"));
    expect(updateContentList).not.toHaveBeenCalled();
    expect(await screen.findByTestId("design-content-list-5")).toBeTruthy();
    expect(screen.getByTestId("design-content-list-generator-5")).toHaveFocus();

    fireEvent.click(screen.getByTestId("design-content-list-generator-5"));
    expect(await screen.findByTestId("contentlist-generator-form")).toBeTruthy();
    updateContentList.mockResolvedValue({ ...modern, generator: "sys_Next" });
    fireEvent.change(screen.getByLabelText("Generator"), {
      target: { value: "sys_Next" },
    });
    fireEvent.click(screen.getByTestId("contentlist-generator-save"));
    await waitFor(() =>
      expect(updateContentList).toHaveBeenCalledWith("5", {
        generator: "sys_Next",
      }),
    );
    expect(await screen.findByTestId("design-content-list-5")).toBeTruthy();
    expect(screen.getByTestId("design-content-list-generator-5")).toHaveFocus();
  });

  it("returns focus to the Legacy URL row button after Cancel and save", async () => {
    await openUrl();
    fireEvent.click(screen.getByTestId("contentlist-url-cancel"));
    expect(updateContentList).not.toHaveBeenCalled();
    expect(await screen.findByTestId("design-content-list-8")).toBeTruthy();
    expect(screen.getByTestId("design-content-list-url-8")).toHaveFocus();

    fireEvent.click(screen.getByTestId("design-content-list-url-8"));
    expect(await screen.findByTestId("contentlist-url-form")).toBeTruthy();
    updateContentList.mockResolvedValue({ ...legacy, url: "/Rhythmyx/next" });
    listContentLists.mockResolvedValue([{ ...legacy, url: "/Rhythmyx/next" }]);
    fireEvent.change(screen.getByLabelText("Legacy URL"), {
      target: { value: "/Rhythmyx/next" },
    });
    fireEvent.click(screen.getByTestId("contentlist-url-save"));
    await waitFor(() =>
      expect(updateContentList).toHaveBeenCalledWith("8", {
        url: "/Rhythmyx/next",
      }),
    );
    expect(await screen.findByTestId("design-content-list-8")).toBeTruthy();
    expect(screen.getByTestId("design-content-list-url-8")).toHaveFocus();
  });

  it("returns focus to the row button after editor Back and rename save", async () => {
    await openEditor();
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "Nope" },
    });
    fireEvent.click(screen.getByRole("button", { name: /Back/i }));
    expect(updateContentList).not.toHaveBeenCalled();
    expect(await screen.findByTestId("design-content-list-5")).toBeTruthy();
    expect(screen.getByTestId("design-content-list-5")).toHaveFocus();

    fireEvent.click(screen.getByTestId("design-content-list-5"));
    expect(await screen.findByTestId("contentlist-editor")).toBeTruthy();
    updateContentList.mockResolvedValue({ ...modern, name: "RenamedCl" });
    listContentLists.mockResolvedValue([{ ...modern, name: "RenamedCl" }]);
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "RenamedCl" },
    });
    fireEvent.click(screen.getByTestId("contentlist-save"));
    await waitFor(() => expect(updateContentList).toHaveBeenCalled());
    expect(await screen.findByTestId("design-content-list-5")).toBeTruthy();
    expect(screen.getByTestId("design-content-list-5")).toHaveFocus();
  });

  it("falls back to Add content list after a confirmed delete removes the row", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    deleteContentList.mockResolvedValue(undefined);
    listContentLists.mockResolvedValueOnce([modern]).mockResolvedValue([]);
    await openEditor();
    fireEvent.click(screen.getByTestId("contentlist-delete"));
    await waitFor(() => expect(deleteContentList).toHaveBeenCalledWith("5"));
    expect(await screen.findByTestId("design-add-content-list")).toBeTruthy();
    expect(screen.queryByTestId("design-content-list-5")).not.toBeInTheDocument();
    expect(screen.getByTestId("design-add-content-list")).toHaveFocus();
    vi.restoreAllMocks();
  });
});
