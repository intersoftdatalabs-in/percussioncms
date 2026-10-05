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
import { CONTEXT_NAME_MAX_LENGTH } from "@/publishing/contextRename";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

const updateContext = vi.fn();
const listContexts = vi.fn();
const listSchemesForContext = vi.fn();
const createScheme = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  listContexts: (...args: unknown[]) => listContexts(...args),
  listSchemesForContext: (...args: unknown[]) => listSchemesForContext(...args),
  createContext: vi.fn(),
  updateContext: (...args: unknown[]) => updateContext(...args),
  deleteContext: vi.fn(),
  createScheme: (...args: unknown[]) => createScheme(...args),
  updateScheme: vi.fn(),
  deleteScheme: vi.fn(),
  getScheme: vi.fn(),
}));

const source = {
  contextId: "3",
  name: "Publish",
  description: "Public site",
  defaultSchemeId: "11",
};

const scheme = {
  schemeId: "11",
  name: "Article",
  generator: "Java/global/percussion/contentassembler/sys_JexlAssemblyLocation",
  contextId: "3",
};

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <ContextsPanel />
    </DirtyFormProvider>,
  );
}

async function openRename(): Promise<void> {
  listContexts.mockResolvedValue([source]);
  listSchemesForContext.mockResolvedValue([scheme]);
  renderPanel();
  await waitFor(() => expect(screen.getByTestId("context-rename")).toBeTruthy());
  await waitFor(() => expect(screen.getByRole("button", { name: "Article" })).toBeTruthy());
  fireEvent.click(screen.getByTestId("context-rename"));
  await waitFor(() => expect(screen.getByTestId("context-rename-form")).toBeTruthy());
}

describe("ContextsPanel publishing context rename", () => {
  beforeEach(() => {
    updateContext.mockReset();
    listContexts.mockReset();
    listSchemesForContext.mockReset();
    createScheme.mockReset();
    listSchemesForContext.mockResolvedValue([scheme]);
  });

  it("shows the new name only after the rename reload and keeps the scheme", async () => {
    let releaseUpdate: (value: unknown) => void = () => undefined;
    updateContext.mockImplementation(
      () =>
        new Promise((resolve) => {
          releaseUpdate = resolve;
        }),
    );
    let releaseList: ((rows: unknown) => void) | undefined;
    let listed = false;
    listContexts.mockImplementation(
      () =>
        new Promise((resolve) => {
          if (!listed) {
            resolve([source]);
            return;
          }
          releaseList = resolve;
        }),
    );

    renderPanel();
    await waitFor(() => expect(screen.getByTestId("context-rename")).toBeTruthy());
    await waitFor(() => expect(screen.getByRole("button", { name: "Article" })).toBeTruthy());
    const schemeLoads = listSchemesForContext.mock.calls.length;
    fireEvent.click(screen.getByTestId("context-rename"));
    await waitFor(() => expect(screen.getByTestId("context-rename-form")).toBeTruthy());
    expect(screen.getByLabelText("* Name")).toHaveValue("Publish");
    expect(screen.getByTestId("context-rename-description")).toHaveTextContent("Public site");
    expect(screen.getByTestId("context-rename-schemes-note")).toHaveTextContent(
      /stay on this context/i,
    );

    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "Renamed" },
    });
    listed = true;
    fireEvent.click(screen.getByTestId("context-rename-submit"));
    await waitFor(() => expect(updateContext).toHaveBeenCalledTimes(1));
    expect(updateContext).toHaveBeenCalledWith("3", { name: "Renamed" });
    expect(screen.getByTestId("context-rename-form")).toBeTruthy();
    expect(screen.queryByRole("option", { name: "Renamed" })).toBeNull();
    expect(listContexts).toHaveBeenCalledTimes(1);
    expect(createScheme).not.toHaveBeenCalled();

    releaseUpdate({ ...source, name: "Renamed" });
    await waitFor(() => expect(screen.queryByTestId("context-rename-form")).toBeNull());
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.queryByRole("option", { name: "Renamed" })).toBeNull();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(releaseList).toEqual(expect.any(Function));
    releaseList?.([{ ...source, name: "Renamed" }]);
    await waitFor(() => expect(screen.getByRole("option", { name: "Renamed" })).toBeTruthy());
    expect(screen.queryByRole("option", { name: "Publish" })).toBeNull();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("rejects a blank or overlong name without calling the server", async () => {
    await openRename();
    const loads = listContexts.mock.calls.length;
    const schemeLoads = listSchemesForContext.mock.calls.length;
    fireEvent.change(screen.getByLabelText("* Name"), { target: { value: " " } });
    fireEvent.click(screen.getByTestId("context-rename-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Name is required");
    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "n".repeat(CONTEXT_NAME_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("context-rename-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Publishing context name must be 50 characters or fewer",
    );
    expect(updateContext).not.toHaveBeenCalled();
    expect(listContexts).toHaveBeenCalledTimes(loads);
    expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("context-rename-cancel"));
    await waitFor(() => expect(screen.queryByTestId("context-rename-form")).toBeNull());
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
  });

  it("cancel keeps the old name and the scheme and does not update", async () => {
    await openRename();
    fireEvent.change(screen.getByLabelText("* Name"), { target: { value: "Nope" } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("context-rename-cancel"));
    await waitFor(() => expect(screen.queryByTestId("context-rename-form")).toBeNull());
    expect(updateContext).not.toHaveBeenCalled();
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.queryByRole("option", { name: "Nope" })).toBeNull();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it.each([
    [400, "Bad Request", "Publishing context name must be 50 characters or fewer"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Publishing context name already exists"],
  ])(
    "keeps the previous name and scheme when HTTP %s rejects the rename",
    async (status, statusText, message) => {
      updateContext.mockRejectedValue({
        status,
        statusText,
        body: { message },
      });
      await openRename();
      const loads = listContexts.mock.calls.length;
      const schemeLoads = listSchemesForContext.mock.calls.length;
      fireEvent.change(screen.getByLabelText("* Name"), { target: { value: "Taken" } });
      fireEvent.click(screen.getByTestId("context-rename-submit"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("context-rename-form")).toBeTruthy();
      expect(screen.queryByRole("option", { name: "Taken" })).toBeNull();
      expect(screen.getByTestId("context-rename-description")).toHaveTextContent("Public site");
      expect(listContexts).toHaveBeenCalledTimes(loads);
      expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
      vi.spyOn(window, "confirm").mockReturnValue(true);
      fireEvent.click(screen.getByTestId("context-rename-cancel"));
      await waitFor(() => expect(screen.queryByTestId("context-rename-form")).toBeNull());
      expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
      expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    },
  );

  it("keeps the scheme when the reload after success fails", async () => {
    updateContext.mockResolvedValue({ ...source, name: "Renamed" });
    let failRefresh = false;
    listContexts.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [source];
    });
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("context-rename")).toBeTruthy());
    await waitFor(() => expect(screen.getByRole("button", { name: "Article" })).toBeTruthy());
    const schemeLoads = listSchemesForContext.mock.calls.length;
    fireEvent.click(screen.getByTestId("context-rename"));
    await waitFor(() => expect(screen.getByTestId("context-rename-form")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("* Name"), { target: { value: "Renamed" } });
    failRefresh = true;
    fireEvent.click(screen.getByTestId("context-rename-submit"));
    await waitFor(() => expect(screen.queryByTestId("context-rename-form")).toBeNull());
    expect(screen.getByRole("option", { name: "Renamed" })).toBeTruthy();
    expect(screen.queryByRole("option", { name: "Publish" })).toBeNull();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
    expect(createScheme).not.toHaveBeenCalled();
  });
});
