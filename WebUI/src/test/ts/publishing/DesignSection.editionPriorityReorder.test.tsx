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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { DesignSection } from "@/publishing/sections/DesignSection";

const listEditionsBySite = vi.fn();
const updateEdition = vi.fn();

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([{ name: "SiteA", siteId: "1" }]),
}));

vi.mock("@/api/publishing/designApi", () => ({
  listEditionsBySite: (...args: unknown[]) => listEditionsBySite(...args),
  createEdition: vi.fn(),
  updateEdition: (...args: unknown[]) => updateEdition(...args),
  deleteEdition: vi.fn(),
  copyEdition: vi.fn(),
  listEditionContentLists: vi.fn().mockResolvedValue([]),
  listContentLists: vi.fn().mockResolvedValue([]),
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

const low = {
  editionId: "21",
  name: "NightAaa",
  siteId: "1",
  comment: "low-keep",
  priority: 2,
};

const high = {
  editionId: "22",
  name: "NightZzz",
  siteId: "1",
  comment: "high-keep",
  priority: 3,
};

function editionOrder(): string[] {
  return screen
    .getAllByRole("listitem")
    .map((li) => li.textContent ?? "");
}

describe("DesignSection edition priority reorder", () => {
  beforeEach(() => {
    listEditionsBySite.mockReset();
    updateEdition.mockReset();
    listEditionsBySite.mockResolvedValue([low, high]);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  async function openEditions(): Promise<void> {
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Editions/i }));
    await screen.findByTestId("design-edition-21");
    await screen.findByTestId("design-edition-22");
  }

  it("lists highest priority first", async () => {
    await openEditions();
    const order = editionOrder();
    expect(order[0]).toContain("NightZzz");
    expect(order[1]).toContain("NightAaa");
  });

  it("moves an edition up with a priority-only update and re-sorts", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    let rows = [low, high];
    listEditionsBySite.mockImplementation(() => Promise.resolve(rows));
    updateEdition.mockImplementation(async (id: unknown, body: unknown) => {
      rows = rows.map((r) =>
        r.editionId === String(id)
          ? { ...r, ...(body as Record<string, unknown>) }
          : r,
      );
      return rows.find((r) => r.editionId === String(id));
    });

    await openEditions();
    const before = editionOrder();
    expect(before[0]).toContain("NightZzz");
    fireEvent.click(screen.getByTestId("design-edition-move-up-21"));
    await waitFor(() =>
      expect(updateEdition).toHaveBeenCalledWith("21", { priority: 3 }),
    );
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("name");
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("comment");
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("siteId");
    // Reload sees the stored write, so the moved edition lands first.
    await waitFor(() =>
      expect(screen.getByTestId("design-edition-priority-value-21")).toHaveTextContent(
        "3",
      ),
    );
    const after = editionOrder();
    expect(after[0]).toContain("NightAaa");
    expect(after[1]).toContain("NightZzz");
    expect(screen.getByTestId("design-edition-comment-21")).toHaveTextContent(
      "low-keep",
    );
  });

  it("does not call the server when the confirm is dismissed", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    await openEditions();
    fireEvent.click(screen.getByTestId("design-edition-move-up-21"));
    expect(updateEdition).not.toHaveBeenCalled();
    expect(await screen.findByTestId("design-edition-21")).toBeTruthy();
  });

  it.each([400, 403, 409])(
    "keeps the previous order on HTTP %i",
    async (status) => {
      vi.spyOn(window, "confirm").mockReturnValue(true);
      updateEdition.mockRejectedValue({
        status,
        statusText: "Rejected",
        body: { message: "nope" },
      });
      await openEditions();
      const before = editionOrder();
      fireEvent.click(screen.getByTestId("design-edition-move-down-22"));
      await waitFor(() => expect(updateEdition).toHaveBeenCalledTimes(1));
      expect(await screen.findByRole("alert")).toBeTruthy();
      expect(editionOrder()).toEqual(before);
      expect(screen.getByTestId("design-edition-priority-value-22")).toHaveTextContent(
        "3",
      );
    },
  );

  it("disables move up at priority 5 and move down at priority 1", async () => {
    listEditionsBySite.mockResolvedValue([
      { ...low, priority: 5 },
      { ...high, priority: 1 },
    ]);
    await openEditions();
    expect(screen.getByTestId("design-edition-move-up-21")).toBeDisabled();
    expect(screen.getByTestId("design-edition-move-down-22")).toBeDisabled();
    expect(screen.getByTestId("design-edition-move-down-21")).not.toBeDisabled();
    expect(screen.getByTestId("design-edition-move-up-22")).not.toBeDisabled();
  });
});
