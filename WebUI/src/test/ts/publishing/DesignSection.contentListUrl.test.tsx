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
import { CONTENT_LIST_URL_MAX_LENGTH } from "@/publishing/contentListUrl";

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

describe("DesignSection content list legacy URL", () => {
  beforeEach(() => {
    listContentLists.mockReset();
    updateContentList.mockReset();
    listContentLists.mockResolvedValue([modern, legacy]);
  });

  async function openUrl(): Promise<void> {
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Content lists/i }));
    expect(await screen.findByTestId("design-content-list-source-8")).toHaveTextContent(
      "/Rhythmyx/legacyList",
    );
    expect(screen.getByTestId("design-content-list-type-8")).toHaveTextContent(
      "legacy",
    );
    expect(screen.getByTestId("design-content-list-description-8")).toHaveTextContent(
      "legacy notes",
    );
    expect(screen.getByTestId("design-content-list-filter-8")).toHaveTextContent(
      "No item filter",
    );
    expect(screen.queryByTestId("design-content-list-url-5")).not.toBeInTheDocument();
    expect(screen.getByTestId("design-content-list-generator-5")).toBeTruthy();
    fireEvent.click(screen.getByTestId("design-content-list-url-8"));
    expect(await screen.findByTestId("contentlist-url-form")).toBeTruthy();
    expect(screen.getByTestId("contentlist-url-name")).toHaveTextContent("LegacyCl");
    expect(screen.getByTestId("contentlist-url-description")).toHaveTextContent(
      "legacy notes",
    );
    expect(screen.getByTestId("contentlist-url-type")).toHaveTextContent("legacy");
    expect(screen.getByTestId("contentlist-url-filter")).toHaveTextContent(
      "No item filter",
    );
    expect(screen.getByTestId("contentlist-url-input")).toHaveValue(
      "/Rhythmyx/legacyList",
    );
    expect(screen.queryByTestId("design-content-list-source-8")).not.toBeInTheDocument();
  }

  it("shows the new URL only after reload and leaves name, description, type, and filter", async () => {
    let releaseReload: ((rows: (typeof legacy)[]) => void) | undefined;
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
    updateContentList.mockResolvedValue({
      ...legacy,
      url: "/Rhythmyx/night-next",
    });

    await openUrl();
    fireEvent.change(screen.getByLabelText("Legacy URL"), {
      target: { value: "  /Rhythmyx/night-next  " },
    });
    expect(updateContentList).not.toHaveBeenCalled();
    held = true;
    fireEvent.click(screen.getByTestId("contentlist-url-save"));
    expect(await screen.findByTestId("design-content-list-source-8")).toHaveTextContent(
      "/Rhythmyx/legacyList",
    );
    expect(screen.getByTestId("design-content-list-8")).toHaveTextContent("LegacyCl");
    expect(screen.getByTestId("design-content-list-description-8")).toHaveTextContent(
      "legacy notes",
    );
    expect(screen.getByTestId("design-content-list-type-8")).toHaveTextContent("legacy");
    expect(screen.getByTestId("design-content-list-filter-8")).toHaveTextContent(
      "No item filter",
    );
    expect(screen.getByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
    await waitFor(() =>
      expect(updateContentList).toHaveBeenCalledWith("8", {
        url: "/Rhythmyx/night-next",
      }),
    );
    const body = updateContentList.mock.calls[0][1] as Record<string, unknown>;
    expect(body).not.toHaveProperty("name");
    expect(body).not.toHaveProperty("description");
    expect(body).not.toHaveProperty("listType");
    expect(body).not.toHaveProperty("generator");
    expect(body).not.toHaveProperty("itemFilterId");
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.([{ ...legacy, url: "/Rhythmyx/night-next" }, modern]);
    await waitFor(() =>
      expect(screen.getByTestId("design-content-list-source-8")).toHaveTextContent(
        "/Rhythmyx/night-next",
      ),
    );
    expect(screen.getByTestId("design-content-list-8")).toHaveTextContent("LegacyCl");
    expect(screen.getByTestId("design-content-list-description-8")).toHaveTextContent(
      "legacy notes",
    );
    expect(screen.getByTestId("design-content-list-type-8")).toHaveTextContent("legacy");
    expect(screen.getByTestId("design-content-list-filter-8")).toHaveTextContent(
      "No item filter",
    );
    expect(screen.getByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
    expect(screen.queryByTestId("design-content-list-url-5")).not.toBeInTheDocument();
  });

  it("keeps the new URL when the reload fails", async () => {
    let failReload = false;
    listContentLists.mockImplementation(() => {
      if (failReload) {
        return Promise.reject(new Error("reload failed"));
      }
      return Promise.resolve([modern, legacy]);
    });
    updateContentList.mockResolvedValue({
      ...legacy,
      url: "/Rhythmyx/kept",
    });
    await openUrl();
    fireEvent.change(screen.getByLabelText("Legacy URL"), {
      target: { value: "/Rhythmyx/kept" },
    });
    failReload = true;
    fireEvent.click(screen.getByTestId("contentlist-url-save"));
    await waitFor(() =>
      expect(screen.getByTestId("design-content-list-source-8")).toHaveTextContent(
        "/Rhythmyx/kept",
      ),
    );
    expect(screen.getByTestId("design-content-list-8")).toHaveTextContent("LegacyCl");
    expect(screen.getByTestId("design-content-list-description-8")).toHaveTextContent(
      "legacy notes",
    );
    expect(screen.getByTestId("design-content-list-type-8")).toHaveTextContent("legacy");
    expect(screen.getByTestId("design-content-list-filter-8")).toHaveTextContent(
      "No item filter",
    );
    expect(screen.getByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
  });

  it("cancel does not call save", async () => {
    await openUrl();
    fireEvent.change(screen.getByLabelText("Legacy URL"), {
      target: { value: "/Rhythmyx/not-saved" },
    });
    fireEvent.click(screen.getByTestId("contentlist-url-cancel"));
    expect(await screen.findByTestId("design-content-list-source-8")).toHaveTextContent(
      "/Rhythmyx/legacyList",
    );
    expect(screen.getByTestId("design-content-list-description-8")).toHaveTextContent(
      "legacy notes",
    );
    expect(updateContentList).not.toHaveBeenCalled();
  });

  it("rejects a blank URL without calling the server", async () => {
    await openUrl();
    fireEvent.change(screen.getByLabelText("Legacy URL"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("contentlist-url-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Content list URL is required",
    );
    expect(updateContentList).not.toHaveBeenCalled();
    expect(screen.getByTestId("contentlist-url-form")).toBeTruthy();
    fireEvent.click(screen.getByTestId("contentlist-url-cancel"));
    expect(await screen.findByTestId("design-content-list-source-8")).toHaveTextContent(
      "/Rhythmyx/legacyList",
    );
  });

  it("rejects an overlong URL without calling the server", async () => {
    await openUrl();
    fireEvent.change(screen.getByLabelText("Legacy URL"), {
      target: { value: "u".repeat(CONTENT_LIST_URL_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("contentlist-url-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Content list URL must be 2100 characters or fewer",
    );
    expect(updateContentList).not.toHaveBeenCalled();
    expect(screen.getByTestId("contentlist-url-form")).toBeTruthy();
  });

  it.each([
    [400, "Content list URL must be 2100 characters or fewer"],
    [403, "Admin or Designer role required to save a publish edition"],
    [409, "Content list name already exists"],
  ])("HTTP %s leaves the previous URL", async (status, message) => {
    updateContentList.mockRejectedValue({
      status,
      statusText: "error",
      body: { message },
    });
    await openUrl();
    fireEvent.change(screen.getByLabelText("Legacy URL"), {
      target: { value: `/Rhythmyx/rejected-${status}` },
    });
    fireEvent.click(screen.getByTestId("contentlist-url-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.getByTestId("contentlist-url-form")).toBeTruthy();
    expect(screen.getByTestId("contentlist-url-name")).toHaveTextContent("LegacyCl");
    expect(screen.getByTestId("contentlist-url-description")).toHaveTextContent(
      "legacy notes",
    );
    expect(screen.getByTestId("contentlist-url-filter")).toHaveTextContent(
      "No item filter",
    );
    expect(screen.queryByTestId("design-content-list-source-8")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("contentlist-url-cancel"));
    expect(await screen.findByTestId("design-content-list-source-8")).toHaveTextContent(
      "/Rhythmyx/legacyList",
    );
    expect(screen.getByTestId("design-content-list-8")).toHaveTextContent("LegacyCl");
    expect(screen.getByTestId("design-content-list-description-8")).toHaveTextContent(
      "legacy notes",
    );
    expect(screen.getByTestId("design-content-list-type-8")).toHaveTextContent("legacy");
    expect(screen.getByTestId("design-content-list-filter-8")).toHaveTextContent(
      "No item filter",
    );
    expect(screen.getByTestId("design-content-list-source-5")).toHaveTextContent(
      "sys_Search",
    );
  });
});
