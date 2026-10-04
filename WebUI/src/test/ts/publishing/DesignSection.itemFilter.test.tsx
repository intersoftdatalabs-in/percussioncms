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

const listContentLists = vi.fn();
const updateContentList = vi.fn();
const listItemFilters = vi.fn();

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([{ name: "SiteA", siteId: "1" }]),
}));

vi.mock("@/api/developer/itemFiltersApi", () => ({
  listItemFilters: () => listItemFilters(),
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

const original = {
  contentListId: "5",
  name: "NightCl",
  description: "old",
  listType: "modern",
  itemFilterId: "1",
  itemFilterName: "public",
};

describe("DesignSection item filter", () => {
  beforeEach(() => {
    listContentLists.mockReset();
    updateContentList.mockReset();
    listItemFilters.mockReset();
    listContentLists.mockResolvedValue([original]);
    listItemFilters.mockResolvedValue([
      { name: "public", filterId: { uuid: 1 } },
      { name: "preview", filterId: { uuid: 2 } },
    ]);
  });

  async function openList(): Promise<void> {
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Content lists/i }));
    expect(await screen.findByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
    fireEvent.click(screen.getByTestId("design-content-list-5"));
    await screen.findByRole("option", { name: "preview" });
  }

  it("shows the new filter on the list only after save reloads", async () => {
    let releaseReload: (() => void) | undefined;
    let calls = 0;
    listContentLists.mockImplementation(
      () =>
        new Promise((resolve) => {
          calls += 1;
          if (calls === 1) {
            resolve([original]);
            return;
          }
          releaseReload = () =>
            resolve([
              {
                ...original,
                itemFilterId: "2",
                itemFilterName: "preview",
              },
            ]);
        }),
    );
    updateContentList.mockResolvedValue({
      ...original,
      itemFilterId: "2",
      itemFilterName: "preview",
    });

    await openList();
    fireEvent.change(screen.getByTestId("contentlist-item-filter"), {
      target: { value: "2" },
    });
    expect(screen.getByTestId("contentlist-stored-item-filter")).toHaveTextContent(
      "public",
    );
    expect(screen.queryByTestId("design-content-list-filter-5")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("contentlist-save"));
    expect(await screen.findByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.();
    await waitFor(() =>
      expect(screen.getByTestId("design-content-list-filter-5")).toHaveTextContent(
        "preview",
      ),
    );
  });

  it("keeps the previous filter when save is cancelled", async () => {
    await openList();
    fireEvent.change(screen.getByTestId("contentlist-item-filter"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByRole("button", { name: /Back/i }));
    expect(updateContentList).not.toHaveBeenCalled();
    expect(await screen.findByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
  });

  it("keeps the previous filter when the server rejects the save", async () => {
    updateContentList.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "Unknown item filter" },
    });
    await openList();
    fireEvent.change(screen.getByTestId("contentlist-item-filter"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByTestId("contentlist-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Unknown item filter");
    expect(screen.getByTestId("contentlist-stored-item-filter")).toHaveTextContent(
      "public",
    );
    fireEvent.click(screen.getByRole("button", { name: /Back/i }));
    expect(await screen.findByTestId("design-content-list-filter-5")).toHaveTextContent(
      "public",
    );
    expect(listContentLists).toHaveBeenCalledTimes(1);
  });
});
