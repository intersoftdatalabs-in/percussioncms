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
  startDate: "11/09/2026 08:00 am",
  endDate: "11/10/2026 05:00 pm",
};

describe("DesignSection edition schedule", () => {
  beforeEach(() => {
    listEditionsBySite.mockReset();
    updateEdition.mockReset();
    reorderEditionContentList.mockReset();
    associateContentList.mockReset();
    disassociateContentList.mockReset();
    listEditionsBySite.mockResolvedValue([original]);
  });

  async function openSchedule(): Promise<void> {
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Editions/i }));
    expect(
      await screen.findByTestId("design-edition-schedule-value-12"),
    ).toHaveTextContent("11/09/2026 08:00 am → 11/10/2026 05:00 pm");
    expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent(
      "keep-me",
    );
    expect(
      screen.getByTestId("design-edition-priority-value-12"),
    ).toHaveTextContent("1");
    fireEvent.click(screen.getByTestId("design-edition-set-schedule-12"));
    expect(await screen.findByTestId("edition-schedule-form")).toBeTruthy();
    expect(screen.getByTestId("edition-schedule-name")).toHaveTextContent(
      "NightEd",
    );
    expect(screen.getByTestId("edition-schedule-priority")).toHaveTextContent(
      "1",
    );
    expect(screen.getByTestId("edition-schedule-comment")).toHaveTextContent(
      "keep-me",
    );
    expect(screen.getByTestId("edition-schedule-start-input")).toHaveValue(
      "2026-11-09T08:00",
    );
    expect(screen.getByTestId("edition-schedule-end-input")).toHaveValue(
      "2026-11-10T17:00",
    );
  }

  it("shows the new schedule only after reload and leaves name, priority, comment", async () => {
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
    updateEdition.mockResolvedValue({
      ...original,
      startDate: "11/11/2026 08:00 am",
      endDate: "11/12/2026 05:00 pm",
    });

    await openSchedule();
    fireEvent.change(screen.getByLabelText("Embargo (publish from)"), {
      target: { value: "2026-11-11T08:00" },
    });
    fireEvent.change(screen.getByLabelText("Expiry (until)"), {
      target: { value: "2026-11-12T17:00" },
    });
    expect(updateEdition).not.toHaveBeenCalled();
    held = true;
    fireEvent.click(screen.getByTestId("edition-schedule-save"));
    expect(
      await screen.findByTestId("design-edition-schedule-value-12"),
    ).toHaveTextContent("11/09/2026 08:00 am → 11/10/2026 05:00 pm");
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent(
      "keep-me",
    );
    expect(
      screen.getByTestId("design-edition-priority-value-12"),
    ).toHaveTextContent("1");
    await waitFor(() =>
      expect(updateEdition).toHaveBeenCalledWith("12", {
        startDate: "11/11/2026 08:00 am",
        endDate: "11/12/2026 05:00 pm",
      }),
    );
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("name");
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("priority");
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("comment");
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("siteId");
    expect(reorderEditionContentList).not.toHaveBeenCalled();
    expect(associateContentList).not.toHaveBeenCalled();
    expect(disassociateContentList).not.toHaveBeenCalled();
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.([
      {
        ...original,
        startDate: "11/11/2026 08:00 am",
        endDate: "11/12/2026 05:00 pm",
      },
    ]);
    await waitFor(() =>
      expect(
        screen.getByTestId("design-edition-schedule-value-12"),
      ).toHaveTextContent("11/11/2026 08:00 am → 11/12/2026 05:00 pm"),
    );
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent(
      "keep-me",
    );
    expect(
      screen.getByTestId("design-edition-priority-value-12"),
    ).toHaveTextContent("1");
  });

  it("clears the stored schedule when both fields are cleared", async () => {
    updateEdition.mockResolvedValue({ ...original, startDate: "", endDate: "" });
    listEditionsBySite
      .mockResolvedValueOnce([original])
      .mockResolvedValueOnce([{ ...original, startDate: "", endDate: "" }]);
    await openSchedule();
    fireEvent.click(screen.getByTestId("edition-schedule-clear"));
    expect(screen.getByTestId("edition-schedule-start-input")).toHaveValue("");
    expect(screen.getByTestId("edition-schedule-end-input")).toHaveValue("");
    fireEvent.click(screen.getByTestId("edition-schedule-save"));
    await waitFor(() =>
      expect(updateEdition).toHaveBeenCalledWith("12", {
        startDate: "",
        endDate: "",
      }),
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("design-edition-schedule-value-12"),
      ).toHaveTextContent(""),
    );
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent(
      "keep-me",
    );
    expect(
      screen.getByTestId("design-edition-priority-value-12"),
    ).toHaveTextContent("1");
    expect(reorderEditionContentList).not.toHaveBeenCalled();
  });

  it("keeps the new schedule when the reload fails", async () => {
    let failReload = false;
    listEditionsBySite.mockImplementation(() => {
      if (failReload) {
        return Promise.reject(new Error("reload failed"));
      }
      return Promise.resolve([original]);
    });
    updateEdition.mockResolvedValue({
      ...original,
      startDate: "11/11/2026 08:00 am",
      endDate: "",
    });
    await openSchedule();
    fireEvent.change(screen.getByLabelText("Embargo (publish from)"), {
      target: { value: "2026-11-11T08:00" },
    });
    fireEvent.change(screen.getByLabelText("Expiry (until)"), {
      target: { value: "" },
    });
    failReload = true;
    fireEvent.click(screen.getByTestId("edition-schedule-save"));
    await waitFor(() =>
      expect(
        screen.getByTestId("design-edition-schedule-value-12"),
      ).toHaveTextContent("from 11/11/2026 08:00 am"),
    );
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.queryByRole("button", { name: "OtherEd" })).not.toBeInTheDocument();
  });

  it("rejects an expiry before the embargo without calling save", async () => {
    await openSchedule();
    fireEvent.change(screen.getByLabelText("Embargo (publish from)"), {
      target: { value: "2026-11-12T17:00" },
    });
    fireEvent.change(screen.getByLabelText("Expiry (until)"), {
      target: { value: "2026-11-09T08:00" },
    });
    fireEvent.click(screen.getByTestId("edition-schedule-save"));
    expect(await screen.findByRole("alert")).toBeTruthy();
    expect(screen.getByTestId("edition-schedule-form")).toBeTruthy();
    expect(updateEdition).not.toHaveBeenCalled();
  });

  it("cancel does not call save", async () => {
    await openSchedule();
    fireEvent.change(screen.getByLabelText("Embargo (publish from)"), {
      target: { value: "2026-11-11T08:00" },
    });
    fireEvent.click(screen.getByTestId("edition-schedule-cancel"));
    expect(
      await screen.findByTestId("design-edition-schedule-value-12"),
    ).toHaveTextContent("11/09/2026 08:00 am → 11/10/2026 05:00 pm");
    expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent(
      "keep-me",
    );
    expect(
      screen.getByTestId("design-edition-priority-value-12"),
    ).toHaveTextContent("1");
    expect(updateEdition).not.toHaveBeenCalled();
    expect(reorderEditionContentList).not.toHaveBeenCalled();
  });

  it.each([
    [400, "Edition schedule dates must be MM/dd/yyyy hh:mm am or pm"],
    [403, "Admin or Designer role required to save a publish edition"],
    [409, "Edition name already exists"],
  ])("HTTP %s leaves the previous schedule", async (status, message) => {
    updateEdition.mockRejectedValue({ status, statusText: "error", body: { message } });
    await openSchedule();
    fireEvent.change(screen.getByLabelText("Embargo (publish from)"), {
      target: { value: "2026-11-11T08:00" },
    });
    fireEvent.change(screen.getByLabelText("Expiry (until)"), {
      target: { value: "2026-11-12T17:00" },
    });
    fireEvent.click(screen.getByTestId("edition-schedule-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.getByTestId("edition-schedule-form")).toBeTruthy();
    expect(screen.queryByTestId("design-edition-schedule-value-12")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("edition-schedule-cancel"));
    expect(
      await screen.findByTestId("design-edition-schedule-value-12"),
    ).toHaveTextContent("11/09/2026 08:00 am → 11/10/2026 05:00 pm");
    expect(screen.getByTestId("design-edition-12")).toHaveTextContent("NightEd");
    expect(screen.getByTestId("design-edition-comment-12")).toHaveTextContent(
      "keep-me",
    );
    expect(
      screen.getByTestId("design-edition-priority-value-12"),
    ).toHaveTextContent("1");
    expect(reorderEditionContentList).not.toHaveBeenCalled();
  });
});
