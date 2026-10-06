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
import { CONTEXT_DESCRIPTION_MAX_LENGTH } from "@/publishing/contextDescription";
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

async function openDescribe(): Promise<void> {
  listContexts.mockResolvedValue([source]);
  listSchemesForContext.mockResolvedValue([scheme]);
  renderPanel();
  await waitFor(() => expect(screen.getByTestId("context-describe")).toBeTruthy());
  await waitFor(() => expect(screen.getByRole("button", { name: "Article" })).toBeTruthy());
  fireEvent.click(screen.getByTestId("context-describe"));
  await waitFor(() => expect(screen.getByTestId("context-describe-form")).toBeTruthy());
}

describe("ContextsPanel publishing context description", () => {
  beforeEach(() => {
    updateContext.mockReset();
    listContexts.mockReset();
    listSchemesForContext.mockReset();
    createScheme.mockReset();
    listSchemesForContext.mockResolvedValue([scheme]);
  });

  it("shows the new description only after reload and keeps the name and scheme", async () => {
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
    await waitFor(() => expect(screen.getByTestId("context-describe")).toBeTruthy());
    await waitFor(() => expect(screen.getByRole("button", { name: "Article" })).toBeTruthy());
    expect(screen.getByTestId("context-description-3")).toHaveTextContent("Public site");
    const schemeLoads = listSchemesForContext.mock.calls.length;
    fireEvent.click(screen.getByTestId("context-describe"));
    await waitFor(() => expect(screen.getByTestId("context-describe-form")).toBeTruthy());
    expect(screen.getByTestId("context-describe-name")).toHaveTextContent("Publish");
    expect(screen.getByTestId("context-describe-scheme")).toHaveTextContent("Article");
    expect(screen.getByLabelText("Description")).toHaveValue("Public site");

    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "  Night notes  " },
    });
    listed = true;
    fireEvent.click(screen.getByTestId("context-describe-submit"));
    await waitFor(() => expect(updateContext).toHaveBeenCalledTimes(1));
    expect(updateContext).toHaveBeenCalledWith("3", { description: "Night notes" });
    expect(screen.getByTestId("context-describe-form")).toBeTruthy();
    expect(screen.queryByTestId("context-description-3")).toBeNull();
    expect(listContexts).toHaveBeenCalledTimes(1);
    expect(createScheme).not.toHaveBeenCalled();

    releaseUpdate({ ...source, description: "Night notes" });
    await waitFor(() => expect(screen.queryByTestId("context-describe-form")).toBeNull());
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.getByTestId("context-description-3")).toHaveTextContent("Public site");
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(releaseList).toEqual(expect.any(Function));
    releaseList?.([{ ...source, description: "Night notes" }]);
    await waitFor(() =>
      expect(screen.getByTestId("context-description-3")).toHaveTextContent("Night notes"),
    );
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("clears a blank description only after reload", async () => {
    updateContext.mockResolvedValue({ ...source, description: "" });
    listContexts.mockResolvedValue([source]);
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("context-describe")).toBeTruthy());
    await waitFor(() => expect(screen.getByRole("button", { name: "Article" })).toBeTruthy());
    fireEvent.click(screen.getByTestId("context-describe"));
    await waitFor(() => expect(screen.getByTestId("context-describe-form")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("Description"), { target: { value: "   " } });
    listContexts.mockResolvedValueOnce([{ ...source, description: "" }]);
    fireEvent.click(screen.getByTestId("context-describe-submit"));
    await waitFor(() => expect(screen.queryByTestId("context-describe-form")).toBeNull());
    expect(updateContext).toHaveBeenCalledWith("3", { description: "" });
    expect(screen.getByTestId("context-description-3")).toHaveTextContent("");
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
  });

  it("rejects an overlong description without calling the server", async () => {
    await openDescribe();
    const loads = listContexts.mock.calls.length;
    const schemeLoads = listSchemesForContext.mock.calls.length;
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "d".repeat(CONTEXT_DESCRIPTION_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("context-describe-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Publishing context description must be 255 characters or fewer",
    );
    expect(updateContext).not.toHaveBeenCalled();
    expect(listContexts).toHaveBeenCalledTimes(loads);
    expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("context-describe-cancel"));
    await waitFor(() => expect(screen.queryByTestId("context-describe-form")).toBeNull());
    expect(screen.getByTestId("context-description-3")).toHaveTextContent("Public site");
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
  });

  it("cancel keeps the old description and the scheme and does not update", async () => {
    await openDescribe();
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "Will not save" },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("context-describe-cancel"));
    await waitFor(() => expect(screen.queryByTestId("context-describe-form")).toBeNull());
    expect(updateContext).not.toHaveBeenCalled();
    expect(screen.getByTestId("context-description-3")).toHaveTextContent("Public site");
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it.each([
    [400, "Bad Request", "Publishing context description must be 255 characters or fewer"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Publishing context name already exists"],
  ])(
    "keeps the previous description and scheme when HTTP %s rejects the save",
    async (status, statusText, message) => {
      updateContext.mockRejectedValue({
        status,
        statusText,
        body: { message },
      });
      await openDescribe();
      const loads = listContexts.mock.calls.length;
      const schemeLoads = listSchemesForContext.mock.calls.length;
      fireEvent.change(screen.getByLabelText("Description"), {
        target: { value: "Will not stick" },
      });
      fireEvent.click(screen.getByTestId("context-describe-submit"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("context-describe-form")).toBeTruthy();
      expect(screen.queryByTestId("context-description-3")).toBeNull();
      expect(screen.getByTestId("context-describe-name")).toHaveTextContent("Publish");
      expect(screen.getByTestId("context-describe-scheme")).toHaveTextContent("Article");
      expect(listContexts).toHaveBeenCalledTimes(loads);
      expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
      vi.spyOn(window, "confirm").mockReturnValue(true);
      fireEvent.click(screen.getByTestId("context-describe-cancel"));
      await waitFor(() => expect(screen.queryByTestId("context-describe-form")).toBeNull());
      expect(screen.getByTestId("context-description-3")).toHaveTextContent("Public site");
      expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
      expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    },
  );

  it("keeps the name and scheme when the reload after success fails", async () => {
    updateContext.mockResolvedValue({ ...source, description: "Night notes" });
    let failRefresh = false;
    listContexts.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [source];
    });
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("context-describe")).toBeTruthy());
    await waitFor(() => expect(screen.getByRole("button", { name: "Article" })).toBeTruthy());
    const schemeLoads = listSchemesForContext.mock.calls.length;
    fireEvent.click(screen.getByTestId("context-describe"));
    await waitFor(() => expect(screen.getByTestId("context-describe-form")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "Night notes" },
    });
    failRefresh = true;
    fireEvent.click(screen.getByTestId("context-describe-submit"));
    await waitFor(() => expect(screen.queryByTestId("context-describe-form")).toBeNull());
    expect(screen.getByTestId("context-description-3")).toHaveTextContent("Night notes");
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
    expect(createScheme).not.toHaveBeenCalled();
  });
});
