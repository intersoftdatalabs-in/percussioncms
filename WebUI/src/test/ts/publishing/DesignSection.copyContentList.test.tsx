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
import { CONTENT_LIST_NAME_MAX_LENGTH } from "@/publishing/contentListCopy";

const listContentLists = vi.fn();
const copyContentList = vi.fn();
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
  copyContentList: (...args: unknown[]) => copyContentList(...args),
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

describe("DesignSection copy content list", () => {
  beforeEach(() => {
    listContentLists.mockReset();
    copyContentList.mockReset();
    updateContentList.mockReset();
    listContentLists.mockResolvedValue([original]);
  });

  async function openCopy(): Promise<void> {
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Content lists/i }));
    fireEvent.click(await screen.findByTestId("design-content-list-copy-5"));
    expect(await screen.findByTestId("contentlist-copy")).toBeTruthy();
  }

  it("lists the new content list only after copy succeeds and keeps the source", async () => {
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
          releaseReload = () => resolve([original]);
        }),
    );
    copyContentList.mockResolvedValue({
      contentListId: "9",
      name: "CopiedCl",
      listType: "modern",
    });

    await openCopy();
    expect(screen.getByLabelText(/New name/i)).toHaveValue("NightCl copy");
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "CopiedCl" },
    });
    expect(copyContentList).not.toHaveBeenCalled();
    expect(screen.queryByTestId("design-content-list-9")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("contentlist-copy-submit"));
    await waitFor(() =>
      expect(copyContentList).toHaveBeenCalledWith({
        sourceContentListId: "5",
        newName: "CopiedCl",
      }),
    );
    expect(updateContentList).not.toHaveBeenCalled();
    expect(screen.getByTestId("contentlist-copy")).toBeTruthy();
    expect(screen.queryByTestId("design-content-list-9")).not.toBeInTheDocument();
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.();
    await waitFor(() =>
      expect(screen.getByTestId("design-content-list-9")).toHaveTextContent(
        "CopiedCl",
      ),
    );
    expect(screen.getByTestId("design-content-list-5")).toHaveTextContent("NightCl");
  });

  it("does not copy when cancel is chosen", async () => {
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "Nope" },
    });
    fireEvent.click(screen.getByTestId("contentlist-copy-cancel"));
    expect(copyContentList).not.toHaveBeenCalled();
    expect(await screen.findByTestId("design-content-list-5")).toHaveTextContent(
      "NightCl",
    );
    expect(screen.queryByRole("button", { name: "Nope" })).not.toBeInTheDocument();
  });

  it("rejects a blank name without calling the server", async () => {
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: " " },
    });
    fireEvent.click(screen.getByTestId("contentlist-copy-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Name is required");
    expect(copyContentList).not.toHaveBeenCalled();
  });

  it("rejects an overlong name without calling the server", async () => {
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "N".repeat(CONTENT_LIST_NAME_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("contentlist-copy-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "100 characters or fewer",
    );
    expect(copyContentList).not.toHaveBeenCalled();
  });

  it("keeps the source and does not add a row when the name is a duplicate", async () => {
    copyContentList.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Content list name already exists" },
    });
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "Taken" },
    });
    fireEvent.click(screen.getByTestId("contentlist-copy-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Content list name already exists",
    );
    expect(screen.queryByRole("button", { name: "Taken" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("contentlist-copy-cancel"));
    expect(await screen.findByTestId("design-content-list-5")).toHaveTextContent(
      "NightCl",
    );
    expect(listContentLists).toHaveBeenCalledTimes(1);
    expect(updateContentList).not.toHaveBeenCalled();
  });
});
