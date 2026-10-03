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
const createEdition = vi.fn();

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([
    { name: "SiteA", siteId: "1" },
    { name: "SiteB", siteId: "2" },
  ]),
}));

vi.mock("@/api/publishing/designApi", () => ({
  listEditionsBySite: (...args: unknown[]) => listEditionsBySite(...args),
  createEdition: (...args: unknown[]) => createEdition(...args),
  updateEdition: vi.fn(),
  deleteEdition: vi.fn(),
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

describe("DesignSection create edition", () => {
  beforeEach(() => {
    listEditionsBySite.mockReset();
    createEdition.mockReset();
  });

  it("shows a created edition on the open site", async () => {
    let created = false;
    listEditionsBySite.mockImplementation(async (siteId: string) => {
      if (siteId === "1" && created) {
        return [{ editionId: "12", name: "NightEd", siteId: "1" }];
      }
      return [];
    });
    createEdition.mockImplementation(async (body: { name?: string; siteId?: string }) => {
      created = true;
      return { editionId: "12", name: body.name, siteId: body.siteId };
    });

    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Editions/i }));
    await screen.findByRole("option", { name: "SiteA" });
    expect(screen.getByLabelText("Design site")).toHaveValue("1");
    fireEvent.click(screen.getByTestId("design-add-edition"));
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "NightEd" },
    });
    fireEvent.click(screen.getByTestId("edition-save"));

    expect(await screen.findByTestId("design-edition-12")).toHaveTextContent("NightEd");
    await waitFor(() =>
      expect(createEdition).toHaveBeenCalledWith(
        expect.objectContaining({ name: "NightEd", siteId: "1" }),
      ),
    );
    expect(screen.getByLabelText("Design site")).toHaveValue("1");
  });
});
