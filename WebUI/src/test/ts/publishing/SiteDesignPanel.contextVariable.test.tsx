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
const created: SitePropertyDto = {
  name: "nightVar",
  contextId: "3",
  value: "night-value",
};

describe("SiteDesignPanel set one context variable", () => {
  beforeEach(() => {
    listDesignSites.mockReset();
    listContexts.mockReset();
    listSiteProperties.mockReset();
    putSiteProperty.mockReset();
    listDesignSites.mockResolvedValue([
      { siteId: "42", name: "Enterprise", folderRoot: "//Sites/Enterprise" },
    ]);
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    listSiteProperties.mockResolvedValue([kept]);
  });

  async function openForm(): Promise<void> {
    render(<SiteDesignPanel />);
    expect(await screen.findByTestId("context-variable-row-name")).toHaveTextContent(
      "kept",
    );
    expect(screen.getByTestId("context-variable-row-value")).toHaveTextContent("old");
    expect(screen.getByTestId("context-variable-form")).toBeTruthy();
  }

  function fill(name: string, value: string): void {
    fireEvent.change(screen.getByTestId("context-variable-name"), {
      target: { value: name },
    });
    fireEvent.change(screen.getByTestId("context-variable-value"), {
      target: { value },
    });
  }

  it("shows the new variable only after reload and leaves the other value", async () => {
    let releaseReload: ((rows: SitePropertyDto[]) => void) | undefined;
    let held = false;
    listSiteProperties.mockImplementation(
      () =>
        new Promise((resolve) => {
          if (!held) {
            resolve([kept]);
            return;
          }
          releaseReload = resolve;
        }),
    );
    putSiteProperty.mockResolvedValue(created);

    await openForm();
    fill("  nightVar  ", "  night-value  ");
    expect(putSiteProperty).not.toHaveBeenCalled();
    held = true;
    fireEvent.click(screen.getByTestId("context-variable-save"));
    expect(screen.getByTestId("context-variable-row-name")).toHaveTextContent("kept");
    expect(screen.getByTestId("context-variable-row-value")).toHaveTextContent("old");
    expect(screen.queryByText("nightVar")).not.toBeInTheDocument();
    await waitFor(() =>
      expect(putSiteProperty).toHaveBeenCalledWith("42", {
        name: "nightVar",
        contextId: "3",
        value: "night-value",
      }),
    );
    expect(releaseReload).toEqual(expect.any(Function));
    releaseReload?.([kept, created]);
    await waitFor(() => expect(screen.getAllByTestId("context-variable-row")).toHaveLength(2));
    const names = screen
      .getAllByTestId("context-variable-row-name")
      .map((node) => node.textContent);
    const values = screen
      .getAllByTestId("context-variable-row-value")
      .map((node) => node.textContent);
    expect(names).toEqual(["kept", "nightVar"]);
    expect(values).toEqual(["old", "night-value"]);
  });

  it("keeps both variables when the reload fails", async () => {
    let failReload = false;
    listSiteProperties.mockImplementation(() => {
      if (failReload) {
        return Promise.reject(new Error("reload failed"));
      }
      return Promise.resolve([kept]);
    });
    putSiteProperty.mockResolvedValue(created);
    await openForm();
    fill("nightVar", "night-value");
    failReload = true;
    fireEvent.click(screen.getByTestId("context-variable-save"));
    await waitFor(() =>
      expect(screen.getAllByTestId("context-variable-row-name")).toHaveLength(2),
    );
    expect(screen.getAllByTestId("context-variable-row-value")[0]).toHaveTextContent("old");
    expect(screen.getAllByTestId("context-variable-row-value")[1]).toHaveTextContent(
      "night-value",
    );
  });

  it("does not write a blank name or an existing name", async () => {
    await openForm();
    fill("   ", "night-value");
    fireEvent.click(screen.getByTestId("context-variable-save"));
    expect(await screen.findByTestId("context-variable-error")).toHaveTextContent(
      "Context variable name is required",
    );
    expect(putSiteProperty).not.toHaveBeenCalled();
    expect(screen.getByTestId("context-variable-row-value")).toHaveTextContent("old");

    fill("kept", "replaced");
    fireEvent.click(screen.getByTestId("context-variable-save"));
    expect(screen.getByTestId("context-variable-error")).toHaveTextContent(
      "Context variable already exists",
    );
    expect(putSiteProperty).not.toHaveBeenCalled();
    expect(screen.getByTestId("context-variable-row-value")).toHaveTextContent("old");
  });

  it("does not add a row for HTTP 400, 403, or 409", async () => {
    await openForm();
    const cases = [
      [400, "Context variable value is required"],
      [403, "Admin or Designer role required"],
      [409, "Context variable already exists"],
    ] as const;
    for (const [status, message] of cases) {
      putSiteProperty.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message },
      });
      fill(`night${status}`, "night-value");
      fireEvent.click(screen.getByTestId("context-variable-save"));
      expect(await screen.findByTestId("context-variable-error")).toHaveTextContent(message);
      expect(screen.getAllByTestId("context-variable-row")).toHaveLength(1);
      expect(screen.getByTestId("context-variable-row-name")).toHaveTextContent("kept");
      expect(screen.getByTestId("context-variable-row-value")).toHaveTextContent("old");
    }
    expect(putSiteProperty).toHaveBeenCalledTimes(3);
  });
});
