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
import { CONTEXT_NAME_MAX_LENGTH } from "@/publishing/contextCopy";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

const createContext = vi.fn();
const createScheme = vi.fn();
const listContexts = vi.fn();
const listSchemesForContext = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  listContexts: (...args: unknown[]) => listContexts(...args),
  listSchemesForContext: (...args: unknown[]) => listSchemesForContext(...args),
  createContext: (...args: unknown[]) => createContext(...args),
  updateContext: vi.fn(),
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

async function openCopy(): Promise<void> {
  listContexts.mockResolvedValue([source]);
  listSchemesForContext.mockResolvedValue([scheme]);
  renderPanel();
  await waitFor(() => expect(screen.getByTestId("context-copy")).toBeTruthy());
  fireEvent.click(screen.getByTestId("context-copy"));
  await waitFor(() => expect(screen.getByTestId("context-copy-form")).toBeTruthy());
}

describe("ContextsPanel publishing context copy", () => {
  beforeEach(() => {
    createContext.mockReset();
    createScheme.mockReset();
    listContexts.mockReset();
    listSchemesForContext.mockReset();
    listSchemesForContext.mockResolvedValue([scheme]);
  });

  it("creates a copy and lists the new name only after success", async () => {
    let resolveCreate: (value: unknown) => void = () => undefined;
    createContext.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    await openCopy();
    expect(screen.getByLabelText(/New name/i)).toHaveValue("Publish copy");
    expect(screen.getByTestId("context-copy-description")).toHaveTextContent("Public site");
    expect(screen.getByTestId("context-copy-schemes-note")).toHaveTextContent(
      /stay on the source/i,
    );
    const listCallsBefore = listContexts.mock.calls.length;
    fireEvent.click(screen.getByTestId("context-copy-submit"));
    await waitFor(() => expect(createContext).toHaveBeenCalledTimes(1));
    expect(screen.getByTestId("context-copy-form")).toBeTruthy();
    expect(screen.queryByTestId("contexts-panel")).toBeNull();
    expect(listContexts.mock.calls.length).toBe(listCallsBefore);
    expect(createContext).toHaveBeenCalledWith({
      name: "Publish copy",
      description: "Public site",
    });
    expect(createScheme).not.toHaveBeenCalled();
    listContexts.mockResolvedValueOnce([
      source,
      { contextId: "9", name: "Publish copy", description: "Public site" },
    ]);
    resolveCreate({
      contextId: "9",
      name: "Publish copy",
      description: "Public site",
    });
    await waitFor(() => expect(screen.queryByTestId("context-copy-form")).toBeNull());
    expect(screen.getByTestId("contexts-panel")).toBeTruthy();
    expect(screen.getByRole("option", { name: "Publish copy" })).toBeTruthy();
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("rejects a blank name before any request", async () => {
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("context-copy-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Name is required");
    expect(createContext).not.toHaveBeenCalled();
    expect(screen.getByTestId("context-copy-form")).toBeTruthy();
  });

  it("rejects an overlong name before any request", async () => {
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "n".repeat(CONTEXT_NAME_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("context-copy-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Publishing context name must be 50 characters or fewer",
    );
    expect(createContext).not.toHaveBeenCalled();
  });

  it("cancel does not call the server", async () => {
    await openCopy();
    fireEvent.click(screen.getByTestId("context-copy-cancel"));
    await waitFor(() => expect(screen.queryByTestId("context-copy-form")).toBeNull());
    expect(createContext).not.toHaveBeenCalled();
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.queryByRole("option", { name: "Publish copy" })).toBeNull();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
  });

  it("cancel after an edit does not POST when the operator confirms discard", async () => {
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "other" },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("context-copy-cancel"));
    await waitFor(() => expect(screen.queryByTestId("context-copy-form")).toBeNull());
    expect(createContext).not.toHaveBeenCalled();
    expect(screen.queryByRole("option", { name: "other" })).toBeNull();
  });

  it.each([
    [400, "Bad Request", "Publishing context name must be 50 characters or fewer"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Publishing context name already exists"],
  ])("HTTP %s does not claim the context was copied", async (status, statusText, message) => {
    createContext.mockRejectedValue({ status, statusText, body: { message } });
    await openCopy();
    const listCallsBefore = listContexts.mock.calls.length;
    fireEvent.click(screen.getByTestId("context-copy-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.getByTestId("context-copy-form")).toBeTruthy();
    expect(listContexts.mock.calls.length).toBe(listCallsBefore);
    expect(createScheme).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("context-copy-cancel"));
    await waitFor(() => expect(screen.queryByTestId("context-copy-form")).toBeNull());
    expect(screen.queryByRole("option", { name: "Publish copy" })).toBeNull();
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
  });

  it("lists the created context when the refresh after success fails", async () => {
    let failRefresh = false;
    createContext.mockResolvedValue({
      contextId: "9",
      name: "Publish copy",
      description: "Public site",
    });
    listContexts.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [source];
    });
    listSchemesForContext.mockResolvedValue([scheme]);
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("context-copy")).toBeTruthy());
    fireEvent.click(screen.getByTestId("context-copy"));
    await waitFor(() => expect(screen.getByTestId("context-copy-form")).toBeTruthy());
    failRefresh = true;
    fireEvent.click(screen.getByTestId("context-copy-submit"));
    await waitFor(() => expect(screen.queryByTestId("context-copy-form")).toBeNull());
    expect(screen.getByRole("option", { name: "Publish copy" })).toBeTruthy();
    expect(screen.getByRole("option", { name: "Publish" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
  });
});
