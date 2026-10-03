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

const listEditionsBySite = vi.fn();
const deleteEdition = vi.fn();

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([{ name: "SiteA", siteId: "1" }]),
}));

vi.mock("@/api/publishing/designApi", () => ({
  listEditionsBySite: (...args: unknown[]) => listEditionsBySite(...args),
  createEdition: vi.fn(),
  updateEdition: vi.fn(),
  deleteEdition: (...args: unknown[]) => deleteEdition(...args),
  copyEdition: vi.fn(),
  listEditionContentLists: vi.fn().mockResolvedValue([]),
  listContentLists: vi.fn().mockResolvedValue([]),
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

describe("DesignSection delete edition", () => {
  beforeEach(() => {
    listEditionsBySite.mockReset();
    deleteEdition.mockReset();
    vi.restoreAllMocks();
  });

  it("removes a deleted edition from the open site list", async () => {
    let deleted = false;
    listEditionsBySite.mockImplementation(async () => {
      if (deleted) {
        return [];
      }
      return [{ editionId: "12", name: "NightEd", siteId: "1" }];
    });
    deleteEdition.mockImplementation(async () => {
      deleted = true;
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Editions/i }));
    fireEvent.click(await screen.findByTestId("design-edition-12"));
    fireEvent.click(screen.getByTestId("edition-delete"));

    expect(await screen.findByText("No editions for this site.")).toBeInTheDocument();
    await waitFor(() => expect(deleteEdition).toHaveBeenCalledWith("12"));
    expect(screen.queryByTestId("design-edition-12")).not.toBeInTheDocument();
  });

  it("keeps the edition listed when delete is in use", async () => {
    listEditionsBySite.mockResolvedValue([
      { editionId: "12", name: "NightEd", siteId: "1" },
    ]);
    deleteEdition.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Edition is in use" },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Editions/i }));
    fireEvent.click(await screen.findByTestId("design-edition-12"));
    fireEvent.click(screen.getByTestId("edition-delete"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Edition is in use");
    fireEvent.click(screen.getByRole("button", { name: /Back/i }));
    expect(await screen.findByTestId("design-edition-12")).toHaveTextContent("NightEd");
  });
});
