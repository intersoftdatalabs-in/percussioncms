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

import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DesignSection } from "@/publishing/sections/DesignSection";

const listEditionsBySite = vi.fn();

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([{ name: "SiteA", siteId: "1" }]),
}));

vi.mock("@/api/publishing/designApi", () => ({
  listEditionsBySite: (...args: unknown[]) => listEditionsBySite(...args),
  createEdition: vi.fn(),
  updateEdition: vi.fn(),
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
  listDesignSites: vi.fn().mockResolvedValue([]),
  listSiteProperties: vi.fn().mockResolvedValue([]),
  deleteSiteProperty: vi.fn(),
  putSiteProperty: vi.fn(),
}));

const LOADED = [
  {
    editionId: "1",
    name: "Nightly Full",
    siteId: "1",
    comment: "ships at midnight",
    priority: 1,
    startDate: "11/10/2026 12:00 AM",
    endDate: "",
  },
  {
    editionId: "2",
    name: "Hourly Incremental",
    siteId: "1",
    comment: "",
    priority: 2,
    startDate: "",
    endDate: "",
  },
];

describe("DesignSection editions search and schedule filter", () => {
  beforeEach(() => {
    listEditionsBySite.mockReset();
    listEditionsBySite.mockResolvedValue(LOADED);
  });

  async function openEditions(): Promise<void> {
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Editions/i }));
    expect(await screen.findByTestId("design-edition-1")).toBeTruthy();
    expect(screen.getByTestId("design-edition-2")).toBeTruthy();
  }

  it("narrows the list by name and restores it when cleared", async () => {
    await openEditions();

    fireEvent.change(screen.getByTestId("design-editions-search"), {
      target: { value: "hourly" },
    });
    expect(screen.queryByTestId("design-edition-1")).toBeNull();
    expect(screen.getByTestId("design-edition-2")).toBeTruthy();

    fireEvent.change(screen.getByTestId("design-editions-search"), {
      target: { value: "" },
    });
    expect(screen.getByTestId("design-edition-1")).toBeTruthy();
    expect(screen.getByTestId("design-edition-2")).toBeTruthy();
  });

  it("matches comment text and shows the empty-filter state", async () => {
    await openEditions();

    fireEvent.change(screen.getByTestId("design-editions-search"), {
      target: { value: "midnight" },
    });
    expect(screen.getByTestId("design-edition-1")).toBeTruthy();
    expect(screen.queryByTestId("design-edition-2")).toBeNull();

    fireEvent.change(screen.getByTestId("design-editions-search"), {
      target: { value: "zzzz-no-match" },
    });
    expect(screen.getByTestId("design-editions-empty-filter")).toBeTruthy();
    expect(screen.queryByTestId("design-edition-1")).toBeNull();
  });

  it("filters by schedule facet without new server calls", async () => {
    await openEditions();
    const calls = listEditionsBySite.mock.calls.length;

    fireEvent.change(screen.getByTestId("design-editions-schedule-filter"), {
      target: { value: "scheduled" },
    });
    expect(screen.getByTestId("design-edition-1")).toBeTruthy();
    expect(screen.queryByTestId("design-edition-2")).toBeNull();

    fireEvent.change(screen.getByTestId("design-editions-schedule-filter"), {
      target: { value: "unscheduled" },
    });
    expect(screen.queryByTestId("design-edition-1")).toBeNull();
    expect(screen.getByTestId("design-edition-2")).toBeTruthy();

    fireEvent.change(screen.getByTestId("design-editions-schedule-filter"), {
      target: { value: "all" },
    });
    expect(screen.getByTestId("design-edition-1")).toBeTruthy();
    expect(screen.getByTestId("design-edition-2")).toBeTruthy();

    expect(listEditionsBySite.mock.calls.length).toBe(calls);
  });
});
