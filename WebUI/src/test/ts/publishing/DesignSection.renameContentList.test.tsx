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
};

describe("DesignSection rename content list", () => {
  beforeEach(() => {
    listContentLists.mockReset();
    updateContentList.mockReset();
    listContentLists.mockResolvedValue([original]);
  });

  async function openList(): Promise<void> {
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Content lists/i }));
    fireEvent.click(await screen.findByTestId("design-content-list-5"));
  }

  it("keeps the previous name on the list until rename save succeeds", async () => {
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
                contentListId: "5",
                name: "RenamedCl",
                listType: "modern",
              },
            ]);
        }),
    );
    updateContentList.mockResolvedValue({
      contentListId: "5",
      name: "RenamedCl",
      listType: "modern",
    });

    await openList();
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "RenamedCl" },
    });
    expect(screen.queryByTestId("design-content-list-5")).not.toBeInTheDocument();
    expect(updateContentList).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("contentlist-save"));
    expect(await screen.findByTestId("design-content-list-5")).toHaveTextContent(
      "NightCl",
    );
    expect(screen.queryByRole("button", { name: "RenamedCl" })).not.toBeInTheDocument();
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.();
    await waitFor(() =>
      expect(screen.getByTestId("design-content-list-5")).toHaveTextContent(
        "RenamedCl",
      ),
    );
  });

  it("does not PUT when rename is cancelled", async () => {
    await openList();
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "Nope" },
    });
    fireEvent.click(screen.getByRole("button", { name: /Back/i }));
    expect(updateContentList).not.toHaveBeenCalled();
    expect(await screen.findByTestId("design-content-list-5")).toHaveTextContent(
      "NightCl",
    );
  });

  it("keeps the previous name when the rename is rejected", async () => {
    updateContentList.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Content list name already exists" },
    });
    await openList();
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "Taken" },
    });
    fireEvent.click(screen.getByTestId("contentlist-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Content list name already exists",
    );
    expect(screen.queryByRole("button", { name: "Taken" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /Back/i }));
    expect(await screen.findByTestId("design-content-list-5")).toHaveTextContent(
      "NightCl",
    );
    expect(listContentLists).toHaveBeenCalledTimes(1);
  });

  it("rejects a blank name without calling the server", async () => {
    await openList();
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: " " },
    });
    fireEvent.click(screen.getByTestId("contentlist-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Name is required");
    expect(updateContentList).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: /Back/i }));
    expect(await screen.findByTestId("design-content-list-5")).toHaveTextContent(
      "NightCl",
    );
  });
});
