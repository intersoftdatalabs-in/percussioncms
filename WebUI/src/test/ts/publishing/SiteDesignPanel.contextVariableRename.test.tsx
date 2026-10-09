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
const renamed: SitePropertyDto = { name: "nextName", contextId: "3", value: "before" };

describe("SiteDesignPanel rename one context variable", () => {
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

  async function openRename(): Promise<void> {
    render(<SiteDesignPanel />);
    expect(await screen.findByText("nightVar")).toBeTruthy();
    const rows = screen.getAllByTestId("context-variable-row");
    const targetRow = rows.find((row) => row.textContent?.includes("nightVar"));
    expect(targetRow).toBeTruthy();
    fireEvent.click(targetRow!.querySelector("[data-testid='context-variable-rename']")!);
    expect(screen.getByTestId("context-variable-rename-name")).toHaveTextContent("nightVar");
    expect(screen.getByTestId("context-variable-rename-value")).toHaveTextContent("before");
    expect(screen.getByTestId("context-variable-rename-other")).toHaveTextContent("kept: old");
    expect(putSiteProperty).not.toHaveBeenCalled();
  }

  function fillName(value: string): void {
    fireEvent.change(screen.getByTestId("context-variable-rename-input"), {
      target: { value },
    });
  }

  function names(): string[] {
    return screen.getAllByTestId("context-variable-row-name").map((node) => node.textContent ?? "");
  }

  it("shows the new name only after the server accepts and leaves the other variable", async () => {
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
    putSiteProperty.mockResolvedValue(renamed);

    await openRename();
    fillName("  nextName  ");
    held = true;
    fireEvent.click(screen.getByTestId("context-variable-rename-save"));
    expect(names()).toEqual(["kept", "nightVar"]);
    await waitFor(() =>
      expect(putSiteProperty).toHaveBeenCalledWith("42", {
        name: "nightVar",
        contextId: "3",
        newName: "nextName",
        renameName: true,
      }),
    );
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.([kept, renamed]);
    await waitFor(() => expect(names()).toEqual(["kept", "nextName"]));
    expect(
      screen.getAllByTestId("context-variable-row-value").map((node) => node.textContent),
    ).toEqual(["old", "before"]);
    expect(screen.queryByTestId("context-variable-rename-form")).not.toBeInTheDocument();
  });

  it("keeps the value and the other variable when the reload fails", async () => {
    let failReload = false;
    listSiteProperties.mockImplementation(() => {
      if (failReload) {
        return Promise.reject(new Error("reload failed"));
      }
      return Promise.resolve([kept, target]);
    });
    putSiteProperty.mockResolvedValue(renamed);
    await openRename();
    fillName("nextName");
    failReload = true;
    fireEvent.click(screen.getByTestId("context-variable-rename-save"));
    await waitFor(() => expect(names()).toEqual(["kept", "nextName"]));
    expect(
      screen.getAllByTestId("context-variable-row-value").map((node) => node.textContent),
    ).toEqual(["old", "before"]);
  });

  it("does not write a blank, overlong, or duplicate name, or a cancel", async () => {
    await openRename();
    fillName("   ");
    fireEvent.click(screen.getByTestId("context-variable-rename-save"));
    expect(await screen.findByTestId("context-variable-rename-error")).toHaveTextContent(
      "Context variable name is required",
    );
    expect(putSiteProperty).not.toHaveBeenCalled();
    expect(names()).toEqual(["kept", "nightVar"]);

    fillName("n".repeat(51));
    fireEvent.click(screen.getByTestId("context-variable-rename-save"));
    expect(screen.getByTestId("context-variable-rename-error")).toHaveTextContent(
      "50 characters or fewer",
    );
    expect(putSiteProperty).not.toHaveBeenCalled();

    fillName("kept");
    fireEvent.click(screen.getByTestId("context-variable-rename-save"));
    expect(screen.getByTestId("context-variable-rename-error")).toHaveTextContent(
      "already exists",
    );
    expect(putSiteProperty).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("context-variable-rename-cancel"));
    expect(screen.queryByTestId("context-variable-rename-form")).not.toBeInTheDocument();
    expect(putSiteProperty).not.toHaveBeenCalled();
    expect(
      screen.getAllByTestId("context-variable-row-value").map((node) => node.textContent),
    ).toEqual(["old", "before"]);
  });

  it("does not claim success for HTTP 400, 403, or 409", async () => {
    await openRename();
    const cases = [
      [400, "Context variable name is required"],
      [403, "Admin or Designer role required"],
      [409, "Context variable already exists"],
    ] as const;
    for (const [status, message] of cases) {
      putSiteProperty.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message },
      });
      fillName("nextName");
      fireEvent.click(screen.getByTestId("context-variable-rename-save"));
      await waitFor(() =>
        expect(screen.getByTestId("context-variable-rename-error")).toHaveTextContent(message),
      );
      expect(names()).toEqual(["kept", "nightVar"]);
      expect(
        screen.getAllByTestId("context-variable-row-value").map((node) => node.textContent),
      ).toEqual(["old", "before"]);
      expect(screen.getByTestId("context-variable-rename-name")).toHaveTextContent("nightVar");
    }
    expect(putSiteProperty).toHaveBeenCalledTimes(3);
  });
});
