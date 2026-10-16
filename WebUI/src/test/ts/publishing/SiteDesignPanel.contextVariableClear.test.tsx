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
const cleared: SitePropertyDto = { name: "nightVar", contextId: "3", value: "" };

describe("SiteDesignPanel clear one context variable value", () => {
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

  async function openClear(): Promise<void> {
    render(<SiteDesignPanel />);
    expect(await screen.findByText("nightVar")).toBeTruthy();
    const rows = screen.getAllByTestId("context-variable-row");
    const targetRow = rows.find((row) => row.textContent?.includes("nightVar"));
    expect(targetRow).toBeTruthy();
    fireEvent.click(targetRow!.querySelector("[data-testid='context-variable-clear']")!);
    expect(screen.getByTestId("context-variable-clear-name")).toHaveTextContent("nightVar");
    expect(screen.getByTestId("context-variable-clear-current")).toHaveTextContent("before");
    expect(screen.getByTestId("context-variable-clear-other")).toHaveTextContent("kept: old");
    expect(putSiteProperty).not.toHaveBeenCalled();
  }

  function values(): string[] {
    return screen.getAllByTestId("context-variable-row-value").map((node) => node.textContent ?? "");
  }

  it("clears only that value after confirm and leaves the name and the other variable", async () => {
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
    putSiteProperty.mockResolvedValue(cleared);

    await openClear();
    held = true;
    fireEvent.click(screen.getByTestId("context-variable-clear-confirm"));
    expect(values()).toEqual(["old", "before"]);
    await waitFor(() =>
      expect(putSiteProperty).toHaveBeenCalledWith("42", {
        name: "nightVar",
        contextId: "3",
        clearValue: true,
      }),
    );
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.([kept, cleared]);
    await waitFor(() => expect(values()).toEqual(["old", ""]));
    expect(
      screen.getAllByTestId("context-variable-row-name").map((node) => node.textContent),
    ).toEqual(["kept", "nightVar"]);
    expect(screen.queryByTestId("context-variable-clear-form")).not.toBeInTheDocument();
  });

  it("keeps the cleared value when the reload fails", async () => {
    let failReload = false;
    listSiteProperties.mockImplementation(() => {
      if (failReload) {
        return Promise.reject(new Error("reload failed"));
      }
      return Promise.resolve([kept, target]);
    });
    putSiteProperty.mockResolvedValue(cleared);
    await openClear();
    failReload = true;
    fireEvent.click(screen.getByTestId("context-variable-clear-confirm"));
    await waitFor(() => expect(values()).toEqual(["old", ""]));
    expect(screen.getAllByTestId("context-variable-row-name")[1]).toHaveTextContent("nightVar");
  });

  it("cancel does not write, and the change-value form still refuses a blank value", async () => {
    render(<SiteDesignPanel />);
    expect(await screen.findByText("nightVar")).toBeTruthy();
    const row = () =>
      screen
        .getAllByTestId("context-variable-row")
        .find((item) => item.textContent?.includes("nightVar"))!;
    fireEvent.click(row().querySelector("[data-testid='context-variable-change-value']")!);
    fireEvent.change(screen.getByTestId("context-variable-value-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("context-variable-value-save"));
    expect(await screen.findByTestId("context-variable-value-error")).toHaveTextContent(
      "Context variable value is required",
    );
    expect(putSiteProperty).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("context-variable-value-cancel"));

    fireEvent.click(row().querySelector("[data-testid='context-variable-clear']")!);
    fireEvent.click(screen.getByTestId("context-variable-clear-cancel"));
    expect(screen.queryByTestId("context-variable-clear-form")).not.toBeInTheDocument();
    expect(putSiteProperty).not.toHaveBeenCalled();
    expect(values()).toEqual(["old", "before"]);
  });

  it("does not clear the value for HTTP 400, 403, or 409", async () => {
    await openClear();
    const cases = [
      [400, "Context variable name is required"],
      [403, "Admin or Designer role required"],
      [409, "Context variable is not listed"],
    ] as const;
    for (const [status, message] of cases) {
      putSiteProperty.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message },
      });
      fireEvent.click(screen.getByTestId("context-variable-clear-confirm"));
      await waitFor(() =>
        expect(screen.getByTestId("context-variable-clear-error")).toHaveTextContent(message),
      );
      expect(values()).toEqual(["old", "before"]);
      expect(screen.getByTestId("context-variable-clear-name")).toHaveTextContent("nightVar");
    }
    expect(putSiteProperty).toHaveBeenCalledTimes(3);
  });
});
