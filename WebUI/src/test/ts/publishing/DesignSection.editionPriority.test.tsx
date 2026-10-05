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
  listEditionContentLists: vi.fn().mockResolvedValue([]),
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
  priority: 1,
};

describe("DesignSection edition priority", () => {
  beforeEach(() => {
    listEditionsBySite.mockReset();
    updateEdition.mockReset();
    reorderEditionContentList.mockReset();
    associateContentList.mockReset();
    disassociateContentList.mockReset();
    listEditionsBySite.mockResolvedValue([original]);
  });

  async function openPriority(): Promise<void> {
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Editions/i }));
    expect(await screen.findByTestId("design-edition-priority-value-12")).toHaveTextContent(
      "1",
    );
    expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent("keep-me");
    fireEvent.click(screen.getByTestId("design-edition-set-priority-12"));
    expect(await screen.findByTestId("edition-priority-form")).toBeTruthy();
    expect(screen.getByTestId("edition-priority-name")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("edition-priority-comment")).toHaveTextContent("keep-me");
  }

  it("shows the new priority only after reload and leaves name and comment", async () => {
    let releaseReload: ((rows: (typeof original)[]) => void) | undefined;
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
    updateEdition.mockResolvedValue({ ...original, priority: 5 });

    await openPriority();
    fireEvent.change(screen.getByLabelText(/Priority/i), {
      target: { value: " 5 " },
    });
    expect(updateEdition).not.toHaveBeenCalled();
    held = true;
    fireEvent.click(screen.getByTestId("edition-priority-save"));
    expect(await screen.findByTestId("design-edition-priority-value-12")).toHaveTextContent(
      "1",
    );
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent("keep-me");
    await waitFor(() =>
      expect(updateEdition).toHaveBeenCalledWith("12", { priority: 5 }),
    );
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("name");
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("comment");
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("siteId");
    expect(reorderEditionContentList).not.toHaveBeenCalled();
    expect(associateContentList).not.toHaveBeenCalled();
    expect(disassociateContentList).not.toHaveBeenCalled();
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.([{ ...original, priority: 5 }]);
    await waitFor(() =>
      expect(screen.getByTestId("design-edition-priority-value-12")).toHaveTextContent(
        "5",
      ),
    );
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent("keep-me");
  });

  it("keeps the new priority when the reload fails", async () => {
    let failReload = false;
    listEditionsBySite.mockImplementation(() => {
      if (failReload) {
        return Promise.reject(new Error("reload failed"));
      }
      return Promise.resolve([original]);
    });
    updateEdition.mockResolvedValue({ ...original, priority: 4 });
    await openPriority();
    fireEvent.change(screen.getByLabelText(/Priority/i), { target: { value: "4" } });
    failReload = true;
    fireEvent.click(screen.getByTestId("edition-priority-save"));
    await waitFor(() =>
      expect(screen.getByTestId("design-edition-priority-value-12")).toHaveTextContent(
        "4",
      ),
    );
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent("keep-me");
    expect(screen.queryByRole("button", { name: "OtherEd" })).not.toBeInTheDocument();
  });

  it("cancel and values outside 1–5 do not call save", async () => {
    await openPriority();
    fireEvent.change(screen.getByLabelText(/Priority/i), { target: { value: "5" } });
    fireEvent.click(screen.getByTestId("edition-priority-cancel"));
    expect(await screen.findByTestId("design-edition-priority-value-12")).toHaveTextContent(
      "1",
    );
    expect(updateEdition).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("design-edition-set-priority-12"));
    expect(await screen.findByTestId("edition-priority-form")).toBeTruthy();
    for (const value of ["0", "6", "", "1.5"]) {
      fireEvent.change(screen.getByLabelText(/Priority/i), { target: { value } });
      fireEvent.click(screen.getByTestId("edition-priority-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(
        "Edition priority must be from 1 to 5",
      );
    }
    expect(updateEdition).not.toHaveBeenCalled();
    expect(reorderEditionContentList).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("edition-priority-cancel"));
    expect(await screen.findByTestId("design-edition-priority-value-12")).toHaveTextContent(
      "1",
    );
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent("keep-me");
  });

  it.each([
    [400, "Edition priority must be from 1 to 5"],
    [403, "Admin or Designer role required to save a publish edition"],
    [409, "Edition name already exists"],
  ])("HTTP %s leaves the previous priority", async (status, message) => {
    updateEdition.mockRejectedValue({ status, statusText: "error", body: { message } });
    await openPriority();
    fireEvent.change(screen.getByLabelText(/Priority/i), { target: { value: "5" } });
    fireEvent.click(screen.getByTestId("edition-priority-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.getByTestId("edition-priority-form")).toBeTruthy();
    expect(screen.queryByTestId("design-edition-priority-value-12")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("edition-priority-cancel"));
    expect(await screen.findByTestId("design-edition-priority-value-12")).toHaveTextContent(
      "1",
    );
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent("keep-me");
    expect(reorderEditionContentList).not.toHaveBeenCalled();
  });
});
