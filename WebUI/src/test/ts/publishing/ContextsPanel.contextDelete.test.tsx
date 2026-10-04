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
import { ContextsPanel } from "@/publishing/design/ContextsPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { CONTEXT_HAS_LOCATION_SCHEMES } from "@/publishing/contextDelete";

const deleteContext = vi.fn();
const listContexts = vi.fn();
const listSchemesForContext = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  listContexts: (...args: unknown[]) => listContexts(...args),
  listSchemesForContext: (...args: unknown[]) => listSchemesForContext(...args),
  createContext: vi.fn(),
  updateContext: vi.fn(),
  deleteContext: (...args: unknown[]) => deleteContext(...args),
  createScheme: vi.fn(),
  updateScheme: vi.fn(),
  deleteScheme: vi.fn(),
  getScheme: vi.fn(),
}));

const publish = { contextId: "3", name: "Publish" };
const keep = { contextId: "4", name: "Keep" };

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <ContextsPanel />
    </DirtyFormProvider>,
  );
}

describe("ContextsPanel publishing context delete", () => {
  beforeEach(() => {
    deleteContext.mockReset();
    listContexts.mockReset();
    listSchemesForContext.mockReset();
    listContexts.mockResolvedValue([publish, keep]);
    listSchemesForContext.mockResolvedValue([]);
    vi.restoreAllMocks();
  });

  it("removes one context only after delete succeeds", async () => {
    let resolveDelete: () => void = () => undefined;
    deleteContext.mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveDelete = resolve;
        }),
    );
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("context-delete")).toBeTruthy());
    const listCallsBefore = listContexts.mock.calls.length;
    fireEvent.click(screen.getByTestId("context-delete"));
    await waitFor(() => expect(deleteContext).toHaveBeenCalledWith("3"));
    expect(deleteContext).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.getByRole("option", { name: "Keep" })).toBeTruthy();
    expect(listContexts.mock.calls.length).toBe(listCallsBefore);
    listContexts.mockResolvedValueOnce([keep]);
    resolveDelete();
    await waitFor(() => expect(screen.queryByRole("option", { name: "Publish" })).toBeNull());
    expect(screen.getByRole("option", { name: "Keep" })).toBeTruthy();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("cancel does not call the server", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("context-delete")).toBeTruthy());
    fireEvent.click(screen.getByTestId("context-delete"));
    expect(deleteContext).not.toHaveBeenCalled();
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.getByRole("option", { name: "Keep" })).toBeTruthy();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it.each([
    [400, "Bad Request", "contextId is required"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", CONTEXT_HAS_LOCATION_SCHEMES],
  ])("HTTP %s does not claim the context was deleted", async (status, statusText, message) => {
    deleteContext.mockRejectedValue({ status, statusText, body: { message } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("context-delete")).toBeTruthy());
    const listCallsBefore = listContexts.mock.calls.length;
    fireEvent.click(screen.getByTestId("context-delete"));
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.getByRole("option", { name: "Keep" })).toBeTruthy();
    expect(listContexts.mock.calls.length).toBe(listCallsBefore);
    expect(deleteContext).toHaveBeenCalledTimes(1);
  });

  it("drops the context when the refresh after success fails", async () => {
    let failRefresh = false;
    deleteContext.mockImplementation(async () => {
      failRefresh = true;
    });
    listContexts.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [publish, keep];
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("context-delete")).toBeTruthy());
    fireEvent.click(screen.getByTestId("context-delete"));
    await waitFor(() => expect(screen.queryByRole("option", { name: "Publish" })).toBeNull());
    expect(screen.getByRole("option", { name: "Keep" })).toBeTruthy();
    expect(screen.queryByRole("alert")).toBeNull();
  });
});
