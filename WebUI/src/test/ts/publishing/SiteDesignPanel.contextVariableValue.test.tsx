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
import type { SitePropertyDto } from "@/api/publishing/designApi";
import { SiteDesignPanel } from "@/publishing/design/SiteDesignPanel";

const listDesignSites = vi.fn();
const listContexts = vi.fn();
const listSiteProperties = vi.fn();
const putSiteProperty = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  listDesignSites: (...args: unknown[]) => listDesignSites(...args),
  listContexts: (...args: unknown[]) => listContexts(...args),
  listSiteProperties: (...args: unknown[]) => listSiteProperties(...args),
  putSiteProperty: (...args: unknown[]) => putSiteProperty(...args),
  deleteSiteProperty: vi.fn(),
}));

const kept: SitePropertyDto = { name: "kept", contextId: "3", value: "old" };
const target: SitePropertyDto = { name: "nightVar", contextId: "3", value: "before" };
const updated: SitePropertyDto = { name: "nightVar", contextId: "3", value: "next-value" };

describe("SiteDesignPanel change one context variable value", () => {
  beforeEach(() => {
    listDesignSites.mockReset();
    listContexts.mockReset();
    listSiteProperties.mockReset();
    putSiteProperty.mockReset();
    listDesignSites.mockResolvedValue([
      { siteId: "42", name: "Enterprise", folderRoot: "//Sites/Enterprise" },
    ]);
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    listSiteProperties.mockResolvedValue([kept, target]);
  });

  async function openChange(): Promise<void> {
    render(<SiteDesignPanel />);
    expect(await screen.findByText("nightVar")).toBeTruthy();
    const rows = screen.getAllByTestId("context-variable-row");
    const targetRow = rows.find((row) => row.textContent?.includes("nightVar"));
    expect(targetRow).toBeTruthy();
    fireEvent.click(targetRow!.querySelector("[data-testid='context-variable-change-value']")!);
    expect(screen.getByTestId("context-variable-value-name")).toHaveTextContent("nightVar");
    expect(screen.getByTestId("context-variable-value-current")).toHaveTextContent("before");
    expect(screen.getByTestId("context-variable-value-other")).toHaveTextContent("kept: old");
  }

  function fillValue(value: string): void {
    fireEvent.change(screen.getByTestId("context-variable-value-input"), {
      target: { value },
    });
  }

  it("shows the new value only after reload and leaves the other variable", async () => {
    let releaseReload: ((rows: SitePropertyDto[]) => void) | undefined;
    let held = false;
    listSiteProperties.mockImplementation(
      () =>
        new Promise((resolve) => {
          if (!held) {
            resolve([kept, target]);
            return;
          }
          releaseReload = resolve;
        }),
    );
    putSiteProperty.mockResolvedValue(updated);

    await openChange();
    fillValue("  next-value  ");
    expect(putSiteProperty).not.toHaveBeenCalled();
    held = true;
    fireEvent.click(screen.getByTestId("context-variable-value-save"));
    const valuesWhileSaving = screen
      .getAllByTestId("context-variable-row-value")
      .map((node) => node.textContent);
    expect(valuesWhileSaving).toEqual(["old", "before"]);
    await waitFor(() =>
      expect(putSiteProperty).toHaveBeenCalledWith("42", {
        name: "nightVar",
        contextId: "3",
        value: "next-value",
        updateValue: true,
      }),
    );
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.([kept, updated]);
    await waitFor(() =>
      expect(
        screen.getAllByTestId("context-variable-row-value").map((node) => node.textContent),
      ).toEqual(["old", "next-value"]),
    );
    const names = screen
      .getAllByTestId("context-variable-row-name")
      .map((node) => node.textContent);
    expect(names).toEqual(["kept", "nightVar"]);
    expect(screen.queryByTestId("context-variable-value-form")).not.toBeInTheDocument();
  });

  it("keeps both variables when the reload fails", async () => {
    let failReload = false;
    listSiteProperties.mockImplementation(() => {
      if (failReload) {
        return Promise.reject(new Error("reload failed"));
      }
      return Promise.resolve([kept, target]);
    });
    putSiteProperty.mockResolvedValue(updated);
    await openChange();
    fillValue("next-value");
    failReload = true;
    fireEvent.click(screen.getByTestId("context-variable-value-save"));
    await waitFor(() =>
      expect(
        screen.getAllByTestId("context-variable-row-value").map((node) => node.textContent),
      ).toEqual(["old", "next-value"]),
    );
    expect(screen.getAllByTestId("context-variable-row-name")[1]).toHaveTextContent("nightVar");
  });

  it("does not write a blank value, an overlong value, or a cancel", async () => {
    await openChange();
    fillValue("   ");
    fireEvent.click(screen.getByTestId("context-variable-value-save"));
    expect(await screen.findByTestId("context-variable-value-error")).toHaveTextContent(
      "Context variable value is required",
    );
    expect(putSiteProperty).not.toHaveBeenCalled();
    expect(screen.getAllByTestId("context-variable-row-value")[1]).toHaveTextContent("before");

    fillValue("v".repeat(256));
    fireEvent.click(screen.getByTestId("context-variable-value-save"));
    expect(screen.getByTestId("context-variable-value-error")).toHaveTextContent(
      "255 characters or fewer",
    );
    expect(putSiteProperty).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("context-variable-value-cancel"));
    expect(screen.queryByTestId("context-variable-value-form")).not.toBeInTheDocument();
    expect(putSiteProperty).not.toHaveBeenCalled();
    expect(screen.getAllByTestId("context-variable-row-value")[0]).toHaveTextContent("old");
    expect(screen.getAllByTestId("context-variable-row-value")[1]).toHaveTextContent("before");
  });

  it("does not claim success for HTTP 400, 403, or 409", async () => {
    await openChange();
    const cases = [
      [400, "Context variable value is required"],
      [403, "Admin or Designer role required"],
      [409, "Context variable is not listed"],
    ] as const;
    for (const [status, message] of cases) {
      putSiteProperty.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message },
      });
      fillValue("next-value");
      fireEvent.click(screen.getByTestId("context-variable-value-save"));
      await waitFor(() =>
        expect(screen.getByTestId("context-variable-value-error")).toHaveTextContent(message),
      );
      expect(
        screen.getAllByTestId("context-variable-row-value").map((node) => node.textContent),
      ).toEqual(["old", "before"]);
      expect(screen.getByTestId("context-variable-value-name")).toHaveTextContent("nightVar");
    }
    expect(putSiteProperty).toHaveBeenCalledTimes(3);
  });
});
