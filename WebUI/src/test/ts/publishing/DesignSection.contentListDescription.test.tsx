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
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DesignSection } from "@/publishing/sections/DesignSection";
import { CONTENT_LIST_DESCRIPTION_MAX_LENGTH } from "@/publishing/contentListDescription";

const listContentLists = vi.fn();
const updateContentList = vi.fn();

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([{ name: "SiteA", siteId: "1" }]),
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
  deleteContentList: vi.fn(),
  copyContentList: vi.fn(),
  listContexts: vi.fn().mockResolvedValue([]),
  associateContentList: vi.fn(),
  disassociateContentList: vi.fn(),
  reorderEditionContentList: vi.fn(),
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

describe("DesignSection content list description", () => {
  beforeEach(() => {
    listContentLists.mockReset();
    updateContentList.mockReset();
    listContentLists.mockResolvedValue([modern]);
  });

  async function openDescribe(): Promise<void> {
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Content lists/i }));
    expect(
      await screen.findByTestId("design-content-list-description-5"),
    ).toHaveTextContent("old notes");
    expect(screen.getByTestId("design-content-list-type-5")).toHaveTextContent(
      "modern",
    );
    expect(screen.getByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
    expect(screen.getByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
    fireEvent.click(screen.getByTestId("design-content-list-describe-5"));
    expect(await screen.findByTestId("contentlist-description-form")).toBeTruthy();
    expect(screen.getByTestId("contentlist-description-name")).toHaveTextContent(
      "NightCl",
    );
    expect(screen.getByTestId("contentlist-description-type")).toHaveTextContent(
      "modern",
    );
    expect(screen.getByTestId("contentlist-description-source")).toHaveTextContent(
      "sys_Search",
    );
    expect(screen.getByTestId("contentlist-description-filter")).toHaveTextContent(
      "public",
    );
    expect(screen.getByTestId("contentlist-description-input")).toHaveValue(
      "old notes",
    );
  }

  it("shows the new description only after reload and leaves name, type, generator, and filter", async () => {
    let releaseReload: ((rows: (typeof modern)[]) => void) | undefined;
    let held = false;
    listContentLists.mockImplementation(
      () =>
        new Promise((resolve) => {
          if (!held) {
            resolve([modern]);
            return;
          }
          releaseReload = resolve;
        }),
    );
    updateContentList.mockResolvedValue({ ...modern, description: "night note" });

    await openDescribe();
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "  night note  " },
    });
    expect(updateContentList).not.toHaveBeenCalled();
    held = true;
    fireEvent.click(screen.getByTestId("contentlist-description-save"));
    expect(
      await screen.findByTestId("design-content-list-description-5"),
    ).toHaveTextContent("old notes");
    expect(screen.getByTestId("design-content-list-5")).toHaveTextContent("NightCl");
    expect(screen.getByTestId("design-content-list-type-5")).toHaveTextContent(
      "modern",
    );
    expect(screen.getByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
    expect(screen.getByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
    await waitFor(() =>
      expect(updateContentList).toHaveBeenCalledWith("5", {
        description: "night note",
      }),
    );
    const body = updateContentList.mock.calls[0][1] as Record<string, unknown>;
    expect(body).not.toHaveProperty("name");
    expect(body).not.toHaveProperty("listType");
    expect(body).not.toHaveProperty("generator");
    expect(body).not.toHaveProperty("url");
    expect(body).not.toHaveProperty("itemFilterId");
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.([{ ...modern, description: "night note" }]);
    await waitFor(() =>
      expect(
        screen.getByTestId("design-content-list-description-5"),
      ).toHaveTextContent("night note"),
    );
    expect(screen.getByTestId("design-content-list-5")).toHaveTextContent("NightCl");
    expect(screen.getByTestId("design-content-list-type-5")).toHaveTextContent(
      "modern",
    );
    expect(screen.getByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
    expect(screen.getByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
  });

  it("shows a legacy URL and does not send it with the description", async () => {
    listContentLists.mockResolvedValue([legacy]);
    updateContentList.mockResolvedValue({ ...legacy, description: "next" });
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Content lists/i }));
    expect(
      await screen.findByTestId("design-content-list-source-8"),
    ).toHaveTextContent("/Rhythmyx/legacyList");
    fireEvent.click(screen.getByTestId("design-content-list-describe-8"));
    expect(await screen.findByTestId("contentlist-description-source")).toHaveTextContent(
      "/Rhythmyx/legacyList",
    );
    expect(screen.getByText(/Legacy URL/)).toBeTruthy();
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "next" },
    });
    fireEvent.click(screen.getByTestId("contentlist-description-save"));
    await waitFor(() =>
      expect(updateContentList).toHaveBeenCalledWith("8", { description: "next" }),
    );
    const body = updateContentList.mock.calls[0][1] as Record<string, unknown>;
    expect(body).not.toHaveProperty("url");
    expect(body).not.toHaveProperty("name");
    expect(body).not.toHaveProperty("listType");
  });

  it("clears the stored description when the field is blank", async () => {
    updateContentList.mockResolvedValue({ ...modern, description: "" });
    listContentLists
      .mockResolvedValueOnce([modern])
      .mockResolvedValueOnce([{ ...modern, description: "" }]);
    await openDescribe();
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("contentlist-description-save"));
    await waitFor(() =>
      expect(updateContentList).toHaveBeenCalledWith("5", { description: "" }),
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("design-content-list-description-5"),
      ).toHaveTextContent(""),
    );
    expect(screen.getByTestId("design-content-list-5")).toHaveTextContent("NightCl");
    expect(screen.getByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
    expect(screen.getByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
  });

  it("keeps the new description when the reload fails", async () => {
    let failReload = false;
    listContentLists.mockImplementation(() => {
      if (failReload) {
        return Promise.reject(new Error("reload failed"));
      }
      return Promise.resolve([modern]);
    });
    updateContentList.mockResolvedValue({ ...modern, description: "kept" });
    await openDescribe();
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "kept" },
    });
    failReload = true;
    fireEvent.click(screen.getByTestId("contentlist-description-save"));
    await waitFor(() =>
      expect(
        screen.getByTestId("design-content-list-description-5"),
      ).toHaveTextContent("kept"),
    );
    expect(screen.getByTestId("design-content-list-5")).toHaveTextContent("NightCl");
    expect(screen.getByTestId("design-content-list-type-5")).toHaveTextContent(
      "modern",
    );
    expect(screen.getByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
    expect(screen.getByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
  });

  it("cancel does not call save", async () => {
    await openDescribe();
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "not saved" },
    });
    fireEvent.click(screen.getByTestId("contentlist-description-cancel"));
    expect(
      await screen.findByTestId("design-content-list-description-5"),
    ).toHaveTextContent("old notes");
    expect(screen.getByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
    expect(updateContentList).not.toHaveBeenCalled();
  });

  it("rejects an overlong description without calling the server", async () => {
    await openDescribe();
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "d".repeat(CONTENT_LIST_DESCRIPTION_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("contentlist-description-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Content list description must be 255 characters or fewer",
    );
    expect(updateContentList).not.toHaveBeenCalled();
    expect(screen.getByTestId("contentlist-description-form")).toBeTruthy();
    fireEvent.click(screen.getByTestId("contentlist-description-cancel"));
    expect(
      await screen.findByTestId("design-content-list-description-5"),
    ).toHaveTextContent("old notes");
  });

  it.each([
    [400, "Content list description must be 255 characters or fewer"],
    [403, "Admin or Designer role required to save a publish edition"],
    [409, "Content list name already exists"],
  ])("HTTP %s leaves the previous description", async (status, message) => {
    updateContentList.mockRejectedValue({
      status,
      statusText: "error",
      body: { message },
    });
    await openDescribe();
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "rejected" },
    });
    fireEvent.click(screen.getByTestId("contentlist-description-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.getByTestId("contentlist-description-form")).toBeTruthy();
    expect(screen.getByTestId("contentlist-description-name")).toHaveTextContent(
      "NightCl",
    );
    expect(screen.getByTestId("contentlist-description-source")).toHaveTextContent(
      "sys_Search",
    );
    expect(screen.getByTestId("contentlist-description-filter")).toHaveTextContent(
      "public",
    );
    expect(
      screen.queryByTestId("design-content-list-description-5"),
    ).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("contentlist-description-cancel"));
    expect(
      await screen.findByTestId("design-content-list-description-5"),
    ).toHaveTextContent("old notes");
    expect(screen.getByTestId("design-content-list-5")).toHaveTextContent("NightCl");
    expect(screen.getByTestId("design-content-list-type-5")).toHaveTextContent(
      "modern",
    );
    expect(screen.getByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
    expect(screen.getByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
  });
});
