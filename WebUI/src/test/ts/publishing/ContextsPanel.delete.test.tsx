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

import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ContextsPanel } from "@/publishing/design/ContextsPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

const deleteScheme = vi.fn();
const listContexts = vi.fn();
const listSchemesForContext = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  listContexts: (...args: unknown[]) => listContexts(...args),
  listSchemesForContext: (...args: unknown[]) => listSchemesForContext(...args),
  createContext: vi.fn(),
  updateContext: vi.fn(),
  deleteContext: vi.fn(),
  createScheme: vi.fn(),
  updateScheme: vi.fn(),
  deleteScheme: (...args: unknown[]) => deleteScheme(...args),
  getScheme: vi.fn(),
}));

const article = {
  schemeId: "11",
  name: "Article",
  generator: "gen",
  contextId: "3",
};
const keep = {
  schemeId: "12",
  name: "Keep",
  generator: "gen",
  contextId: "3",
};

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <ContextsPanel />
    </DirtyFormProvider>,
  );
}

function deleteButton(name: string): HTMLElement {
  const row = screen.getByRole("button", { name }).closest("li");
  if (!row) {
    throw new Error(`no row for ${name}`);
  }
  return within(row).getByTestId("location-scheme-delete");
}

describe("ContextsPanel location scheme delete", () => {
  beforeEach(() => {
    deleteScheme.mockReset();
    listContexts.mockReset();
    listSchemesForContext.mockReset();
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    listSchemesForContext.mockResolvedValue([article, keep]);
    vi.restoreAllMocks();
  });

  it("removes one scheme only after delete succeeds", async () => {
    let resolveDelete: () => void = () => undefined;
    deleteScheme.mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveDelete = resolve;
        }),
    );
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPanel();
    await waitFor(() => expect(deleteButton("Article")).toBeTruthy());
    const listCallsBefore = listSchemesForContext.mock.calls.length;
    fireEvent.click(deleteButton("Article"));
    await waitFor(() => expect(deleteScheme).toHaveBeenCalledWith("11"));
    expect(deleteScheme).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Keep" })).toBeTruthy();
    expect(listSchemesForContext.mock.calls.length).toBe(listCallsBefore);
    listSchemesForContext.mockResolvedValueOnce([keep]);
    resolveDelete();
    await waitFor(() => expect(screen.queryByRole("button", { name: "Article" })).toBeNull());
    expect(screen.getByRole("button", { name: "Keep" })).toBeTruthy();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("cancel does not call the server", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    renderPanel();
    await waitFor(() => expect(deleteButton("Article")).toBeTruthy());
    fireEvent.click(deleteButton("Article"));
    expect(deleteScheme).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it.each([
    [400, "Bad Request", "schemeId is required"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Location scheme is in use"],
  ])("HTTP %s does not claim the scheme was deleted", async (status, statusText, message) => {
    deleteScheme.mockRejectedValue({ status, statusText, body: { message } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPanel();
    await waitFor(() => expect(deleteButton("Article")).toBeTruthy());
    const listCallsBefore = listSchemesForContext.mock.calls.length;
    fireEvent.click(deleteButton("Article"));
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Keep" })).toBeTruthy();
    expect(listSchemesForContext.mock.calls.length).toBe(listCallsBefore);
    expect(deleteScheme).toHaveBeenCalledTimes(1);
  });

  it("drops the row when the refresh after success fails", async () => {
    let failRefresh = false;
    deleteScheme.mockImplementation(async () => {
      failRefresh = true;
    });
    listSchemesForContext.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [article, keep];
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPanel();
    await waitFor(() => expect(deleteButton("Article")).toBeTruthy());
    fireEvent.click(deleteButton("Article"));
    await waitFor(() => expect(screen.queryByRole("button", { name: "Article" })).toBeNull());
    expect(screen.getByRole("button", { name: "Keep" })).toBeTruthy();
    expect(screen.queryByRole("alert")).toBeNull();
  });
});
