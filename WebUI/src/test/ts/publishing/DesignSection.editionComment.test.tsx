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

describe("DesignSection edition comment", () => {
  beforeEach(() => {
    listEditionsBySite.mockReset();
    updateEdition.mockReset();
    reorderEditionContentList.mockReset();
    associateContentList.mockReset();
    disassociateContentList.mockReset();
    listEditionsBySite.mockResolvedValue([original]);
  });

  async function openComment(): Promise<void> {
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Editions/i }));
    expect(await screen.findByTestId("design-edition-comment-12")).toHaveTextContent(
      "keep-me",
    );
    expect(screen.getByTestId("design-edition-priority-value-12")).toHaveTextContent(
      "1",
    );
    fireEvent.click(screen.getByTestId("design-edition-set-comment-12"));
    expect(await screen.findByTestId("edition-comment-form")).toBeTruthy();
    expect(screen.getByTestId("edition-comment-name")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("edition-comment-priority")).toHaveTextContent("1");
    expect(screen.getByTestId("edition-comment-input")).toHaveValue("keep-me");
  }

  it("shows the new comment only after reload and leaves name and priority", async () => {
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
    updateEdition.mockResolvedValue({ ...original, comment: "night note" });

    await openComment();
    fireEvent.change(screen.getByLabelText("Comment"), {
      target: { value: "  night note  " },
    });
    expect(updateEdition).not.toHaveBeenCalled();
    held = true;
    fireEvent.click(screen.getByTestId("edition-comment-save"));
    expect(await screen.findByTestId("design-edition-comment-12")).toHaveTextContent(
      "keep-me",
    );
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-priority-value-12")).toHaveTextContent(
      "1",
    );
    await waitFor(() =>
      expect(updateEdition).toHaveBeenCalledWith("12", { comment: "night note" }),
    );
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("name");
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("priority");
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("siteId");
    expect(reorderEditionContentList).not.toHaveBeenCalled();
    expect(associateContentList).not.toHaveBeenCalled();
    expect(disassociateContentList).not.toHaveBeenCalled();
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.([{ ...original, comment: "night note" }]);
    await waitFor(() =>
      expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent(
        "night note",
      ),
    );
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-priority-value-12")).toHaveTextContent(
      "1",
    );
  });

  it("clears the stored comment when the field is blank", async () => {
    updateEdition.mockResolvedValue({ ...original, comment: "" });
    listEditionsBySite
      .mockResolvedValueOnce([original])
      .mockResolvedValueOnce([{ ...original, comment: "" }]);
    await openComment();
    fireEvent.change(screen.getByLabelText("Comment"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("edition-comment-save"));
    await waitFor(() =>
      expect(updateEdition).toHaveBeenCalledWith("12", { comment: "" }),
    );
    await waitFor(() =>
      expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent(
        "",
      ),
    );
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-priority-value-12")).toHaveTextContent(
      "1",
    );
    expect(reorderEditionContentList).not.toHaveBeenCalled();
  });

  it("keeps the new comment when the reload fails", async () => {
    let failReload = false;
    listEditionsBySite.mockImplementation(() => {
      if (failReload) {
        return Promise.reject(new Error("reload failed"));
      }
      return Promise.resolve([original]);
    });
    updateEdition.mockResolvedValue({ ...original, comment: "kept" });
    await openComment();
    fireEvent.change(screen.getByLabelText("Comment"), {
      target: { value: "kept" },
    });
    failReload = true;
    fireEvent.click(screen.getByTestId("edition-comment-save"));
    await waitFor(() =>
      expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent(
        "kept",
      ),
    );
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-priority-value-12")).toHaveTextContent(
      "1",
    );
    expect(screen.queryByRole("button", { name: "OtherEd" })).not.toBeInTheDocument();
  });

  it("cancel does not call save", async () => {
    await openComment();
    fireEvent.change(screen.getByLabelText("Comment"), {
      target: { value: "not saved" },
    });
    fireEvent.click(screen.getByTestId("edition-comment-cancel"));
    expect(await screen.findByTestId("design-edition-comment-12")).toHaveTextContent(
      "keep-me",
    );
    expect(screen.getByTestId("design-edition-priority-value-12")).toHaveTextContent(
      "1",
    );
    expect(updateEdition).not.toHaveBeenCalled();
    expect(reorderEditionContentList).not.toHaveBeenCalled();
  });

  it.each([
    [400, "Invalid edition update"],
    [403, "Admin or Designer role required to save a publish edition"],
    [409, "Edition name already exists"],
  ])("HTTP %s leaves the previous comment", async (status, message) => {
    updateEdition.mockRejectedValue({ status, statusText: "error", body: { message } });
    await openComment();
    fireEvent.change(screen.getByLabelText("Comment"), {
      target: { value: "rejected" },
    });
    fireEvent.click(screen.getByTestId("edition-comment-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.getByTestId("edition-comment-form")).toBeTruthy();
    expect(screen.queryByTestId("design-edition-comment-12")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("edition-comment-cancel"));
    expect(await screen.findByTestId("design-edition-comment-12")).toHaveTextContent(
      "keep-me",
    );
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-priority-value-12")).toHaveTextContent(
      "1",
    );
    expect(reorderEditionContentList).not.toHaveBeenCalled();
  });
});
