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
const updateEdition = vi.fn();
const listEditionContentLists = vi.fn();
const reorderEditionContentList = vi.fn();
const associateContentList = vi.fn();
const disassociateContentList = vi.fn();

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([{ name: "SiteA", siteId: "1" }]),
}));

vi.mock("@/api/publishing/designApi", () => ({
  listEditionsBySite: (...args: unknown[]) => listEditionsBySite(...args),
  createEdition: vi.fn(),
  updateEdition: (...args: unknown[]) => updateEdition(...args),
  deleteEdition: vi.fn(),
  copyEdition: vi.fn(),
  listEditionContentLists: (...args: unknown[]) =>
    listEditionContentLists(...args),
  listContentLists: vi.fn().mockResolvedValue([]),
  listContexts: vi.fn().mockResolvedValue([]),
  associateContentList: (...args: unknown[]) => associateContentList(...args),
  disassociateContentList: (...args: unknown[]) =>
    disassociateContentList(...args),
  reorderEditionContentList: (...args: unknown[]) =>
    reorderEditionContentList(...args),
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
  editionId: "12",
  name: "NightEd",
  siteId: "1",
  comment: "keep-me",
  priority: 5,
};

const lists = [
  { contentListId: "a", name: "Alpha", listType: "modern" },
  { contentListId: "b", name: "Beta", listType: "modern" },
];

describe("DesignSection rename edition", () => {
  beforeEach(() => {
    listEditionsBySite.mockReset();
    updateEdition.mockReset();
    listEditionContentLists.mockReset();
    reorderEditionContentList.mockReset();
    associateContentList.mockReset();
    disassociateContentList.mockReset();
    listEditionsBySite.mockResolvedValue([original]);
    listEditionContentLists.mockResolvedValue(lists);
  });

  async function openEdition(): Promise<void> {
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Editions/i }));
    fireEvent.click(await screen.findByTestId("design-edition-12"));
    expect(await screen.findByTestId("edition-assoc-name-a")).toHaveTextContent(
      "Alpha",
    );
  }

  it("shows the new name only after the rename reload, with the same priority and order", async () => {
    let releaseReload: ((rows: typeof original[]) => void) | undefined;
    let held = false;
    listEditionsBySite.mockImplementation(
      () =>
        new Promise((resolve) => {
          if (!held) {
            resolve([original]);
            return;
          }
          releaseReload = resolve;
        }),
    );
    updateEdition.mockResolvedValue({ ...original, name: "RenamedEd" });

    await openEdition();
    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "RenamedEd" },
    });
    expect(screen.queryByTestId("design-edition-12")).not.toBeInTheDocument();
    expect(updateEdition).not.toHaveBeenCalled();
    held = true;
    fireEvent.click(screen.getByTestId("edition-save"));
    expect(await screen.findByTestId("design-edition-12")).toHaveTextContent(
      "NightEd",
    );
    expect(screen.queryByRole("button", { name: "RenamedEd" })).not.toBeInTheDocument();
    await waitFor(() =>
      expect(updateEdition).toHaveBeenCalledWith("12", {
        editionId: "12",
        name: "RenamedEd",
        siteId: "1",
      }),
    );
    expect(reorderEditionContentList).not.toHaveBeenCalled();
    expect(associateContentList).not.toHaveBeenCalled();
    expect(disassociateContentList).not.toHaveBeenCalled();
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.([{ ...original, name: "RenamedEd" }]);
    await waitFor(() =>
      expect(screen.getByTestId("design-edition-12")).toHaveTextContent(
        "RenamedEd",
      ),
    );

    fireEvent.click(screen.getByTestId("design-edition-12"));
    expect(await screen.findByLabelText(/Priority/i)).toHaveValue(5);
    expect(screen.getByLabelText(/^Comment$/)).toHaveValue("keep-me");
    const names = await screen.findAllByTestId(/edition-assoc-name-/);
    expect(names.map((node) => node.textContent)).toEqual(["Alpha", "Beta"]);
  });

  it("does not PUT when rename is cancelled", async () => {
    await openEdition();
    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "Nope" },
    });
    fireEvent.click(screen.getByRole("button", { name: /Back/i }));
    expect(updateEdition).not.toHaveBeenCalled();
    expect(await screen.findByTestId("design-edition-12")).toHaveTextContent(
      "NightEd",
    );
  });

  it("rejects a blank or over-long name without calling the server", async () => {
    await openEdition();
    const loads = listEditionsBySite.mock.calls.length;
    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: " " },
    });
    fireEvent.click(screen.getByTestId("edition-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Name is required");
    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "N".repeat(101) },
    });
    fireEvent.click(screen.getByTestId("edition-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Edition name must be 100 characters or fewer",
    );
    expect(updateEdition).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: /Back/i }));
    expect(await screen.findByTestId("design-edition-12")).toHaveTextContent(
      "NightEd",
    );
    expect(listEditionsBySite).toHaveBeenCalledTimes(loads);
  });

  it.each([
    [400, "Bad Request", "name and siteId are required"],
    [403, "Forbidden", "Admin or Designer role required to save a publish edition"],
    [409, "Conflict", "Edition name already exists"],
  ])(
    "keeps the previous name when HTTP %s rejects the rename",
    async (status, statusText, message) => {
      updateEdition.mockRejectedValue({
        status,
        statusText,
        body: { message },
      });
      await openEdition();
      const loads = listEditionsBySite.mock.calls.length;
      fireEvent.change(screen.getByLabelText("* Name"), {
        target: { value: "Taken" },
      });
      fireEvent.click(screen.getByTestId("edition-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.queryByRole("button", { name: "Taken" })).not.toBeInTheDocument();
      fireEvent.click(screen.getByRole("button", { name: /Back/i }));
      expect(await screen.findByTestId("design-edition-12")).toHaveTextContent(
        "NightEd",
      );
      expect(listEditionsBySite).toHaveBeenCalledTimes(loads);
      expect(reorderEditionContentList).not.toHaveBeenCalled();
    },
  );
});
