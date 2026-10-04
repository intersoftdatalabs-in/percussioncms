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
const deleteContentList = vi.fn();

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
  updateContentList: vi.fn(),
  deleteContentList: (...args: unknown[]) => deleteContentList(...args),
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

describe("DesignSection delete content list", () => {
  beforeEach(() => {
    listContentLists.mockReset();
    deleteContentList.mockReset();
    vi.restoreAllMocks();
  });

  it("removes a deleted content list from the list", async () => {
    let deleted = false;
    listContentLists.mockImplementation(async () => {
      if (deleted) {
        return [];
      }
      return [{ contentListId: "5", name: "NightCl", listType: "modern" }];
    });
    deleteContentList.mockImplementation(async () => {
      deleted = true;
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Content lists/i }));
    fireEvent.click(await screen.findByTestId("design-content-list-5"));
    fireEvent.click(screen.getByTestId("contentlist-delete"));

    expect(await screen.findByText("No content lists.")).toBeInTheDocument();
    await waitFor(() => expect(deleteContentList).toHaveBeenCalledWith("5"));
    expect(screen.queryByTestId("design-content-list-5")).not.toBeInTheDocument();
  });

  it("keeps the content list when delete is in use", async () => {
    listContentLists.mockResolvedValue([
      { contentListId: "5", name: "NightCl", listType: "modern" },
    ]);
    deleteContentList.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Content list is in use" },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Content lists/i }));
    fireEvent.click(await screen.findByTestId("design-content-list-5"));
    fireEvent.click(screen.getByTestId("contentlist-delete"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Content list is in use",
    );
    fireEvent.click(screen.getByRole("button", { name: /Back/i }));
    expect(await screen.findByTestId("design-content-list-5")).toHaveTextContent(
      "NightCl",
    );
  });
});
