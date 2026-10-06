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
import { CONTENT_LIST_GENERATOR_MAX_LENGTH } from "@/publishing/contentListGenerator";

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
  description: "Night notes",
  listType: "modern",
  generator: "sys_Search",
  url: "/Rhythmyx/contentlist",
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

describe("DesignSection content list generator", () => {
  beforeEach(() => {
    listContentLists.mockReset();
    updateContentList.mockReset();
    listContentLists.mockResolvedValue([modern, legacy]);
  });

  async function openGenerator(): Promise<void> {
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Content lists/i }));
    expect(await screen.findByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
    expect(screen.getByTestId("design-content-list-type-5")).toHaveTextContent(
      "modern",
    );
    expect(screen.getByTestId("design-content-list-description-5")).toHaveTextContent(
      "Night notes",
    );
    expect(screen.getByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
    expect(screen.queryByTestId("design-content-list-generator-8")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("design-content-list-generator-5"));
    expect(await screen.findByTestId("contentlist-generator-form")).toBeTruthy();
    expect(screen.getByTestId("contentlist-generator-name")).toHaveTextContent(
      "NightCl",
    );
    expect(screen.getByTestId("contentlist-generator-description")).toHaveTextContent(
      "Night notes",
    );
    expect(screen.getByTestId("contentlist-generator-type")).toHaveTextContent(
      "modern",
    );
    expect(screen.getByTestId("contentlist-generator-filter")).toHaveTextContent(
      "public",
    );
    expect(screen.getByTestId("contentlist-generator-input")).toHaveValue(
      "sys_Search",
    );
    expect(screen.queryByText("/Rhythmyx/contentlist")).not.toBeInTheDocument();
  }

  it("shows the new generator only after reload and leaves name, description, type, and filter", async () => {
    let releaseReload: ((rows: (typeof modern)[]) => void) | undefined;
    let held = false;
    listContentLists.mockImplementation(
      () =>
        new Promise((resolve) => {
          if (!held) {
            resolve([modern, legacy]);
            return;
          }
          releaseReload = resolve;
        }),
    );
    updateContentList.mockResolvedValue({ ...modern, generator: "sys_Changed" });

    await openGenerator();
    fireEvent.change(screen.getByLabelText("Generator"), {
      target: { value: "  sys_Changed  " },
    });
    expect(updateContentList).not.toHaveBeenCalled();
    held = true;
    fireEvent.click(screen.getByTestId("contentlist-generator-save"));
    expect(await screen.findByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
    expect(screen.getByTestId("design-content-list-5")).toHaveTextContent("NightCl");
    expect(screen.getByTestId("design-content-list-description-5")).toHaveTextContent(
      "Night notes",
    );
    expect(screen.getByTestId("design-content-list-type-5")).toHaveTextContent(
      "modern",
    );
    expect(screen.getByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
    expect(screen.getByTestId("design-content-list-source-8")).toHaveTextContent(
      "/Rhythmyx/legacyList",
    );
    await waitFor(() =>
      expect(updateContentList).toHaveBeenCalledWith("5", {
        generator: "sys_Changed",
      }),
    );
    const body = updateContentList.mock.calls[0][1] as Record<string, unknown>;
    expect(body).not.toHaveProperty("name");
    expect(body).not.toHaveProperty("description");
    expect(body).not.toHaveProperty("listType");
    expect(body).not.toHaveProperty("url");
    expect(body).not.toHaveProperty("itemFilterId");
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.([
      { ...modern, generator: "sys_Changed" },
      legacy,
    ]);
    await waitFor(() =>
      expect(screen.getByTestId("design-content-list-source-5")).toHaveTextContent(
        "sys_Changed",
      ),
    );
    expect(screen.getByTestId("design-content-list-5")).toHaveTextContent("NightCl");
    expect(screen.getByTestId("design-content-list-description-5")).toHaveTextContent(
      "Night notes",
    );
    expect(screen.getByTestId("design-content-list-type-5")).toHaveTextContent(
      "modern",
    );
    expect(screen.getByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
    expect(screen.getByTestId("design-content-list-source-8")).toHaveTextContent(
      "/Rhythmyx/legacyList",
    );
  });

  it("keeps the new generator when the reload fails", async () => {
    let failReload = false;
    listContentLists.mockImplementation(() => {
      if (failReload) {
        return Promise.reject(new Error("reload failed"));
      }
      return Promise.resolve([modern, legacy]);
    });
    updateContentList.mockResolvedValue({ ...modern, generator: "sys_Kept" });
    await openGenerator();
    fireEvent.change(screen.getByLabelText("Generator"), {
      target: { value: "sys_Kept" },
    });
    failReload = true;
    fireEvent.click(screen.getByTestId("contentlist-generator-save"));
    await waitFor(() =>
      expect(screen.getByTestId("design-content-list-source-5")).toHaveTextContent(
        "sys_Kept",
      ),
    );
    expect(screen.getByTestId("design-content-list-5")).toHaveTextContent("NightCl");
    expect(screen.getByTestId("design-content-list-description-5")).toHaveTextContent(
      "Night notes",
    );
    expect(screen.getByTestId("design-content-list-type-5")).toHaveTextContent(
      "modern",
    );
    expect(screen.getByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
    expect(screen.getByTestId("design-content-list-source-8")).toHaveTextContent(
      "/Rhythmyx/legacyList",
    );
  });

  it("cancel does not call save", async () => {
    await openGenerator();
    fireEvent.change(screen.getByLabelText("Generator"), {
      target: { value: "sys_NotSaved" },
    });
    fireEvent.click(screen.getByTestId("contentlist-generator-cancel"));
    expect(await screen.findByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
    expect(screen.getByTestId("design-content-list-description-5")).toHaveTextContent(
      "Night notes",
    );
    expect(updateContentList).not.toHaveBeenCalled();
  });

  it("rejects a blank generator without calling the server", async () => {
    await openGenerator();
    fireEvent.change(screen.getByLabelText("Generator"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("contentlist-generator-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Content list generator is required",
    );
    expect(updateContentList).not.toHaveBeenCalled();
    expect(screen.getByTestId("contentlist-generator-form")).toBeTruthy();
    fireEvent.click(screen.getByTestId("contentlist-generator-cancel"));
    expect(await screen.findByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
  });

  it("rejects an overlong generator without calling the server", async () => {
    await openGenerator();
    fireEvent.change(screen.getByLabelText("Generator"), {
      target: { value: "g".repeat(CONTENT_LIST_GENERATOR_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("contentlist-generator-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Content list generator must be 256 characters or fewer",
    );
    expect(updateContentList).not.toHaveBeenCalled();
    expect(screen.getByTestId("contentlist-generator-form")).toBeTruthy();
  });

  it.each([
    [400, "Content list generator must be 256 characters or fewer"],
    [403, "Admin or Designer role required to save a publish edition"],
    [409, "Content list name already exists"],
  ])("HTTP %s leaves the previous generator", async (status, message) => {
    updateContentList.mockRejectedValue({
      status,
      statusText: "error",
      body: { message },
    });
    await openGenerator();
    fireEvent.change(screen.getByLabelText("Generator"), {
      target: { value: "sys_Rejected" },
    });
    fireEvent.click(screen.getByTestId("contentlist-generator-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.getByTestId("contentlist-generator-form")).toBeTruthy();
    expect(screen.getByTestId("contentlist-generator-name")).toHaveTextContent(
      "NightCl",
    );
    expect(screen.getByTestId("contentlist-generator-description")).toHaveTextContent(
      "Night notes",
    );
    expect(screen.getByTestId("contentlist-generator-filter")).toHaveTextContent(
      "public",
    );
    expect(screen.queryByTestId("design-content-list-source-5")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("contentlist-generator-cancel"));
    expect(await screen.findByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
    expect(screen.getByTestId("design-content-list-5")).toHaveTextContent("NightCl");
    expect(screen.getByTestId("design-content-list-description-5")).toHaveTextContent(
      "Night notes",
    );
    expect(screen.getByTestId("design-content-list-type-5")).toHaveTextContent(
      "modern",
    );
    expect(screen.getByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
    expect(screen.getByTestId("design-content-list-source-8")).toHaveTextContent(
      "/Rhythmyx/legacyList",
    );
  });
});
