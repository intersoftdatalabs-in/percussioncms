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
const deleteSiteProperty = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  listDesignSites: (...args: unknown[]) => listDesignSites(...args),
  listContexts: (...args: unknown[]) => listContexts(...args),
  listSiteProperties: (...args: unknown[]) => listSiteProperties(...args),
  putSiteProperty: vi.fn(),
  deleteSiteProperty: (...args: unknown[]) => deleteSiteProperty(...args),
}));

const kept: SitePropertyDto = { name: "kept", contextId: "3", value: "old" };
const target: SitePropertyDto = { name: "nightVar", contextId: "3", value: "before" };

describe("SiteDesignPanel delete one context variable", () => {
  beforeEach(() => {
    listDesignSites.mockReset();
    listContexts.mockReset();
    listSiteProperties.mockReset();
    deleteSiteProperty.mockReset();
    listDesignSites.mockResolvedValue([
      { siteId: "42", name: "Enterprise", folderRoot: "//Sites/Enterprise" },
    ]);
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    listSiteProperties.mockResolvedValue([kept, target]);
    deleteSiteProperty.mockResolvedValue(undefined);
  });

  async function openDelete(): Promise<void> {
    render(<SiteDesignPanel />);
    expect(await screen.findByText("nightVar")).toBeTruthy();
    const rows = screen.getAllByTestId("context-variable-row");
    const targetRow = rows.find((row) => row.textContent?.includes("nightVar"));
    expect(targetRow).toBeTruthy();
    fireEvent.click(targetRow!.querySelector("[data-testid='context-variable-delete']")!);
    expect(screen.getByTestId("context-variable-delete-name")).toHaveTextContent("nightVar");
    expect(screen.getByTestId("context-variable-delete-other")).toHaveTextContent("kept: old");
    expect(deleteSiteProperty).not.toHaveBeenCalled();
  }

  function names(): string[] {
    return screen.getAllByTestId("context-variable-row-name").map((node) => node.textContent ?? "");
  }

  it("removes only that name after the server accepts and leaves the other", async () => {
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

    await openDelete();
    held = true;
    fireEvent.click(screen.getByTestId("context-variable-delete-confirm"));
    expect(names()).toEqual(["kept", "nightVar"]);
    await waitFor(() => expect(deleteSiteProperty).toHaveBeenCalledWith("42", "nightVar", "3"));
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.([kept]);
    await waitFor(() => expect(names()).toEqual(["kept"]));
    expect(screen.getByTestId("context-variable-row-value")).toHaveTextContent("old");
    expect(screen.queryByTestId("context-variable-delete-form")).not.toBeInTheDocument();
  });

  it("keeps the other variable when the reload fails", async () => {
    let failReload = false;
    listSiteProperties.mockImplementation(() => {
      if (failReload) {
        return Promise.reject(new Error("reload failed"));
      }
      return Promise.resolve([kept, target]);
    });
    await openDelete();
    failReload = true;
    fireEvent.click(screen.getByTestId("context-variable-delete-confirm"));
    await waitFor(() => expect(names()).toEqual(["kept"]));
    expect(screen.getByTestId("context-variable-row-value")).toHaveTextContent("old");
  });

  it("cancel does not call delete and keeps both rows", async () => {
    await openDelete();
    fireEvent.click(screen.getByTestId("context-variable-delete-cancel"));
    expect(screen.queryByTestId("context-variable-delete-form")).not.toBeInTheDocument();
    expect(deleteSiteProperty).not.toHaveBeenCalled();
    expect(names()).toEqual(["kept", "nightVar"]);
    expect(
      screen.getAllByTestId("context-variable-row-value").map((node) => node.textContent),
    ).toEqual(["old", "before"]);
  });

  it("does not remove the row for HTTP 400, 403, or 409", async () => {
    await openDelete();
    const cases = [
      [400, "Context variable name is required"],
      [403, "Admin or Designer role required"],
      [409, "Context variable is not listed"],
    ] as const;
    for (const [status, message] of cases) {
      deleteSiteProperty.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message },
      });
      fireEvent.click(screen.getByTestId("context-variable-delete-confirm"));
      await waitFor(() =>
        expect(screen.getByTestId("context-variable-delete-error")).toHaveTextContent(message),
      );
      expect(names()).toEqual(["kept", "nightVar"]);
      expect(screen.getByTestId("context-variable-delete-name")).toHaveTextContent("nightVar");
    }
    expect(deleteSiteProperty).toHaveBeenCalledTimes(3);
  });
});
